import { useEffect, useState } from 'react'
import { api } from './api'
import { formatMoney } from './money'
import ProductForm from './ProductForm'

// The products screen. Everyone logged in can look and search; only administrators can add, edit or delete.
export default function ProductList({ user }) {
  const [products, setProducts] = useState([])
  const [categories, setCategories] = useState([])
  const [loadError, setLoadError] = useState('')
  const [loading, setLoading] = useState(true)
  const [actionError, setActionError] = useState('')
  const [form, setForm] = useState(null) // null = hidden, { product: null } = add, { product } = edit
  const [reloadCount, setReloadCount] = useState(0)
  const [searchText, setSearchText] = useState('')
  const [appliedSearch, setAppliedSearch] = useState('')
  const [category, setCategory] = useState('') // '' = all categories

  const isAdmin = user.role === 'ADMIN'

  useEffect(() => {
    let cancelled = false
    const params = new URLSearchParams()
    if (appliedSearch) params.set('search', appliedSearch)
    if (category) params.set('category', category)
    const query = params.toString() ? '?' + params.toString() : ''

    Promise.all([api.get('/api/products' + query), api.get('/api/products/categories')])
      .then(([items, names]) => {
        if (!cancelled) {
          setProducts(items)
          setCategories(names)
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
  }, [reloadCount, appliedSearch, category])

  function reload() {
    setReloadCount((n) => n + 1)
  }

  function handleSearch(event) {
    event.preventDefault()
    setAppliedSearch(searchText.trim())
  }

  function handleClear() {
    setSearchText('')
    setAppliedSearch('')
    setCategory('')
  }

  function handleSaved() {
    setForm(null)
    setActionError('')
    reload()
  }

  async function handleDelete(product) {
    if (!window.confirm('Delete ' + product.productName + '?')) return
    setActionError('')
    try {
      await api.del('/api/products/' + product.id)
      reload()
    } catch (e) {
      setActionError(e.message)
    }
  }

  function statusClass(status) {
    if (status === 'OUT OF STOCK') return 'status status-out'
    if (status === 'LOW STOCK') return 'status status-low'
    return 'status status-in'
  }

  if (loading) return <p className="muted">Loading products...</p>
  if (loadError) return <div className="error">{loadError}</div>

  const filtered = appliedSearch || category

  return (
    <section>
      <div className="toolbar">
        <div>
          <h2>Products</h2>
          <p className="muted">
            {products.length} product(s)
            {appliedSearch && ' matching "' + appliedSearch + '"'}
            {category && ' in ' + category}
            {!isAdmin && ' (view only: only an administrator can change products)'}
          </p>
        </div>
        {isAdmin && <button onClick={() => setForm({ product: null })}>Add product</button>}
      </div>

      <form className="search-bar" onSubmit={handleSearch}>
        <input
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          placeholder="Search by product name"
        />
        <select value={category} onChange={(e) => setCategory(e.target.value)}>
          <option value="">All categories</option>
          {categories.map((c) => (
            <option key={c} value={c}>
              {c}
            </option>
          ))}
        </select>
        <button type="submit">Search</button>
        <button type="button" className="secondary" onClick={handleClear}>
          Clear
        </button>
      </form>

      {actionError && <div className="error">{actionError}</div>}

      {form && (
        <ProductForm
          key={form.product ? form.product.id : 'new'}
          product={form.product}
          onSaved={handleSaved}
          onCancel={() => setForm(null)}
        />
      )}

      {products.length === 0 ? (
        <p>{filtered ? 'No products match your search.' : 'There are no products yet.'}</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Category</th>
              <th className="number">Price</th>
              <th className="number">In stock</th>
              <th className="number">Reorder at</th>
              <th>Status</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {products.map((p) => (
              <tr key={p.id} onDoubleClick={isAdmin ? () => setForm({ product: p }) : undefined}>
                <td>{p.productName}</td>
                <td>{p.category}</td>
                <td className="number">{formatMoney(p.priceCents)}</td>
                <td className="number">{p.quantityInStock}</td>
                <td className="number">{p.reorderLevel}</td>
                <td>
                  <span className={statusClass(p.stockStatus)}>{p.stockStatus}</span>
                </td>
                <td className="actions">
                  {isAdmin && (
                    <>
                      <button className="secondary" onClick={() => setForm({ product: p })}>
                        Edit
                      </button>
                      <button className="danger" onClick={() => handleDelete(p)}>
                        Delete
                      </button>
                    </>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}
