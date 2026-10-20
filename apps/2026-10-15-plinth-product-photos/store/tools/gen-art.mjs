#!/usr/bin/env node
/**
 * gen-art.mjs — generate the Plinth store marketing artwork through the FAL queue API.
 *
 * Every image is an editorial product / texture plate behind a phone mockup in a
 * 1080x1920 marketing frame, so every prompt reserves one calm vertical third of the
 * frame and forbids text, logos, people and any second object that could read as a
 * second product.
 *
 * Usage:
 *   node gen-art.mjs --all
 *   node gen-art.mjs --only hero-vase
 *   node gen-art.mjs --only hero-vase,texture-teal --force
 *   node gen-art.mjs --all --seed 1234
 *
 * Behaviour:
 *   - reads the FAL key from <repo-root>/.kiln/prod.key_fal and never prints it;
 *   - submits to https://queue.fal.run/<endpoint>, polls status_url until COMPLETED,
 *     then reads response_url;
 *   - skips an image whose output already exists unless --force;
 *   - converts to PNG with sharp when it is resolvable, otherwise keeps the JPEG bytes
 *     and writes <name>.jpg;
 *   - writes every intermediate byte under <repo-root>/.kiln/scratch/art/.
 *
 * No npm dependency: global fetch only. Node 20+.
 */

