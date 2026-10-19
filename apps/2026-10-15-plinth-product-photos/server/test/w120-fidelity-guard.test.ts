import sharp from 'sharp';
import { describe, expect, it, vi } from 'vitest';
import { preserveProductPixels } from '../src/fidelity.js';

const W = 40;
const H = 30;

function pic(fn: (x: number, y: number, c: number) => number, w = W, h = H) {
  const data = Buffer.alloc(w * h * 3);
  for (let y = 0; y < h; y += 1) for (let x = 0; x < w; x += 1) {
    for (let c = 0; c < 3; c += 1) data[(y * w + x) * 3 + c] = fn(x, y, c);
  }
  return data;
}
const png = (data: Buffer, w = W, h = H, ch: 1 | 3 = 3) =>
  sharp(data, { raw: { width: w, height: h, channels: ch } }).png().toBuffer();
const inSquare = (x: number, y: number) => x >= 10 && x < 20 && y >= 10 && y < 20;
const maskPng = (w = W, h = H, fill = true) => png(
  Buffer.from(Array.from({ length: w * h }, (_, i) => (fill && inSquare(i % w, Math.floor(i / w)) ? 255 : 0))), w, h, 1);
const raw = async (b: Buffer) => (await sharp(b).removeAlpha().raw().toBuffer());
const source = () => pic((x, y, c) => (x * 5 + y * 3 + c * 40) % 256);

async function reason(p: Promise<unknown>) {
  const error = await p.then(() => null, (e: Error) => e);
  expect(error?.message).toBe('FIDELITY_GUARD_FAILED');
  return (error!.cause as Error).message;
}

describe('W120 O-12 fidelity by compositing', () => {
  vi.spyOn(console, 'error').mockImplementation(() => undefined);

  it('passes a provider output identical outside the mask', async () => {
    const src = source();
    const out = pic((x, y, c) => (inSquare(x, y) ? 0 : src[(y * W + x) * 3 + c]!));
    const result = await raw(await preserveProductPixels('O-12', await png(src), await png(out), {}, await maskPng()));
    for (let y = 0; y < H; y += 1) for (let x = 0; x < W; x += 1) if (!inSquare(x, y)) {
      expect([...result.subarray((y * W + x) * 3, (y * W + x) * 3 + 3)])
        .toEqual([...src.subarray((y * W + x) * 3, (y * W + x) * 3 + 3)]);
    }
  });

  it('corrects +-2 drift on many kept pixels so kept bytes equal the source exactly', async () => {
    const src = source();
    const out = pic((x, y, c) => Math.min(255, Math.max(0,
      src[(y * W + x) * 3 + c]! + ((x + y + c) % 2 ? 2 : -2))));
    const result = await raw(await preserveProductPixels('O-12', await png(src), await png(out), {}, await maskPng()));
    let kept = 0;
    for (let y = 0; y < H; y += 1) for (let x = 0; x < W; x += 1) if (!inSquare(x, y)) {
      kept += 1;
      for (let c = 0; c < 3; c += 1) expect(result[(y * W + x) * 3 + c]).toBe(src[(y * W + x) * 3 + c]);
    }
    expect(kept).toBeGreaterThan(1000);
  });

  it('changes the white region and leaves neighbours untouched', async () => {
    const src = pic(() => 100);
    const out = pic(() => 200);
    const result = await raw(await preserveProductPixels('O-12', await png(src), await png(out), {}, await maskPng()));
    expect(result[(15 * W + 15) * 3]).toBe(200);
    expect(result[(15 * W + 9) * 3]).toBe(100);
    expect(result[(15 * W + 20) * 3]).toBe(100);
    expect(result[(9 * W + 15) * 3]).toBe(100);
  });

  it('names an output size mismatch', async () => {
    expect(await reason(preserveProductPixels('O-12', await png(source()),
      await png(pic(() => 1, 20, 10), 20, 10), {}, await maskPng()))).toBe('FIDELITY_OUTPUT_SIZE_MISMATCH');
  });

  it('names a mask size mismatch', async () => {
    expect(await reason(preserveProductPixels('O-12', await png(source()), await png(source()), {},
      await maskPng(20, 10)))).toBe('FIDELITY_MASK_SIZE_MISMATCH');
  });

  it('names an undecodable or missing mask', async () => {
    expect(await reason(preserveProductPixels('O-12', await png(source()), await png(source()), {},
      Buffer.from('nope')))).toBe('FIDELITY_MASK_INVALID');
    expect(await reason(preserveProductPixels('O-12', await png(source()), await png(source()), {})))
      .toBe('FIDELITY_MASK_INVALID');
  });

  it('rejects an all-black mask', async () => {
    expect(await reason(preserveProductPixels('O-12', await png(source()), await png(source()), {},
      await maskPng(W, H, false)))).toBe('FIDELITY_MASK_EMPTY');
  });
});
