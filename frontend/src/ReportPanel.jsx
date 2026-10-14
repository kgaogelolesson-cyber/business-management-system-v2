import { useEffect, useState } from 'react'
import { api } from './api'
import { downloadCsv } from './reportCsv'

// The reports screen (administrators only; the server also refuses everyone else).
// Pick a report, choose dates if it needs them, click "Run report", read the table, optionally export it.

function localDate(date) {
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return date.getFullYear() + '-' + month + '-' + day
}

function formatCents(cents, symbol) {
  return symbol + Math.floor(cents / 100).toLocaleString('en-US') + '.' + String(cents % 100).padStart(2, '0')
}

export default function ReportPanel() {
  const today = new Date()
  const [types, setTypes] = useState([])
  const [loadError, setLoadError] = useState('')
  const [loading, setLoading] = useState(true)
  const [typeKey, setTypeKey] = useState('')
  const [from, setFrom] = useState(localDate(new Date(today.getFullYear(), today.getMonth(), 1)))
  const [to, setTo] = useState(localDate(today))
  const [report, setReport] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    let cancelled = false
    api
      .get('/api/reports')
      .then((items) => {
        if (!cancelled) {
          setTypes(items)
          setTypeKey(items.length > 0 ? items[0].key : '')
        }
      })
      .catch((e) => {
        if (!cancelled) setLoadError(e.message)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  const selected = types.find((t) => t.key === typeKey)

  async function handleRun(event) {
    event.preventDefault()
    setError('')
    setBusy(true)
    const params = new URLSearchParams()
    if (selected?.needsDates) {
      params.set('from', from)
      params.set('to', to)
    }
    const query = params.toString() ? '?' + params.toString() : ''
    try {
      setReport(await api.get('/api/reports/' + typeKey + query))
    } catch (e) {
      setError(e.message)
      setReport(null)
    } finally {
      setBusy(false)
    }
  }

  if (loading) return <p className="muted">Loading reports...</p>
  if (loadError) return <div className="error">{loadError}</div>

  return (
    <section>
      <div className="toolbar">
        <div>
          <h2>Reports</h2>
          <p className="muted">Sales reports count completed orders only. Cancelled orders are not counted.</p>
        </div>
      </div>

      <form className="search-bar report-bar" onSubmit={handleRun}>
        <label>
          Report
          <select value={typeKey} onChange={(e) => setTypeKey(e.target.value)}>
            {types.map((t) => (
              <option key={t.key} value={t.key}>
                {t.label}
              </option>
            ))}
          </select>
        </label>
        {selected?.needsDates && (
          <>
            <label>
              From
              <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
            </label>
            <label>
              To
              <input type="date" value={to} onChange={(e) => setTo(e.target.value)} />
            </label>
          </>
        )}
        <button type="submit" disabled={busy}>
          {busy ? 'Running...' : 'Run report'}
        </button>
        <button type="button" className="secondary" disabled={!report} onClick={() => downloadCsv(report)}>
          Export to CSV
        </button>
      </form>

      {error && <div className="error">{error}</div>}

      {report && (
        <>
          <h3>{report.title}</h3>
          <p className="report-summary">{report.summary}</p>
          {report.rows.length === 0 ? (
            <p>There is nothing to show for this report.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  {report.columns.map((c) => (
                    <th key={c.header} className={c.type === 'TEXT' ? '' : 'number'}>
                      {c.header}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {report.rows.map((row, r) => (
                  <tr key={r}>
                    {row.map((value, c) => {
                      const type = report.columns[c].type
                      return (
                        <td key={c} className={type === 'TEXT' ? '' : 'number'}>
                          {type === 'MONEY' ? formatCents(value, report.currencySymbol) : value}
                        </td>
                      )
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}
    </section>
  )
}
