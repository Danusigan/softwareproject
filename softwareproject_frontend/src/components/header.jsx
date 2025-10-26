"use client"

export default function Header() {
  return (
    <nav className="bg-blue-700 text-white">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-24">
          {/* Logo and Branding */}
          <div className="flex items-center gap-4">
            <div className="bg-white p-2 rounded">
              <div className="w-12 h-12 bg-blue-900 rounded flex items-center justify-center">
                <span className="text-white font-bold text-lg">LO</span>
              </div>
            </div>
            <div>
              <h1 className="text-lg font-bold">LO-PO</h1>
              <p className="text-sm">Faculty of Engineering</p>
              <p className="text-sm">University of Ruhuna</p>
            </div>
          </div>

          {/* Navigation Links */}
          <div className="flex items-center gap-8">
            <a href="#" className="hover:text-blue-200 transition">Home</a>
            <a href="#" className="hover:text-blue-200 transition">Signup</a>

            {/* Login Dropdown */}
            <div className="relative group">
              <button className="flex items-center gap-1 hover:text-blue-200 transition">
                Login <span>▼</span>
              </button>
              <div className="absolute right-0 mt-2 w-32 bg-blue-600 rounded shadow-lg opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-opacity duration-300">
                <a href="#" className="block px-4 py-2 hover:bg-blue-800 rounded-t">Admin</a>
              </div>
            </div>

            <button className="bg-gray-300 text-gray-800 px-4 py-2 rounded hover:bg-gray-400 transition">
              Lecturer
            </button>
          </div>
        </div>
      </div>
    </nav>
  )
}