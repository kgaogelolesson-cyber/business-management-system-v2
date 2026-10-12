import { useEffect, useState } from 'react'
import { api } from './api'
import { formatMoney } from './money'

// One order with its lines. The prices shown are the prices AT THE TIME of the sale.
export default function OrderDetail({ orderId, onClose }) {
  const [detail, setDetail] = useState(null) // null = still loading
  const [error, setError] = useState('')

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
        </>
      )}
    </div>
  )
}
