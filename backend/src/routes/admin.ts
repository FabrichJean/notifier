import crypto from "node:crypto";
import express, { Router } from "express";
import rateLimit from "express-rate-limit";
import { db } from "../db";
import { generateSecret, hashSecret } from "../auth/hash";
import { ADMIN_COOKIE_NAME, isValidAdminToken, requireAdminApi, requireAdminPage } from "../auth/adminAuth";

export const adminRouter = Router();

const loginLimiter = rateLimit({
  windowMs: 15 * 60_000,
  limit: 10,
  standardHeaders: true,
  legacyHeaders: false,
});

const pageShell = (title: string, body: string) => `<!doctype html>
<html lang="fr">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${title}</title>
<style>
  :root { color-scheme: light dark; }
  body { font-family: system-ui, sans-serif; max-width: 720px; margin: 40px auto; padding: 0 16px; }
  h1 { font-size: 1.4rem; }
  table { width: 100%; border-collapse: collapse; margin-top: 16px; }
  th, td { text-align: left; padding: 8px 6px; border-bottom: 1px solid #8884; font-size: 0.9rem; }
  input[type=text], input[type=password] { padding: 8px; font-size: 1rem; width: 100%; box-sizing: border-box; }
  button { padding: 8px 14px; font-size: 0.95rem; cursor: pointer; }
  form.inline { display: flex; gap: 8px; margin-top: 16px; }
  form.inline input { flex: 1; }
  .error { color: #c0392b; }
  .token-banner { background: #2ecc7122; border: 1px solid #2ecc71; padding: 12px; border-radius: 6px; margin: 12px 0; word-break: break-all; }
  .revoked { opacity: 0.5; }
  .danger { background: #c0392b; color: white; border: none; border-radius: 4px; }
</style>
</head>
<body>
${body}
</body>
</html>`;

adminRouter.get("/admin/login", (req, res) => {
  const error = req.query.error ? `<p class="error">Jeton invalide.</p>` : "";
  res.send(
    pageShell(
      "Notifier — Connexion admin",
      `<h1>Connexion admin</h1>
       ${error}
       <form method="post" action="/admin/login">
         <input type="password" name="token" placeholder="Jeton admin" autofocus required>
         <button type="submit" style="margin-top:8px;">Se connecter</button>
       </form>`
    )
  );
});

adminRouter.post(
  "/admin/login",
  loginLimiter,
  express.urlencoded({ extended: false }),
  (req, res) => {
    const token = typeof req.body?.token === "string" ? req.body.token : "";
    if (!isValidAdminToken(token)) {
      res.redirect("/admin/login?error=1");
      return;
    }
    res.cookie(ADMIN_COOKIE_NAME, token, {
      httpOnly: true,
      sameSite: "lax",
      maxAge: 30 * 24 * 60 * 60 * 1000,
    });
    res.redirect("/admin");
  }
);

adminRouter.post("/admin/logout", (req, res) => {
  res.clearCookie(ADMIN_COOKIE_NAME);
  res.redirect("/admin/login");
});

adminRouter.get("/admin", requireAdminPage, (req, res) => {
  res.send(
    pageShell(
      "Notifier — Admin",
      `<h1>Devices</h1>
       <div id="banner"></div>
       <table>
         <thead><tr><th>Nom</th><th>Créé le</th><th>Statut</th><th></th></tr></thead>
         <tbody id="rows"><tr><td colspan="4">Chargement…</td></tr></tbody>
       </table>
       <form class="inline" id="createForm">
         <input type="text" id="newName" placeholder="Nom du nouveau device" required>
         <button type="submit">Créer</button>
       </form>
       <form method="post" action="/admin/logout" style="margin-top:24px;">
         <button type="submit">Se déconnecter</button>
       </form>
       <script src="/admin/admin.js"></script>`
    )
  );
});

