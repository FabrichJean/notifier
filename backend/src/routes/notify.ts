import crypto from "node:crypto";
import { Router } from "express";
import { z } from "zod";
import { apiKeyAuth } from "../auth/apiKey";
import { db } from "../db";
import { broadcast, recordDeliveries } from "../ws/hub";
import { DEFAULT_ALARM_CONFIG, type AlarmConfig, type DeviceEnvelope } from "../types";

const alarmConfigSchema = z.object({
  fullScreen: z.boolean().optional(),
  sound: z.enum(["default", "alarm_classic", "gentle"]).optional(),
  loop: z.boolean().optional(),
  vibrate: z.boolean().optional(),
  snoozeMinutes: z.number().int().positive().nullable().optional(),
});

const notifyBodySchema = z.object({
  title: z.string().min(1).max(200),
  body: z.string().min(1).max(2000),
  type: z.enum(["notification", "alarm"]),
  alarm: alarmConfigSchema.optional(),
  metadata: z.record(z.unknown()).optional(),
});

export const notifyRouter = Router();

notifyRouter.post("/api/notify", apiKeyAuth, (req, res) => {
  const parsed = notifyBodySchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: "invalid request body", details: parsed.error.flatten() });
    return;
  }

  const { title, body, type, alarm, metadata } = parsed.data;
  const apiKey = req.apiKey!;

  const alarmConfig: AlarmConfig | null =
    type === "alarm" ? { ...DEFAULT_ALARM_CONFIG, ...alarm } : null;

  const id = crypto.randomUUID();
  const createdAt = new Date().toISOString();

  db.prepare(
    `INSERT INTO notifications
      (id, source_app_id, source_app_name, title, body, type, alarm_config, metadata, created_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`
  ).run(
    id,
    apiKey.id,
    apiKey.name,
    title,
    body,
    type,
    alarmConfig ? JSON.stringify(alarmConfig) : null,
    metadata ? JSON.stringify(metadata) : null,
    createdAt
  );

  const envelope: DeviceEnvelope = {
    event: "notification",
    data: {
      id,
      title,
      body,
      type,
      alarm: alarmConfig,
      source: apiKey.name,
      metadata: metadata ?? null,
      createdAt,
    },
  };

  const delivered = broadcast(envelope);
  if (delivered.length > 0) {
    recordDeliveries(
      id,
      delivered.map((d) => d.deviceId)
    );
  }

  res.status(201).json({ id, createdAt, deliveredTo: delivered.length });
});
