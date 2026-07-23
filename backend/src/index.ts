import http from "node:http";
import express from "express";
import cors from "cors";
import helmet from "helmet";
import rateLimit from "express-rate-limit";
import pinoHttp from "pino-http";
import pino from "pino";
import { config } from "./config";
import { runMigrations } from "./db";
import { healthRouter } from "./routes/health";
import { notifyRouter } from "./routes/notify";
import { notificationsRouter } from "./routes/notifications";
import { adminRouter } from "./routes/admin";
import { attachWebSocketServer } from "./ws/server";

runMigrations();

const logger = pino({ level: config.logLevel });
const app = express();

// hsts désactivé : ce serveur ne sert que du HTTP brut (pas de TLS). L'envoyer
// pousserait les navigateurs à forcer https:// sur cet hôte, ce qui casse tout
// puisqu'aucun certificat n'est servi ici.
app.use(helmet({ hsts: false }));
app.use(express.json({ limit: "50kb" }));
app.use(pinoHttp({ logger }));

const notifyLimiter = rateLimit({
  windowMs: 60_000,
  limit: 60,
  standardHeaders: true,
  legacyHeaders: false,
});

app.use(healthRouter);
app.use(
  "/api",
  cors({
    origin: "*",
    methods: ["GET", "POST"],
    allowedHeaders: ["Content-Type", "X-API-Key"],
  }),
  helmet.crossOriginResourcePolicy({ policy: "cross-origin" })
);
app.use("/api/notify", notifyLimiter);
app.use(notifyRouter);
app.use(notificationsRouter);
app.use(adminRouter);

const server = http.createServer(app);
attachWebSocketServer(server);

server.listen(config.port, () => {
  logger.info(`notifier backend listening on port ${config.port}`);
});
