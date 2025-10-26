"use client"

export default function ForgotPasswordPage() {
  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header Section */}
      <header className="bg-blue-700 text-white py-6">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center gap-4">
            <div className="bg-white p-2 rounded">
              <div className="w-12 h-12 bg-blue-900 rounded flex items-center justify-center">
                <span className="text-white font-bold text-lg">LO</span>
              </div>
            </div>
            <div>
              <h1 className="text-xl font-bold">LO-PO</h1>
              <p className="text-sm">Faculty of Engineering</p>
              <p className="text-sm">University of Ruhuna</p>
            </div>
          </div>
        </div>
      </header>

      {/* Navigation */}
      <nav className="bg-white shadow-sm">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-center space-x-8 py-4">
            <a href="#" className="text-gray-700 hover:text-blue-600 transition">Home</a>
            <a href="#" className="text-gray-700 hover:text-blue-600 transition">Signup</a>
            <a href="#" className="text-gray-700 hover:text-blue-600 transition">Login</a>
          </div>
        </div>
      </nav>

      {/* Main Content */}
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 items-start">
          
          {/* Forgot Password Form Section */}
          <div className="bg-white rounded-lg shadow-md p-8">
            <h2 className="text-2xl font-bold text-gray-900 mb-6 text-center">Forgotten password ??</h2>
            
            <form className="space-y-6">
              {/* Username Field */}
              <div className="space-y-2">
                <div className="flex items-center">
                  <input 
                    type="checkbox" 
                    className="mr-3 h-4 w-4 text-blue-600" 
                    defaultChecked={false} 
                  />
                  <label className="text-sm font-medium text-gray-700">Enter you User Name</label>
                </div>
                <input
                  type="text"
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition"
                  placeholder="Enter your username"
                />
              </div>

              {/* Email Field */}
              <div className="space-y-2">
                <div className="flex items-center">
                  <input 
                    type="checkbox" 
                    className="mr-3 h-4 w-4 text-blue-600" 
                    defaultChecked={true} 
                  />
                  <label className="text-sm font-medium text-gray-700">Enter you email address</label>
                </div>
                <input
                  type="email"
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition"
                  placeholder="Enter your email address"
                />
              </div>

              {/* Submit Button */}
              <div className="space-y-2">
                <div className="flex items-center">
                  <input 
                    type="checkbox" 
                    className="mr-3 h-4 w-4 text-blue-600" 
                    defaultChecked={false} 
                  />
                  <label className="text-sm font-medium text-gray-700">Submit</label>
                </div>
                <button
                  type="submit"
                  className="w-full bg-blue-700 text-white py-3 px-4 rounded-lg font-semibold hover:bg-blue-800 focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 transition"
                >
                  Submit
                </button>
              </div>
            </form>

            {/* Back to Login Link */}
            <div className="mt-6 text-center">
              <a href="#" className="text-blue-600 hover:text-blue-800 text-sm transition">
                ← Back to Login
              </a>
            </div>
          </div>

          {/* Contact Information Section */}
          <div className="space-y-6">
            {/* Postal Address */}
            <div className="bg-white rounded-lg shadow-md p-6">
              <h3 className="font-bold text-lg text-gray-900 mb-3">Postal Address</h3>
              <p className="text-gray-700">
                Faculty of Engineering, University of Ruhuna,<br />
                Hapugala, Galle, Sri Lanka.<br />
                80000
              </p>
            </div>

            {/* Phone Number */}
            <div className="bg-white rounded-lg shadow-md p-6">
              <h3 className="font-bold text-lg text-gray-900 mb-3">Phone Number</h3>
              <p className="text-gray-700">
                + (94) 912245765,<br />
                + (94) 912245766,<br />
                + (94) 912245767
              </p>
            </div>

            {/* Other Contact Info */}
            <div className="bg-white rounded-lg shadow-md p-6">
              <h3 className="font-bold text-lg text-gray-900 mb-3">Other</h3>
              <p className="text-gray-700">
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