import { useEffect, useState } from 'react'
import { api } from './api'

// The stock history of ONE product, newest first (the backend sends at most the newest 200 lines).
export default function HistoryPanel({ item, onClose }) {
  const [rows, setRows] = useState(null) // null = still loading
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    api
      .get('/api/inventory/' + item.id + '/history')
      .then((data) => {
        if (!cancelled) setRows(data)
      })
      .catch((e) => {
        if (!cancelled) setError(e.message)
      })
    return () => {
      cancelled = true
    }
  }, [item.id])

  return (
    <div className="form-card">
      <div className="panel-head">
        <h3>Stock history: {item.productName}</h3>
        <button className="secondary" onClick={onClose}>
          Close
        </button>
      </div>

      {error && <div className="error">{error}</div>}
      {!error && rows === null && <p className="muted">Loading history...</p>}
      {rows && rows.length === 0 && <p>No stock adjustments have been recorded for this product yet.</p>}
      {rows && rows.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>When</th>
              <th className="number">Change</th>
              <th>Reason</th>
              <th>By</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r, i) => (
              <tr key={i}>
                <td>{r.adjustedAt.replace('T', ' ').slice(0, 16)}</td>
                <td className={'number ' + (r.quantityChange > 0 ? 'change-add' : 'change-remove')}>
                  {r.quantityChange > 0 ? '+' + r.quantityChange : r.quantityChange}
                </td>
                <td>{r.reason}</td>
                <td>{r.adjustedBy}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
