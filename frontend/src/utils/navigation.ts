export function getSafeRedirect(value: unknown, fallback = '/') {
  if (
    typeof value !== 'string' ||
    !value.startsWith('/') ||
    value.startsWith('//') ||
    value.includes('\\') ||
    /[\u0000-\u001f\u007f]/.test(value)
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
