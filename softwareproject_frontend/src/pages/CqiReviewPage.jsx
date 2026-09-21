import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Header from '../components/header'
import Footer from '../components/footer'
import authService from '../services/authService'
import cqiService from '../services/cqiService'

const actionTypeLabels = {
  ADD_LAB_SESSION: 'Add Lab Session',
  REVISE_ASSESSMENT: 'Revise Assessment',
  CHANGE_TEACHING_METHOD: 'Change Teaching Method',
  ADD_RESOURCE: 'Add Resource',
  REDESIGN_LO: 'Redesign LO',
}

export default function CqiReviewPage() {
  const navigate = useNavigate()
  const [pending, setPending] = useState([])
  const [loading, setLoading] = useState(true)
  const [busyAction, setBusyAction] = useState('')
  const [message, setMessage] = useState({ type: '', text: '' })
  const [returnComments, setReturnComments] = useState({}) // { [actionId]: comment }
  const [openReturnFor, setOpenReturnFor] = useState(null)

  const authHeaders = () => { const t = authService.getToken(); return t ? { Authorization: `Bearer ${t}` } : undefined }

  const loadPending = async () => {
    try {
      setLoading(true)
      const r = await cqiService.getPending({ headers: authHeaders() })
      setPending(r.data?.data || [])
    } catch (e) {
      setMessage({ type: 'error', text: e.response?.data?.message || 'Failed to load pending CQI plans.' })
    } finally { setLoading(false) }
  }

  useEffect(() => { loadPending() }, [])

  const handleApprove = async id => {
    try {
      setBusyAction(`approve-${id}`); setMessage({ type: '', text: '' })
      await cqiService.approvePlan(id, { headers: authHeaders() })
      setMessage({ type: 'success', text: 'CQI plan approved.' })
      await loadPending()
    } catch (e) { setMessage({ type: 'error', text: e.response?.data?.message || 'Failed to approve plan.' }) }
    finally { setBusyAction('') }
  }

  const handleReturn = async id => {
    const comment = (returnComments[id] || '').trim()
    if (!comment) { setMessage({ type: 'error', text: 'Add a comment explaining why this plan is being returned.' }); return }
    try {
      setBusyAction(`return-${id}`); setMessage({ type: '', text: '' })
      await cqiService.returnPlan(id, comment, { headers: authHeaders() })
      setMessage({ type: 'success', text: 'CQI plan returned to the lecturer for revision.' })
      setReturnComments(prev => { const next = { ...prev }; delete next[id]; return next })
      setOpenReturnFor(null)
      await loadPending()
    } catch (e) { setMessage({ type: 'error', text: e.response?.data?.message || 'Failed to return plan.' }) }
    finally { setBusyAction('') }
  }

  return (
    <div className="min-h-screen bg-[#f8fafc] flex flex-col relative overflow-hidden">
      <Header />
      <div className="absolute top-[-10%] right-[-10%] w-[40%] h-[40%] bg-indigo-500/10 rounded-full blur-[120px] pointer-events-none" />

      <main className="flex-1 max-w-6xl mx-auto px-6 py-12 w-full relative z-10 animate-in fade-in duration-700">
        <div className="mb-10 space-y-3">
          <button type="button" onClick={() => navigate(-1)}
            className="group inline-flex items-center text-slate-500 hover:text-indigo-600 font-bold transition-all">
            <div className="p-2 bg-white rounded-xl shadow-sm border border-slate-100 mr-3 group-hover:bg-indigo-50 transition-colors">
              <svg className="w-5 h-5 group-hover:-translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
              </svg>
            </div>
            Back
          </button>
          <span className="px-4 py-1.5 bg-indigo-50 text-indigo-600 rounded-full text-[10px] font-black tracking-widest uppercase inline-block">Continuous Quality Improvement</span>
          <h1 className="heading-xl bg-clip-text text-transparent bg-gradient-to-r from-slate-900 to-slate-600">CQI Review Queue</h1>
        </div>

        {message.text && (
          <div className={`mb-8 flex items-center gap-3 p-4 rounded-2xl text-sm font-bold ${message.type === 'success' ? 'bg-emerald-50 text-emerald-700' : 'bg-red-50 text-red-700'}`}>
            {message.text}
          </div>
        )}

        {loading ? (
          <div className="glass-card rounded-[2.5rem] p-16 text-center">
            <div className="relative mx-auto w-16 h-16 mb-6">
              <div className="w-16 h-16 border-4 border-indigo-100 rounded-full" />
              <div className="w-16 h-16 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin absolute inset-0" />
            </div>
            <p className="text-slate-500 font-bold">Loading…</p>
          </div>
        ) : pending.length === 0 ? (
          <div className="glass-card rounded-[2.5rem] p-16 text-center">
            <h2 className="heading-lg mb-2">No plans awaiting review</h2>
            <p className="text-slate-500">Submitted CQI action plans will show up here.</p>
          </div>
        ) : (
          <div className="space-y-6">
            {pending.map(action => {
              const approveKey = `approve-${action.id}`
              const returnKey = `return-${action.id}`
              return (
                <section key={action.id} className="glass-card rounded-[2.5rem] p-7 border-slate-100 space-y-5">
                  <div className="flex flex-wrap items-start justify-between gap-4">
                    <div>
                      <span className="text-[10px] font-black text-indigo-600 uppercase tracking-[0.2em] mb-1 block">{action.moduleName || action.moduleId} · Batch {action.batch}</span>
                      <h2 className="heading-lg">{action.losId} — {action.losName || 'Learning Outcome'}</h2>
                    </div>
                    <div className="flex gap-3">
                      <span className="px-3 py-1.5 rounded-xl bg-red-50 text-red-700 text-xs font-bold">Achieved: {Number(action.attainmentScore).toFixed(1)}%</span>
                      <span className="px-3 py-1.5 rounded-xl bg-amber-50 text-amber-700 text-xs font-bold">Threshold: {Number(action.targetScore).toFixed(1)}%</span>
                      <span className="px-3 py-1.5 rounded-xl bg-emerald-50 text-emerald-700 text-xs font-bold">Target: {action.targetAttainment != null ? `${Number(action.targetAttainment).toFixed(1)}%` : '—'}</span>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="rounded-2xl border border-slate-200 bg-white/70 p-4">
                      <div className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2">Root Cause</div>
                      <p className="text-sm text-slate-700">{action.rootCause || '—'}</p>
                    </div>
                    <div className="rounded-2xl border border-slate-200 bg-white/70 p-4">
                      <div className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2">Action Plan</div>
                      <p className="text-sm text-slate-700">{action.actionPlan || '—'}</p>
                    </div>
                  </div>

                  <div className="flex flex-wrap gap-3 text-xs">
                    <span className="px-3 py-1.5 rounded-lg bg-indigo-50 text-indigo-700 font-bold">Action type: {actionTypeLabels[action.actionType] || action.actionType || '—'}</span>
                    <span className="px-3 py-1.5 rounded-lg bg-slate-100 text-slate-600 font-bold">Deadline: {action.deadline || '—'}</span>
                    <span className="px-3 py-1.5 rounded-lg bg-slate-100 text-slate-600 font-bold">Submitted by: {action.createdBy || '—'}</span>
                  </div>

                  <div className="flex flex-wrap items-center gap-3 pt-3 border-t border-slate-100">
                    <button type="button" onClick={() => handleApprove(action.id)} disabled={busyAction === approveKey}
                      className={`px-6 py-3 rounded-2xl font-bold transition-all flex items-center gap-3 ${busyAction === approveKey ? 'bg-slate-200 text-slate-400 cursor-not-allowed' : 'bg-emerald-600 text-white hover:bg-emerald-700 shadow-lg shadow-emerald-600/20'}`}>
                      {busyAction === approveKey && <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />}
                      Approve
                    </button>
                    <button type="button" onClick={() => setOpenReturnFor(v => v === action.id ? null : action.id)}
                      className="px-6 py-3 rounded-2xl font-bold border border-slate-200 text-slate-700 hover:border-red-200 hover:text-red-600 transition-colors">
                      Return for Revision
                    </button>
                  </div>

                  {openReturnFor === action.id && (
                    <div className="space-y-3 pt-2">
                      <textarea value={returnComments[action.id] || ''} onChange={e => setReturnComments(prev => ({ ...prev, [action.id]: e.target.value }))}
                        rows={3} className="input-field bg-white" placeholder="Explain what needs to change before this plan can be approved…" />
                      <button type="button" onClick={() => handleReturn(action.id)} disabled={busyAction === returnKey}
                        className={`px-6 py-3 rounded-2xl font-bold transition-all flex items-center gap-3 ${busyAction === returnKey ? 'bg-slate-200 text-slate-400 cursor-not-allowed' : 'bg-red-600 text-white hover:bg-red-700 shadow-lg shadow-red-600/20'}`}>
                        {busyAction === returnKey && <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />}
                        Confirm Return
                      </button>
                    </div>
                  )}
                </section>
              )
            })}
          </div>
        )}
      </main>
      <Footer />
    </div>
  )
}
