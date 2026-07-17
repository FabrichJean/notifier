import "dotenv/config";
import path from "node:path";

export const config = {
  port: Number(process.env.PORT ?? 8787),
  dbPath: path.resolve(process.cwd(), process.env.DB_PATH ?? "./data/notifier.db"),
  logLevel: process.env.LOG_LEVEL ?? "info",
  adminToken: process.env.ADMIN_TOKEN || null,
};
