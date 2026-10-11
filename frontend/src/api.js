// The only file that talks to the Spring Boot API.
// The browser sends the username and password with EVERY request (HTTP Basic), because the
// server keeps no session. We keep them in this variable, in memory only. Reloading the page logs you out.

let authHeader = null

export function setCredentials(username, password) {
  // btoa() only handles basic characters, so we turn the text into UTF-8 bytes first.
  const bytes = new TextEncoder().encode(username + ':' + password)
  authHeader = 'Basic ' + btoa(String.fromCharCode(...bytes))
}

export function clearCredentials() {
  authHeader = null
}

async function request(method, path, body) {
  let response
  try {
    response = await fetch(path, {
      method,
      headers: {
        // This header tells Spring Security "a script is calling, not a person in a browser",
        // so a wrong password gives a plain 401 instead of the browser's own login pop-up.
        'X-Requested-With': 'XMLHttpRequest',
        ...(authHeader ? { Authorization: authHeader } : {}),
        ...(body ? { 'Content-Type': 'application/json' } : {}),
      },
      body: body ? JSON.stringify(body) : undefined,
    })
  } catch {
    throw new Error('The server could not be reached. Is the backend running?')
  }

  if (response.status === 204) {
    return null // success with nothing to send back (for example after a delete)
  }

  const text = await response.text()
  let data = null
  try {
    data = text ? JSON.parse(text) : null
  } catch {
    data = null
  }

  if (!response.ok) {
    // Our backend sends {"message": "..."} for rule violations; show that message as it is.
    const error = new Error((data && data.message) || 'Something went wrong (HTTP ' + response.status + ').')
    error.status = response.status
    throw error
  }
  return data
}

export const api = {
  get: (path) => request('GET', path),
  post: (path, body) => request('POST', path, body),
  put: (path, body) => request('PUT', path, body),
  del: (path) => request('DELETE', path),
}
