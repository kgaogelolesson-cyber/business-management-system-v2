import { useState } from 'react'
import { api } from './api'

// Add or remove stock for ONE product. The person picks "Add" or "Remove" and types a positive quantity;
// we turn that into the signed number the backend expects (+10 or -10). A reason is always required.
export default function AdjustForm({ item, onSaved, onCancel }) {
  const [mode, setMode] = useState('add') // 'add' or 'remove'
  const [quantity, setQuantity] = useState('')
  const [reason, setReason] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    if (!/^\d+$/.test(quantity.trim()) || Number(quantity) < 1) {
      return setError('The quantity must be at least 1.')
    }
    const amount = Number(quantity)
    setBusy(true)
    try {
      await api.post('/api/inventory/' + item.id + '/adjust', {
        change: mode === 'add' ? amount : -amount,
        reason,
      })
      onSaved()
    } catch (e) {
      // For example: "Only 3 in stock, so you cannot remove 5."
      setError(e.message)
      setBusy(false)
    }
  }

  return (
    <form
      className="form-card"
      onSubmit={handleSubmit}
      onKeyDown={(e) => {
        if (e.key === 'Escape') onCancel()
      }}
    >
      <h3>Adjust stock: {item.productName}</h3>
      <p className="hint">
        In stock now: <strong>{item.quantityInStock}</strong>
      </p>

      <div className="choice-row">
        <label>
          <input type="radio" name="mode" checked={mode === 'add'} onChange={() => setMode('add')} />
          Add stock
        </label>
        <label>
          <input type="radio" name="mode" checked={mode === 'remove'} onChange={() => setMode('remove')} />
          Remove stock
        </label>
      </div>

      <label>
        Quantity
        <input value={quantity} onChange={(e) => setQuantity(e.target.value)} autoFocus />
      </label>
      <label>
        Reason (for example: Delivery received, Damaged goods, Stock count)
        <input value={reason} onChange={(e) => setReason(e.target.value)} />
      </label>

      {error && <div className="error">{error}</div>}

      <div className="form-actions">
        <button type="submit" disabled={busy}>
          {busy ? 'Saving...' : 'Save'}
        </button>
        <button type="button" className="secondary" onClick={onCancel}>
          Cancel
        </button>
      </div>
    </form>
  )
}
