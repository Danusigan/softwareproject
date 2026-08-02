import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../components/header';
import Footer from '../components/footer';

export default function SuperAdminDashboard() {
    const navigate = useNavigate();

    // Verify user is superadmin on mount
    useEffect(() => {
        const userType = localStorage.getItem('userType');
        console.log('SuperAdmin Dashboard - User Type:', userType);
        
        const normalizedType = userType?.toLowerCase?.() || '';
        const isSuperAdmin = normalizedType === 'superadmin' || normalizedType === 'super admin' || normalizedType === 'super-admin';
        
        if (!isSuperAdmin) {
            console.log('Unauthorized! Redirecting to home. Normalized type:', normalizedType);
            navigate('/');
        }
    }, [navigate]);

    return (
        <div className="min-h-screen bg-gray-50">
            <Header />

            {/* Main Content */}
            <div className="max-w-7xl mx-auto px-4 py-8">
                <div className="flex items-center justify-between mb-8">
                    <h1 className="text-3xl font-bold text-gray-800">SuperAdmin Dashboard</h1>
                </div>

                {/* Welcome Section */}
                <div className="bg-gradient-to-r from-purple-600 to-blue-600 text-white rounded-xl shadow-lg p-8 mb-8">
                    <h2 className="text-2xl font-bold mb-2">Welcome, Superadmin!</h2>
                    <p className="text-purple-100">Manage your administrators and control the system.</p>
                </div>

                {/* Modern Cards */}
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
                    {/* Manage Admins Card */}
                    <div
                        onClick={() => navigate('/manage-admins')}
                        className="bg-white rounded-xl shadow-lg p-8 cursor-pointer transform transition-all duration-300 hover:scale-105 hover:shadow-2xl border-2 border-transparent hover:border-purple-500"
                    >
                        <div className="flex items-center justify-center mb-4">
                            <div className="w-16 h-16 bg-purple-100 rounded-full flex items-center justify-center">
                                <svg className="w-8 h-8 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 20h5v-2a4 4 0 00-3-3.87M9 20H4v-2a4 4 0 013-3.87m6-1.13a4 4 0 10-4-4 4 4 0 004 4zm6-4a4 4 0 11-1.33-2.98" />
                                </svg>
                            </div>
                        </div>
                        <h2 className="text-2xl font-bold text-center text-gray-800 mb-2">Manage Admins</h2>
                        <p className="text-center text-gray-600">Add, edit & remove system administrators</p>
                    </div>

                    {/* System Info Card */}
                    <div className="bg-white rounded-xl shadow-lg p-8 border-2 border-gray-200">
                        <div className="flex items-center justify-center mb-4">
                            <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center">
                                <svg className="w-8 h-8 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                                </svg>
                            </div>
                        </div>
                        <h2 className="text-2xl font-bold text-center text-gray-800 mb-2">System Status</h2>
                        <p className="text-center text-gray-600">All systems operational</p>
                        <div className="mt-4 text-center">
                            <span className="inline-block bg-green-100 text-green-700 px-3 py-1 rounded-full text-sm font-semibold">Active</span>
                        </div>
                    </div>

                    {/* Management Info Card */}
                    <div className="bg-white rounded-xl shadow-lg p-8 border-2 border-gray-200">
                        <div className="flex items-center justify-center mb-4">
                            <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center">
                                <svg className="w-8 h-8 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                                </svg>
                            </div>
                        </div>
                        <h2 className="text-2xl font-bold text-center text-gray-800 mb-2">Admin Control</h2>
                        <p className="text-center text-gray-600">Full system management capabilities</p>
                    </div>
                </div>
            </div>

            <Footer />
        </div>
    );
}
