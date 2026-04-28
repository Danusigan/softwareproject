import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import axios from 'axios'
import Header from '../components/Header'
import Footer from '../components/Footer'
import authService from '../services/authService'

const API = 'http://localhost:8080'
const token = () => localStorage.getItem('token')
const auth = () => ({ headers: { Authorization: `Bearer ${token()}` } })

export default function AdminDashboard() {
  const navigate = useNavigate()
  const user = authService.getUserInfo()
  const [tab, setTab] = useState('modules') // 'modules' | 'lecturers' | 'pending'
  const [modules, setModules] = useState([])
  const [pendingMappings, setPendingMappings] = useState([])
  const [loading, setLoading] = useState(true)
  const [msg, setMsg] = useState(null)

  // Module form
  const [showModuleForm, setShowModuleForm] = useState(false)
  const [editingModule, setEditingModule] = useState(null)
  const [moduleForm, setModuleForm] = useState({ moduleId: '', moduleName: '' })

  // Lecturer form
  const [showLecturerForm, setShowLecturerForm] = useState(false)
  const [lecturerForm, setLecturerForm] = useState({ username: '', email: '', password: '' })

  useEffect(() => {
    const role = (user?.userType || '').toLowerCase()
    if (role !== 'admin' && role !== 'superadmin') navigate('/', { replace: true })
    fetchModules()
    fetchPendingMappings()
  }, [])

  const fetchModules = async () => {
    try {
      const res = await axios.get(`${API}/api/modules/all`, auth())
      setModules(res.data.data || [])
    } catch { setModules([]) } finally { setLoading(false) }
  }

  const fetchPendingMappings = async () => {
    try {
      const res = await axios.get(`${API}/api/lo-po-mapping/admin/pending`, auth())
      setPendingMappings(res.data.data || [])
    } catch { setPendingMappings([]) }
  }

  const flash = (type, text) => {
    setMsg({ type, text })
    setTimeout(() => setMsg(null), 3000)
  }

  const handleModuleSubmit = async (e) => {
    e.preventDefault()
    if (moduleForm.moduleId && !/^[A-Z0-9]+$/.test(moduleForm.moduleId)) {
      flash('error', 'Module ID must be uppercase letters and digits only.'); return
    }
    try {
      if (editingModule) {
        await axios.put(`${API}/api/modules/${editingModule.moduleId}`, moduleForm, auth())
        flash('success', 'Module updated!')
      } else {
        await axios.post(`${API}/api/modules/create`, moduleForm, auth())
        flash('success', 'Module created!')
      }
      setModuleForm({ moduleId: '', moduleName: '' }); setShowModuleForm(false); setEditingModule(null)
      fetchModules()
    } catch (err) { flash('error', err.response?.data?.message || 'Operation failed') }
  }

  const handleDeleteModule = async (moduleId) => {
    if (!confirm(`Delete module ${moduleId}?`)) return
    try {
      await axios.delete(`${API}/api/modules/${moduleId}`, auth())
      flash('success', 'Module deleted!')
      fetchModules()
    } catch (err) { flash('error', err.response?.data?.message || 'Failed to delete') }
  }

  const handleLecturerSubmit = async (e) => {
    e.preventDefault()
    try {
      await axios.post(`${API}/api/auth/add-user`,
        { userID: lecturerForm.username, email: lecturerForm.email, password: lecturerForm.password, usertype: 'lecture' },
        auth()
      )
      flash('success', 'Lecturer added successfully!')
      setLecturerForm({ username: '', email: '', password: '' }); setShowLecturerForm(false)
    } catch (err) { flash('error', err.response?.data?.message || 'Failed to add lecturer') }
  }

  const handleApproveMapping = async (mappingId) => {
    try {
      await axios.put(`${API}/api/lo-po-mapping/admin/${mappingId}/approve`, { adminRemarks: 'Approved' }, auth())
      flash('success', 'Mapping approved!')
      fetchPendingMappings()
    } catch { flash('error', 'Failed to approve') }
  }

  const handleRejectMapping = async (mappingId) => {
    const remarks = prompt('Enter rejection reason (required):')
    if (!remarks?.trim()) return
    try {
      await axios.put(`${API}/api/lo-po-mapping/admin/${mappingId}/reject`, { adminRemarks: remarks }, auth())
      flash('success', 'Mapping rejected.')
      fetchPendingMappings()
    } catch { flash('error', 'Failed to reject') }
  }

  const WEIGHT = { 0: 'None', 1: 'Low', 2: 'Medium', 3: 'High' }
  const WEIGHT_COLOR = { 0: 'text-slate-500', 1: 'text-blue-400', 2: 'text-amber-400', 3: 'text-emerald-400' }

  return (
    <div className="min-h-screen bg-[#0a0f1e] flex flex-col">
      <Header />
      <main className="flex-1 max-w-6xl mx-auto px-6 py-10 w-full">
        {/* Header */}
        <div className="flex items-start justify-between mb-8">
          <div>
            <p className="text-slate-500 text-sm">Admin Dashboard</p>
            <h1 className="text-white text-3xl font-black mt-1">Hello, {user?.username} 👋</h1>
          </div>
          <div className="flex items-center gap-2">
            {pendingMappings.length > 0 && (
              <div className="flex items-center gap-1.5 px-3 py-1.5 bg-amber-500/10 border border-amber-500/30 rounded-lg">
                <span className="w-1.5 h-1.5 bg-amber-400 rounded-full animate-pulse" />
                <span className="text-amber-400 text-xs font-medium">{pendingMappings.length} pending</span>
              </div>
            )}
          </div>
        </div>

        {/* Global message */}
        {msg && (
          <div className={`mb-6 p-3 rounded-xl text-xs font-medium border ${msg.type === 'success' ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400' : 'bg-red-500/10 border-red-500/30 text-red-400'}`}>
            {msg.text}
          </div>
        )}

        {/* Tabs */}
        <div className="flex gap-1 mb-8 p-1 bg-white/5 rounded-xl w-fit">
          {[
            { id: 'modules', label: `Modules (${modules.length})` },
            { id: 'lecturers', label: 'Add Lecturer' },
            { id: 'pending', label: `Pending Mappings${pendingMappings.length ? ` (${pendingMappings.length})` : ''}` },
          ].map(t => (
            <button key={t.id} onClick={() => setTab(t.id)}
              className={`px-4 py-2 rounded-lg text-xs font-bold transition-all ${tab === t.id ? 'bg-blue-600 text-white shadow-lg shadow-blue-500/25' : 'text-slate-400 hover:text-white'}`}>
              {t.label}
            </button>
          ))}
        </div>

        {/* Modules Tab */}
        {tab === 'modules' && (
          <div>
            <div className="flex items-center justify-between mb-5">
              <h2 className="text-white font-bold">Module Management</h2>
              <button onClick={() => { setShowModuleForm(s => !s); setEditingModule(null); setModuleForm({ moduleId: '', moduleName: '' }) }}
                className="px-4 py-2 bg-blue-600 text-white text-xs font-bold rounded-lg hover:bg-blue-500 transition-colors">
                {showModuleForm ? 'Cancel' : '+ Create Module'}
              </button>
            </div>

            {showModuleForm && (
              <form onSubmit={handleModuleSubmit} className="bg-white/5 border border-white/10 rounded-xl p-6 mb-6">
                <h3 className="text-white font-bold text-sm mb-4">{editingModule ? 'Edit Module' : 'Create New Module'}</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                  <div>
                    <label className="block text-slate-400 text-xs mb-1">Module ID (e.g. EE101)</label>
                    <input value={moduleForm.moduleId} onChange={e => setModuleForm(f => ({ ...f, moduleId: e.target.value.toUpperCase() }))}
                      placeholder="EE101" required disabled={!!editingModule}
                      className="w-full bg-white/5 border border-white/10 text-white placeholder-slate-600 rounded-lg px-3 py-2 text-xs focus:outline-none focus:border-blue-500 transition-colors disabled:opacity-50" />
                  </div>
                  <div>
                    <label className="block text-slate-400 text-xs mb-1">Module Name</label>
                    <input value={moduleForm.moduleName} onChange={e => setModuleForm(f => ({ ...f, moduleName: e.target.value }))}
                      placeholder="e.g. Digital Electronics" required
                      className="w-full bg-white/5 border border-white/10 text-white placeholder-slate-600 rounded-lg px-3 py-2 text-xs focus:outline-none focus:border-blue-500 transition-colors" />
                  </div>
                </div>
                <button type="submit" className="px-5 py-2 bg-blue-600 text-white text-xs font-bold rounded-lg hover:bg-blue-500 transition-colors">
                  {editingModule ? 'Update Module' : 'Create Module'}
                </button>
              </form>
            )}

            {loading ? (
              <div className="text-center py-16 text-slate-500 text-sm">Loading modules...</div>
            ) : modules.length === 0 ? (
              <div className="text-center py-16 bg-white/3 border border-white/8 rounded-xl">
                <p className="text-slate-500 text-sm">No modules yet. Create your first module.</p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {modules.map((m, i) => (
                  <div key={m.moduleId} className="bg-white/5 border border-white/10 rounded-xl p-5 hover:border-white/20 transition-all group">
                    <div className="flex items-start justify-between mb-3">
                      <div className={`px-2.5 py-1 rounded-lg text-xs font-black ${['bg-blue-500/20 text-blue-400','bg-purple-500/20 text-purple-400','bg-emerald-500/20 text-emerald-400','bg-amber-500/20 text-amber-400'][i % 4]}`}>
                        {m.moduleId}
                      </div>
                      <div className="flex gap-1.5 opacity-0 group-hover:opacity-100 transition-opacity">
                        <button onClick={() => { setEditingModule(m); setModuleForm({ moduleId: m.moduleId, moduleName: m.moduleName }); setShowModuleForm(true) }}
                          className="p-1.5 bg-white/5 rounded-lg text-slate-400 hover:text-blue-400 hover:bg-blue-500/10 transition-colors text-xs">✏️</button>
                        <button onClick={() => handleDeleteModule(m.moduleId)}
                          className="p-1.5 bg-white/5 rounded-lg text-slate-400 hover:text-red-400 hover:bg-red-500/10 transition-colors text-xs">🗑</button>
                      </div>
                    </div>
                    <h3 className="text-white font-bold text-sm">{m.moduleName}</h3>
                    <p className="text-slate-500 text-xs mt-1">{(m.losList || []).length} learning outcomes</p>
                    <button onClick={() => navigate('/modules')} className="mt-3 text-blue-400 text-xs hover:text-blue-300 transition-colors">
                      View LOs →
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Lecturers Tab */}
        {tab === 'lecturers' && (
          <div className="max-w-md">
            <h2 className="text-white font-bold mb-5">Add New Lecturer</h2>
            <div className="bg-white/5 border border-white/10 rounded-xl p-6">
              <form onSubmit={handleLecturerSubmit} className="space-y-4">
                {[
                  { field: 'username', label: 'Username', type: 'text', placeholder: 'e.g. john_doe' },
                  { field: 'email', label: 'Email', type: 'email', placeholder: 'lecturer@eng.ruh.ac.lk' },
                  { field: 'password', label: 'Password', type: 'password', placeholder: 'Set initial password' },
                ].map(({ field, label, type, placeholder }) => (
                  <div key={field}>
                    <label className="block text-slate-400 text-xs mb-1">{label}</label>
                    <input type={type} value={lecturerForm[field]}
                      onChange={e => setLecturerForm(f => ({ ...f, [field]: e.target.value }))}
                      placeholder={placeholder} required
                      className="w-full bg-white/5 border border-white/10 text-white placeholder-slate-600 rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:border-blue-500 transition-colors" />
                  </div>
                ))}
                <button type="submit" className="w-full py-2.5 bg-blue-600 text-white font-bold rounded-lg hover:bg-blue-500 transition-colors text-sm">
                  Create Lecturer Account
                </button>
              </form>
            </div>
          </div>
        )}

        {/* Pending Mappings Tab */}
        {tab === 'pending' && (
          <div>
            <h2 className="text-white font-bold mb-5">Pending LO-PO Mapping Approvals</h2>
            {pendingMappings.length === 0 ? (
              <div className="text-center py-16 bg-white/3 border border-white/8 rounded-xl">
                <p className="text-emerald-400 text-sm">✅ No pending mappings — all caught up!</p>
              </div>
            ) : (
              <div className="space-y-3">
                {pendingMappings.map(m => (
                  <div key={m.id} className="bg-white/5 border border-white/10 rounded-xl p-5 flex items-start justify-between gap-4">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 mb-2">
                        <span className="px-2 py-0.5 bg-amber-500/10 border border-amber-500/30 text-amber-400 text-xs font-bold rounded">PENDING</span>
                        <span className={`text-xs font-bold ${WEIGHT_COLOR[m.weight] || 'text-slate-400'}`}>
                          Weight: {WEIGHT[m.weight] || m.weight}
                        </span>
                      </div>
                      <p className="text-white text-sm font-medium">
                        LO: <span className="text-blue-400">{m.learningOutcome?.name || m.learningOutcome?.id}</span>
                        {' → '}
                        PO: <span className="text-purple-400">{m.programOutcome?.code || m.programOutcome?.poId}</span>
                      </p>
                      {m.lecturerRemarks && <p className="text-slate-500 text-xs mt-1">Remarks: {m.lecturerRemarks}</p>}
                    </div>
                    <div className="flex gap-2 flex-shrink-0">
                      <button onClick={() => handleApproveMapping(m.id)}
                        className="px-3 py-1.5 bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-bold rounded-lg hover:bg-emerald-500/20 transition-colors">
                        Approve
                      </button>
                      <button onClick={() => handleRejectMapping(m.id)}
                        className="px-3 py-1.5 bg-red-500/10 border border-red-500/30 text-red-400 text-xs font-bold rounded-lg hover:bg-red-500/20 transition-colors">
                        Reject
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </main>
      <Footer />
    </div>
  )
}
