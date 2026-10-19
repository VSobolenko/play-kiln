export const PRODUCT_INTEGRITY_PROMPT =
  "Preserve the product identity. Preserve the product's shape, label, text, logo, and all unrequested colours exactly as shown; never crop or obscure any part of it.";

export const NO_INVENTED_ELEMENTS_PROMPT =
  'Never invent an unrelated subject, substitute another product, or add people or animals not present. Do not add or invent objects, textures, or props; only adjust the requested background, light, colour, surface, or mood.';

const DIRECTION_TERMS: ReadonlyArray<{ pattern: RegExp; token: string }> = [
  { pattern: /(?<![\p{L}])(?:from\s+the\s+)?left(?![\p{L}])|(?<![\p{L}])слева(?![\p{L}])/iu, token: 'from the left' },
  { pattern: /(?<![\p{L}])(?:from\s+the\s+)?right(?![\p{L}])|(?<![\p{L}])справа(?![\p{L}])/iu, token: 'from the right' },
  { pattern: /(?<![\p{L}])(?:from\s+)?above(?![\p{L}])|(?<![\p{L}])сверху(?![\p{L}])/iu, token: 'from above' },
  { pattern: /(?<![\p{L}])(?:from\s+)?below(?![\p{L}])|(?<![\p{L}])снизу(?![\p{L}])/iu, token: 'from below' },
  { pattern: /(?<![\p{L}])(?:from\s+the\s+)?front(?![\p{L}])|(?<![\p{L}])спереди(?![\p{L}])/iu, token: 'from the front' },
  { pattern: /(?<![\p{L}])(?:from\s+)?behind(?![\p{L}])|(?<![\p{L}])сзади(?![\p{L}])/iu, token: 'from behind' },
];

const MOOD_TERMS: ReadonlyArray<{ pattern: RegExp; token: string }> = [
  { pattern: /(?<![\p{L}])warm(?:er)?(?![\p{L}])|(?<![\p{L}])тёпл\p{L}*/iu, token: 'warm' },
  { pattern: /(?<![\p{L}])cool(?:er)?(?![\p{L}])|(?<![\p{L}])холодн\p{L}*/iu, token: 'cool' },
  { pattern: /(?<![\p{L}])soft(?![\p{L}])|(?<![\p{L}])мягк\p{L}*/iu, token: 'soft' },
  { pattern: /(?<![\p{L}])dramatic(?![\p{L}])|(?<![\p{L}])драматич\p{L}*/iu, token: 'dramatic' },
  { pattern: /(?<![\p{L}])calm(?![\p{L}])|(?<![\p{L}])спокойн\p{L}*/iu, token: 'calm' },
  { pattern: /(?<![\p{L}])evening(?![\p{L}])|(?<![\p{L}])вечерн\p{L}*/iu, token: 'evening' },
];

const SURFACE_TERMS: ReadonlyArray<{ pattern: RegExp; token: string }> = [
  { pattern: /(?<![\p{L}])matte(?![\p{L}])|(?<![\p{L}])матов\p{L}*/iu, token: 'matte' },
  { pattern: /(?<![\p{L}])(?:glossy|shiny)(?![\p{L}])|(?<![\p{L}])глянц\p{L}*/iu, token: 'glossy' },
  { pattern: /(?<![\p{L}])satin(?![\p{L}])|(?<![\p{L}])сатин\p{L}*/iu, token: 'satin' },
  { pattern: /(?<![\p{L}])smooth(?![\p{L}])|(?<![\p{L}])гладк\p{L}*/iu, token: 'smooth' },
  { pattern: /(?<![\p{L}])textured(?![\p{L}])|(?<![\p{L}])фактур\p{L}*/iu, token: 'textured' },
];

function matchedTokens(text: string, terms: ReadonlyArray<{ pattern: RegExp; token: string }>): string[] {
  return terms.filter(({ pattern }) => pattern.test(text)).map(({ token }) => token);
}

export function canonicalUserDescriptors(instruction: string): string[] {
  const directions = matchedTokens(instruction, DIRECTION_TERMS);
  const moods = matchedTokens(instruction, MOOD_TERMS);
  const surfaces = matchedTokens(instruction, SURFACE_TERMS);
  return [
    ...(directions.length ? [`Canonical lighting direction: ${directions.join(', ')}.`] : []),
    ...(moods.length ? [`Canonical requested mood: ${moods.join(', ')}.`] : []),
    ...(surfaces.length ? [`Canonical requested surface: ${surfaces.join(', ')}.`] : []),
  ];
}

export async function normalizeSceneGuidance(
  guidance: string,
  translate: (guidance: string) => Promise<string>,
): Promise<string> {
  const text = guidance.trim();
  if (!/\p{Script=Cyrillic}/u.test(text)) return text;

  const translated = (await translate(text)).trim();
  if (!translated || translated.length > 2000 || /\p{Script=Cyrillic}/u.test(translated)) {
    throw new Error('SCENE_TRANSLATION_INVALID');
  }
  return translated;
}
