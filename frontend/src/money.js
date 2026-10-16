// Money is kept as whole cents in the database and the API (R159.00 = 15900), so there are no rounding errors.
// These two helpers convert between cents and the rand text a person types or reads.

// The symbol shown in front of amounts. It is loaded from the server after login (see App.jsx),
// and the administrator can change it on the Settings tab. "R" is only used until it has loaded.
let symbol = 'R'

export function setCurrencySymbol(newSymbol) {
  symbol = newSymbol
}

/** 15900 -> "R159.00", 123456789 -> "R1,234,567.89" (with whatever symbol is set) */
export function formatMoney(cents) {
  const whole = Math.floor(cents / 100)
  const rest = String(cents % 100).padStart(2, '0')
  return symbol + whole.toLocaleString('en-US') + '.' + rest
}

/** "159", "159.5", "159,50" -> cents (15900, 15950, 15950). Returns null when the text is not a valid amount. */
export function randsToCents(text) {
  const t = text.trim()
  if (!/^\d+([.,]\d{1,2})?$/.test(t)) return null
  const [whole, fraction = ''] = t.replace(',', '.').split('.')
  return Number(whole) * 100 + Number(fraction.padEnd(2, '0'))
}

/** 15900 -> "159.00" (for filling the edit form). */
export function centsToRands(cents) {
  return Math.floor(cents / 100) + '.' + String(cents % 100).padStart(2, '0')
}
