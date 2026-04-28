import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import axios from 'axios'
import authService from '../services/authService'
import Header from '../components/Header'
import Footer from '../components/Footer'

export default function LoginPage() {
  const [form, setForm] = useState({ username: '', password: '', userRole: '' })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [showPass, setShowPass] = useState(false)
  const [rememberMe, setRememberMe] = useState(false)
  const navigate = useNavigate()

  const handleChange = (e) => {
    setForm(f => ({ ...f, [e.target.name]: e.target.value }))
    setError('')
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!form.userRole) { setError('Please select your role.'); return }
    setLoading(true)
    setError('')
    try {
      const res = await axios.post('http://localhost:8080/api/auth/login', {
        userID: form.username,
        password: form.password,
        userType: form.userRole,
      })

      if (res.data?.status === 'SUCCESS') {
        const { token, userId, userType } = res.data
        authService.storeLogin(token, userId, userType, rememberMe)
        const role = (userType || '').toLowerCase().trim()
        if (role === 'superadmin') navigate('/super-admin', { replace: true })
        else if (role === 'admin') navigate('/admin', { replace: true })
        else navigate('/lecturer', { replace: true })
      } else {
        setError(res.data?.message || 'Login failed.')
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid credentials. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-[#0a0f1e] flex flex-col">
      <Header />
      <main className="flex-1 flex items-center justify-center px-6 py-16">
        <div className="w-full max-w-sm">
          {/* Card */}
          <div className="bg-white/5 border border-white/10 rounded-2xl p-8 backdrop-blur">
            <div className="text-center mb-8">
              <div className="w-12 h-12 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-xl flex items-center justify-center mx-auto mb-4 shadow-lg shadow-blue-500/30">
                <span className="text-white font-black text-sm">OBE</span>
              </div>
              <h1 className="text-white text-2xl font-black">Welcome back</h1>
              <p className="text-slate-500 text-sm mt-1">Sign in to your account</p>
            </div>

            {error && (
              <div className="mb-5 p-3 bg-red-500/10 border border-red-500/30 rounded-xl flex items-start gap-2">
                <span className="text-red-400 text-xs mt-0.5">⚠</span>
                <p className="text-red-400 text-xs">{error}</p>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              {/* Role */}
              <div>
                <label className="block text-slate-400 text-xs font-medium mb-1.5">Role</label>
                <select
                  name="userRole"
                  value={form.userRole}
                  onChange={handleChange}
                  required
                  className="w-full bg-white/5 border border-white/10 text-white rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500/50 transition-colors appearance-none"
                >
                  <option value="" className="bg-[#0a0f1e]">Select your role</option>
                  <option value="superadmin" className="bg-[#0a0f1e]">Superadmin</option>
                  <option value="admin" className="bg-[#0a0f1e]">Admin</option>
                  <option value="lecture" className="bg-[#0a0f1e]">Lecturer</option>
                </select>
              </div>

              {/* Username */}
              <div>
                <label className="block text-slate-400 text-xs font-medium mb-1.5">Username</label>
                <input
                  name="username"
                  type="text"
                  value={form.username}
                  onChange={handleChange}
                  placeholder="Enter username"
                  required
                  className="w-full bg-white/5 border border-white/10 text-white placeholder-slate-600 rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500/50 transition-colors"
                />
              </div>

              {/* Password */}
              <div>
                <label className="block text-slate-400 text-xs font-medium mb-1.5">Password</label>
                <div className="relative">
                  <input
                    name="password"
                    type={showPass ? 'text' : 'password'}
                    value={form.password}
                    onChange={handleChange}
                    placeholder="Enter password"
                    required
                    className="w-full bg-white/5 border border-white/10 text-white placeholder-slate-600 rounded-xl px-4 py-2.5 pr-10 text-sm focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500/50 transition-colors"
                  />
                  <button type="button" onClick={() => setShowPass(s => !s)} className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300 transition-colors text-xs">
                    {showPass ? '🙈' : '👁'}
                  </button>
                </div>
              </div>

              {/* Remember + Forgot */}
              <div className="flex items-center justify-between">
                <label className="flex items-center gap-2 cursor-pointer group">
                  <input type="checkbox" checked={rememberMe} onChange={e => setRememberMe(e.target.checked)} className="w-3.5 h-3.5 accent-blue-500" />
                  <span className="text-slate-500 text-xs group-hover:text-slate-400 transition-colors">Remember me</span>
                </label>
                <span className="text-blue-400 text-xs cursor-pointer hover:text-blue-300 transition-colors">Forgot password?</span>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full py-3 bg-blue-600 text-white font-bold rounded-xl hover:bg-blue-500 disabled:opacity-50 disabled:cursor-not-allowed transition-all shadow-lg shadow-blue-500/25 text-sm mt-2"
              >
                {loading ? (
                  <span className="flex items-center justify-center gap-2">
                    <svg className="animate-spin w-4 h-4" viewBox="0 0 24 24" fill="none">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                    </svg>
                    Signing in...
                  </span>
                ) : 'Sign In'}
              </button>
            </form>
          </div>

          <p className="text-center text-slate-600 text-xs mt-6">
            LO-PO Analytics System · University of Ruhuna
          </p>
        </div>
      </main>
      <Footer />
    </div>
  )
}
