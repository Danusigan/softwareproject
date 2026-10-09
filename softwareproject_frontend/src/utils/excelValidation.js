export const EXCEL_UPLOAD_HELP = 'Excel .xlsx or .xls, maximum 5 MB. Use the downloaded template and paste values instead of formulas.'
export function excelFileError(file) {
  if (!file || file.size === 0) return 'Choose a non-empty Excel file.'
  if (!/\.xlsx?$/i.test(file.name)) return 'Only .xlsx or .xls files are allowed.'
  if (file.size > 5 * 1024 * 1024) return 'Excel files must be 5 MB or smaller.'
  return ''
}
export function validateExcelFile(file) {
  const error = excelFileError(file)
  if (error) throw new Error(error)
}
