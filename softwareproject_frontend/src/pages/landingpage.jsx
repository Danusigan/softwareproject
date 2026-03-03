"use client"

import Header from '../components/header'
import Footer from '../components/footer'
import { useNavigate } from 'react-router-dom'
import { useEffect } from 'react'
import authService from '../services/authService'

export default function LandingPage() {
  const navigate = useNavigate();

  useEffect(() => {
    // ✅ If user is logged in, redirect to their dashboard
    if (authService.isLoggedIn()) {
      const userInfo = authService.getUserInfo();
      const userRole = userInfo?.userType?.toLowerCase()?.trim();

      if (userRole === 'superadmin' || userRole === 'super admin') {
        navigate('/super-admin-dashboard', { replace: true });
      } else if (userRole === 'admin') {
        navigate('/admin-dashboard', { replace: true });
      } else if (userRole === 'lecture') {
        navigate('/lecturer-dashboard', { replace: true });
      }
    }
  }, [navigate]);

  const handleAccessDashboard = () => {
    // If already logged in, navigate to dashboard; otherwise go to login
    if (authService.isLoggedIn()) {
      const userInfo = authService.getUserInfo();
      const userRole = userInfo?.userType?.toLowerCase()?.trim();

      if (userRole === 'superadmin' || userRole === 'super admin') {
        navigate('/super-admin-dashboard');
      } else if (userRole === 'admin') {
        navigate('/admin-dashboard');
      } else if (userRole === 'lecture') {
        navigate('/lecturer-dashboard');
      }
    } else {
      navigate('/loginpage');
    }
  };

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
                onClick={handleAccessDashboard}
                className="bg-blue-700 text-white px-6 py-3 rounded-full font-semibold hover:bg-blue-800 transition flex items-center gap-2"
                aria-label="Access your dashboard"
              >
                <span>▶</span> Access your dashboard
              </button>
            </div>

            {/* Right Illustration */}
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
      <section className="bg-gray-100 py-16">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            <div className="bg-white rounded-lg p-8 shadow-md">
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

      <Footer />
    </div>
  )
}
