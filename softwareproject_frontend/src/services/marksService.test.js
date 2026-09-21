import { describe, it, expect, vi, beforeEach } from 'vitest'
import axios from 'axios'
import { marksService } from './marksService'

vi.mock('axios')

describe('marksService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getModule requests the module by id', async () => {
    axios.get.mockResolvedValue({ data: { id: 'M1' } })

    await marksService.getModule('M1')

    expect(axios.get).toHaveBeenCalledWith('/api/modules/M1', {})
  })

  it('getPOAttainment posts the selected LOs, mark type, batch and threshold', async () => {
    axios.post.mockResolvedValue({ data: {} })

    await marksService.getPOAttainment({
      losIds: ['LO1', 'LO2'],
      markType: 'FINAL_EXAM',
      batch: '20',
      threshold: 50,
    })

    expect(axios.post).toHaveBeenCalledWith(
      '/api/obe/po-attainment',
      { losIds: ['LO1', 'LO2'], markType: 'FINAL_EXAM', batch: '20', threshold: 50 },
      {}
    )
  })

  it('exportMarks requests the response as a blob so the file download works', async () => {
    axios.post.mockResolvedValue({ data: new Blob() })

    await marksService.exportMarks({ losIds: ['LO1'], markType: 'FINAL_EXAM', batch: '20', threshold: 50 })

    expect(axios.post).toHaveBeenCalledWith(
      '/api/obe/export/marks',
      { losIds: ['LO1'], markType: 'FINAL_EXAM', batch: '20', threshold: 50 },
      { responseType: 'blob' }
    )
  })

  it('uploadBulk sends losIds as a comma-joined string in the multipart form data', async () => {
    axios.post.mockResolvedValue({ data: {} })
    const file = new File(['content'], 'marks.xlsx')

    await marksService.uploadBulk({ excelFile: file, losIds: ['LO1', 'LO2'], batch: '20', markType: 'FINAL_EXAM' })

    expect(axios.post).toHaveBeenCalledTimes(1)
    const [url, formData] = axios.post.mock.calls[0]
    expect(url).toBe('/api/obe/marks/upload-bulk')
    expect(formData.get('losIds')).toBe('LO1,LO2')
    expect(formData.get('batch')).toBe('20')
    expect(formData.get('markType')).toBe('FINAL_EXAM')
    expect(formData.get('excelFile')).toBe(file)
  })

  it('uploadBulk passes a single losIds string through unchanged', async () => {
    axios.post.mockResolvedValue({ data: {} })
    const file = new File(['content'], 'marks.xlsx')

    await marksService.uploadBulk({ excelFile: file, losIds: 'LO1', batch: '20', markType: 'FINAL_EXAM' })

    const [, formData] = axios.post.mock.calls[0]
    expect(formData.get('losIds')).toBe('LO1')
  })
})
