import { useState } from 'react'
import { api } from './api'

// The Settings tab (administrator only). For now it has one setting: the currency symbol.
export default function SettingsPanel({ currencySymbol, onSaved }) {
  const [value, setValue] = useState(currencySymbol)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setMessage('')
    setBusy(true)
    try {
      const saved = await api.put('/api/settings', { currencySymbol: value })
      setValue(saved.currencySymbol)
      setMessage('Saved. Prices now show "' + saved.currencySymbol + '".')
      onSaved(saved.currencySymbol)
    } catch (e) {
      setError(e.message)
    }
    setBusy(false)
  }

  return (
    <section>
      <h2>Settings</h2>
      <form className="form-card" onSubmit={handleSubmit}>
        <label>
          Currency symbol (shown in front of prices, at most 5 characters)
          <input value={value} onChange={(e) => setValue(e.target.value)} maxLength={5} />
        </label>
        {error && <p className="error">{error}</p>}
        {message && <p className="notice">{message}</p>}
        <div className="form-actions">
          <button type="submit" disabled={busy}>
            {busy ? 'Saving...' : 'Save'}
          </button>
        </div>
      </form>
    </section>
  )
}
