# API Notifier — guide d'intégration

Ce document explique comment envoyer des notifications vers l'application Notifier depuis votre propre application ou service.

## 1. Obtenir une clé API

Vous avez besoin d'une clé API pour appeler ce service. Elle est générée par l'administrateur du serveur Notifier (pas par vous) :

```bash
npm run create-api-key -- --name "nom-de-votre-application"
```

La clé ressemble à `nk_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`. **Elle n'est affichée qu'une seule fois** — demandez-la à l'administrateur si vous ne l'avez pas notée.

Toutes les requêtes doivent inclure cette clé dans l'en-tête `X-API-Key`.

## 2. Envoyer une notification

```
POST {BASE_URL}/api/notify
Content-Type: application/json
X-API-Key: <votre clé API>
```

`{BASE_URL}` est l'adresse du serveur Notifier (fournie par l'administrateur), par exemple `http://192.168.1.191:8787`.

### Corps de la requête

| Champ      | Type                       | Requis | Description |
|------------|----------------------------|--------|-------------|
| `title`    | string (1–200 caractères)  | oui    | Titre affiché dans la notification |
| `body`     | string (1–2000 caractères) | oui    | Contenu du message |
| `type`     | `"notification"` \| `"alarm"` | oui | Type d'alerte (voir ci-dessous) |
| `alarm`    | objet (voir plus bas)      | non    | Uniquement si `type` = `"alarm"` |
| `metadata` | objet libre (clé/valeur)   | non    | Données additionnelles, transmises telles quelles, non affichées |
| `deviceId` | string                     | non    | Cible un seul appareil (voir section ci-dessous). Omis = diffusion à tous les appareils connectés |

### Cibler un appareil précis

Par défaut (sans `deviceId`), la notification est diffusée à **tous** les appareils connectés. Pour ne toucher qu'un seul appareil, indiquez son `deviceId` (l'ID renvoyé à sa création, pas son nom) :

```json
{
  "title": "Colis livré",
  "body": "Ton colis est arrivé",
  "type": "notification",
  "deviceId": "91588a81-ff31-4a05-84aa-d014384bd114"
}
```

Si `deviceId` ne correspond à aucun appareil, la requête échoue avec `404`. Si l'appareil a été révoqué, elle échoue avec `400`. Un compte admin connecté en mode observateur (voir README du backend) reçoit toujours la notification, qu'elle soit ciblée ou diffusée à tous.

### Type `notification`

Notification standard affichée dans le tiroir de notifications du téléphone.

```json
{
  "title": "Nouvelle commande",
  "body": "La commande #4821 vient d'être passée",
  "type": "notification"
}
```

### Type `alarm`

Déclenche une alerte prioritaire : si l'écran du téléphone est verrouillé ou éteint, l'app s'ouvre en plein écran par-dessus le verrouillage, avec son et vibration en boucle jusqu'à ce que l'utilisateur appuie sur "Arrêter" ou "Reporter". Si le téléphone est déjà déverrouillé et l'app au premier plan, une notification prioritaire avec ces mêmes actions s'affiche à la place.

```json
{
  "title": "Réveil",
  "body": "Il est temps de se lever !",
  "type": "alarm",
  "alarm": {
    "fullScreen": true,
    "sound": "default",
    "loop": true,
    "vibrate": true,
    "snoozeMinutes": 5
  }
}
```

Champ `alarm` (tous optionnels, valeurs par défaut indiquées) :

| Champ           | Type                                        | Défaut      | Description |
|-----------------|----------------------------------------------|-------------|-------------|
| `fullScreen`    | boolean                                      | `true`      | Lancer l'écran plein écran si le téléphone est verrouillé |
| `sound`         | `"default"` \| `"alarm_classic"` \| `"gentle"` | `"default"` | Son joué en boucle |
| `loop`          | boolean                                      | `true`      | Répéter le son jusqu'à l'arrêt manuel |
| `vibrate`       | boolean                                      | `true`      | Vibrer en boucle |
| `snoozeMinutes` | entier positif ou `null`                     | `null`      | Si renseigné, affiche un bouton "Reporter" qui redéclenche l'alarme après ce délai |

### Réponse

`201 Created` :

```json
{
  "id": "0c909480-5259-46df-a980-278eb812cd91",
  "createdAt": "2026-07-17T15:58:40.558Z",
  "deliveredTo": 1
}
```

`deliveredTo` indique le nombre d'appareils actuellement connectés qui ont reçu la notification en temps réel. Si `0`, aucun appareil n'était en ligne au moment de l'envoi — la notification reste tout de même dans l'historique (voir section 4) mais n'est pas retransmise rétroactivement.

### Erreurs possibles

| Code | Cause |
|------|-------|
| `400` | Corps de requête invalide (champ manquant, type incorrect, texte trop long...), ou `deviceId` correspondant à un appareil révoqué. Le détail est dans `details`. |
| `401` | En-tête `X-API-Key` manquant, invalide ou révoqué. |
| `404` | `deviceId` fourni ne correspond à aucun appareil. |
| `429` | Trop de requêtes : limite de 60 requêtes par minute par serveur. |

## 3. Exemples

### cURL

```bash
curl -X POST http://192.168.1.191:8787/api/notify \
  -H "Content-Type: application/json" \
  -H "X-API-Key: nk_votre_cle" \
  -d '{"title":"Alerte","body":"Nouvel evenement detecte","type":"notification"}'
```

### JavaScript / Node.js

```javascript
await fetch("http://192.168.1.191:8787/api/notify", {
  method: "POST",
  headers: {
    "Content-Type": "application/json",
    "X-API-Key": "nk_votre_cle",
  },
  body: JSON.stringify({
    title: "Alerte",
    body: "Quelque chose s'est passé",
    type: "notification",
  }),
});
```

### Python

```python
import requests

requests.post(
    "http://192.168.1.191:8787/api/notify",
    headers={"X-API-Key": "nk_votre_cle"},
    json={"title": "Alerte", "body": "Quelque chose s'est passé", "type": "notification"},
)
```

## 4. Consulter l'historique (optionnel)

```
GET {BASE_URL}/api/notifications?limit=50&before=<date ISO>
X-API-Key: <votre clé API>
```

Retourne les notifications envoyées (toutes clés API confondues), les plus récentes en premier, avec le statut de livraison par appareil (`deliveredAt`, `ackedAt`). Utilisez `before` (date ISO 8601) avec la valeur `nextBefore` de la réponse précédente pour paginer.

## 5. Limites à connaître

- Un seul serveur local, pas de TLS par défaut : n'envoyez pas ce service sur internet sans mettre un reverse proxy HTTPS devant.
- La notification n'est délivrée qu'aux appareils connectés au moment de l'envoi (pas de file d'attente pour les appareils hors ligne).
- Limite de débit : 60 requêtes/minute sur `/api/notify`.
