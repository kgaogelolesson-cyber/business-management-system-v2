import { useEffect, useState } from 'react'
import { api } from './api'

// The first screen after login. The server decides what each person may see:
// an administrator gets everything, an employee gets a limited view with only their own recent orders.

function formatCents(cents, symbol) {
  return symbol + Math.floor(cents / 100).toLocaleString('en-US') + '.' + String(cents % 100).padStart(2, '0')
}

function Card({ label, value, warn }) {
  return (
    <div className={warn ? 'stat-card stat-warn' : 'stat-card'}>
      <div className="stat-value">{value}</div>
      <div className="stat-label">{label}</div>
    </div>
  )
}

function stockClass(status) {
  return status === 'OUT OF STOCK' ? 'status status-out' : 'status status-low'
}

export default function DashboardPanel() {
  const [data, setData] = useState(null)
  const [loadError, setLoadError] = useState('')
  const [loading, setLoading] = useState(true)
  const [reloadCount, setReloadCount] = useState(0)

  useEffect(() => {
    let cancelled = false
    api
      .get('/api/dashboard')
      .then((d) => {
        if (!cancelled) {
          setData(d)
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
  }, [reloadCount])

  if (loading) return <p className="muted">Loading dashboard...</p>
  if (loadError) return <div className="error">{loadError}</div>

  const sym = data.currencySymbol

  return (
    <section>
      <div className="toolbar">
        <div>
          <h2>Dashboard</h2>
          <p className="muted">
            {data.admin
              ? 'A summary of the whole business. Cancelled orders are not counted.'
              : 'A summary for you. You see your own recent orders only.'}
          </p>
        </div>
        <button className="secondary" onClick={() => setReloadCount((n) => n + 1)}>
          Refresh
        </button>
      </div>

      <div className="stat-grid">
        <Card label="Customers" value={data.customers} />
        <Card label="Products" value={data.products} />
        {data.admin && <Card label="Employees" value={data.employees} />}
        {data.admin && <Card label="Completed orders" value={data.completedOrders} />}
        {data.admin && <Card label="Revenue" value={formatCents(data.revenueCents, sym)} />}
        <Card label="Low or out of stock" value={data.lowStockCount} warn={data.lowStockCount > 0} />
      </div>

      <div className="dash-lists">
        <div>
          <h3>Low stock</h3>
          {data.lowStockItems.length === 0 ? (
            <p>All products are well stocked.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Product</th>
                  <th className="number">In stock</th>
                  <th className="number">Reorder at</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {data.lowStockItems.map((p) => (
                  <tr key={p.productName}>
                    <td>{p.productName}</td>
                    <td className="number">{p.quantityInStock}</td>
                    <td className="number">{p.reorderLevel}</td>
                    <td>
                      <span className={stockClass(p.stockStatus)}>{p.stockStatus}</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          {data.lowStockCount > data.lowStockItems.length && (
            <p className="muted">
              Showing {data.lowStockItems.length} of {data.lowStockCount}. See the Inventory tab for all.
            </p>
          )}
        </div>

        <div>
          <h3>{data.admin ? 'Recent orders' : 'My recent orders'}</h3>
          {data.recentOrders.length === 0 ? (
            <p>There are no orders to show yet.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>#</th>
                  <th>Date</th>
                  <th>Customer</th>
                  {data.admin && <th>Employee</th>}
                  <th className="number">Total</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {data.recentOrders.map((o) => (
                  <tr key={o.orderId} className={o.status === 'CANCELLED' ? 'row-cancelled' : ''}>
                    <td>{o.orderId}</td>
                    <td>{o.orderDate}</td>
                    <td>{o.customerName}</td>
                    {data.admin && <td>{o.employeeName}</td>}
                    <td className="number">{formatCents(o.totalCents, sym)}</td>
                    <td>
                      <span
                        className={'status ' + (o.status === 'CANCELLED' ? 'status-cancelled' : 'status-completed')}
                      >
                        {o.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {data.admin && (
          <div>
            <h3>New customers</h3>
            {data.recentCustomers.length === 0 ? (
              <p>There are no customers yet.</p>
            ) : (
              <table>
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Email</th>
                    <th>Added</th>
                  </tr>
                </thead>
                <tbody>
                  {data.recentCustomers.map((c, i) => (
                    <tr key={i}>
                      <td>{c.customerName}</td>
                      <td>{c.email}</td>
                      <td>{c.createdAt}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )}
      </div>
    </section>
  )
}
