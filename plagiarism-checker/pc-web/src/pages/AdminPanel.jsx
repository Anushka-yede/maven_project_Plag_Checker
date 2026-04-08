import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import api from '../api/client'

export default function AdminPanel() {
  const qc = useQueryClient()
  const navigate = useNavigate()
  const [activeTab, setActiveTab] = useState('users')

  const { data: users = [], isLoading: loadingUsers } = useQuery({
    queryKey: ['admin-users'],
    queryFn: () => api.get('/admin/users').then(r => r.data),
    enabled: activeTab === 'users',
  })

  const { data: stats } = useQuery({
    queryKey: ['admin-stats'],
    queryFn: () => api.get('/admin/stats').then(r => r.data),
  })

  const { data: submissions = [] } = useQuery({
    queryKey: ['admin-submissions'],
    queryFn: () => api.get('/admin/submissions').then(r => r.data),
    enabled: activeTab === 'submissions',
  })

  const roleChange = useMutation({
    mutationFn: ({ id, role }) => api.put(`/admin/users/${id}/role`, { role }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin-users'] }),
  })

  const TABS = ['users', 'submissions', 'webhooks']
  const ROLES = ['STUDENT', 'INSTRUCTOR', 'ADMIN']

  return (
    <div className="animate-slide-up space-y-6">
      <button 
        onClick={() => navigate('/dashboard')}
        className="flex items-center gap-2 text-slate-400 hover:text-white transition-colors text-sm font-medium mb-2"
      >
        <ArrowLeft size={16} /> Back to Dashboard
      </button>

      <div>
        <h1 className="text-3xl font-bold text-white flex items-center gap-3">
          <span>⚙</span> Admin Panel
        </h1>
        <p className="text-slate-400 mt-1">System management — restricted to ADMIN role.</p>
      </div>

      {/* Stats */}
      {stats && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {[
            { label: 'Total Users',       value: stats.totalUsers,       icon: '👥' },
            { label: 'Total Submissions', value: stats.totalSubmissions, icon: '📋' },
            { label: 'Server Status',     value: 'Online',               icon: '🟢' },
            { label: 'Server Time',       value: new Date(stats.serverTime).toLocaleTimeString(), icon: '🕐' },
          ].map(s => (
            <div key={s.label} className="card flex items-center gap-4">
              <span className="text-3xl">{s.icon}</span>
              <div>
                <p className="text-xl font-bold text-white">{s.value}</p>
                <p className="text-xs text-slate-400">{s.label}</p>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Tabs */}
      <div className="flex rounded-xl p-1 gap-1 w-fit" style={{ background: 'rgba(0,0,0,0.3)' }}>
        {TABS.map(t => (
          <button key={t} onClick={() => setActiveTab(t)}
                  className={`px-5 py-2 rounded-lg text-sm font-semibold capitalize transition-all
                    ${activeTab === t ? 'bg-blue-600 text-white shadow-lg' : 'text-slate-400 hover:text-white'}`}>
            {t}
          </button>
        ))}
      </div>

      {/* Users tab */}
      {activeTab === 'users' && (
        <div className="card overflow-hidden">
          <h2 className="text-lg font-bold text-white mb-4">User Management</h2>
          {loadingUsers ? (
            <div className="animate-pulse space-y-3">
              {[1,2,3].map(i => <div key={i} className="h-12 bg-slate-700 rounded-xl" />)}
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-slate-700">
                    {['Email','Role','Full Name','Created','Actions'].map(h => (
                      <th key={h} className="text-left py-3 px-4 text-xs text-slate-400 font-semibold uppercase tracking-wider">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800">
                  {users.map(u => (
                    <tr key={u.id} className="hover:bg-slate-800/30 transition-colors">
                      <td className="py-3 px-4 text-white font-mono text-xs">{u.email}</td>
                      <td className="py-3 px-4">
                        <span className={u.role === 'ADMIN' ? 'badge-red' : u.role === 'INSTRUCTOR' ? 'badge-blue' : 'badge-gray'}>
                          {u.role}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-slate-300">{u.fullName || '—'}</td>
                      <td className="py-3 px-4 text-slate-400 text-xs">
                        {new Date(u.createdAt).toLocaleDateString()}
                      </td>
                      <td className="py-3 px-4">
                        <select
                          value={u.role}
                          onChange={e => roleChange.mutate({ id: u.id, role: e.target.value })}
                          className="text-xs bg-slate-800 border border-slate-700 rounded-lg px-2 py-1
                                     text-slate-300 focus:outline-none focus:ring-1 focus:ring-blue-500">
                          {ROLES.map(r => <option key={r}>{r}</option>)}
                        </select>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* Submissions tab */}
      {activeTab === 'submissions' && (
        <div className="card overflow-hidden">
          <h2 className="text-lg font-bold text-white mb-4">All Submissions</h2>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-700">
                  {['Filename','Status','Version','Assignment','Created'].map(h => (
                    <th key={h} className="text-left py-3 px-4 text-xs text-slate-400 font-semibold uppercase tracking-wider">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {submissions.map(s => (
                  <tr key={s.id} className="hover:bg-slate-800/30 transition-colors">
                    <td className="py-3 px-4 text-white">{s.filename}</td>
                    <td className="py-3 px-4">
                      <span className={s.status === 'DONE' ? 'badge-green' : s.status === 'FAILED' ? 'badge-red' : 'badge-gray'}>
                        {s.status}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-slate-400">v{s.version}</td>
                    <td className="py-3 px-4 text-slate-400">{s.assignmentId || '—'}</td>
                    <td className="py-3 px-4 text-slate-400 text-xs">{new Date(s.createdAt).toLocaleDateString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Webhooks tab */}
      {activeTab === 'webhooks' && (
        <WebhookConfig />
      )}
    </div>
  )
}

function WebhookConfig() {
  const [assignmentId, setAssignmentId] = useState('')
  const [webhookUrl,   setWebhookUrl]   = useState('')
  const [saved,        setSaved]        = useState(false)

  const handleSave = async () => {
    try {
      await api.put(`/admin/webhooks/${assignmentId}`, { webhookUrl })
      setSaved(true)
      setTimeout(() => setSaved(false), 3000)
    } catch (err) {
      alert('Failed to save webhook: ' + (err.response?.data?.detail || err.message))
    }
  }

  return (
    <div className="card max-w-xl space-y-5">
      <h2 className="text-lg font-bold text-white">Webhook Configuration</h2>
      <p className="text-sm text-slate-400">
        Configure a POST webhook URL per assignment. Triggered on batch job completion.
      </p>
      <div>
        <label className="block text-xs text-slate-400 mb-1.5 font-medium">Assignment ID</label>
        <input className="input" placeholder="e.g. CS101-HW3" value={assignmentId}
               onChange={e => setAssignmentId(e.target.value)} />
      </div>
      <div>
        <label className="block text-xs text-slate-400 mb-1.5 font-medium">Webhook URL</label>
        <input className="input" type="url" placeholder="https://your-server.com/webhook"
               value={webhookUrl} onChange={e => setWebhookUrl(e.target.value)} />
      </div>
      <button onClick={handleSave} disabled={!assignmentId || !webhookUrl} className="btn-primary">
        {saved ? '✅ Saved!' : '💾 Save Webhook'}
      </button>
    </div>
  )
}

