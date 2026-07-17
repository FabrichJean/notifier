import crypto from "node:crypto";
import { db, runMigrations } from "../db";
import { generateSecret, hashSecret } from "../auth/hash";

function main(): void {
  const nameArgIndex = process.argv.indexOf("--name");
  const name = nameArgIndex !== -1 ? process.argv[nameArgIndex + 1] : undefined;

  if (!name) {
    console.error("Usage: npm run create-api-key -- --name \"mon-appli\"");
    process.exit(1);
  }

  runMigrations();

  const id = crypto.randomUUID();
  const secret = generateSecret("nk");
  const keyHash = hashSecret(secret);

  db.prepare("INSERT INTO api_keys (id, name, key_hash) VALUES (?, ?, ?)").run(
    id,
    name,
    keyHash
  );

  console.log("Clé API créée pour:", name);
  console.log("ID:", id);
  console.log("Clé (à copier maintenant, non récupérable ensuite):");
  console.log(secret);
}

main();
