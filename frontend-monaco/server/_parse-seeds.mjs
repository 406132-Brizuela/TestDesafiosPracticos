const fs = require('fs');

// --- 1) Leer index.mjs y extraer SEED_CHALLENGES de forma robusta ---
const src = fs.readFileSync('server/index.mjs', 'utf8');
const startMarker = 'const SEED_CHALLENGES = [';
const start = src.indexOf(startMarker);
if (start === -1) throw new Error('no encuentro SEED_CHALLENGES');
let i = start + startMarker.length - 1; // apuntar al '['
let depth = 0;
let mode = 'code'; // code | template | tplExpr | str
const tplStack = [];
const exprStack = [];
let depthCode = 0;
// recorremos desde el '[' hasta cerrarlo, saltando strings/templates/comments
let j = i;
let openIndex = -1;
const N = src.length;
let state = 'code';
let bracket = 0;
// re-hacer limpio: parsear [ ... ] con awareness de template literals
let k = i哪有;
// Lo hago con un escaneo char a char:
let pos = i;
let inTag;
let totalOpen = 0;
let lastOpen = -1;
const stack = [];
for (let p = i; p < N; p++) {
  const ch = src[p];
}
// (placeholder no llegue a escribir logica aca)
console.log('tool de este script se reemplaza por materialize-seeds.mjs');
