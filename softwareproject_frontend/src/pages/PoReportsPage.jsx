import { useState } from 'react'
import axios from 'axios'
import Header from '../components/header'
import Footer from '../components/footer'
import authService from '../services/authService'

const headers = () => ({ Authorization: `Bearer ${authService.getToken()}` })
const num = v => (v == null ? '—' : Number(v).toFixed(1))

function statusClasses(status) {
  if (status === 'Attained' || status === 'Success') return 'bg-emerald-50 text-emerald-700 border-emerald-200'
  if (status === 'No evidence') return 'bg-amber-50 text-amber-700 border-amber-200'
  return 'bg-red-50 text-red-700 border-red-200'
}

function StatusBadge({ status }) {
  return <span className={`inline-block px-2.5 py-1 rounded-lg border text-[11px] font-black uppercase tracking-wide ${statusClasses(status)}`}>{status}</span>
}

async function downloadPdf(url, params, filename, setBusy, setError, key) {
  setBusy(key); setError('')
  try {
    const response = await axios.get(url, { headers: headers(), responseType: 'blob', params: { ...params, format: 'pdf' } })
    const blobUrl = URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }))
    const anchor = document.createElement('a')
    anchor.href = blobUrl; anchor.download = filename
    document.body.appendChild(anchor); anchor.click(); anchor.remove()
    setTimeout(() => URL.revokeObjectURL(blobUrl), 1000)
  } catch (e) {
    let message = 'Could not download the PDF. Please try again.'
    if (e.response?.data instanceof Blob) {
      try { message = JSON.parse(await e.response.data.text()).message || message } catch { /* keep default */ }
    } else if (e.response?.data?.message) message = e.response.data.message
    setError(message)
  } finally { setBusy('') }
}

