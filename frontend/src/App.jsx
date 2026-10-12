import { useState } from 'react'
import './App.css'
import { clearCredentials } from './api'
import LoginForm from './LoginForm'
import CustomerList from './CustomerList'
import ProductList from './ProductList'

// The top-level component. Until someone is logged in it shows only the login screen.
export default function App() {
  const [user, setUser] = useState(null) // becomes { username, role } after a successful login
  const [tab, setTab] = useState('customers') // which screen is showing

  function handleLogout() {
    clearCredentials()
    setUser(null)
    setTab('customers')
  }

  if (!user) {
    return <LoginForm onLogin={setUser} />
  }

  return (
    <div className="page">
      <header className="topbar">
        <h1>Business Management System</h1>
        <div>
          Signed in as <strong>{user.username}</strong> ({user.role}){' '}
          <button onClick={handleLogout}>Log out</button>
        </div>
      </header>

      <nav className="tabs">
        <button className={tab === 'customers' ? 'active' : ''} onClick={() => setTab('customers')}>
          Customers
        </button>
        <button className={tab === 'products' ? 'active' : ''} onClick={() => setTab('products')}>
          Products
        </button>
      </nav>

      {tab === 'customers' ? <CustomerList user={user} /> : <ProductList user={user} />}
    </div>
  )
}
