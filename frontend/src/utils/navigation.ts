function containsControlCharacter(value: string) {
  return Array.from(value).some((character) => {
    const codePoint = character.codePointAt(0) ?? 0
    return codePoint <= 0x1f || codePoint === 0x7f
  })
}

export function getSafeRedirect(value: unknown, fallback = '/') {
  if (
    typeof value !== 'string' ||
    !value.startsWith('/') ||
    value.startsWith('//') ||
    value.includes('\\') ||
    containsControlCharacter(value)
  ) {
    return fallback
  }

  try {
    const base = new URL('https://xiaolan.local')
    const resolved = new URL(value, base)
    return resolved.origin === base.origin ? `${resolved.pathname}${resolved.search}${resolved.hash}` : fallback
  } catch {
    return fallback
  }
}
