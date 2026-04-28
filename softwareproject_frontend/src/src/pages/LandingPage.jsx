import { useNavigate } from 'react-router-dom'
import Header from '../components/Header'
import Footer from '../components/Footer'
import authService from '../services/authService'

const features = [
  { icon: '⚙️', title: 'Automated LO-PO Mapping', desc: 'Align course learning outcomes with program outcomes automatically with configurable weights.' },
  { icon: '📊', title: 'Attainment Analysis', desc: 'Calculate and visualize LO and PO attainment levels from student performance data.' },
  { icon: '📈', title: 'Trend Reports', desc: 'Track performance across batches and identify continuous quality improvement opportunities.' },
  { icon: '📁', title: 'Excel Import/Export', desc: 'Bulk import student marks via Excel and export attainment reports in standard formats.' },
  { icon: '🔒', title: 'Role-Based Access', desc: 'Superadmin, Admin, and Lecturer roles with fine-grained permissions on all operations.' },
  { icon: '✅', title: 'Approval Workflow', desc: 'Lecturer-submitted mappings flow through an admin approval process before taking effect.' },
]

export default function LandingPage() {
  const navigate = useNavigate()

  const handleAccess = () => {
    if (authService.isLoggedIn()) {
      const { userType } = authService.getUserInfo() || {}
      const role = (userType || '').toLowerCase()
      if (role === 'superadmin') navigate('/super-admin')
      else if (role === 'admin') navigate('/admin')
      else navigate('/lecturer')
    } else {
      navigate('/login')
    }
  }

  return (
    <div className="min-h-screen bg-[#0a0f1e] flex flex-col">
      <Header />

      {/* Hero */}
      <section className="relative overflow-hidden px-6 pt-24 pb-32">
        <div className="absolute inset-0 bg-gradient-to-b from-blue-600/10 via-transparent to-transparent" />
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[800px] h-[400px] bg-blue-600/5 rounded-full blur-3xl" />
        <div className="max-w-4xl mx-auto text-center relative">
          <div className="inline-flex items-center gap-2 px-4 py-1.5 bg-blue-500/10 border border-blue-500/30 rounded-full text-blue-400 text-xs font-medium mb-8">
            <span className="w-1.5 h-1.5 bg-blue-400 rounded-full animate-pulse" />
            DEIE · University of Ruhuna OBE System
          </div>
          <h1 className="text-5xl md:text-6xl font-black text-white leading-tight mb-6">
            Data-Driven<br />
            <span className="bg-gradient-to-r from-blue-400 to-indigo-400 bg-clip-text text-transparent">Quality Assurance</span>
          </h1>
          <p className="text-slate-400 text-lg max-w-2xl mx-auto mb-10 leading-relaxed">
            Automate your Outcome-Based Education process. Map LOs to POs, calculate attainment levels, and drive continuous quality improvement for the DEIE department.
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <button onClick={handleAccess} className="px-8 py-3.5 bg-blue-600 text-white font-bold rounded-xl hover:bg-blue-500 transition-all shadow-xl shadow-blue-500/25 text-sm">
              Access Dashboard →
            </button>
            <button onClick={() => navigate('/modules')} className="px-8 py-3.5 bg-white/5 border border-white/10 text-white font-bold rounded-xl hover:bg-white/10 transition-all text-sm">
              Browse Modules
            </button>
          </div>
        </div>
      </section>

      {/* Stats */}
      <section className="px-6 pb-20">
        <div className="max-w-4xl mx-auto grid grid-cols-2 md:grid-cols-4 gap-4">
          {[
            { value: '12', label: 'Washington Accord POs' },
            { value: '3', label: 'User Roles' },
            { value: '100%', label: 'Web-Based' },
            { value: 'Real-time', label: 'Attainment' },
          ].map(({ value, label }) => (
            <div key={label} className="bg-white/5 border border-white/10 rounded-xl p-5 text-center">
              <p className="text-2xl font-black text-white mb-1">{value}</p>
              <p className="text-slate-500 text-xs">{label}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Features */}
      <section className="px-6 pb-24">
        <div className="max-w-5xl mx-auto">
          <h2 className="text-white text-2xl font-bold text-center mb-12">Everything You Need</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {features.map(({ icon, title, desc }) => (
              <div key={title} className="bg-white/3 border border-white/8 rounded-xl p-6 hover:bg-white/5 hover:border-white/15 transition-all group">
                <div className="text-2xl mb-4">{icon}</div>
                <h3 className="text-white font-bold text-sm mb-2 group-hover:text-blue-400 transition-colors">{title}</h3>
                <p className="text-slate-500 text-xs leading-relaxed">{desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="px-6 pb-24">
        <div className="max-w-2xl mx-auto text-center bg-gradient-to-r from-blue-600/20 to-indigo-600/20 border border-blue-500/20 rounded-2xl p-12">
          <h2 className="text-white text-2xl font-bold mb-4">Ready to get started?</h2>
          <p className="text-slate-400 text-sm mb-8">Sign in with your institutional credentials to access the OBE dashboard.</p>
          <button onClick={handleAccess} className="px-8 py-3 bg-blue-600 text-white font-bold rounded-xl hover:bg-blue-500 transition-all shadow-xl shadow-blue-500/25 text-sm">
            Sign In Now
          </button>
        </div>
      </section>

      <Footer />
    </div>
  )
}
