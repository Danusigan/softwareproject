import { useState } from 'react'
import Header from '../components/header'
import Footer from '../components/footer'
import authService from '../services/authService'
import marksService from '../services/marksService'

const authHeaders = () => {
  const t = authService.getToken()
  return t ? { Authorization: `Bearer ${t}` } : undefined
}

// A module can contribute to the same PO twice under the hood (once via Final Exam marks,
// once via Assignment marks) — two separate saved rows. This page doesn't distinguish mark
// types, so those are combined into one badge per module+batch here rather than shown as two
// unexplained near-duplicate entries.
function mergeByModule(moduleBreakdown) {
  const merged = new Map()
  for (const m of moduleBreakdown || []) {
    const key = `${m.moduleId}|${m.batch}`
    const existing = merged.get(key)
    if (existing) {
      existing.creditsEarned += m.creditsEarned
      existing.maxCredits += m.maxCredits
    } else {
      merged.set(key, { moduleId: m.moduleId, batch: m.batch, creditsEarned: m.creditsEarned, maxCredits: m.maxCredits })
    }
  }
  return Array.from(merged.values())
}

export default function StudentPOSummaryPage() {
  const [studentId, setStudentId] = useState('')
  const [summary, setSummary] = useState(null)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState({ type: '', text: '' })

  const handleSearch = async (e) => {
    e?.preventDefault?.()
    const id = studentId.trim()
    if (!id) { setMessage({ type: 'error', text: 'Enter a student ID.' }); return }
    try {
      setBusy(true); setMessage({ type: '', text: '' }); setSummary(null)
      const r = await marksService.getStudentPOSummary({ studentId: id }, { headers: authHeaders() })
      const data = r.data?.data || r.data
      setSummary(data)
      if (!data?.poSummaries?.length) {
        setMessage({ type: 'info', text: `No saved PO attainment found for ${id}. Calculate PO attainment for a module this student is in first — every calculation there saves automatically.` })
      }
    } catch (err) {
      setMessage({ type: 'error', text: err.response?.data?.message || 'Could not load this student’s PO summary.' })
    } finally { setBusy(false) }
  }

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <Header />
      <main className="flex-1 max-w-5xl mx-auto w-full px-6 py-10 space-y-6">
        <div>
          <span className="text-[10px] font-black text-indigo-600 uppercase tracking-[0.2em] mb-1 block">Cross-Module</span>
          <h1 className="heading-lg text-2xl">Student PO Credit Summary</h1>
          <p className="text-sm text-slate-500 mt-1 max-w-2xl">
            Adds up a student&apos;s saved PO credits across every module whose PO attainment has
            been calculated. This shows raw credits earned vs. possible only — it does not decide
            whether a PO counts as attained; that threshold decision is a separate, future feature.
          </p>
        </div>

        <form onSubmit={handleSearch} className="glass-card rounded-[2rem] p-6 flex flex-col sm:flex-row gap-3 items-stretch sm:items-end">
          <div className="flex-1 space-y-1">
            <label className="text-xs font-black text-slate-500 uppercase tracking-widest">Student ID</label>
            <input
              type="text"
              value={studentId}
              onChange={e => setStudentId(e.target.value)}
              placeholder="e.g. EG/2024/6555"
              className="input-field bg-white w-full"
            />
          </div>
          <button type="submit" disabled={busy}
            className={`px-6 py-3 rounded-2xl text-white font-bold shadow-lg transition-all flex items-center justify-center gap-2 ${busy ? 'bg-slate-300 cursor-not-allowed' : 'bg-indigo-600 hover:bg-indigo-700'}`}>
            {busy && <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />}
            Search
          </button>
        </form>

        {message.text && (
          <div className={`rounded-2xl border-2 p-4 text-sm font-semibold ${
            message.type === 'error' ? 'border-red-200 bg-red-50 text-red-700' : 'border-amber-200 bg-amber-50 text-amber-700'
          }`}>
            {message.text}
          </div>
        )}

        {summary?.poSummaries?.length > 0 && (
          <section className="glass-card rounded-[2.5rem] p-8 border-slate-100 space-y-6">
            <div>
              <span className="text-[10px] font-black text-emerald-600 uppercase tracking-[0.2em] mb-1 block">Results</span>
              <h2 className="heading-lg">{summary.studentId}</h2>
              <p className="text-sm text-slate-500 mt-1">
                Modules contributing: <strong>{summary.moduleCount}</strong>
              </p>
            </div>

            <div className="overflow-x-auto rounded-2xl border border-slate-200">
              <table className="w-full text-sm">
                <thead>
                  <tr className="bg-slate-800 text-white">
                    <th className="px-4 py-3 text-left font-bold text-xs uppercase tracking-widest">PO</th>
                    <th className="px-4 py-3 text-center font-bold text-xs uppercase tracking-widest">Credits Earned</th>
                    <th className="px-4 py-3 text-center font-bold text-xs uppercase tracking-widest">Max Credits</th>
                    <th className="px-4 py-3 text-center font-bold text-xs uppercase tracking-widest">%</th>
                    <th className="px-4 py-3 text-left font-bold text-xs uppercase tracking-widest">Contributing Modules</th>
                  </tr>
                </thead>
                <tbody>
                  {summary.poSummaries.map((po, idx) => (
                    <tr key={po.poCode} className={idx % 2 === 0 ? 'bg-white' : 'bg-slate-50'}>
                      <td className="px-4 py-3 font-black text-slate-800 border-r border-slate-100">{po.poCode}</td>
                      <td className="px-4 py-3 text-center font-bold border-r border-slate-100">{po.creditsEarned}</td>
                      <td className="px-4 py-3 text-center font-bold border-r border-slate-100">{po.maxCredits}</td>
                      <td className="px-4 py-3 text-center font-bold border-r border-slate-100">
                        {po.percentage != null ? `${po.percentage.toFixed(1)}%` : '—'}
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex flex-wrap gap-1.5">
                          {mergeByModule(po.moduleBreakdown).map((m, i) => (
                            <span key={i} className="inline-flex items-center gap-1 px-2 py-1 rounded-lg bg-indigo-50 text-indigo-700 text-[11px] font-semibold">
                              {m.moduleId} <span className="text-indigo-400">(batch {m.batch}: {m.creditsEarned}/{m.maxCredits})</span>
                            </span>
                          ))}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <p className="text-xs text-slate-400">
              These numbers come from every module’s saved PO attainment calculation as of the
              last time it was run there — they update the next time a lecturer recalculates that
              module, not automatically. Whether a given percentage counts as the PO being
              attained is not decided here.
            </p>
          </section>
        )}
      </main>
      <Footer />
    </div>
  )
}