import { createRequire } from 'node:module';
import { existsSync } from 'node:fs';
import { mkdir, readFile, rename, stat, unlink, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

// ---------------------------------------------------------------------------
// Paths — everything resolves from this file's own location, never from cwd.
// ---------------------------------------------------------------------------

const HERE = path.dirname(fileURLToPath(import.meta.url));
const SLOT_ROOT = path.resolve(HERE, '..', '..'); // apps/<slot>
const ART_DIR = path.join(SLOT_ROOT, 'store', 'art');
const SLOT_SERVER = path.join(SLOT_ROOT, 'server');

function findRepoRoot(startDir) {
  let dir = startDir;
  for (let i = 0; i < 12; i += 1) {
    if (existsSync(path.join(dir, '.kiln')) && existsSync(path.join(dir, 'scripts'))) {
      return dir;
    }
    const parent = path.dirname(dir);
    if (parent === dir) break;
    dir = parent;
  }
  throw new Error(
    `could not locate the PlayKiln repo root above ${startDir} ` +
      '(no ancestor holds both .kiln/ and scripts/). Run the script from inside the repository.',
  );
}

const REPO_ROOT = findRepoRoot(SLOT_ROOT);
const KEY_PATH = path.join(REPO_ROOT, '.kiln', 'prod.key_fal');
const SCRATCH_DIR = path.join(REPO_ROOT, '.kiln', 'scratch', 'art');

// ---------------------------------------------------------------------------
// Endpoints — tried in this order, each with the request body it actually accepts.
// ---------------------------------------------------------------------------

// 9:16 - the canvas the composition actually draws on. A 2:3 plate has to be zoomed by about a
// third after the top trim, which both softens the photograph and pushes its subject out of the
// band the phone frame leaves free; a plate at the canvas ratio is cropped by ~2% and stays sharp.
const SIZE = { width: 1024, height: 1792 }; // both sides multiples of 32

const ENDPOINTS = [
  {
    id: 'fal-ai/flux-pro/v1.1',
    body: ({ prompt, seed }) => ({
      prompt,
      image_size: SIZE,
      num_images: 1,
      safety_tolerance: '2',
      output_format: 'jpeg',
      ...(Number.isInteger(seed) ? { seed } : {}),
    }),
  },
  {
    id: 'fal-ai/flux/dev',
    body: ({ prompt, seed }) => ({
      prompt,
      image_size: SIZE,
      num_images: 1,
      enable_safety_checker: true,
      output_format: 'jpeg',
      ...(Number.isInteger(seed) ? { seed } : {}),
    }),
  },
  {
    id: 'fal-ai/flux/schnell',
    body: ({ prompt, seed }) => ({
      prompt,
      image_size: SIZE,
      num_images: 1,
      enable_safety_checker: true,
      output_format: 'jpeg',
      ...(Number.isInteger(seed) ? { seed } : {}),
    }),
  },
];

const QUEUE_BASE = 'https://queue.fal.run';
const POLL_INTERVAL_MS = 1500;
const MAX_WAIT_MS = 300_000; // 5 min per attempt; flux pro is ~15-30 s
const SUBMIT_TIMEOUT_MS = 60_000;
const POLL_TIMEOUT_MS = 30_000;
const DOWNLOAD_TIMEOUT_MS = 120_000;

// ---------------------------------------------------------------------------
// Prompt text — palette-bound, portrait, one calm third, no text/logos/people.
// ---------------------------------------------------------------------------

// The frame these plates land in: a headline block across the top quarter, a phone mock-up
// standing from about two fifths of the way down and bleeding off the bottom. That leaves one
// band the photograph actually lives in - roughly 15% to 40% of the height - and a product that
// is small, centred and dim disappears into it. So every prompt asks for a LARGE subject cropped
// by the frame edges, a saturated colour story of its own, and the top-left corner kept quiet
// enough for type.
const RULES =
  'Exactly one hero subject: no second product, no duplicate item, no prop that could read ' +
  'as another product. The subject is LARGE - it fills the upper two thirds of the frame and is ' +
  'cropped by the top and side edges, so it reads as a close editorial crop rather than an ' +
  'object standing far away. Strong colour contrast between the product and its backdrop, one ' +
  'dominant colour per image. No text, no lettering, no captions, no numbers, no logos, no ' +
  'brand marks, no watermarks, no user interface, no people, no hands. Vertical 3:5 portrait ' +
  'composition. Editorial commercial photography, photorealistic, crisp detail, 85mm lens look, ' +
  'confident studio lighting, expensive and uncluttered.';

const CALM_PRODUCT =
  'Composition: the subject is close and large in the upper half, cropped by the top and side ' +
  'edges. Keep the top-left fifth of the frame calm and lower in contrast so a headline can sit ' +
  'there, and keep the lower two fifths a smooth uncluttered field of the same colour as the ' +
  'backdrop, with no props, so a phone mock-up can stand on it.';

const CALM_TEXTURE =
  'Composition: keep the top-left fifth calm and low in contrast and the lower two fifths a ' +
  'smooth uncluttered sweep, so copy and a phone mock-up can sit over the frame.';

const IMAGES = [
  {
    name: 'hero-vase',
    scene:
      'A big sculptural ceramic vase in glossy deep teal glaze with a softly ribbed silhouette, ' +
      'shot close and large, cropped by the top edge. Seamless backdrop of warm sand-beige that ' +
      'falls to a darker warm shadow toward the corners. One crisp key light from the right draws ' +
      'a long pale highlight down the glaze, and a soft shadow drops to the right of the vase. ' +
      'Realistic ceramic texture, tactile ribbing.',
    calm: CALM_PRODUCT,
  },
  {
    name: 'hero-sneaker',
    scene:
      'A single white leather sneaker, completely unbranded, seen in clean side view at eye ' +
      'level, standing on a wet dark stone ledge that ends around the middle of the frame, with ' +
      'a faint mirror reflection under it. Saturated deep teal seamless backdrop with a cool ' +
      'light falling from the upper right. Crisp pale highlight along the sole and the toe. ' +
      'Realistic leather grain, stitching and laces.',
    calm:
      'Composition: the WHOLE shoe is visible - heel to toe, sole to laces - centred in the ' +
      'upper half of the frame, uncropped, with open backdrop above it. Keep the top-left fifth ' +
      'calm and low in contrast for a headline, and keep the lower two fifths a smooth even ' +
      'teal floor with nothing on it for a phone mock-up.',
  },
  {
    name: 'hero-perfume',
    scene:
      'A tall amber glass perfume bottle with a plain black cylindrical cap, shot close and ' +
      'large, cropped by the top edge, on a charcoal ledge. Behind it a warm amber-to-graphite ' +
      'gradient field with a soft glow, fine suspended mist catching a hard rim light on the left ' +
      'side of the glass. Realistic refraction and amber luminosity, no label of any kind.',
    calm: CALM_PRODUCT,
  },
  {
    name: 'hero-candle',
    scene:
      'A matte cream candle poured into a rounded dark stone vessel, whole and centred on a pale ' +
      'plaster shelf that ends around the middle of the frame. Backdrop of soft terracotta that ' +
      'deepens toward the frame edges, warm low sunlight raking in from the left with a long soft ' +
      'shadow. Realistic wax surface, stone vessel texture, no label.',
    calm:
      'Composition: the jar is WHOLE and centred in the upper half, uncropped, with open warm ' +
      'backdrop above it. Keep the top-left fifth calm for a headline, and the lower two fifths ' +
      'a smooth even terracotta floor with nothing on it for a phone mock-up.',
  },
  {
    name: 'hero-bag',
    scene:
      'A tan leather crossbody bag with a slim strap, shot close and large at chest height, ' +
      'cropped by the top edge, against a deep graphite-green concrete wall. One warm directional ' +
      'key light from the left makes the tan leather glow amber against the dark wall, with a ' +
      'cool teal fill on the right and a quiet shadow cast behind the bag. Realistic leather ' +
      'grain, plain unbranded hardware.',
    calm: CALM_PRODUCT,
  },
  {
    name: 'texture-teal',
    scene:
      'Abstract dark teal silk filling the frame, near-black overall. Soft deep folds in the left ' +
      'two thirds catching a single cool teal-white light. The right third is a smooth unbroken ' +
      'sweep of silk falling into deep near-black shadow — much darker than the left, with no ' +
      'highlight and no pale grey fabric. Strictly cool tones, no warm, golden or amber tint. ' +
      'Very soft, low contrast, no objects, no props. Usable as a dark background texture.',
    calm: CALM_TEXTURE,
  },
  {
    name: 'texture-stone',
    scene:
      'Extreme macro photograph of a very dark polished basalt slab filling the frame, almost ' +
      'black, with fine mineral grain and faint hairline veining. Extremely low contrast, no ' +
      'bright highlights, no warm brown or beige tones — only cool graphite-black with a subtle ' +
      'cool teal sheen where a single grazing light touches the surface. No objects, no edges, ' +
      'no pebbles. Usable as a dark background texture.',
    calm: CALM_TEXTURE,
  },
  {
    name: 'scene-studio',
    scene:
      'A pale stone plinth in warm sand-beige light, standing on a seamless floor of the same ' +
      'stone, seen straight from the front, the plinth totally empty with nothing on it. A ' +
      'single column of warm light falls on the plinth face and a long quiet shadow stretches to ' +
      'the right, with a soft teal shadow deep in the background. Editorial, empty, inviting. No ' +
      'product at all.',
    calm:
      'Composition: the empty plinth stands WHOLE in the upper half, uncropped, with open warm ' +
      'backdrop above it. Keep the top-left fifth calm and low in contrast, and the lower two ' +
      'fifths a smooth even stone floor with no object, so a phone mock-up can stand there.',
  },
];

function buildPrompt(spec) {
  return `${spec.scene}\n\n${spec.calm}\n\n${RULES}`;
}

// ---------------------------------------------------------------------------
// Small helpers
// ---------------------------------------------------------------------------

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
const nowMs = () => Number(process.hrtime.bigint() / 1_000_000n);
const fmtSeconds = (ms) => `${(ms / 1000).toFixed(1)}s`;

function fail(message) {
  console.error(`\nERROR: ${message}\n`);
  process.exit(2);
}

async function readKey() {
  let raw;
  try {
    raw = await readFile(KEY_PATH, 'utf8');
  } catch (err) {
    fail(
      `FAL key not found at ${KEY_PATH} (${err.code || err.message}).\n` +
        'This tool never takes a key from the environment: place the key on one line in that file.',
    );
  }
  const key = raw.replace(/^\uFEFF/, '').trim();
  if (!key) fail(`FAL key file ${KEY_PATH} is empty.`);
  if (/\s/.test(key)) {
    fail(
      `FAL key file ${KEY_PATH} contains whitespace inside the value; expected a single line of ` +
        `key material (${key.length} chars read).`,
    );
  }
  return key;
}

/**
 * Resolve sharp the way a reviewer would check it, then look beside the slot's own
 * server, which is where this repository already vendors it. Returns { sharp, path }
 * or { sharp: null } — never throws.
 */
async function resolveSharp() {
  const require = createRequire(import.meta.url);
  const attempts = [
    () => require.resolve('sharp'),
    () => require.resolve('sharp', { paths: [SLOT_SERVER] }),
  ];
  for (const attempt of attempts) {
    let resolved;
    try {
      resolved = attempt();
    } catch {
      continue;
    }
    try {
      const mod = await import(pathToFileURL(resolved).href);
      const sharp = mod.default ?? mod;
      if (typeof sharp === 'function') return { sharp, path: resolved };
    } catch (err) {
      console.warn(`  note: sharp resolved at ${resolved} but failed to load (${err.message})`);
    }
  }
  return { sharp: null, path: null };
}

async function fetchText(url, init, timeoutMs) {
  const res = await fetch(url, { ...init, signal: AbortSignal.timeout(timeoutMs) });
  const text = await res.text();
  return { res, text };
}

// ---------------------------------------------------------------------------
// FAL queue: submit -> poll status_url -> read response_url
// ---------------------------------------------------------------------------

class EndpointRefused extends Error {
  constructor(status, body) {
    super(`${status}: ${body.slice(0, 400)}`);
    this.status = status;
  }
}

async function submit(key, endpointId, body) {
  const { res, text } = await fetchText(
    `${QUEUE_BASE}/${endpointId}`,
    {
      method: 'POST',
      headers: { Authorization: `Key ${key}`, 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    },
    SUBMIT_TIMEOUT_MS,
  );
  if (!res.ok) throw new EndpointRefused(res.status, text);
  let json;
  try {
    json = JSON.parse(text);
  } catch {
    throw new Error(`submit returned non-JSON (${res.status}): ${text.slice(0, 200)}`);
  }
  const requestId = json.request_id;
  if (!requestId) throw new Error(`submit returned no request_id: ${text.slice(0, 200)}`);
  return {
    requestId,
    statusUrl: json.status_url || `${QUEUE_BASE}/${endpointId}/requests/${requestId}/status`,
    responseUrl: json.response_url || `${QUEUE_BASE}/${endpointId}/requests/${requestId}`,
  };
}

async function waitForCompletion(key, job) {
  const started = Date.now();
  let lastStatus = 'unknown';
  while (Date.now() - started < MAX_WAIT_MS) {
    await sleep(POLL_INTERVAL_MS);
    let payload;
    try {
      const { res, text } = await fetchText(
        job.statusUrl,
        { headers: { Authorization: `Key ${key}` } },
        POLL_TIMEOUT_MS,
      );
      if (res.status === 401 || res.status === 403) throw new Error(`auth rejected while polling (${res.status})`);
      if (!res.ok) continue; // transient; keep polling until the deadline
      payload = JSON.parse(text);
    } catch (err) {
      if (/auth rejected/.test(err.message)) throw err;
      continue; // network hiccup or non-JSON poll; retry until timeout
    }
    lastStatus = String(payload.status || 'unknown');
    if (lastStatus === 'COMPLETED') return payload;
    if (lastStatus === 'FAILED' || lastStatus === 'ERROR' || payload.error) {
      throw new Error(`job ${job.requestId} ${lastStatus}: ${JSON.stringify(payload).slice(0, 400)}`);
    }
  }
  throw new Error(`job ${job.requestId} did not complete within ${fmtSeconds(MAX_WAIT_MS)} (last status ${lastStatus})`);
}

async function readResult(key, job) {
  const { res, text } = await fetchText(
    job.responseUrl,
    { headers: { Authorization: `Key ${key}` } },
    POLL_TIMEOUT_MS,
  );
  if (!res.ok) throw new Error(`result read failed (${res.status}): ${text.slice(0, 400)}`);
  const json = JSON.parse(text);
  const image = Array.isArray(json.images) ? json.images[0] : undefined;
  if (!image?.url) throw new Error(`result carried no image url: ${text.slice(0, 300)}`);
  return { json, image };
}

async function download(url) {
  const res = await fetch(url, { signal: AbortSignal.timeout(DOWNLOAD_TIMEOUT_MS) });
  if (!res.ok) throw new Error(`download failed (${res.status})`);
  const bytes = Buffer.from(await res.arrayBuffer());
  if (bytes.byteLength < 1024) throw new Error(`download is suspiciously small (${bytes.byteLength} bytes)`);
  return { bytes, contentType: res.headers.get('content-type') || '' };
}

// ---------------------------------------------------------------------------
// One image, with the endpoint fallback ladder
// ---------------------------------------------------------------------------

async function generateOne(key, spec, opts) {
  const prompt = buildPrompt(spec);
  const body = { prompt, seed: opts.seed, force: opts.force };
  const failures = [];
  const attempted = [];

  for (let i = opts.startIndex; i < ENDPOINTS.length; i += 1) {
    const endpoint = ENDPOINTS[i];
    const requestBody = endpoint.body(body);

    for (let attempt = 1; attempt <= 2; attempt += 1) {
      const startedAt = nowMs();
      try {
        const job = await submit(key, endpoint.id, requestBody);
        const status = await waitForCompletion(key, job);
        const { json, image } = await readResult(key, job);
        const { bytes, contentType } = await download(image.url);
        const elapsedMs = nowMs() - startedAt;
        return {
          spec,
          endpoint: endpoint.id,
          endpointIndex: i,
          requestBody,
          requestId: job.requestId,
          seed: Number.isInteger(image.seed) ? image.seed : (json.seed ?? opts.seed ?? null),
          elapsedMs,
          bytes,
          contentType,
          apiWidth: image.width ?? null,
          apiHeight: image.height ?? null,
          attempts: attempted,
          queueStatus: status.status,
        };
      } catch (err) {
        const refusal = err instanceof EndpointRefused ? err.status : null;
        failures.push({ endpoint: endpoint.id, attempt, message: err.message, refusal });
        attempted.push(`${endpoint.id}#${attempt}`);
        const hardRefusal = refusal === 404 || refusal === 422;
        console.warn(
          `  ${endpoint.id} attempt ${attempt} failed${refusal ? ` (${refusal})` : ''}: ` +
            `${err.message.replace(/\s+/g, ' ').slice(0, 200)}`,
        );
        if (hardRefusal) break; // the route or the body shape is wrong; retrying cannot help
        if (attempt < 2) await sleep(2000);
      }
    }
  }

  const detail = failures.map((f) => `${f.endpoint}#${f.attempt}: ${f.message.slice(0, 160)}`).join(' | ');
  throw new Error(`every endpoint failed for ${spec.name} — ${detail}`);
}

// ---------------------------------------------------------------------------
// Output: PNG via sharp when available, otherwise the untouched JPEG bytes
// ---------------------------------------------------------------------------

function sniffExtension(contentType, sourceUrl) {
  const type = (contentType || '').toLowerCase();
  const url = (sourceUrl || '').toLowerCase();
  if (type.includes('png') || /\.png(\?|$)/.test(url)) return '.png';
  return '.jpg';
}

async function writeArtifact(sharpInfo, result) {
  const base = result.spec.name;
  const scratchBase = path.join(SCRATCH_DIR, base);
  const ext = sharpInfo.sharp ? '.png' : sniffExtension(result.contentType, '');
  const finalPath = path.join(ART_DIR, `${base}${ext}`);

  let bytes = result.bytes;
  let width = result.apiWidth;
  let height = result.apiHeight;

  if (sharpInfo.sharp) {
    const sharp = sharpInfo.sharp;
    const meta = await sharp(bytes).metadata();
    width = meta.width ?? width;
    height = meta.height ?? height;
    bytes = await sharp(bytes).png({ compressionLevel: 9, adaptiveFiltering: true }).toBuffer();
  }

  const partPath = `${scratchBase}${ext}.part`;
  await writeFile(partPath, bytes);
  try {
    await rename(partPath, finalPath);
  } catch {
    await unlink(finalPath).catch(() => {});
    await rename(partPath, finalPath);
  }
  const info = await stat(finalPath);
  return { path: finalPath, size: info.size, width, height, converted: Boolean(sharpInfo.sharp) };
}

// ---------------------------------------------------------------------------
// CLI
// ---------------------------------------------------------------------------

const USAGE = `gen-art.mjs — FAL marketing artwork for ${path.relative(REPO_ROOT, ART_DIR)}

  node gen-art.mjs --all                     every image
  node gen-art.mjs --only <name>[,<name>]    one or more images by name
  node gen-art.mjs --all --force             regenerate even if the file exists
  node gen-art.mjs --all --seed 1234         pin a seed (default: server-chosen)

  images: ${IMAGES.map((i) => i.name).join(', ')}`;

function parseArgs(argv) {
  const opts = { all: false, only: [], force: false, seed: undefined, help: false };
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i];
    if (arg === '--all') opts.all = true;
    else if (arg === '--force') opts.force = true;
    else if (arg === '--help' || arg === '-h') opts.help = true;
    else if (arg === '--only') {
      const value = argv[i + 1];
      if (!value) fail('--only needs a name, e.g. --only hero-vase');
      opts.only.push(...value.split(',').map((v) => v.trim()).filter(Boolean));
      i += 1;
    } else if (arg.startsWith('--only=')) {
      opts.only.push(...arg.slice('--only='.length).split(',').map((v) => v.trim()).filter(Boolean));
    } else if (arg === '--seed') {
      const value = Number(argv[i + 1]);
      if (!Number.isInteger(value) || value < 0) fail('--seed needs a non-negative integer');
      opts.seed = value;
      i += 1;
    } else if (arg.startsWith('--seed=')) {
      const value = Number(arg.slice('--seed='.length));
      if (!Number.isInteger(value) || value < 0) fail('--seed needs a non-negative integer');
      opts.seed = value;
    } else {
      fail(`unknown argument "${arg}"\n\n${USAGE}`);
    }
  }
  return opts;
}

