import { useEffect, useState } from 'react'
import { api } from './api'
import SupplierForm from './SupplierForm'

// The suppliers screen. Administrators only (the server also refuses everyone else).
export default function SupplierList() {
  const [suppliers, setSuppliers] = useState([])
  const [loadError, setLoadError] = useState('')
  const [loading, setLoading] = useState(true)
  const [actionError, setActionError] = useState('')
  const [form, setForm] = useState(null) // null = hidden, { supplier: null } = add, { supplier } = edit
  const [reloadCount, setReloadCount] = useState(0)
  const [searchText, setSearchText] = useState('')
  const [appliedSearch, setAppliedSearch] = useState('')

  useEffect(() => {
    let cancelled = false
    const query = appliedSearch ? '?search=' + encodeURIComponent(appliedSearch) : ''
    api
      .get('/api/suppliers' + query)
      .then((items) => {
        if (!cancelled) {
          setSuppliers(items)
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
  }, [reloadCount, appliedSearch])

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
  }

  function handleSaved() {
    setForm(null)
    setActionError('')
    reload()
  }

  async function handleDelete(supplier) {
    if (!window.confirm('Delete ' + supplier.supplierName + '?')) return
    setActionError('')
    try {
      await api.del('/api/suppliers/' + supplier.id)
      reload()
    } catch (e) {
      setActionError(e.message)
    }
  }

  if (loading) return <p className="muted">Loading suppliers...</p>
  if (loadError) return <div className="error">{loadError}</div>

  return (
    <section>
      <div className="toolbar">
        <div>
          <h2>Suppliers</h2>
          <p className="muted">
            {suppliers.length} supplier(s)
            {appliedSearch && ' matching "' + appliedSearch + '"'}
          </p>
        </div>
        <button onClick={() => setForm({ supplier: null })}>Add supplier</button>
      </div>

      <form className="search-bar" onSubmit={handleSearch}>
        <input
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          placeholder="Search by name, contact person, email or phone"
        />
        <button type="submit">Search</button>
        <button type="button" className="secondary" onClick={handleClear}>
          Clear
        </button>
      </form>

      {actionError && <div className="error">{actionError}</div>}

      {form && (
        <SupplierForm
          key={form.supplier ? form.supplier.id : 'new'}
          supplier={form.supplier}
          onSaved={handleSaved}
          onCancel={() => setForm(null)}
        />
      )}

      {suppliers.length === 0 ? (
        <p>{appliedSearch ? 'No suppliers match your search.' : 'There are no suppliers yet.'}</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Contact person</th>
              <th>Email</th>
              <th>Phone</th>
              <th className="number">Products</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {suppliers.map((s) => (
              <tr key={s.id} onDoubleClick={() => setForm({ supplier: s })}>
                <td>{s.supplierName}</td>
                <td>{s.contactPerson}</td>
                <td>{s.email}</td>
                <td>{s.phone}</td>
                <td className="number">{s.productCount}</td>
                <td className="actions">
                  <button className="secondary" onClick={() => setForm({ supplier: s })}>
                    Edit
                  </button>
                  <button className="danger" onClick={() => handleDelete(s)}>
                    Delete
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
