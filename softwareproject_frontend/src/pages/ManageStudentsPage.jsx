import React, { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Header from '../components/header'
import Footer from '../components/footer'
import authService from '../services/authService'
import studentService from '../services/studentService'

const parseFilename = (cd, fallback) => {
  if (!cd) return fallback
  const m = cd.match(/filename\*?=(?:UTF-8''|")?([^";]+)(?:")?/i)
  if (!m?.[1]) return fallback
  try { return decodeURIComponent(m[1].replace(/"/g, '').trim()) } catch { return m[1].replace(/"/g, '').trim() || fallback }
}
const downloadBlob = (blob, filename) => {
  const url = window.URL.createObjectURL(new Blob([blob]))
  const a = document.createElement('a')
  a.href = url; a.download = filename
  document.body.appendChild(a); a.click(); a.remove()
  window.URL.revokeObjectURL(url)
}
const readBlobError = async (error) => {
  const p = error?.response?.data
  if (p instanceof Blob) {
    try { const t = await p.text(); const j = JSON.parse(t); return j?.message || j?.error || t } catch { return await p.text() }
  }
  return p?.message || p?.error || error?.message || 'Request failed'
}
const authHeaders = () => ({ Authorization: `Bearer ${authService.getToken()}` })

export default function ManageStudentsPage() {
  const navigate = useNavigate()
  const fileInputRef = useRef(null)

  const [students, setStudents] = useState([])
  const [batchFilter, setBatchFilter] = useState('')
  const [yearFilter, setYearFilter] = useState('')
  const [uploadFile, setUploadFile] = useState(null)
  const [busyAction, setBusyAction] = useState('')
  const [message, setMessage] = useState({ type: '', text: '' })
  const [loadingList, setLoadingList] = useState(false)

  useEffect(() => {
    const userType = localStorage.getItem('userType')
    const normalized = userType?.toLowerCase?.() || ''
    const isAdmin = normalized === 'admin' || normalized === 'superadmin' || normalized === 'super admin' || normalized === 'super-admin'
    if (!isAdmin) navigate('/')
  }, [navigate])

  const fetchStudents = async (batch = batchFilter, academicYear = yearFilter) => {
    setLoadingList(true)
    try {
      const r = await studentService.list({ batch, academicYear }, { headers: authHeaders() })
      setStudents(r.data?.data || [])
    } catch (e) {
      setMessage({ type: 'error', text: e.response?.data?.message || 'Could not load students.' })
    } finally { setLoadingList(false) }
  }

  useEffect(() => { fetchStudents() }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const handleFilter = e => { e.preventDefault(); fetchStudents() }

  const handleDownloadTemplate = async () => {
    try {
      setBusyAction('template'); setMessage({ type: '', text: '' })
      const r = await studentService.downloadTemplate({ headers: authHeaders(), responseType: 'blob' })
      downloadBlob(r.data, parseFilename(r.headers?.['content-disposition'], 'student_upload_template.xlsx'))
    } catch (e) { setMessage({ type: 'error', text: await readBlobError(e) }) }
    finally { setBusyAction('') }
  }

  const handleFileChange = e => {
    const f = e.target.files?.[0]
    if (f && !(f.name.endsWith('.xlsx') || f.name.endsWith('.xls'))) {
      setMessage({ type: 'error', text: 'Only .xlsx or .xls files are allowed.' })
      e.target.value = ''
      setUploadFile(null)
      return
    }
    setUploadFile(f || null)
  }

  const handleUpload = async () => {
    if (!uploadFile) { setMessage({ type: 'error', text: 'Select a completed Excel file first.' }); return }
    try {
      setBusyAction('upload'); setMessage({ type: '', text: '' })
      const r = await studentService.uploadStudents({ file: uploadFile }, { headers: authHeaders() })
      setMessage({ type: 'success', text: r.data?.message || 'Students imported.' })
      setUploadFile(null)
      if (fileInputRef.current) fileInputRef.current.value = ''
      fetchStudents()
    } catch (e) {
      setMessage({ type: 'error', text: e.response?.data?.message || 'Could not import the uploaded file.' })
    } finally { setBusyAction('') }
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Header />
      <div className="max-w-7xl mx-auto px-4 py-8">
        <div className="flex items-center gap-4 mb-8">
          <button
            onClick={() => navigate('/admin-dashboard')}
            className="p-2 text-gray-600 hover:bg-gray-200 rounded-lg transition-colors"
            title="Back to Dashboard"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
          </button>
          <div>
            <h1 className="text-3xl font-bold text-gray-800">Manage Students</h1>
            <p className="text-gray-600 mt-1">Bulk-import the student roster (with batch) so marks upload, PO attainment and batch reports have data to work with.</p>
          </div>
        </div>

        {message.text && (
          <div className={`mb-6 p-4 rounded-lg whitespace-pre-line ${message.type === 'success' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
            {message.text}
          </div>
        )}

        <div className="bg-white rounded-xl shadow-lg p-6 mb-8">
          <h2 className="text-xl font-bold text-gray-800 mb-1">Import students from Excel</h2>
          <p className="text-gray-500 text-sm mb-4">
            Columns: Student ID, Student Name, Email, Academic Year, Batch. Existing students (matched by Student ID) are updated;
            new ones are created. Leave a cell blank to keep an existing student&apos;s current value for that field.
          </p>
          <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
            <button
              onClick={handleDownloadTemplate}
              disabled={!!busyAction}
              className="px-4 py-2 rounded-lg border border-gray-300 text-gray-700 font-semibold hover:bg-gray-50 disabled:opacity-50"
            >
              {busyAction === 'template' ? 'Downloading…' : 'Download template'}
            </button>
            <input
              ref={fileInputRef}
              type="file"
              accept=".xlsx,.xls"
              onChange={handleFileChange}
              disabled={!!busyAction}
              className="flex-1 text-sm text-gray-600 file:mr-3 file:py-2 file:px-4 file:rounded-lg file:border-0 file:bg-indigo-50 file:text-indigo-700 file:font-semibold hover:file:bg-indigo-100"
            />
            <button
              onClick={handleUpload}
              disabled={!!busyAction || !uploadFile}
              className="px-5 py-2 rounded-lg bg-indigo-600 text-white font-semibold hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {busyAction === 'upload' ? 'Uploading…' : 'Upload'}
            </button>
          </div>
        </div>

        <div className="bg-white rounded-xl shadow-lg p-6">
          <div className="flex items-center justify-between mb-4 flex-wrap gap-3">
            <h2 className="text-xl font-bold text-gray-800">Current roster{students.length ? ` (${students.length})` : ''}</h2>
            <form onSubmit={handleFilter} className="flex items-center gap-2">
              <input
                type="text" placeholder="Filter by batch" value={batchFilter}
                onChange={e => setBatchFilter(e.target.value)}
                className="px-3 py-2 border border-gray-300 rounded-lg text-sm w-32"
              />
              <input
                type="text" placeholder="Filter by academic year" value={yearFilter}
                onChange={e => setYearFilter(e.target.value)}
                className="px-3 py-2 border border-gray-300 rounded-lg text-sm w-40"
              />
              <button type="submit" className="px-4 py-2 rounded-lg border border-gray-300 text-gray-700 text-sm font-semibold hover:bg-gray-50">
                Filter
              </button>
            </form>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 border-b">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Student ID</th>
                  <th className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Name</th>
                  <th className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Email</th>
                  <th className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Academic Year</th>
                  <th className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Batch</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {students.map(s => (
                  <tr key={s.studentId} className="hover:bg-gray-50">
                    <td className="px-6 py-3 text-sm font-medium text-gray-800">{s.studentId}</td>
                    <td className="px-6 py-3 text-sm text-gray-600">{s.studentName}</td>
                    <td className="px-6 py-3 text-sm text-gray-600">{s.email || '—'}</td>
                    <td className="px-6 py-3 text-sm text-gray-600">{s.academicYear || '—'}</td>
                    <td className="px-6 py-3 text-sm">
                      {s.batch
                        ? <span className="px-2 py-1 rounded-md bg-indigo-50 text-indigo-700 text-xs font-semibold">{s.batch}</span>
                        : <span className="px-2 py-1 rounded-md bg-amber-50 text-amber-700 text-xs font-semibold">No batch</span>}
                    </td>
                  </tr>
                ))}
                {!loadingList && !students.length && (
                  <tr><td colSpan={5} className="px-6 py-8 text-center text-gray-400">No students found. Import a roster above to get started.</td></tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
      <Footer />
    </div>
  )
}
