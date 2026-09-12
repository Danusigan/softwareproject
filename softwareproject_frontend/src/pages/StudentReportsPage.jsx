import { useState } from 'react';
import axios from 'axios';
import PropTypes from 'prop-types';
import Header from '../components/header';
import Footer from '../components/footer';
import authService from '../services/authService';
import ProgressConfigurationPanel from '../components/ProgressConfigurationPanel';
import './studentReports.css';

const number = (value) => (value == null ? 'Not recorded' : Number(value).toFixed(2));
const text = (value) => value || 'Not configured';
const headers = () => ({ Authorization: `Bearer ${authService.getToken()}` });
const emptyFilters = { year: '', semester: '', module: '', lo: '', po: '' };
const label = (status) => status?.replaceAll('_', ' ') || 'INSUFFICIENT EVIDENCE';

function Status({ value }) {
  return <span className={`report-status status-${value?.toLowerCase()}`}>{label(value)}</span>;
}

Status.propTypes = { value: PropTypes.string };

function Evidence({ marks }) {
  return (
    <div className="report-table-scroll">
      <table>
        <caption>Assessment evidence</caption>
        <thead>
          <tr>
            <th>Assessment</th>
            <th>Question / component</th>
            <th>Obtained mark</th>
            <th>Maximum mark</th>
          </tr>
        </thead>
        <tbody>
          {marks.map((mark, index) => (
            <tr key={index}>
              <td>{mark.assessment}</td>
              <td>{mark.question}</td>
              <td>{number(mark.obtained)}</td>
              <td>{number(mark.maximum)}</td>
            </tr>
          ))}
          {!marks.length && (
            <tr>
              <td colSpan={4}>No mapped question evidence is available.</td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}

Evidence.propTypes = {
  marks: PropTypes.arrayOf(
    PropTypes.shape({
      assessment: PropTypes.string,
      question: PropTypes.string,
      obtained: PropTypes.number,
      maximum: PropTypes.number,
    })
  ).isRequired,
};

export default function StudentReportsPage() {
  const [query, setQuery] = useState('');
  const [students, setStudents] = useState([]);
  const [studentId, setStudentId] = useState('');
  const [report, setReport] = useState(null);
  const [filters, setFilters] = useState(emptyFilters);
  const [busy, setBusy] = useState('');
  const [error, setError] = useState('');
  const [searched, setSearched] = useState(false);
  const role = authService.getUserInfo()?.userType?.toLowerCase();

  async function search(event) {
    event.preventDefault();
    setBusy('search');
    setError('');
    setStudents([]);
    setStudentId('');
    setReport(null);
    setSearched(false);
    try {
      const response = await axios.get('/api/reports/progress/students', {
        headers: headers(),
        params: { q: query.trim() },
      });
      setStudents(response.data);
      setSearched(true);
    } catch (e) {
      setError(
        e.response?.data?.message ||
          'Could not search students. Please check your login and connection.'
      );
    } finally {
      setBusy('');
    }
  }
  async function generate(event) {
    event.preventDefault();
    setBusy('generate');
    setError('');
    setReport(null);
    try {
      const response = await axios.post(
        `/api/reports/progress/students/${encodeURIComponent(studentId)}/snapshots`,
        {},
        { headers: headers() }
      );
      setReport(response.data);
      setFilters(emptyFilters);
    } catch (e) {
      setError(e.response?.data?.message || 'Could not generate the student report.');
    } finally {
      setBusy('');
    }
  }
  async function download() {
    setBusy('download');
    setError('');
    try {
      const response = await axios.get(`/api/reports/progress/snapshots/${report.reference}/pdf`, {
        headers: headers(),
        responseType: 'blob',
      });
      const url = URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = `student-progress-${report.reference}.pdf`;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (e) {
      let message = 'Could not download the PDF.';
      if (e.response?.data instanceof Blob) {
        try {
          message = JSON.parse(await e.response.data.text()).message || message;
        } catch {
          /* Preserve the download error. */
        }
      }
      setError(message);
    } finally {
      setBusy('');
    }
  }

  const history =
    report?.modules
      .flatMap((module) => module.attempts.map((attempt) => ({ module, attempt })))
      .sort(
        (a, b) =>
          a.attempt.startsOn.localeCompare(b.attempt.startsOn) ||
          a.module.moduleId.localeCompare(b.module.moduleId)
      ) || [];
  const matchesPeriod = (attempt) =>
    (!filters.year || attempt.academicYear === filters.year) &&
    (!filters.semester || attempt.semester === filters.semester);
  const visibleHistory = history.filter(
    ({ module, attempt }) =>
      (!filters.module || module.moduleId === filters.module) && matchesPeriod(attempt)
  );
  const visibleModules =
    report?.modules.filter(
      (module) =>
        (!filters.module || module.moduleId === filters.module) &&
        ((!filters.year && !filters.semester) || module.attempts.some(matchesPeriod))
    ) || [];
  const visiblePos = report?.pos.filter((po) => !filters.po || po.poId === filters.po) || [];
  const ranked =
    report?.pos
      .filter((po) => po.calculation.result.percentage != null)
      .sort((a, b) => a.calculation.result.percentage - b.calculation.result.percentage) || [];
  const options = {
    year: [...new Set(history.map((h) => h.attempt.academicYear))].map((v) => [v, v]),
    semester: [...new Set(history.map((h) => h.attempt.semester))].map((v) => [v, v]),
    module: report?.modules.map((m) => [m.moduleId, `${m.moduleId} - ${m.moduleName}`]) || [],
    lo: [
      ...new Map(
        report?.modules.flatMap((m) => m.los.map((lo) => [lo.loId, `${lo.loId} - ${lo.name}`])) ||
          []
      ),
    ],
    po: report?.pos.map((po) => [po.poId, po.code]) || [],
  };

  return (
    <div className="student-reports">
      <Header />
      <main className="report-shell">
        <p className="report-eyebrow">University quality assurance</p>
        <h1>Student academic progress report</h1>
        <p className="report-intro">
          Review academic history, Learning Outcomes and Programme Outcome evidence. Preview and PDF
          use the same saved report.
        </p>
        <section className="report-controls" aria-label="Student selection">
          <form onSubmit={search}>
            <label htmlFor="student-search">Student index or name</label>
            <input
              id="student-search"
              maxLength={100}
              value={query}
              disabled={!!busy}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search accessible students"
            />
            <button disabled={!!busy}>
              {busy === 'search' ? 'Searching...' : 'Find students'}
            </button>
          </form>
          {students.length > 0 && (
            <form onSubmit={generate}>
              <label htmlFor="report-student">Student</label>
              <select
                id="report-student"
                required
                value={studentId}
                disabled={!!busy}
                onChange={(e) => {
                  setStudentId(e.target.value);
                  setReport(null);
                }}
              >
                <option value="">Select student</option>
                {students.map((s) => (
                  <option key={s.student_id} value={s.student_id}>
                    {s.student_id} - {s.student_name} | Cohort {s.batch || 'unknown'}
                  </option>
                ))}
              </select>
              <button disabled={!!busy || !studentId}>
                {busy === 'generate' ? 'Generating...' : 'Generate and preview'}
              </button>
            </form>
          )}
          <p className="report-help">
            Thresholds and retake rules come from the saved curriculum policy. The default report
            covers the entire available academic history.
          </p>
        </section>
        {busy && (
          <p role="status">{busy === 'download' ? 'Preparing PDF...' : 'Loading report data...'}</p>
        )}
        {error && (
          <p role="alert" className="report-error">
            {error}
          </p>
        )}
        {searched && !students.length && (
          <p role="status">No accessible students match this search.</p>
        )}
        {['admin', 'superadmin'].includes(role) && <ProgressConfigurationPanel />}
        {report && (
          <article className="report-preview">
            <div className="report-heading">
              <div>
                <p className="report-eyebrow">Confidential academic record</p>
                <h2>{report.student.studentName}</h2>
                <p>{report.student.studentId}</p>
              </div>
              <button onClick={download} disabled={!!busy}>
                {busy === 'download' ? 'Downloading...' : 'Download full PDF'}
              </button>
            </div>
            <dl className="report-metadata">
              {Object.entries({
                University: text(report.student.university),
                Programme: text(report.student.programme),
                Curriculum: text(report.student.curriculum),
                Cohort: text(report.student.cohort),
                'Current academic status': label(report.student.academicStatus),
                'Generated at': new Date(report.generatedAt).toLocaleString(),
                'Policy version': text(report.policy.policy_version),
                'Retake policy': text(report.policy.retake_policy),
              }).map(([key, value]) => (
                <div key={key}>
                  <dt>{key}</dt>
                  <dd>{value}</dd>
                </div>
              ))}
            </dl>
            <p className="report-help">Report reference: {report.reference}</p>
            <div className="report-stats">
              <div>
                <strong>{number(report.summary.creditsAttempted)}</strong>
                <span>Credits attempted, including repeats</span>
              </div>
              <div>
                <strong>{number(report.summary.creditsCompleted)}</strong>
                <span>Credits completed</span>
              </div>
              <div>
                <strong>
                  {report.summary.losAchieved} / {report.summary.losNotAchieved}
                </strong>
                <span>LOs achieved / not achieved</span>
              </div>
              <div>
                <strong>
                  {report.summary.posAchieved} / {report.summary.posNotAchieved}
                </strong>
                <span>POs achieved / not achieved</span>
              </div>
            </div>
            <p>
              Academic completion: <Status value={report.summary.academicStatus} />
            </p>
            <p>
              Overall PO attainment: <Status value={report.summary.poStatus} />
            </p>
            <p>
              Data completeness: <strong>{report.summary.completeness}</strong> | GPA:{' '}
              {number(report.summary.gpa)}
            </p>
            <section className="report-controls" aria-label="Report filters">
              <h3>Explore report details</h3>
              <div className="report-filters">
                {Object.entries({
                  year: 'Academic year',
                  semester: 'Semester',
                  module: 'Module',
                  lo: 'LO',
                  po: 'PO',
                }).map(([key, title]) => (
                  <label key={key} htmlFor={`filter-${key}`}>
                    {title}
                    <select
                      id={`filter-${key}`}
                      value={filters[key]}
                      onChange={(e) => setFilters({ ...filters, [key]: e.target.value })}
                    >
                      <option value="">All</option>
                      {options[key].map(([value, name]) => (
                        <option key={value} value={value}>
                          {name}
                        </option>
                      ))}
                    </select>
                  </label>
                ))}
                <button onClick={() => setFilters(emptyFilters)}>Entire academic history</button>
              </div>
              <p className="report-help">
                Filters change detail views. Summary decisions and the PDF always use the complete
                saved history. PO details retain all contributing modules.
              </p>
            </section>
            <section>
              <h3>Semester progress</h3>
              <div className="report-table-scroll">
                <table>
                  <caption>Chronological module attempts</caption>
                  <thead>
                    <tr>
                      <th>Academic year / semester</th>
                      <th>Module</th>
                      <th>Attempt</th>
                      <th>Selected for attainment</th>
                      <th>Mark / grade</th>
                      <th>Credits</th>
                      <th>Academic result</th>
                    </tr>
                  </thead>
                  <tbody>
                    {visibleHistory.map(({ module, attempt }) => (
                      <tr key={attempt.offering}>
                        <td>
                          {attempt.academicYear} / {attempt.semester}
                        </td>
                        <td>
                          {module.moduleId}
                          <small>{module.moduleName}</small>
                        </td>
                        <td>{attempt.number}</td>
                        <td>{attempt.selected ? 'Yes' : 'No'}</td>
                        <td>
                          {number(attempt.finalMark)} / {attempt.grade || 'Not recorded'}
                        </td>
                        <td>{number(module.credits)}</td>
                        <td>{label(attempt.status)}</td>
                      </tr>
                    ))}
                    {!visibleHistory.length && (
                      <tr>
                        <td colSpan={7}>No configured module attempts in this view.</td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </section>
            <section>
              <h3>Module LO attainment</h3>
              {!visibleModules.length && <p>No configured modules in this view.</p>}
              {visibleModules.map((module) => (
                <section className="report-module" key={module.moduleId}>
                  <h4>
                    {module.moduleId} - {module.moduleName}
                  </h4>
                  <p>Selected offering: {module.selectedOffering || 'No eligible attempt'}</p>
                  <div className="report-table-scroll">
                    <table>
                      <caption>Selected-attempt LO results for {module.moduleId}</caption>
                      <thead>
                        <tr>
                          <th>LO / description</th>
                          <th>Obtained</th>
                          <th>Maximum</th>
                          <th>Attainment</th>
                          <th>Threshold</th>
                          <th>Status</th>
                          <th>Evidence</th>
                        </tr>
                      </thead>
                      <tbody>
                        {module.los
                          .filter((lo) => !filters.lo || lo.loId === filters.lo)
                          .map((lo) => (
                            <tr key={lo.loId}>
                              <td>
                                {lo.loId} - {lo.name}
                                <small>{lo.description}</small>
                              </td>
                              <td>{number(lo.result.obtained)}</td>
                              <td>{number(lo.result.maximum)}</td>
                              <td>
                                {number(lo.result.percentage)}
                                {lo.result.percentage != null && '%'}
                              </td>
                              <td>{number(lo.result.threshold)}%</td>
                              <td>
                                <Status value={lo.result.status} />
                              </td>
                              <td>
                                <details>
                                  <summary>Evidence for {lo.loId}</summary>
                                  <Evidence marks={lo.marks} />
                                </details>
                              </td>
                            </tr>
                          ))}
                      </tbody>
                    </table>
                  </div>
                  <details>
                    <summary>All attempt evidence for {module.moduleId}</summary>
                    {module.attempts.filter(matchesPeriod).map((attempt) => (
                      <section key={attempt.offering}>
                        <h4>
                          Attempt {attempt.number}: {attempt.academicYear} / {attempt.semester} (
                          {attempt.status})
                        </h4>
                        {attempt.los
                          .filter((lo) => !filters.lo || lo.loId === filters.lo)
                          .map((lo) => (
                            <div key={lo.loId}>
                              <p>
                                {lo.loId}: <Status value={lo.result.status} />
                              </p>
                              <Evidence marks={lo.marks} />
                            </div>
                          ))}
                      </section>
                    ))}
                  </details>
                </section>
              ))}
            </section>
            <section className="report-module">
              <h3>Programme Outcome attainment</h3>
              <p className="report-help">
                PO = sum(LO percentage × mapping weight × credit weight) / sum(mapping weight ×
                credit weight). Complete unsuccessful LOs also contribute. An incomplete PO cannot
                be achieved.
              </p>
              {!visiblePos.length && <p>No Programme Outcomes are configured in this view.</p>}
              {visiblePos.map((po) => (
                <section className="report-lo" key={po.poId}>
                  <h4>
                    {po.code} - {po.description}
                  </h4>
                  <p>
                    Attainment: {number(po.calculation.result.percentage)}
                    {po.calculation.result.percentage != null && '%'} | Threshold:{' '}
                    {number(po.calculation.result.threshold)}% | Evidence:{' '}
                    {po.calculation.result.evidenceCount} / minimum {po.minimumEvidence} |{' '}
                    {po.required ? 'Required' : 'Optional'}{' '}
                    <Status value={po.calculation.result.status} />
                  </p>
                  <details>
                    <summary>Calculation and evidence for {po.code}</summary>
                    <p>
                      Numerator {number(po.calculation.numerator)} / denominator{' '}
                      {number(po.calculation.denominator)}. Values are rounded for display; status
                      uses unrounded calculations.
                    </p>
                    {po.calculation.contributions.map((c) => (
                      <section key={`${c.evidence.module}-${c.evidence.lo}`}>
                        <h4>
                          {c.evidence.module} / {c.evidence.lo}
                        </h4>
                        <p>
                          LO: {number(c.evidence.percentage)}% | Mapping weight:{' '}
                          {number(c.evidence.mappingWeight)} | Credit weight:{' '}
                          {number(c.evidence.creditWeight)} | Weighted value:{' '}
                          {number(c.weightedValue)} | Contribution: {number(c.percentagePoints)}{' '}
                          percentage points
                        </p>
                        <Status value={c.evidence.status} />
                        <Evidence marks={c.evidence.marks} />
                      </section>
                    ))}
                  </details>
                </section>
              ))}
            </section>
            <section className="report-notes">
              <h3>Strongest and weakest outcome areas</h3>
              {ranked.length ? (
                <p>
                  Lowest available PO: {ranked[0].code}. Highest available PO:{' '}
                  {ranked[ranked.length - 1].code}. Check evidence status before interpreting
                  provisional results.
                </p>
              ) : (
                <p>Insufficient evidence to rank outcomes.</p>
              )}
            </section>
            <section className="report-notes">
              <h3>Data-completeness warnings</h3>
              {report.warnings.length ? (
                <ul>
                  {report.warnings.map((warning) => (
                    <li key={warning}>{warning}</li>
                  ))}
                </ul>
              ) : (
                <p>No data-completeness warnings.</p>
              )}
              {!!report.unassignedEvidence.length && (
                <details>
                  <summary>Unassigned historical question marks</summary>
                  <div className="report-table-scroll">
                    <table>
                      <caption>Evidence excluded until enrolments are configured</caption>
                      <thead>
                        <tr>
                          <th>Period</th>
                          <th>Module / LO</th>
                          <th>Assessment / question</th>
                          <th>Obtained / maximum</th>
                        </tr>
                      </thead>
                      <tbody>
                        {report.unassignedEvidence.map((e, i) => (
                          <tr key={i}>
                            <td>
                              {e.academic_year} / {e.semester}
                            </td>
                            <td>
                              {e.module_id} / {e.lo_id}
                            </td>
                            <td>
                              {e.assessment} / {e.question}
                            </td>
                            <td>
                              {number(e.obtained)} / {number(e.maximum)}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </details>
              )}
            </section>
            <section className="report-notes">
              <h3>Conclusion</h3>
              <p>{report.conclusion}</p>
              <p>
                The university or authorized awarding body makes the final qualification decision.
              </p>
              <p>Authorized review: ____________________ Date: ____________________</p>
            </section>
          </article>
        )}
      </main>
      <Footer />
    </div>
  );
}
