import { useEffect, useState } from 'react'
import { api } from './api'

// Shows the customers in a table. Read-only for now: add, edit and delete come in a later step.
export default function CustomerList() {
  const [customers, setCustomers] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  // useEffect runs code after the screen has appeared. The empty [] at the end means "only once".
  useEffect(() => {
    let cancelled = false
    api
      .get('/api/customers')
      .then((data) => {
        if (!cancelled) setCustomers(data)
      })
      .catch((e) => {
        if (!cancelled) setError(e.message)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  if (loading) return <p className="muted">Loading customers...</p>
  if (error) return <div className="error">{error}</div>

  return (
    <section>
      <h2>Customers</h2>
      <p className="muted">{customers.length} customer(s)</p>
      {customers.length === 0 ? (
        <p>There are no customers yet.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Phone</th>
              <th>Address</th>
              <th>Added</th>
            </tr>
          </thead>
          <tbody>
            {customers.map((c) => (
              <tr key={c.id}>
                <td>{c.firstName} {c.lastName}</td>
                <td>{c.email}</td>
                <td>{c.phone}</td>
                <td>{c.address}</td>
                <td>{c.createdAt ? c.createdAt.slice(0, 10) : ''}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}
