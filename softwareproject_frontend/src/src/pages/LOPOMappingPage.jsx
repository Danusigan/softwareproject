import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import axios from 'axios'
import Header from '../components/Header'
import Footer from '../components/Footer'
import authService from '../services/authService'

const API = 'http://localhost:8080'
const auth = () => ({ headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } })

const WEIGHT_LABEL = { 0: 'None', 1: 'Low', 2: 'Medium', 3: 'High' }
const WEIGHT_COLOR = { 0: 'bg-slate-500/20 text-slate-400', 1: 'bg-blue-500/20 text-blue-400', 2: 'bg-amber-500/20 text-amber-400', 3: 'bg-emerald-500/20 text-emerald-400' }
const STATUS_CLS = {
  APPROVED: 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400',
  PENDING: 'bg-amber-500/10 border-amber-500/30 text-amber-400',
  REJECTED: 'bg-red-500/10 border-red-500/30 text-red-400',
}

export default function LOPOMappingPage() {
  const navigate = useNavigate()
  const user = authService.getUserInfo()
  const role = (user?.userType || '').toLowerCase()
  const isAdmin = role === 'admin' || role === 'superadmin'

  const [mappings, setMappings] = useState([])
  const [pos, setPOs] = useState([])
  const [los, setLos] = useState([])
  const [modules, setModules] = useState([])
  const [loading, setLoading] = useState(true)
  const [msg, setMsg] = useState(null)
  const [stats, setStats] = useState(null)
  const [filterStatus, setFilterStatus] = useState('')
  const [filterModule, setFilterModule] = useState('')
  const [showCreateForm, setShowCreateForm] = useState(false)
  const [createForm, setCreateForm] = useState({ loId: '', mappings: {}, remarks: '' })

  const flash = (type, text) => { setMsg({ type, text }); setTimeout(() => setMsg(null), 3500) }

  useEffect(() => {
    fetchAll()
  }, [])

  const fetchAll = async () => {
    await Promise.all([fetchMappings(), fetchPOs(), fetchModules(), fetchStats()])
    setLoading(false)
  }

  const fetchMappings = async () => {
    try {
      const res = await axios.get(`${API}/api/lo-po-mapping/all`, auth())
      setMappings(res.data.data || [])
    } catch { setMappings([]) }
  }

  const fetchPOs = async () => {
    try {
      const res = await axios.get(`${API}/api/program-outcomes/all`, auth())
      setPOs(res.data.data || [])
    } catch { setPOs([]) }
  }

  const fetchModules = async () => {
    try {
      const res = await axios.get(`${API}/api/modules/all`, auth())
      setModules(res.data.data || [])
    } catch { setModules([]) }
  }

  const fetchStats = async () => {
    try {
      const res = await axios.get(`${API}/api/lo-po-mapping/statistics`, auth())
      setStats(res.data.data)
    } catch { setStats(null) }
  }

  const fetchLOsForModule = async (moduleId) => {
    if (!moduleId) { setLos([]); return }
    try {
      const res = await axios.get(`${API}/api/lospos/module/${moduleId}`, auth())
      setLos(res.data.data || [])
    } catch { setLos([]) }
  }

  const handleCreateMappings = async (e) => {
    e.preventDefault()
    if (!createForm.loId) { flash('error', 'Select an LO first'); return }
    if (Object.keys(createForm.mappings).length === 0) { flash('error', 'Set at least one PO weight'); return }
    try {
      await axios.post(`${API}/api/lo-po-mapping/create?loId=${createForm.loId}`,
        { mappings: createForm.mappings, remarks: createForm.remarks }, auth())
      flash('success', 'Mappings created and submitted for approval!')
      setShowCreateForm(false); setCreateForm({ loId: '', mappings: {}, remarks: '' })
      fetchMappings(); fetchStats()
    } catch (err) { flash('error', err.response?.data?.message || 'Failed to create mappings') }
  }

  const handleApprove = async (mappingId) => {
    try {
      await axios.put(`${API}/api/lo-po-mapping/admin/${mappingId}/approve`, { adminRemarks: 'Approved' }, auth())
      flash('success', 'Mapping approved!')
      fetchMappings(); fetchStats()
    } catch { flash('error', 'Failed') }
  }

  const handleReject = async (mappingId) => {
    const remarks = prompt('Rejection reason (required):')
    if (!remarks?.trim()) return
    try {
      await axios.put(`${API}/api/lo-po-mapping/admin/${mappingId}/reject`, { adminRemarks: remarks }, auth())
      flash('success', 'Mapping rejected.')
      fetchMappings(); fetchStats()
    } catch { flash('error', 'Failed') }
  }

  const handleDelete = async (mappingId) => {
    if (!confirm('Delete this mapping?')) return
    try {
      await axios.delete(`${API}/api/lo-po-mapping/${mappingId}`, auth())
      flash('success', 'Mapping deleted')
      fetchMappings(); fetchStats()
    } catch { flash('error', 'Cannot delete approved mappings') }
  }

  const setWeight = (poId, w) => {
    setCreateForm(f => {
      const maps = { ...f.mappings }
      if (w === 0) delete maps[poId]
      else maps[poId] = w
      return { ...f, mappings: maps }
    })
  }

  const filtered = mappings.filter(m => {
    if (filterStatus && m.status !== filterStatus) return false
    return true
  })

  return (
    <div className="min-h-screen bg-[#0a0f1e] flex flex-col">
      <Header />
      <main className="flex-1 max-w-6xl mx-auto px-6 py-10 w-full">
        {/* Header */}
        <div className="flex items-start justify-between mb-8 flex-wrap gap-4">
          <div>
            <p className="text-slate-500 text-sm">OBE System</p>
            <h1 className="text-white text-3xl font-black mt-1">LO-PO Mappings</h1>
          </div>
          <button onClick={() => setShowCreateForm(s => !s)}
            className="px-4 py-2 bg-blue-600 text-white text-xs font-bold rounded-xl hover:bg-blue-500 transition-colors">
            {showCreateForm ? 'Cancel' : '+ Create Mappings'}
          </button>
        </div>

        {/* Stats */}
        {stats && (
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
            {[
              { label: 'Total', value: stats.totalMappings, color: 'text-white' },
              { label: 'Pending', value: stats.pendingMappings, color: 'text-amber-400' },
              { label: 'Approved', value: stats.approvedMappings, color: 'text-emerald-400' },
              { label: 'Coverage', value: `${Math.round(stats.coveragePercentage || 0)}%`, color: 'text-blue-400' },
            ].map(({ label, value, color }) => (
              <div key={label} className="bg-white/5 border border-white/10 rounded-xl p-4 text-center">
                <p className={`text-2xl font-black ${color}`}>{value}</p>
                <p className="text-slate-500 text-xs mt-1">{label}</p>
              </div>
            ))}
          </div>
        )}

        {msg && (
          <div className={`mb-6 p-3 rounded-xl text-xs font-medium border ${msg.type === 'success' ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400' : 'bg-red-500/10 border-red-500/30 text-red-400'}`}>
            {msg.text}
          </div>
        )}

        {/* Create Mapping Form */}
        {showCreateForm && (
          <form onSubmit={handleCreateMappings} className="bg-white/5 border border-white/10 rounded-xl p-6 mb-8">
            <h2 className="text-white font-bold text-sm mb-5">Create New LO-PO Mappings</h2>

            {/* Module + LO Select */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-5">
              <div>
                <label className="block text-slate-400 text-xs mb-1">Module</label>
                <select onChange={e => fetchLOsForModule(e.target.value)}
                  className="w-full bg-white/5 border border-white/10 text-white rounded-lg px-3 py-2 text-xs focus:outline-none focus:border-blue-500 transition-colors">
                  <option value="" className="bg-[#0a0f1e]">Select module</option>
                  {modules.map(m => <option key={m.moduleId} value={m.moduleId} className="bg-[#0a0f1e]">{m.moduleId} — {m.moduleName}</option>)}
                </select>
              </div>
              <div>
                <label className="block text-slate-400 text-xs mb-1">Learning Outcome</label>
                <select value={createForm.loId} onChange={e => setCreateForm(f => ({ ...f, loId: e.target.value }))} required
                  className="w-full bg-white/5 border border-white/10 text-white rounded-lg px-3 py-2 text-xs focus:outline-none focus:border-blue-500 transition-colors">
                  <option value="" className="bg-[#0a0f1e]">Select LO</option>
                  {los.map(lo => <option key={lo.id} value={lo.id} className="bg-[#0a0f1e]">{lo.name || lo.id}</option>)}
                </select>
              </div>
            </div>

            {/* PO Weight Grid */}
            {pos.length > 0 && (
              <div className="mb-5">
                <label className="block text-slate-400 text-xs mb-3">Set PO Weights <span className="text-slate-600">(0=None, 1=Low, 2=Medium, 3=High)</span></label>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                  {pos.map(po => (
                    <div key={po.poId} className="flex items-center gap-3 bg-white/3 rounded-lg p-3">
                      <span className="text-purple-400 font-black text-xs min-w-[40px]">{po.code || po.poId}</span>
                      <span className="text-slate-400 text-xs flex-1 truncate">{po.title}</span>
                      <div className="flex gap-1">
                        {[0,1,2,3].map(w => (
                          <button key={w} type="button"
                            onClick={() => setWeight(po.poId, w)}
                            className={`w-7 h-7 rounded-lg text-xs font-bold transition-colors ${createForm.mappings[po.poId] === w || (w === 0 && !createForm.mappings[po.poId]) ? (w === 0 ? 'bg-slate-600 text-white' : 'bg-blue-600 text-white') : 'bg-white/5 text-slate-500 hover:bg-white/10'}`}>
                            {w}
                          </button>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Remarks */}
            <div className="mb-5">
              <label className="block text-slate-400 text-xs mb-1">Remarks (optional)</label>
              <input value={createForm.remarks} onChange={e => setCreateForm(f => ({ ...f, remarks: e.target.value }))} placeholder="Add context for the admin..."
                className="w-full bg-white/5 border border-white/10 text-white placeholder-slate-600 rounded-lg px-3 py-2 text-xs focus:outline-none focus:border-blue-500 transition-colors" />
            </div>

            <div className="flex gap-2">
              <button type="submit" className="px-5 py-2 bg-blue-600 text-white text-xs font-bold rounded-lg hover:bg-blue-500 transition-colors">
                Submit for Approval
              </button>
              <button type="button" onClick={() => setShowCreateForm(false)} className="px-5 py-2 bg-white/5 text-slate-400 text-xs font-bold rounded-lg hover:bg-white/10 transition-colors">
                Cancel
              </button>
            </div>
          </form>
        )}

        {/* Filter */}
        <div className="flex gap-2 mb-6 flex-wrap">
          {[{ value: '', label: 'All' }, { value: 'PENDING', label: 'Pending' }, { value: 'APPROVED', label: 'Approved' }, { value: 'REJECTED', label: 'Rejected' }].map(f => (
            <button key={f.value} onClick={() => setFilterStatus(f.value)}
              className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all ${filterStatus === f.value ? 'bg-blue-600 text-white' : 'bg-white/5 text-slate-400 hover:text-white'}`}>
              {f.label}
            </button>
          ))}
        </div>

        {/* Mappings List */}
        {loading ? (
          <div className="text-center py-16 text-slate-500 text-sm">Loading mappings...</div>
        ) : filtered.length === 0 ? (
          <div className="text-center py-16 bg-white/3 border border-white/8 rounded-xl">
            <p className="text-4xl mb-3">🗺️</p>
            <p className="text-slate-400 text-sm">No mappings found.</p>
          </div>
        ) : (
          <div className="space-y-3">
            {filtered.map(m => (
              <div key={m.id} className="bg-white/5 border border-white/10 rounded-xl p-5 flex items-start justify-between gap-4 hover:border-white/20 transition-all">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-2 flex-wrap">
                    <span className={`px-2 py-0.5 border text-xs rounded-full font-bold ${STATUS_CLS[m.status] || 'text-slate-400 border-slate-500/30'}`}>
                      {m.status}
                    </span>
                    <span className={`px-2 py-0.5 rounded text-xs font-bold ${WEIGHT_COLOR[m.weight] || 'text-slate-400'}`}>
                      {WEIGHT_LABEL[m.weight] || m.weight} weight
                    </span>
                  </div>
                  <p className="text-white text-sm font-medium">
                    <span className="text-blue-400">{m.learningOutcome?.name || m.learningOutcome?.id}</span>
                    <span className="text-slate-500 mx-2">→</span>
                    <span className="text-purple-400">{m.programOutcome?.code || m.programOutcome?.poId}</span>
                    {m.programOutcome?.title && <span className="text-slate-500 text-xs ml-1">({m.programOutcome.title})</span>}
                  </p>
                  {m.lecturerRemarks && <p className="text-slate-500 text-xs mt-1">Remarks: {m.lecturerRemarks}</p>}
                  {m.adminRemarks && <p className="text-slate-500 text-xs mt-0.5">Admin: {m.adminRemarks}</p>}
                </div>
                <div className="flex gap-2 flex-shrink-0">
                  {isAdmin && m.status === 'PENDING' && (
                    <>
                      <button onClick={() => handleApprove(m.id)} className="px-3 py-1.5 bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-bold rounded-lg hover:bg-emerald-500/20 transition-colors">
                        Approve
                      </button>
                      <button onClick={() => handleReject(m.id)} className="px-3 py-1.5 bg-red-500/10 border border-red-500/30 text-red-400 text-xs font-bold rounded-lg hover:bg-red-500/20 transition-colors">
                        Reject
                      </button>
                    </>
                  )}
                  {m.status !== 'APPROVED' && (
                    <button onClick={() => handleDelete(m.id)} className="p-2 bg-white/5 text-slate-500 hover:text-red-400 hover:bg-red-500/10 rounded-lg transition-colors text-xs">
                      🗑
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </main>
      <Footer />
    </div>
  )
}
