import { db } from "../db";
import { hashSecret } from "./hash";

export interface DeviceRecord {
  id: string;
  name: string;
}

export function authenticateDeviceToken(token: string | null): DeviceRecord | null {
  if (!token) return null;

  const tokenHash = hashSecret(token);
  const row = db
    .prepare(
      "SELECT id, name FROM devices WHERE token_hash = ? AND revoked_at IS NULL"
    )
    .get(tokenHash) as DeviceRecord | undefined;

  return row ?? null;
}

export function touchDeviceLastSeen(deviceId: string): void {
  db.prepare(
    "UPDATE devices SET last_seen_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE id = ?"
  ).run(deviceId);
}
