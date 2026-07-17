# Notifier

Service personnel de notification : des applications tierces envoient des demandes de notification via une API sécurisée (`backend/`), un client Android natif (`android/`) reste connecté en temps réel et affiche soit une notification simple, soit une alarme plein écran configurable selon la requête reçue.

## Composants

- [`backend/`](backend/) — API Node.js/TypeScript + WebSocket + historique SQLite. Voir [backend/README.md](backend/README.md).
  - Pour un tiers qui veut intégrer l'envoi de notifications depuis sa propre application : [backend/docs/API.md](backend/docs/API.md).
- [`android/`](android/) — client Android natif (Kotlin) qui reste connecté en WebSocket et déclenche notification ou alarme.

## Mise en route

1. **Backend**
   ```bash
   cd backend
   npm install
   cp .env.example .env
   npm run create-api-key -- --name "mon-appli"     # à donner à l'application appelante
   npm run create-device -- --name "mon-telephone"   # à entrer dans l'app Android
   npm run dev
   ```

2. **Android**
   - Ouvrir le dossier `android/` dans Android Studio (il détectera le wrapper Gradle déjà présent).
   - Lancer l'app sur un téléphone/émulateur connecté au même réseau que la machine qui fait tourner le backend.
   - Dans l'écran de config : entrer l'URL du serveur (ex: `ws://192.168.1.10:8787`) et le token device généré ci-dessus, puis appuyer sur "Connecter".
   - Autoriser les notifications quand demandé, et désactiver l'optimisation de batterie pour l'app (Réglages > Batterie) pour que le service reste connecté en arrière-plan.

3. **Tester**
   ```bash
   curl -X POST http://localhost:8787/api/notify \
     -H "Content-Type: application/json" \
     -H "X-API-Key: <clé générée>" \
     -d '{"title":"Test","body":"Ça marche","type":"notification"}'
   ```
   Pour une alarme plein écran :
   ```bash
   curl -X POST http://localhost:8787/api/notify \
     -H "Content-Type: application/json" \
     -H "X-API-Key: <clé générée>" \
     -d '{"title":"Réveil","body":"Debout !","type":"alarm","alarm":{"fullScreen":true,"loop":true,"vibrate":true,"snoozeMinutes":5}}'
   ```

## État actuel

- Backend : entièrement implémenté et vérifié de bout en bout (auth par clé API, validation, broadcast WebSocket, ack, historique, rate limiting).
- Android : projet complet (service de connexion, dispatcher notification/alarme, écran plein écran avec dismiss/snooze, configuration), compile et build avec succès (`./gradlew assembleDebug`). Le test réel sur téléphone (son, vibration, écran verrouillé) reste à faire manuellement, cet environnement ne pouvant pas piloter un appareil Android.

## Limites connues

- Pas de FCM/APNs : le client dépend d'un WebSocket persistant maintenu par un foreground service. Certains fabricants (Xiaomi, Huawei, OnePlus...) tuent agressivement les apps en arrière-plan malgré le foreground service — désactiver l'optimisation de batterie pour l'app si des notifications sont manquées.
- Déploiement local uniquement pour l'instant : pour exposer le service sur internet, mettre un reverse proxy TLS (Caddy/nginx) devant le backend.
