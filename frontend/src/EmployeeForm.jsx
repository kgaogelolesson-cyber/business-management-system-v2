import { useState } from 'react'
import { api } from './api'

// One form for "add" and "edit" an employee.
// Adding also creates the login (username, role, password). Editing changes the details and the role only:
// the username never changes, and passwords are changed with the separate "Password" button.
export default function EmployeeForm({ employee, isSelf, onSaved, onCancel }) {
  const editing = Boolean(employee)
  const [firstName, setFirstName] = useState(employee?.firstName ?? '')
  const [lastName, setLastName] = useState(employee?.lastName ?? '')
  const [email, setEmail] = useState(employee?.email ?? '')
  const [phone, setPhone] = useState(employee?.phone ?? '')
  const [position, setPosition] = useState(employee?.position ?? '')
  const [department, setDepartment] = useState(employee?.department ?? '')
  const [hireDate, setHireDate] = useState(employee?.hireDate ?? '')
  const [username, setUsername] = useState('')
  const [role, setRole] = useState(employee?.role ?? 'EMPLOYEE')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setBusy(true)
    const body = {
      firstName, lastName, email, phone, position, department, hireDate, role,
      ...(editing ? {} : { username, password, confirmPassword }),
    }
    try {
      if (editing) {
        await api.put('/api/employees/' + employee.id, body)
      } else {
        await api.post('/api/employees', body)
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
      <h3>{editing ? 'Edit employee' : 'Add employee'}</h3>

      <label>
        First name
        <input value={firstName} onChange={(e) => setFirstName(e.target.value)} autoFocus />
      </label>
      <label>
        Last name
        <input value={lastName} onChange={(e) => setLastName(e.target.value)} />
      </label>
      <label>
        Email
        <input value={email} onChange={(e) => setEmail(e.target.value)} placeholder="name@company.co.za" />
      </label>
      <label>
        Phone
        <input value={phone} onChange={(e) => setPhone(e.target.value)} />
      </label>
      <label>
        Position
        <input value={position} onChange={(e) => setPosition(e.target.value)} />
      </label>
      <label>
        Department
        <input value={department} onChange={(e) => setDepartment(e.target.value)} />
      </label>
      <label>
        Hire date (yyyy-MM-dd)
        <input value={hireDate} onChange={(e) => setHireDate(e.target.value)} placeholder="2026-03-15" />
      </label>

      <label>
        Role
        <select value={role} onChange={(e) => setRole(e.target.value)} disabled={editing && isSelf}>
          <option value="EMPLOYEE">EMPLOYEE</option>
          <option value="ADMIN">ADMIN</option>
        </select>
      </label>

      {editing ? (
        <p className="hint">
          Username: <strong>{employee.username ?? '(no login)'}</strong>. It cannot be changed here; use the
          Password button to set a new password.
          {isSelf && ' You cannot change your own role.'}
        </p>
      ) : (
        <>
          <label>
            Username (letters, numbers, . _ -)
            <input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="off" />
          </label>
          <label>
            Password (8+ characters, a letter and a number)
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="new-password"
            />
          </label>
          <label>
            Confirm password
            <input
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              autoComplete="new-password"
            />
          </label>
        </>
      )}

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
