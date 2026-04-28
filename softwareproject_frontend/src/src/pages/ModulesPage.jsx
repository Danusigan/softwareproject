import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import axios from 'axios'
import Header from '../components/Header'
import Footer from '../components/Footer'
import authService from '../services/authService'

const API = 'http://localhost:8080'
const auth = () => ({ headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } })
const COLORS = ['from-blue-600 to-indigo-600','from-purple-600 to-pink-600','from-emerald-600 to-teal-600','from-amber-600 to-orange-600','from-red-600 to-rose-600','from-cyan-600 to-blue-600']

export default function ModulesPage() {
  const navigate = useNavigate()
  const user = authService.getUserInfo()
  const [modules, setModules] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')

  useEffect(() => {
    fetchModules()
  }, [])

  const fetchModules = async () => {
    try {
      const res = await axios.get(`${API}/api/modules/all`, auth())
      setModules(res.data.data || [])
    } catch { setModules([]) } finally { setLoading(false) }
  }

  const filtered = modules.filter(m =>
    m.moduleId.toLowerCase().includes(search.toLowerCase()) ||
    m.moduleName.toLowerCase().includes(search.toLowerCase())
  )

  const role = (user?.userType || '').toLowerCase()

  return (
    <div className="min-h-screen bg-[#0a0f1e] flex flex-col">
      <Header />
      <main className="flex-1 max-w-6xl mx-auto px-6 py-12 w-full">
        {/* Page Header */}
        <div className="flex items-start justify-between mb-8 flex-wrap gap-4">
          <div>
            <p className="text-slate-500 text-sm">OBE System</p>
            <h1 className="text-white text-3xl font-black mt-1">All Modules</h1>
            <p className="text-slate-400 text-sm mt-1">{modules.length} module{modules.length !== 1 ? 's' : ''} available</p>
          </div>
          <div className="relative">
            <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search modules..."
              className="bg-white/5 border border-white/10 text-white placeholder-slate-600 rounded-xl px-4 py-2.5 text-sm w-64 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500/30 transition-colors" />
            {search && <button onClick={() => setSearch('')} className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-white text-xs">✕</button>}
          </div>
        </div>

        {loading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {[...Array(6)].map((_, i) => (
              <div key={i} className="bg-white/5 border border-white/10 rounded-xl h-40 animate-pulse" />
            ))}
          </div>
        ) : filtered.length === 0 ? (
          <div className="text-center py-24">
            <p className="text-4xl mb-4">📦</p>
            <p className="text-slate-400 text-sm">{search ? 'No modules match your search.' : 'No modules available yet.'}</p>
            {(role === 'admin' || role === 'superadmin') && !search && (
              <button onClick={() => navigate('/admin')} className="mt-4 px-5 py-2.5 bg-blue-600 text-white text-sm font-bold rounded-xl hover:bg-blue-500 transition-colors">
                Create First Module
              </button>
            )}
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {filtered.map((m, i) => (
              <div key={m.moduleId}
                onClick={() => role === 'lecture' ? navigate('/lecturer') : navigate('/admin')}
                className="group bg-white/5 border border-white/10 rounded-xl overflow-hidden hover:border-white/25 hover:bg-white/8 transition-all cursor-pointer">
                {/* Color bar */}
                <div className={`h-1.5 bg-gradient-to-r ${COLORS[i % COLORS.length]}`} />
                <div className="p-6">
                  <div className="flex items-start justify-between mb-4">
                    <div className={`px-3 py-1 rounded-lg bg-gradient-to-r ${COLORS[i % COLORS.length]} text-white text-xs font-black shadow-lg`}>
                      {m.moduleId}
                    </div>
                    <span className="text-slate-600 text-xs">{(m.losList || []).length} LOs</span>
                  </div>
                  <h3 className="text-white font-bold text-sm leading-snug group-hover:text-blue-300 transition-colors">{m.moduleName}</h3>
                  <div className="mt-4 flex items-center justify-between">
                    <div className="flex gap-1">
                      {[...Array(Math.min((m.losList || []).length, 5))].map((_, j) => (
                        <div key={j} className="w-1.5 h-4 rounded-full bg-white/20" />
                      ))}
                    </div>
                    <span className="text-slate-500 text-xs group-hover:text-blue-400 transition-colors">View LOs →</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>
      <Footer />
    </div>
  )
}
