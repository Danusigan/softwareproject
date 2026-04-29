import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import Header from '../components/header'
import Footer from '../components/footer'
import authService from '../services/authService'
import marksService from '../services/marksService'

const markTypeOptions = [
  { value: 'FINAL_EXAM', label: 'Final Exam', description: 'Use for examination-focused marks.' },
  { value: 'ASSIGNMENT', label: 'Assignment', description: 'Use for coursework or continuous assessment.' },
]

const batchOptions = ['20', '21', '22', '23', '24', '25', '26']

const getDefaultBatch = () => new Date().getFullYear().toString().slice(-2)

const parseFilename = (contentDisposition, fallbackName) => {
  if (!contentDisposition) return fallbackName

  const match = contentDisposition.match(/filename\*?=(?:UTF-8''|")?([^";]+)(?:")?/i)
  if (!match?.[1]) return fallbackName

  try {
    return decodeURIComponent(match[1].replace(/"/g, '').trim())
  } catch {
    return match[1].replace(/"/g, '').trim() || fallbackName
  }
}

const downloadBlob = (blob, filename) => {
  const url = window.URL.createObjectURL(new Blob([blob]))
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(url)
}

const readBlobError = async (error) => {
  const payload = error?.response?.data

  if (payload instanceof Blob) {
    try {
      const text = await payload.text()
      const parsed = JSON.parse(text)
      if (parsed?.error) return parsed.error
      return parsed.message || text
    } catch {
      return await payload.text()
    }
  }

  return payload?.message || payload?.error || error?.message || 'Request failed'
}

export default function MarksWorkbenchPage() {
  const { moduleId } = useParams()
  const navigate = useNavigate()
  const fileInputRef = useRef(null)

  const [moduleData, setModuleData] = useState(null)
  const [los, setLos] = useState([])
  const [selectedLosIds, setSelectedLosIds] = useState([])
  const [batch, setBatch] = useState(getDefaultBatch())
  const [markType, setMarkType] = useState('FINAL_EXAM')
  const [threshold, setThreshold] = useState(50)
  const [uploadFile, setUploadFile] = useState(null)
  const [poAttainment, setPOAttainment] = useState(null)
  const [dragActive, setDragActive] = useState(false)
  const batchInputId = 'marks-workbench-batch'
  const markTypeInputId = 'marks-workbench-mark-type'
  const thresholdInputId = 'marks-workbench-threshold'
  const fileInputId = 'marks-workbench-upload'

  const [loading, setLoading] = useState(true)
  const [busyAction, setBusyAction] = useState('')
  const [message, setMessage] = useState({ type: '', text: '' })

  useEffect(() => {
    if (!moduleId) return

    const fetchData = async () => {
      try {
        setLoading(true)
        setMessage({ type: '', text: '' })

        const token = authService.getToken()
        const headers = token ? { Authorization: `Bearer ${token}` } : undefined

        const [moduleResponse, losResponse] = await Promise.all([
          marksService.getModule(moduleId, { headers }),
          marksService.getModuleLos(moduleId, { headers }),
        ])

        setModuleData(moduleResponse.data?.data || moduleResponse.data || null)

        const losData = losResponse.data?.data || losResponse.data || []
        setLos(Array.isArray(losData) ? losData : [])
        setSelectedLosIds(Array.isArray(losData) ? losData.map((lo) => lo.id) : [])
      } catch (error) {
        setMessage({
          type: 'error',
          text: error.response?.data?.message || 'Failed to load marks workflow.',
        })
      } finally {
        setLoading(false)
      }
    }

    fetchData()
  }, [moduleId])

  const selectedLos = useMemo(
    () => los.filter((lo) => selectedLosIds.includes(lo.id)),
    [los, selectedLosIds]
  )

  const selectedLosCount = selectedLos.length

  const toggleLo = (loId) => {
    setSelectedLosIds((current) => {
      if (current.includes(loId)) {
        return current.filter((id) => id !== loId)
      }

      return [...current, loId]
    })
  }

  const selectAllLos = () => {
    setSelectedLosIds(los.map((lo) => lo.id))
  }

  const clearSelection = () => {
    setSelectedLosIds([])
  }

  const resetFile = () => {
    setUploadFile(null)
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  const handleDrag = (event) => {
    event.preventDefault()
    event.stopPropagation()

    if (event.type === 'dragenter' || event.type === 'dragover') {
      setDragActive(true)
    } else if (event.type === 'dragleave') {
      setDragActive(false)
    }
  }

  const handleDrop = (event) => {
    event.preventDefault()
    event.stopPropagation()
    setDragActive(false)

    const droppedFile = event.dataTransfer.files?.[0]
    if (!droppedFile) return

    if (droppedFile.name.endsWith('.xlsx') || droppedFile.name.endsWith('.xls')) {
      setUploadFile(droppedFile)
      setMessage({ type: '', text: '' })
    } else {
      setMessage({ type: 'error', text: 'Please choose an Excel file with .xlsx or .xls extension.' })
    }
  }

  const handleFileChange = (event) => {
    const nextFile = event.target.files?.[0]
    if (!nextFile) return

    if (nextFile.name.endsWith('.xlsx') || nextFile.name.endsWith('.xls')) {
      setUploadFile(nextFile)
      setMessage({ type: '', text: '' })
    } else {
      setMessage({ type: 'error', text: 'Please choose an Excel file with .xlsx or .xls extension.' })
    }
  }

  const validateWorkflow = () => {
    if (!selectedLosCount) {
      setMessage({ type: 'error', text: 'Select at least one learning outcome.' })
      return false
    }

    if (!batch.trim()) {
      setMessage({ type: 'error', text: 'Batch is required.' })
      return false
    }

    if (!markType) {
      setMessage({ type: 'error', text: 'Mark type is required.' })
      return false
    }

    return true
  }

  const handleDownloadTemplate = async () => {
    if (!validateWorkflow()) return

    try {
      setBusyAction('template')
      setMessage({ type: '', text: '' })

      const token = authService.getToken()
      const response = await marksService.downloadTemplate(
        {
          losIds: selectedLos.map((lo) => lo.id),
          markType,
          batch,
        },
        { headers: token ? { Authorization: `Bearer ${token}` } : undefined }
      )

      const fallbackName = `mark_template_${batch}_${markType.toLowerCase()}.xlsx`
      const filename = parseFilename(response.headers?.['content-disposition'], fallbackName)
      downloadBlob(response.data, filename)
      setMessage({ type: 'success', text: 'Template downloaded successfully.' })
    } catch (error) {
      setMessage({ type: 'error', text: await readBlobError(error) })
    } finally {
      setBusyAction('')
    }
  }

  const handleExportMarks = async () => {
    if (!validateWorkflow()) return

    const parsedThreshold = Number(threshold)
    if (!Number.isFinite(parsedThreshold) || parsedThreshold < 0 || parsedThreshold > 100) {
      setMessage({ type: 'error', text: 'Threshold must be a number between 0 and 100.' })
      return
    }

    try {
      setBusyAction('export')
      setMessage({ type: '', text: '' })

      const token = authService.getToken()
      const response = await marksService.exportMarks(
        {
          losIds: selectedLos.map((lo) => lo.id),
          markType,
          batch,
          threshold: parsedThreshold,
        },
        { headers: token ? { Authorization: `Bearer ${token}` } : undefined }
      )

      const fallbackName = `marks_report_${batch}_${markType.toLowerCase()}.xlsx`
      const filename = parseFilename(response.headers?.['content-disposition'], fallbackName)
      downloadBlob(response.data, filename)
      setMessage({ type: 'success', text: 'Excel report exported successfully.' })
    } catch (error) {
      setMessage({ type: 'error', text: await readBlobError(error) })
    } finally {
      setBusyAction('')
    }
  }

  const handleCalculatePOAttainment = async () => {
    if (!validateWorkflow()) return

    const parsedThreshold = Number(threshold)
    if (!Number.isFinite(parsedThreshold) || parsedThreshold < 0 || parsedThreshold > 100) {
      setMessage({ type: 'error', text: 'Threshold must be a number between 0 and 100.' })
      return
    }

    try {
      setBusyAction('po-attainment')
      setMessage({ type: '', text: '' })
      setPOAttainment(null)

      const token = authService.getToken()
      const response = await marksService.getPOAttainment(
        {
          losIds: selectedLos.map((lo) => lo.id),
          markType,
          batch,
          threshold: parsedThreshold,
        },
        { headers: token ? { Authorization: `Bearer ${token}` } : undefined }
      )

      const data = response.data?.data || response.data
      setPOAttainment(data)
      setMessage({ type: 'success', text: 'PO attainment calculated successfully.' })
    } catch (error) {
      setMessage({
        type: 'error',
        text: error.response?.data?.message || 'Failed to calculate PO attainment.',
      })
    } finally {
      setBusyAction('')
    }
  }

  const handleExportPOAttainment = async () => {
    if (!validateWorkflow()) return

    const parsedThreshold = Number(threshold)
    if (!Number.isFinite(parsedThreshold) || parsedThreshold < 0 || parsedThreshold > 100) {
      setMessage({ type: 'error', text: 'Threshold must be a number between 0 and 100.' })
      return
    }

    try {
      setBusyAction('po-export')
      setMessage({ type: '', text: '' })

      const token = authService.getToken()
      const response = await marksService.exportPOAttainment(
        {
          losIds: selectedLos.map((lo) => lo.id),
          markType,
          batch,
          threshold: parsedThreshold,
        },
        { headers: token ? { Authorization: `Bearer ${token}` } : undefined }
      )

      const fallbackName = `po_attainment_${batch}_${markType.toLowerCase()}.xlsx`
      const filename = parseFilename(response.headers?.['content-disposition'], fallbackName)
      downloadBlob(response.data, filename)
      setMessage({ type: 'success', text: 'PO attainment Excel exported successfully.' })
    } catch (error) {
      setMessage({ type: 'error', text: await readBlobError(error) })
    } finally {
      setBusyAction('')
    }
  }

  const handleUploadBulk = async () => {
    if (!validateWorkflow()) return

    if (!uploadFile) {
      setMessage({ type: 'error', text: 'Choose a completed Excel file before uploading.' })
      return
    }

    try {
      setBusyAction('upload')
      setMessage({ type: '', text: '' })

      const token = authService.getToken()
      const response = await marksService.uploadBulk(
        {
          excelFile: uploadFile,
          losIds: selectedLos.map((lo) => lo.id),
          batch,
          markType,
        },
        { headers: token ? { Authorization: `Bearer ${token}` } : undefined }
      )

      setMessage({
        type: 'success',
        text: response.data?.message || 'Bulk marks uploaded successfully.',
      })
      resetFile()
    } catch (error) {
      setMessage({
        type: 'error',
        text: error.response?.data?.message || error.response?.data?.error || 'Failed to upload marks.',
      })
    } finally {
      setBusyAction('')
    }
  }

  const moduleTitle = moduleData?.moduleName || moduleData?.name || 'Marks Workflow'

  return (
    <div className="min-h-screen bg-[#f8fafc] flex flex-col relative overflow-hidden">
      <Header />

      <div className="absolute top-[-10%] right-[-10%] w-[40%] h-[40%] bg-indigo-500/10 rounded-full blur-[120px] pointer-events-none" />
      <div className="absolute bottom-[-10%] left-[-10%] w-[40%] h-[40%] bg-emerald-500/10 rounded-full blur-[120px] pointer-events-none" />

      <main className="flex-1 max-w-7xl mx-auto px-6 py-12 w-full relative z-10 animate-in fade-in duration-700">
        <div className="flex flex-col lg:flex-row lg:items-end justify-between gap-6 mb-10">
          <div className="space-y-3 max-w-3xl">
            <button
              type="button"
              onClick={() => navigate(-1)}
              className="group inline-flex items-center text-slate-500 hover:text-indigo-600 font-bold transition-all duration-300"
            >
              <div className="p-2 bg-white rounded-xl shadow-sm border border-slate-100 mr-3 group-hover:bg-indigo-50 group-hover:border-indigo-100 transition-colors">
                <svg className="w-5 h-5 group-hover:-translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
                </svg>
              </div>
              Back
            </button>
            <div className="space-y-2">
              <span className="px-4 py-1.5 bg-indigo-50 text-indigo-600 rounded-full text-[10px] font-black tracking-widest uppercase inline-block">
                Bulk Excel Workflow
              </span>
              <h1 className="heading-xl bg-clip-text text-transparent bg-gradient-to-r from-slate-900 to-slate-600">
                {moduleTitle}
              </h1>
              <p className="text-slate-500 text-lg leading-relaxed max-w-2xl">
                Download a template, fill marks, upload in one step, or export the pass/fail report for the selected learning outcomes.
              </p>
            </div>
          </div>

          <div className="glass-card rounded-[2rem] p-5 min-w-[260px]">
            <div className="grid grid-cols-2 gap-4 text-center">
              <div>
                <div className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1">Selected LOs</div>
                <div className="text-3xl font-black text-slate-900">{selectedLosCount}</div>
              </div>
              <div>
                <div className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1">Batch</div>
                <div className="text-3xl font-black text-slate-900">{batch || '-'}</div>
              </div>
            </div>
          </div>
        </div>

        {message.text && (
          <div className={`mb-8 flex items-center gap-3 p-4 rounded-2xl text-sm font-bold animate-in slide-in-from-top-4 duration-300 ${message.type === 'success' ? 'bg-emerald-50 text-emerald-700' : 'bg-red-50 text-red-700'}`}>
            <svg className="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d={message.type === 'success' ? 'M5 13l4 4L19 7' : 'M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 0 0118 0z'} />
            </svg>
            {message.text}
          </div>
        )}

        {loading ? (
          <div className="glass-card rounded-[2.5rem] p-16 text-center">
            <div className="relative mx-auto w-16 h-16 mb-6">
              <div className="w-16 h-16 border-4 border-indigo-100 rounded-full" />
              <div className="w-16 h-16 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin absolute inset-0" />
            </div>
            <p className="text-slate-500 font-bold tracking-wide">Loading module and learning outcomes...</p>
          </div>
        ) : !moduleData ? (
          <div className="glass-card rounded-[2.5rem] p-12 text-center border-red-100">
            <h2 className="heading-lg mb-3">Module not found</h2>
            <p className="text-slate-500 mb-8">The selected module could not be loaded.</p>
            <button type="button" onClick={() => navigate(-1)} className="btn-primary">
              Go back
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-[1.3fr_0.9fr] gap-8 items-start">
            <div className="space-y-8">
              <section className="glass-card rounded-[2.5rem] p-8 border-slate-100">
                <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
                  <div>
                    <span className="text-[10px] font-black text-indigo-600 uppercase tracking-[0.2em] mb-2 block">Learning Outcomes</span>
                    <h2 className="heading-lg">Select the columns for the Excel workflow</h2>
                  </div>
                  <div className="flex gap-3">
                    <button type="button" onClick={selectAllLos} className="btn-secondary text-xs font-black uppercase tracking-widest py-3">
                      Select all
                    </button>
                    <button type="button" onClick={clearSelection} className="px-4 py-3 rounded-xl bg-slate-100 text-slate-600 text-xs font-black uppercase tracking-widest hover:bg-slate-200 transition-colors">
                      Clear
                    </button>
                  </div>
                </div>

                <p className="text-sm text-slate-500 mb-5">
                  The order of the selected LOs is the same order used in the template, export, and bulk upload.
                </p>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {los.map((lo) => {
                    const checked = selectedLosIds.includes(lo.id)
                    return (
                      <button
                        key={lo.id}
                        type="button"
                        onClick={() => toggleLo(lo.id)}
                        className={`text-left p-4 rounded-2xl border transition-all duration-300 ${checked ? 'bg-indigo-50 border-indigo-200 shadow-sm' : 'bg-white/60 border-slate-200 hover:border-indigo-200 hover:bg-indigo-50/30'}`}
                      >
                        <div className="flex items-start gap-3">
                          <div className={`mt-0.5 w-5 h-5 rounded-md border flex items-center justify-center ${checked ? 'bg-indigo-600 border-indigo-600 text-white' : 'border-slate-300 bg-white'}`}>
                            {checked && (
                              <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M5 13l4 4L19 7" />
                              </svg>
                            )}
                          </div>
                          <div className="min-w-0 flex-1">
                            <div className="flex items-center gap-2 mb-2">
                              <span className="px-2.5 py-1 bg-slate-100 text-slate-600 rounded-lg text-[10px] font-black uppercase tracking-widest">
                                {lo.id}
                              </span>
                              {checked && <span className="text-[10px] font-black uppercase tracking-widest text-indigo-600">Selected</span>}
                            </div>
                            <p className="text-sm font-semibold text-slate-800 leading-relaxed">
                              {lo.description || lo.name || 'Learning Outcome'}
                            </p>
                          </div>
                        </div>
                      </button>
                    )
                  })}
                </div>
              </section>

              <section className="glass-card rounded-[2.5rem] p-8 border-slate-100">
                <div className="flex items-center justify-between gap-4 mb-6">
                  <div>
                    <span className="text-[10px] font-black text-indigo-600 uppercase tracking-[0.2em] mb-2 block">Workflow Summary</span>
                    <h2 className="heading-lg">Selected columns and file actions</h2>
                  </div>
                  <span className="px-3 py-1.5 bg-slate-100 text-slate-500 rounded-lg text-[10px] font-black uppercase tracking-widest">
                    {selectedLosCount} selected
                  </span>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
                  <div className="rounded-2xl border border-slate-200 bg-white/70 p-4">
                    <div className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2">Step 1</div>
                    <div className="font-semibold text-slate-800">Download template</div>
                    <p className="text-xs text-slate-500 mt-2">Generate a sheet with the selected LO headers.</p>
                  </div>
                  <div className="rounded-2xl border border-slate-200 bg-white/70 p-4">
                    <div className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2">Step 2</div>
                    <div className="font-semibold text-slate-800">Fill in Excel or export</div>
                    <p className="text-xs text-slate-500 mt-2">Open the downloaded template in Excel to enter marks, or export report from saved marks.</p>
                  </div>
                  <div className="rounded-2xl border border-slate-200 bg-white/70 p-4">
                    <div className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2">Step 3</div>
                    <div className="font-semibold text-slate-800">Upload bulk workbook</div>
                    <p className="text-xs text-slate-500 mt-2">Send the completed Excel file back to the backend.</p>
                  </div>
                </div>

                <div className="flex flex-wrap gap-3">
                  {selectedLos.map((lo, index) => (
                    <span key={lo.id} className="inline-flex items-center gap-2 px-3 py-2 rounded-xl bg-slate-100 text-slate-700 text-sm font-medium">
                      <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest">{index + 1}</span>
                      {lo.id}
                    </span>
                  ))}
                </div>
              </section>
            </div>

            <div className="space-y-8">
              <section className="glass-card rounded-[2.5rem] p-8 border-slate-100 sticky top-28">
                <div className="space-y-6">
                  <div>
                    <span className="text-[10px] font-black text-indigo-600 uppercase tracking-[0.2em] mb-2 block">Controls</span>
                    <h2 className="heading-lg">Configure the Excel workflow</h2>
                  </div>

                  <div className="space-y-3">
                    <label htmlFor={batchInputId} className="text-xs font-black text-slate-500 uppercase tracking-widest ml-1">Batch</label>
                    <input
                      id={batchInputId}
                      type="text"
                      value={batch}
                      onChange={(event) => setBatch(event.target.value)}
                      className="input-field"
                      placeholder="e.g. 22"
                    />
                    <div className="flex flex-wrap gap-2">
                      {batchOptions.map((option) => (
                        <button
                          key={option}
                          type="button"
                          onClick={() => setBatch(option)}
                          className={`px-3 py-1.5 rounded-lg text-[10px] font-black uppercase tracking-widest transition-colors ${batch === option ? 'bg-indigo-600 text-white' : 'bg-slate-100 text-slate-500 hover:bg-slate-200'}`}
                        >
                          {option}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="space-y-3">
                    <label htmlFor={markTypeInputId} className="text-xs font-black text-slate-500 uppercase tracking-widest ml-1">Mark type</label>
                    <select id={markTypeInputId} value={markType} onChange={(event) => setMarkType(event.target.value)} className="input-field bg-white">
                      {markTypeOptions.map((option) => (
                        <option key={option.value} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                    <p className="text-xs text-slate-500 leading-relaxed">
                      {markTypeOptions.find((option) => option.value === markType)?.description}
                    </p>
                  </div>

                  <div className="space-y-3">
                    <label htmlFor={thresholdInputId} className="text-xs font-black text-slate-500 uppercase tracking-widest ml-1">Pass threshold</label>
                    <input
                      id={thresholdInputId}
                      type="number"
                      min="0"
                      max="100"
                      value={threshold}
                      onChange={(event) => setThreshold(event.target.value)}
                      className="input-field"
                    />
                    <p className="text-xs text-slate-500 leading-relaxed">
                      Used by the export endpoint to calculate pass and fail values. Default is 50.
                    </p>
                  </div>
                </div>
              </section>

              <section className="glass-card rounded-[2.5rem] p-8 border-slate-100">
                <div className="space-y-5">
                  <div>
                    <span className="text-[10px] font-black text-indigo-600 uppercase tracking-[0.2em] mb-2 block">Download</span>
                    <h2 className="heading-lg">Template and report</h2>
                    <p className="text-sm text-slate-500 mt-3 leading-relaxed">
                      There is no in-page table editor here. Download template, fill student marks in Excel, save the file, then upload it below.
                    </p>
                  </div>

                  <button
                    type="button"
                    onClick={handleDownloadTemplate}
                    disabled={busyAction === 'template' || !selectedLosCount}
                    className={`w-full py-4 px-6 rounded-2xl text-white font-bold shadow-lg transition-all duration-300 transform active:scale-95 flex items-center justify-center gap-3 ${busyAction === 'template' || !selectedLosCount ? 'bg-slate-300 cursor-not-allowed shadow-none' : 'bg-indigo-600 hover:bg-indigo-700 hover:shadow-indigo-200'}`}
                  >
                    {busyAction === 'template' && <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />}
                    Download template
                  </button>

                  <button
                    type="button"
                    onClick={handleExportMarks}
                    disabled={busyAction === 'export' || !selectedLosCount}
                    className={`w-full py-4 px-6 rounded-2xl border font-bold shadow-sm transition-all duration-300 transform active:scale-95 flex items-center justify-center gap-3 ${busyAction === 'export' || !selectedLosCount ? 'bg-slate-100 text-slate-400 cursor-not-allowed' : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50 hover:border-indigo-200 hover:text-indigo-700'}`}
                  >
                    {busyAction === 'export' && <span className="w-4 h-4 border-2 border-slate-300 border-t-indigo-600 rounded-full animate-spin" />}
                    Export report (saved marks)
                  </button>

                  <div className="border-t border-slate-100 pt-5 mt-3">
                    <span className="text-[10px] font-black text-emerald-600 uppercase tracking-[0.2em] mb-3 block">PO Attainment</span>
                    <p className="text-sm text-slate-500 mb-4 leading-relaxed">
                      Calculate per-student PO credits based on LO pass/fail and LO→PO mappings. If a student passes an LO, they receive 100% of the assigned PO weight.
                    </p>

                    <button
                      type="button"
                      onClick={handleCalculatePOAttainment}
                      disabled={busyAction === 'po-attainment' || !selectedLosCount}
                      className={`w-full py-4 px-6 rounded-2xl text-white font-bold shadow-lg transition-all duration-300 transform active:scale-95 flex items-center justify-center gap-3 mb-3 ${busyAction === 'po-attainment' || !selectedLosCount ? 'bg-slate-300 cursor-not-allowed shadow-none' : 'bg-emerald-600 hover:bg-emerald-700 hover:shadow-emerald-200'}`}
                    >
                      {busyAction === 'po-attainment' && <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />}
                      Calculate PO Attainment
                    </button>

                    <button
                      type="button"
                      onClick={handleExportPOAttainment}
                      disabled={busyAction === 'po-export' || !selectedLosCount}
                      className={`w-full py-4 px-6 rounded-2xl border font-bold shadow-sm transition-all duration-300 transform active:scale-95 flex items-center justify-center gap-3 ${busyAction === 'po-export' || !selectedLosCount ? 'bg-slate-100 text-slate-400 cursor-not-allowed' : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50 hover:border-emerald-200 hover:text-emerald-700'}`}
                    >
                      {busyAction === 'po-export' && <span className="w-4 h-4 border-2 border-slate-300 border-t-emerald-600 rounded-full animate-spin" />}
                      Export PO Attainment Excel
                    </button>
                  </div>
                </div>
              </section>

              <section className="glass-card rounded-[2.5rem] p-8 border-slate-100">
                <div className="space-y-5">
                  <div>
                    <span className="text-[10px] font-black text-indigo-600 uppercase tracking-[0.2em] mb-2 block">Upload</span>
                    <h2 className="heading-lg">Send completed workbook</h2>
                  </div>

                  <button
                    type="button"
                    className={`w-full group p-8 border-2 border-dashed rounded-[2rem] transition-all duration-300 flex flex-col items-center justify-center cursor-pointer ${dragActive ? 'border-indigo-500 bg-indigo-50/30' : 'border-slate-200 hover:border-indigo-400 bg-white/30'}`}
                    onDragEnter={handleDrag}
                    onDragLeave={handleDrag}
                    onDragOver={handleDrag}
                    onDrop={handleDrop}
                    onClick={() => fileInputRef.current?.click()}
                  >
                    <input ref={fileInputRef} id={fileInputId} type="file" className="hidden" onChange={handleFileChange} accept=".xlsx,.xls" />

                    <div className="w-16 h-16 bg-indigo-50 rounded-2xl flex items-center justify-center mb-5 text-indigo-600 transition-transform group-hover:scale-110 duration-300">
                      <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                      </svg>
                    </div>

                    <div className="text-center">
                      <h3 className="text-lg font-bold text-slate-800 mb-1">{uploadFile ? uploadFile.name : 'Drop your file here'}</h3>
                      <p className="text-slate-500 text-sm">{uploadFile ? `${(uploadFile.size / 1024).toFixed(1)} KB` : 'Click to browse or drag and drop an Excel file'}</p>
                    </div>
                  </button>

                  {uploadFile && (
                    <button
                      type="button"
                      onClick={resetFile}
                      className="px-4 py-2 bg-slate-100 text-slate-600 rounded-lg text-xs font-bold hover:bg-red-50 hover:text-red-500 transition-colors"
                    >
                      Clear file
                    </button>
                  )}

                  <button
                    type="button"
                    onClick={handleUploadBulk}
                    disabled={busyAction === 'upload' || !selectedLosCount || !uploadFile}
                    className={`w-full py-4 px-6 rounded-2xl text-white font-bold shadow-lg transition-all duration-300 transform active:scale-95 flex items-center justify-center gap-3 ${busyAction === 'upload' || !selectedLosCount || !uploadFile ? 'bg-slate-300 cursor-not-allowed shadow-none' : 'bg-emerald-600 hover:bg-emerald-700 hover:shadow-emerald-200'}`}
                  >
                    {busyAction === 'upload' && <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />}
                    Upload workbook
                  </button>
                </div>
              </section>
            </div>

            {/* PO Attainment Results Table - Full Width Below */}
            {poAttainment && poAttainment.poList && poAttainment.poList.length > 0 && (
              <section className="glass-card rounded-[2.5rem] p-8 border-slate-100 col-span-1 lg:col-span-2 mt-8">
                <div className="space-y-6">
                  <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                    <div>
                      <span className="text-[10px] font-black text-emerald-600 uppercase tracking-[0.2em] mb-2 block">Results</span>
                      <h2 className="heading-lg">Per-Student PO Credit Attainment</h2>
                      <p className="text-sm text-slate-500 mt-2">
                        Threshold: <strong>{poAttainment.threshold}%</strong> &middot; Students: <strong>{poAttainment.studentCount}</strong> &middot; POs: <strong>{poAttainment.poList.length}</strong>
                      </p>
                    </div>
                    <button
                      type="button"
                      onClick={() => setPOAttainment(null)}
                      className="px-4 py-2 bg-slate-100 text-slate-600 rounded-lg text-xs font-bold hover:bg-red-50 hover:text-red-500 transition-colors"
                    >
                      Clear results
                    </button>
                  </div>

                  {/* LO→PO Mapping Info */}
                  {poAttainment.loPoMappings && poAttainment.loPoMappings.length > 0 && (
                    <div className="rounded-2xl border border-slate-200 bg-white/70 p-4">
                      <div className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-3">Active LO → PO Mappings</div>
                      <div className="flex flex-wrap gap-2">
                        {poAttainment.loPoMappings.map((m, i) => (
                          <span key={i} className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-indigo-50 text-indigo-700 text-xs font-semibold">
                            {m.loId} → {m.poCode} <span className="text-[10px] text-indigo-400">(weight: {m.weight})</span>
                          </span>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Max Credits Row */}
                  <div className="rounded-2xl border border-emerald-200 bg-emerald-50/50 p-4">
                    <div className="text-[10px] font-black text-emerald-600 uppercase tracking-widest mb-2">Maximum Possible Credits</div>
                    <div className="flex flex-wrap gap-3">
                      {poAttainment.poList.map(po => (
                        <span key={po} className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-emerald-100 text-emerald-700 text-xs font-bold">
                          {po}: {poAttainment.maxCredits?.[po] || 0}
                        </span>
                      ))}
                      <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-emerald-600 text-white text-xs font-bold">
                        Total: {poAttainment.totalMaxCredit}
                      </span>
                    </div>
                  </div>

                  {/* Results Table */}
                  <div className="overflow-x-auto rounded-2xl border border-slate-200">
                    <table className="w-full text-sm">
                      <thead>
                        <tr className="bg-slate-800 text-white">
                          <th className="px-4 py-3 text-left font-bold text-xs uppercase tracking-widest">Student Index</th>
                          {poAttainment.poList.map(po => (
                            <th key={po} className="px-4 py-3 text-center font-bold text-xs uppercase tracking-widest">{po}</th>
                          ))}
                          <th className="px-4 py-3 text-center font-bold text-xs uppercase tracking-widest bg-slate-900">Total</th>
                        </tr>
                      </thead>
                      <tbody>
                        {poAttainment.students?.map((student, idx) => (
                          <tr key={student.studentId} className={idx % 2 === 0 ? 'bg-white' : 'bg-slate-50'}>
                            <td className="px-4 py-3 font-semibold text-slate-800 border-r border-slate-100">{student.studentId}</td>
                            {poAttainment.poList.map(po => {
                              const credit = student.poCredits?.[po] || 0
                              const maxCredit = poAttainment.maxCredits?.[po] || 1
                              return (
                                <td
                                  key={po}
                                  className={`px-4 py-3 text-center font-bold border-r border-slate-100 ${credit > 0
                                    ? credit >= maxCredit
                                      ? 'bg-emerald-100 text-emerald-700'
                                      : 'bg-emerald-50 text-emerald-600'
                                    : 'text-slate-400'
                                    }`}
                                >
                                  {credit}
                                </td>
                              )
                            })}
                            <td className={`px-4 py-3 text-center font-black border-l-2 border-slate-200 ${student.totalCredit > 0
                              ? student.totalCredit >= poAttainment.totalMaxCredit
                                ? 'bg-emerald-200 text-emerald-800'
                                : 'bg-emerald-50 text-emerald-700'
                              : 'text-slate-400'
                              }`}>
                              {student.totalCredit}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              </section>
            )}
          </div>
        )}
      </main>

      <Footer />
    </div>
  )
}