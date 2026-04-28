export default function Footer() {
  return (
    <footer className="bg-[#0a0f1e] border-t border-white/5 mt-auto">
      <div className="max-w-7xl mx-auto px-6 py-10">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-8 mb-8">
          <div>
            <div className="flex items-center gap-2 mb-3">
              <div className="w-7 h-7 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-lg flex items-center justify-center">
                <span className="text-white font-black text-[10px]">OBE</span>
              </div>
              <span className="text-white font-bold text-sm">LO-PO Analytics</span>
            </div>
            <p className="text-slate-500 text-xs leading-relaxed">Automated Outcome-Based Education system for the Faculty of Engineering, University of Ruhuna.</p>
          </div>
          <div>
            <p className="text-slate-400 text-xs font-semibold uppercase tracking-wider mb-3">Location</p>
            <p className="text-slate-500 text-xs leading-relaxed">Faculty of Engineering,<br />University of Ruhuna,<br />Hapugala, Galle 80000, Sri Lanka</p>
          </div>
          <div>
            <p className="text-slate-400 text-xs font-semibold uppercase tracking-wider mb-3">Contact</p>
            <p className="text-slate-500 text-xs">+(94) 91 224 5765</p>
            <p className="text-slate-500 text-xs mt-1">ar@eng.ruh.ac.lk</p>
            <div className="flex items-center gap-1.5 mt-3">
              <div className="w-1.5 h-1.5 bg-emerald-500 rounded-full animate-pulse" />
              <span className="text-emerald-500 text-xs">System operational</span>
            </div>
          </div>
        </div>
        <div className="pt-6 border-t border-white/5 flex justify-between items-center">
          <p className="text-slate-600 text-xs">© 2025 Faculty of Engineering, University of Ruhuna. All rights reserved.</p>
        </div>
      </div>
    </footer>
  )
}
