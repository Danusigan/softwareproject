import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import axios from 'axios'
import Header from '../components/Header'
import Footer from '../components/Footer'

const API = 'http://localhost:8080'
const auth = () => ({ headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } })

export default function LODetailPage() {
  const { loId } = useParams()
  const navigate = useNavigate()
  const [lo, setLo] = useState(null)
  const [batches, setBatches] = useState([])
  const [selectedBatch, setSelectedBatch] = useState(null)
  const [marks, setMarks] = useState([])
  const [mappings, setMappings] = useState([])
  const [loading, setLoading] = useState(true)
  const [marksLoading, setMarksLoading] = useState(false)
  const [tab, setTab] = useState('marks')
  const [msg, setMsg] = useState(null)
  const [editingMark, setEditingMark] = useState(null)
  const [newScore, setNewScore] = useState('')

  const flash = (type, text) => { setMsg({ type, text }); setTimeout(() => setMsg(null), 3000) }

  useEffect(() => {
    fetchLO()
    fetchBatches()
    fetchMappings()
  }, [loId])

  useEffect(() => {
    if (selectedBatch) fetchMarksByBatch(selectedBatch)
    else fetchAllMarks()
  }, [selectedBatch])

  const fetchLO = async () => {
    try {
      const res = await axios.get(`${API}/api/lospos/${loId}`, auth())
      setLo(res.data.data || res.data)
    } catch { } finally { setLoading(false) }
  }

  const fetchBatches = async () => {
    try {
      const res = await axios.get(`${API}/api/lospos/${loId}/batches`, auth())
      setBatches(res.data.data || [])
    } catch { setBatches([]) }
  }

  const fetchAllMarks = async () => {
    setMarksLoading(true)
    try {
      const res = await axios.get(`${API}/api/lospos/${loId}/marks`, auth())
      setMarks(res.data.data || [])
    } catch { setMarks([]) } finally { setMarksLoading(false) }
  }

  const fetchMarksByBatch = async (batch) => {
    setMarksLoading(true)
    try {
      const res = await axios.get(`${API}/api/lospos/${loId}/batches/${batch}/marks`, auth())
      setMarks(res.data.data || [])
    } catch { setMarks([]) } finally { setMarksLoading(false) }
  }

  const fetchMappings = async () => {
    try {
      const res = await axios.get(`${API}/api/lo-po-mapping/lo/${loId}`, auth())
      setMappings(res.data.data || [])
    } catch { setMappings([]) }
  }

  const handleUpdateMark = async (markId) => {
    try {
      await axios.put(`${API}/api/lospos/${loId}/marks/${markId}`, { score: parseFloat(newScore) }, auth())
      flash('success', 'Mark updated!')
      setEditingMark(null); setNewScore('')
      if (selectedBatch) fetchMarksByBatch(selectedBatch); else fetchAllMarks()
    } catch { flash('error', 'Failed to update mark') }
  }

  const handleDeleteMark = async (markId) => {
    if (!confirm('Delete this mark?')) return
    try {
      await axios.delete(`${API}/api/lospos/${loId}/marks/${markId}`, auth())
      flash('success', 'Mark deleted')
      if (selectedBatch) fetchMarksByBatch(selectedBatch); else fetchAllMarks()
    } catch { flash('error', 'Failed to delete') }
  }

  const handleDeleteBatch = async (batch) => {
    if (!confirm(`Delete all marks for batch ${batch}?`)) return
    try {
      await axios.delete(`${API}/api/lospos/${loId}/batches/${batch}`, auth())
      flash('success', `Batch ${batch} deleted`)
      setSelectedBatch(null)
      fetchBatches()
      fetchAllMarks()
    } catch { flash('error', 'Failed to delete batch') }
  }

  const avgScore = marks.length > 0 ? (marks.reduce((s, m) => s + (m.score || 0), 0) / marks.length).toFixed(1) : '—'
  const passCount = marks.filter(m => (m.score || 0) >= 50).length
  const passRate = marks.length > 0 ? Math.round((passCount / marks.length) * 100) : 0

  const WEIGHT_LABEL = { 0: 'None', 1: 'Low', 2: 'Medium', 3: 'High' }
  const STATUS_CLS = {
    APPROVED: 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400',
    PENDING: 'bg-amber-500/10 border-amber-500/30 text-amber-400',
    REJECTED: 'bg-red-500/10 border-red-500/30 text-red-400',
  }

  if (loading) return (
    <div className="min-h-screen bg-[#0a0f1e] flex flex-col">
      <Header />
      <div className="flex-1 flex items-center justify-center text-slate-500">Loading...</div>
    </div>
  )

  return (
    <div className="min-h-screen bg-[#0a0f1e] flex flex-col">
      <Header />
      <main className="flex-1 max-w-6xl mx-auto px-6 py-10 w-full">
        {/* Back */}
        <button onClick={() => navigate(-1)} className="text-slate-500 hover:text-white text-sm mb-6 flex items-center gap-1 transition-colors">
          ← Back
        </button>

        {/* LO Header */}
        <div className="bg-white/5 border border-white/10 rounded-xl p-6 mb-6">
          <div className="flex items-start justify-between flex-wrap gap-4">
            <div>
              <div className="flex items-center gap-2 mb-2">
                <span className="px-2.5 py-1 bg-blue-500/20 text-blue-400 text-xs font-black rounded-lg">{lo?.id}</span>
              </div>
              <h1 className="text-white text-2xl font-black">{lo?.name || lo?.id}</h1>
              {lo?.description && <p className="text-slate-400 text-sm mt-2 max-w-2xl">{lo.description}</p>}
              <div className="flex items-center gap-4 mt-3 text-xs text-slate-500">
                {lo?.batch && <span>Batch: {lo.batch}</span>}
                {lo?.createdBy && <span>By: {lo.createdBy}</span>}
              </div>
            </div>
            {/* Quick Stats */}
            <div className="grid grid-cols-3 gap-3">
              {[
                { label: 'Students', value: marks.length || '—' },
                { label: 'Avg Score', value: avgScore },
                { label: 'Pass Rate', value: marks.length ? `${passRate}%` : '—' },
              ].map(({ label, value }) => (
                <div key={label} className="bg-white/5 rounded-lg p-3 text-center min-w-[72px]">
                  <p className="text-white text-lg font-black">{value}</p>
                  <p className="text-slate-500 text-xs">{label}</p>
                </div>
              ))}
            </div>
          </div>
        </div>

        {msg && (
          <div className={`mb-5 p-3 rounded-xl text-xs font-medium border ${msg.type === 'success' ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400' : 'bg-red-500/10 border-red-500/30 text-red-400'}`}>
            {msg.text}
          </div>
        )}

        {/* Tabs */}
        <div className="flex gap-1 mb-6 p-1 bg-white/5 rounded-xl w-fit">
          {[{ id: 'marks', label: `Marks (${marks.length})` }, { id: 'mappings', label: `PO Mappings (${mappings.length})` }].map(t => (
            <button key={t.id} onClick={() => setTab(t.id)}
              className={`px-4 py-2 rounded-lg text-xs font-bold transition-all ${tab === t.id ? 'bg-blue-600 text-white' : 'text-slate-400 hover:text-white'}`}>
              {t.label}
            </button>
          ))}
        </div>

        {/* Marks Tab */}
        {tab === 'marks' && (
          <div>
            {/* Batch Selector */}
            {batches.length > 0 && (
              <div className="flex gap-2 mb-5 flex-wrap">
                <button onClick={() => setSelectedBatch(null)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all ${!selectedBatch ? 'bg-blue-600 text-white' : 'bg-white/5 text-slate-400 hover:text-white'}`}>
                  All Batches
                </button>
                {batches.map(b => (
                  <div key={b.batch} className="flex items-center gap-1">
                    <button onClick={() => setSelectedBatch(b.batch)}
                      className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all ${selectedBatch === b.batch ? 'bg-blue-600 text-white' : 'bg-white/5 text-slate-400 hover:text-white'}`}>
                      Batch {b.batch} ({b.recordCount})
                    </button>
                    {selectedBatch === b.batch && (
                      <button onClick={() => handleDeleteBatch(b.batch)}
                        className="p-1.5 bg-red-500/10 text-red-400 text-xs rounded-lg hover:bg-red-500/20 transition-colors">🗑</button>
                    )}
                  </div>
                ))}
              </div>
            )}

            {/* Marks Table */}
            {marksLoading ? (
              <div className="text-center py-16 text-slate-500 text-sm">Loading marks...</div>
            ) : marks.length === 0 ? (
              <div className="text-center py-16 bg-white/3 border border-white/8 rounded-xl">
                <p className="text-4xl mb-3">📋</p>
                <p className="text-slate-400 text-sm">No marks uploaded yet. Use the Upload Marks feature on the dashboard.</p>
              </div>
            ) : (
              <div className="bg-white/5 border border-white/10 rounded-xl overflow-hidden">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-white/10">
                      <th className="text-left px-5 py-3 text-slate-400 text-xs font-semibold">Student ID</th>
                      <th className="text-left px-5 py-3 text-slate-400 text-xs font-semibold">Name</th>
                      <th className="text-right px-5 py-3 text-slate-400 text-xs font-semibold">Score</th>
                      <th className="text-center px-5 py-3 text-slate-400 text-xs font-semibold">Status</th>
                      <th className="px-5 py-3" />
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/5">
                    {marks.map(m => (
                      <tr key={m.id} className="hover:bg-white/3 transition-colors">
                        <td className="px-5 py-3 text-blue-400 font-mono text-xs">{m.studentId}</td>
                        <td className="px-5 py-3 text-white text-xs">{m.studentName || '—'}</td>
                        <td className="px-5 py-3 text-right">
                          {editingMark === m.id ? (
                            <div className="flex items-center justify-end gap-2">
                              <input type="number" min="0" max="100" value={newScore} onChange={e => setNewScore(e.target.value)}
                                className="w-16 bg-white/10 border border-white/20 text-white text-xs rounded-lg px-2 py-1 text-right focus:outline-none focus:border-blue-500" />
                              <button onClick={() => handleUpdateMark(m.id)} className="text-emerald-400 text-xs hover:text-emerald-300">✓</button>
                              <button onClick={() => { setEditingMark(null); setNewScore('') }} className="text-slate-500 text-xs hover:text-white">✕</button>
                            </div>
                          ) : (
                            <span className={`font-bold text-sm ${(m.score || 0) >= 50 ? 'text-emerald-400' : 'text-red-400'}`}>
                              {m.score?.toFixed(1) ?? '—'}
                            </span>
                          )}
                        </td>
                        <td className="px-5 py-3 text-center">
                          <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${(m.score || 0) >= 50 ? 'bg-emerald-500/10 text-emerald-400' : 'bg-red-500/10 text-red-400'}`}>
                            {(m.score || 0) >= 50 ? 'Pass' : 'Fail'}
                          </span>
                        </td>
                        <td className="px-5 py-3">
                          <div className="flex items-center justify-end gap-1 opacity-0 group-hover:opacity-100">
                            <button onClick={() => { setEditingMark(m.id); setNewScore(m.score?.toString() || '') }}
                              className="p-1 text-slate-500 hover:text-blue-400 text-xs transition-colors">✏️</button>
                            <button onClick={() => handleDeleteMark(m.id)}
                              className="p-1 text-slate-500 hover:text-red-400 text-xs transition-colors">🗑</button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {/* Mappings Tab */}
        {tab === 'mappings' && (
          <div>
            {mappings.length === 0 ? (
              <div className="text-center py-16 bg-white/3 border border-white/8 rounded-xl">
                <p className="text-4xl mb-3">🗺️</p>
                <p className="text-slate-400 text-sm">No PO mappings yet.</p>
                <button onClick={() => navigate('/lo-po-mappings')} className="mt-4 px-5 py-2 bg-blue-600 text-white text-xs font-bold rounded-xl hover:bg-blue-500 transition-colors">
                  Manage Mappings
                </button>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {mappings.map(m => (
                  <div key={m.id} className="bg-white/5 border border-white/10 rounded-xl p-5">
                    <div className="flex items-start justify-between mb-3">
                      <div>
                        <p className="text-purple-400 font-black text-sm">{m.programOutcome?.code || m.programOutcome?.poId}</p>
                        <p className="text-white text-xs mt-0.5">{m.programOutcome?.title}</p>
                      </div>
                      <span className={`px-2 py-0.5 border text-xs rounded-full font-medium ${STATUS_CLS[m.status] || 'text-slate-400'}`}>
                        {m.status}
                      </span>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="text-slate-500 text-xs">Weight:</span>
                      <div className="flex gap-1">
                        {[1,2,3].map(w => (
                          <div key={w} className={`w-3 h-3 rounded-full ${w <= m.weight ? 'bg-blue-500' : 'bg-white/10'}`} />
                        ))}
                      </div>
                      <span className="text-slate-400 text-xs">{WEIGHT_LABEL[m.weight]}</span>
                    </div>
                    {m.lecturerRemarks && <p className="text-slate-500 text-xs mt-2">Remark: {m.lecturerRemarks}</p>}
                    {m.adminRemarks && <p className="text-slate-500 text-xs mt-1">Admin: {m.adminRemarks}</p>}
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
