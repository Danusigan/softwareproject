import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import axios from 'axios'
import BatchReportsPage from './BatchReportsPage'
vi.mock('axios', () => ({ default: { get: vi.fn() } }))
vi.mock('../components/header', () => ({ default: () => <div>Header</div> }))
vi.mock('../components/footer', () => ({ default: () => <div>Footer</div> }))
vi.mock('../services/authService', () => ({ default: { getToken: () => 'test-token' } }))
const report = { batch: '22', generatedAt: '2026-09-10T00:00:00Z', studentThreshold: 50, loTarget: 70, poTarget: 70,
  scope: 'Selected modules only', studentsWithRecords: 2, notes: [], pos: [],
  modules: [{ moduleId: 'SE101', moduleName: 'Engineering', studentsWithRecords: 2, achievedLos: 0, totalLos: 1, achievedLoPercent: 0, status: 'Pending',
    los: [{ loId: 'LO1', name: 'Analysis', studentsWithRecords: 2, assessed: 1, achieved: 1, belowThreshold: 0, pending: 1,
      achievementPercent: 100, coveragePercent: 50, target: 70, status: 'Pending' }] }] }
beforeEach(() => { vi.clearAllMocks() })
afterEach(() => { cleanup(); vi.restoreAllMocks() })
async function preview() {
  axios.get.mockResolvedValueOnce({ data: [{ moduleId: 'SE101', moduleName: 'Engineering' }] }).mockResolvedValueOnce({ data: report })
  render(<BatchReportsPage />)
  fireEvent.change(screen.getByLabelText('Batch'), { target: { value: '22' } })
  fireEvent.click(screen.getByText('Find modules'))
  await screen.findByText('Preview batch report')
  fireEvent.click(screen.getByText('Preview batch report'))
  await screen.findByText('Download batch PDF')
}
describe('Batch reports', () => {
  it('preserves provisional status and invalidates stale preview when a target changes', async () => {
    await preview()
    expect(screen.getAllByText('Pending').length).toBeGreaterThan(0)
    const params = axios.get.mock.calls[1][1].params
    expect(params.getAll('moduleIds')).toEqual(['SE101'])
    expect(params.get('loTarget')).toBe('70')
    fireEvent.change(screen.getByLabelText('Batch LO target (%)'), { target: { value: '80' } })
    expect(screen.queryByText('Download batch PDF')).not.toBeInTheDocument()
  })
  it('downloads the selected batch and module scope as PDF', async () => {
    URL.createObjectURL = vi.fn(() => 'blob:batch-report')
    URL.revokeObjectURL = vi.fn()
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    await preview()
    axios.get.mockResolvedValueOnce({ data: new Blob(['%PDF-1.5'], { type: 'application/pdf' }) })
    fireEvent.click(screen.getByText('Download batch PDF'))
    await waitFor(() => expect(click).toHaveBeenCalled())
    const options = axios.get.mock.calls[2][1]
    expect(options.params.get('format')).toBe('pdf')
    expect(options.params.getAll('moduleIds')).toEqual(['SE101'])
    expect(options.params.get('batch')).toBe('22')
    expect(options.responseType).toBe('blob')
  })
  it('shows empty and forbidden batch results without download buttons', async () => {
    axios.get.mockResolvedValueOnce({ data: [] }).mockRejectedValueOnce({ response: { data: { message: 'Staff access required' } } })
    render(<BatchReportsPage />)
    fireEvent.change(screen.getByLabelText('Batch'), { target: { value: '22' } })
    fireEvent.click(screen.getByText('Find modules'))
    await screen.findByText('No accessible modules found for batch 22.')
    fireEvent.click(screen.getByText('Find modules'))
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Staff access required'))
    expect(screen.queryByText('Download batch PDF')).not.toBeInTheDocument()
  })
})
