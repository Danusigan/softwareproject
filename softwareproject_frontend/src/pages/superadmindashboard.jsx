import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../components/header';
import Footer from '../components/footer';
import axios from 'axios';

export default function SuperAdminDashboard() {
    const [sidePanelOpen, setSidePanelOpen] = useState(null); // 'admin' only
    const navigate = useNavigate();

    // Admin form states
    const [adminData, setAdminData] = useState({
        username: '',
        email: '',
        password: ''
    });

    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState({ type: '', text: '' });

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

    const handleAdminSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setMessage({ type: '', text: '' });

        try {
            const token = localStorage.getItem('token');
            const res = await axios.post(
                'http://localhost:8080/api/auth/add-user',
                {
                    userID: adminData.username,
                    email: adminData.email,
                    password: adminData.password,
                    usertype: 'Admin'
                },
                {
                    headers: {
                        'Authorization': `Bearer ${token}`
                    }
                }
            );

            if (res.data.status === 'SUCCESS') {
                setMessage({ type: 'success', text: 'Admin added successfully!' });
                setAdminData({ username: '', email: '', password: '' });
                setTimeout(() => setSidePanelOpen(null), 2000);
            }
        } catch (err) {
            setMessage({ 
                type: 'error', 
                text: err.response?.data?.message || 'Failed to add admin' 
            });
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-gray-50">
            <Header />

            {/* Main Content */}
            <div className="max-w-7xl mx-auto px-4 py-8">
                <div className="flex items-center justify-between mb-8">
                    <h1 className="text-3xl font-bold text-gray-800">SuperAdmin Dashboard</h1>
                </div>

                {message.text && (
                    <div className={`mb-4 p-4 rounded-lg ${
                        message.type === 'success' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
                    }`}>
                        {message.text}
                    </div>
                )}

                {/* Welcome Section */}
                <div className="bg-gradient-to-r from-purple-600 to-blue-600 text-white rounded-xl shadow-lg p-8 mb-8">
                    <h2 className="text-2xl font-bold mb-2">Welcome, Superadmin!</h2>
                    <p className="text-purple-100">Manage your administrators and control the system. Use the panel below to add new admins to the system.</p>
                </div>

                {/* Modern Cards */}
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
                    {/* Add Admin Card */}
                    <div
                        onClick={() => setSidePanelOpen('admin')}
                        className="bg-white rounded-xl shadow-lg p-8 cursor-pointer transform transition-all duration-300 hover:scale-105 hover:shadow-2xl border-2 border-transparent hover:border-purple-500"
                    >
                        <div className="flex items-center justify-center mb-4">
                            <div className="w-16 h-16 bg-purple-100 rounded-full flex items-center justify-center">
                                <svg className="w-8 h-8 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
                                </svg>
                            </div>
                        </div>
                        <h2 className="text-2xl font-bold text-center text-gray-800 mb-2">Add Administrator</h2>
                        <p className="text-center text-gray-600">Click to add a new administrator to the system</p>
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

            {/* Side Panel for Add Admin */}
            <div
                className={`fixed top-0 right-0 h-full w-full md:w-96 bg-white shadow-2xl transform transition-transform duration-300 ease-in-out z-50 ${
                    sidePanelOpen === 'admin' ? 'translate-x-0' : 'translate-x-full'
                }`}
            >
                <div className="p-6 h-full overflow-y-auto">
                    <div className="flex justify-between items-center mb-6">
                        <h2 className="text-2xl font-bold text-gray-800">Add Administrator</h2>
                        <button
                            onClick={() => setSidePanelOpen(null)}
                            className="text-gray-500 hover:text-gray-700"
                        >
                            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                            </svg>
                        </button>
                    </div>

                    {message.text && (
                        <div className={`mb-4 p-3 rounded-lg ${
                            message.type === 'success' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
                        }`}>
                            {message.text}
                        </div>
                    )}

                    <form onSubmit={handleAdminSubmit} className="space-y-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">Username</label>
                            <input
                                type="text"
                                value={adminData.username}
                                onChange={(e) => setAdminData({ ...adminData, username: e.target.value })}
                                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                                placeholder="Enter Username"
                                required
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">Email</label>
                            <input
                                type="email"
                                value={adminData.email}
                                onChange={(e) => setAdminData({ ...adminData, email: e.target.value })}
                                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                                placeholder="Enter Email"
                                required
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">Password</label>
                            <input
                                type="password"
                                value={adminData.password}
                                onChange={(e) => setAdminData({ ...adminData, password: e.target.value })}
                                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                                placeholder="Enter Password"
                                required
                            />
                        </div>

                        <div className="bg-purple-50 border border-purple-200 rounded-lg p-3 mt-4">
                            <p className="text-sm text-purple-700">
                                <strong>Note:</strong> The user role will automatically be set to "Admin".
                            </p>
                        </div>

                        <button
                            type="submit"
                            disabled={loading}
                            className="px-8 py-2 bg-purple-600 text-white rounded-lg font-semibold hover:bg-purple-700 transition-colors disabled:bg-gray-400 mt-6 max-w-xs mx-auto block"
                        >
                            {loading ? 'Adding Admin...' : 'Add Administrator'}
                        </button>
                    </form>
                </div>
            </div>

            {/* Overlay */}
            {sidePanelOpen && (
                <div
                    onClick={() => setSidePanelOpen(null)}
                    className="fixed inset-0 bg-black bg-opacity-50 z-40"
                />
            )}

            <Footer />
        </div>
    );
}
