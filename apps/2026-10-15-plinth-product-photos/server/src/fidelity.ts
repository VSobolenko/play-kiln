import sharp from 'sharp';

type FidelityOperation = 'O-09' | 'O-12';

type RawPixels = {
  data: Buffer;
  width: number;
  height: number;
  channels: number;
};

function pair(
  value: unknown,
  predicate: (part: unknown) => boolean,
): [number, number] | null {
  if (!Array.isArray(value) || value.length !== 2 ||
      !value.every(predicate)) return null;
  return [Number(value[0]), Number(value[1])];
}

async function rgb(bytes: Buffer): Promise<RawPixels> {
  const { data, info } = await sharp(bytes, { limitInputPixels: 40_000_000 })
    .toColourspace('srgb').removeAlpha().raw().toBuffer({ resolveWithObject: true });
  return { data, width: info.width, height: info.height, channels: info.channels };
}

function fail(reason: string): never {
  throw new Error('FIDELITY_GUARD_FAILED', { cause: new Error(reason) });
}

export async function preserveProductPixels(
  op: FidelityOperation,
  sourceBytes: Buffer,
  providerBytes: Buffer,
  params: Record<string, unknown>,
  maskBytes?: Buffer,
): Promise<Buffer> {
  let maxKeptDrift = 0;
  try {
    const source = await rgb(sourceBytes);
    const output = await rgb(providerBytes);
    if (source.channels !== 3 || output.channels !== 3) fail('FIDELITY_INPUT_INVALID');

    if (op === 'O-09') {
      const canvas = pair(params['canvas_size'], (value) =>
        typeof value === 'number' && Number.isSafeInteger(value) && value > 0);
      const size = pair(params['original_image_size'], (value) =>
        typeof value === 'number' && Number.isSafeInteger(value) && value > 0);
      const location = pair(params['original_image_location'], (value) =>
        typeof value === 'number' && Number.isSafeInteger(value) && value >= 0);
      if (!canvas || !size || !location ||
          size[0] !== source.width || size[1] !== source.height ||
          location[0] + size[0] > canvas[0] || location[1] + size[1] > canvas[1] ||
          output.width !== canvas[0] || output.height !== canvas[1]) fail('FIDELITY_INPUT_INVALID');

      const rowBytes = source.width * 3;
      for (let y = 0; y < source.height; y += 1) {
        const sourceOffset = y * rowBytes;
        const outputOffset = ((location[1] + y) * output.width + location[0]) * 3;
        // The preserved rectangle is written from the source, so it is exact by construction.
        source.data.copy(output.data, outputOffset, sourceOffset, sourceOffset + rowBytes);
      }
    } else {
      if (!maskBytes) fail('FIDELITY_MASK_INVALID');
      let mask: Buffer;
      let info: sharp.OutputInfo;
      try {
        ({ data: mask, info } = await sharp(maskBytes, { limitInputPixels: 40_000_000 })
          .greyscale().raw().toBuffer({ resolveWithObject: true }));
      } catch {
        fail('FIDELITY_MASK_INVALID');
      }
      if (info.channels !== 1) fail('FIDELITY_MASK_INVALID');
      if (info.width !== source.width || info.height !== source.height) fail('FIDELITY_MASK_SIZE_MISMATCH');
      if (source.width !== output.width || source.height !== output.height) fail('FIDELITY_OUTPUT_SIZE_MISMATCH');

      let erased = 0;
      let drift = 0;
      // Bria polarity: white (255) erases; black (0) keeps the source pixel.
      for (let pixel = 0; pixel < mask.length; pixel += 1) {
        const offset = pixel * 3;
        const erase = mask[pixel]!;
        if (erase !== 0) erased += 1;
        const keep = 255 - erase;
        for (let channel = 0; channel < 3; channel += 1) {
          const from = source.data[offset + channel]!;
          if (erase === 0) drift = Math.max(drift, Math.abs(output.data[offset + channel]! - from));
          // keep=255 yields the source value exactly, so kept pixels are overwritten, not compared.
          output.data[offset + channel] = Math.round(
            (from * keep + output.data[offset + channel]! * erase) / 255,
          );
        }
      }
      maxKeptDrift = drift;
      if (erased === 0) fail('FIDELITY_MASK_EMPTY');
    }
    return sharp(output.data, {
      raw: { width: output.width, height: output.height, channels: 3 },
    }).png().toBuffer();
  } catch (error) {
    const reason = error instanceof Error && error.message === 'FIDELITY_GUARD_FAILED' &&
      error.cause instanceof Error ? error.cause.message : 'FIDELITY_INTERNAL_ERROR';
    console.error(JSON.stringify({ event: 'fidelity_guard_failed', op, reason, maxKeptDrift }));
    if (error instanceof Error && error.message === 'FIDELITY_GUARD_FAILED') throw error;
    throw new Error('FIDELITY_GUARD_FAILED', { cause: new Error(reason, { cause: error }) });
  }
}