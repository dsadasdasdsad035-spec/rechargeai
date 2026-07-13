export function maskSecret(value?: string | null) {
  const trimmed = value?.trim()
  if (!trimmed) return ''
  if (trimmed.length <= 12) return '••••••'
  return `${trimmed.slice(0, 6)}...${trimmed.slice(-4)}`
}
