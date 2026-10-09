import { describe, it, expect, vi } from 'vitest'
import axios from 'axios'
import { excelFileError } from './excelValidation'
import { marksService } from '../services/marksService'
import { studentService } from '../services/studentService'
vi.mock('axios', () => ({ default: { post: vi.fn() } }))
describe('Excel uploads', () => {
  it.each(['data.xls','data.xlsx','DATA.XLSX'])('accepts supported filename %s', name => {
    expect(excelFileError(new File(['data'],name))).toBe('')
  })
  it.each([{name:'bad.csv',size:1},{name:'empty.xlsx',size:0},{name:'large.xls',size:5242881}])('rejects invalid selection %o', file => {
    expect(excelFileError(file)).not.toBe('')
  })
  it('accepts the exact 5 MB boundary', () => expect(excelFileError({name:'marks.xlsx',size:5242880})).toBe(''))
  it('does not send invalid uploads through any service', async () => {
    const file=new File(['data'],'file.txt')
    await expect(studentService.uploadStudents({file})).rejects.toThrow('Only .xlsx')
    for (const method of ['uploadBulk','uploadQuestionWise','uploadMarks'])
      await expect(marksService[method]({excelFile:file})).rejects.toThrow('Only .xlsx')
    expect(axios.post).not.toHaveBeenCalled()
  })
})
