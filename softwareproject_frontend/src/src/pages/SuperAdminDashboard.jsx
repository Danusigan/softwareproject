import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import axios from 'axios'
import Header from '../components/Header'
import Footer from '../components/Footer'
import authService from '../services/authService'

export default function SuperAdminDashboard() {
  const navigate = useNavigate()
  const [showAddAdmin, setShowAddAdmin] = useState(false)
  const [form, setForm] = useState({ username: '', email: '', password: '' })
  const [loading, setLoading] = useState(false)
  const [msg, setMsg] = useState(null)
  const user = authService.getUserInfo()

  useEffect(() => {
    const role = (user?.userType || '').toLowerCase()
    if (role !== 'superadmin') navigate('/', { replace: true })
  }, [])

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true); setMsg(null)
    try {
      const token = localStorage.getItem('token')
      await axios.post('http://localhost:8080/api/auth/add-user',
        { userID: form.username, email: form.email, password: form.password, usertype: 'admin' },
        { headers: { Authorization: `Bearer ${token}` } }
      )
      setMsg({ type: 'success', text: 'Admin added successfully!' })
      setForm({ username: '', email: '', password: '' })
      setTimeout(() => { setShowAddAdmin(false); setMsg(null) }, 2000)
    } catch (err) {
      setMsg({ type: 'error', text: err.response?.data?.message || 'Failed to add admin' })
    } finally { setLoading(false) }
  }

  return (
    <div className="min-h-screen bg-[#0a0f1e] flex flex-col">
      <Header />
      <main className="flex-1 max-w-5xl mx-auto px-6 py-12 w-full">
        {/* Welcome */}
        <div className="mb-10">
          <p className="text-slate-500 text-sm mb-1">Superadmin Dashboard</p>
          <h1 className="text-white text-3xl font-black">Welcome, {user?.username} 👋</h1>
          <p className="text-slate-400 text-sm mt-2">You have full control over the OBE system. Manage admins and oversee all operations.</p>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-5 mb-10">
          {[
            { label: 'Your Role', value: 'Superadmin', color: 'text-purple-400', bg: 'bg-purple-500/10 border-purple-500/20' },
            { label: 'Access Level', value: 'Full Access', color: 'text-emerald-400', bg: 'bg-emerald-500/10 border-emerald-500/20' },
            { label: 'System', value: 'Operational', color: 'text-blue-400', bg: 'bg-blue-500/10 border-blue-500/20' },
          ].map(({ label, value, color, bg }) => (
            <div key={label} className={`border rounded-xl p-6 ${bg}`}>
              <p className="text-slate-500 text-xs mb-2">{label}</p>
              <p className={`text-xl font-black ${color}`}>{value}</p>
            </div>
          ))}
        </div>

        {/* Actions */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          {/* Add Admin */}
          <div className="bg-white/5 border border-white/10 rounded-xl p-6">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h2 className="text-white font-bold">Add Admin</h2>
                <p className="text-slate-500 text-xs mt-0.5">Create a new admin account</p>
              </div>
              <button onClick={() => setShowAddAdmin(s => !s)} className="px-4 py-2 bg-blue-600 text-white text-xs font-bold rounded-lg hover:bg-blue-500 transition-colors">
                {showAddAdmin ? 'Cancel' : '+ Add Admin'}
              </button>
            </div>

            {showAddAdmin && (
              <form onSubmit={handleSubmit} className="space-y-3 pt-4 border-t border-white/10">
                {msg && (
                  <div className={`p-3 rounded-lg text-xs font-medium ${msg.type === 'success' ? 'bg-emerald-500/10 border border-emerald-500/30 text-emerald-400' : 'bg-red-500/10 border border-red-500/30 text-red-400'}`}>
                    {msg.text}
                  </div>
                )}
                {['username', 'email', 'password'].map(field => (
                  <div key={field}>
                    <label className="block text-slate-400 text-xs mb-1 capitalize">{field}</label>
                    <input
                      type={field === 'password' ? 'password' : field === 'email' ? 'email' : 'text'}
                      value={form[field]}
                      onChange={e => setForm(f => ({ ...f, [field]: e.target.value }))}
                      placeholder={`Enter ${field}`}
                      required
                      className="w-full bg-white/5 border border-white/10 text-white placeholder-slate-600 rounded-lg px-3 py-2 text-xs focus:outline-none focus:border-blue-500 transition-colors"
                    />
                  </div>
                ))}
                <button type="submit" disabled={loading} className="w-full py-2 bg-blue-600 text-white text-xs font-bold rounded-lg hover:bg-blue-500 disabled:opacity-50 transition-colors">
                  {loading ? 'Creating...' : 'Create Admin Account'}
                </button>
              </form>
            )}
          </div>

          {/* Info Card */}
          <div className="bg-gradient-to-br from-blue-600/10 to-indigo-600/10 border border-blue-500/20 rounded-xl p-6">
            <h2 className="text-white font-bold mb-3">Superadmin Privileges</h2>
            <ul className="space-y-2">
              {[
                'Create and manage admin accounts',
                'Full access to all system modules',
                'View all LO-PO mappings',
                'Access program outcome management',
                'Approve or reject mapping requests',
              ].map(item => (
                <li key={item} className="flex items-center gap-2 text-slate-400 text-xs">
                  <span className="w-1.5 h-1.5 bg-blue-400 rounded-full flex-shrink-0" />
                  {item}
                </li>
              ))}
            </ul>
          </div>
        </div>
      </main>
      <Footer />
    </div>
  )
}
