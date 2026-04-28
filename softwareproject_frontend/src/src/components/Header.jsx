import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useState, useEffect } from 'react'
import authService from '../services/authService'

export default function Header() {
  const navigate = useNavigate()
  const location = useLocation()
  const [user, setUser] = useState(null)
  const [scrolled, setScrolled] = useState(false)

  useEffect(() => {
    setUser(authService.getUserInfo())
    const interval = setInterval(() => {
      if (authService.isLoggedIn()) {
        if (authService.getTimeRemaining() <= 0) { handleLogout(); return }
      }
      setUser(authService.getUserInfo())
    }, 5000)
    return () => clearInterval(interval)
  }, [])

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 10)
    window.addEventListener('scroll', onScroll)
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  const handleLogout = () => {
    authService.logout()
    setUser(null)
    navigate('/login', { replace: true })
  }

  const getDashboardLink = () => {
    if (!user) return '/login'
    const role = (user.userType || '').toLowerCase()
    if (role === 'superadmin') return '/super-admin'
    if (role === 'admin') return '/admin'
    return '/lecturer'
  }

  return (
    <header className={`sticky top-0 z-50 transition-all duration-300 ${scrolled ? 'bg-[#0f172a]/95 backdrop-blur-md shadow-xl shadow-black/20' : 'bg-[#0f172a]'}`}>
      <div className="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
        {/* Logo */}
        <button onClick={() => navigate('/')} className="flex items-center gap-3 group">
          <div className="w-8 h-8 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-lg flex items-center justify-center shadow-lg shadow-blue-500/30 group-hover:scale-105 transition-transform">
            <span className="text-white font-black text-xs">OBE</span>
          </div>
          <div>
            <p className="text-white font-bold text-sm leading-none">LO-PO Analytics</p>
            <p className="text-slate-400 text-[10px] leading-none mt-0.5">University of Ruhuna</p>
          </div>
        </button>

        {/* Nav */}
        <nav className="hidden md:flex items-center gap-1">
          {[{ label: 'Home', to: '/' }, { label: 'Modules', to: '/modules' }].map(({ label, to }) => (
            <Link key={to} to={to} className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${location.pathname === to ? 'bg-white/10 text-white' : 'text-slate-400 hover:text-white hover:bg-white/5'}`}>
              {label}
            </Link>
          ))}
          {user && (
            <>
              <Link to={getDashboardLink()} className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${location.pathname.includes('dashboard') || location.pathname.includes('admin') || location.pathname.includes('lecturer') ? 'bg-white/10 text-white' : 'text-slate-400 hover:text-white hover:bg-white/5'}`}>
                Dashboard
              </Link>
              {(user.userType === 'admin' || user.userType === 'superadmin') && (
                <Link to="/program-outcomes" className="px-3 py-1.5 rounded-lg text-sm font-medium text-slate-400 hover:text-white hover:bg-white/5 transition-colors">
                  Program Outcomes
                </Link>
              )}
              <Link to="/lo-po-mappings" className="px-3 py-1.5 rounded-lg text-sm font-medium text-slate-400 hover:text-white hover:bg-white/5 transition-colors">
                LO-PO Mappings
              </Link>
            </>
          )}
        </nav>

        {/* Auth */}
        <div className="flex items-center gap-3">
          {user ? (
            <>
              <div className="hidden sm:flex items-center gap-2 px-3 py-1.5 bg-white/5 rounded-lg border border-white/10">
                <div className="w-6 h-6 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-full flex items-center justify-center">
                  <span className="text-white text-[10px] font-bold">{user.username?.[0]?.toUpperCase()}</span>
                </div>
                <div>
                  <p className="text-white text-xs font-semibold leading-none">{user.username}</p>
                  <p className="text-slate-400 text-[10px] leading-none mt-0.5 capitalize">{user.userType}</p>
                </div>
              </div>
              <button onClick={handleLogout} className="px-3 py-1.5 bg-red-500/10 border border-red-500/30 text-red-400 rounded-lg text-xs font-semibold hover:bg-red-500/20 transition-colors">
                Logout
              </button>
            </>
          ) : (
            <Link to="/login" className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-semibold hover:bg-blue-500 transition-colors shadow-lg shadow-blue-500/20">
              Sign In
            </Link>
          )}
        </div>
      </div>
    </header>
  )
}
