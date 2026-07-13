const CURRENCY_SYMBOL: Record<string, string> = {
  CNY: '¥',
  USD: '$',
  EUR: '€',
  GBP: '£',
  HKD: 'HK$',
  TWD: 'NT$',
  JPY: '¥',
}

export function formatMoney(amount: number | string | null | undefined, currency?: string | null) {
  if (amount == null || amount === '') return '-'
  const code = (currency || 'CNY').toUpperCase()
  const value = Number(amount)
  const formatted = value.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
  const symbol = CURRENCY_SYMBOL[code]
  return symbol ? `${symbol}${formatted}` : `${formatted} ${code}`
}

export function formatSignedMoney(amount: number | string | null | undefined, currency = 'CNY') {
  if (amount == null || amount === '') return '-'
  const numeric = Number(amount)
  const sign = numeric > 0 ? '+' : ''
  return `${sign}${formatMoney(numeric, currency)}`
}
