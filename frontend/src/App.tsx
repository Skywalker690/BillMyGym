import { useCallback, useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import './App.css'

type Role = 'SUPER_ADMIN' | 'ORG_ADMIN'

type LoginResponse = {
  accessToken: string
  role: Role
  organizationId: number | null
}

type AuthState = LoginResponse & {
  email: string
}

type PlatformAnalytics = {
  totalOrganizations: number
  activeSubscriptions: number
}

type Organization = {
  id: number
  name: string
  district: string
  logoUrl: string | null
  mapLocationLink: string | null
  address: string | null
  adminFullName: string
  adminPhoneNumber: string
  adminEmail: string
  description: string | null
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED'
}

type OrganizationAnalytics = {
  activeMembers: number
  remindersSent: number
}

type Member = {
  id: number
  organizationId: number
  fullName: string
  phoneNumber: string
  email: string | null
  joiningDate: string
  membershipStartDate: string
  membershipEndDate: string
  membershipStatus: 'ACTIVE' | 'EXPIRED' | 'OVERDUE'
  notes: string | null
}

const AUTH_STORAGE_KEY = 'gymflow.auth'
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

async function apiFetch<T>(
  path: string,
  options: RequestInit,
  accessToken?: string,
): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      ...(options.headers ?? {}),
    },
  })

  if (!response.ok) {
    let message = 'Request failed'
    try {
      const body = await response.json() as { message?: string }
      if (body.message) {
        message = body.message
      }
    } catch {
      message = `${message} (${response.status})`
    }
    throw new Error(message)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
}

function readStoredAuth(): AuthState | null {
  const raw = localStorage.getItem(AUTH_STORAGE_KEY)
  if (!raw) {
    return null
  }
  try {
    const parsed = JSON.parse(raw) as AuthState
    if (!parsed.accessToken || !parsed.role || !parsed.email) {
      return null
    }
    return parsed
  } catch {
    return null
  }
}

function App() {
  const [auth, setAuth] = useState<AuthState | null>(() => readStoredAuth())

  const handleLogin = (nextAuth: AuthState) => {
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(nextAuth))
    setAuth(nextAuth)
  }

  const handleLogout = () => {
    localStorage.removeItem(AUTH_STORAGE_KEY)
    setAuth(null)
  }

  if (!auth) {
    return <LoginPage onLogin={handleLogin} />
  }

  return (
    <div className="app-shell">
      <header className="app-header">
        <div>
          <p className="brand">GymFlow</p>
          <p className="meta">{auth.email} · {auth.role}</p>
        </div>
        <button type="button" className="ghost-btn" onClick={handleLogout}>Logout</button>
      </header>

      {auth.role === 'SUPER_ADMIN' ? (
        <SuperAdminPage auth={auth} />
      ) : (
        <OrgAdminPage auth={auth} />
      )}
    </div>
  )
}

function LoginPage({ onLogin }: { onLogin: (auth: AuthState) => void }) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setLoading(true)
    setError(null)
    try {
      const response = await apiFetch<LoginResponse>('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      })
      onLogin({ ...response, email })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="centered-shell">
      <form className="card" onSubmit={submit}>
        <h1>Login</h1>
        <p className="muted">Sign in as SUPER_ADMIN or ORG_ADMIN</p>

        <label>
          Email
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            autoComplete="email"
          />
        </label>

        <label>
          Password
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            autoComplete="current-password"
          />
        </label>

        {error ? <p className="error-text">{error}</p> : null}

        <button type="submit" className="primary-btn" disabled={loading}>
          {loading ? 'Signing in...' : 'Sign In'}
        </button>
      </form>
    </div>
  )
}

