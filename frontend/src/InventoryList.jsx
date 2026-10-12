import { useEffect, useState } from 'react'
import { api } from './api'
import AdjustForm from './AdjustForm'
import HistoryPanel from './HistoryPanel'

// The inventory screen. Everyone logged in can see stock and history; only administrators can adjust stock.
export default function InventoryList({ user }) {
  const [items, setItems] = useState([])
  const [loadError, setLoadError] = useState('')
  const [loading, setLoading] = useState(true)
  const [panel, setPanel] = useState(null) // null, { type: 'adjust', item } or { type: 'history', item }
  const [reloadCount, setReloadCount] = useState(0)
  const [searchText, setSearchText] = useState('')
  const [appliedSearch, setAppliedSearch] = useState('')
  const [status, setStatus] = useState('') // '' = all

  const isAdmin = user.role === 'ADMIN'

  useEffect(() => {
    let cancelled = false
    const params = new URLSearchParams()
    if (appliedSearch) params.set('search', appliedSearch)
    if (status) params.set('status', status)
    const query = params.toString() ? '?' + params.toString() : ''
    api
      .get('/api/inventory' + query)
      .then((data) => {
        if (!cancelled) {
          setItems(data)
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
  }, [reloadCount, appliedSearch, status])

  function handleSearch(event) {
    event.preventDefault()
    setAppliedSearch(searchText.trim())
  }

  function handleClear() {
    setSearchText('')
    setAppliedSearch('')
    setStatus('')
  }

  function handleSaved() {
    setPanel(null)
    setReloadCount((n) => n + 1)
  }

  function statusClass(s) {
    if (s === 'OUT OF STOCK') return 'status status-out'
    if (s === 'LOW STOCK') return 'status status-low'
    return 'status status-in'
  }

  if (loading) return <p className="muted">Loading inventory...</p>
  if (loadError) return <div className="error">{loadError}</div>

  const filtered = appliedSearch || status

  return (
    <section>
      <div className="toolbar">
        <div>
          <h2>Inventory</h2>
          <p className="muted">
            {items.length} product(s)
            {appliedSearch && ' matching "' + appliedSearch + '"'}
            {status && ' with status ' + status}
            {!isAdmin && ' (view only: only an administrator can adjust stock)'}
          </p>
        </div>
      </div>

      <form className="search-bar" onSubmit={handleSearch}>
        <input
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          placeholder="Search by name, category or supplier"
        />
        <select value={status} onChange={(e) => setStatus(e.target.value)}>
          <option value="">All products</option>
          <option value="IN STOCK">In stock</option>
          <option value="LOW STOCK">Low stock</option>
          <option value="OUT OF STOCK">Out of stock</option>
        </select>
        <button type="submit">Search</button>
        <button type="button" className="secondary" onClick={handleClear}>
          Clear
        </button>
      </form>

      {panel && panel.type === 'adjust' && (
        <AdjustForm
          key={'adjust-' + panel.item.id}
          item={panel.item}
          onSaved={handleSaved}
          onCancel={() => setPanel(null)}
        />
      )}
      {panel && panel.type === 'history' && (
        <HistoryPanel key={'history-' + panel.item.id} item={panel.item} onClose={() => setPanel(null)} />
      )}

      {items.length === 0 ? (
        <p>{filtered ? 'No products match your search.' : 'There are no products yet.'}</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Category</th>
              <th>Supplier</th>
              <th className="number">In stock</th>
              <th className="number">Reorder at</th>
              <th>Status</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {items.map((p) => (
              <tr key={p.id}>
                <td>{p.productName}</td>
                <td>{p.category}</td>
                <td>{p.supplierName}</td>
                <td className="number">{p.quantityInStock}</td>
                <td className="number">{p.reorderLevel}</td>
                <td>
                  <span className={statusClass(p.stockStatus)}>{p.stockStatus}</span>
                </td>
                <td className="actions">
                  {isAdmin && (
                    <button onClick={() => setPanel({ type: 'adjust', item: p })}>Adjust stock</button>
                  )}
                  <button className="secondary" onClick={() => setPanel({ type: 'history', item: p })}>
                    History
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
