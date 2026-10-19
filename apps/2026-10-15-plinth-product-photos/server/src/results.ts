import sharp from 'sharp';

export const MIN_RESULT_SIDE = 1024;
export const MAX_RESULT_SIDE = 4096;
export const MAX_RESULT_BYTES = 25 * 1024 * 1024;

export class ResultUnusableError extends Error {
  readonly code = 'RESULT_UNUSABLE';

  constructor(detail: string) {
    super('RESULT_UNUSABLE');
    this.name = 'ResultUnusableError';
    this.detail = detail;
  }

  readonly detail: string;
}

export type NormalisedResult = { bytes: Buffer; width: number; height: number; format: string };

async function describe(bytes: Buffer): Promise<NormalisedResult> {
  const metadata = await sharp(bytes).metadata();
  if (!metadata.width || !metadata.height || !metadata.format) {
    throw new ResultUnusableError('ZERO_DIMENSIONS');
  }
  return { bytes, width: metadata.width, height: metadata.height, format: metadata.format };
}

export async function normaliseResult(input: Buffer): Promise<NormalisedResult> {
  if (input.length === 0) throw new ResultUnusableError('EMPTY');
  let decoded: NormalisedResult;
  try {
    decoded = await describe(input);
  } catch (error) {
    if (error instanceof ResultUnusableError) throw error;
    throw new ResultUnusableError('UNDECODABLE');
  }

  const longSide = Math.max(decoded.width, decoded.height);
  if (longSide >= MIN_RESULT_SIDE && longSide <= MAX_RESULT_SIDE) return decoded;

  const target = longSide < MIN_RESULT_SIDE ? MIN_RESULT_SIDE : MAX_RESULT_SIDE;
  const scale = target / longSide;
  const width = Math.max(1, Math.round(decoded.width * scale));
  const height = Math.max(1, Math.round(decoded.height * scale));
  try {
    const bytes = await sharp(input)
      .resize({ width, height, kernel: 'lanczos3', fit: 'fill' })
      .png()
      .toBuffer();
    return await describe(bytes);
  } catch (error) {
    if (error instanceof ResultUnusableError) throw error;
    throw new ResultUnusableError('RESIZE_FAILED');
  }
}