async function main() {
  const opts = parseArgs(process.argv.slice(2));
  if (opts.help) {
    console.log(USAGE);
    return 0;
  }
  if (!opts.all && opts.only.length === 0) {
    console.error(USAGE);
    fail('nothing selected: pass --all or --only <name>');
  }

  const known = new Map(IMAGES.map((img) => [img.name, img]));
  const unknown = opts.only.filter((name) => !known.has(name));
  if (unknown.length) fail(`unknown image name(s): ${unknown.join(', ')}\n\n${USAGE}`);

  const selected = opts.all ? IMAGES : opts.only.map((name) => known.get(name));

  const key = await readKey();
  const sharpInfo = await resolveSharp();
  await mkdir(SCRATCH_DIR, { recursive: true });
  await mkdir(ART_DIR, { recursive: true });

  console.log(`Plinth store artwork — ${selected.length} image(s)`);
  console.log(`  slot      ${SLOT_ROOT}`);
  console.log(`  scratch   ${SCRATCH_DIR}`);
  console.log(`  key       ${KEY_PATH} (loaded, ${key.length} chars, not printed)`);
  console.log(
    `  sharp     ${sharpInfo.sharp ? `resolved at ${sharpInfo.path} -> PNG` : 'not resolvable -> JPEG bytes kept as .jpg'}`,
  );
  console.log('');

  const results = [];
  const skipped = [];
  const errors = [];
  let startIndex = 0;

  for (const spec of selected) {
    const pngPath = path.join(ART_DIR, `${spec.name}.png`);
    const jpgPath = path.join(ART_DIR, `${spec.name}.jpg`);

    if (!opts.force) {
      const existing = [pngPath, jpgPath].find((p) => existsSync(p));
      if (existing) {
        const info = await stat(existing);
        console.log(`- ${spec.name}: exists, skipped (${info.size} bytes) ${existing}`);
        skipped.push({ name: spec.name, path: existing, size: info.size });
        continue;
      }
    }

    console.log(`> ${spec.name}`);
    try {
      const result = await generateOne(key, spec, { ...opts, startIndex });
      startIndex = result.endpointIndex; // sticky: a working endpoint is used for the rest
      const written = await writeArtifact(sharpInfo, result);
      console.log(`  endpoint  ${result.endpoint}`);
      console.log(`  seed      ${result.seed ?? '(server default, not reported)'}`);
      console.log(`  elapsed   ${fmtSeconds(result.elapsedMs)}`);
      console.log(`  output    ${written.path}`);
      console.log(`  size      ${written.size} bytes${written.converted ? ' (PNG via sharp)' : ' (JPEG bytes unchanged)'}`);
      console.log(`  dims      ${written.width ?? '?'}x${written.height ?? '?'}`);
      console.log(`  body      ${JSON.stringify(result.requestBody)}`);
      if (result.attempts.length) console.log(`  attempts  ${result.attempts.join(' -> ')}`);
      console.log('');
      results.push({ ...written, name: spec.name, endpoint: result.endpoint, seed: result.seed, elapsedMs: result.elapsedMs });
    } catch (err) {
      console.error(`  FAILED  ${err.message}\n`);
      errors.push({ name: spec.name, message: err.message });
    }
  }

  console.log('--- summary ---');
  if (results.length) {
    for (const r of results) {
      console.log(
        `  ok    ${r.name.padEnd(15)} ${r.endpoint.padEnd(22)} seed=${String(r.seed).padEnd(10)} ` +
          `${fmtSeconds(r.elapsedMs).padStart(7)}  ${String(r.size).padStart(9)} bytes  ${r.width}x${r.height}  ${r.path}`,
      );
    }
  }
  for (const s of skipped) console.log(`  skip  ${s.name.padEnd(15)} existing ${s.size} bytes  ${s.path}`);
  for (const e of errors) console.log(`  FAIL  ${e.name.padEnd(15)} ${e.message.slice(0, 200)}`);
  console.log(`\n${results.length} generated, ${skipped.length} skipped, ${errors.length} failed.`);

  return errors.length ? 1 : 0;
}

main()
  .then((code) => process.exit(code))
  .catch((err) => {
    console.error(`\nUNHANDLED: ${err?.stack || err}\n`);
    process.exit(2);
  });
