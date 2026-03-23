export default function Footer() {
  return (
    <footer className="bg-slate-900 text-slate-200 mt-20 border-t border-slate-800">
      <div className="max-w-7xl mx-auto px-6 sm:px-10 py-14">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-10">
          {/* Logo & Intro */}
          <div className="col-span-1 md:col-span-1 md:border-r md:border-slate-800 md:pr-8">
            <div className="flex items-center gap-3 mb-5">
              <div className="bg-indigo-600 p-1.5 rounded-lg shadow-sm">
                <div className="w-8 h-8 rounded flex items-center justify-center">
                  <span className="text-white font-semibold text-sm">LO</span>
                </div>
              </div>
              <h3 className="font-semibold tracking-tight text-lg text-white">LO-PO Analytics</h3>
            </div>
            <p className="text-slate-400 text-sm leading-relaxed">
              Empowering academic excellence through automated attainment mapping and curriculum analysis.
            </p>
          </div>

          {/* Postal Address */}
          <div>
            <h3 className="text-sm font-semibold text-white mb-4">Location</h3>
            <div className="space-y-1.5 text-sm text-slate-300">
              <p>Faculty of Engineering,</p>
              <p>University of Ruhuna,</p>
              <p>Hapugala, Galle, 80000</p>
              <p>Sri Lanka.</p>
            </div>
          </div>

          {/* Contact Details */}
          <div>
            <h3 className="text-sm font-semibold text-white mb-4">Contact</h3>
            <div className="space-y-4">
              <div className="flex flex-col">
                <span className="text-xs font-medium text-slate-500">Phone</span>
                <p className="text-sm text-slate-300">+(94) 91 224 5765-7</p>
              </div>
              <div className="flex flex-col">
                <span className="text-xs font-medium text-slate-500">Email</span>
                <p className="text-sm text-slate-300">ar@eng.ruh.ac.lk</p>
              </div>
            </div>
          </div>

          {/* System Info */}
          <div className="bg-slate-800/70 rounded-2xl p-5 border border-slate-700">
            <h3 className="text-sm font-semibold text-white mb-3">System Status</h3>
            <p className="text-xs text-slate-400 leading-relaxed mb-4">
              System is operational and synced with UOR curriculum database.
            </p>
            <div className="flex items-center gap-2">
              <div className="w-1.5 h-1.5 bg-green-500 rounded-full animate-pulse"></div>
              <span className="text-xs font-medium text-green-400">Secure connection active</span>
            </div>
          </div>
        </div>

        {/* Bottom Line */}
        <div className="mt-12 pt-6 border-t border-slate-800 flex flex-col md:flex-row justify-between items-center gap-4">
          <p className="text-slate-500 text-xs">
            © 2026 Faculty of Engineering. All Rights Reserved.
          </p>
          <div className="flex gap-6">
            <span className="text-slate-500 text-xs hover:text-slate-200 cursor-pointer transition-colors">Privacy Policy</span>
            <span className="text-slate-500 text-xs hover:text-slate-200 cursor-pointer transition-colors">Terms of Use</span>
          </div>
        </div>
      </div>
    </footer>
  )
}
