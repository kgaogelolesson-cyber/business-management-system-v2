import { useState } from 'react'
import './App.css'
import { api, clearCredentials } from './api'
import { setCurrencySymbol } from './money'
import LoginForm from './LoginForm'
import CustomerList from './CustomerList'
import ProductList from './ProductList'
import InventoryList from './InventoryList'
import OrderList from './OrderList'
import SupplierList from './SupplierList'
import EmployeeList from './EmployeeList'
import ReportPanel from './ReportPanel'
import DashboardPanel from './DashboardPanel'
import SettingsPanel from './SettingsPanel'

// The top-level component. Until someone is logged in it shows only the login screen.
export default function App() {
  const [user, setUser] = useState(null) // becomes { username, role } after a successful login
  const [tab, setTab] = useState('dashboard') // which screen is showing

  const [symbol, setSymbol] = useState('R') // the currency symbol from the server

  function applySymbol(newSymbol) {
    setCurrencySymbol(newSymbol) // used by formatMoney
    setSymbol(newSymbol) // changing it makes the screens below start fresh and show the new symbol
  }

  // After a successful login, load the settings first so prices show the right symbol from the start.
  async function handleLogin(loggedIn) {
    try {
      const settings = await api.get('/api/settings')
      applySymbol(settings.currencySymbol)
    } catch {
      // If the settings cannot be loaded, prices simply show "R".
    }
    setUser(loggedIn)
  }

  function handleLogout() {
    clearCredentials()
    setUser(null)
    setTab('dashboard')
  }

  if (!user) {
    return <LoginForm onLogin={handleLogin} />
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
        <button className={tab === 'dashboard' ? 'active' : ''} onClick={() => setTab('dashboard')}>
          Dashboard
        </button>
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
        {user.role === 'ADMIN' && (
          <button className={tab === 'employees' ? 'active' : ''} onClick={() => setTab('employees')}>
            Employees
          </button>
        )}
        {user.role === 'ADMIN' && (
          <button className={tab === 'reports' ? 'active' : ''} onClick={() => setTab('reports')}>
            Reports
          </button>
        )}
        {user.role === 'ADMIN' && (
          <button className={tab === 'settings' ? 'active' : ''} onClick={() => setTab('settings')}>
            Settings
          </button>
        )}
      </nav>

      <div key={symbol}>
      {tab === 'dashboard' && <DashboardPanel />}
      {tab === 'customers' && <CustomerList user={user} />}
      {tab === 'products' && <ProductList user={user} />}
      {tab === 'inventory' && <InventoryList user={user} />}
      {tab === 'orders' && <OrderList user={user} />}
      {tab === 'suppliers' && user.role === 'ADMIN' && <SupplierList />}
      {tab === 'employees' && user.role === 'ADMIN' && <EmployeeList user={user} />}
      {tab === 'reports' && user.role === 'ADMIN' && <ReportPanel />}
      {tab === 'settings' && user.role === 'ADMIN' && (
        <SettingsPanel currencySymbol={symbol} onSaved={applySymbol} />
      )}
      </div>
    </div>
  )
}
