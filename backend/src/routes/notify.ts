import crypto from "node:crypto";
import { Router } from "express";
import { z } from "zod";
import { apiKeyAuth } from "../auth/apiKey";
import { ADMIN_CONNECTION_ID } from "../auth/adminAuth";
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
  deviceId: z.string().min(1).optional(),
});

export const notifyRouter = Router();

notifyRouter.post("/api/notify", apiKeyAuth, (req, res) => {
  const parsed = notifyBodySchema.safeParse(req.body);
  if (!parsed.success) {
    res.status(400).json({ error: "invalid request body", details: parsed.error.flatten() });
    return;
  }

  const { title, body, type, alarm, metadata, deviceId } = parsed.data;
  const apiKey = req.apiKey!;

  let targetDeviceName: string | null = null;
  if (deviceId) {
    const device = db
      .prepare("SELECT name, revoked_at FROM devices WHERE id = ?")
      .get(deviceId) as { name: string; revoked_at: string | null } | undefined;

    if (!device) {
      res.status(404).json({ error: "device not found" });
      return;
    }
    if (device.revoked_at) {
      res.status(400).json({ error: "device has been revoked" });
      return;
    }
    targetDeviceName = device.name;
  }

  const alarmConfig: AlarmConfig | null =
    type === "alarm" ? { ...DEFAULT_ALARM_CONFIG, ...alarm } : null;

  const id = crypto.randomUUID();
  const createdAt = new Date().toISOString();

  db.prepare(
    `INSERT INTO notifications
      (id, source_app_id, source_app_name, title, body, type, alarm_config, metadata, target_device_id, created_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
  ).run(
    id,
    apiKey.id,
    apiKey.name,
    title,
    body,
    type,
    alarmConfig ? JSON.stringify(alarmConfig) : null,
    metadata ? JSON.stringify(metadata) : null,
    deviceId ?? null,
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
      targetDeviceId: deviceId ?? null,
      targetDeviceName,
      createdAt,
    },
  };

  const delivered = broadcast(envelope, deviceId);
  const realDeviceIds = delivered
    .map((d) => d.deviceId)
    .filter((id) => id !== ADMIN_CONNECTION_ID);
  if (realDeviceIds.length > 0) {
    recordDeliveries(id, realDeviceIds);
  }

  res.status(201).json({ id, createdAt, deliveredTo: delivered.length });
});
