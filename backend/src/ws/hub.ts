import crypto from "node:crypto";
import type { WebSocket } from "ws";
import { db } from "../db";
import type { DeviceEnvelope } from "../types";

interface ConnectedDevice {
  deviceId: string;
  socket: WebSocket;
  isAlive: boolean;
}

const connections = new Map<string, ConnectedDevice>();

export function registerDevice(deviceId: string, socket: WebSocket): void {
  const existing = connections.get(deviceId);
  if (existing) {
    existing.socket.terminate();
  }
  connections.set(deviceId, { deviceId, socket, isAlive: true });
}

export function unregisterDevice(deviceId: string, socket: WebSocket): void {
  const existing = connections.get(deviceId);
  if (existing && existing.socket === socket) {
    connections.delete(deviceId);
  }
}

export function markAlive(deviceId: string): void {
  const existing = connections.get(deviceId);
  if (existing) existing.isAlive = true;
}

export function connectedDeviceCount(): number {
  return connections.size;
}

export function broadcast(envelope: DeviceEnvelope): { deviceId: string }[] {
  const delivered: { deviceId: string }[] = [];
  const payload = JSON.stringify(envelope);

  for (const { deviceId, socket } of connections.values()) {
    if (socket.readyState === socket.OPEN) {
      socket.send(payload);
      delivered.push({ deviceId });
    }
  }

  return delivered;
}

export function recordDeliveries(notificationId: string, deviceIds: string[]): void {
  const insert = db.prepare(
    "INSERT INTO deliveries (id, notification_id, device_id, delivered_at) VALUES (?, ?, ?, strftime('%Y-%m-%dT%H:%M:%fZ', 'now'))"
  );
  const insertMany = db.transaction((ids: string[]) => {
    for (const deviceId of ids) {
      insert.run(crypto.randomUUID(), notificationId, deviceId);
    }
  });
  insertMany(deviceIds);
}

export function ackDelivery(notificationId: string, deviceId: string): void {
  db.prepare(
    "UPDATE deliveries SET acked_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE notification_id = ? AND device_id = ?"
  ).run(notificationId, deviceId);
}

export function heartbeatSweep(): void {
  for (const { deviceId, socket, isAlive } of connections.values()) {
    if (!isAlive) {
      socket.terminate();
      connections.delete(deviceId);
      continue;
    }
    const entry = connections.get(deviceId);
    if (entry) entry.isAlive = false;
    socket.ping();
  }
}
