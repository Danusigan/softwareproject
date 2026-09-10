import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import axios from 'axios'
import StudentReportsPage from './StudentReportsPage'
vi.mock('axios', () => ({ default: { get: vi.fn() } }))
vi.mock('../components/header', () => ({ default: () => <div>Header</div> }))
vi.mock('../components/footer', () => ({ default: () => <div>Footer</div> }))
vi.mock('../services/authService', () => ({ default: { getToken: () => 'test-token' } }))
const report = { studentId: 'S1', studentName: 'Sample Student', batch: '22',
  academicYear: '2025/26', generatedAt: '2026-09-10T00:00:00Z', threshold: 50, notes: [],
  scope: 'Visible modules only', modules: [{ moduleId: 'SE101', moduleName: 'Engineering',
    los: [{ loId: 'LO1', name: 'Analysis', percentage: null, threshold: 50, status: 'Pending', marks: [] }] }] }
beforeEach(() => { vi.clearAllMocks() })
afterEach(cleanup)
describe('Individual student reports', () => {
  it('loads students, previews pending evidence and clears stale results when threshold changes', async () => {
    axios.get.mockResolvedValueOnce({ data: [{ studentId: 'S1', studentName: 'Sample Student' }] })
      .mockResolvedValueOnce({ data: report })
    render(<StudentReportsPage />)
    fireEvent.change(screen.getByLabelText('Batch'), { target: { value: '22' } })
    fireEvent.click(screen.getByText('Find students'))
    await screen.findByLabelText('Student')
    fireEvent.change(screen.getByLabelText('Student'), { target: { value: 'S1' } })
    fireEvent.click(screen.getByText('Preview report'))
    await screen.findByText('Download PDF')
    expect(screen.getByText('Pending')).toBeInTheDocument()
    expect(axios.get).toHaveBeenLastCalledWith('/api/reports/students/individual',
      expect.objectContaining({ params: { studentId: 'S1', batch: '22', threshold: 50 } }))
    fireEvent.change(screen.getByLabelText('LO achievement threshold (%)'), { target: { value: '60' } })
    expect(screen.queryByText('Download PDF')).not.toBeInTheDocument()
  })
  it('shows backend errors without offering a download', async () => {
    axios.get.mockRejectedValueOnce({ response: { data: { message: 'Staff access required' } } })
    render(<StudentReportsPage />)
    fireEvent.change(screen.getByLabelText('Batch'), { target: { value: '22' } })
    fireEvent.click(screen.getByText('Find students'))
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('Staff access required'))
    expect(screen.queryByText('Download PDF')).not.toBeInTheDocument()
  })
})
