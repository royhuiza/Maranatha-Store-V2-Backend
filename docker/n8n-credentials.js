// Registra en n8n las credenciales que usa el workflow del chatbot.
//
// El workflow "My workflow.json" referencia las credenciales por ID fijo
// (aMj6l1dKcgtt0PSp para Google Gemini y sQWVIoDhhSOTtwS para Postgres).
// En una instalacion nueva esas credenciales no existen, el nodo "Google Gemini
// Chat Model" falla con "Error in sub-node" y el chat responde el mensaje de
// fallback en vez de la respuesta del modelo.
//
// Este script las crea si faltan, cifradas con el mismo algoritmo que usa n8n
// (AES-256-GCM + HKDF, ver n8n-cipher.js), para que la UI las muestre y las
// use sin pedir nada al usuario.
//
// Es idempotente: si ya existen no hace nada.
const fs = require("fs");
const { DatabaseSync } = require("node:sqlite");

const { encrypt, dekWrappingKey } = require("./n8n-cipher.js");

const DB_FILE = process.argv[2] || "/home/node/.n8n/database.sqlite";
const CONFIG_FILE = process.argv[3] || "/home/node/.n8n/config";
const GEMINI_KEY = process.env.GEMINI_API_KEY || "";

const encryptionKey = JSON.parse(fs.readFileSync(CONFIG_FILE, "utf8")).encryptionKey;
const key = dekWrappingKey(encryptionKey);

const db = new DatabaseSync(DB_FILE);

const GEMINI_ID = "aMj6l1dKcgtt0PSp";
const POSTGRES_ID = "sQWVIoDhhSOTtwS";

const existing = db.prepare("select id, name from credentials_entity").all();
const known = new Set(existing.map((c) => c.id));

function upsertCredential(id, name, type, plain) {
  if (known.has(id)) {
    console.log("  ya existe: " + name);
    return;
  }
  db.prepare(
    "insert into credentials_entity (id, name, data, type, createdAt, updatedAt) values (?, ?, ?, ?, datetime('now'), datetime('now'))"
  ).run(id, name, encrypt(JSON.stringify(plain), key), type);
  known.add(id);
  console.log("  creada: " + name + " (" + type + ")");
}

console.log("credenciales existentes: " + existing.length);

upsertCredential(GEMINI_ID, "Google Gemini(PaLM) Api account 2", "googlePalmApi", {
  apiKey: GEMINI_KEY,
});

upsertCredential(POSTGRES_ID, "Postgres account", "postgres", {
  host: process.env.POSTGRES_HOST || "maranatha-postgres",
  port: Number(process.env.POSTGRES_PORT || 5432),
  database: process.env.POSTGRES_DATABASE || "postgres",
  user: process.env.POSTGRES_USER || "postgres",
  password: process.env.POSTGRES_PASSWORD || "admin123",
  ssl: false,
  allowUnauthorizedCerts: false,
  sshTunnel: false,
});

// Vincular las credenciales al proyecto para que aparezcan en la UI de n8n.
try {
  const project = db.prepare("select id from project limit 1").all()[0];
  const pid = project ? project.id : null;
  if (pid) {
    const cols = db.prepare("PRAGMA table_info(shared_credentials)").all().map((c) => c.name);
    const hasRole = cols.includes("role");
    for (const id of [GEMINI_ID, POSTGRES_ID]) {
      const rel = db
        .prepare("select count(*) as n from shared_credentials where credentialsId = ? and projectId = ?")
        .get(id, pid);
      if (!rel || rel.n === 0) {
        if (hasRole) {
          db.prepare(
            "insert into shared_credentials (credentialsId, projectId, role) values (?, ?, ?)"
          ).run(id, pid, "project:owner");
        } else {
          db.prepare("insert into shared_credentials (credentialsId, projectId) values (?, ?)").run(id, pid);
        }
      }
    }
    console.log("  vinculadas al proyecto " + pid);
  }
} catch (e) {
  console.log("  aviso: no se pudieron vincular al proyecto: " + e.message);
}

if (!GEMINI_KEY) {
  console.log(
    "\nAVISO: falta GEMINI_API_KEY. La credencial se creo vacia; el nodo seguira\n" +
      "fallando hasta que pegues la clave en la UI de n8n o la exportes."
  );
} else {
  console.log("\ncredencial de Gemini creada con GEMINI_API_KEY.");
}
