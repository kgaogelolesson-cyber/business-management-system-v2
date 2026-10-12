import { useState } from 'react'
import { api } from './api'
import { centsToRands, randsToCents } from './money'

// One form for "add" and "edit". The stock quantity can only be typed when ADDING a product;
// when editing it is shown but not changeable (stock changes through orders and adjustments, later steps).
export default function ProductForm({ product, onSaved, onCancel }) {
  const editing = Boolean(product)
  const [productName, setProductName] = useState(product?.productName ?? '')
  const [category, setCategory] = useState(product?.category ?? '')
  const [description, setDescription] = useState(product?.description ?? '')
  const [price, setPrice] = useState(product ? centsToRands(product.priceCents) : '')
  const [stock, setStock] = useState('')
  const [reorderLevel, setReorderLevel] = useState(product ? String(product.reorderLevel) : '5')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')

    // Quick checks of the typed text. The backend still checks every real rule.
    const priceCents = randsToCents(price)
    if (price.trim() === '') return setError('The price is required.')
    if (priceCents === null) return setError('The price must be a number such as 159.00.')
    if (!editing && stock.trim() !== '' && !/^\d+$/.test(stock.trim())) {
      return setError('The stock quantity must be a whole number.')
    }
    if (reorderLevel.trim() !== '' && !/^\d+$/.test(reorderLevel.trim())) {
      return setError('The reorder level must be a whole number.')
    }

    setBusy(true)
    const body = {
      productName,
      category,
      description,
      priceCents,
      quantityInStock: !editing && stock.trim() !== '' ? Number(stock) : null,
      reorderLevel: reorderLevel.trim() !== '' ? Number(reorderLevel) : null,
      supplierId: product?.supplierId ?? null, // suppliers come in a later step; keep whatever is there
    }
    try {
      if (editing) {
        await api.put('/api/products/' + product.id, body)
      } else {
        await api.post('/api/products', body)
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
      <h3>{editing ? 'Edit product' : 'Add product'}</h3>

      <label>
        Product name
        <input value={productName} onChange={(e) => setProductName(e.target.value)} autoFocus />
      </label>
      <label>
        Category
        <input value={category} onChange={(e) => setCategory(e.target.value)} />
      </label>
      <label>
        Description
        <textarea rows={2} value={description} onChange={(e) => setDescription(e.target.value)} />
      </label>
      <label>
        Price (rand)
        <input value={price} onChange={(e) => setPrice(e.target.value)} placeholder="159.00" />
      </label>

      {editing ? (
        <p className="hint">
          Stock in hand: <strong>{product.quantityInStock}</strong>. It cannot be changed here; it changes
          through orders and stock adjustments.
        </p>
      ) : (
        <label>
          Stock quantity (starting stock)
          <input value={stock} onChange={(e) => setStock(e.target.value)} placeholder="0" />
        </label>
      )}

      <label>
        Reorder level (low-stock warning at or below this)
        <input value={reorderLevel} onChange={(e) => setReorderLevel(e.target.value)} />
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
