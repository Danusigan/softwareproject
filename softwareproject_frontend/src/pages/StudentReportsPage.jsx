import { useState } from 'react'
import axios from 'axios'
import Header from '../components/header'
import Footer from '../components/footer'
import authService from '../services/authService'
import './studentReports.css'

const number = value => value == null ? 'â€”' : Number(value).toFixed(2)
const recorded = value => value || 'Not recorded'
const headers = () => ({ Authorization: `Bearer ${authService.getToken()}` })

export default function StudentReportsPage() {
  const [batch, setBatch] = useState('')
  const [loadedBatch, setLoadedBatch] = useState('')
  const [students, setStudents] = useState([])
  const [studentId, setStudentId] = useState('')
  const [threshold, setThreshold] = useState('50')
  const [report, setReport] = useState(null)
  const [busy, setBusy] = useState('')
  const [error, setError] = useState('')
  const [searched, setSearched] = useState(false)

  const loadStudents = async event => {
    event.preventDefault()
    setBusy('students'); setError(''); setReport(null); setStudents([]); setStudentId(''); setSearched(false)
    try {
      const response = await axios.get('/api/reports/students', { headers: headers(), params: { batch: batch.trim() } })
      setStudents(response.data); setLoadedBatch(batch.trim()); setSearched(true)
    } catch (e) { setError(e.response?.data?.message || 'Could not load students. Check that the backend is running and you are signed in.') }
    finally { setBusy('') }
  }

  const preview = async event => {
    event.preventDefault()
    setBusy('preview'); setError(''); setReport(null)
    try {
      const response = await axios.get('/api/reports/students/individual', {
        headers: headers(), params: { studentId, batch: loadedBatch, threshold: Number(threshold) },
      })
      setReport(response.data)
    } catch (e) { setError(e.response?.data?.message || 'Could not generate the report.') }
    finally { setBusy('') }
  }

  const download = async () => {
    if (!report) return
    setBusy('download'); setError('')
    try {
      const response = await axios.get('/api/reports/students/individual', {
        headers: headers(), responseType: 'blob',
        params: { studentId: report.studentId, batch: report.batch, threshold: report.threshold, format: 'pdf' },
      })
      const url = URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }))
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = `student-report-${report.studentId.replace(/[^a-zA-Z0-9_-]/g, '_')}.pdf`
      document.body.appendChild(anchor); anchor.click(); anchor.remove()
      setTimeout(() => URL.revokeObjectURL(url), 1000)
    } catch (e) {
      let message = 'Could not download the PDF. Please try again.'
      if (e.response?.data instanceof Blob) {
        try { message = JSON.parse(await e.response.data.text()).message || message } catch { /* keep friendly message */ }
      }
      setError(message)
    } finally { setBusy('') }
  }

  const allLos = report?.modules.flatMap(module => module.los) || []
  return (
    <div className="student-reports">
      <Header />
      <main className="report-shell">
        <p className="report-eyebrow">Department quality assurance</p>
        <h1>Individual student report</h1>
        <p className="report-intro">Review assessment evidence and LO achievement for one student, then download a PDF.</p>
        <section className="report-controls" aria-label="Report selection">
          <form onSubmit={loadStudents}>
            <label htmlFor="report-batch">Batch</label>
            <input id="report-batch" required maxLength={50} value={batch} placeholder="e.g. 22" disabled={!!busy}
              onChange={e => { setBatch(e.target.value); setStudents([]); setStudentId(''); setReport(null); setSearched(false) }} />
            <button disabled={!!busy || !batch.trim()}>{busy === 'students' ? 'Loadingâ€¦' : 'Find students'}</button>
          </form>
          {students.length > 0 && <form onSubmit={preview}>
            <label htmlFor="report-student">Student</label>
            <select id="report-student" required value={studentId} disabled={!!busy}
              onChange={e => { setStudentId(e.target.value); setReport(null) }}>
              <option value="">Select student</option>
              {students.map(student => <option key={student.studentId} value={student.studentId}>
                {student.studentId} â€” {student.studentName}
              </option>)}
            </select>
            <label htmlFor="report-threshold">LO achievement threshold (%)</label>
            <input id="report-threshold" type="number" min="0" max="100" step="0.01" required
              value={threshold} disabled={!!busy} onChange={e => { setThreshold(e.target.value); setReport(null) }} />
            <button disabled={!!busy || !studentId}>{busy === 'preview' ? 'Generatingâ€¦' : 'Preview report'}</button>
          </form>}
          <p className="report-help">The threshold applies only to this report. Saved marks and attainment settings are not changed.</p>
        </section>
        {error && <p role="alert" className="report-error">{error}</p>}
        {searched && !students.length && !error && <p role="status">No students with accessible marks were found for batch {loadedBatch}.</p>}
        {report && <article className="report-preview">
          <div className="report-heading">
            <div><p className="report-eyebrow">Analysis copy Â· Batch {report.batch}</p><h2>{report.studentName}</h2><p>{report.studentId}</p></div>
            <button onClick={download} disabled={!!busy}>{busy === 'download' ? 'Downloadingâ€¦' : 'Download PDF'}</button>
          </div>
          <dl className="report-metadata">
            <div><dt>Email</dt><dd>{recorded(report.email)}</dd></div>
            <div><dt>Academic year</dt><dd>{recorded(report.academicYear)}</dd></div>
            <div><dt>Report threshold</dt><dd>{number(report.threshold)}%</dd></div>
            <div><dt>Generated</dt><dd>{new Date(report.generatedAt).toLocaleString()}</dd></div>
          </dl>
          <p>{report.scope}</p>
          <div className="report-stats">
            <div><strong>{report.modules.length}</strong><span>Modules</span></div>
            <div><strong>{allLos.filter(lo => lo.status === 'Achieved').length}</strong><span>LOs achieved</span></div>
            <div><strong>{allLos.filter(lo => lo.status === 'Below threshold').length}</strong><span>Below threshold</span></div>
            <div><strong>{allLos.filter(lo => lo.percentage == null).length}</strong><span>Pending / not assessed</span></div>
          </div>
          {report.modules.map(module => <section key={module.moduleId} className="report-module">
            <h3>{module.moduleId} â€” {module.moduleName}</h3>
            <p className="report-help">Bars show LO scores; the vertical line marks the selected threshold. These are LO results, not module pass/fail.</p>
            {module.los.map(lo => <section key={lo.loId} className="report-lo">
              <div className="report-heading"><h4>{lo.loId} â€” {lo.name}</h4>
                <span className={`report-status ${lo.status === 'Achieved' ? 'achieved' : lo.status === 'Below threshold' ? 'below' : 'pending'}`}>{lo.status}</span>
              </div>
              {lo.description && <p>{lo.description}</p>}
              {lo.percentage != null ? <>
                <div className="report-bar" role="img" aria-label={`${lo.loId}: ${number(lo.percentage)} percent, threshold ${number(lo.threshold)} percent`}>
                  <span style={{ width: `${lo.percentage}%`, background: lo.status === 'Achieved' ? '#15803d' : '#b91c1c' }} />
                  <i style={{ left: `${lo.threshold}%` }} />
                </div>
                <p>Score: <strong>{number(lo.percentage)}%</strong> Â· Threshold: {number(lo.threshold)}% Â· Gap: {lo.margin > 0 ? '+' : ''}{number(lo.margin)} percentage points</p>
              </> : <p>Achievement cannot yet be determined from the available evidence.</p>}
              <div className="report-table-scroll">
                <table><caption>Assessment marks for {lo.loId}</caption><thead><tr>
                  <th>Assessment</th><th>Period</th><th>Question / component</th><th>Mark</th><th>Maximum</th><th>%</th><th>Evidence</th>
                </tr></thead><tbody>
                  {lo.marks.map((mark, index) => <tr key={index}>
                    <td>{recorded(mark.assessment)}<small>{recorded(mark.type)}</small></td>
                    <td>{recorded(mark.academicYear)}<small>{recorded(mark.semester)}</small></td>
                    <td>{mark.question}</td><td>{number(mark.score)}</td><td>{number(mark.maximum)}</td><td>{number(mark.percentage)}</td><td>{mark.source}</td>
                  </tr>)}
                  {!lo.marks.length && <tr><td colSpan={7}>No recorded assessments.</td></tr>}
                </tbody></table>
              </div>
            </section>)}
          </section>)}
          <section className="report-notes"><h3>Calculation and interpretation</h3><ul>{report.notes.map(note => <li key={note}>{note}</li>)}</ul>
            <p>PDF downloads use the latest saved marks at download time.</p>
          </section>
        </article>}
      </main>
      <Footer />
    </div>
  )
}
