import { useEffect, useState } from 'react'
import { api } from './api'
import { formatMoney } from './money'

// Build a new order: pick a customer, add product lines, see the total, save.
// The total shown here is only a preview: the server works out the real total from its own prices.
export default function NewOrderForm({ onSaved, onCancel }) {
  const [customers, setCustomers] = useState([])
  const [products, setProducts] = useState([])
  const [loadError, setLoadError] = useState('')
  const [loading, setLoading] = useState(true)

  const [customerId, setCustomerId] = useState('')
  const [lines, setLines] = useState([]) // { productId, quantity }
  const [pickProductId, setPickProductId] = useState('')
  const [pickQuantity, setPickQuantity] = useState('1')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    let cancelled = false
    Promise.all([api.get('/api/customers'), api.get('/api/products')])
      .then(([c, p]) => {
        if (!cancelled) {
          setCustomers(c)
          setProducts(p)
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

  const productById = (id) => products.find((p) => p.id === Number(id))

  function handleAddLine() {
    setError('')
    if (!pickProductId) return setError('Choose a product to add.')
    if (!/^\d+$/.test(pickQuantity.trim()) || Number(pickQuantity) < 1) {
      return setError('Every quantity must be at least 1.')
    }
    const productId = Number(pickProductId)
    const quantity = Number(pickQuantity)
    // The same product twice is merged into one line (the server refuses duplicates).
    setLines((current) => {
      const existing = current.find((l) => l.productId === productId)
      if (existing) {
        return current.map((l) => (l.productId === productId ? { ...l, quantity: l.quantity + quantity } : l))
      }
      return [...current, { productId, quantity }]
    })
    setPickProductId('')
    setPickQuantity('1')
  }

  function handleRemoveLine(productId) {
    setLines((current) => current.filter((l) => l.productId !== productId))
  }

  const previewTotal = lines.reduce((sum, l) => {
    const p = productById(l.productId)
    return sum + (p ? p.priceCents * l.quantity : 0)
  }, 0)

  async function handleSave() {
    setError('')
    if (!customerId) return setError('Please choose a customer.')
    if (lines.length === 0) return setError('Add at least one product to the order.')
    setBusy(true)
    try {
      const created = await api.post('/api/orders', {
        customerId: Number(customerId),
        lines: lines.map((l) => ({ productId: l.productId, quantity: l.quantity })),
      })
      onSaved(created.order.id)
    } catch (e) {
      // For example: Only 3 in stock for "Milk 1L" (you asked for 5).
      setError(e.message)
      setBusy(false)
    }
  }

  if (loading) return <p className="muted">Loading customers and products...</p>
  if (loadError) return <div className="error">{loadError}</div>

  return (
    <div
      className="form-card"
      onKeyDown={(e) => {
        if (e.key === 'Escape') onCancel()
      }}
    >
      <h3>New order</h3>

      <label>
        Customer
        <select value={customerId} onChange={(e) => setCustomerId(e.target.value)} autoFocus>
          <option value="">Choose a customer...</option>
          {customers.map((c) => (
            <option key={c.id} value={c.id}>
              {c.firstName} {c.lastName}
            </option>
          ))}
        </select>
      </label>

      <div className="line-adder">
        <label>
          Product
          <select value={pickProductId} onChange={(e) => setPickProductId(e.target.value)}>
            <option value="">Choose a product...</option>
            {products.map((p) => (
              <option key={p.id} value={p.id} disabled={p.quantityInStock === 0}>
                {p.productName} ({formatMoney(p.priceCents)}, {p.quantityInStock} in stock)
              </option>
            ))}
          </select>
        </label>
        <label>
          Quantity
          <input value={pickQuantity} onChange={(e) => setPickQuantity(e.target.value)} />
        </label>
        <button type="button" onClick={handleAddLine}>
          Add line
        </button>
      </div>

      {lines.length === 0 ? (
        <p className="muted">No products added yet.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Product</th>
              <th className="number">Quantity</th>
              <th className="number">Unit price</th>
              <th className="number">Subtotal</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {lines.map((l) => {
              const p = productById(l.productId)
              return (
                <tr key={l.productId}>
                  <td>{p ? p.productName : 'Unknown product'}</td>
                  <td className="number">{l.quantity}</td>
                  <td className="number">{p ? formatMoney(p.priceCents) : ''}</td>
                  <td className="number">{p ? formatMoney(p.priceCents * l.quantity) : ''}</td>
                  <td className="actions">
                    <button className="danger" onClick={() => handleRemoveLine(l.productId)}>
                      Remove
                    </button>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      )}

      <p className="order-total">
        Total: <strong>{formatMoney(previewTotal)}</strong>
      </p>

      {error && <div className="error">{error}</div>}

      <div className="form-actions">
        <button type="button" onClick={handleSave} disabled={busy}>
          {busy ? 'Saving...' : 'Save order'}
        </button>
        <button type="button" className="secondary" onClick={onCancel}>
          Cancel
        </button>
      </div>
    </div>
  )
}
