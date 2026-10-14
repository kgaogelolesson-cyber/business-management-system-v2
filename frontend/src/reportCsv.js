// Turns a finished report into CSV text and saves it as a file, like Version 1's "Export to CSV".
// Money is written as plain numbers with two decimals (684.98), without the currency symbol,
// so a spreadsheet can calculate with it.

function centsToPlain(cents) {
  return Math.floor(cents / 100) + '.' + String(cents % 100).padStart(2, '0')
}

function cell(value, type) {
  if (value === null || value === undefined) return ''
  if (type === 'MONEY') return centsToPlain(value)
  if (type === 'NUMBER') return String(value)
  let text = String(value)
  // A text cell that starts with = + - or @ could be run as a formula by a spreadsheet.
  // A leading apostrophe makes it plain text instead.
  if (text !== '' && '=+-@'.includes(text[0])) text = "'" + text
  if (/[,"\r\n]/.test(text)) text = '"' + text.replace(/"/g, '""') + '"'
  return text
}

export function reportToCsv(report) {
  const lines = [cell(report.title, 'TEXT')]
  lines.push(report.columns.map((c) => cell(c.header, 'TEXT')).join(','))
  for (const row of report.rows) {
    lines.push(row.map((value, i) => cell(value, report.columns[i].type)).join(','))
  }
  return lines.join('\r\n') + '\r\n'
}

/** Starts a download in the browser. The file starts with a UTF-8 marker so Excel shows accents correctly. */
export function downloadCsv(report) {
  const blob = new Blob(['﻿' + reportToCsv(report)], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = report.title.replace(/[^A-Za-z0-9._-]+/g, '_') + '.csv'
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
