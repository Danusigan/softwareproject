import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import MarksWorkbenchPage from './MarksWorkbenchPage'
import authService from '../services/authService'
import marksService from '../services/marksService'
import cqiService from '../services/cqiService'

vi.mock('../services/marksService', () => ({
  default: {
    getModule: vi.fn(),
    getModuleLos: vi.fn(),
  },
}))

vi.mock('../services/cqiService', () => ({
  default: {
    getModuleHistory: vi.fn(),
  },
}))

/**
 * Smoke tests for the Marks Workbench page — the primary bulk-upload workflow page
 * (see Project _init.md's "Current End-to-End Workflow"). These cover the initial
 * module/LO load only: loading state, the happy path landing on batch selection,
 * and the "module not found" / load-failure states. The upload/export sub-flows are
 * exercised by the ExcelImportService and marksService unit tests instead — this test
 * exists to catch a regression in the page's own data-loading wiring.
 */
function renderPage(moduleId = 'MOD1') {
  return render(
    <MemoryRouter initialEntries={[`/marks-workbench/${moduleId}`]}>
      <Routes>
        <Route path="/marks-workbench/:moduleId" element={<MarksWorkbenchPage />} />
      </Routes>
    </MemoryRouter>
  )
}

describe('MarksWorkbenchPage', () => {
  beforeEach(() => {
    localStorage.clear()
    authService.storeLogin('fake-jwt', 'lecturer1', 'lecture')
    global.fetch = vi.fn().mockResolvedValue({
      json: () => Promise.resolve({ data: [] }),
    })
    cqiService.getModuleHistory.mockResolvedValue({ data: { data: [] } })
  })

  afterEach(() => {
    localStorage.clear()
    vi.restoreAllMocks()
  })

  it('shows a loading state before the module data arrives', () => {
    marksService.getModule.mockReturnValue(new Promise(() => {})) // never resolves
    marksService.getModuleLos.mockReturnValue(new Promise(() => {}))

    renderPage()

    expect(screen.getByText('Loading…')).toBeInTheDocument()
  })

  it('loads the module and lands on batch selection once data arrives', async () => {
    marksService.getModule.mockResolvedValue({ data: { moduleName: 'Software Engineering' } })
    marksService.getModuleLos.mockResolvedValue({ data: [{ id: 'LO001', name: 'LO 1' }] })

    renderPage()

    await waitFor(() => expect(screen.getByText('Software Engineering')).toBeInTheDocument())
    expect(screen.getByText('Select a Batch')).toBeInTheDocument()
  })

  it('shows "Module not found" when the module lookup returns no data', async () => {
    marksService.getModule.mockResolvedValue({ data: null })
    marksService.getModuleLos.mockResolvedValue({ data: [] })

    renderPage()

    await waitFor(() => expect(screen.getByText('Module not found')).toBeInTheDocument())
  })

  it('shows an error message when the module fails to load', async () => {
    marksService.getModule.mockRejectedValue({ response: { data: { message: 'Module fetch failed' } } })
    marksService.getModuleLos.mockResolvedValue({ data: [] })

    renderPage()

    await waitFor(() => expect(screen.getByText('Module fetch failed')).toBeInTheDocument())
  })
})
