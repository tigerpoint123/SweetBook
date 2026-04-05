/** 채택 미리보기 페이지 네비: 1→라벨 0, 짝수 장→페이지÷2 */

export function navTargetPages(total: number): number[] {
  if (total < 1) return [];
  const out = [1];
  for (let p = 2; p <= total; p += 2) {
    out.push(p);
  }
  return out;
}

export function navLabelForTarget(target1Based: number): string {
  if (target1Based === 1) return "0";
  return String(target1Based / 2);
}

export function navButtonIsActive(
  target1Based: number,
  current1Based: number,
  total: number
): boolean {
  if (target1Based === 1) return current1Based === 1;
  if (target1Based % 2 === 0) {
    return (
      current1Based === target1Based ||
      (current1Based === target1Based + 1 && target1Based + 1 <= total)
    );
  }
  return false;
}
