import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import axios from 'axios';
import StudentReportsPage from './StudentReportsPage';
import ProgressConfigurationPanel from '../components/ProgressConfigurationPanel';
vi.mock('axios', () => ({ default: { get: vi.fn(), post: vi.fn(), put: vi.fn() } }));
vi.mock('../components/header', () => ({ default: () => <div>Header</div> }));
vi.mock('../components/footer', () => ({ default: () => <div>Footer</div> }));
vi.mock('../services/authService', () => ({
  default: { getToken: () => 'test-token', getUserInfo: () => ({ userType: 'lecture' }) },
}));
const result = { obtained: 24, maximum: 30, percentage: 80, threshold: 60, status: 'ACHIEVED' };
const lo = {
  loId: 'LO1',
  name: 'Analysis',
  description: 'Analyse evidence',
  result,
  marks: [{ assessment: 'Assignment', question: 'Q1', obtained: 8, maximum: 10 }],
};
const report = {
  reference: 'saved-reference',
  generatedAt: '2026-09-12T00:00:00Z',
  student: {
    studentId: 'S1',
    studentName: 'Sample Student',
    university: 'Sample University',
    programme: 'Engineering',
    curriculum: 'C1',
    cohort: '22',
    academicStatus: 'IN_PROGRESS',
  },
  policy: { policy_version: 'v1', retake_policy: 'OFFICIAL' },
  summary: {
    creditsAttempted: 3,
    creditsCompleted: 3,
    losAchieved: 1,
    losNotAchieved: 0,
    posAchieved: 0,
    posNotAchieved: 0,
    academicStatus: 'IN_PROGRESS',
    poStatus: 'INSUFFICIENT_EVIDENCE',
    completeness: 'INCOMPLETE',
    gpa: null,
  },
  modules: [
    {
      moduleId: 'SE101',
      moduleName: 'Engineering',
      credits: 3,
      selectedOffering: 'O1',
      los: [lo],
      attempts: [
        {
          offering: 'O1',
          number: 1,
          academicYear: '2025/26',
          semester: '1',
          startsOn: '2025-01-01',
          status: 'PASS',
          selected: true,
          finalMark: 80,
          grade: 'A',
          los: [lo],
        },
      ],
    },
  ],
  pos: [
    {
      poId: 'PO1',
      code: 'PO1',
      description: 'Knowledge',
      minimumEvidence: 2,
      required: true,
      calculation: {
        numerator: 240,
        denominator: 3,
        result: {
          percentage: 80,
          threshold: 65,
          evidenceCount: 1,
          status: 'INSUFFICIENT_EVIDENCE',
        },
        contributions: [
          {
            evidence: {
              module: 'SE101',
              lo: 'LO1',
              percentage: 80,
              mappingWeight: 3,
              creditWeight: 1,
              status: 'ACHIEVED',
              marks: lo.marks,
            },
            weightedValue: 240,
            percentagePoints: 80,
          },
        ],
      },
    },
  ],
  warnings: ['Some evidence is missing.'],
  unassignedEvidence: [],
  conclusion: 'Academic study is still in progress.',
};
beforeEach(() => {
  vi.clearAllMocks();
});
afterEach(cleanup);
async function preview() {
  axios.get.mockResolvedValueOnce({
    data: [{ student_id: 'S1', student_name: 'Sample Student', batch: '22' }],
  });
  axios.post.mockResolvedValueOnce({ data: report });
  render(<StudentReportsPage />);
  fireEvent.change(screen.getByLabelText('Student index or name'), { target: { value: 'Sample' } });
  fireEvent.click(screen.getByText('Find students'));
  await screen.findByLabelText('Student');
  fireEvent.change(screen.getByLabelText('Student'), { target: { value: 'S1' } });
  fireEvent.click(screen.getByText('Generate and preview'));
  await screen.findByText('Download full PDF');
}
describe('Student academic progress reports', () => {
  it('searches by name and uses server policy with separate academic and PO status', async () => {
    await preview();
    expect(axios.get).toHaveBeenCalledWith(
      '/api/reports/progress/students',
      expect.objectContaining({ params: { q: 'Sample' } })
    );
    expect(axios.post).toHaveBeenCalledWith(
      '/api/reports/progress/students/S1/snapshots',
      {},
      expect.anything()
    );
    expect(screen.getByText('Academic study is still in progress.')).toBeInTheDocument();
    expect(screen.getAllByText('INSUFFICIENT EVIDENCE').length).toBeGreaterThan(0);
    expect(screen.queryByLabelText('LO achievement threshold (%)')).not.toBeInTheDocument();
    expect(screen.getByText('Calculation and evidence for PO1')).toBeInTheDocument();
    expect(screen.getByText('Some evidence is missing.')).toBeInTheDocument();
  });
  it('filters details and restores the entire academic history', async () => {
    await preview();
    fireEvent.change(screen.getByLabelText('Academic year'), { target: { value: '2025/26' } });
    expect(screen.getByLabelText('Academic year')).toHaveValue('2025/26');
    fireEvent.click(screen.getByText('Entire academic history'));
    expect(screen.getByLabelText('Academic year')).toHaveValue('');
    expect(screen.getByText('Academic study is still in progress.')).toBeInTheDocument();
  });
  it('downloads the exact preview snapshot without regenerating marks', async () => {
    await preview();
    axios.get.mockResolvedValueOnce({ data: new Blob(['%PDF']) });
    const create = vi.fn(() => 'blob:report');
    vi.stubGlobal('URL', { createObjectURL: create, revokeObjectURL: vi.fn() });
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});
    fireEvent.click(screen.getByText('Download full PDF'));
    await waitFor(() => expect(create).toHaveBeenCalled());
    expect(axios.get).toHaveBeenLastCalledWith(
      '/api/reports/progress/snapshots/saved-reference/pdf',
      expect.objectContaining({ responseType: 'blob' })
    );
    expect(axios.post).toHaveBeenCalledTimes(1);
    click.mockRestore();
    vi.unstubAllGlobals();
  });
  it('clears the previous report when student selection changes', async () => {
    await preview();
    fireEvent.change(screen.getByLabelText('Student'), { target: { value: '' } });
    expect(screen.queryByText('Download full PDF')).not.toBeInTheDocument();
  });
  it('shows server errors and empty search results', async () => {
    axios.get.mockRejectedValueOnce({ response: { data: { message: 'Access denied' } } });
    render(<StudentReportsPage />);
    fireEvent.click(screen.getByText('Find students'));
    expect(await screen.findByRole('alert')).toHaveTextContent('Access denied');
    axios.get.mockResolvedValueOnce({ data: [] });
    fireEvent.click(screen.getByText('Find students'));
    expect(
      await screen.findByText('No accessible students match this search.')
    ).toBeInTheDocument();
  });
  it('validates configuration JSON before submitting', async () => {
    render(<ProgressConfigurationPanel />);
    fireEvent.change(screen.getByLabelText('Configuration values (JSON)'), {
      target: { value: '{invalid' },
    });
    fireEvent.click(screen.getByText('Save configuration'));
    expect(await screen.findByRole('alert')).toHaveTextContent('Enter valid JSON');
    expect(axios.post).not.toHaveBeenCalled();
  });
});
