import { describe, expect, it } from 'vitest';
import { resolveColor } from '../src/colors.js';
import { falImageInput } from '../src/providers/fal.js';

describe('resolveColor', () => {
  it.each([
    ['хочу красный', { name: 'red', hex: '#FF0000', matched: 'красный' }],
    ['red', { name: 'red', hex: '#FF0000', matched: 'red' }],
    ['КРАСНЫЙ', { name: 'red', hex: '#FF0000', matched: 'КРАСНЫЙ' }],
    ['красного цвета', { name: 'red', hex: '#FF0000', matched: 'красного' }],
  ])('resolves red in %s', (phrase, expected) => {
    expect(resolveColor(phrase)).toEqual(expected);
  });

  it('resolves dark blue to navy with a darker hue than blue', () => {
    expect(resolveColor('make it dark blue')).toEqual({
      name: 'navy', hex: '#000080', matched: 'dark blue',
    });
    expect(resolveColor('тёмно-синий')).toEqual({
      name: 'navy', hex: '#000080', matched: 'тёмно-синий',
    });
    expect(Number.parseInt(resolveColor('dark blue')?.hex.slice(1) ?? 'FFFFFF', 16))
      .toBeLessThan(Number.parseInt(resolveColor('blue')?.hex.slice(1) ?? '000000', 16));
  });

  it('passes through a raw hex colour as custom', () => {
    expect(resolveColor('#00FF00')).toEqual({
      name: 'custom', hex: '#00FF00', matched: '#00FF00',
    });
    expect(resolveColor('00ff00')).toEqual({
      name: 'custom', hex: '#00FF00', matched: '00ff00',
    });
  });

  it.each(['', null, 'redesign the layout'])('does not invent a colour for %s', (phrase) => {
    expect(resolveColor(phrase)).toBeNull();
  });

  it('applies mixed-language bright modifiers deterministically', () => {
    expect(resolveColor('хочу bright red')).toEqual({
      name: 'red', hex: '#FF4040', matched: 'bright red',
    });
  });

  it('recognizes Russian grammatical forms and English colour names', () => {
    expect(resolveColor('светло-голубую')).toEqual({
      name: 'light blue', hex: '#C2E2EC', matched: 'светло-голубую',
    });
    expect(resolveColor('серебристого корпуса')?.name).toBe('silver');
    expect(resolveColor('gray case')?.hex).toBe('#808080');
  });

  it.each([
    ['orange', 'orange'], ['оранжевого', 'orange'],
    ['yellow', 'yellow'], ['жёлтый', 'yellow'], ['желтого', 'yellow'],
    ['green', 'green'], ['зелёной', 'green'], ['зеленого', 'green'],
    ['blue', 'blue'], ['голубой', 'light blue'],
    ['turquoise', 'turquoise'], ['бирюзовую', 'turquoise'],
    ['purple', 'purple'], ['фиолетового', 'purple'],
    ['violet', 'violet'], ['сиреневой', 'violet'],
    ['pink', 'pink'], ['розовую', 'pink'],
    ['brown', 'brown'], ['коричневого', 'brown'],
    ['beige', 'beige'], ['бежевую', 'beige'],
    ['black', 'black'], ['чёрную', 'black'], ['черного', 'black'],
    ['white', 'white'], ['белой', 'white'],
    ['gray', 'gray'], ['серого', 'gray'],
    ['gold', 'gold'], ['золотую', 'gold'],
    ['silver', 'silver'], ['серебристого', 'silver'],
    ['bronze', 'bronze'], ['бронзовой', 'bronze'],
  ])('recognizes %s as %s', (phrase, name) => {
    expect(resolveColor(phrase)?.name).toBe(name);
  });
});

describe('O-10 colour prompts', () => {
  const source = Buffer.from('phone');

  it('pins a Russian requested colour while retaining the seller phrase', () => {
    const prompt = String(falImageInput('O-10', source, { prompt: 'хочу красный' }).prompt);

    expect(prompt).toContain('Set the product surface to exactly #FF0000 (red). This exact hue must be visible in the result.');
    expect(prompt).toContain('"хочу красный"');
    expect(prompt).not.toContain('neutral white');
  });

  it('does not invent white when guidance has no recognized colour', () => {
    const prompt = String(falImageInput('O-10', source, { prompt: 'make the finish glossy' }).prompt);

    expect(prompt).toContain('"make the finish glossy"');
    expect(prompt).not.toContain('neutral white');
  });

  it('uses the neutral white fallback only when guidance is empty', () => {
    const prompt = String(falImageInput('O-10', source, {}).prompt);

    expect(prompt).toContain('If no colour is specified, create a neutral white colour variant.');
  });

  // A long instruction is the user's own words and must be shortened, never refused: throwing
  // turns "I wrote a lot" into a failed paid job. The fixed constraints must survive the cut.
  it('shortens an over-long instruction instead of rejecting the request', () => {
    const prompt = String(falImageInput('O-10', source, { prompt: 'a'.repeat(2000) }).prompt);

    expect(prompt.length).toBeLessThanOrEqual(2000);
    expect(prompt).toContain('Preserve the product identity');
  });

  it('adds background preservation to O-08 without changing O-09 negatives', () => {
    const relight = falImageInput('O-08', source, {});
    const extend = falImageInput('O-09', source, { canvas_size: [1200, 1600] });

    expect(relight['negative_prompt']).toContain('replaced background, new scene, different setting');
    expect(extend['negative_prompt']).toBe(
      'unrelated subject, replacement product, changed product shape, added people, added animals, invented text, distorted logo',
    );
  });
});
