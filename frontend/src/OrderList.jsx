import { useEffect, useState } from 'react'
import { api } from './api'
import { formatMoney } from './money'
import NewOrderForm from './NewOrderForm'
import OrderDetail from './OrderDetail'

// The orders screen. Everyone logged in can create orders. An administrator sees every order;
// an employee sees only their own (the server decides that, not this screen).
export default function OrderList({ user }) {
  const [orders, setOrders] = useState([])
  const [loadError, setLoadError] = useState('')
  const [loading, setLoading] = useState(true)
  const [panel, setPanel] = useState(null) // null, { type: 'new' } or { type: 'detail', id }
  const [reloadCount, setReloadCount] = useState(0)
  const [searchText, setSearchText] = useState('')
  const [appliedSearch, setAppliedSearch] = useState('')
  const [status, setStatus] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')

  const isAdmin = user.role === 'ADMIN'

  useEffect(() => {
    let cancelled = false
    const params = new URLSearchParams()
    if (appliedSearch) params.set('search', appliedSearch)
    if (status) params.set('status', status)
    if (from) params.set('from', from)
    if (to) params.set('to', to)
    const query = params.toString() ? '?' + params.toString() : ''
    api
      .get('/api/orders' + query)
      .then((data) => {
        if (!cancelled) {
          setOrders(data)
          setLoadError('')
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
  }, [reloadCount, appliedSearch, status, from, to])

  function handleSearch(event) {
    event.preventDefault()
    setAppliedSearch(searchText.trim())
  }

  function handleClear() {
    setSearchText('')
    setAppliedSearch('')
    setStatus('')
    setFrom('')
    setTo('')
  }

  function handleCreated(orderId) {
    setPanel({ type: 'detail', id: orderId })
    setReloadCount((n) => n + 1)
  }

  if (loading) return <p className="muted">Loading orders...</p>
  if (loadError) return <div className="error">{loadError}</div>

  const filtered = appliedSearch || status || from || to

  return (
    <section>
      <div className="toolbar">
        <div>
          <h2>Orders</h2>
          <p className="muted">
            {orders.length} order(s)
            {!isAdmin && ' (you see only the orders you processed)'}
            {orders.length === 1000 && ' (showing the newest 1,000; narrow the search to see older ones)'}
          </p>
        </div>
        <button onClick={() => setPanel({ type: 'new' })}>New order</button>
      </div>

      <form className="search-bar" onSubmit={handleSearch}>
        <input
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          placeholder="Search by order number, date, customer or employee"
        />
        <button type="submit">Search</button>
        <button type="button" className="secondary" onClick={handleClear}>
          Clear
        </button>
      </form>

      <div className="filter-row">
        <label>
          Status
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">All orders</option>
            <option value="COMPLETED">Completed</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
        </label>
        <label>
          From
          <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
        </label>
        <label>
          To
          <input type="date" value={to} onChange={(e) => setTo(e.target.value)} />
        </label>
      </div>

      {panel && panel.type === 'new' && (
        <NewOrderForm onSaved={handleCreated} onCancel={() => setPanel(null)} />
      )}
      {panel && panel.type === 'detail' && (
        <OrderDetail
          key={panel.id}
          orderId={panel.id}
          user={user}
          onClose={() => setPanel(null)}
          onChanged={() => setReloadCount((n) => n + 1)}
        />
      )}

      {orders.length === 0 ? (
        <p>{filtered ? 'No orders match your search.' : 'There are no orders yet.'}</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Order</th>
              <th>Date</th>
              <th>Customer</th>
              <th>Processed by</th>
              <th className="number">Total</th>
              <th>Status</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {orders.map((o) => (
              <tr
                key={o.id}
                className={o.status === 'CANCELLED' ? 'row-cancelled' : ''}
                onDoubleClick={() => setPanel({ type: 'detail', id: o.id })}
              >
                <td>#{o.id}</td>
                <td>{o.orderDate.replace('T', ' ').slice(0, 16)}</td>
                <td>{o.customerName}</td>
                <td>{o.employeeName}</td>
                <td className="number">{formatMoney(o.totalCents)}</td>
                <td>
                  <span className={'status ' + (o.status === 'CANCELLED' ? 'status-cancelled' : 'status-completed')}>
                    {o.status}
                  </span>
                </td>
                <td className="actions">
                  <button className="secondary" onClick={() => setPanel({ type: 'detail', id: o.id })}>
                    View
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}
