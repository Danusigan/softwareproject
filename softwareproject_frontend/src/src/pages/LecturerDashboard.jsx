import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import axios from 'axios'
import Header from '../components/Header'
import Footer from '../components/Footer'
import authService from '../services/authService'

const API = 'http://localhost:8080'
const auth = () => ({ headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } })

export default function LecturerDashboard() {
  const navigate = useNavigate()
  const user = authService.getUserInfo()
  const [modules, setModules] = useState([])
  const [selectedModule, setSelectedModule] = useState(null)
  const [los, setLos] = useState([])
  const [loading, setLoading] = useState(true)
  const [losLoading, setLosLoading] = useState(false)
  const [msg, setMsg] = useState(null)

  // LO form
  const [showLoForm, setShowLoForm] = useState(false)
  const [editingLo, setEditingLo] = useState(null)
  const [loForm, setLoForm] = useState({ id: '', name: '', description: '', batch: '' })

  // Marks upload
  const [showUploadForm, setShowUploadForm] = useState(false)
  const [uploadLoId, setUploadLoId] = useState('')
  const [uploadFile, setUploadFile] = useState(null)
  const [uploadBatch, setUploadBatch] = useState('')
  const [uploading, setUploading] = useState(false)

  useEffect(() => {
    const role = (user?.userType || '').toLowerCase()
    if (!['lecture', 'admin', 'superadmin'].includes(role)) navigate('/', { replace: true })
    fetchModules()
  }, [])

  useEffect(() => {
    if (selectedModule) fetchLos(selectedModule.moduleId)
  }, [selectedModule])

  const flash = (type, text) => { setMsg({ type, text }); setTimeout(() => setMsg(null), 3500) }

  const fetchModules = async () => {
    try {
      const res = await axios.get(`${API}/api/modules/all`, auth())
      setModules(res.data.data || [])
    } catch { setModules([]) } finally { setLoading(false) }
  }

  const fetchLos = async (moduleId) => {
    setLosLoading(true)
    try {
      const res = await axios.get(`${API}/api/lospos/module/${moduleId}`, auth())
      const loList = res.data.data || []
      // Fetch mappings for each LO
      const withMappings = await Promise.all(loList.map(async lo => {
        try {
          const mRes = await axios.get(`${API}/api/lo-po-mapping/lo/${lo.id}`, auth())
          return { ...lo, mappings: mRes.data.data || [] }
        } catch { return { ...lo, mappings: [] } }
      }))
      setLos(withMappings)
    } catch { setLos([]) } finally { setLosLoading(false) }
  }

  const getMappingStatus = (lo) => {
    const maps = lo.mappings || []
    if (!maps.length) return { label: 'Unmapped', cls: 'bg-slate-500/10 border-slate-500/30 text-slate-400' }
    if (maps.every(m => m.status === 'APPROVED')) return { label: 'Approved', cls: 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400' }
    if (maps.some(m => m.status === 'REJECTED')) return { label: 'Rejected', cls: 'bg-red-500/10 border-red-500/30 text-red-400' }
    if (maps.some(m => m.status === 'PENDING')) return { label: 'Pending', cls: 'bg-amber-500/10 border-amber-500/30 text-amber-400' }
    return { label: 'Mixed', cls: 'bg-blue-500/10 border-blue-500/30 text-blue-400' }
  }

  const handleLoSubmit = async (e) => {
    e.preventDefault()
    try {
      if (editingLo) {
        await axios.put(`${API}/api/lospos/${editingLo.id}`, loForm, auth())
        flash('success', 'LO updated!')
      } else {
        await axios.post(`${API}/api/lospos/${selectedModule.moduleId}/add`, loForm, auth())
        flash('success', 'Learning Outcome created!')
      }
      setLoForm({ id: '', name: '', description: '', batch: '' })
      setShowLoForm(false); setEditingLo(null)
      fetchLos(selectedModule.moduleId)
    } catch (err) { flash('error', err.response?.data?.message || 'Failed') }
  }

  const handleDeleteLo = async (loId) => {
    if (!confirm('Delete this Learning Outcome?')) return
    try {
      await axios.delete(`${API}/api/lospos/${loId}`, auth())
      flash('success', 'LO deleted')
      fetchLos(selectedModule.moduleId)
    } catch { flash('error', 'Failed to delete') }
  }

  const handleUploadMarks = async (e) => {
    e.preventDefault()
    if (!uploadFile || !uploadBatch || !uploadLoId) return
    setUploading(true)
    const fd = new FormData()
    fd.append('excelFile', uploadFile)
    fd.append('batch', uploadBatch)
    try {
      await axios.post(`${API}/api/lospos/${uploadLoId}/marks/import-obe`, fd,
        { headers: { Authorization: `Bearer ${localStorage.getItem('token')}`, 'Content-Type': 'multipart/form-data' } })
      flash('success', `Marks uploaded for batch ${uploadBatch}!`)
      setShowUploadForm(false); setUploadFile(null); setUploadBatch(''); setUploadLoId('')
      fetchLos(selectedModule.moduleId)
    } catch (err) { flash('error', err.response?.data?.message || 'Upload failed') } finally { setUploading(false) }
  }

  return (
    <div className="min-h-screen bg-[#0a0f1e] flex flex-col">
      <Header />
      <main className="flex-1 max-w-7xl mx-auto px-6 py-10 w-full">
        <div className="mb-8">
          <p className="text-slate-500 text-sm">Lecturer Dashboard</p>
          <h1 className="text-white text-3xl font-black mt-1">Hello, {user?.username} 👋</h1>
        </div>

        {msg && (
          <div className={`mb-6 p-3 rounded-xl text-xs font-medium border ${msg.type === 'success' ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400' : 'bg-red-500/10 border-red-500/30 text-red-400'}`}>
            {msg.text}
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Module List */}
          <div className="lg:col-span-1">
            <div className="bg-white/5 border border-white/10 rounded-xl overflow-hidden">
              <div className="p-4 border-b border-white/10">
                <h2 className="text-white font-bold text-sm">Modules</h2>
                <p className="text-slate-500 text-xs mt-0.5">Select a module to manage LOs</p>
              </div>
              {loading ? (
                <div className="p-6 text-center text-slate-500 text-xs">Loading...</div>
              ) : modules.length === 0 ? (
                <div className="p-6 text-center text-slate-500 text-xs">No modules assigned yet</div>
              ) : (
                <div className="divide-y divide-white/5">
                  {modules.map(m => (
                    <button key={m.moduleId} onClick={() => setSelectedModule(m)}
                      className={`w-full text-left p-4 transition-colors ${selectedModule?.moduleId === m.moduleId ? 'bg-blue-600/20 border-l-2 border-blue-500' : 'hover:bg-white/5'}`}>
                      <p className="text-white text-xs font-bold">{m.moduleId}</p>
                      <p className="text-slate-400 text-xs mt-0.5 truncate">{m.moduleName}</p>
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* LO Management */}
          <div className="lg:col-span-2">
            {!selectedModule ? (
              <div className="h-full flex items-center justify-center bg-white/3 border border-white/8 rounded-xl">
                <div className="text-center py-16">
                  <p className="text-4xl mb-3">📚</p>
                  <p className="text-slate-400 text-sm">Select a module from the left to manage Learning Outcomes</p>
                </div>
              </div>
            ) : (
              <div className="bg-white/5 border border-white/10 rounded-xl overflow-hidden">
                {/* Module Header */}
                <div className="p-5 border-b border-white/10 flex items-start justify-between">
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="px-2 py-0.5 bg-blue-500/20 text-blue-400 text-xs font-black rounded">{selectedModule.moduleId}</span>
                    </div>
                    <h2 className="text-white font-bold mt-1">{selectedModule.moduleName}</h2>
                  </div>
                  <div className="flex gap-2">
                    <button onClick={() => { setShowUploadForm(s => !s); setShowLoForm(false) }}
                      className="px-3 py-1.5 bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 text-xs font-bold rounded-lg hover:bg-emerald-500/20 transition-colors">
                      📁 Upload Marks
                    </button>
                    <button onClick={() => { setShowLoForm(s => !s); setEditingLo(null); setLoForm({ id: '', name: '', description: '', batch: '' }); setShowUploadForm(false) }}
                      className="px-3 py-1.5 bg-blue-600 text-white text-xs font-bold rounded-lg hover:bg-blue-500 transition-colors">
                      + Add LO
                    </button>
                  </div>
                </div>

                {/* Upload Marks Form */}
                {showUploadForm && (
                  <form onSubmit={handleUploadMarks} className="p-5 border-b border-white/10 bg-emerald-500/5">
                    <h3 className="text-white font-bold text-sm mb-4">Upload Student Marks (Excel)</h3>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-3 mb-4">
                      <div>
                        <label className="block text-slate-400 text-xs mb-1">Learning Outcome</label>
                        <select value={uploadLoId} onChange={e => setUploadLoId(e.target.value)} required
                          className="w-full bg-white/5 border border-white/10 text-white rounded-lg px-3 py-2 text-xs focus:outline-none focus:border-emerald-500 transition-colors">
                          <option value="" className="bg-[#0a0f1e]">Select LO</option>
                          {los.map(lo => <option key={lo.id} value={lo.id} className="bg-[#0a0f1e]">{lo.name || lo.id}</option>)}
                        </select>
                      </div>
                      <div>
                        <label className="block text-slate-400 text-xs mb-1">Batch Year (e.g. 22)</label>
                        <input value={uploadBatch} onChange={e => setUploadBatch(e.target.value)} placeholder="22" required
                          className="w-full bg-white/5 border border-white/10 text-white placeholder-slate-600 rounded-lg px-3 py-2 text-xs focus:outline-none focus:border-emerald-500 transition-colors" />
                      </div>
                      <div>
                        <label className="block text-slate-400 text-xs mb-1">Excel File (.xlsx)</label>
                        <input type="file" accept=".xlsx,.xls" onChange={e => setUploadFile(e.target.files[0])} required
                          className="w-full bg-white/5 border border-white/10 text-slate-400 rounded-lg px-3 py-1.5 text-xs focus:outline-none" />
                      </div>
                    </div>
                    <div className="flex gap-2">
                      <button type="submit" disabled={uploading}
                        className="px-4 py-2 bg-emerald-600 text-white text-xs font-bold rounded-lg hover:bg-emerald-500 disabled:opacity-50 transition-colors">
                        {uploading ? 'Uploading...' : 'Upload Marks'}
                      </button>
                      <button type="button" onClick={() => setShowUploadForm(false)}
                        className="px-4 py-2 bg-white/5 text-slate-400 text-xs font-bold rounded-lg hover:bg-white/10 transition-colors">
                        Cancel
                      </button>
                    </div>
                  </form>
                )}

                {/* Add/Edit LO Form */}
                {showLoForm && (
                  <form onSubmit={handleLoSubmit} className="p-5 border-b border-white/10 bg-blue-500/5">
                    <h3 className="text-white font-bold text-sm mb-4">{editingLo ? 'Edit Learning Outcome' : 'Add Learning Outcome'}</h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-3 mb-4">
                      {!editingLo && (
                        <div>
                          <label className="block text-slate-400 text-xs mb-1">LO ID (e.g. LO1)</label>
                          <input value={loForm.id} onChange={e => setLoForm(f => ({ ...f, id: e.target.value }))} placeholder="LO1" required
                            className="w-full bg-white/5 border border-white/10 text-white placeholder-slate-600 rounded-lg px-3 py-2 text-xs focus:outline-none focus:border-blue-500 transition-colors" />
                        </div>
                      )}
                      <div>
                        <label className="block text-slate-400 text-xs mb-1">LO Name</label>
                        <input value={loForm.name} onChange={e => setLoForm(f => ({ ...f, name: e.target.value }))} placeholder="e.g. Apply circuit analysis" required
                          className="w-full bg-white/5 border border-white/10 text-white placeholder-slate-600 rounded-lg px-3 py-2 text-xs focus:outline-none focus:border-blue-500 transition-colors" />
                      </div>
                      <div>
                        <label className="block text-slate-400 text-xs mb-1">Batch Year</label>
                        <input value={loForm.batch} onChange={e => setLoForm(f => ({ ...f, batch: e.target.value }))} placeholder="e.g. 22"
                          className="w-full bg-white/5 border border-white/10 text-white placeholder-slate-600 rounded-lg px-3 py-2 text-xs focus:outline-none focus:border-blue-500 transition-colors" />
                      </div>
                      <div className="md:col-span-2">
                        <label className="block text-slate-400 text-xs mb-1">Description</label>
                        <textarea value={loForm.description} onChange={e => setLoForm(f => ({ ...f, description: e.target.value }))} placeholder="Describe this learning outcome..."
                          rows={2}
                          className="w-full bg-white/5 border border-white/10 text-white placeholder-slate-600 rounded-lg px-3 py-2 text-xs focus:outline-none focus:border-blue-500 transition-colors resize-none" />
                      </div>
                    </div>
                    <div className="flex gap-2">
                      <button type="submit" className="px-4 py-2 bg-blue-600 text-white text-xs font-bold rounded-lg hover:bg-blue-500 transition-colors">
                        {editingLo ? 'Update LO' : 'Create LO'}
                      </button>
                      <button type="button" onClick={() => { setShowLoForm(false); setEditingLo(null) }}
                        className="px-4 py-2 bg-white/5 text-slate-400 text-xs font-bold rounded-lg hover:bg-white/10 transition-colors">
                        Cancel
                      </button>
                    </div>
                  </form>
                )}

                {/* LO List */}
                {losLoading ? (
                  <div className="p-8 text-center text-slate-500 text-sm">Loading learning outcomes...</div>
                ) : los.length === 0 ? (
                  <div className="p-8 text-center">
                    <p className="text-slate-500 text-sm">No learning outcomes yet. Add your first LO.</p>
                  </div>
                ) : (
                  <div className="divide-y divide-white/5">
                    {los.map(lo => {
                      const status = getMappingStatus(lo)
                      return (
                        <div key={lo.id} className="p-5 flex items-start justify-between gap-4 hover:bg-white/3 transition-colors">
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center gap-2 mb-1 flex-wrap">
                              <span className="text-blue-400 font-bold text-xs">{lo.id}</span>
                              <span className={`px-2 py-0.5 border text-xs rounded-full font-medium ${status.cls}`}>{status.label}</span>
                              {lo.batch && <span className="text-slate-600 text-xs">Batch {lo.batch}</span>}
                            </div>
                            <p className="text-white text-sm font-medium">{lo.name}</p>
                            {lo.description && <p className="text-slate-500 text-xs mt-0.5 line-clamp-1">{lo.description}</p>}
                            <p className="text-slate-600 text-xs mt-1">{(lo.mappings || []).length} PO mapping(s)</p>
                          </div>
                          <div className="flex gap-1.5 flex-shrink-0">
                            <button onClick={() => navigate(`/lo/${lo.id}`)}
                              className="px-2.5 py-1.5 bg-white/5 text-slate-400 hover:text-blue-400 hover:bg-blue-500/10 text-xs rounded-lg transition-colors font-medium">
                              View
                            </button>
                            <button onClick={() => { setEditingLo(lo); setLoForm({ id: lo.id, name: lo.name || '', description: lo.description || '', batch: lo.batch || '' }); setShowLoForm(true); setShowUploadForm(false) }}
                              className="px-2.5 py-1.5 bg-white/5 text-slate-400 hover:text-white hover:bg-white/10 text-xs rounded-lg transition-colors">
                              ✏️
                            </button>
                            <button onClick={() => handleDeleteLo(lo.id)}
                              className="px-2.5 py-1.5 bg-white/5 text-slate-400 hover:text-red-400 hover:bg-red-500/10 text-xs rounded-lg transition-colors">
                              🗑
                            </button>
                          </div>
                        </div>
                      )
                    })}
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      </main>
      <Footer />
    </div>
  )
}
