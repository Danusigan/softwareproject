import { useState } from 'react'
import axios from 'axios'
import Header from '../components/header'
import Footer from '../components/footer'
import authService from '../services/authService'
import './studentReports.css'
import './batchReports.css'

const number = value => value == null ? 'â€”' : Number(value).toFixed(2)
const statusClass = status => status === 'Achieved' ? 'achieved' : status === 'Below target' ? 'below' : 'pending'
const headers = () => ({ Authorization: `Bearer ${authService.getToken()}` })
const paramsFor = (batch, moduleIds, studentThreshold, loTarget, poTarget, format = 'json') => {
  const params = new URLSearchParams({ batch, studentThreshold, loTarget, poTarget, format })
  moduleIds.forEach(id => params.append('moduleIds', id))
  return params
}

function AttainmentBar({ label, value, target, status }) {
  return <div className="batch-chart-row">
    <span>{label}</span>
    <div className="report-bar" role="img" aria-label={`${label}: ${number(value)} percent, target ${target} percent, ${status}`}>
      {value != null && <span className={statusClass(status)} style={{ width: `${value}%` }} />}
      <i style={{ left: `${target}%` }} />
    </div>
    <span>{number(value)}% <small>{status}</small></span>
  </div>
}

export default function BatchReportsPage() {
  const [batch, setBatch] = useState('')
  const [loadedBatch, setLoadedBatch] = useState('')
  const [modules, setModules] = useState([])
  const [selected, setSelected] = useState([])
  const [studentThreshold, setStudentThreshold] = useState('50')
  const [loTarget, setLoTarget] = useState('70')
  const [poTarget, setPoTarget] = useState('70')
  const [searched, setSearched] = useState(false)
  const [report, setReport] = useState(null)
  const [busy, setBusy] = useState('')
  const [error, setError] = useState('')

  const loadModules = async event => {
    event.preventDefault()
    setBusy('modules'); setError(''); setReport(null); setModules([]); setSelected([]); setSearched(false)
    try {
      const response = await axios.get('/api/reports/batches/modules', { headers: headers(), params: { batch: batch.trim() } })
      setModules(response.data); setSelected(response.data.map(m => m.moduleId))
      setLoadedBatch(batch.trim()); setSearched(true)
    } catch (e) { setError(e.response?.data?.message || 'Could not load batch modules. Check your login and backend connection.') }
    finally { setBusy('') }
  }
  const preview = async event => {
    event.preventDefault()
    setBusy('preview'); setReport(null); setError('')
    try {
      const response = await axios.get('/api/reports/batches/attainment', {
        headers: headers(), params: paramsFor(loadedBatch, selected, studentThreshold, loTarget, poTarget),
      })
      setReport(response.data)
    } catch (e) { setError(e.response?.data?.message || 'Could not generate the batch report.') }
    finally { setBusy('') }
  }
  const download = async () => {
    if (!report) return
    setBusy('download'); setError('')
    try {
      const response = await axios.get('/api/reports/batches/attainment', {
        headers: headers(), responseType: 'blob',
        params: paramsFor(report.batch, report.modules.map(m => m.moduleId), report.studentThreshold, report.loTarget, report.poTarget, 'pdf'),
      })
      const url = URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }))
      const anchor = document.createElement('a')
      anchor.href = url; anchor.download = `batch-attainment-${report.batch.replace(/[^A-Za-z0-9_-]/g, '_')}.pdf`
      document.body.appendChild(anchor); anchor.click(); anchor.remove()
      setTimeout(() => URL.revokeObjectURL(url), 1000)
    } catch (e) {
      let message = 'Could not download the PDF. Please try again.'
      if (e.response?.data instanceof Blob) {
        try { message = JSON.parse(await e.response.data.text()).message || message } catch { /* keep download message */ }
      }
      setError(message)
    } finally { setBusy('') }
  }
  const changeSetting = setter => event => { setter(event.target.value); setReport(null) }
  const toggleModule = id => {
    setSelected(current => current.includes(id) ? current.filter(value => value !== id) : [...current, id])
    setReport(null)
  }
  const allLos = report?.modules.flatMap(m => m.los) || []
  return <div className="student-reports batch-reports">
    <Header />
    <main className="report-shell">
      <p className="report-eyebrow">Department quality assurance</p>
      <h1>Batch attainment report</h1>
      <p className="report-intro">Review LO achievement, module coverage and approved LOâ€“PO contributions for a batch.</p>
      <section className="report-controls" aria-label="Batch report settings">
        <form onSubmit={loadModules}>
          <label htmlFor="batch-report-batch">Batch</label>
          <input id="batch-report-batch" required maxLength={50} value={batch} placeholder="e.g. 22" disabled={!!busy}
            onChange={event => { setBatch(event.target.value); setModules([]); setSelected([]); setReport(null); setSearched(false) }} />
          <button disabled={!!busy || !batch.trim()}>{busy === 'modules' ? 'Loadingâ€¦' : 'Find modules'}</button>
        </form>
        {!!modules.length && <form onSubmit={preview} className="batch-report-form">
          <fieldset className="batch-modules"><legend>Modules in report</legend>
            {modules.map(module => <label key={module.moduleId}>
              <input type="checkbox" checked={selected.includes(module.moduleId)} disabled={!!busy} onChange={() => toggleModule(module.moduleId)} />
              {module.moduleId} â€” {module.moduleName}
            </label>)}
          </fieldset>
          <div className="batch-targets">
            <label>Student LO threshold (%)<input aria-label="Student LO threshold (%)" type="number" required min="0" max="100" step="0.01"
              value={studentThreshold} disabled={!!busy} onChange={changeSetting(setStudentThreshold)} />
              <small>Minimum individual LO score.</small></label>
            <label>Batch LO target (%)<input aria-label="Batch LO target (%)" type="number" required min="0" max="100" step="0.01"
              value={loTarget} disabled={!!busy} onChange={changeSetting(setLoTarget)} />
              <small>Required share of assessed students achieving an LO.</small></label>
            <label>PO target (%)<input aria-label="PO target (%)" type="number" required min="0" max="100" step="0.01"
              value={poTarget} disabled={!!busy} onChange={changeSetting(setPoTarget)} />
              <small>Required weighted LO achievement rate.</small></label>
          </div>
          <p className="report-help">These are editable report settings, not approved academic policy. Module attainment requires all defined LOs to meet the batch target with complete evidence.</p>
          <button disabled={!!busy || !selected.length}>{busy === 'preview' ? 'Generatingâ€¦' : 'Preview batch report'}</button>
        </form>}
      </section>
      {error && <p role="alert" className="report-error">{error}</p>}
      {searched && !modules.length && !error && <p role="status">No accessible modules found for batch {loadedBatch}.</p>}
      {report && <article className="report-preview">
        <div className="report-heading"><div>
          <p className="report-eyebrow">Analysis copy</p><h2>Batch {report.batch}</h2>
          <p>Generated {new Date(report.generatedAt).toLocaleString()}</p>
        </div><button onClick={download} disabled={!!busy}>{busy === 'download' ? 'Downloadingâ€¦' : 'Download batch PDF'}</button></div>
        <p className="batch-scope">{report.scope}</p>
        <p>Student threshold: <strong>{number(report.studentThreshold)}%</strong> Â· Batch LO target: <strong>{number(report.loTarget)}%</strong> Â· PO target: <strong>{number(report.poTarget)}%</strong></p>
        <div className="report-stats">
          <div><strong>{report.studentsWithRecords}</strong><span>Students with records</span></div>
          <div><strong>{report.modules.length}</strong><span>Modules in scope</span></div>
          <div><strong>{allLos.filter(lo => lo.status === 'Achieved').length} / {allLos.length}</strong><span>LO targets achieved</span></div>
          <div><strong>{report.pos.filter(po => po.status === 'Achieved').length} / {report.pos.length}</strong><span>Active PO targets achieved in scope</span></div>
        </div>
        <section className="report-module">
          <h3>Module LO achievement coverage</h3>
          <p className="report-help">This is the percentage of defined LOs meeting the batch target, not a module pass rate. Report target: 100% with complete evidence.</p>
          <div className="report-table-scroll"><table><caption>Module summary</caption><thead><tr>
            <th>Module</th><th>Students with records</th><th>LOs achieved / total</th><th>Coverage</th><th>Status</th>
          </tr></thead><tbody>{report.modules.map(module => <tr key={module.moduleId}>
            <td>{module.moduleId} â€” {module.moduleName}</td><td>{module.studentsWithRecords}</td>
            <td>{module.achievedLos} / {module.totalLos}</td><td>{number(module.achievedLoPercent)}%</td>
            <td><span className={`report-status ${statusClass(module.status)}`}>{module.status}</span></td>
          </tr>)}</tbody></table></div>
        </section>
        {report.modules.map(module => <section className="report-module" key={module.moduleId}>
          <h3>{module.moduleId} â€” {module.moduleName}</h3>
          <p className="report-help">LO percentage = achieved / fully assessed students. Percentages with pending evidence are provisional; the target line is {report.loTarget}%.</p>
          {module.los.map(lo => <AttainmentBar key={lo.loId} label={lo.loId} value={lo.achievementPercent} target={lo.target} status={lo.status} />)}
          <div className="report-table-scroll"><table><caption>LO results and evidence completeness</caption><thead><tr>
            <th>LO</th><th>Assessed / known</th><th>Achieved</th><th>Below threshold</th><th>Pending</th><th>LO achievement</th><th>Evidence coverage</th><th>Status</th>
          </tr></thead><tbody>{module.los.map(lo => <tr key={lo.loId}>
            <td>{lo.loId}<small>{lo.name}</small></td><td>{lo.assessed} / {lo.studentsWithRecords}</td><td>{lo.achieved}</td>
            <td>{lo.belowThreshold}</td><td>{lo.pending}</td><td>{number(lo.achievementPercent)}%</td><td>{number(lo.coveragePercent)}%</td>
            <td><span className={`report-status ${statusClass(lo.status)}`}>{lo.status}</span></td>
          </tr>)}</tbody></table></div>
        </section>)}
        <section className="report-module"><h3>PO attainment in the selected scope</h3>
          <p className="report-help">Weighted achievement rates from approved positive LOâ€“PO mappings. Pending scores use complete mapped LOs only and are provisional. These are not student PO pass rates.</p>
          {!report.pos.length && <p>No active programme outcomes are configured.</p>}
          {report.pos.map(po => <div className="batch-po" key={po.poId}>
            <h4>{po.code} â€” {po.title}</h4>
            <AttainmentBar label={po.code} value={po.attainmentPercent} target={po.target} status={po.status} />
            <p>{po.completeLos} of {po.mappedLos} mapped LOs have complete evidence.</p>
            {!!po.contributions.length && <div className="report-table-scroll"><table><caption>Approved contributions to {po.code}</caption>
              <thead><tr><th>Module</th><th>LO</th><th>Weight</th><th>LO achievement</th><th>Evidence status</th></tr></thead>
              <tbody>{po.contributions.map(c => <tr key={c.loId}><td>{c.moduleId}</td><td>{c.loId}</td><td>{c.weight}</td><td>{number(c.achievementPercent)}%</td><td>{c.status}</td></tr>)}</tbody>
            </table></div>}
          </div>)}
        </section>
        <section className="report-notes"><h3>Areas requiring review</h3>
          <ul>{report.modules.flatMap(module => module.los.filter(lo => lo.status !== 'Achieved').map(lo =>
            <li key={lo.loId}><strong>{module.moduleId} / {lo.loId}: {lo.status}.</strong> {lo.status === 'Below target'
              ? 'Review teaching and assessment evidence for this LO.' : 'Complete or verify assessment records before making an attainment decision.'}</li>))}</ul>
          {allLos.length > 0 && allLos.every(lo => lo.status === 'Achieved') && <p>All listed LOs meet the selected batch target.</p>}
          <h3>Calculation and scope notes</h3><ul>{report.notes.map(note => <li key={note}>{note}</li>)}</ul>
          <p>PDF downloads use the latest saved data at download time.</p>
        </section>
      </article>}
    </main><Footer />
  </div>
}
