# Notifier backend

API de notification personnelle : les applications appelantes envoient une requête `POST /api/notify` avec une clé API, le serveur diffuse en temps réel (WebSocket) vers les appareils connectés et garde un historique en SQLite.

## Installation

```bash
npm install
cp .env.example .env
```

## Créer une clé API (pour une application appelante)

```bash
npm run create-api-key -- --name "mon-appli"
```

La clé (`nk_...`) s'affiche une seule fois — à copier dans la config de l'application appelante (header `X-API-Key`).

## Créer un device (pour le téléphone Android)

```bash
npm run create-device -- --name "mon-telephone"
```

Le token (`dv_...`) s'affiche une seule fois — à copier dans les réglages de l'app Android (utilisé pour la connexion WebSocket).

## Interface admin (gérer les devices depuis un navigateur)

Alternative à la ligne de commande pour créer/révoquer des tokens device.

1. Définir `ADMIN_TOKEN` dans `.env` (voir `.env.example` pour générer une valeur aléatoire), puis redémarrer le serveur.
2. Ouvrir `http://<host>:<port>/admin` dans un navigateur.
3. Entrer le jeton admin pour se connecter (session conservée 30 jours via cookie).

Si `ADMIN_TOKEN` n'est pas défini, l'interface admin renvoie `503` (désactivée par défaut).

## Lancer le serveur

```bash
npm run dev
```

## API

### `POST /api/notify`

Header : `X-API-Key: <clé>`

```json
{
  "title": "Titre",
  "body": "Message",
  "type": "notification",
  "alarm": {
    "fullScreen": true,
    "sound": "default",
    "loop": true,
    "vibrate": true,
    "snoozeMinutes": 5
  },
  "metadata": {}
}
```

`type` est `"notification"` (simple) ou `"alarm"`. Le bloc `alarm` n'est utilisé que si `type` vaut `"alarm"`, tous ses champs sont optionnels (valeurs par défaut appliquées côté serveur).

Réponse `201` : `{ "id", "createdAt", "deliveredTo" }` — `deliveredTo` est le nombre d'appareils connectés au moment de l'envoi (la notification reste dans l'historique même si 0).

### `GET /api/notifications?limit=&before=`

Header : `X-API-Key: <clé>`. Historique paginé avec le statut de livraison/ack par appareil.

### WebSocket `ws://<host>:<port>/ws?token=<token device>`

Le serveur pousse `{ "event": "notification", "data": {...} }` à chaque nouvelle notification. Le client doit répondre `{ "event": "ack", "id": "<id>" }` pour marquer la notification comme lue.

## Sécurité

- Clés API et tokens device générés uniquement via les scripts CLI ci-dessus, jamais stockés en clair (hash SHA-256 en base).
- `helmet` pour les en-têtes HTTP, `express-rate-limit` sur `/api/notify` (60 req/min).
- Pensé pour un usage local pour l'instant. Pour exposer ce service sur internet, mettre un reverse proxy TLS (Caddy/nginx) devant — ne pas l'exposer directement en HTTP.
