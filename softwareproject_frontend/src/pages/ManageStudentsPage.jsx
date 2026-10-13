import { excelFileError, EXCEL_UPLOAD_HELP } from '../utils/excelValidation'
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

  const fetchStudents = async () => {
    setLoadingList(true)
    try {
      const r = await studentService.list({}, { headers: authHeaders() })
      setStudents(r.data?.data || [])
    } catch (e) {
      setMessage({ type: 'error', text: e.response?.data?.message || 'Could not load students.' })
    } finally { setLoadingList(false) }
  }

  useEffect(() => { fetchStudents() }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const NO_BATCH = '__none__'
  const [selectedBatch, setSelectedBatch] = useState(null)
  const [form, setForm] = useState(null) // null = closed; { editingId?, studentId, studentName, email, academicYear, batch }
  const [formErr, setFormErr] = useState('')

  const batches = Object.entries(students.reduce((acc, s) => {
    const k = s.batch || NO_BATCH
    acc[k] = (acc[k] || 0) + 1
    return acc
  }, {})).sort(([a], [b]) => (a === NO_BATCH ? 1 : b === NO_BATCH ? -1 : a.localeCompare(b, undefined, { numeric: true })))
  const visible = selectedBatch === null ? [] : students.filter(s => (s.batch || NO_BATCH) === selectedBatch)

  const [showDeleted, setShowDeleted] = useState(false)
  const [deletedList, setDeletedList] = useState([])

  const loadDeleted = async () => {
    try {
      const r = await studentService.listDeleted({ headers: authHeaders() })
      setDeletedList(r.data?.data || [])
    } catch (e) {
      setMessage({ type: 'error', text: e.response?.data?.message || 'Could not load deleted students.' })
    }
  }
  const toggleDeleted = async () => {
    if (!showDeleted) await loadDeleted()
    setShowDeleted(v => !v)
  }
  const handleRestore = async s => {
    try {
      setBusyAction('restore'); setMessage({ type: '', text: '' })
      const r = await studentService.restore(s.studentId, { headers: authHeaders() })
      setMessage({ type: 'success', text: r.data?.message || 'Restored.' })
      await Promise.all([fetchStudents(), loadDeleted()])
    } catch (e) {
      setMessage({ type: 'error', text: e.response?.data?.message || 'Could not restore the student.' })
    } finally { setBusyAction('') }
  }

  const openAdd = () => {
    setFormErr('')
    setForm({ studentId: '', studentName: '', email: '', academicYear: '', batch: selectedBatch && selectedBatch !== NO_BATCH ? selectedBatch : '' })
  }
  const openEdit = s => {
    setFormErr('')
    setForm({ editingId: s.studentId, studentId: s.studentId, studentName: s.studentName || '', email: s.email || '', academicYear: s.academicYear || '', batch: s.batch || '' })
  }
  const saveForm = async () => {
    if (!form.studentId.trim() || !form.studentName.trim()) { setFormErr('Student ID and name are required.'); return }
    try {
      setBusyAction('save'); setFormErr('')
      const body = { studentId: form.studentId, studentName: form.studentName, email: form.email, academicYear: form.academicYear, batch: form.batch }
      const r = form.editingId
        ? await studentService.update(form.editingId, body, { headers: authHeaders() })
        : await studentService.create(body, { headers: authHeaders() })
      setMessage({ type: 'success', text: r.data?.message || 'Saved.' })
      setForm(null)
      await fetchStudents()
    } catch (e) {
      setFormErr(e.response?.data?.message || 'Could not save the student.')
    } finally { setBusyAction('') }
  }
  const handleDelete = async s => {
    if (!window.confirm(`Delete ${s.studentName} (${s.studentId})?\n\nIf this student has marks or other records they will be archived (soft-deleted) rather than removed.`)) return
    try {
      setBusyAction('delete'); setMessage({ type: '', text: '' })
      const r = await studentService.remove(s.studentId, { headers: authHeaders() })
      setMessage({ type: 'success', text: r.data?.message || 'Deleted.' })
      await fetchStudents()
      if (showDeleted) await loadDeleted()
    } catch (e) {
      setMessage({ type: 'error', text: e.response?.data?.message || 'Could not delete the student.' })
    } finally { setBusyAction('') }
  }

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
    if (f && excelFileError(f)) {
      setMessage({ type: 'error', text: excelFileError(f) })
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
          <p className="text-sm text-gray-500 mb-3">{EXCEL_UPLOAD_HELP}</p>
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
              title={EXCEL_UPLOAD_HELP} accept=".xlsx,.xls"
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

        {showDeleted && (
          <div className="bg-white rounded-xl shadow-lg p-6">
            <div className="flex items-center justify-between mb-4 flex-wrap gap-3">
              <div className="flex items-center gap-3">
                <button onClick={toggleDeleted} className="px-3 py-1.5 rounded-lg border border-gray-300 text-sm font-semibold text-gray-700 hover:bg-gray-50">
                  ← Back to students
                </button>
                <h2 className="text-xl font-bold text-gray-800">Deleted students ({deletedList.length})</h2>
              </div>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-gray-50 border-b">
                  <tr>
                    {['Student ID', 'Name', 'Batch', 'Deleted', 'Actions'].map(h => (
                      <th key={h} className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {deletedList.map(s => (
                    <tr key={s.studentId} className="hover:bg-gray-50">
                      <td className="px-6 py-3 text-sm font-medium text-gray-800">{s.studentId}</td>
                      <td className="px-6 py-3 text-sm text-gray-700">{s.studentName}</td>
                      <td className="px-6 py-3 text-sm text-gray-600">{s.batch || '—'}</td>
                      <td className="px-6 py-3 text-sm text-gray-600">{s.deletedAt ? new Date(s.deletedAt).toLocaleDateString('en-GB') : '—'}</td>
                      <td className="px-6 py-3 text-sm">
                        <button onClick={() => handleRestore(s)} disabled={!!busyAction} className="text-green-700 font-semibold hover:underline disabled:opacity-50">Restore</button>
                      </td>
                    </tr>
                  ))}
                  {!deletedList.length && (
                    <tr><td colSpan={5} className="px-6 py-8 text-center text-gray-400">No deleted students.</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {!showDeleted && <div className="bg-white rounded-xl shadow-lg p-6">
          <div className="flex items-center justify-between mb-4 flex-wrap gap-3">
            {selectedBatch === null ? (
              <h2 className="text-xl font-bold text-gray-800">Batches{batches.length ? ` (${batches.length})` : ''}</h2>
            ) : (
              <div className="flex items-center gap-3">
                <button onClick={() => setSelectedBatch(null)} className="px-3 py-1.5 rounded-lg border border-gray-300 text-sm font-semibold text-gray-700 hover:bg-gray-50">
                  ← All batches
                </button>
                <h2 className="text-xl font-bold text-gray-800">
                  {selectedBatch === NO_BATCH ? 'No batch' : `Batch ${selectedBatch}`} ({visible.length})
                </h2>
              </div>
            )}
            <div className="flex items-center gap-2">
              <button onClick={toggleDeleted} className="px-4 py-2 rounded-lg border border-gray-300 text-gray-700 text-sm font-semibold hover:bg-gray-50">
                Deleted students
              </button>
              <button onClick={openAdd} className="px-4 py-2 rounded-lg bg-orange-500 text-white text-sm font-semibold hover:bg-orange-600">
                + Add student
              </button>
            </div>
          </div>

          {selectedBatch === null ? (
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              {batches.map(([b, count]) => (
                <button key={b} type="button" onClick={() => setSelectedBatch(b)}
                  className="text-left p-5 rounded-xl border border-gray-200 hover:border-indigo-400 hover:bg-indigo-50 transition-colors">
                  <p className="text-2xl font-bold text-indigo-700">{b === NO_BATCH ? 'No batch' : `Batch ${b}`}</p>
                  <p className="text-sm text-gray-500 mt-1">{count} student{count > 1 ? 's' : ''}</p>
                </button>
              ))}
              {!loadingList && !batches.length && (
                <p className="text-gray-400 col-span-full text-center py-8">No students yet. Import a roster above or add a student.</p>
              )}
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead className="bg-gray-50 border-b">
                  <tr>
                    {['Student ID', 'Name', 'Email', 'Academic Year', 'Actions'].map(h => (
                      <th key={h} className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {visible.map(s => (
                    <tr key={s.studentId} className="hover:bg-gray-50">
                      <td className="px-6 py-3 text-sm font-medium text-gray-800">{s.studentId}</td>
                      <td className="px-6 py-3 text-sm">
                        <button type="button" onClick={() => navigate(`/student-profile?id=${encodeURIComponent(s.studentId)}`)}
                          className="text-blue-600 hover:text-blue-800 hover:underline text-left">
                          {s.studentName}
                        </button>
                      </td>
                      <td className="px-6 py-3 text-sm text-gray-600">{s.email || '—'}</td>
                      <td className="px-6 py-3 text-sm text-gray-600">{s.academicYear || '—'}</td>
                      <td className="px-6 py-3 text-sm whitespace-nowrap">
                        <button onClick={() => openEdit(s)} disabled={!!busyAction} className="text-indigo-600 hover:underline mr-4 disabled:opacity-50">Edit</button>
                        <button onClick={() => handleDelete(s)} disabled={!!busyAction} className="text-red-600 hover:underline disabled:opacity-50">Delete</button>
                      </td>
                    </tr>
                  ))}
                  {!visible.length && (
                    <tr><td colSpan={5} className="px-6 py-8 text-center text-gray-400">No students in this batch.</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          )}
        </div>}

        {form && (
          <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50" onClick={() => setForm(null)}>
            <div className="bg-white rounded-xl p-6 w-full max-w-md" onClick={e => e.stopPropagation()}>
              <h3 className="text-lg font-bold mb-3">{form.editingId ? 'Edit student' : 'Add student'}</h3>
              {[
                ['studentId', 'Student ID (e.g. EG/2024/6555)', form.editingId ? true : false],
                ['studentName', 'Student name', false],
                ['email', 'Email', false],
                ['academicYear', 'Academic year', false],
                ['batch', 'Batch (e.g. 24)', false],
              ].map(([key, label, locked]) => (
                <input key={key} value={form[key]} disabled={locked}
                  onChange={e => setForm({ ...form, [key]: e.target.value })}
                  placeholder={label} aria-label={label}
                  className="w-full border border-gray-300 rounded-lg p-3 text-sm mb-3 disabled:bg-gray-100 disabled:text-gray-500" />
              ))}
              {formErr && <p className="text-sm text-red-600 mb-2">{formErr}</p>}
              <div className="flex justify-end gap-2 mt-2">
                <button onClick={() => setForm(null)} className="px-4 py-2 rounded-lg border border-gray-300 text-sm">Cancel</button>
                <button onClick={saveForm} disabled={busyAction === 'save'}
                  className="px-4 py-2 rounded-lg bg-orange-500 text-white text-sm font-semibold disabled:opacity-50">
                  {busyAction === 'save' ? 'Saving…' : 'Save'}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
      <Footer />
    </div>
  )
}
