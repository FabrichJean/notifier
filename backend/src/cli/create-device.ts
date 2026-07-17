import crypto from "node:crypto";
import { db, runMigrations } from "../db";
import { generateSecret, hashSecret } from "../auth/hash";

function main(): void {
  const nameArgIndex = process.argv.indexOf("--name");
  const name = nameArgIndex !== -1 ? process.argv[nameArgIndex + 1] : undefined;

  if (!name) {
    console.error("Usage: npm run create-device -- --name \"mon-telephone\"");
    process.exit(1);
  }

  runMigrations();

  const id = crypto.randomUUID();
  const secret = generateSecret("dv");
  const tokenHash = hashSecret(secret);

  db.prepare("INSERT INTO devices (id, name, token_hash) VALUES (?, ?, ?)").run(
    id,
    name,
    tokenHash
  );

  console.log("Device créé:", name);
  console.log("ID:", id);
  console.log("Token (à copier maintenant, non récupérable ensuite):");
  console.log(secret);
}

main();
