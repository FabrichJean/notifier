import { Router } from "express";
import { apiKeyAuth } from "../auth/apiKey";
import { db } from "../db";

export const notificationsRouter = Router();

interface NotificationRow {
  id: string;
  source_app_name: string;
  title: string;
  body: string;
  type: string;
  alarm_config: string | null;
  metadata: string | null;
  target_device_id: string | null;
  target_device_name: string | null;
  created_at: string;
}

interface DeliveryRow {
  device_id: string;
  device_name: string;
  delivered_at: string | null;
  acked_at: string | null;
}

notificationsRouter.get("/api/notifications", apiKeyAuth, (req, res) => {
  const limit = Math.min(Number(req.query.limit) || 50, 200);
  const before = typeof req.query.before === "string" ? req.query.before : null;

  const baseQuery = `SELECT n.id, n.source_app_name, n.title, n.body, n.type, n.alarm_config, n.metadata,
      n.target_device_id, td.name as target_device_name, n.created_at
     FROM notifications n
     LEFT JOIN devices td ON td.id = n.target_device_id`;

  const rows = (
    before
      ? db
          .prepare(`${baseQuery} WHERE n.created_at < ? ORDER BY n.created_at DESC LIMIT ?`)
          .all(before, limit)
      : db
          .prepare(`${baseQuery} ORDER BY n.created_at DESC LIMIT ?`)
          .all(limit)
  ) as NotificationRow[];

  const deliveryStmt = db.prepare(
    `SELECT d.device_id as device_id, dev.name as device_name, d.delivered_at as delivered_at, d.acked_at as acked_at
     FROM deliveries d JOIN devices dev ON dev.id = d.device_id
     WHERE d.notification_id = ?`
  );

  const results = rows.map((row) => ({
    id: row.id,
    source: row.source_app_name,
    title: row.title,
    body: row.body,
    type: row.type,
    alarm: row.alarm_config ? JSON.parse(row.alarm_config) : null,
    metadata: row.metadata ? JSON.parse(row.metadata) : null,
    targetDeviceId: row.target_device_id,
    targetDeviceName: row.target_device_name,
    createdAt: row.created_at,
    deliveries: (deliveryStmt.all(row.id) as DeliveryRow[]).map((d) => ({
      deviceId: d.device_id,
      deviceName: d.device_name,
      deliveredAt: d.delivered_at,
      ackedAt: d.acked_at,
    })),
  }));

  res.json({ notifications: results, nextBefore: rows.length === limit ? rows[rows.length - 1].created_at : null });
});
