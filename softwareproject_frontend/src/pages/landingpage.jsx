import { useNavigate } from 'react-router-dom'
import { useState, useEffect } from 'react'
import Header from '../components/header'
import Footer from '../components/footer'
import authService from '../services/authService'

// ── SVG Icons ────────────────────────────────────────────────────────────────
const IconMapping = () => (
  <svg className="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8"
      d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
  </svg>
)
const IconChart = () => (
  <svg className="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8"
      d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
  </svg>
)
const IconUpload = () => (
  <svg className="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8"
      d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
  </svg>
)
const IconTrend = () => (
  <svg className="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8"
      d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
  </svg>
)
const IconShield = () => (
  <svg className="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8"
      d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
  </svg>
)
const IconAcademic = () => (
  <svg className="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8"
      d="M12 14l9-5-9-5-9 5 9 5z" />
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.8"
      d="M12 14l6.16-3.422a12.083 12.083 0 01.665 6.479A11.952 11.952 0 0012 20.055a11.952 11.952 0 00-6.824-2.998 12.078 12.078 0 01.665-6.479L12 14z" />
  </svg>
)
const IconArrow = () => (
  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 8l4 4m0 0l-4 4m4-4H3" />
  </svg>
)
const IconCheck = () => (
  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M5 13l4 4L19 7" />
  </svg>
)

// ── Animated counter ─────────────────────────────────────────────────────────
function Counter({ target, suffix = '' }) {
  const [count, setCount] = useState(0)
  useEffect(() => {
    const step = Math.ceil(target / 40)
    const timer = setInterval(() => {
      setCount(prev => {
        if (prev + step >= target) { clearInterval(timer); return target }
        return prev + step
      })
    }, 30)
    return () => clearInterval(timer)
  }, [target])
  return <>{count}{suffix}</>
}

