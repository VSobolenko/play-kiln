import { describe, expect, it } from 'vitest';
import { randomBytes } from 'node:crypto';
import sharp from 'sharp';
import { MAX_RESULT_SIDE, MIN_RESULT_SIDE, normaliseResult, ResultUnusableError } from '../src/results.js';

const background = { r: 120, g: 80, b: 40 };

function blank(width: number, height: number): sharp.Sharp {
  return sharp({ create: { width, height, channels: 3, background } });
}

async function expectConsistent(result: Awaited<ReturnType<typeof normaliseResult>>): Promise<void> {
  const metadata = await sharp(result.bytes).metadata();
  expect(result.width).toBe(metadata.width);
  expect(result.height).toBe(metadata.height);
  expect(result.format).toBe(metadata.format);
}

describe('normaliseResult', () => {
  it('upscales a 224x224 PNG to a 1024 long side', async () => {
    const result = await normaliseResult(await blank(224, 224).png().toBuffer());
    expect(Math.max(result.width, result.height)).toBe(MIN_RESULT_SIDE);
    expect(result.format).toBe('png');
    await expectConsistent(result);
  });

  it('passes an already large JPEG through byte-identical', async () => {
    const input = await blank(1024, 1024).jpeg().toBuffer();
    const result = await normaliseResult(input);
    expect(result.bytes.equals(input)).toBe(true);
    expect(result.format).toBe('jpeg');
    expect(result.width).toBe(1024);
    await expectConsistent(result);
  });

  it('keeps the aspect ratio of a portrait upscale', async () => {
    const result = await normaliseResult(await blank(600, 900).png().toBuffer());
    expect(result.height).toBe(1024);
    expect(Math.abs(result.width - Math.round(600 * 1024 / 900))).toBeLessThanOrEqual(1);
    await expectConsistent(result);
  });

  it('leaves an 800x1200 portrait alone because its long side already meets the floor', async () => {
    const input = await blank(800, 1200).png().toBuffer();
    const result = await normaliseResult(input);
    expect(result.bytes.equals(input)).toBe(true);
    expect([result.width, result.height]).toEqual([800, 1200]);
  });

  it('downscales an oversized image to the maximum long side', async () => {
    const result = await normaliseResult(await blank(6000, 4000).png().toBuffer());
    expect(result.width).toBe(MAX_RESULT_SIDE);
    expect(Math.abs(result.height - Math.round(4000 * MAX_RESULT_SIDE / 6000))).toBeLessThanOrEqual(1);
    await expectConsistent(result);
  });

  it('rejects an empty buffer with the named error', async () => {
    await expect(normaliseResult(Buffer.alloc(0))).rejects.toBeInstanceOf(ResultUnusableError);
    await expect(normaliseResult(Buffer.alloc(0))).rejects.toMatchObject({ code: 'RESULT_UNUSABLE' });
  });

  it('rejects bytes that are not an image with the named error', async () => {
    await expect(normaliseResult(randomBytes(512))).rejects.toBeInstanceOf(ResultUnusableError);
  });
});