// Served as a separate same-origin script (not inline) because Helmet's default
// Content-Security-Policy (script-src 'self') blocks inline <script> tags.
adminRouter.get("/admin/admin.js", requireAdminPage, (req, res) => {
  res.type("application/javascript").send(`
async function loadDevices() {
  const res = await fetch('/admin/api/devices');
  const data = await res.json();
  const rows = document.getElementById('rows');
  if (data.devices.length === 0) {
    rows.innerHTML = '<tr><td colspan="4">Aucun device.</td></tr>';
    return;
  }
  rows.innerHTML = data.devices.map(function(d) {
    var status = d.revokedAt ? 'Révoqué' : 'Actif';
    var rowClass = d.revokedAt ? 'revoked' : '';
    var action = d.revokedAt ?
      '<button data-id="' + d.id + '" class="reactivate">Réactiver</button>' :
      '<button data-id="' + d.id + '" class="danger revoke">Révoquer</button>';
    return '<tr class="' + rowClass + '"><td>' + escapeHtml(d.name) + '</td><td>' +
      new Date(d.createdAt).toLocaleString() + '</td><td>' + status + '</td><td>' + action + '</td></tr>';
  }).join('');
  document.querySelectorAll('.revoke').forEach(function(btn) {
    btn.addEventListener('click', function() { revokeDevice(btn.dataset.id); });
  });
  document.querySelectorAll('.reactivate').forEach(function(btn) {
    btn.addEventListener('click', function() { reactivateDevice(btn.dataset.id); });
  });
}

function escapeHtml(s) {
  return s.replace(/[&<>"']/g, function(c) {
    return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
  });
}

async function revokeDevice(id) {
  if (!confirm('Révoquer ce device ? Il ne pourra plus se connecter.')) return;
  await fetch('/admin/api/devices/' + id + '/revoke', { method: 'POST' });
  loadDevices();
}

async function reactivateDevice(id) {
  await fetch('/admin/api/devices/' + id + '/reactivate', { method: 'POST' });
  loadDevices();
}

document.getElementById('createForm').addEventListener('submit', async function(e) {
  e.preventDefault();
  var name = document.getElementById('newName').value.trim();
  if (!name) return;
  var res = await fetch('/admin/api/devices', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name: name }),
  });
  var data = await res.json();
  document.getElementById('banner').innerHTML =
    '<div class="token-banner"><strong>Device "' + escapeHtml(data.name) + '" créé.</strong><br>' +
    'Token (à copier maintenant, non récupérable ensuite) :<br><code>' + data.token + '</code></div>';
  document.getElementById('newName').value = '';
  loadDevices();
});

loadDevices();
`);
});

adminRouter.get("/admin/api/devices", requireAdminApi, (req, res) => {
  const rows = db
    .prepare("SELECT id, name, created_at, revoked_at FROM devices ORDER BY created_at DESC")
    .all() as { id: string; name: string; created_at: string; revoked_at: string | null }[];

  res.json({
    devices: rows.map((r) => ({
      id: r.id,
      name: r.name,
      createdAt: r.created_at,
      revokedAt: r.revoked_at,
    })),
  });
});

adminRouter.post(
  "/admin/api/devices",
  requireAdminApi,
  (req, res) => {
    const name = typeof req.body?.name === "string" ? req.body.name.trim() : "";
    if (!name) {
      res.status(400).json({ error: "name is required" });
      return;
    }

    const id = crypto.randomUUID();
    const secret = generateSecret("dv");
    const tokenHash = hashSecret(secret);

    db.prepare("INSERT INTO devices (id, name, token_hash) VALUES (?, ?, ?)").run(
      id,
      name,
      tokenHash
    );

    res.status(201).json({ id, name, token: secret });
  }
);

adminRouter.post("/admin/api/devices/:id/revoke", requireAdminApi, (req, res) => {
  const result = db
    .prepare("UPDATE devices SET revoked_at = strftime('%Y-%m-%dT%H:%M:%fZ', 'now') WHERE id = ? AND revoked_at IS NULL")
    .run(req.params.id);

  if (result.changes === 0) {
    res.status(404).json({ error: "device not found or already revoked" });
    return;
  }

  res.json({ ok: true });
});

adminRouter.post("/admin/api/devices/:id/reactivate", requireAdminApi, (req, res) => {
  const result = db
    .prepare("UPDATE devices SET revoked_at = NULL WHERE id = ? AND revoked_at IS NOT NULL")
    .run(req.params.id);

  if (result.changes === 0) {
    res.status(404).json({ error: "device not found or already active" });
    return;
  }

  res.json({ ok: true });
});
