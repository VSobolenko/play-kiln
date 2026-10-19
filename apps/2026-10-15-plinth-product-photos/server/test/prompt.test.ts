import { describe, expect, it, vi } from 'vitest';
import { normalizeSceneGuidance } from '../src/prompt.js';

describe('scene prompt normalization', () => {
  it('translates free-form Cyrillic guidance and uses the English result', async () => {
    const translate = vi.fn(async () => 'in a tropical jungle');

    await expect(normalizeSceneGuidance('  в джунглях  ', translate))
      .resolves.toBe('in a tropical jungle');
    expect(translate).toHaveBeenCalledWith('в джунглях');
  });

  it('passes English guidance through without paying for translation', async () => {
    const translate = vi.fn();

    await expect(normalizeSceneGuidance('tent campsite in a pine forest', translate))
      .resolves.toBe('tent campsite in a pine forest');
    expect(translate).not.toHaveBeenCalled();
  });

  it('rejects a translation that is empty, too long, or still contains Cyrillic', async () => {
    for (const translation of ['', 'x'.repeat(2001), 'jungle джунгли']) {
      await expect(normalizeSceneGuidance('джунгли', async () => translation))
        .rejects.toThrow('SCENE_TRANSLATION_INVALID');
    }
  });
});
