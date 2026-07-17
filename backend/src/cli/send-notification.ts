import { config } from "../config";

function parseArgs() {
  const args = process.argv.slice(2);
  const get = (flag: string): string | undefined => {
    const i = args.indexOf(flag);
    return i !== -1 ? args[i + 1] : undefined;
  };
  const has = (flag: string): boolean => args.includes(flag);
  return { get, has };
}

function usageAndExit(): never {
  console.error(
    `Usage: npm run send-notification -- --api-key <clé> --title "..." --body "..." [options]

Options:
  --type notification|alarm   (défaut: notification)
  --url http://host:port      (défaut: http://localhost:${config.port})
  --sound default|alarm_classic|gentle   (alarme uniquement, défaut: default)
  --snooze <minutes>          (alarme uniquement, active le bouton "Reporter")
  --no-loop                   (alarme uniquement, ne pas boucler le son)
  --no-vibrate                (alarme uniquement, désactive la vibration)
  --no-fullscreen             (alarme uniquement, pas d'écran plein écran verrouillé)

La clé API peut aussi être fournie via la variable d'environnement API_KEY.`
  );
  process.exit(1);
}

async function main(): Promise<void> {
  const { get, has } = parseArgs();

  const apiKey = get("--api-key") ?? process.env.API_KEY;
  const title = get("--title");
  const body = get("--body");
  const type = get("--type") ?? "notification";
  const baseUrl = get("--url") ?? `http://localhost:${config.port}`;

  if (!apiKey || !title || !body) usageAndExit();
  if (type !== "notification" && type !== "alarm") usageAndExit();

  const payload: Record<string, unknown> = { title, body, type };

  if (type === "alarm") {
    const snooze = get("--snooze");
    payload.alarm = {
      fullScreen: !has("--no-fullscreen"),
      loop: !has("--no-loop"),
      vibrate: !has("--no-vibrate"),
      sound: get("--sound") ?? "default",
      snoozeMinutes: snooze ? Number(snooze) : null,
    };
  }

  const res = await fetch(`${baseUrl}/api/notify`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-API-Key": apiKey,
    },
    body: JSON.stringify(payload),
  });

  const data = await res.json().catch(() => null);

  if (!res.ok) {
    console.error(`Échec (${res.status}):`, data);
    process.exit(1);
  }

  console.log("Notification envoyée:", data);
}

main().catch((err) => {
  console.error("Erreur:", err.message ?? err);
  process.exit(1);
});
