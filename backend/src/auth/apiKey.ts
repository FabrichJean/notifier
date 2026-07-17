import type { NextFunction, Request, Response } from "express";
import { db } from "../db";
import { hashSecret } from "./hash";

export interface ApiKeyRecord {
  id: string;
  name: string;
}

declare module "express-serve-static-core" {
  interface Request {
    apiKey?: ApiKeyRecord;
  }
}

export function apiKeyAuth(req: Request, res: Response, next: NextFunction): void {
  const key = req.header("X-API-Key");
  if (!key) {
    res.status(401).json({ error: "missing X-API-Key header" });
    return;
  }

  const keyHash = hashSecret(key);
  const row = db
    .prepare(
      "SELECT id, name FROM api_keys WHERE key_hash = ? AND revoked_at IS NULL"
    )
    .get(keyHash) as ApiKeyRecord | undefined;

  if (!row) {
    res.status(401).json({ error: "invalid or revoked API key" });
    return;
  }

  req.apiKey = row;
  next();
}
