// Importa y publica la version del workflow del chatbot en n8n 2.x.
//
// En n8n 2.x un workflow queda operativo solo si tiene una version publicada
// (workflow_published_version) apuntada por activeVersionId. Marcar
// active=1 en workflow_entity no basta y el webhook sigue devolviendo 404.
//
// Uso:
//   node publish-n8n-workflow.js [--check] [ruta/base/n8n/database.sqlite]
//
// Con --check solo verifica si el workflow ya esta importado y publicado:
// devuelve codigo 0 si lo esta, 1 si falta importarlo o publicarlo.
const { DatabaseSync } = require("node:sqlite");

const args = process.argv.slice(2);
const checkOnly = args.includes("--check");
const dbFile = args.find((a) => !a.startsWith("--")) || "/home/node/.n8n/database.sqlite";

const db = new DatabaseSync(dbFile);
const workflows = db.prepare("select id, name, active, activeVersionId from workflow_entity").all();
const published = db
  .prepare("select workflowId, publishedVersionId from workflow_published_version")
  .all();
const history = db.prepare("select versionId, workflowId from workflow_history").all();

const operativo = workflows.some((wf) => {
  const pub = published.find((p) => p.workflowId === wf.id);
  return wf.active === 1 && pub && wf.activeVersionId === pub.publishedVersionId;
});

if (checkOnly) {
  console.log(operativo ? "publicado" : "pendiente");
  process.exit(operativo ? 0 : 1);
}

if (workflows.length === 0) {
  console.log("no hay workflows importados");
  process.exit(0);
}

for (const wf of workflows) {
  const version = history.find((h) => h.workflowId === wf.id);
  if (!version) {
    console.log("sin version para " + wf.name + "; se omite");
    continue;
  }
  db.prepare(
    "insert or ignore into workflow_published_version (workflowId, publishedVersionId, createdAt, updatedAt) values (?, ?, datetime('now'), datetime('now'))"
  ).run(wf.id, version.versionId);
  db.prepare("update workflow_entity set active = 1, activeVersionId = ? where id = ?").run(
    version.versionId,
    wf.id
  );
  console.log("publicado: " + wf.name + " version=" + version.versionId);
}
