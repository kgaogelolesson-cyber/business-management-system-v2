import { useEffect, useState } from 'react'
import { api } from './api'
import CustomerForm from './CustomerForm'

// The customers screen: list, add, edit and (for administrators) delete.
export default function CustomerList({ user }) {
  const [customers, setCustomers] = useState([])
  const [loadError, setLoadError] = useState('')
  const [loading, setLoading] = useState(true)
  const [actionError, setActionError] = useState('')
  const [form, setForm] = useState(null) // null = form hidden, { customer: null } = add, { customer } = edit
  const [reloadCount, setReloadCount] = useState(0) // changing this number makes the list load again
  const [searchText, setSearchText] = useState('') // what is typed in the box
  const [appliedSearch, setAppliedSearch] = useState('') // what the list is actually filtered by

  const isAdmin = user.role === 'ADMIN'

  useEffect(() => {
    let cancelled = false
    const query = appliedSearch ? '?search=' + encodeURIComponent(appliedSearch) : ''
    api
      .get('/api/customers' + query)
      .then((data) => {
        if (!cancelled) {
          setCustomers(data)
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

  async function handleDelete(customer) {
    if (!window.confirm('Delete ' + customer.firstName + ' ' + customer.lastName + '?')) return
    setActionError('')
    try {
      await api.del('/api/customers/' + customer.id)
      reload()
    } catch (e) {
      // For example: "This customer has 1 order and cannot be deleted. Their order history must be kept."
      setActionError(e.message)
    }
  }

  if (loading) return <p className="muted">Loading customers...</p>
  if (loadError) return <div className="error">{loadError}</div>

  return (
    <section>
      <div className="toolbar">
        <div>
          <h2>Customers</h2>
          <p className="muted">
            {customers.length} customer(s)
            {appliedSearch && ' matching "' + appliedSearch + '"'}
          </p>
        </div>
        <button onClick={() => setForm({ customer: null })}>Add customer</button>
      </div>

      <form className="search-bar" onSubmit={handleSearch}>
        <input
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          placeholder="Search by name, email or phone"
        />
        <button type="submit">Search</button>
        <button type="button" className="secondary" onClick={handleClear}>
          Clear
        </button>
      </form>

      {actionError && <div className="error">{actionError}</div>}

      {form && (
        <CustomerForm
          key={form.customer ? form.customer.id : 'new'}
          customer={form.customer}
          onSaved={handleSaved}
          onCancel={() => setForm(null)}
        />
      )}

      {customers.length === 0 ? (
        <p>{appliedSearch ? 'No customers match your search.' : 'There are no customers yet.'}</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Phone</th>
              <th>Address</th>
              <th>Added</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {customers.map((c) => (
              <tr key={c.id} onDoubleClick={() => setForm({ customer: c })}>
                <td>{c.firstName} {c.lastName}</td>
                <td>{c.email}</td>
                <td>{c.phone}</td>
                <td>{c.address}</td>
                <td>{c.createdAt ? c.createdAt.slice(0, 10) : ''}</td>
                <td className="actions">
                  <button className="secondary" onClick={() => setForm({ customer: c })}>
                    Edit
                  </button>
                  {isAdmin && (
                    <button className="danger" onClick={() => handleDelete(c)}>
                      Delete
                    </button>
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
