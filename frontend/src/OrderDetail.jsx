import { useEffect, useState } from 'react'
import { api } from './api'
import { formatMoney } from './money'

// One order with its lines. The prices shown are the prices AT THE TIME of the sale.
// An administrator also gets a "Cancel order" button on a completed order.
export default function OrderDetail({ orderId, user, onClose, onChanged }) {
  const [detail, setDetail] = useState(null) // null = still loading
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')
  const [busy, setBusy] = useState(false)

  const isAdmin = user && user.role === 'ADMIN'

  async function handleCancel() {
    if (
      !window.confirm(
        'Cancel order #' + orderId + '? Its products will be put back into stock. This cannot be undone.'
      )
    ) {
      return
    }
    setActionError('')
    setBusy(true)
    try {
      const updated = await api.post('/api/orders/' + orderId + '/cancel')
      setDetail(updated)
      onChanged() // makes the list behind this panel load again
    } catch (e) {
      // For example: Order #4 is already cancelled.
      setActionError(e.message)
    } finally {
      setBusy(false)
    }
  }

  useEffect(() => {
    let cancelled = false
    api
      .get('/api/orders/' + orderId)
      .then((data) => {
        if (!cancelled) setDetail(data)
      })
      .catch((e) => {
        if (!cancelled) setError(e.message)
      })
    return () => {
      cancelled = true
    }
  }, [orderId])

  return (
    <div className="form-card">
      <div className="panel-head">
        <h3>Order #{orderId}</h3>
        <button className="secondary" onClick={onClose}>
          Close
        </button>
      </div>

      {error && <div className="error">{error}</div>}
      {!error && detail === null && <p className="muted">Loading order...</p>}

      {detail && (
        <>
          <p>
            <strong>{detail.order.customerName}</strong>
            <br />
            <span className="muted">
              {detail.order.orderDate.replace('T', ' ').slice(0, 16)} &middot; processed by{' '}
              {detail.order.employeeName} &middot;{' '}
              {detail.order.status === 'CANCELLED' ? 'CANCELLED' : 'COMPLETED'}
            </span>
          </p>
          {detail.cancellation && <div className="error">{detail.cancellation}</div>}
          {actionError && <div className="error">{actionError}</div>}

          <table>
            <thead>
              <tr>
                <th>Product</th>
                <th className="number">Quantity</th>
                <th className="number">Unit price</th>
                <th className="number">Subtotal</th>
              </tr>
            </thead>
            <tbody>
              {detail.lines.map((l) => (
                <tr key={l.productId}>
                  <td>{l.productName}</td>
                  <td className="number">{l.quantity}</td>
                  <td className="number">{formatMoney(l.unitPriceCents)}</td>
                  <td className="number">{formatMoney(l.subtotalCents)}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <p className="order-total">
            Total: <strong>{formatMoney(detail.order.totalCents)}</strong>
          </p>

          {isAdmin && detail.order.status === 'COMPLETED' && (
            <div className="form-actions">
              <button className="danger" onClick={handleCancel} disabled={busy}>
                {busy ? 'Cancelling...' : 'Cancel order'}
              </button>
            </div>
          )}
        </>
      )}
    </div>
  )
}