// ── Mini dashboard preview SVG ────────────────────────────────────────────────
function DashboardPreview() {
  return (
    <div className="relative w-full max-w-lg mx-auto">
      {/* Glow */}
      <div className="absolute inset-0 bg-indigo-500/20 blur-3xl rounded-3xl" />
      <div className="relative bg-slate-800/90 backdrop-blur rounded-2xl border border-slate-700/60 shadow-2xl overflow-hidden">
        {/* Window chrome */}
        <div className="flex items-center gap-2 px-4 py-3 border-b border-slate-700/60 bg-slate-900/60">
          <span className="w-3 h-3 rounded-full bg-red-500/80" />
          <span className="w-3 h-3 rounded-full bg-amber-400/80" />
          <span className="w-3 h-3 rounded-full bg-emerald-500/80" />
          <span className="ml-4 text-xs text-slate-400 font-mono">OBE Analytics Dashboard</span>
        </div>
        <div className="p-5 space-y-4">
          {/* Top stat row */}
          <div className="grid grid-cols-3 gap-3">
            {[
              { label: 'PO Attainment', value: '87%', color: 'text-emerald-400', bg: 'bg-emerald-500/10' },
              { label: 'Modules', value: '12', color: 'text-indigo-400', bg: 'bg-indigo-500/10' },
              { label: 'Pass Rate', value: '73%', color: 'text-amber-400', bg: 'bg-amber-500/10' },
            ].map(s => (
              <div key={s.label} className={`${s.bg} rounded-xl p-3 border border-white/5`}>
                <p className="text-[10px] text-slate-400 mb-1">{s.label}</p>
                <p className={`text-xl font-black ${s.color}`}>{s.value}</p>
              </div>
            ))}
          </div>
          {/* Bar chart */}
          <div className="bg-slate-900/50 rounded-xl p-4 border border-slate-700/40">
            <p className="text-[10px] text-slate-400 mb-3 font-semibold uppercase tracking-widest">LO Attainment by Batch</p>
            <div className="flex items-end gap-2 h-24">
              {[55, 62, 70, 65, 78, 87].map((h, i) => (
                <div key={i} className="flex-1 flex flex-col items-center gap-1">
                  <div
                    className="w-full rounded-t-md"
                    style={{
                      height: `${h}%`,
                      background: i === 5
                        ? 'linear-gradient(to top, #6366f1, #818cf8)'
                        : 'rgba(99,102,241,0.3)'
                    }}
                  />
                  <span className="text-[9px] text-slate-500">{`'${19 + i}`}</span>
                </div>
              ))}
            </div>
          </div>
          {/* Mapping status rows */}
          <div className="space-y-2">
            {[
              { lo: 'LO1 — Network Design', po: 'PO3', status: 'Approved', color: 'bg-emerald-500/20 text-emerald-400' },
              { lo: 'LO2 — Data Analysis', po: 'PO7', status: 'Pending', color: 'bg-amber-500/20 text-amber-400' },
              { lo: 'LO3 — System Modelling', po: 'PO2', status: 'Approved', color: 'bg-emerald-500/20 text-emerald-400' },
            ].map(r => (
              <div key={r.lo} className="flex items-center justify-between bg-slate-900/40 rounded-lg px-3 py-2 border border-slate-700/30">
                <div>
                  <p className="text-[11px] text-slate-200 font-medium">{r.lo}</p>
                  <p className="text-[10px] text-slate-500">{r.po}</p>
                </div>
                <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${r.color}`}>{r.status}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}

// ── Main Component ────────────────────────────────────────────────────────────
export default function LandingPage() {
  const navigate = useNavigate()

  const handleAccessDashboard = () => {
    if (authService.isLoggedIn()) {
      const userInfo = authService.getUserInfo()
      const role = userInfo?.userType?.toLowerCase()?.trim()
      if (role === 'superadmin' || role === 'super admin') navigate('/super-admin-dashboard')
      else if (role === 'admin') navigate('/admin-dashboard')
      else if (role === 'lecture') navigate('/lecturer-dashboard')
    } else {
      navigate('/loginpage')
    }
  }

  const features = [
    {
      icon: <IconMapping />,
      title: 'LO-PO Mapping',
      desc: 'Map Learning Outcomes to Washington Accord Programme Outcomes with configurable correlation weights (Low / Medium / High). Admin approval workflow ensures academic integrity.',
      iconColor: 'text-indigo-600',
      bg: 'bg-indigo-50',
      border: 'border-indigo-100',
    },
    {
      icon: <IconChart />,
      title: 'PO Attainment Calculation',
      desc: 'Automatically compute and visualize LO and PO attainment levels from student performance data — per batch, per mark type, with normalized percentage scoring.',
      iconColor: 'text-emerald-600',
      bg: 'bg-emerald-50',
      border: 'border-emerald-100',
    },
    {
      icon: <IconUpload />,
      title: 'Excel Marks Integration',
      desc: 'Download pre-formatted Q-wise or LO-wise templates, fill in student scores, and upload in bulk. Validation catches every out-of-range score before saving — no partial imports.',
      iconColor: 'text-sky-600',
      bg: 'bg-sky-50',
      border: 'border-sky-100',
    },
    {
      icon: <IconTrend />,
      title: 'Batch-over-Batch Trend Analysis',
      desc: 'Track average score trends across cohorts with IMPROVED / STABLE / DECLINED status. Charts show normalized percentages so batches with different max marks compare fairly.',
      iconColor: 'text-amber-600',
      bg: 'bg-amber-50',
      border: 'border-amber-100',
    },
    {
      icon: <IconShield />,
      title: 'Role-Based Access Control',
      desc: 'Three-tier access: Super Admin provisions Admins, Admins manage modules and approve mappings, Lecturers handle LO creation and marks. JWT-secured sessions with auto-expiry.',
      iconColor: 'text-rose-600',
      bg: 'bg-rose-50',
      border: 'border-rose-100',
    },
    {
      icon: <IconAcademic />,
      title: 'Continuous Quality Improvement',
      desc: 'CQI action tracking links quality issues to specific modules and LOs. Pass rate analytics and trend deltas give coordinators the data needed to drive programme improvements.',
      iconColor: 'text-purple-600',
      bg: 'bg-purple-50',
      border: 'border-purple-100',
    },
  ]

  const steps = [
    {
      num: '01',
      title: 'Set Up Modules & Outcomes',
      desc: 'Admin creates modules and defines Programme Outcomes aligned with Washington Accord. Lecturers create Learning Outcomes and submit LO-PO mappings for approval.',
    },
    {
      num: '02',
      title: 'Upload Student Marks',
      desc: 'Download the pre-built Excel template (Q-wise or LO-wise), fill in student scores, and upload. The system validates every mark and stores results per batch.',
    },
    {
      num: '03',
      title: 'Analyse & Improve',
      desc: 'View PO attainment levels, pass/fail rates, and batch-over-batch trends. Use CQI actions to address gaps and drive continuous improvement cycles.',
    },
  ]

  const roles = [
    {
      title: 'Super Admin',
      badge: 'System Level',
      badgeColor: 'bg-violet-100 text-violet-700',
      icon: '⬡',
      iconBg: 'bg-violet-100 text-violet-600',
      items: ['Create and manage Admin accounts', 'System-level oversight'],
    },
    {
      title: 'Admin',
      badge: 'Department Level',
      badgeColor: 'bg-blue-100 text-blue-700',
      icon: '⬡',
      iconBg: 'bg-blue-100 text-blue-600',
      items: [
        'Create/manage modules & teacher accounts',
        'Manage Programme Outcomes (POs)',
        'Review and approve LO-PO mappings',
        'Full analytics access',
      ],
    },
    {
      title: 'Lecturer',
      badge: 'Course Level',
      badgeColor: 'bg-emerald-100 text-emerald-700',
      icon: '⬡',
      iconBg: 'bg-emerald-100 text-emerald-600',
      items: [
        'Create Learning Outcomes per module',
        'Map LOs to POs and submit for approval',
        'Upload student marks via Excel',
        'View attainment and trend analytics',
      ],
    },
  ]

  return (
    <div className="min-h-screen bg-white">
      <Header />

      {/* ── HERO ─────────────────────────────────────────────────────────── */}
      <section className="relative overflow-hidden bg-slate-950 pt-20 pb-28">
        {/* Background grid */}
        <div className="absolute inset-0 opacity-[0.04]"
          style={{ backgroundImage: 'radial-gradient(circle, #fff 1px, transparent 1px)', backgroundSize: '28px 28px' }} />
        {/* Gradient blobs */}
        <div className="absolute top-0 left-1/4 w-96 h-96 bg-indigo-600/20 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute bottom-0 right-1/4 w-80 h-80 bg-violet-600/15 rounded-full blur-3xl pointer-events-none" />

        <div className="relative max-w-7xl mx-auto px-6 sm:px-10">
          <div className="grid lg:grid-cols-2 gap-16 items-center">
            {/* Left */}
            <div className="space-y-8">
              <div className="inline-flex items-center gap-2 bg-indigo-500/10 border border-indigo-500/20 rounded-full px-4 py-1.5">
                <span className="w-2 h-2 bg-indigo-400 rounded-full animate-pulse" />
                <span className="text-indigo-300 text-xs font-semibold uppercase tracking-widest">
                  DEIE · University of Ruhuna
                </span>
              </div>

              <div className="space-y-4">
                <h1 className="text-5xl lg:text-6xl font-black text-white leading-[1.08] tracking-tight">
                  Automate Your
                  <span className="block bg-gradient-to-r from-indigo-400 to-violet-400 bg-clip-text text-transparent">
                    OBE Assurance.
                  </span>
                </h1>
                <p className="text-slate-400 text-lg leading-relaxed max-w-md">
                  A dedicated platform for the Department of Electrical & Information Engineering to streamline
                  LO-PO mapping, calculate attainment levels, and drive Continuous Quality Improvement.
                </p>
              </div>

              <div className="flex flex-wrap gap-4">
                <button
                  onClick={handleAccessDashboard}
                  className="flex items-center gap-3 bg-indigo-600 hover:bg-indigo-500 text-white px-7 py-3.5 rounded-xl font-bold text-sm tracking-wide transition-all duration-200 shadow-lg shadow-indigo-500/25 hover:shadow-indigo-500/40 hover:-translate-y-0.5 active:scale-95"
                >
                  Access Dashboard
                  <IconArrow />
                </button>
                <a
                  href="#features"
                  className="flex items-center gap-2 border border-slate-700 hover:border-slate-500 text-slate-300 hover:text-white px-7 py-3.5 rounded-xl font-bold text-sm tracking-wide transition-all duration-200"
                >
                  Explore Features
                </a>
              </div>

              {/* Mini trust badges */}
              <div className="flex flex-wrap gap-3 pt-2">
                {['Washington Accord Aligned', 'JWT Secured', 'Excel Integration', 'Multi-role Access'].map(b => (
                  <span key={b} className="flex items-center gap-1.5 bg-slate-800 text-slate-400 text-[11px] font-semibold px-3 py-1.5 rounded-lg border border-slate-700">
                    <span className="text-emerald-400">✓</span> {b}
                  </span>
                ))}
              </div>
            </div>

            {/* Right — Dashboard preview */}
            <div className="hidden lg:block">
              <DashboardPreview />
            </div>
          </div>
        </div>
      </section>

      {/* ── STATS STRIP ─────────────────────────────────────────────────── */}
      <section className="bg-slate-900 border-y border-slate-800">
        <div className="max-w-7xl mx-auto px-6 sm:px-10 py-8">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-8 text-center">
            {[
              { target: 12, suffix: '+', label: 'Programme Outcomes' },
              { target: 3, suffix: '', label: 'User Role Tiers' },
              { target: 100, suffix: '%', label: 'Data Cascade Safety' },
              { target: 6, suffix: '+', label: 'Batch Cohorts Tracked' },
            ].map(s => (
              <div key={s.label} className="space-y-1">
                <p className="text-3xl font-black text-white">
                  <Counter target={s.target} suffix={s.suffix} />
                </p>
                <p className="text-xs text-slate-400 font-medium uppercase tracking-widest">{s.label}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── FEATURES ────────────────────────────────────────────────────── */}
      <section id="features" className="py-24 bg-slate-50">
        <div className="max-w-7xl mx-auto px-6 sm:px-10">
          {/* Section header */}
          <div className="text-center mb-16 space-y-3">
            <span className="text-indigo-600 text-xs font-black uppercase tracking-[0.2em]">Platform Features</span>
            <h2 className="text-4xl font-black text-slate-900 tracking-tight">
              Everything you need for<br />
              <span className="text-indigo-600">outcome-based assurance</span>
            </h2>
            <p className="text-slate-500 max-w-xl mx-auto leading-relaxed">
              Built specifically for DEIE's accreditation workflow — from LO creation to PO attainment reporting.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {features.map((f, i) => (
              <div
                key={i}
                className={`group bg-white rounded-2xl p-7 border ${f.border} shadow-sm hover:shadow-xl hover:-translate-y-1 transition-all duration-300`}
              >
                <div className={`w-14 h-14 ${f.bg} rounded-2xl flex items-center justify-center mb-5 group-hover:scale-110 transition-transform duration-300 ${f.iconColor}`}>
                  {f.icon}
                </div>
                <h3 className="text-lg font-bold text-slate-900 mb-2">{f.title}</h3>
                <p className="text-slate-500 text-sm leading-relaxed">{f.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── HOW IT WORKS ────────────────────────────────────────────────── */}
      <section className="py-24 bg-white">
        <div className="max-w-7xl mx-auto px-6 sm:px-10">
          <div className="text-center mb-16 space-y-3">
            <span className="text-indigo-600 text-xs font-black uppercase tracking-[0.2em]">How It Works</span>
            <h2 className="text-4xl font-black text-slate-900 tracking-tight">Three steps to full attainment visibility</h2>
          </div>

          <div className="grid md:grid-cols-3 gap-8">
            {steps.map((s, i) => (
              <div key={i} className="relative">
                {/* Connector line */}
                {i < steps.length - 1 && (
                  <div className="hidden md:block absolute top-12 left-full w-full h-px bg-gradient-to-r from-indigo-200 to-transparent -translate-y-1/2 z-0" style={{ width: 'calc(100% - 80px)', left: '80px' }} />
                )}
                <div className="relative z-10 space-y-4">
                  <div className="w-20 h-20 bg-indigo-600 rounded-2xl flex items-center justify-center shadow-lg shadow-indigo-500/20">
                    <span className="text-white font-black text-2xl">{s.num}</span>
                  </div>
                  <h3 className="text-xl font-bold text-slate-900">{s.title}</h3>
                  <p className="text-slate-500 text-sm leading-relaxed">{s.desc}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── ROLES ───────────────────────────────────────────────────────── */}
      <section className="py-24 bg-slate-50">
        <div className="max-w-7xl mx-auto px-6 sm:px-10">
          <div className="text-center mb-16 space-y-3">
            <span className="text-indigo-600 text-xs font-black uppercase tracking-[0.2em]">User Roles</span>
            <h2 className="text-4xl font-black text-slate-900 tracking-tight">Designed for everyone in the workflow</h2>
            <p className="text-slate-500 max-w-lg mx-auto leading-relaxed">
              Each role has precisely scoped access — no more, no less — enforced at both frontend and backend.
            </p>
          </div>

          <div className="grid md:grid-cols-3 gap-6">
            {roles.map((r, i) => (
              <div key={i} className="bg-white rounded-2xl p-8 border border-slate-100 shadow-sm hover:shadow-lg transition-shadow duration-300">
                <div className="flex items-start justify-between mb-6">
                  <div className={`w-12 h-12 ${r.iconBg} rounded-xl flex items-center justify-center`}>
                    <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 24 24">
                      <path d="M12 12c2.7 0 4.8-2.1 4.8-4.8S14.7 2.4 12 2.4 7.2 4.5 7.2 7.2 9.3 12 12 12zm0 2.4c-3.2 0-9.6 1.6-9.6 4.8v2.4h19.2v-2.4c0-3.2-6.4-4.8-9.6-4.8z" />
                    </svg>
                  </div>
                  <span className={`text-[11px] font-bold px-3 py-1 rounded-full ${r.badgeColor}`}>{r.badge}</span>
                </div>
                <h3 className="text-xl font-black text-slate-900 mb-4">{r.title}</h3>
                <ul className="space-y-2.5">
                  {r.items.map((item, j) => (
                    <li key={j} className="flex items-start gap-2.5 text-sm text-slate-600">
                      <span className="mt-0.5 w-5 h-5 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center flex-shrink-0">
                        <IconCheck />
                      </span>
                      {item}
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── CTA BANNER ──────────────────────────────────────────────────── */}
      <section className="relative overflow-hidden bg-indigo-700 py-20">
        <div className="absolute inset-0 opacity-10"
          style={{ backgroundImage: 'radial-gradient(circle, #fff 1px, transparent 1px)', backgroundSize: '24px 24px' }} />
        <div className="absolute top-0 right-0 w-96 h-96 bg-violet-500/30 rounded-full blur-3xl pointer-events-none" />
        <div className="relative max-w-4xl mx-auto px-6 text-center space-y-7">
          <h2 className="text-4xl lg:text-5xl font-black text-white leading-tight tracking-tight">
            Ready to streamline your OBE process?
          </h2>
          <p className="text-indigo-200 text-lg max-w-2xl mx-auto leading-relaxed">
            Log in with your DEIE credentials to access your dashboard — manage modules, upload marks, and track PO attainment all in one place.
          </p>
          <button
            onClick={handleAccessDashboard}
            className="inline-flex items-center gap-3 bg-white text-indigo-700 hover:bg-indigo-50 font-black px-8 py-4 rounded-xl text-sm uppercase tracking-widest transition-all duration-200 shadow-xl hover:shadow-2xl hover:-translate-y-0.5 active:scale-95"
          >
            Access Dashboard
            <IconArrow />
          </button>
        </div>
      </section>

      <Footer />
    </div>
  )
}
