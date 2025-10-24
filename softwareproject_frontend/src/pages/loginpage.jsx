"use client"

export default function LoginPage() {
  return (
    <div className="min-h-screen bg-gray-50 py-8">
      {/* Header */}
      <div className="bg-blue-700 text-white py-6 mb-8">
        <div className="max-w-4xl mx-auto px-4">
          <div className="flex items-center gap-4">
            <div className="bg-white p-2 rounded">
              <div className="w-12 h-12 bg-blue-900 rounded flex items-center justify-center">
                <span className="text-white font-bold">LO</span>
              </div>
            </div>
            <div>
              <h1 className="text-xl font-bold">LO-PO</h1>
              <p className="text-sm">Faculty of Engineering</p>
              <p className="text-sm">University of Ruhuna</p>
            </div>
          </div>
        </div>
      </div>

      {/* Navigation */}
      <div className="max-w-4xl mx-auto px-4 mb-8">
        <div className="flex justify-center space-x-8">
          <a href="#" className="text-gray-700 hover:text-blue-600">Home</a>
          <a href="#" className="text-gray-700 hover:text-blue-600">Signup</a>
          <a href="#" className="text-blue-600 font-semibold border-b-2 border-blue-600">Login</a>
        </div>
      </div>

      {/* Main Content */}
      <div className="max-w-6xl mx-auto px-4">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          
          {/* Login Form */}
          <div className="bg-white p-8 rounded-lg shadow-md">
            <h2 className="text-2xl font-bold text-center mb-6">Login</h2>
            
            <div className="space-y-4">
              <div>
                <div className="flex items-center mb-2">
                  <input type="checkbox" className="mr-2" />
                  <label>Enter Your Username</label>
                </div>
                <input type="text" className="w-full p-3 border rounded" placeholder="Username" />
              </div>

              <div>
                <div className="flex items-center mb-2">
                  <input type="checkbox" className="mr-2" defaultChecked />
                  <label>Enter Your Password</label>
                </div>
                <input type="password" className="w-full p-3 border rounded" placeholder="Password" />
              </div>

              <div className="flex justify-between items-center">
                <label className="flex items-center">
                  <input type="checkbox" className="mr-2" />
                  <span>Remember me?</span>
                </label>
                <a href="#" className="text-blue-600 text-sm">Forget Password</a>
              </div>

              <div>
                <div className="flex items-center mb-2">
                  <input type="checkbox" className="mr-2" />
                  <label>Login</label>
                </div>
                <button className="w-full bg-blue-700 text-white p-3 rounded font-semibold hover:bg-blue-800">
                  Login
                </button>
              </div>
            </div>
          </div>

          {/* Contact Info */}
          <div className="space-y-4">
            <div className="bg-white p-6 rounded-lg shadow-md">
              <h3 className="font-bold mb-2">Postal Address</h3>
              <p className="text-sm">
                Faculty of Engineering, University of Ruhuna,<br />
                Hapugala, Galle, Sri Lanka. 80000
              </p>
            </div>
            
            <div className="bg-white p-6 rounded-lg shadow-md">
              <h3 className="font-bold mb-2">Phone Number</h3>
              <p className="text-sm">
                + (94) 912245765,<br />
                + (94) 912245766,<br />
                + (94) 912245767
              </p>
            </div>
            
            <div className="bg-white p-6 rounded-lg shadow-md">
              <h3 className="font-bold mb-2">Other</h3>
              <p className="text-sm">
                Fax : +94 912245762<br />
                Email : ar@eng.ruh.ac.lk
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}