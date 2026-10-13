import { useState } from 'react'
import { api } from './api'

// One form for "add" and "edit" a supplier. The backend checks every real rule and sends clear messages.
export default function SupplierForm({ supplier, onSaved, onCancel }) {
  const editing = Boolean(supplier)
  const [supplierName, setSupplierName] = useState(supplier?.supplierName ?? '')
  const [contactPerson, setContactPerson] = useState(supplier?.contactPerson ?? '')
  const [email, setEmail] = useState(supplier?.email ?? '')
  const [phone, setPhone] = useState(supplier?.phone ?? '')
  const [address, setAddress] = useState(supplier?.address ?? '')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setBusy(true)
    const body = { supplierName, contactPerson, email, phone, address }
    try {
      if (editing) {
        await api.put('/api/suppliers/' + supplier.id, body)
      } else {
        await api.post('/api/suppliers', body)
      }
      onSaved()
    } catch (e) {
      setError(e.message)
      setBusy(false)
    }
  }

  return (
    <form
      className="form-card"
      onSubmit={handleSubmit}
      onKeyDown={(e) => {
        if (e.key === 'Escape') onCancel()
      }}
    >
      <h3>{editing ? 'Edit supplier' : 'Add supplier'}</h3>

      <label>
        Supplier name
        <input value={supplierName} onChange={(e) => setSupplierName(e.target.value)} autoFocus />
      </label>
      <label>
        Contact person
        <input value={contactPerson} onChange={(e) => setContactPerson(e.target.value)} />
      </label>
      <label>
        Email
        <input value={email} onChange={(e) => setEmail(e.target.value)} placeholder="orders@supplier.co.za" />
      </label>
      <label>
        Phone
        <input value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="011 555 0101" />
      </label>
      <label>
        Address
        <input value={address} onChange={(e) => setAddress(e.target.value)} />
      </label>

      {error && <div className="error">{error}</div>}

      <div className="form-actions">
        <button type="submit" disabled={busy}>
          {busy ? 'Saving...' : 'Save'}
        </button>
        <button type="button" className="secondary" onClick={onCancel}>
          Cancel
        </button>
      </div>
    </form>
  )
}
