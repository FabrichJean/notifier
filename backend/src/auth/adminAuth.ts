import crypto from "node:crypto";
import type { NextFunction, Request, Response } from "express";
import { config } from "../config";

export const ADMIN_COOKIE_NAME = "notifier_admin";

/** Pseudo device id used for WS connections authenticated via the admin token
 *  instead of a per-device token. Not a real row in the devices table. */
export const ADMIN_CONNECTION_ID = "__admin__";

export function isValidAdminToken(token: string): boolean {
  if (!config.adminToken) return false;
  const provided = Buffer.from(token);
  const expected = Buffer.from(config.adminToken);
  if (provided.length !== expected.length) return false;
  return crypto.timingSafeEqual(provided, expected);
}

function readCookie(req: Request, name: string): string | undefined {
  const header = req.headers.cookie;
  if (!header) return undefined;
  const match = header
    .split(";")
    .map((c) => c.trim())
    .find((c) => c.startsWith(`${name}=`));
  return match ? decodeURIComponent(match.slice(name.length + 1)) : undefined;
}

function hasValidSession(req: Request): boolean {
  const cookie = readCookie(req, ADMIN_COOKIE_NAME);
  return cookie !== undefined && isValidAdminToken(cookie);
}

/** Protects HTML admin pages: redirects to the login form when not authenticated. */
export function requireAdminPage(req: Request, res: Response, next: NextFunction): void {
  if (!config.adminToken) {
    res.status(503).send("Interface admin désactivée : ADMIN_TOKEN n'est pas configuré.");
    return;
  }
  if (!hasValidSession(req)) {
    res.redirect("/admin/login");
    return;
  }
  next();
}

/**
 * Protects the admin JSON API: returns 401 when not authenticated.
 * Accepts either the web session cookie (browser) or an `X-Admin-Token`
 * header carrying the admin token directly (non-browser clients, e.g. the
 * mobile app, which has no cookie jar tied to a login flow).
 */
export function requireAdminApi(req: Request, res: Response, next: NextFunction): void {
  if (!config.adminToken) {
    res.status(503).json({ error: "admin interface disabled: ADMIN_TOKEN not configured" });
    return;
  }

  const headerToken = req.header("X-Admin-Token");
  if (headerToken && isValidAdminToken(headerToken)) {
    next();
    return;
  }

  if (!hasValidSession(req)) {
    res.status(401).json({ error: "unauthorized" });
    return;
  }
  next();
}
