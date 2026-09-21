import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../components/header';
import Footer from '../components/footer';
import axios from 'axios';

export default function ManageAdminsPage() {
    const navigate = useNavigate();
    const [sidePanelOpen, setSidePanelOpen] = useState(false);
    const [admins, setAdmins] = useState([]);
    const [showEditAdminDialog, setShowEditAdminDialog] = useState(false);
    const [editingAdmin, setEditingAdmin] = useState(null);
    const [adminEditData, setAdminEditData] = useState({ email: '' });
    const [adminFilter, setAdminFilter] = useState('');

    const [adminData, setAdminData] = useState({
        username: '',
        email: '',
        password: ''
    });

    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState({ type: '', text: '' });

    const filteredAdmins = admins.filter(a => {
        const q = adminFilter.trim().toLowerCase();
        if (!q) return true;
        return a.username.toLowerCase().includes(q) || a.email.toLowerCase().includes(q);
    });

    // Verify user is superadmin on mount
    useEffect(() => {
        const userType = localStorage.getItem('userType');
        const normalizedType = userType?.toLowerCase?.() || '';
        const isSuperAdmin = normalizedType === 'superadmin' || normalizedType === 'super admin' || normalizedType === 'super-admin';

        if (!isSuperAdmin) {
            navigate('/');
        }
    }, [navigate]);

    useEffect(() => {
        fetchAdmins();
    }, []);

    const fetchAdmins = async () => {
        try {
            const token = localStorage.getItem('token');
            const res = await axios.get('/api/auth/admins', {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            setAdmins(res.data.data || []);
        } catch (err) {
            console.error('Failed to fetch admins:', err);
            setAdmins([]);
        }
    };

    const handleAdminSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setMessage({ type: '', text: '' });

        try {
            const token = localStorage.getItem('token');
            const res = await axios.post(
                '/api/auth/add-user',
                {
                    userID: adminData.username,
                    email: adminData.email,
                    password: adminData.password,
                    usertype: 'Admin'
                },
                {
                    headers: { 'Authorization': `Bearer ${token}` }
                }
            );

            if (res.data.status === 'SUCCESS') {
                setMessage({ type: 'success', text: 'Admin added successfully!' });
                setAdminData({ username: '', email: '', password: '' });
                fetchAdmins();
                setTimeout(() => setSidePanelOpen(false), 2000);
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

    const openEditAdminDialog = (admin) => {
        setEditingAdmin(admin);
        setAdminEditData({ email: admin.email });
        setShowEditAdminDialog(true);
    };

    const handleEditAdmin = async (e) => {
        e.preventDefault();
        setLoading(true);
        setMessage({ type: '', text: '' });

        try {
            const token = localStorage.getItem('token');
            await axios.put(
                `/api/auth/admins/${editingAdmin.username}`,
                { email: adminEditData.email },
                { headers: { 'Authorization': `Bearer ${token}` } }
            );

            setMessage({ type: 'success', text: 'Admin updated successfully!' });
            setShowEditAdminDialog(false);
            setEditingAdmin(null);
            fetchAdmins();
        } catch (err) {
            setMessage({
                type: 'error',
                text: err.response?.data?.message || 'Failed to update admin'
            });
        } finally {
            setLoading(false);
        }
    };

    const handleDeleteAdmin = async (username) => {
        if (!window.confirm(`Are you sure you want to delete admin "${username}"?`)) return;

        try {
            const token = localStorage.getItem('token');
            await axios.delete(`/api/auth/admins/${username}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            setMessage({ type: 'success', text: 'Admin deleted successfully!' });
            fetchAdmins();
        } catch (err) {
            setMessage({
                type: 'error',
                text: err.response?.data?.message || 'Failed to delete admin'
            });
        }
    };

    return (
        <div className="min-h-screen bg-gray-50">
            <Header />

            <div className="max-w-7xl mx-auto px-4 py-8">
                <div className="flex items-center justify-between mb-8 gap-4 flex-wrap">
                    <div className="flex items-center gap-4">
                        <button
                            onClick={() => navigate('/super-admin-dashboard')}
                            className="p-2 text-gray-600 hover:bg-gray-200 rounded-lg transition-colors"
                            title="Back to Dashboard"
                        >
                            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
                            </svg>
                        </button>
                        <h1 className="text-3xl font-bold text-gray-800">Manage Admins</h1>
                    </div>
                    <button
                        onClick={() => setSidePanelOpen(true)}
                        className="px-5 py-2 bg-purple-600 text-white rounded-lg font-semibold hover:bg-purple-700 transition-colors flex items-center gap-2"
                    >
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" />
                        </svg>
                        Add Administrator
                    </button>
                </div>

                {message.text && (
                    <div className={`mb-4 p-4 rounded-lg ${
                        message.type === 'success' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
                    }`}>
                        {message.text}
                    </div>
                )}

                <div className="mb-4">
                    <div className="relative max-w-sm">
                        <svg className="w-5 h-5 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                        </svg>
                        <input
                            type="text"
                            value={adminFilter}
                            onChange={(e) => setAdminFilter(e.target.value)}
                            placeholder="Filter by username or email..."
                            className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                        />
                    </div>
                </div>

                {admins.length === 0 ? (
                    <div className="bg-white rounded-xl shadow-lg p-8 text-center text-gray-500">
                        No admins available. Click "Add Administrator" above to add one.
                    </div>
                ) : filteredAdmins.length === 0 ? (
                    <div className="bg-white rounded-xl shadow-lg p-8 text-center text-gray-500">
                        No admins match "{adminFilter}".
                    </div>
                ) : (
                    <div className="bg-white rounded-xl shadow-lg overflow-hidden">
                        <table className="w-full text-left">
                            <thead className="bg-gray-50 border-b border-gray-200">
                                <tr>
                                    <th className="px-6 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider">Username</th>
                                    <th className="px-6 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider">Email</th>
                                    <th className="px-6 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider text-right">Actions</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-100">
                                {filteredAdmins.map((admin) => (
                                    <tr key={admin.username} className="hover:bg-gray-50 transition-colors">
                                        <td className="px-6 py-4 font-medium text-gray-800">{admin.username}</td>
                                        <td className="px-6 py-4 text-gray-600">{admin.email}</td>
                                        <td className="px-6 py-4 text-right">
                                            <div className="flex justify-end gap-2">
                                                <button
                                                    onClick={() => openEditAdminDialog(admin)}
                                                    className="p-2 text-blue-600 hover:bg-blue-100 rounded-lg transition-colors"
                                                    title="Edit Admin"
                                                >
                                                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                                                    </svg>
                                                </button>
                                                <button
                                                    onClick={() => handleDeleteAdmin(admin.username)}
                                                    className="p-2 text-red-600 hover:bg-red-100 rounded-lg transition-colors"
                                                    title="Delete Admin"
                                                >
                                                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                                    </svg>
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>

            {/* Side Panel for Add Admin */}
            <div
                className={`fixed top-0 right-0 h-full w-full md:w-96 bg-white shadow-2xl transform transition-transform duration-300 ease-in-out z-50 ${
                    sidePanelOpen ? 'translate-x-0' : 'translate-x-full'
                }`}
            >
                <div className="p-6 h-full overflow-y-auto">
                    <div className="flex justify-between items-center mb-6">
                        <h2 className="text-2xl font-bold text-gray-800">Add Administrator</h2>
                        <button
                            onClick={() => setSidePanelOpen(false)}
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
                    onClick={() => setSidePanelOpen(false)}
                    className="fixed inset-0 bg-black bg-opacity-50 z-40"
                />
            )}

            {/* Edit Admin Dialog */}
            {showEditAdminDialog && editingAdmin && (
                <div className="fixed inset-0 bg-black bg-opacity-50 z-[60] flex items-center justify-center p-4">
                    <div className="bg-white rounded-xl shadow-2xl w-full max-w-md p-6">
                        <div className="flex justify-between items-center mb-6">
                            <h3 className="text-xl font-bold text-gray-800">Edit Admin</h3>
                            <button
                                onClick={() => {
                                    setShowEditAdminDialog(false);
                                    setEditingAdmin(null);
                                }}
                                className="text-gray-500 hover:text-gray-700"
                            >
                                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                                </svg>
                            </button>
                        </div>

                        <form onSubmit={handleEditAdmin} className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-2">Username</label>
                                <input
                                    type="text"
                                    value={editingAdmin.username}
                                    disabled
                                    className="w-full px-4 py-2 border border-gray-200 bg-gray-100 text-gray-500 rounded-lg cursor-not-allowed"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-2">Email</label>
                                <input
                                    type="email"
                                    value={adminEditData.email}
                                    onChange={(e) => setAdminEditData({ ...adminEditData, email: e.target.value })}
                                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                                    required
                                />
                            </div>

                            <button
                                type="submit"
                                disabled={loading}
                                className="w-full bg-purple-600 text-white py-3 rounded-lg font-semibold hover:bg-purple-700 transition-colors disabled:bg-gray-400"
                            >
                                {loading ? 'Updating...' : 'Update Admin'}
                            </button>
                        </form>
                    </div>
                </div>
            )}

            <Footer />
        </div>
    );
}
