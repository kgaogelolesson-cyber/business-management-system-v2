import { useState } from 'react'
import './App.css'
import { clearCredentials } from './api'
import LoginForm from './LoginForm'
import CustomerList from './CustomerList'
import ProductList from './ProductList'
import InventoryList from './InventoryList'
import OrderList from './OrderList'
import SupplierList from './SupplierList'

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
        <button className={tab === 'inventory' ? 'active' : ''} onClick={() => setTab('inventory')}>
          Inventory
        </button>
        <button className={tab === 'orders' ? 'active' : ''} onClick={() => setTab('orders')}>
          Orders
        </button>
        {user.role === 'ADMIN' && (
          <button className={tab === 'suppliers' ? 'active' : ''} onClick={() => setTab('suppliers')}>
            Suppliers
          </button>
        )}
      </nav>

      {tab === 'customers' && <CustomerList user={user} />}
      {tab === 'products' && <ProductList user={user} />}
      {tab === 'inventory' && <InventoryList user={user} />}
      {tab === 'orders' && <OrderList user={user} />}
      {tab === 'suppliers' && user.role === 'ADMIN' && <SupplierList />}
    </div>
  )
}
