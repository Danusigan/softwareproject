import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import Header from '../components/header'
import Footer from '../components/footer'
import authService from '../services/authService'
import marksService from '../services/marksService'

const markTypeOptions = [
  { value: 'FINAL_EXAM', label: 'Final Exam' },
  { value: 'ASSIGNMENT', label: 'Assignment' },
]
const batchOptions = ['20', '21', '22', '23', '24', '25', '26']

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
    try { const t = await p.text(); const j = JSON.parse(t); return j?.error || j?.message || t } catch { return await p.text() }
  }
  return p?.message || p?.error || error?.message || 'Request failed'
}
const slugify = s => (s || '').replace(/\s+/g, '_')

export default function MarksWorkbenchPage() {
  const { moduleId } = useParams()
  const navigate = useNavigate()
  const fileInputRef = useRef(null)

  // ── module & LO data ─────────────────────────────────────────────────
  const [moduleData, setModuleData] = useState(null)
  const [los, setLos] = useState([])
  const [loading, setLoading] = useState(true)
  const [availableMarks, setAvailableMarks] = useState([])

  // ── batch navigation ──────────────────────────────────────────────────
  const [activeBatch, setActiveBatch] = useState(null)
  const [newBatchInput, setNewBatchInput] = useState('')
  const [newBatchThreshold, setNewBatchThreshold] = useState(50)

  // ── per-batch thresholds (persisted in localStorage) ─────────────────
  const [batchThresholds, setBatchThresholds] = useState(() => {
    try { return JSON.parse(localStorage.getItem(`marks-thresholds-${moduleId}`) || '{}') } catch { return {} }
  })
  const getThreshold = b => batchThresholds[String(b)] ?? 50
  const saveThreshold = (b, val) => {
    const next = { ...batchThresholds, [String(b)]: Number(val) }
    setBatchThresholds(next)
    localStorage.setItem(`marks-thresholds-${moduleId}`, JSON.stringify(next))
  }

  // ── upload workflow state ─────────────────────────────────────────────
  const [showUploadWorkflow, setShowUploadWorkflow] = useState(false)
  const [uploadMode, setUploadMode] = useState(null) // null | 'download-template' | 'upload-marks'
  const [editingEntry, setEditingEntry] = useState(null)
  const [assignmentLabel, setAssignmentLabel] = useState('')
  const [markType, setMarkType] = useState('FINAL_EXAM')
  const [templateTypeTab, setTemplateTypeTab] = useState('question-wise')
  const [maxMarksPerLo, setMaxMarksPerLo] = useState(100)
  const [loMaxMarks, setLoMaxMarks] = useState({}) // { [loId]: number } per-LO max marks for LO-wise template
  const [numberOfQuestions, setNumberOfQuestions] = useState(5)
  const [questionMappings, setQuestionMappings] = useState([])
  const [selectedLosIds, setSelectedLosIds] = useState([])
  const [uploadFile, setUploadFile] = useState(null)
  const [dragActive, setDragActive] = useState(false)

  // ── analytics state ───────────────────────────────────────────────────
  const [analyticsLosIds, setAnalyticsLosIds] = useState([])
  const [analyticsMarkType, setAnalyticsMarkType] = useState('FINAL_EXAM')
  const [poAttainment, setPOAttainment] = useState(null)

  // ── UI feedback ───────────────────────────────────────────────────────
  const [busyAction, setBusyAction] = useState('')
  const [message, setMessage] = useState({ type: '', text: '' })

  // ── initial load ──────────────────────────────────────────────────────
  const loadAvailableMarks = async () => {
    try {
      const token = authService.getToken()
      const res = await fetch(`http://localhost:8080/api/obe/marks/available/module/${moduleId}`, {
        headers: { Authorization: `Bearer ${token}` }
      })
      const json = await res.json()
      setAvailableMarks(json.data || [])
    } catch { /* non-critical */ }
  }

  useEffect(() => {
    if (!moduleId) return
    const init = async () => {
      try {
        setLoading(true)
        const token = authService.getToken()
        const headers = { Authorization: `Bearer ${token}` }
        const [modRes, losRes] = await Promise.all([
          marksService.getModule(moduleId, { headers }),
          marksService.getModuleLos(moduleId, { headers }),
        ])
        setModuleData(modRes.data?.data || modRes.data || null)
        const losArr = Array.isArray(losRes.data?.data) ? losRes.data.data : (Array.isArray(losRes.data) ? losRes.data : [])
        setLos(losArr)
        setSelectedLosIds(losArr.map(lo => lo.id))
        setAnalyticsLosIds(losArr.map(lo => lo.id))
        await loadAvailableMarks()
      } catch (e) {
        setMessage({ type: 'error', text: e.response?.data?.message || 'Failed to load module.' })
      } finally { setLoading(false) }
    }
    init()
  }, [moduleId])

  // ── derived selections ────────────────────────────────────────────────
  const selectedLos = useMemo(() => los.filter(lo => selectedLosIds.includes(lo.id)), [los, selectedLosIds])
  const analyticsLos = useMemo(() => los.filter(lo => analyticsLosIds.includes(lo.id)), [los, analyticsLosIds])

  useEffect(() => {
    const count = Math.max(1, Math.floor(Number(numberOfQuestions)) || 1)
    const ids = selectedLos.map(lo => lo.id)
    const fallback = ids[0] || ''
    setQuestionMappings(prev => Array.from({ length: count }, (_, i) => ({
      questionNumber: i + 1,
      loId: prev[i]?.loId && ids.includes(prev[i].loId) ? prev[i].loId : fallback,
      maxMarks: prev[i]?.maxMarks ?? 10,
    })))
  }, [numberOfQuestions, selectedLos])

  const updateQM = (idx, field, val) =>
    setQuestionMappings(prev => prev.map((it, i) => i === idx ? { ...it, [field]: val } : it))

  const toggleLo = id => setSelectedLosIds(cur => cur.includes(id) ? cur.filter(x => x !== id) : [...cur, id])
  const toggleAnalyticsLo = id => setAnalyticsLosIds(cur => cur.includes(id) ? cur.filter(x => x !== id) : [...cur, id])

  const resetFile = () => { setUploadFile(null); if (fileInputRef.current) fileInputRef.current.value = '' }

  const handleDrag = e => { e.preventDefault(); e.stopPropagation(); setDragActive(e.type === 'dragenter' || e.type === 'dragover') }
  const handleDrop = e => {
    e.preventDefault(); e.stopPropagation(); setDragActive(false)
    const f = e.dataTransfer.files?.[0]
    if (!f) return
    if (f.name.endsWith('.xlsx') || f.name.endsWith('.xls')) { setUploadFile(f); setMessage({ type: '', text: '' }) }
    else setMessage({ type: 'error', text: 'Please choose an Excel file (.xlsx or .xls).' })
  }
  const handleFileChange = e => {
    const f = e.target.files?.[0]
    if (!f) return
    if (f.name.endsWith('.xlsx') || f.name.endsWith('.xls')) { setUploadFile(f); setMessage({ type: '', text: '' }) }
    else setMessage({ type: 'error', text: 'Please choose an Excel file (.xlsx or .xls).' })
  }

  const authHeaders = () => { const t = authService.getToken(); return t ? { Authorization: `Bearer ${t}` } : undefined }

  // ── workflow open / close ─────────────────────────────────────────────
  const buildDefaultLoMaxMarks = () => los.reduce((acc, lo) => ({ ...acc, [lo.id]: 100 }), {})

  const openNewUpload = () => {
    setEditingEntry(null)
    setAssignmentLabel('')
    setMarkType('FINAL_EXAM')
    setSelectedLosIds(los.map(lo => lo.id))
    setLoMaxMarks(buildDefaultLoMaxMarks())
    setUploadMode(null)
    setMessage({ type: '', text: '' })
    resetFile()
    setShowUploadWorkflow(true)
  }
  const openEditUpload = entry => {
    setEditingEntry(entry)
    setAssignmentLabel(entry.assignmentLabel || '')
    setMarkType(entry.markType)
    setSelectedLosIds(los.map(lo => lo.id))
    setLoMaxMarks(buildDefaultLoMaxMarks())
    setUploadMode(null)
    setMessage({ type: '', text: '' })
    resetFile()
    setShowUploadWorkflow(true)
  }
  const closeUploadWorkflow = () => {
    setShowUploadWorkflow(false); setUploadMode(null); setEditingEntry(null)
    setMessage({ type: '', text: '' }); resetFile()
  }

  const validateUpload = () => {
    if (!assignmentLabel.trim()) { setMessage({ type: 'error', text: 'Assignment label is required.' }); return false }
    if (!selectedLos.length) { setMessage({ type: 'error', text: 'Select at least one LO.' }); return false }
    return true
  }

  // ── download handlers ─────────────────────────────────────────────────
  const handleDownloadQuestionMarkTemplate = async () => {
    if (!validateUpload()) return
    const qCount = Number(numberOfQuestions)
    if (!Number.isFinite(qCount) || qCount <= 0) { setMessage({ type: 'error', text: 'Number of questions must be positive.' }); return }
    const loById = new Map(selectedLos.map(lo => [lo.id, lo]))
    const payload = questionMappings.slice(0, qCount).map((m, idx) => {
      const lo = loById.get(m.loId)
      return { questionNumber: idx + 1, loId: m.loId, loName: lo?.name || lo?.description || m.loId, maxMarks: Number(m.maxMarks) }
    })
    if (payload.some(m => !m.loId || !Number.isFinite(m.maxMarks) || m.maxMarks < 0)) {
      setMessage({ type: 'error', text: 'Each question must have a valid LO and max marks ≥ 0.' }); return
    }
    try {
      setBusyAction('q-template'); setMessage({ type: '', text: '' })
      const r = await marksService.downloadQuestionMarkTemplate({
        templateId: null, numberOfQuestions: Math.floor(qCount),
        questionMappings: payload, batch: activeBatch, markType, moduleId,
        assignmentLabel: assignmentLabel.trim(),
      }, { headers: authHeaders() })
      downloadBlob(r.data, parseFilename(r.headers?.['content-disposition'], `q_template_${activeBatch}_${slugify(assignmentLabel.trim())}.xlsx`))
      setMessage({ type: 'success', text: 'Question-wise template downloaded. Fill it in then upload below.' })
    } catch (e) { setMessage({ type: 'error', text: await readBlobError(e) }) }
    finally { setBusyAction('') }
  }

  const handleDownloadLoTemplate = async () => {
    if (!validateUpload()) return
    try {
      setBusyAction('lo-template'); setMessage({ type: '', text: '' })
      // Build per-LO max marks map from state; fall back to global if missing
      const perLoMaxMarks = Object.fromEntries(
        selectedLos.map(lo => [lo.id, Number(loMaxMarks[lo.id]) || Number(maxMarksPerLo) || 100])
      )
      const r = await marksService.downloadTemplate({
        losIds: selectedLos.map(lo => lo.id), markType, batch: activeBatch,
        threshold: getThreshold(activeBatch),
        perLoMaxMarks,
        moduleId, assignmentLabel: assignmentLabel.trim(),
      }, { headers: authHeaders() })
      downloadBlob(r.data, parseFilename(r.headers?.['content-disposition'], `lo_template_${activeBatch}_${slugify(assignmentLabel.trim())}.xlsx`))
      setMessage({ type: 'success', text: 'LO-wise template downloaded. Fill in the marks and upload it.' })
    } catch (e) { setMessage({ type: 'error', text: await readBlobError(e) }) }
    finally { setBusyAction('') }
  }

  // ── single unified upload ─────────────────────────────────────────────
  const handleUploadMarks = async () => {
    if (!uploadFile) { setMessage({ type: 'error', text: 'Select a completed Excel file first.' }); return }
    try {
      setBusyAction('upload'); setMessage({ type: '', text: '' })
      const r = await marksService.uploadMarks({ excelFile: uploadFile }, { headers: authHeaders() })
      setMessage({ type: 'success', text: r.data?.message || 'Marks uploaded successfully.' })
      resetFile()
      await loadAvailableMarks()
      setTimeout(() => closeUploadWorkflow(), 1400)
    } catch (e) { setMessage({ type: 'error', text: e.response?.data?.message || e.response?.data?.error || await readBlobError(e) }) }
    finally { setBusyAction('') }
  }

  // ── delete assignment ─────────────────────────────────────────────────
  const handleDeleteAssignment = async entry => {
    const label = entry.assignmentLabel || entry.markType
    if (!window.confirm(`Delete marks for "${label}" in batch ${entry.batch}? This cannot be undone.`)) return
    try {
      setBusyAction(`del-${label}`)
      await marksService.deleteAssignmentMarks({
        moduleId, batch: String(entry.batch),
        markType: entry.markType,
        assignmentLabel: entry.assignmentLabel || '',
      }, { headers: authHeaders() })
      setMessage({ type: 'success', text: `Deleted "${label}".` })
      await loadAvailableMarks()
    } catch (e) {
      setMessage({ type: 'error', text: e.response?.data?.message || 'Delete failed.' })
    } finally { setBusyAction('') }
  }

  // ── download existing marks report ────────────────────────────────────
  const handleDownloadBatchMarks = async (batchVal, markTypeVal) => {
    try {
      setBusyAction(`dl-${batchVal}-${markTypeVal}`)
      const token = authService.getToken()
      const res = await fetch(
        `http://localhost:8080/api/obe/marks/export/module/${moduleId}?batch=${batchVal}&markType=${markTypeVal}&threshold=${getThreshold(batchVal)}`,
        { headers: { Authorization: `Bearer ${token}` } }
      )
      if (!res.ok) throw new Error('Download failed')
      const blob = await res.blob()
      downloadBlob(blob, parseFilename(res.headers.get('content-disposition'), `marks_${batchVal}_${markTypeVal.toLowerCase()}.xlsx`))
    } catch (e) { setMessage({ type: 'error', text: e.message }) }
    finally { setBusyAction('') }
  }

  // ── PO Attainment ─────────────────────────────────────────────────────
  const handleCalculatePOAttainment = async () => {
    if (!analyticsLos.length) { setMessage({ type: 'error', text: 'Select at least one LO.' }); return }
    try {
      setBusyAction('po'); setMessage({ type: '', text: '' }); setPOAttainment(null)
      const r = await marksService.getPOAttainment({ losIds: analyticsLos.map(lo => lo.id), markType: analyticsMarkType, batch: activeBatch, threshold: getThreshold(activeBatch) }, { headers: authHeaders() })
      setPOAttainment(r.data?.data || r.data)
      setMessage({ type: 'success', text: 'PO attainment calculated.' })
    } catch (e) { setMessage({ type: 'error', text: e.response?.data?.message || 'PO attainment failed.' }) }
    finally { setBusyAction('') }
  }

  const handleExportPOAttainment = async () => {
    if (!analyticsLos.length) return
    try {
      setBusyAction('po-export')
      const r = await marksService.exportPOAttainment({ losIds: analyticsLos.map(lo => lo.id), markType: analyticsMarkType, batch: activeBatch, threshold: getThreshold(activeBatch) }, { headers: authHeaders() })
      downloadBlob(r.data, parseFilename(r.headers?.['content-disposition'], `po_attainment_${activeBatch}.xlsx`))
    } catch (e) { setMessage({ type: 'error', text: await readBlobError(e) }) }
    finally { setBusyAction('') }
  }

  // ── derived data ──────────────────────────────────────────────────────
  const batchAssignments = useMemo(() => availableMarks.filter(m => String(m.batch) === String(activeBatch)), [availableMarks, activeBatch])
  const marksByBatch = useMemo(() => availableMarks.reduce((acc, m) => {
    const b = String(m.batch); if (!acc[b]) acc[b] = []; acc[b].push(m); return acc
  }, {}), [availableMarks])

  const moduleTitle = moduleData?.moduleName || moduleData?.name || 'Marks Workflow'

  // ═══════════════════════════════════════════════════════════════════════
  return (
    <div className="min-h-screen bg-[#f8fafc] flex flex-col relative overflow-hidden">
      <Header />
      <div className="absolute top-[-10%] right-[-10%] w-[40%] h-[40%] bg-indigo-500/10 rounded-full blur-[120px] pointer-events-none" />
      <div className="absolute bottom-[-10%] left-[-10%] w-[40%] h-[40%] bg-emerald-500/10 rounded-full blur-[120px] pointer-events-none" />

      <main className="flex-1 max-w-7xl mx-auto px-6 py-12 w-full relative z-10 animate-in fade-in duration-700">

        {/* Page header */}
        <div className="mb-10 space-y-3">
          <button type="button" onClick={() => navigate(-1)}
            className="group inline-flex items-center text-slate-500 hover:text-indigo-600 font-bold transition-all">
            <div className="p-2 bg-white rounded-xl shadow-sm border border-slate-100 mr-3 group-hover:bg-indigo-50 transition-colors">
              <svg className="w-5 h-5 group-hover:-translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
              </svg>
            </div>
            Back
          </button>
          <span className="px-4 py-1.5 bg-indigo-50 text-indigo-600 rounded-full text-[10px] font-black tracking-widest uppercase inline-block">Marks Management</span>
          <h1 className="heading-xl bg-clip-text text-transparent bg-gradient-to-r from-slate-900 to-slate-600">{moduleTitle}</h1>
        </div>

        {/* Global message */}
        {message.text && (
          <div className={`mb-8 flex items-center gap-3 p-4 rounded-2xl text-sm font-bold animate-in slide-in-from-top-4 duration-300 ${message.type === 'success' ? 'bg-emerald-50 text-emerald-700' : 'bg-red-50 text-red-700'}`}>
            <svg className="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d={message.type === 'success' ? 'M5 13l4 4L19 7' : 'M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z'} />
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
            <p className="text-slate-500 font-bold">Loading…</p>
          </div>
        ) : !moduleData ? (
          <div className="glass-card rounded-[2.5rem] p-12 text-center">
            <h2 className="heading-lg mb-3">Module not found</h2>
            <button type="button" onClick={() => navigate(-1)} className="btn-primary">Go back</button>
          </div>

        ) : activeBatch === null ? (
          /* ══ SCREEN 1 — Batch selection ═══════════════════════════════ */
          <div className="space-y-8 animate-in fade-in duration-500">
            <div>
              <span className="text-[10px] font-black text-indigo-600 uppercase tracking-[0.2em] mb-2 block">Marks &amp; Analytics</span>
              <h2 className="heading-xl">Select a Batch</h2>
              <p className="text-slate-500 mt-2">Click an existing batch or add a new one below.</p>
            </div>

            {Object.keys(marksByBatch).length > 0 && (
              <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                {Object.entries(marksByBatch).sort(([a],[b]) => b.localeCompare(a)).map(([batchVal, entries]) => {
                  const t = getThreshold(batchVal)
                  return (
                    <button key={batchVal} type="button"
                      onClick={() => { setActiveBatch(batchVal); setMessage({ type:'',text:'' }); setPOAttainment(null) }}
                      className="glass-card rounded-[2rem] p-6 text-left hover:shadow-lg hover:border-indigo-200 transition-all duration-300 group">
                      <div className="w-12 h-12 rounded-2xl bg-indigo-600 text-white text-lg font-black flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">{batchVal}</div>
                      <div className="font-black text-slate-800 text-lg mb-1">{batchVal}th Batch</div>
                      <div className="text-xs text-slate-500 mb-2">{entries.reduce((s,e)=>s+Number(e.markCount||0),0)} records · {entries.length} upload{entries.length!==1?'s':''}</div>
                      <div className="flex items-center gap-1.5 mb-3">
                        <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest">Threshold:</span>
                        <span className="px-2 py-0.5 bg-amber-50 text-amber-700 rounded text-[10px] font-black">{t}%</span>
                      </div>
                      <div className="flex flex-wrap gap-1">
                        {entries.map((e,i) => (
                          <span key={i} className={`px-2 py-0.5 rounded-md text-[10px] font-black uppercase tracking-widest ${e.markType==='FINAL_EXAM'?'bg-indigo-50 text-indigo-600':'bg-emerald-50 text-emerald-600'}`}>
                            {e.assignmentLabel||(e.markType==='FINAL_EXAM'?'Final Exam':'Assignment')}
                          </span>
                        ))}
                      </div>
                    </button>
                  )
                })}
              </div>
            )}

            {/* Add new batch */}
            <div className="glass-card rounded-[2rem] p-6 border-2 border-dashed border-indigo-200 bg-indigo-50/30">
              <div className="text-[10px] font-black text-indigo-500 uppercase tracking-widest mb-4">Add marks for a new batch</div>
              <div className="grid grid-cols-1 md:grid-cols-[1fr_1fr_auto] gap-4 items-end">
                <div className="space-y-2">
                  <label className="text-xs font-black text-slate-500 uppercase tracking-widest">Batch number</label>
                  <input type="text" value={newBatchInput} onChange={e => setNewBatchInput(e.target.value)}
                    onKeyDown={e => { if (e.key==='Enter'&&newBatchInput.trim()) { saveThreshold(newBatchInput.trim(),newBatchThreshold); setActiveBatch(newBatchInput.trim()); setShowUploadWorkflow(true) }}}
                    className="input-field" placeholder="e.g. 25" />
                </div>
                <div className="space-y-2">
                  <label className="text-xs font-black text-slate-500 uppercase tracking-widest">Pass threshold (%)</label>
                  <input type="number" min="0" max="100" value={newBatchThreshold} onChange={e => setNewBatchThreshold(e.target.value)} className="input-field" placeholder="50" />
                </div>
                <button type="button"
                  disabled={!newBatchInput.trim()}
                  onClick={() => { const b=newBatchInput.trim(); if(!b) return; saveThreshold(b,newBatchThreshold); setActiveBatch(b); setShowUploadWorkflow(true) }}
                  className="px-6 py-3 rounded-2xl bg-indigo-600 text-white font-bold disabled:opacity-40 hover:bg-indigo-700 transition-colors whitespace-nowrap">
                  Continue →
                </button>
              </div>
              <div className="flex flex-wrap gap-2 mt-4">
                {batchOptions.filter(b => !marksByBatch[b]).map(opt => (
                  <button key={opt} type="button" onClick={() => setNewBatchInput(opt)}
                    className={`px-3 py-1.5 rounded-lg text-[10px] font-black uppercase tracking-widest border transition-colors ${newBatchInput===opt?'bg-indigo-600 text-white border-indigo-600':'bg-white text-slate-500 border-slate-200 hover:border-indigo-400'}`}>
                    {opt}
                  </button>
                ))}
              </div>
            </div>
          </div>

        ) : !showUploadWorkflow ? (
          /* ══ SCREEN 2 — Batch detail (assignment list + analytics) ════ */
          <div className="space-y-8 animate-in fade-in duration-500">
            {/* Context bar */}
            <div className="flex items-center flex-wrap gap-4 p-4 rounded-2xl bg-indigo-50 border border-indigo-100">
              <button type="button" onClick={() => { setActiveBatch(null); setMessage({type:'',text:''}); setPOAttainment(null) }}
                className="flex items-center gap-2 text-indigo-600 font-bold text-sm hover:text-indigo-800 transition-colors">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
                </svg>
                All Batches
              </button>
              <div className="h-5 w-px bg-indigo-200" />
              <span className="text-[10px] font-black text-indigo-400 uppercase tracking-widest">Batch</span>
              <span className="px-3 py-1 bg-indigo-600 text-white rounded-lg text-sm font-black">{activeBatch}</span>
              <div className="ml-auto flex items-center gap-3">
                <span className="text-xs text-slate-600 font-bold">Pass threshold:</span>
                <input type="number" min="0" max="100"
                  value={getThreshold(activeBatch)}
                  onChange={e => saveThreshold(activeBatch, e.target.value)}
                  className="w-20 text-center text-sm font-black border border-indigo-200 rounded-xl px-2 py-1.5 bg-white focus:outline-none focus:ring-2 focus:ring-indigo-300" />
                <span className="text-xs text-slate-400">%</span>
              </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-[1.4fr_1fr] gap-8 items-start">
              {/* Left — assignment list */}
              <div className="space-y-5">
                <div className="flex items-center justify-between">
                  <div>
                    <span className="text-[10px] font-black text-indigo-600 uppercase tracking-[0.2em] mb-1 block">Uploaded Marks</span>
                    <h2 className="heading-lg">Batch {activeBatch} Assignments</h2>
                  </div>
                  <button type="button" onClick={openNewUpload}
                    className="flex items-center gap-2 px-5 py-3 rounded-2xl bg-indigo-600 text-white font-bold hover:bg-indigo-700 transition-colors shadow-lg shadow-indigo-600/20">
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M12 4v16m8-8H4" />
                    </svg>
                    Upload New Marks
                  </button>
                </div>

                {batchAssignments.length === 0 ? (
                  <div className="glass-card rounded-[2rem] p-12 text-center border-2 border-dashed border-slate-200">
                    <div className="w-16 h-16 bg-indigo-50 rounded-2xl flex items-center justify-center mx-auto mb-4">
                      <svg className="w-8 h-8 text-indigo-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                      </svg>
                    </div>
                    <h3 className="font-black text-slate-700 mb-2">No marks yet</h3>
                    <p className="text-slate-500 text-sm mb-4">Click "Upload New Marks" to get started.</p>
                    <button type="button" onClick={openNewUpload} className="px-6 py-3 rounded-2xl bg-indigo-600 text-white font-bold hover:bg-indigo-700 transition-colors">
                      Upload New Marks
                    </button>
                  </div>
                ) : (
                  <div className="space-y-3">
                    {batchAssignments.map((entry, idx) => {
                      const label = entry.assignmentLabel||(entry.markType==='FINAL_EXAM'?'Final Exam':'Assignment')
                      const isFinal = entry.markType === 'FINAL_EXAM'
                      const delKey = `del-${label}`
                      return (
                        <div key={idx} className="glass-card rounded-2xl p-5 flex items-center gap-4">
                          <div className={`w-12 h-12 rounded-2xl flex items-center justify-center flex-shrink-0 ${isFinal?'bg-indigo-100 text-indigo-600':'bg-emerald-100 text-emerald-600'}`}>
                            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                            </svg>
                          </div>
                          <div className="flex-1 min-w-0">
                            <div className="font-black text-slate-800 text-base">{label}</div>
                            <div className="flex items-center gap-2 mt-1 flex-wrap">
                              <span className={`px-2 py-0.5 rounded-md text-[10px] font-black uppercase tracking-widest ${isFinal?'bg-indigo-50 text-indigo-600':'bg-emerald-50 text-emerald-600'}`}>
                                {isFinal?'Final Exam':'Assignment'}
                              </span>
                              <span className="text-xs text-slate-500">{entry.markCount} records · {entry.loCount} LOs</span>
                            </div>
                          </div>
                          <div className="flex gap-2 flex-shrink-0">
                            {/* Edit */}
                            <button type="button" onClick={() => openEditUpload(entry)} disabled={!!busyAction}
                              title="Edit marks — re-upload to replace"
                              className="p-2.5 rounded-xl bg-slate-100 text-slate-600 hover:bg-amber-50 hover:text-amber-600 transition-colors disabled:opacity-40">
                              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                              </svg>
                            </button>
                            {/* Download */}
                            <button type="button" onClick={() => handleDownloadBatchMarks(entry.batch,entry.markType)} disabled={!!busyAction}
                              title="Download marks report"
                              className="p-2.5 rounded-xl bg-slate-100 text-slate-600 hover:bg-indigo-50 hover:text-indigo-600 transition-colors disabled:opacity-40">
                              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                              </svg>
                            </button>
                            {/* Delete */}
                            <button type="button" onClick={() => handleDeleteAssignment(entry)} disabled={busyAction===delKey}
                              title="Delete this assignment's marks"
                              className="p-2.5 rounded-xl bg-slate-100 text-slate-600 hover:bg-red-50 hover:text-red-600 transition-colors disabled:opacity-40">
                              {busyAction===delKey
                                ? <span className="w-4 h-4 border-2 border-slate-300 border-t-red-500 rounded-full animate-spin block" />
                                : <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                  </svg>
                              }
                            </button>
                          </div>
                        </div>
                      )
                    })}
                  </div>
                )}
              </div>

              {/* Right — PO Attainment panel */}
              <div className="sticky top-28">
                <section className="glass-card rounded-[2.5rem] p-7 border-slate-100 space-y-5">
                  <div>
                    <span className="text-[10px] font-black text-emerald-600 uppercase tracking-[0.2em] mb-2 block">Analytics</span>
                    <h2 className="heading-lg">PO Attainment</h2>
                    <p className="text-xs text-slate-500 mt-1">Using threshold: <strong>{getThreshold(activeBatch)}%</strong></p>
                  </div>
                  <div className="space-y-2">
                    <label className="text-xs font-black text-slate-500 uppercase tracking-widest">Mark type</label>
                    <select value={analyticsMarkType} onChange={e => setAnalyticsMarkType(e.target.value)} className="input-field bg-white">
                      {markTypeOptions.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
                    </select>
                  </div>
                  <div className="space-y-2">
                    <div className="flex justify-between items-center">
                      <label className="text-xs font-black text-slate-500 uppercase tracking-widest">Learning outcomes</label>
                      <div className="flex gap-2">
                        <button type="button" onClick={() => setAnalyticsLosIds(los.map(l=>l.id))} className="text-[10px] font-black text-indigo-600 hover:underline">All</button>
                        <button type="button" onClick={() => setAnalyticsLosIds([])} className="text-[10px] font-black text-slate-400 hover:underline">Clear</button>
                      </div>
                    </div>
                    <div className="flex flex-wrap gap-2">
                      {los.map(lo => {
                        const checked = analyticsLosIds.includes(lo.id)
                        return (
                          <button key={lo.id} type="button" onClick={() => toggleAnalyticsLo(lo.id)}
                            className={`px-3 py-1.5 rounded-lg text-[10px] font-black uppercase tracking-widest border transition-colors ${checked?'bg-indigo-600 text-white border-indigo-600':'bg-white text-slate-500 border-slate-200 hover:border-indigo-400'}`}>
                            {lo.id}
                          </button>
                        )
                      })}
                    </div>
                  </div>
                  <div className="flex flex-col gap-3 pt-2 border-t border-slate-100">
                    <button type="button" onClick={handleCalculatePOAttainment}
                      disabled={busyAction==='po'||!analyticsLos.length}
                      className={`w-full py-4 px-6 rounded-2xl text-white font-bold shadow-lg transition-all flex items-center justify-center gap-3 ${busyAction==='po'||!analyticsLos.length?'bg-slate-300 cursor-not-allowed':'bg-emerald-600 hover:bg-emerald-700'}`}>
                      {busyAction==='po'&&<span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"/>}
                      Calculate PO Attainment
                    </button>
                    <button type="button" onClick={handleExportPOAttainment}
                      disabled={busyAction==='po-export'||!analyticsLos.length}
                      className={`w-full py-3 px-6 rounded-2xl border font-bold transition-all flex items-center justify-center gap-3 ${busyAction==='po-export'||!analyticsLos.length?'bg-slate-100 text-slate-400 cursor-not-allowed':'bg-white text-slate-700 border-slate-200 hover:border-emerald-200 hover:text-emerald-700'}`}>
                      {busyAction==='po-export'&&<span className="w-4 h-4 border-2 border-slate-300 border-t-emerald-600 rounded-full animate-spin"/>}
                      Export PO Attainment Excel
                    </button>
                  </div>
                </section>
              </div>
            </div>

            {/* PO Attainment Results */}
            {poAttainment?.poList?.length > 0 && (
              <section className="glass-card rounded-[2.5rem] p-8 border-slate-100 space-y-6">
                <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
                  <div>
                    <span className="text-[10px] font-black text-emerald-600 uppercase tracking-[0.2em] mb-2 block">Results</span>
                    <h2 className="heading-lg">Per-Student PO Credit Attainment</h2>
                    <p className="text-sm text-slate-500 mt-1">Threshold: <strong>{poAttainment.threshold}%</strong> · Students: <strong>{poAttainment.studentCount}</strong> · POs: <strong>{poAttainment.poList.length}</strong></p>
                  </div>
                  <button type="button" onClick={() => setPOAttainment(null)}
                    className="px-4 py-2 bg-slate-100 text-slate-600 rounded-lg text-xs font-bold hover:bg-red-50 hover:text-red-500 transition-colors">
                    Clear results
                  </button>
                </div>
                {poAttainment.loPoMappings?.length > 0 && (
                  <div className="rounded-2xl border border-slate-200 bg-white/70 p-4">
                    <div className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-3">LO → PO Mappings</div>
                    <div className="flex flex-wrap gap-2">
                      {poAttainment.loPoMappings.map((m,i) => (
                        <span key={i} className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-indigo-50 text-indigo-700 text-xs font-semibold">
                          {m.loId} → {m.poCode} <span className="text-[10px] text-indigo-400">(wt: {m.weight})</span>
                        </span>
                      ))}
                    </div>
                  </div>
                )}
                <div className="rounded-2xl border border-emerald-200 bg-emerald-50/50 p-4">
                  <div className="text-[10px] font-black text-emerald-600 uppercase tracking-widest mb-2">Maximum Possible Credits</div>
                  <div className="flex flex-wrap gap-3">
                    {poAttainment.poList.map(po => (
                      <span key={po} className="px-3 py-1.5 rounded-xl bg-emerald-100 text-emerald-700 text-xs font-bold">{po}: {poAttainment.maxCredits?.[po]||0}</span>
                    ))}
                    <span className="px-3 py-1.5 rounded-xl bg-emerald-600 text-white text-xs font-bold">Total: {poAttainment.totalMaxCredit}</span>
                  </div>
                </div>
                <div className="overflow-x-auto rounded-2xl border border-slate-200">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="bg-slate-800 text-white">
                        <th className="px-4 py-3 text-left font-bold text-xs uppercase tracking-widest">Student</th>
                        {poAttainment.poList.map(po=><th key={po} className="px-4 py-3 text-center font-bold text-xs uppercase tracking-widest">{po}</th>)}
                        <th className="px-4 py-3 text-center font-bold text-xs uppercase tracking-widest bg-slate-900">Total</th>
                      </tr>
                    </thead>
                    <tbody>
                      {poAttainment.students?.map((s,idx)=>(
                        <tr key={s.studentId} className={idx%2===0?'bg-white':'bg-slate-50'}>
                          <td className="px-4 py-3 font-semibold text-slate-800 border-r border-slate-100">{s.studentId}</td>
                          {poAttainment.poList.map(po=>{
                            const c=s.poCredits?.[po]||0; const max=poAttainment.maxCredits?.[po]||1
                            return <td key={po} className={`px-4 py-3 text-center font-bold border-r border-slate-100 ${c>0?c>=max?'bg-emerald-100 text-emerald-700':'bg-emerald-50 text-emerald-600':'text-slate-400'}`}>{c}</td>
                          })}
                          <td className={`px-4 py-3 text-center font-black border-l-2 border-slate-200 ${s.totalCredit>0?s.totalCredit>=poAttainment.totalMaxCredit?'bg-emerald-200 text-emerald-800':'bg-emerald-50 text-emerald-700':'text-slate-400'}`}>{s.totalCredit}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </section>
            )}
          </div>

        ) : (
          /* ══ SCREEN 3 — Upload workflow ════════════════════════════════ */
          <div className="space-y-8 animate-in fade-in duration-500">
            {/* Context bar */}
            <div className="flex items-center flex-wrap gap-4 p-4 rounded-2xl bg-indigo-50 border border-indigo-100">
              <button type="button" onClick={uploadMode ? () => setUploadMode(null) : closeUploadWorkflow}
                className="flex items-center gap-2 text-indigo-600 font-bold text-sm hover:text-indigo-800 transition-colors">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
                </svg>
                {uploadMode ? 'Back to Options' : `Back to Batch ${activeBatch}`}
              </button>
              <div className="h-5 w-px bg-indigo-200" />
              <span className="text-[10px] font-black text-indigo-400 uppercase tracking-widest">
                {editingEntry ? `Edit — ${editingEntry.assignmentLabel||editingEntry.markType}` : 'Upload New Marks'}
              </span>
              {editingEntry && (
                <span className="px-3 py-1 bg-amber-100 text-amber-700 rounded-lg text-xs font-black">Re-uploading will replace existing data</span>
              )}
              <button type="button" onClick={closeUploadWorkflow}
                className="ml-auto p-2 text-slate-400 hover:text-red-500 transition-colors">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"/>
                </svg>
              </button>
            </div>

            {/* ── 3A: Mode selection ── */}
            {!uploadMode && (
              <div className="space-y-6">
                <div>
                  <h2 className="heading-xl">What would you like to do?</h2>
                  <p className="text-slate-500 mt-2">Choose an option to get started.</p>
                </div>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6 max-w-3xl">
                  {/* Option A — Download Template */}
                  <button type="button" onClick={() => setUploadMode('download-template')}
                    className="group glass-card rounded-[2.5rem] p-10 text-left hover:shadow-xl hover:border-indigo-300 transition-all duration-300 space-y-4">
                    <div className="w-16 h-16 rounded-2xl bg-indigo-100 text-indigo-600 flex items-center justify-center group-hover:scale-110 transition-transform">
                      <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/>
                      </svg>
                    </div>
                    <div>
                      <h3 className="font-black text-slate-800 text-xl mb-2">Download Template</h3>
                      <p className="text-sm text-slate-500 leading-relaxed">Configure your assignment, select LOs and max marks, then download a pre-formatted Excel template to fill in.</p>
                    </div>
                    <span className="inline-flex items-center gap-2 text-indigo-600 font-black text-sm group-hover:gap-3 transition-all">
                      Configure &amp; download
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M9 5l7 7-7 7"/></svg>
                    </span>
                  </button>

                  {/* Option B — Upload Marks */}
                  <button type="button" onClick={() => setUploadMode('upload-marks')}
                    className="group glass-card rounded-[2.5rem] p-10 text-left hover:shadow-xl hover:border-emerald-300 transition-all duration-300 space-y-4">
                    <div className="w-16 h-16 rounded-2xl bg-emerald-100 text-emerald-600 flex items-center justify-center group-hover:scale-110 transition-transform">
                      <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"/>
                      </svg>
                    </div>
                    <div>
                      <h3 className="font-black text-slate-800 text-xl mb-2">Upload Marks</h3>
                      <p className="text-sm text-slate-500 leading-relaxed">Already have a filled template? Upload it here. The system auto-detects mark type, LO mapping, and max marks from the file.</p>
                    </div>
                    <span className="inline-flex items-center gap-2 text-emerald-600 font-black text-sm group-hover:gap-3 transition-all">
                      Upload file
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M9 5l7 7-7 7"/></svg>
                    </span>
                  </button>
                </div>
              </div>
            )}

            {/* ── 3B: Download template flow ── */}
            {uploadMode === 'download-template' && (
              <div className="grid grid-cols-1 lg:grid-cols-[1.3fr_0.9fr] gap-8 items-start">
                <div className="space-y-6">
                  {/* Step 1 — Assignment details */}
                  <section className="glass-card rounded-[2.5rem] p-8 border-slate-100 space-y-5">
                    <div>
                      <span className="text-[10px] font-black text-indigo-600 uppercase tracking-[0.2em] mb-2 block">Step 1</span>
                      <h2 className="heading-lg">Assignment Details</h2>
                    </div>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div className="space-y-2">
                        <label className="text-xs font-black text-slate-500 uppercase tracking-widest">Assignment label <span className="text-red-400">*</span></label>
                        {editingEntry ? (
                          <>
                            <div className="input-field bg-slate-50 text-slate-600 cursor-not-allowed">{assignmentLabel||'—'}</div>
                            <p className="text-xs text-amber-600 font-bold">Label is fixed in edit mode.</p>
                          </>
                        ) : (
                          <input type="text" value={assignmentLabel} onChange={e=>setAssignmentLabel(e.target.value)}
                            className="input-field" placeholder='e.g. Assignment 1, Mid Exam' />
                        )}
                      </div>
                      <div className="space-y-2">
                        <label className="text-xs font-black text-slate-500 uppercase tracking-widest">Mark type</label>
                        {editingEntry ? (
                          <div className="input-field bg-slate-50 text-slate-600 cursor-not-allowed">
                            {markType==='FINAL_EXAM'?'Final Exam':'Assignment'}
                          </div>
                        ) : (
                          <select value={markType} onChange={e=>setMarkType(e.target.value)} className="input-field bg-white">
                            {markTypeOptions.map(o=><option key={o.value} value={o.value}>{o.label}</option>)}
                          </select>
                        )}
                      </div>
                    </div>
                  </section>

                  {/* Step 2 — LO selection */}
                  <section className="glass-card rounded-[2.5rem] p-8 border-slate-100">
                    <div className="flex items-center justify-between mb-5">
                      <div>
                        <span className="text-[10px] font-black text-indigo-600 uppercase tracking-[0.2em] mb-1 block">Step 2</span>
                        <h2 className="heading-lg">Learning Outcomes</h2>
                      </div>
                      <div className="flex gap-2">
                        <button type="button" onClick={() => setSelectedLosIds(los.map(l=>l.id))} className="btn-secondary text-xs py-2">All</button>
                        <button type="button" onClick={() => setSelectedLosIds([])} className="px-3 py-2 rounded-xl bg-slate-100 text-slate-600 text-xs font-black hover:bg-slate-200 transition-colors">Clear</button>
                      </div>
                    </div>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                      {los.map(lo => {
                        const checked = selectedLosIds.includes(lo.id)
                        return (
                          <button key={lo.id} type="button" onClick={() => toggleLo(lo.id)}
                            className={`text-left p-4 rounded-2xl border transition-all ${checked?'bg-indigo-50 border-indigo-200 shadow-sm':'bg-white/60 border-slate-200 hover:border-indigo-200'}`}>
                            <div className="flex items-start gap-3">
                              <div className={`mt-0.5 w-5 h-5 rounded-md border flex items-center justify-center flex-shrink-0 ${checked?'bg-indigo-600 border-indigo-600 text-white':'border-slate-300 bg-white'}`}>
                                {checked&&<svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M5 13l4 4L19 7"/></svg>}
                              </div>
                              <div>
                                <span className="px-2 py-0.5 bg-slate-100 text-slate-600 rounded text-[10px] font-black uppercase">{lo.id}</span>
                                <p className="text-sm font-semibold text-slate-800 mt-1">{lo.description||lo.name||'Learning Outcome'}</p>
                              </div>
                            </div>
                          </button>
                        )
                      })}
                    </div>
                  </section>

                  {/* Step 3 — Template type & download */}
                  <section className="glass-card rounded-[2.5rem] p-8 border-slate-100 space-y-5">
                    <div>
                      <span className="text-[10px] font-black text-indigo-600 uppercase tracking-[0.2em] mb-2 block">Step 3</span>
                      <h2 className="heading-lg">Template Type &amp; Download</h2>
                      <p className="text-sm text-slate-500 mt-1">Type is embedded in the file and auto-detected on upload.</p>
                    </div>

                    <div className="flex gap-1 p-1 bg-slate-100 rounded-xl w-fit">
                      {[{key:'question-wise',label:'Question-wise'},{key:'lo-wise',label:'LO-wise'}].map(tab=>(
                        <button key={tab.key} type="button" onClick={()=>setTemplateTypeTab(tab.key)}
                          className={`px-5 py-2 rounded-lg text-xs font-black transition-all ${templateTypeTab===tab.key?'bg-white text-indigo-700 shadow-sm':'text-slate-500 hover:text-slate-700'}`}>
                          {tab.label}
                        </button>
                      ))}
                    </div>

                    {templateTypeTab==='question-wise' ? (
                      <div className="space-y-4">
                        <p className="text-xs text-slate-500">Columns are Q1, Q2… each mapped to an LO. Scores are aggregated to LO totals on upload.</p>
                        <div className="space-y-2">
                          <label className="text-xs font-black text-slate-500 uppercase tracking-widest">Number of questions</label>
                          <input type="number" min="1" max="50" value={numberOfQuestions} onChange={e=>setNumberOfQuestions(e.target.value)} className="input-field w-40" />
                        </div>
                        <div className="rounded-2xl border border-slate-200 bg-white/70 p-4 space-y-2 max-h-60 overflow-auto">
                          <div className="grid grid-cols-[0.5fr_1.5fr_1fr] gap-2 mb-2">
                            <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest ml-1">Q</span>
                            <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest">Maps to LO</span>
                            <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest">Max marks</span>
                          </div>
                          {questionMappings.map((m,idx)=>(
                            <div key={m.questionNumber} className="grid grid-cols-[0.5fr_1.5fr_1fr] gap-2 items-center">
                              <span className="text-xs font-black text-slate-600 ml-1">Q{m.questionNumber}</span>
                              <select value={m.loId||''} onChange={e=>updateQM(idx,'loId',e.target.value)} className="input-field bg-white text-sm py-2">
                                {selectedLos.map(lo=><option key={lo.id} value={lo.id}>{lo.id}</option>)}
                              </select>
                              <input type="number" min="0" value={m.maxMarks} onChange={e=>updateQM(idx,'maxMarks',e.target.value)} className="input-field text-sm py-2"/>
                            </div>
                          ))}
                        </div>
                        <button type="button" onClick={handleDownloadQuestionMarkTemplate}
                          disabled={busyAction==='q-template'||!selectedLos.length||!assignmentLabel.trim()}
                          className={`w-full py-4 px-6 rounded-2xl font-bold transition-all flex items-center justify-center gap-3 ${busyAction==='q-template'||!selectedLos.length||!assignmentLabel.trim()?'bg-slate-100 text-slate-400 cursor-not-allowed':'bg-indigo-600 text-white hover:bg-indigo-700 shadow-lg'}`}>
                          {busyAction==='q-template'&&<span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"/>}
                          Download Question-wise Template
                        </button>
                      </div>
                    ) : (
                      <div className="space-y-4">
                        <p className="text-xs text-slate-500">One column per LO. Set the max marks for each LO individually — used for validation and attainment calculation.</p>
                        <div className="space-y-3">
                          <label className="text-xs font-black text-slate-500 uppercase tracking-widest">Max marks per LO <span className="text-red-400">*</span></label>
                          {selectedLos.length === 0 && (
                            <p className="text-xs text-amber-600 font-bold">Select at least one LO above.</p>
                          )}
                          {selectedLos.map(lo => (
                            <div key={lo.id} className="flex items-center gap-3">
                              <div className="flex-1 flex items-center gap-3 p-3 bg-slate-50 rounded-xl border border-slate-200">
                                <span className="px-2 py-0.5 bg-indigo-100 text-indigo-700 rounded text-[10px] font-black uppercase w-20 text-center flex-shrink-0">{lo.id}</span>
                                <span className="text-sm text-slate-600 truncate flex-1">{lo.description||lo.name||''}</span>
                              </div>
                              <input
                                type="number" min="0"
                                value={loMaxMarks[lo.id] ?? 100}
                                onChange={e => setLoMaxMarks(prev => ({ ...prev, [lo.id]: e.target.value }))}
                                className="input-field w-28 text-center font-black"
                                placeholder="100"
                              />
                              <span className="text-xs text-slate-400 font-bold flex-shrink-0">marks</span>
                            </div>
                          ))}
                        </div>
                        <button type="button" onClick={handleDownloadLoTemplate}
                          disabled={busyAction==='lo-template'||!selectedLos.length||!assignmentLabel.trim()}
                          className={`w-full py-4 px-6 rounded-2xl font-bold transition-all flex items-center justify-center gap-3 ${busyAction==='lo-template'||!selectedLos.length||!assignmentLabel.trim()?'bg-slate-100 text-slate-400 cursor-not-allowed':'bg-indigo-600 text-white hover:bg-indigo-700 shadow-lg'}`}>
                          {busyAction==='lo-template'&&<span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"/>}
                          Download LO-wise Template
                        </button>
                      </div>
                    )}

                    {!assignmentLabel.trim()&&(
                      <p className="text-xs text-amber-600 font-bold">Enter an assignment label in Step 1 to enable downloads.</p>
                    )}
                  </section>
                </div>

                {/* Right — mini upload panel for after downloading */}
                <div className="sticky top-28 space-y-5">
                  <section className="glass-card rounded-[2.5rem] p-8 border-slate-100 space-y-5">
                    <div>
                      <span className="text-[10px] font-black text-emerald-600 uppercase tracking-[0.2em] mb-2 block">Step 4 (Optional)</span>
                      <h2 className="heading-lg">Upload Filled Template</h2>
                      <p className="text-sm text-slate-500 mt-1">After filling the downloaded template, upload it here.</p>
                    </div>
                    <button type="button"
                      className={`w-full group p-8 border-2 border-dashed rounded-[2rem] transition-all flex flex-col items-center justify-center cursor-pointer ${dragActive?'border-emerald-500 bg-emerald-50/30':'border-slate-200 hover:border-emerald-400 bg-white/30'}`}
                      onDragEnter={handleDrag} onDragLeave={handleDrag} onDragOver={handleDrag} onDrop={handleDrop}
                      onClick={()=>fileInputRef.current?.click()}>
                      <input ref={fileInputRef} type="file" className="hidden" onChange={handleFileChange} accept=".xlsx,.xls"/>
                      <div className="w-14 h-14 bg-emerald-50 rounded-2xl flex items-center justify-center mb-4 text-emerald-600 group-hover:scale-110 transition-transform">
                        <svg className="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"/>
                        </svg>
                      </div>
                      <h3 className="text-sm font-bold text-slate-800 mb-1">{uploadFile?uploadFile.name:'Drop file here'}</h3>
                      <p className="text-slate-500 text-xs">{uploadFile?`${(uploadFile.size/1024).toFixed(1)} KB`:'Click to browse (.xlsx)'}</p>
                    </button>
                    {uploadFile&&(
                      <button type="button" onClick={resetFile} className="px-4 py-2 bg-slate-100 text-slate-600 rounded-lg text-xs font-bold hover:bg-red-50 hover:text-red-500 transition-colors">Clear file</button>
                    )}
                    <button type="button" onClick={handleUploadMarks}
                      disabled={busyAction==='upload'||!uploadFile}
                      className={`w-full py-4 px-6 rounded-2xl text-white font-bold shadow-lg transition-all flex items-center justify-center gap-3 ${busyAction==='upload'||!uploadFile?'bg-slate-300 cursor-not-allowed':'bg-emerald-600 hover:bg-emerald-700 shadow-emerald-600/20'}`}>
                      {busyAction==='upload'&&<span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"/>}
                      Upload Marks
                    </button>
                  </section>
                </div>
              </div>
            )}

            {/* ── 3C: Upload-only flow ── */}
            {uploadMode === 'upload-marks' && (
              <div className="max-w-xl mx-auto space-y-6">
                <div>
                  <h2 className="heading-xl">Upload Marks File</h2>
                  <p className="text-slate-500 mt-2">The system reads assignment label, LO mapping, mark type, and max marks directly from the file — no extra configuration needed.</p>
                </div>
                <section className="glass-card rounded-[2.5rem] p-10 border-slate-100 space-y-6">
                  <button type="button"
                    className={`w-full group p-12 border-2 border-dashed rounded-[2rem] transition-all flex flex-col items-center justify-center cursor-pointer ${dragActive?'border-emerald-500 bg-emerald-50/30':'border-slate-200 hover:border-emerald-400 bg-white/30'}`}
                    onDragEnter={handleDrag} onDragLeave={handleDrag} onDragOver={handleDrag} onDrop={handleDrop}
                    onClick={()=>fileInputRef.current?.click()}>
                    <input ref={fileInputRef} type="file" className="hidden" onChange={handleFileChange} accept=".xlsx,.xls"/>
                    <div className="w-20 h-20 bg-emerald-50 rounded-3xl flex items-center justify-center mb-6 text-emerald-600 group-hover:scale-110 transition-transform">
                      <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"/>
                      </svg>
                    </div>
                    {uploadFile ? (
                      <>
                        <h3 className="text-base font-black text-slate-800 mb-1">{uploadFile.name}</h3>
                        <p className="text-slate-500 text-sm">{(uploadFile.size/1024).toFixed(1)} KB · Ready to upload</p>
                      </>
                    ) : (
                      <>
                        <h3 className="text-base font-black text-slate-800 mb-1">Drop your Excel file here</h3>
                        <p className="text-slate-500 text-sm">or click to browse (.xlsx, .xls)</p>
                      </>
                    )}
                  </button>

                  {uploadFile&&(
                    <button type="button" onClick={resetFile} className="px-4 py-2 bg-slate-100 text-slate-600 rounded-lg text-xs font-bold hover:bg-red-50 hover:text-red-500 transition-colors">Clear file</button>
                  )}

                  <button type="button" onClick={handleUploadMarks}
                    disabled={busyAction==='upload'||!uploadFile}
                    className={`w-full py-5 px-6 rounded-2xl text-white font-bold shadow-lg transition-all flex items-center justify-center gap-3 text-base ${busyAction==='upload'||!uploadFile?'bg-slate-300 cursor-not-allowed':'bg-emerald-600 hover:bg-emerald-700 shadow-emerald-600/25'}`}>
                    {busyAction==='upload' ? (
                      <><span className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin"/>Uploading…</>
                    ) : (
                      <><svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"/></svg>Upload Marks</>
                    )}
                  </button>

                  <div className="rounded-2xl bg-indigo-50 border border-indigo-100 p-4 text-xs text-indigo-700 space-y-1.5">
                    <p className="font-black uppercase tracking-wide text-indigo-800">Auto-detected from file</p>
                    <p>• Assignment label, batch, mark type</p>
                    <p>• LO columns and per-LO max marks</p>
                    <p>• Any mark out of range rejects the whole file</p>
                    <p>• Re-uploading the same label replaces existing data</p>
                  </div>
                </section>
              </div>
            )}
          </div>
        )}
      </main>
      <Footer />
    </div>
  )
}