function StudentReportSection() {
  const [studentId, setStudentId] = useState('')
  const [threshold, setThreshold] = useState('40')
  const [report, setReport] = useState(null)
  const [busy, setBusy] = useState('')
  const [error, setError] = useState('')

  const preview = async e => {
    e.preventDefault()
    setBusy('preview'); setError(''); setReport(null)
    try {
      const r = await axios.get('/api/reports/po/student', {
        headers: headers(), params: { studentId: studentId.trim(), studentThreshold: threshold },
      })
      setReport(r.data)
    } catch (e2) { setError(e2.response?.data?.message || 'Could not generate this student’s PO report.') }
    finally { setBusy('') }
  }

  const download = () => {
    if (!report) return
    downloadPdf('/api/reports/po/student',
      { studentId: report.studentId, studentThreshold: report.studentThreshold },
      `po-report-${report.studentId.replace(/[^A-Za-z0-9_-]/g, '_')}.pdf`, setBusy, setError, 'download')
  }

  return (
    <section className="glass-card rounded-[2.5rem] p-8 border-slate-100 space-y-6">
      <div>
        <span className="text-[10px] font-black text-indigo-600 uppercase tracking-[0.2em] mb-1 block">Individual</span>
        <h2 className="heading-lg">Student PO Report</h2>
        <p className="text-sm text-slate-500 mt-1">
          A student&apos;s cumulative PO credits across every module calculated so far — their whole recorded academic standing, not one batch.
        </p>
      </div>

      <form onSubmit={preview} className="flex flex-col sm:flex-row gap-3 items-stretch sm:items-end">
        <div className="flex-1 space-y-1">
          <label className="text-xs font-black text-slate-500 uppercase tracking-widest">Student ID</label>
          <input type="text" value={studentId} onChange={e => setStudentId(e.target.value)}
            placeholder="e.g. EG/2024/6555" required className="input-field bg-white w-full" />
        </div>
        <div className="sm:w-56 space-y-1">
          <label className="text-xs font-black text-slate-500 uppercase tracking-widest">Attainment threshold (%)</label>
          <input type="number" min="0" max="100" step="0.01" value={threshold} onChange={e => setThreshold(e.target.value)}
            required className="input-field bg-white w-full" />
        </div>
        <button type="submit" disabled={!!busy}
          className={`px-6 py-3 rounded-2xl text-white font-bold shadow-lg transition-all ${busy ? 'bg-slate-300 cursor-not-allowed' : 'bg-indigo-600 hover:bg-indigo-700'}`}>
          {busy === 'preview' ? 'Generating…' : 'Preview'}
        </button>
      </form>

      {error && <div className="rounded-2xl border-2 border-red-200 bg-red-50 p-4 text-sm font-semibold text-red-700">{error}</div>}

      {report && (
        <div className="space-y-4 pt-2 border-t border-slate-100">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
            <div>
              <h3 className="font-black text-slate-800 text-lg">{report.studentName} <span className="text-slate-400 font-semibold">({report.studentId})</span></h3>
              <p className="text-xs text-slate-500 mt-1">
                Threshold: <strong>{num(report.studentThreshold)}%</strong> · Modules contributing: <strong>{report.moduleCount}</strong>
              </p>
            </div>
            <button type="button" onClick={download} disabled={!!busy}
              className={`px-5 py-2.5 rounded-xl border font-bold text-sm transition-all ${busy === 'download' ? 'bg-slate-100 text-slate-400 cursor-not-allowed' : 'bg-white text-indigo-700 border-indigo-200 hover:border-indigo-400'}`}>
              {busy === 'download' ? 'Downloading…' : 'Download PDF'}
            </button>
          </div>

          <div className="overflow-x-auto rounded-2xl border border-slate-200">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-slate-800 text-white">
                  <th className="px-4 py-3 text-left font-bold text-xs uppercase tracking-widest">PO</th>
                  <th className="px-4 py-3 text-center font-bold text-xs uppercase tracking-widest">Credits</th>
                  <th className="px-4 py-3 text-center font-bold text-xs uppercase tracking-widest">%</th>
                  <th className="px-4 py-3 text-center font-bold text-xs uppercase tracking-widest">Status</th>
                  <th className="px-4 py-3 text-left font-bold text-xs uppercase tracking-widest">Modules</th>
                </tr>
              </thead>
              <tbody>
                {report.pos.map((po, idx) => (
                  <tr key={po.code} className={idx % 2 === 0 ? 'bg-white' : 'bg-slate-50'}>
                    <td className="px-4 py-3 font-black text-slate-800 border-r border-slate-100">{po.code}<div className="text-[11px] font-medium text-slate-400">{po.title}</div></td>
                    <td className="px-4 py-3 text-center font-bold border-r border-slate-100">{po.creditsEarned}/{po.maxCredits}</td>
                    <td className="px-4 py-3 text-center font-bold border-r border-slate-100">{num(po.percentage)}%</td>
                    <td className="px-4 py-3 text-center border-r border-slate-100"><StatusBadge status={po.status} /></td>
                    <td className="px-4 py-3">
                      <div className="flex flex-wrap gap-1.5">
                        {po.moduleBreakdown.map((m, i) => (
                          <span key={i} className="inline-flex items-center gap-1 px-2 py-1 rounded-lg bg-indigo-50 text-indigo-700 text-[11px] font-semibold">
                            {m.moduleId} <span className="text-indigo-400">(batch {m.batch}: {m.creditsEarned}/{m.maxCredits})</span>
                          </span>
                        ))}
                      </div>
                    </td>
                  </tr>
                ))}
                {!report.pos.length && <tr><td colSpan={5} className="px-4 py-6 text-center text-slate-400">No PO credits saved for this student yet.</td></tr>}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </section>
  )
}

function BatchReportSection() {
  const [batch, setBatch] = useState('')
  const [studentThreshold, setStudentThreshold] = useState('40')
  const [batchTarget, setBatchTarget] = useState('60')
  const [report, setReport] = useState(null)
  const [busy, setBusy] = useState('')
  const [error, setError] = useState('')

  const preview = async e => {
    e.preventDefault()
    setBusy('preview'); setError(''); setReport(null)
    try {
      const r = await axios.get('/api/reports/po/batch', {
        headers: headers(), params: { batch: batch.trim(), studentThreshold, batchTarget },
      })
      setReport(r.data)
    } catch (e2) { setError(e2.response?.data?.message || 'Could not generate this batch’s PO report.') }
    finally { setBusy('') }
  }

  const download = () => {
    if (!report) return
    downloadPdf('/api/reports/po/batch',
      { batch: report.batch, studentThreshold: report.studentThreshold, batchTarget: report.batchTarget },
      `po-batch-report-${report.batch.replace(/[^A-Za-z0-9_-]/g, '_')}.pdf`, setBusy, setError, 'download')
  }

  return (
    <section className="glass-card rounded-[2.5rem] p-8 border-slate-100 space-y-6">
      <div>
        <span className="text-[10px] font-black text-emerald-600 uppercase tracking-[0.2em] mb-1 block">Batch</span>
        <h2 className="heading-lg">Batch PO Success Report</h2>
        <p className="text-sm text-slate-500 mt-1">
          A student attains a PO when their saved credit % in this batch meets the student threshold. A PO counts as a batch success when enough of the batch attained it.
        </p>
      </div>

      <form onSubmit={preview} className="flex flex-col sm:flex-row gap-3 items-stretch sm:items-end flex-wrap">
        <div className="flex-1 min-w-[10rem] space-y-1">
          <label className="text-xs font-black text-slate-500 uppercase tracking-widest">Batch</label>
          <input type="text" value={batch} onChange={e => setBatch(e.target.value)} placeholder="e.g. 24" required className="input-field bg-white w-full" />
        </div>
        <div className="sm:w-56 space-y-1">
          <label className="text-xs font-black text-slate-500 uppercase tracking-widest">Student threshold (%)</label>
          <input type="number" min="0" max="100" step="0.01" value={studentThreshold} onChange={e => setStudentThreshold(e.target.value)}
            required className="input-field bg-white w-full" />
          <p className="text-[11px] text-slate-400">Minimum credit % for one student to count as attaining a PO.</p>
        </div>
        <div className="sm:w-56 space-y-1">
          <label className="text-xs font-black text-slate-500 uppercase tracking-widest">Batch success target (%)</label>
          <input type="number" min="0" max="100" step="0.01" value={batchTarget} onChange={e => setBatchTarget(e.target.value)}
            required className="input-field bg-white w-full" />
          <p className="text-[11px] text-slate-400">Minimum share of the batch that must attain a PO for it to succeed.</p>
        </div>
        <button type="submit" disabled={!!busy}
          className={`px-6 py-3 rounded-2xl text-white font-bold shadow-lg transition-all ${busy ? 'bg-slate-300 cursor-not-allowed' : 'bg-emerald-600 hover:bg-emerald-700'}`}>
          {busy === 'preview' ? 'Generating…' : 'Preview'}
        </button>
      </form>

      {error && <div className="rounded-2xl border-2 border-red-200 bg-red-50 p-4 text-sm font-semibold text-red-700">{error}</div>}

      {report && (
        <div className="space-y-4 pt-2 border-t border-slate-100">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
            <div>
              <h3 className="font-black text-slate-800 text-lg">Batch {report.batch}</h3>
              <p className="text-xs text-slate-500 mt-1">
                Total students: <strong>{report.totalStudents}</strong> · Student threshold: <strong>{num(report.studentThreshold)}%</strong> · Batch target: <strong>{num(report.batchTarget)}%</strong>
              </p>
            </div>
            <button type="button" onClick={download} disabled={!!busy}
              className={`px-5 py-2.5 rounded-xl border font-bold text-sm transition-all ${busy === 'download' ? 'bg-slate-100 text-slate-400 cursor-not-allowed' : 'bg-white text-emerald-700 border-emerald-200 hover:border-emerald-400'}`}>
              {busy === 'download' ? 'Downloading…' : 'Download PDF'}
            </button>
          </div>

          <div className="overflow-x-auto rounded-2xl border border-slate-200">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-slate-800 text-white">
                  <th className="px-4 py-3 text-left font-bold text-xs uppercase tracking-widest">PO</th>
                  <th className="px-4 py-3 text-center font-bold text-xs uppercase tracking-widest">Students Attained</th>
                  <th className="px-4 py-3 text-center font-bold text-xs uppercase tracking-widest">Attainment %</th>
                  <th className="px-4 py-3 text-center font-bold text-xs uppercase tracking-widest">Status</th>
                </tr>
              </thead>
              <tbody>
                {report.pos.map((po, idx) => (
                  <tr key={po.code} className={idx % 2 === 0 ? 'bg-white' : 'bg-slate-50'}>
                    <td className="px-4 py-3 font-black text-slate-800 border-r border-slate-100">{po.code}<div className="text-[11px] font-medium text-slate-400">{po.title}</div></td>
                    <td className="px-4 py-3 text-center font-bold border-r border-slate-100">{po.studentsAttained} / {po.totalStudents}</td>
                    <td className="px-4 py-3 text-center font-bold border-r border-slate-100">{num(po.attainmentPercent)}%</td>
                    <td className="px-4 py-3 text-center"><StatusBadge status={po.status} /></td>
                  </tr>
                ))}
                {!report.pos.length && <tr><td colSpan={4} className="px-4 py-6 text-center text-slate-400">No active programme outcomes are configured.</td></tr>}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </section>
  )
}

export default function PoReportsPage() {
  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <Header />
      <main className="flex-1 max-w-5xl mx-auto w-full px-6 py-10 space-y-8">
        <div>
          <span className="text-[10px] font-black text-indigo-600 uppercase tracking-[0.2em] mb-1 block">Admin</span>
          <h1 className="heading-lg text-2xl">PO Attainment Reports</h1>
          <p className="text-sm text-slate-500 mt-1 max-w-2xl">
            Generate and download PO attainment reports as PDF — per student (cumulative across all their modules) or per batch (success rate for each PO).
          </p>
        </div>
        <StudentReportSection />
        <BatchReportSection />
      </main>
      <Footer />
    </div>
  )
}
