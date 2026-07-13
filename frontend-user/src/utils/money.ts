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
  const code = (currency || 'CNY').toUpperCase()
  const value = Number(amount ?? 0)
  const formatted = value.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
  const symbol = CURRENCY_SYMBOL[code]
  return symbol ? `${symbol}${formatted}` : `${formatted} ${code}`
}
