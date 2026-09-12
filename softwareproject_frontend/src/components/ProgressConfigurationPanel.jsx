import { useState } from 'react';
import axios from 'axios';
import authService from '../services/authService';

const templates = {
  curricula: {
    code: '',
    programmeCode: '',
    programmeName: '',
    university: '',
    version: '',
    cohort: '',
    policyVersion: '',
    retakePolicy: 'OFFICIAL',
    creditWeighted: true,
    graduationConfigured: false,
    requiredCredits: null,
    minimumGpa: null,
    modules: [{ moduleId: '', credits: null, compulsory: true }],
    los: [{ loId: '', threshold: null }],
    pos: [{ poId: '', threshold: null, minimumEvidence: 1, required: true }],
  },
  'student-programmes': {
    studentId: '',
    curriculumCode: '',
    accountUsername: null,
    academicStatus: 'IN_PROGRESS',
  },
  offerings: {
    code: '',
    curriculumCode: '',
    moduleId: '',
    periodCode: '',
    academicYear: '',
    semester: '',
    startsOn: '',
    assessmentIds: [],
  },
  enrolments: {
    studentId: '',
    offeringCode: '',
    attemptNumber: 1,
    official: false,
    status: 'IN_PROGRESS',
    finalMark: null,
    grade: null,
    gradePoints: null,
  },
};

export default function ProgressConfigurationPanel() {
  const [kind, setKind] = useState('curricula');
  const [body, setBody] = useState(JSON.stringify(templates.curricula, null, 2));
  const [catalog, setCatalog] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const headers = () => ({ Authorization: `Bearer ${authService.getToken()}` });
  async function load() {
    setBusy(true);
    setError('');
    try {
      setCatalog(
        (await axios.get('/api/reports/progress/configuration', { headers: headers() })).data
      );
    } catch (e) {
      setError(e.response?.data?.message || 'Could not load configuration.');
    } finally {
      setBusy(false);
    }
  }
  async function save(event) {
    event.preventDefault();
    setError('');
    setMessage('');
    let parsed;
    try {
      parsed = JSON.parse(body);
    } catch {
      setError('Enter valid JSON. Check commas, quotes and brackets.');
      return;
    }
    setBusy(true);
    try {
      const method = ['student-programmes', 'enrolments'].includes(kind) ? 'put' : 'post';
      await axios[method](`/api/reports/progress/configuration/${kind}`, parsed, {
        headers: headers(),
      });
      setMessage('Saved. Generate a new report to use the updated configuration.');
      setCatalog(null);
    } catch (e) {
      setError(
        e.response?.data?.message ||
          'Could not save configuration. Check required fields and referenced codes.'
      );
    } finally {
      setBusy(false);
    }
  }
  return (
    <details className="report-controls configuration-panel">
      <summary>QA administrator: curriculum and academic history configuration</summary>
      <p>
        Configure in order: curriculum, student programme, module offerings, then enrolments.
        Curriculum publication preserves current LO/PO descriptions and approved positive mappings.
        Published versions and offerings are immutable.
      </p>
      <p>
        Use university-approved values. Leave graduationConfigured false until credit,
        compulsory-module and GPA rules have been approved. minimumGpa null explicitly means no GPA
        rule. Missing marks, absence, withdrawal and exemption do not count as a pass.
      </p>
      <button type="button" disabled={busy} onClick={load}>
        Load existing codes and configuration
      </button>
      {catalog && (
        <details>
          <summary>Existing modules, outcomes, assessments and saved configuration</summary>
          <pre>{JSON.stringify(catalog, null, 2)}</pre>
        </details>
      )}
      <form onSubmit={save}>
        <label htmlFor="configuration-kind">Configuration record</label>
        <select
          id="configuration-kind"
          value={kind}
          disabled={busy}
          onChange={(e) => {
            setKind(e.target.value);
            setBody(JSON.stringify(templates[e.target.value], null, 2));
            setError('');
            setMessage('');
          }}
        >
          <option value="curricula">1. Publish curriculum and policy</option>
          <option value="student-programmes">2. Assign student programme</option>
          <option value="offerings">3. Create module offering</option>
          <option value="enrolments">4. Save module attempt</option>
        </select>
        <label htmlFor="configuration-json">Configuration values (JSON)</label>
        <textarea
          id="configuration-json"
          required
          rows={20}
          value={body}
          disabled={busy}
          onChange={(e) => setBody(e.target.value)}
          spellCheck={false}
        />
        <p>
          Retake policy: OFFICIAL, LATEST_COMPLETED or BEST. Student status: IN_PROGRESS, COMPLETED
          or WITHDRAWN. Attempt status: IN_PROGRESS, PASS, FAIL, WITHDRAWN, EXEMPT or ABSENT. Dates
          use YYYY-MM-DD. Thresholds are percentages; credits and thresholds support four decimal
          places.
        </p>
        <button disabled={busy}>{busy ? 'Saving / loading...' : 'Save configuration'}</button>
      </form>
      {error && (
        <p role="alert" className="report-error">
          {error}
        </p>
      )}
      {message && <p role="status">{message}</p>}
    </details>
  );
}
