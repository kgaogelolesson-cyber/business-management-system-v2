import { useState } from 'react'
import { api, setCredentials, clearCredentials } from './api'

// The login screen. It replaces Version 1's LoginFrame.
// "useState" gives a component a piece of memory. When the value changes, React redraws the screen.
export default function LoginForm({ onLogin }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault() // stop the browser from reloading the page
    if (!username.trim() || !password) {
      setError('Please enter both your username and password.')
      return
    }
    setBusy(true)
    setError('')
    setCredentials(username.trim(), password)
    try {
      // "Who am I?" only succeeds when the username and password are correct.
      const me = await api.get('/api/auth/me')
      onLogin(me)
    } catch (e) {
      clearCredentials()
      setError(e.status === 401 ? 'Invalid username or password.' : e.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="login-wrap">
      <form className="login-card" onSubmit={handleSubmit}>
        <h1>Business Management System</h1>
        <p className="muted">Please sign in</p>

        <label>
          Username
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
            autoFocus
          />
        </label>

        <label>
          Password
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
          />
        </label>

        {error && <div className="error">{error}</div>}

        <button type="submit" disabled={busy}>
          {busy ? 'Signing in...' : 'Sign in'}
        </button>
      </form>
    </div>
  )
}
