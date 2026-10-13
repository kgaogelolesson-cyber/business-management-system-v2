import { useEffect, useState } from 'react'
import { api } from './api'
import EmployeeForm from './EmployeeForm'
import PasswordForm from './PasswordForm'

// The employees screen (administrators only; the server also refuses everyone else).
// An employee is never deleted: "Deactivate" hides them and blocks their login, "Reactivate" brings them back.
export default function EmployeeList({ user }) {
  const [employees, setEmployees] = useState([])
  const [departments, setDepartments] = useState([])
  const [loadError, setLoadError] = useState('')
  const [loading, setLoading] = useState(true)
  const [actionError, setActionError] = useState('')
  const [notice, setNotice] = useState('')
  const [panel, setPanel] = useState(null) // null | { kind: 'form', employee } | { kind: 'password', employee }
  const [reloadCount, setReloadCount] = useState(0)
  const [searchText, setSearchText] = useState('')
  const [appliedSearch, setAppliedSearch] = useState('')
  const [department, setDepartment] = useState('')
  const [showInactive, setShowInactive] = useState(false)

  useEffect(() => {
    let cancelled = false
    const params = new URLSearchParams()
    if (appliedSearch) params.set('search', appliedSearch)
    if (department) params.set('department', department)
    if (showInactive) params.set('includeInactive', 'true')
    const query = params.toString() ? '?' + params.toString() : ''

    Promise.all([api.get('/api/employees' + query), api.get('/api/employees/departments')])
      .then(([items, names]) => {
        if (!cancelled) {
          setEmployees(items)
          setDepartments(names)
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
  }, [reloadCount, appliedSearch, department, showInactive])

  function reload() {
    setReloadCount((n) => n + 1)
  }

  function isSelf(emp) {
    return Boolean(emp.username) && emp.username.toLowerCase() === user.username.toLowerCase()
  }

  function handleSearch(event) {
    event.preventDefault()
    setAppliedSearch(searchText.trim())
  }

  function handleClear() {
    setSearchText('')
    setAppliedSearch('')
    setDepartment('')
    setShowInactive(false)
  }

  function handleSaved(message) {
    setPanel(null)
    setActionError('')
    setNotice(message)
    reload()
  }

  async function handleDeactivate(emp) {
    if (!window.confirm('Deactivate ' + emp.firstName + ' ' + emp.lastName + '? Their login will stop working.')) return
    setActionError('')
    setNotice('')
    try {
      await api.post('/api/employees/' + emp.id + '/deactivate')
      reload()
    } catch (e) {
      setActionError(e.message)
    }
  }

  async function handleReactivate(emp) {
    setActionError('')
    setNotice('')
    try {
      await api.post('/api/employees/' + emp.id + '/reactivate')
      reload()
    } catch (e) {
      setActionError(e.message)
    }
  }

  if (loading) return <p className="muted">Loading employees...</p>
  if (loadError) return <div className="error">{loadError}</div>

  const filtered = appliedSearch || department || showInactive

  return (
    <section>
      <div className="toolbar">
        <div>
          <h2>Employees</h2>
          <p className="muted">
            {employees.length} employee(s)
            {appliedSearch && ' matching "' + appliedSearch + '"'}
            {department && ' in ' + department}
          </p>
        </div>
        <button onClick={() => setPanel({ kind: 'form', employee: null })}>Add employee</button>
      </div>

      <form className="search-bar" onSubmit={handleSearch}>
        <input
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          placeholder="Search by name, username, email, phone, position or department"
        />
        <select value={department} onChange={(e) => setDepartment(e.target.value)}>
          <option value="">All departments</option>
          {departments.map((d) => (
            <option key={d} value={d}>
              {d}
            </option>
          ))}
        </select>
        <label className="check">
          <input type="checkbox" checked={showInactive} onChange={(e) => setShowInactive(e.target.checked)} />
          Show deactivated
        </label>
        <button type="submit">Search</button>
        <button type="button" className="secondary" onClick={handleClear}>
          Clear
        </button>
      </form>

      {notice && <div className="notice">{notice}</div>}
      {actionError && <div className="error">{actionError}</div>}

      {panel?.kind === 'form' && (
        <EmployeeForm
          key={panel.employee ? panel.employee.id : 'new'}
          employee={panel.employee}
          isSelf={panel.employee ? isSelf(panel.employee) : false}
          onSaved={() => handleSaved(panel.employee ? 'Employee saved.' : 'Employee added.')}
          onCancel={() => setPanel(null)}
        />
      )}
      {panel?.kind === 'password' && (
        <PasswordForm
          key={'pw' + panel.employee.id}
          employee={panel.employee}
          onSaved={() => handleSaved('Password changed.')}
          onCancel={() => setPanel(null)}
        />
      )}

      {employees.length === 0 ? (
        <p>{filtered ? 'No employees match your search.' : 'There are no employees yet.'}</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Username</th>
              <th>Role</th>
              <th>Position</th>
              <th>Department</th>
              <th>Hired</th>
              <th>Status</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {employees.map((emp) => (
              <tr key={emp.id} className={emp.active ? '' : 'row-inactive'}>
                <td>
                  {emp.firstName} {emp.lastName}
                </td>
                <td>{emp.username}</td>
                <td>{emp.role}</td>
                <td>{emp.position}</td>
                <td>{emp.department}</td>
                <td>{emp.hireDate}</td>
                <td>
                  <span className={emp.active ? 'status status-in' : 'status status-out'}>
                    {emp.active ? 'ACTIVE' : 'DEACTIVATED'}
                  </span>
                </td>
                <td className="actions">
                  {emp.active ? (
                    <>
                      <button className="secondary" onClick={() => setPanel({ kind: 'form', employee: emp })}>
                        Edit
                      </button>
                      {emp.username && (
                        <button className="secondary" onClick={() => setPanel({ kind: 'password', employee: emp })}>
                          Password
                        </button>
                      )}
                      {!isSelf(emp) && (
                        <button className="danger" onClick={() => handleDeactivate(emp)}>
                          Deactivate
                        </button>
                      )}
                    </>
                  ) : (
                    <button className="secondary" onClick={() => handleReactivate(emp)}>
                      Reactivate
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
