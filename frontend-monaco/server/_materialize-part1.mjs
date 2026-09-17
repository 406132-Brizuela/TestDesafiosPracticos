import fs from 'node:fs';

const path = 'server/index.mjs';
const src = fs.readFileSync(path, 'utf8');

// Extraer el literal de SEED_CHALLENGES con un tokenizer que entiende:
// strings '..' "..* ", template literals `..${..}..`, comentarios // y /* */,
// y delimitadores [] {} ( ). Devuelve el array JS tal cual (sin evaluar).
function extractArrayLiteral(src, marker) {
  let i = src.indexOf(marker);
  if (i < 0) throw new Error('marker no encontrado: ' + marker);
  i += marker.length;
  const open = src.indexOf('[', i);
  if (open < 0) throw new Error('no hay [ luego del marker');

  let depth = 0;
  let mode = 'code'; // code | single | double | template | tplExpr | lineCom | blockCom
  let started = false;
  for (let j = open; j < src.length; j++) {
    const ch = src[j];
    const nx = src[j + 1];

    if (mode === 'single') {
      if (ch === '\\') { j++; }
      else if (ch === "'") mode = 'code';
    } else if (mode === 'double') {
      if (ch === '\\') { j++; }
      else if (ch === '"') mode = 'code';
    } else if (mode === 'template') {
      if (ch === '\\') { j++; }
      else if (ch === '`') mode = 'code';
      else if (ch === '$' && nx === '{') { j++; depth++; mode = 'tplExpr'; }
    } else if (mode === 'tplExpr') {
      if (ch === '\\') { /* noop */ }
      else if (ch === "'") mode = 'single';
      else if (ch === '"') mode = 'double';
      else if (ch === '`') mode = 'template';
      else if (ch === '{') depth++;
      else if (ch === '}') { depth--; if (depth === 0) mode = 'code'; }
      else if (ch === '/' && nx === '/') { j++; mode = 'lineCom'; }
      else if (ch === '/' && nx === '*') { j++; mode = 'blockCom'; }
      else if (ch === '(') depth++;
      else if (ch === ')') depth--;
      else if (ch === '[') depth++;
      else if (ch === ']') depth--;
    } else if (mode === 'lineCom') {
      if (ch === '\n') {
        // si el comentario estaba dentro de un tplExpr..., se maneja igual
        mode = 'code';
      }
    } else if (mode === 'blockCom') {
      if (ch === '*' && nx === '/') { j++; mode = 'code'; }
    } else {
      // mode === 'code'
      if (ch === '[' ) { depth++; started = true; }
      else if (ch === ']') {
        depth--;
        if (started && depth === 0) {
          return src.slice(open, j + 1);
        }
      }
      else if (ch === "'") mode = 'single';
      else if (ch === '"') mode = 'double';
      else if (ch === '`') mode = 'template';
      else if (ch === '/' && nx === '/') { j++; mode = 'lineCom'; }
      else if (ch === '/' && nx === '*') { j++; mode = 'blockCom'; }
      else if (ch === '{') depth++;
      else if (ch === '}') depth--;
      else if (ch === '(') depth++;
      else if (ch === ')') depth--;
    }
  }
  throw new Error('no se cerro el literal');
}

const marker = 'const SEED_CHALLENGES = [';
const literal = extractArrayLiteral(src, marker)/* + ']' */.replace(/;$/, '');

// Construir un modulo temporal con `export default <literal>` y verificar sintaxis.
fs.writeFileSync('_seed_tmp.mjs', 'export default ' + literal + ';\n');
try {
  const mod = await import('./_seed_tmp.mjs?t=' + Date.now());
  const seeds = mod.default;
  const keep = seeds.filter((s) => ['rf-carrito-refactor-single-medio', 'md-biblioteca-modelado-single-medio', 'hk-playlist-recomendador-single-bajo'].includes(s.challengeId));
  if (keep.length !== 3) { throw new Error('no se encontraron los 3 seeds: ' + keep.map((k) => k.challengeId).join(', ')); }
  console.log('extraidos OK: ' + keep.map((k) => k.challengeId).join(', '));
  // guardar para la segunda etapa
  fs.writeFileSync('_seed_keep.json', JSON.stringify(keep, null, 2));
  process.exit(0);
} catch (e) {
  console.error('fallo import modulo semilla:', e);
  process.exit(1);
}