function SuperAdminPage({ auth }: { auth: AuthState }) {
  const [platformAnalytics, setPlatformAnalytics] = useState<PlatformAnalytics | null>(null)
  const [organizations, setOrganizations] = useState<Organization[]>([])
  const [error, setError] = useState<string | null>(null)
  const [registerLoading, setRegisterLoading] = useState(false)

  const [form, setForm] = useState({
    organizationName: '',
    district: '',
    adminFullName: '',
    adminPhoneNumber: '',
    adminEmail: '',
    password: '12345678',
    address: '',
    organizationDescription: '',
    logoUrl: '',
    mapLocationLink: '',
  })

  const load = useCallback(async () => {
    try {
      setError(null)
      const [analyticsResponse, orgsResponse] = await Promise.all([
        apiFetch<PlatformAnalytics>('/api/analytics/platform', { method: 'GET' }, auth.accessToken),
        apiFetch<Organization[]>('/api/organizations', { method: 'GET' }, auth.accessToken),
      ])
      setPlatformAnalytics(analyticsResponse)
      setOrganizations(orgsResponse)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load data')
    }
  }, [auth.accessToken])

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void load()
    }, 0)
    return () => window.clearTimeout(timer)
  }, [load])

  const statusCounts = useMemo(() => {
    return organizations.reduce(
      (acc, item) => {
        acc[item.status] += 1
        return acc
      },
      { ACTIVE: 0, INACTIVE: 0, SUSPENDED: 0 },
    )
  }, [organizations])

  const registerOrgAdmin = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setRegisterLoading(true)
    setError(null)
    try {
      await apiFetch<void>('/api/auth/register', {
        method: 'POST',
        body: JSON.stringify(form),
      }, auth.accessToken)
      setForm((prev) => ({
        ...prev,
        organizationName: '',
        district: '',
        adminFullName: '',
        adminPhoneNumber: '',
        adminEmail: '',
        address: '',
        organizationDescription: '',
        logoUrl: '',
        mapLocationLink: '',
      }))
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to register organization admin')
    } finally {
      setRegisterLoading(false)
    }
  }

  return (
    <main className="layout">
      <section className="card">
        <h2>Platform Overview</h2>
        {platformAnalytics ? (
          <div className="stats-grid">
            <article className="stat-card">
              <p>Total Organizations</p>
              <strong>{platformAnalytics.totalOrganizations}</strong>
            </article>
            <article className="stat-card">
              <p>Active Subscriptions</p>
              <strong>{platformAnalytics.activeSubscriptions}</strong>
            </article>
            <article className="stat-card">
              <p>Active Orgs</p>
              <strong>{statusCounts.ACTIVE}</strong>
            </article>
            <article className="stat-card">
              <p>Suspended Orgs</p>
              <strong>{statusCounts.SUSPENDED}</strong>
            </article>
          </div>
        ) : (
          <p className="muted">Loading platform analytics...</p>
        )}
      </section>

      <section className="card">
        <h2>Organizations</h2>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>District</th>
                <th>Admin</th>
                <th>Email</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {organizations.map((org) => (
                <tr key={org.id}>
                  <td>{org.id}</td>
                  <td>{org.name}</td>
                  <td>{org.district}</td>
                  <td>{org.adminFullName}</td>
                  <td>{org.adminEmail}</td>
                  <td><span className="status-chip">{org.status}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="card">
        <h2>Create Organization + ORG_ADMIN</h2>
        <form className="form-grid" onSubmit={registerOrgAdmin}>
          <label>
            Organization Name
            <input
              value={form.organizationName}
              onChange={(e) => setForm((prev) => ({ ...prev, organizationName: e.target.value }))}
              required
            />
          </label>
          <label>
            District
            <input
              value={form.district}
              onChange={(e) => setForm((prev) => ({ ...prev, district: e.target.value }))}
              required
            />
          </label>
          <label>
            Admin Full Name
            <input
              value={form.adminFullName}
              onChange={(e) => setForm((prev) => ({ ...prev, adminFullName: e.target.value }))}
              required
            />
          </label>
          <label>
            Admin Phone Number
            <input
              value={form.adminPhoneNumber}
              onChange={(e) => setForm((prev) => ({ ...prev, adminPhoneNumber: e.target.value }))}
              required
            />
          </label>
          <label>
            Admin Email
            <input
              type="email"
              value={form.adminEmail}
              onChange={(e) => setForm((prev) => ({ ...prev, adminEmail: e.target.value }))}
              required
            />
          </label>
          <label>
            Password
            <input
              value={form.password}
              onChange={(e) => setForm((prev) => ({ ...prev, password: e.target.value }))}
              required
            />
          </label>
          <label>
            Address
            <input
              value={form.address}
              onChange={(e) => setForm((prev) => ({ ...prev, address: e.target.value }))}
            />
          </label>
          <label>
            Description
            <input
              value={form.organizationDescription}
              onChange={(e) => setForm((prev) => ({ ...prev, organizationDescription: e.target.value }))}
            />
          </label>
          <label>
            Logo URL
            <input
              value={form.logoUrl}
              onChange={(e) => setForm((prev) => ({ ...prev, logoUrl: e.target.value }))}
            />
          </label>
          <label>
            Map Location URL
            <input
              value={form.mapLocationLink}
              onChange={(e) => setForm((prev) => ({ ...prev, mapLocationLink: e.target.value }))}
            />
          </label>
          <button type="submit" className="primary-btn" disabled={registerLoading}>
            {registerLoading ? 'Creating...' : 'Create Organization'}
          </button>
        </form>
      </section>

      {error ? <p className="error-text">{error}</p> : null}
    </main>
  )
}

function OrgAdminPage({ auth }: { auth: AuthState }) {
  const [analytics, setAnalytics] = useState<OrganizationAnalytics | null>(null)
  const [members, setMembers] = useState<Member[]>([])
  const [error, setError] = useState<string | null>(null)
  const [createLoading, setCreateLoading] = useState(false)

  const [memberForm, setMemberForm] = useState({
    fullName: '',
    phoneNumber: '',
    email: '',
    joiningDate: '',
    membershipStartDate: '',
    membershipEndDate: '',
    membershipStatus: 'ACTIVE',
    notes: '',
  })

  const load = useCallback(async () => {
    try {
      setError(null)
      const [analyticsResponse, membersResponse] = await Promise.all([
        apiFetch<OrganizationAnalytics>('/api/analytics/organization', { method: 'GET' }, auth.accessToken),
        apiFetch<Member[]>('/api/members', { method: 'GET' }, auth.accessToken),
      ])
      setAnalytics(analyticsResponse)
      setMembers(membersResponse)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load data')
    }
  }, [auth.accessToken])

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void load()
    }, 0)
    return () => window.clearTimeout(timer)
  }, [load])

  const createMember = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setCreateLoading(true)
    setError(null)
    try {
      await apiFetch<Member>('/api/members', {
        method: 'POST',
        body: JSON.stringify(memberForm),
      }, auth.accessToken)
      setMemberForm({
        fullName: '',
        phoneNumber: '',
        email: '',
        joiningDate: '',
        membershipStartDate: '',
        membershipEndDate: '',
        membershipStatus: 'ACTIVE',
        notes: '',
      })
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create member')
    } finally {
      setCreateLoading(false)
    }
  }

  return (
    <main className="layout">
      <section className="card">
        <h2>Organization Dashboard</h2>
        {analytics ? (
          <div className="stats-grid two-col">
            <article className="stat-card">
              <p>Active Members</p>
              <strong>{analytics.activeMembers}</strong>
            </article>
            <article className="stat-card">
              <p>Reminders Sent</p>
              <strong>{analytics.remindersSent}</strong>
            </article>
          </div>
        ) : (
          <p className="muted">Loading organization analytics...</p>
        )}
      </section>

      <section className="card">
        <h2>Create Member</h2>
        <form className="form-grid" onSubmit={createMember}>
          <label>
            Full Name
            <input
              value={memberForm.fullName}
              onChange={(e) => setMemberForm((prev) => ({ ...prev, fullName: e.target.value }))}
              required
            />
          </label>
          <label>
            Phone Number
            <input
              value={memberForm.phoneNumber}
              onChange={(e) => setMemberForm((prev) => ({ ...prev, phoneNumber: e.target.value }))}
              required
            />
          </label>
          <label>
            Email
            <input
              type="email"
              value={memberForm.email}
              onChange={(e) => setMemberForm((prev) => ({ ...prev, email: e.target.value }))}
            />
          </label>
          <label>
            Joining Date
            <input
              type="date"
              value={memberForm.joiningDate}
              onChange={(e) => setMemberForm((prev) => ({ ...prev, joiningDate: e.target.value }))}
              required
            />
          </label>
          <label>
            Membership Start
            <input
              type="date"
              value={memberForm.membershipStartDate}
              onChange={(e) => setMemberForm((prev) => ({ ...prev, membershipStartDate: e.target.value }))}
              required
            />
          </label>
          <label>
            Membership End
            <input
              type="date"
              value={memberForm.membershipEndDate}
              onChange={(e) => setMemberForm((prev) => ({ ...prev, membershipEndDate: e.target.value }))}
              required
            />
          </label>
          <label>
            Membership Status
            <select
              value={memberForm.membershipStatus}
              onChange={(e) => setMemberForm((prev) => ({ ...prev, membershipStatus: e.target.value }))}
            >
              <option value="ACTIVE">ACTIVE</option>
              <option value="EXPIRED">EXPIRED</option>
              <option value="OVERDUE">OVERDUE</option>
            </select>
          </label>
          <label>
            Notes
            <input
              value={memberForm.notes}
              onChange={(e) => setMemberForm((prev) => ({ ...prev, notes: e.target.value }))}
            />
          </label>
          <button type="submit" className="primary-btn" disabled={createLoading}>
            {createLoading ? 'Creating...' : 'Create Member'}
          </button>
        </form>
      </section>

      <section className="card">
        <h2>Members</h2>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Phone</th>
                <th>Email</th>
                <th>End Date</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {members.map((member) => (
                <tr key={member.id}>
                  <td>{member.id}</td>
                  <td>{member.fullName}</td>
                  <td>{member.phoneNumber}</td>
                  <td>{member.email ?? '-'}</td>
                  <td>{member.membershipEndDate}</td>
                  <td><span className="status-chip">{member.membershipStatus}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {error ? <p className="error-text">{error}</p> : null}
    </main>
  )
}

export default App
