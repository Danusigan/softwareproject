"use client"

export default function Footer() {
  return (
    <footer className="bg-[#111827] text-white mt-24 border-t border-white/5">
      <div className="max-w-7xl mx-auto px-6 sm:px-10 py-16">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-12">
          {/* Logo & Intro */}
          <div className="col-span-1 md:col-span-1 border-r border-white/10 pr-8">
            <div className="flex items-center gap-3 mb-6">
              <div className="bg-white p-1.5 rounded-lg shadow-md">
                <div className="w-8 h-8 bg-[#1e40af] rounded flex items-center justify-center">
                  <span className="text-white font-black text-sm">LO</span>
                </div>
              </div>
              <h3 className="font-black tracking-tight text-lg">LO-PO</h3>
            </div>
            <p className="text-gray-400 text-xs font-medium leading-relaxed">
              Empowering academic excellence through automated attainment mapping and curriculum analysis.
            </p>
          </div>

          {/* Postal Address */}
          <div>
            <h3 className="text-[10px] font-black text-blue-400 uppercase tracking-[0.2em] mb-6">Location</h3>
            <div className="space-y-2 text-sm text-gray-300 font-medium">
              <p>Faculty of Engineering,</p>
              <p>University of Ruhuna,</p>
              <p>Hapugala, Galle, 80000</p>
              <p>Sri Lanka.</p>
            </div>
          </div>

          {/* Contact Details */}
          <div>
            <h3 className="text-[10px] font-black text-blue-400 uppercase tracking-[0.2em] mb-6">Communications</h3>
            <div className="space-y-4">
              <div className="flex flex-col">
                <span className="text-[10px] font-bold text-gray-500 uppercase">Hotlines</span>
                <p className="text-sm text-gray-300 font-medium">+(94) 91 224 5765-7</p>
              </div>
              <div className="flex flex-col">
                <span className="text-[10px] font-bold text-gray-500 uppercase">Email Dispatch</span>
                <p className="text-sm text-gray-300 font-medium">ar@eng.ruh.ac.lk</p>
              </div>
            </div>
          </div>

          {/* System Info */}
          <div className="bg-white/5 rounded-2xl p-6 border border-white/10">
            <h3 className="text-[10px] font-black text-white uppercase tracking-[0.2em] mb-4">Verification</h3>
            <p className="text-[10px] text-gray-400 font-bold leading-relaxed mb-4 uppercase tracking-wider">
              System is operational and synced with UOR curriculum database.
            </p>
            <div className="flex items-center gap-2">
              <div className="w-1.5 h-1.5 bg-green-500 rounded-full animate-pulse"></div>
              <span className="text-[10px] font-black text-green-500 uppercase">Secure Link</span>
            </div>
          </div>
        </div>

        {/* Bottom Line */}
        <div className="mt-16 pt-8 border-t border-white/10 flex flex-col md:flex-row justify-between items-center gap-6">
          <p className="text-gray-500 text-[10px] font-bold uppercase tracking-[0.2em]">
            © 2026 Faculty of Engineering. All Rights Reserved.
          </p>
          <div className="flex gap-8">
            <span className="text-gray-500 text-[10px] font-bold hover:text-white cursor-pointer transition-colors uppercase tracking-widest">Privacy Policy</span>
            <span className="text-gray-500 text-[10px] font-bold hover:text-white cursor-pointer transition-colors uppercase tracking-widest">Usage Terms</span>
          </div>
        </div>
      </div>
    </footer>
  )
}
