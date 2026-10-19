import { describe, expect, it } from 'vitest';
import { loadConfig } from '../src/config.js';
import { MODEL_REGISTRY, operationConfig } from '../src/registry.js';

const measuredEnhancedOps = ['O-07', 'O-08', 'O-10'];

function configWithFalKey(falKey?: string) {
  return loadConfig({
    JWT_SECRET: 'x'.repeat(32),
    ...(falKey ? { FAL_KEY: falKey } : {}),
  });
}

describe('provider operation registry', () => {
  it('uses the measured O-01 endpoint and request cost', () => {
    expect(MODEL_REGISTRY['O-01']).toMatchObject({
      endpointId: 'fal-ai/any-llm/vision',
      costMicroUsd: 10_000,
      unit: 'requests',
      referenceSize: '1 request',
    });
  });

  it('includes the fal.ai text translation request in the O-07 provider cost', () => {
    expect(MODEL_REGISTRY['O-07'].costMicroUsd).toBe(41_000);
  });

  it('does not enable an operation without a measured cost', () => {
    for (const [id, operation] of Object.entries(MODEL_REGISTRY)) {
      if (operation.enabled) {
        expect(operation.costMicroUsd, `${id} must have a measured cost`).toEqual(expect.any(Number));
      }
    }
  });

  it('keeps every operation disabled when no fal key is configured', () => {
    const configured = operationConfig(configWithFalKey(), false);

    expect(Object.values(configured).every(({ enabled }) => !enabled)).toBe(true);
  });

  it('enables the measured operations only when a fal key is configured', () => {
    const configured = operationConfig(configWithFalKey('test-fal-key'), false);
    const enabled = Object.entries(configured)
      .filter(([, operation]) => operation.enabled)
      .map(([id]) => id)
      .sort();

    expect(enabled).toEqual([
      'O-01',
      'O-02',
      ...measuredEnhancedOps,
      'O-09',
      'O-12',
    ].sort());
    for (const id of [...measuredEnhancedOps, 'O-09', 'O-12']) {
      expect(configured[id]?.enabled, `${id} should be enabled`).toBe(true);
    }
  });

  it('keeps O-11 disabled because its per-megapixel price exceeds its tier ceiling', () => {
    expect(MODEL_REGISTRY['O-11'].enabled).toBe(false);
  });

  it('keeps unavailable operations disabled for their missing endpoint, cost, or inputs', () => {
    expect(MODEL_REGISTRY['O-13']).toMatchObject({
      enabled: false, endpointId: null, costMicroUsd: null,
    });
    expect(MODEL_REGISTRY['O-14']).toMatchObject({
      enabled: false, disabledReason: expect.stringContaining('garment_image_url'),
    });
    expect(MODEL_REGISTRY['O-15']).toMatchObject({
      enabled: false, endpointId: null, costMicroUsd: null,
    });
  });

  it('continues to disable otherwise enabled operations while the kill switch is active', () => {
    const configured = operationConfig(configWithFalKey('test-fal-key'), true);

    expect(Object.values(configured).every(({ enabled }) => !enabled)).toBe(true);
  });
});
