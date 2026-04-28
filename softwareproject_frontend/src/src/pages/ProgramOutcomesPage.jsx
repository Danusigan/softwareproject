import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import axios from 'axios'
import Header from '../components/Header'
import Footer from '../components/Footer'
import authService from '../services/authService'

const API = 'http://localhost:8080'
const auth = () => ({ headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } })

export default function ProgramOutcomesPage() {
  const navigate = useNavigate()
  const user = authService.getUserInfo()
  const [pos, setPOs] = useState([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [editingPO, setEditingPO] = useState(null)
  const [msg, setMsg] = useState(null)
  const [initLoading, setInitLoading] = useState(false)
  const [form, setForm] = useState({ poId: '', code: '', title: '', description: '', category: '', performanceIndicators: '' })

  useEffect(() => {
    const role = (user?.userType || '').toLowerCase()
    if (role !== 'admin' && role !== 'superadmin') navigate('/', { replace: true })
    fetchPOs()
  }, [])

  const flash = (type, text) => { setMsg({ type, text }); setTimeout(() => setMsg(null), 3000) }

  const fetchPOs = async () => {
    try {
      const res = await axios.get(`${API}/api/program-outcomes/all-including-inactive`, auth())
      setPOs(res.data.data || [])
    } catch { setPOs([]) } finally { setLoading(false) }
  }

  const handleInitDefaults = async () => {
    if (!confirm('Initialize the 12 Washington Accord POs? This may add default POs.')) return
    setInitLoading(true)
    try {
      await axios.post(`${API}/api/program-outcomes/initialize-defaults`, {}, auth())
      flash('success', 'Washington Accord POs initialized!')
      fetchPOs()
    } catch (err) { flash('error', err.response?.data?.message || 'Failed') } finally { setInitLoading(false) }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      if (editingPO) {
        await axios.put(`${API}/api/program-outcomes/${editingPO.poId}`, form, auth())
        flash('success', 'PO updated!')
      } else {
        await axios.post(`${API}/api/program-outcomes/create`, form, auth())
        flash('success', 'Program Outcome created!')
      }
      setForm({ poId: '', code: '', title: '', description: '', category: '', performanceIndicators: '' })
      setShowForm(false); setEditingPO(null)
      fetchPOs()
    } catch (err) { flash('error', err.response?.data?.message || 'Failed') }
  }

  const handleDelete = async (poId) => {
    if (!confirm(`Deactivate PO ${poId}?`)) return
    try {
      await axios.delete(`${API}/api/program-outcomes/${poId}`, auth())
      flash('success', 'PO deactivated')
      fetchPOs()
    } catch { flash('error', 'Failed to deactivate') }
  }

  const handleRestore = async (poId) => {
    try {
      await axios.put(`${API}/api/program-outcomes/${poId}/restore`, {}, auth())
      flash('success', 'PO restored')
      fetchPOs()
    } catch { flash('error', 'Failed to restore') }
  }

  const CATEGORIES = ['Knowledge', 'Skills', 'Attitude', 'Engineering', 'Problem Solving', 'Design', 'Investigation', 'Tools', 'Society', 'Ethics', 'Communication', 'Management', 'Life-long Learning']

  return (
    <div className="min-h-screen bg-[#0a0f1e] flex flex-col">
      <Header />
      <main className="flex-1 max-w-6xl mx-auto px-6 py-10 w-full">
        {/* Header */}
        <div className="flex items-start justify-between mb-8 flex-wrap gap-4">
          <div>
            <p className="text-slate-500 text-sm">Admin · OBE System</p>
            <h1 className="text-white text-3xl font-black mt-1">Program Outcomes</h1>
            <p className="text-slate-400 text-sm mt-1">{pos.filter(p => p.isActive !== false).length} active POs</p>
          </div>
          <div className="flex gap-2 flex-wrap">
            <button onClick={handleInitDefaults} disabled={initLoading}
              className="px-4 py-2 bg-purple-600/20 border border-purple-500/30 text-purple-400 text-xs font-bold rounded-xl hover:bg-purple-600/30 transition-colors disabled:opacity-50">
              {initLoading ? 'Initializing...' : '⚡ Init Washington Accord POs'}
            </button>
            <button onClick={() => { setShowForm(s => !s); setEditingPO(null); setForm({ poId: '', code: '', title: '', description: '', category: '', performanceIndicators: '' }) }}
              className="px-4 py-2 bg-blue-600 text-white text-xs font-bold rounded-xl hover:bg-blue-500 transition-colors">
              {showForm ? 'Cancel' : '+ Create PO'}
            </button>
          </div>
        </div>

        {msg && (
          <div className={`mb-6 p-3 rounded-xl text-xs font-medium border ${msg.type === 'success' ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400' : 'bg-red-500/10 border-red-500/30 text-red-400'}`}>
            {msg.text}
          </div>
        )}

        {/* Create/Edit Form */}
        {showForm && (
          <form onSubmit={handleSubmit} className="bg-white/5 border border-white/10 rounded-xl p-6 mb-8">
            <h2 className="text-white font-bold text-sm mb-5">{editingPO ? `Edit ${editingPO.poId}` : 'Create New Program Outcome'}</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
              {!editingPO && (
                <>
                  <div>
                    <label className="block text-slate-400 text-xs mb-1">PO ID (e.g. PO1)</label>
                    <input value={form.poId} onChange={e => setForm(f => ({ ...f, poId: e.target.value }))} placeholder="PO1" required
                      className="w-full bg-white/5 border border-white/10 text-white placeholder-slate-600 rounded-lg px-3 py-2 text-xs focus:outline-none focus:border-blue-500 transition-colors" />
                  </div>
                  <div>
                    <label className="block text-slate-400 text-xs mb-1">Code</label>
                    <input value={form.code} onChange={e => setForm(f => ({ ...f, code: e.target.value }))} placeholder="PO1" required
                      className="w-full bg-white/5 border border-white/10 text-white placeholder-slate-600 rounded-lg px-3 py-2 text-xs focus:outline-none focus:border-blue-500 transition-colors" />
                  </div>
                </>
              )}
              <div className={editingPO ? '' : 'md:col-span-2'}>
                <label className="block text-slate-400 text-xs mb-1">Title</label>
                <input value={form.title} onChange={e => setForm(f => ({ ...f, title: e.target.value }))} placeholder="e.g. Engineering Knowledge" required
                  className="w-full bg-white/5 border border-white/10 text-white placeholder-slate-600 rounded-lg px-3 py-2 text-xs focus:outline-none focus:border-blue-500 transition-colors" />
              </div>
              <div>
                <label className="block text-slate-400 text-xs mb-1">Category</label>
                <select value={form.category} onChange={e => setForm(f => ({ ...f, category: e.target.value }))}
                  className="w-full bg-white/5 border border-white/10 text-white rounded-lg px-3 py-2 text-xs focus:outline-none focus:border-blue-500 transition-colors">
                  <option value="" className="bg-[#0a0f1e]">Select category</option>
                  {CATEGORIES.map(c => <option key={c} value={c} className="bg-[#0a0f1e]">{c}</option>)}
                </select>
              </div>
              <div className="md:col-span-2">
                <label className="block text-slate-400 text-xs mb-1">Description</label>
                <textarea value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} rows={2} placeholder="Describe this program outcome..."
                  className="w-full bg-white/5 border border-white/10 text-white placeholder-slate-600 rounded-lg px-3 py-2 text-xs focus:outline-none focus:border-blue-500 transition-colors resize-none" />
              </div>
              <div className="md:col-span-2">
                <label className="block text-slate-400 text-xs mb-1">Performance Indicators</label>
                <textarea value={form.performanceIndicators} onChange={e => setForm(f => ({ ...f, performanceIndicators: e.target.value }))} rows={2} placeholder="Measurable indicators..."
                  className="w-full bg-white/5 border border-white/10 text-white placeholder-slate-600 rounded-lg px-3 py-2 text-xs focus:outline-none focus:border-blue-500 transition-colors resize-none" />
              </div>
            </div>
            <div className="flex gap-2">
              <button type="submit" className="px-5 py-2 bg-blue-600 text-white text-xs font-bold rounded-lg hover:bg-blue-500 transition-colors">
                {editingPO ? 'Update PO' : 'Create Program Outcome'}
              </button>
              <button type="button" onClick={() => { setShowForm(false); setEditingPO(null) }} className="px-5 py-2 bg-white/5 text-slate-400 text-xs font-bold rounded-lg hover:bg-white/10 transition-colors">
                Cancel
              </button>
            </div>
          </form>
        )}

        {/* PO List */}
        {loading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {[...Array(4)].map((_, i) => <div key={i} className="h-32 bg-white/5 rounded-xl animate-pulse" />)}
          </div>
        ) : pos.length === 0 ? (
          <div className="text-center py-20 bg-white/3 border border-white/8 rounded-xl">
            <p className="text-4xl mb-3">🎯</p>
            <p className="text-slate-400 text-sm mb-4">No program outcomes yet.</p>
            <button onClick={handleInitDefaults} className="px-5 py-2.5 bg-purple-600 text-white text-xs font-bold rounded-xl hover:bg-purple-500 transition-colors">
              Initialize Washington Accord POs
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {pos.map((po, i) => (
              <div key={po.poId} className={`bg-white/5 border rounded-xl p-5 transition-all ${po.isActive === false ? 'border-white/5 opacity-50' : 'border-white/10 hover:border-white/20'}`}>
                <div className="flex items-start justify-between mb-3">
                  <div className="flex items-center gap-2">
                    <span className="px-2.5 py-1 bg-purple-500/20 text-purple-400 text-xs font-black rounded-lg">{po.code || po.poId}</span>
                    {po.isDefault && <span className="px-2 py-0.5 bg-blue-500/10 border border-blue-500/30 text-blue-400 text-xs rounded-full">Washington</span>}
                    {po.isActive === false && <span className="px-2 py-0.5 bg-slate-500/10 border border-slate-500/30 text-slate-500 text-xs rounded-full">Inactive</span>}
                  </div>
                  <div className="flex gap-1.5">
                    {po.isActive !== false ? (
                      <>
                        <button onClick={() => { setEditingPO(po); setForm({ poId: po.poId, code: po.code, title: po.title || '', description: po.description || '', category: po.category || '', performanceIndicators: po.performanceIndicators || '' }); setShowForm(true) }}
                          className="p-1.5 text-slate-500 hover:text-blue-400 hover:bg-blue-500/10 rounded-lg transition-colors text-xs">✏️</button>
                        <button onClick={() => handleDelete(po.poId)}
                          className="p-1.5 text-slate-500 hover:text-red-400 hover:bg-red-500/10 rounded-lg transition-colors text-xs">🗑</button>
                      </>
                    ) : (
                      <button onClick={() => handleRestore(po.poId)}
                        className="px-2 py-1 text-slate-500 hover:text-emerald-400 bg-white/5 hover:bg-emerald-500/10 rounded-lg transition-colors text-xs font-medium">
                        Restore
                      </button>
                    )}
                  </div>
                </div>
                <h3 className="text-white font-bold text-sm">{po.title}</h3>
                {po.category && <span className="inline-block mt-1.5 px-2 py-0.5 bg-white/5 text-slate-400 text-xs rounded-full">{po.category}</span>}
                {po.description && <p className="text-slate-500 text-xs mt-2 line-clamp-2">{po.description}</p>}
              </div>
            ))}
          </div>
        )}
      </main>
      <Footer />
    </div>
  )
}
