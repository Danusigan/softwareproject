import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Header from '../components/header'
import Footer from '../components/footer'
import authService from '../services/authService'
import cqiService from '../services/cqiService'

const statusStyles = {
  COMPLETED: 'bg-emerald-50 text-emerald-600',
  IN_PROGRESS: 'bg-indigo-50 text-indigo-600',
  PLANNED: 'bg-amber-50 text-amber-600',
}

export default function MyCqiPlansPage() {
  const navigate = useNavigate()
  const [plans, setPlans] = useState([])
  const [loading, setLoading] = useState(true)
  const [message, setMessage] = useState({ type: '', text: '' })

  const authHeaders = () => { const t = authService.getToken(); return t ? { Authorization: `Bearer ${t}` } : undefined }

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true)
        const r = await cqiService.getMyPlans({ headers: authHeaders() })
        setPlans(r.data?.data || [])
      } catch (e) {
        setMessage({ type: 'error', text: e.response?.data?.message || 'Failed to load your CQI plans.' })
      } finally { setLoading(false) }
    }
    load()
  }, [])

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
          <h1 className="heading-xl bg-clip-text text-transparent bg-gradient-to-r from-slate-900 to-slate-600">My CQI Plans</h1>
        </div>

        {message.text && (
          <div className="mb-8 flex items-center gap-3 p-4 rounded-2xl text-sm font-bold bg-red-50 text-red-700">{message.text}</div>
        )}

        {loading ? (
          <div className="glass-card rounded-[2.5rem] p-16 text-center">
            <div className="relative mx-auto w-16 h-16 mb-6">
              <div className="w-16 h-16 border-4 border-indigo-100 rounded-full" />
              <div className="w-16 h-16 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin absolute inset-0" />
            </div>
            <p className="text-slate-500 font-bold">Loading…</p>
          </div>
        ) : plans.length === 0 ? (
          <div className="glass-card rounded-[2.5rem] p-16 text-center">
            <h2 className="heading-lg mb-2">No CQI actions yet</h2>
            <p className="text-slate-500">When one of your LOs falls below its threshold, it'll show up here.</p>
          </div>
        ) : (
          <div className="overflow-x-auto rounded-2xl border border-slate-200 glass-card">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-slate-800 text-white">
                  <th className="px-4 py-3 text-left font-bold text-xs uppercase tracking-widest">Module</th>
                  <th className="px-4 py-3 text-left font-bold text-xs uppercase tracking-widest">LO</th>
                  <th className="px-4 py-3 text-center font-bold text-xs uppercase tracking-widest">Batch</th>
                  <th className="px-4 py-3 text-center font-bold text-xs uppercase tracking-widest">Attainment / Threshold</th>
                  <th className="px-4 py-3 text-center font-bold text-xs uppercase tracking-widest">Status</th>
                  <th className="px-4 py-3 text-left font-bold text-xs uppercase tracking-widest">Admin Comment</th>
                  <th className="px-4 py-3 text-center font-bold text-xs uppercase tracking-widest">Action</th>
                </tr>
              </thead>
              <tbody>
                {plans.map((a, idx) => (
                  <tr key={a.id} className={idx % 2 === 0 ? 'bg-white' : 'bg-slate-50'}>
                    <td className="px-4 py-3 font-semibold text-slate-800 border-r border-slate-100">{a.moduleName || a.moduleId}</td>
                    <td className="px-4 py-3 text-slate-700 border-r border-slate-100">{a.losId}{a.losName ? ` — ${a.losName}` : ''}</td>
                    <td className="px-4 py-3 text-center border-r border-slate-100">{a.batch}</td>
                    <td className="px-4 py-3 text-center border-r border-slate-100">
                      {a.attainmentScore != null ? `${Number(a.attainmentScore).toFixed(1)}%` : '—'} / {a.targetScore != null ? `${Number(a.targetScore).toFixed(1)}%` : '—'}
                    </td>
                    <td className="px-4 py-3 text-center border-r border-slate-100">
                      <span className={`px-2 py-0.5 rounded-md text-[10px] font-black uppercase tracking-widest ${statusStyles[a.status] || 'bg-slate-100 text-slate-600'}`}>
                        {a.status === 'PLANNED' && !a.submitted ? 'NEEDS YOUR INPUT' : a.status}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-slate-600 border-r border-slate-100">{a.adminComment || '—'}</td>
                    <td className="px-4 py-3 text-center">
                      <button type="button" onClick={() => navigate(`/marks-workbench/${a.moduleId}`)}
                        className="px-3 py-1.5 rounded-lg bg-indigo-50 text-indigo-600 text-xs font-bold hover:bg-indigo-100 transition-colors">
                        Open Module
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </main>
      <Footer />
    </div>
  )
}
