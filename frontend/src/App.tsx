import './App.css'

const featureCards = [
  {
    title: 'Member Lifecycle',
    description: 'Track join date, plan, expiry, renewal status, and payment notes in one clean flow.',
    tag: 'Operations',
  },
  {
    title: 'Automated Reminders',
    description: 'Trigger WhatsApp and email reminders before expiry and for overdue renewals.',
    tag: 'Automation',
  },
  {
    title: 'Multi-Tenant SaaS',
    description: 'Organization-scoped access with role-based controls for SUPER_ADMIN and ORG_ADMIN.',
    tag: 'Security',
  },
]

const stats = [
  { label: 'Active Members', value: '1,248', trend: '+8.2%' },
  { label: 'Expiring This Week', value: '76', trend: '-12%' },
  { label: 'Reminder Delivery', value: '99.1%', trend: '+1.1%' },
  { label: 'Monthly Revenue', value: '₹4.9L', trend: '+16%' },
]

const upcomingRenewals = [
  { name: 'Rahul Sharma', plan: '12 Months', dueIn: 'Today', status: 'Urgent' },
  { name: 'Nisha Verma', plan: '6 Months', dueIn: '1 day', status: 'Due Soon' },
  { name: 'Aarav Singh', plan: '3 Months', dueIn: '2 days', status: 'Reminder Sent' },
  { name: 'Sneha Patel', plan: '12 Months', dueIn: '4 days', status: 'Scheduled' },
]

function App() {
  return (
    <div className="page">
      <header className="topbar">
        <p className="brand">GymFlow</p>
        <button type="button" className="primary-btn">Start Free Trial</button>
      </header>

      <section className="hero">
        <div className="hero-copy">
          <p className="eyebrow">Multi-tenant Gym Membership SaaS</p>
          <h1>Beautiful gym operations dashboard for memberships, renewals, and reminders.</h1>
          <p className="subtitle">
            Run your fitness business with calm clarity: automate renewal reminders, manage members,
            and monitor growth from one premium interface.
          </p>
          <div className="hero-actions">
            <button type="button" className="primary-btn">Get Demo</button>
            <button type="button" className="ghost-btn">View Architecture</button>
          </div>
        </div>

        <div className="hero-panel">
          <p className="panel-title">Today at a glance</p>
          <div className="mini-grid">
            {stats.slice(0, 2).map((item) => (
              <article key={item.label} className="mini-card">
                <p>{item.label}</p>
                <strong>{item.value}</strong>
                <span>{item.trend}</span>
              </article>
            ))}
          </div>
          <p className="panel-footnote">Smart reminders scheduled at 10:00 AM daily</p>
        </div>
      </section>

      <section className="stats-grid">
        {stats.map((item) => (
          <article className="stat-card" key={item.label}>
            <p>{item.label}</p>
            <h3>{item.value}</h3>
            <span>{item.trend} vs last month</span>
          </article>
        ))}
      </section>

      <section className="feature-grid">
        {featureCards.map((feature) => (
          <article className="feature-card" key={feature.title}>
            <p className="chip">{feature.tag}</p>
            <h2>{feature.title}</h2>
            <p>{feature.description}</p>
          </article>
        ))}
      </section>

      <section className="renewals">
        <div className="renewals-header">
          <h2>Upcoming Renewals</h2>
          <p>Prioritized by due date and reminder state</p>
        </div>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Member</th>
                <th>Plan</th>
                <th>Due In</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {upcomingRenewals.map((row) => (
                <tr key={row.name}>
                  <td>{row.name}</td>
                  <td>{row.plan}</td>
                  <td>{row.dueIn}</td>
                  <td><span className="status">{row.status}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <footer className="footer">
        <p>GymFlow — simple, automated, reliable membership operations.</p>
      </footer>
    </div>
  )
}

export default App
