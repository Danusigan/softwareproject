"use client"

import Header from '../components/header'
import Footer from '../components/footer'

export default function LandingPage() {
  return (
    <div className="min-h-screen bg-white">
      <Header />
      
      {/* Hero Section */}
      <section className="bg-gray-50 py-16">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
            {/* Left Content */}
            <div>
              <h2 className="text-4xl lg:text-5xl font-bold text-red-600 mb-6 leading-tight">
                Data-Driven Quality: Automate Your Outcome-Based Education (OBE) Assurance.
              </h2>
              <p className="text-gray-700 text-lg mb-8 leading-relaxed">
                The dedicated web-based system for the DEIE, University of Ruhuna, to streamline LO-PO mapping,
                calculate attainment levels, and drive Continuous Quality Improvement (CQI).
              </p>
              <button 
                className="bg-blue-700 text-white px-6 py-3 rounded-full font-semibold hover:bg-blue-800 transition flex items-center gap-2"
                aria-label="Access your dashboard"
              >
                <span>▶</span> Access your dashboard
              </button>
            </div>

            {/* Right Illustration - FIXED GRADIENT */}
            <div className="flex justify-center">
              <div className="bg-linear-to-br from-blue-100 to-gray-100 rounded-lg p-8 w-full max-w-md h-80 flex items-center justify-center border border-gray-300">
                <div className="bg-white rounded-lg w-full h-full flex items-center justify-center">
                  <span className="text-blue-600 font-semibold text-center p-4">
                    Dashboard Preview<br />
                    (Image would go here)
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Section Title */}
      <section className="py-12 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <h3 className="text-3xl font-bold text-center text-gray-900">
            Transforming Quality Assurance for DEIE
          </h3>
        </div>
      </section>

      {/* Features Cards */}
      <section className="bg-gray-100 py-16"> {/* Changed from gray-200 to gray-100 for better contrast */}
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            <div className="bg-white rounded-lg p-8 shadow-md"> {/* Changed to white with shadow */}
              <h4 className="text-xl font-bold text-blue-700 mb-4">Automated LO-PO Mapping</h4>
              <div className="text-4xl mb-4" aria-label="Automation gear icon">⚙️</div>
              <p className="text-gray-700">
                Ensure seamless alignment between course objectives (LOs) and overall program goals (POs).
              </p>
            </div>
            <div className="bg-white rounded-lg p-8 shadow-md">
              <h4 className="text-xl font-bold text-blue-700 mb-4">Precision Attainment Analysis</h4>
              <div className="text-4xl mb-4" aria-label="Analytics chart icon">📊</div>
              <p className="text-gray-700">
                Automatically calculate and visualize LO and PO attainment levels based on student performance data.
              </p>
            </div>
            <div className="bg-white rounded-lg p-8 shadow-md">
              <h4 className="text-xl font-bold text-blue-700 mb-4">Continuous Quality Improvement (CQI)</h4>
              <div className="text-4xl mb-4" aria-label="Growth chart icon">📈</div>
              <p className="text-gray-700">
                Ensure ongoing improvement of educational outcomes with systematic monitoring and adjustments.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Tools Section - FIXED GRADIENT */}
      <section className="py-16 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <h3 className="text-3xl font-bold text-gray-900 mb-12">
            Powerful Tools for Administrators and Faculty
          </h3>

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
            <div className="bg-linear-to-br from-blue-400 to-blue-600 rounded-lg p-8 shadow-lg">
              <div className="grid grid-cols-3 gap-4">
                {[1, 2, 3, 4, 5, 6].map((item) => (
                  <div key={item} className="bg-gray-900 rounded aspect-square flex items-center justify-center shadow-md">
                    <div className="text-white text-center">
                      <div className="text-2xl mb-2" aria-label="Dashboard icon">📊</div>
                      <p className="text-xs">Dashboard</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="space-y-6">
              <div className="flex items-start gap-4">
                <div className="text-green-500 text-2xl">✓</div>
                <div>
                  <h4 className="font-bold text-gray-900 text-lg">Secure Data Management</h4>
                  <p className="text-gray-600">Enterprise-grade security for all your data</p>
                </div>
              </div>

              <div className="flex items-start gap-4">
                <div className="text-green-500 text-2xl">✓</div>
                <div>
                  <h4 className="font-bold text-gray-900 text-lg">Performance Tracking</h4>
                  <p className="text-gray-600">Real-time monitoring and analytics</p>
                </div>
              </div>

              <div className="flex items-start gap-4">
                <div className="text-green-500 text-2xl">✓</div>
                <div>
                  <h4 className="font-bold text-gray-900 text-lg">Exportable Reports</h4>
                  <p className="text-gray-600">Generate comprehensive reports in multiple formats</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <Footer />
    </div>
  )
}