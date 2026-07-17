import crypto from "node:crypto";

export function generateSecret(prefix: string): string {
  return `${prefix}_${crypto.randomBytes(24).toString("base64url")}`;
}

export function hashSecret(secret: string): string {
  return crypto.createHash("sha256").update(secret).digest("hex");
}
