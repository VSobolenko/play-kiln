export type ResolvedColor = {
  name: string;
  hex: string;
  /** The exact substring of the input that produced the match. */
  matched: string;
};

type ColorDefinition = {
  name: string;
  hex: string;
  aliases: string[];
};

type ColorModifier = 'light' | 'dark' | 'bright';
type Candidate = {
  pattern: RegExp;
  name: string;
  hex: string;
  modifier?: ColorModifier;
};

const RUSSIAN_ENDINGS: Readonly<Record<'ый' | 'ий' | 'ой', string[]>> = {
  'ый': ['ый', 'ого', 'ому', 'ым', 'ом', 'ая', 'ую', 'ой', 'ою', 'ое', 'ые', 'ых', 'ым', 'ыми'],
  'ий': ['ий', 'его', 'ему', 'им', 'ем', 'яя', 'юю', 'ей', 'ее', 'ие', 'их', 'им', 'ими'],
  'ой': ['ой', 'ого', 'ому', 'ым', 'ом', 'ая', 'ую', 'ой', 'ою', 'ое', 'ые', 'ых', 'ым', 'ыми'],
};

function russianForms(stem: string, ending: 'ый' | 'ий' | 'ой'): string[] {
  return RUSSIAN_ENDINGS[ending].map((suffix) => `${stem}${suffix}`);
}

const COLORS: ColorDefinition[] = [
  { name: 'red', hex: '#FF0000', aliases: ['red', ...russianForms('красн', 'ый')] },
  { name: 'orange', hex: '#FFA500', aliases: ['orange', ...russianForms('оранжев', 'ый')] },
  { name: 'yellow', hex: '#FFFF00', aliases: ['yellow', ...russianForms('желт', 'ый')] },
  { name: 'green', hex: '#008000', aliases: ['green', ...russianForms('зелен', 'ый')] },
  { name: 'blue', hex: '#0000FF', aliases: ['blue', ...russianForms('син', 'ий')] },
  {
    name: 'light blue',
    hex: '#ADD8E6',
    aliases: ['light blue', ...russianForms('голуб', 'ой')],
  },
  {
    name: 'navy',
    hex: '#000080',
    aliases: ['navy', 'dark blue', 'тёмно-синий', 'темно-синий'],
  },
  { name: 'turquoise', hex: '#40E0D0', aliases: ['turquoise', ...russianForms('бирюзов', 'ый')] },
  { name: 'purple', hex: '#800080', aliases: ['purple', ...russianForms('фиолетов', 'ый')] },
  { name: 'violet', hex: '#EE82EE', aliases: ['violet', ...russianForms('сиренев', 'ый')] },
  { name: 'pink', hex: '#FFC0CB', aliases: ['pink', ...russianForms('розов', 'ый')] },
  { name: 'brown', hex: '#A52A2A', aliases: ['brown', ...russianForms('коричнев', 'ый')] },
  { name: 'beige', hex: '#F5F5DC', aliases: ['beige', ...russianForms('бежев', 'ый')] },
  { name: 'black', hex: '#000000', aliases: ['black', ...russianForms('чёрн', 'ый'), ...russianForms('черн', 'ый')] },
  { name: 'white', hex: '#FFFFFF', aliases: ['white', ...russianForms('бел', 'ый')] },
  { name: 'gray', hex: '#808080', aliases: ['gray', ...russianForms('сер', 'ый')] },
  { name: 'gold', hex: '#FFD700', aliases: ['gold', ...russianForms('золот', 'ой')] },
  { name: 'silver', hex: '#C0C0C0', aliases: ['silver', ...russianForms('серебрист', 'ый')] },
  { name: 'bronze', hex: '#CD7F32', aliases: ['bronze', ...russianForms('бронзов', 'ый')] },
];

const MODIFIERS: ReadonlyArray<{ words: string[]; modifier: ColorModifier }> = [
  { words: ['light', 'светло', 'бледно'], modifier: 'light' },
  { words: ['dark', 'deep', 'тёмно', 'темно', ...russianForms('глубок', 'ий')], modifier: 'dark' },
  { words: ['bright', 'ярко'], modifier: 'bright' },
];

function escapedTerm(term: string): string {
  return term.split(/[\s-]+/u).map((part) => part.replace(/[.*+?^${}()|[\]\\]/gu, '\\$&')).join('[\\s-]+');
}

function termPattern(terms: string[]): RegExp {
  const alternatives = [...new Set(terms)].sort((left, right) => right.length - left.length)
    .map(escapedTerm).join('|');
  return new RegExp(`(?<![\\p{L}\\p{N}])(?:${alternatives})(?![\\p{L}\\p{N}])`, 'giu');
}

function candidates(): Candidate[] {
  const direct = COLORS.map((color) => ({
    pattern: termPattern(color.aliases),
    name: color.name,
    hex: color.hex,
  }));
  const modified: Candidate[] = [];
  for (const color of COLORS) {
    if (color.name === 'navy') continue;
    for (const { words, modifier } of MODIFIERS) {
      const combinations = words.flatMap((word) =>
        color.aliases.map((alias) => `${word} ${alias}`));
      modified.push({
        pattern: termPattern(combinations),
        name: color.name,
        hex: color.hex,
        modifier,
      });
      if (color.name === 'blue' && modifier === 'dark') {
        const candidate = modified[modified.length - 1];
        if (candidate) modified[modified.length - 1] = {
          ...candidate,
          name: 'navy',
          hex: '#000080',
          modifier: undefined,
        };
      }
    }
  }
  return [...direct, ...modified];
}

const COLOR_CANDIDATES = candidates();

function adjustHex(hex: string, modifier: ColorModifier): string {
  const amount = modifier === 'dark' ? 0 : 255;
  // Light and bright mix 25% toward white; dark mixes 25% toward black.
  const ratio = 0.25;
  const channels = [1, 3, 5].map((offset) => {
    const value = Number.parseInt(hex.slice(offset, offset + 2), 16);
    return Math.round(value + (amount - value) * ratio).toString(16).padStart(2, '0');
  });
  return `#${channels.join('')}`.toUpperCase();
}

export function resolveColor(phrase: string | null | undefined): ResolvedColor | null {
  if (!phrase) return null;
  const normalized = phrase.toLowerCase().replaceAll('ё', 'е');
  const rawHex = /(?<![\p{L}\p{N}#])#?[0-9a-f]{6}(?![\p{L}\p{N}])/giu.exec(normalized);
  if (rawHex) {
    return {
      name: 'custom',
      hex: rawHex[0].startsWith('#') ? rawHex[0].toUpperCase() : `#${rawHex[0].toUpperCase()}`,
      matched: phrase.slice(rawHex.index, rawHex.index + rawHex[0].length),
    };
  }

  let best: { match: RegExpExecArray; candidate: Candidate } | undefined;
  for (const candidate of COLOR_CANDIDATES) {
    candidate.pattern.lastIndex = 0;
    const match = candidate.pattern.exec(normalized);
    if (match && (!best || match[0].length > best.match[0].length)) best = { match, candidate };
  }
  if (!best) return null;

  const { match, candidate } = best;
  const matched = phrase.slice(match.index, match.index + match[0].length);
  return {
    name: candidate.name,
    hex: candidate.modifier ? adjustHex(candidate.hex, candidate.modifier) : candidate.hex,
    matched,
  };
}
