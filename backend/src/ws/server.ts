import type { Server as HttpServer } from "node:http";
import { URL } from "node:url";
import { WebSocketServer, type WebSocket } from "ws";
import { ADMIN_CONNECTION_ID, isValidAdminToken } from "../auth/adminAuth";
import { authenticateDeviceToken, touchDeviceLastSeen } from "../auth/deviceAuth";
import { ackDelivery, heartbeatSweep, markAlive, registerDevice, unregisterDevice } from "./hub";

const HEARTBEAT_INTERVAL_MS = 30_000;

export function attachWebSocketServer(httpServer: HttpServer): void {
  const wss = new WebSocketServer({ noServer: true });

  httpServer.on("upgrade", (request, socket, head) => {
    if (!request.url || !request.url.startsWith("/ws")) {
      socket.destroy();
      return;
    }

    const url = new URL(request.url, "http://localhost");
    const rawToken = url.searchParams.get("token") ?? request.headers["x-device-token"];
    const token = Array.isArray(rawToken) ? rawToken[0] : (rawToken as string | null);

    const device = token && isValidAdminToken(token)
      ? { id: ADMIN_CONNECTION_ID, name: "Admin (tous les appareils)" }
      : authenticateDeviceToken(token);

    if (!device) {
      socket.write("HTTP/1.1 401 Unauthorized\r\n\r\n");
      socket.destroy();
      return;
    }

    wss.handleUpgrade(request, socket, head, (ws) => {
      wss.emit("connection", ws, device);
    });
  });

  wss.on("connection", (ws: WebSocket, device: { id: string; name: string }) => {
    registerDevice(device.id, ws);
    touchDeviceLastSeen(device.id);

    ws.on("pong", () => markAlive(device.id));

    ws.on("message", (raw) => {
      try {
        const msg = JSON.parse(raw.toString());
        if (msg.event === "ack" && typeof msg.id === "string") {
          ackDelivery(msg.id, device.id);
        }
      } catch {
        // ignore malformed client messages
      }
    });

    ws.on("close", () => unregisterDevice(device.id, ws));
  });

  setInterval(heartbeatSweep, HEARTBEAT_INTERVAL_MS);
}
