import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../components/header';
import Footer from '../components/footer';
import axios from 'axios';

export default function AdminDashboard() {
    const [sidePanelOpen, setSidePanelOpen] = useState(null); // 'teacher' or 'module'
    const navigate = useNavigate();
    const [modules, setModules] = useState([]);
    const [showEditModuleDialog, setShowEditModuleDialog] = useState(false);
    const [editingModule, setEditingModule] = useState(null);

    // Teacher form states
    const [teacherData, setTeacherData] = useState({
        username: '',
        email: '',
        password: '',
        usertype: 'Lecture'
    });

    // Module form states
    const [moduleData, setModuleData] = useState({
        moduleId: '',
        moduleName: ''
    });

    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState({ type: '', text: '' });

    // Verify user is admin on mount
    useEffect(() => {
        const userType = localStorage.getItem('userType');
        console.log('Admin Dashboard - User Type:', userType);
        
        const normalizedType = userType?.toLowerCase?.() || '';
        const isAdmin = normalizedType === 'admin' || normalizedType === 'superadmin' || normalizedType === 'super admin' || normalizedType === 'super-admin';
        
        if (!isAdmin) {
            console.log('Unauthorized! Redirecting to home. Normalized type:', normalizedType);
            navigate('/');
        }
    }, [navigate]);

    // Fetch modules on mount
    useEffect(() => {
        fetchModules();
    }, []);

    const fetchModules = async () => {
        try {
            const token = localStorage.getItem('token');
            const res = await axios.get('http://localhost:8080/api/modules/all', {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            setModules(res.data);
        } catch (err) {
            console.error('Failed to fetch modules:', err);
        }
    };

    const handleTeacherSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setMessage({ type: '', text: '' });

        try {
            const token = localStorage.getItem('token');
            const res = await axios.post(
                'http://localhost:8080/api/auth/add-user',
                {
                    userID: teacherData.username,
                    email: teacherData.email,
                    password: teacherData.password,
                    usertype: teacherData.usertype
                },
                {
                    headers: {
                        'Authorization': `Bearer ${token}`
                    }
                }
            );

            if (res.data.status === 'SUCCESS') {
                setMessage({ type: 'success', text: 'Teacher added successfully!' });
                setTeacherData({ username: '', email: '', password: '', usertype: 'Lecture' });
                setTimeout(() => setSidePanelOpen(null), 2000);
            }
        } catch (err) {
            setMessage({ 
                type: 'error', 
                text: err.response?.data?.message || 'Failed to add teacher' 
            });
        } finally {
            setLoading(false);
        }
    };

    const handleModuleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setMessage({ type: '', text: '' });

        // Validate moduleId: only capital letters and digits
        if (moduleData.moduleId && !moduleData.moduleId.match(/^[A-Z0-9]+$/)) {
            setMessage({ type: 'error', text: 'Module ID must contain only capital letters (A-Z) and digits (0-9)' });
            setLoading(false);
            return;
        }

        try {
            const token = localStorage.getItem('token');
            const res = await axios.post(
                'http://localhost:8080/api/modules/create',
                moduleData,
                {
                    headers: {
                        'Authorization': `Bearer ${token}`
                    }
                }
            );

            setMessage({ type: 'success', text: 'Module created successfully!' });
            setModuleData({ moduleId: '', moduleName: '' });
            fetchModules();
            setTimeout(() => setSidePanelOpen(null), 2000);
        } catch (err) {
            setMessage({ 
                type: 'error', 
                text: err.response?.data || 'Failed to create module' 
            });
        } finally {
            setLoading(false);
        }
    };

    const handleEditModule = async (e) => {
        e.preventDefault();
        setLoading(true);
        setMessage({ type: '', text: '' });

        // Validate moduleId: only capital letters and digits
        if (moduleData.moduleId && !moduleData.moduleId.match(/^[A-Z0-9]+$/)) {
            setMessage({ type: 'error', text: 'Module ID must contain only capital letters (A-Z) and digits (0-9)' });
            setLoading(false);
            return;
        }

        try {
            const token = localStorage.getItem('token');
            await axios.put(
                `http://localhost:8080/api/modules/${editingModule.moduleId}`,
                {
                    moduleId: moduleData.moduleId,
                    moduleName: moduleData.moduleName
                },
                {
                    headers: { 'Authorization': `Bearer ${token}` }
                }
            );

            setMessage({ type: 'success', text: 'Module updated successfully!' });
            setShowEditModuleDialog(false);
            setEditingModule(null);
            setModuleData({ moduleId: '', moduleName: '' });
            fetchModules();
        } catch (err) {
            setMessage({ 
                type: 'error', 
                text: err.response?.data || 'Failed to update module' 
            });
        } finally {
            setLoading(false);
        }
    };

    const handleDeleteModule = async (moduleId) => {
        if (!window.confirm('Are you sure you want to delete this module?')) return;

        try {
            const token = localStorage.getItem('token');
            await axios.delete(`http://localhost:8080/api/modules/${moduleId}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            setMessage({ type: 'success', text: 'Module deleted successfully!' });
            fetchModules();
        } catch (err) {
            setMessage({ 
                type: 'error', 
                text: err.response?.data || 'Failed to delete module' 
            });
        }
    };

    const openEditModuleDialog = (module) => {
        setEditingModule(module);
        setModuleData({ moduleId: module.moduleId, moduleName: module.moduleName });
        setShowEditModuleDialog(true);
    };

    return (
        <div className="min-h-screen bg-gray-50">
            <Header />

            {/* Main Content */}
            <div className="max-w-7xl mx-auto px-4 py-8">
                <div className="flex items-center justify-between mb-8">
                    <h1 className="text-3xl font-bold text-gray-800">dmin Dashboard</h1>
                    <button
                        onClick={() => navigate('/modules')}
                        className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                    >
                        View Modules
                    </button>
                </div>

                {message.text && (
                    <div className={`mb-4 p-4 rounded-lg ${
                        message.type === 'success' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
                    }`}>
                        {message.text}
                    </div>
                )}

                {/* Modules Management Section */}
                <div className="mb-8">
                    <h2 className="text-2xl font-bold text-gray-800 mb-6">Manage Modules</h2>
                    {modules.length === 0 ? (
                        <div className="bg-white rounded-xl shadow-lg p-8 text-center text-gray-500">
                            No modules available. Create one using the "Create Module" card below.
                        </div>
                    ) : (
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
                            {modules.map((module) => (
                                <div
                                    key={module.moduleId}
                                    className="bg-white rounded-xl shadow-lg p-6 border-2 border-gray-200 hover:border-green-400 transition-all"
                                >
                                    <div className="flex items-center justify-center mb-4">
                                        <div className="w-16 h-16 bg-green-100 rounded-lg flex items-center justify-center">
                                            <svg className="w-8 h-8 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                                            </svg>
                                        </div>
                                    </div>
                                    <h3 className="text-xl font-bold text-center text-gray-800 mb-2">
                                        {module.moduleName}
                                    </h3>
                                    <p className="text-center text-gray-600 text-sm mb-4">
                                        Module ID: {module.moduleId}
                                    </p>
                                    <div className="flex justify-center gap-2 mt-4">
                                        {/* Edit Icon */}
                                        <button
                                            onClick={() => openEditModuleDialog(module)}
                                            className="p-2 text-blue-600 hover:bg-blue-100 rounded-lg transition-colors"
                                            title="Edit Module"
                                        >
                                            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                                            </svg>
                                        </button>
                                        {/* Delete Icon */}
                                        <button
                                            onClick={() => handleDeleteModule(module.moduleId)}
                                            className="p-2 text-red-600 hover:bg-red-100 rounded-lg transition-colors"
                                            title="Delete Module"
                                        >
                                            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                            </svg>
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                {/* Modern Cards with Hover Effects */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-8 mt-12">
                    {/* Add Teacher Card */}
                    <div
                        onClick={() => setSidePanelOpen('teacher')}
                        className="bg-white rounded-xl shadow-lg p-8 cursor-pointer transform transition-all duration-300 hover:scale-105 hover:shadow-2xl border-2 border-transparent hover:border-blue-500"
                    >
                        <div className="flex items-center justify-center mb-4">
                            <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center">
                                <svg className="w-8 h-8 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
                                </svg>
                            </div>
                        </div>
                        <h2 className="text-2xl font-bold text-center text-gray-800 mb-2">Add a Teacher</h2>
                        <p className="text-center text-gray-600">Click to add a new teacher to the system</p>
                    </div>

                    {/* Create Module Card */}
                    <div
                        onClick={() => setSidePanelOpen('module')}
                        className="bg-white rounded-xl shadow-lg p-8 cursor-pointer transform transition-all duration-300 hover:scale-105 hover:shadow-2xl border-2 border-transparent hover:border-green-500"
                    >
                        <div className="flex items-center justify-center mb-4">
                            <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center">
                                <svg className="w-8 h-8 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                                </svg>
                            </div>
                        </div>
                        <h2 className="text-2xl font-bold text-center text-gray-800 mb-2">Create the Module</h2>
                        <p className="text-center text-gray-600">Click to create a new course module</p>
                    </div>
                </div>
            </div>

            {/* Side Panel for Add Teacher */}
            <div
                className={`fixed top-0 right-0 h-full w-full md:w-96 bg-white shadow-2xl transform transition-transform duration-300 ease-in-out z-50 ${
                    sidePanelOpen === 'teacher' ? 'translate-x-0' : 'translate-x-full'
                }`}
            >
                <div className="p-6 h-full overflow-y-auto">
                    <div className="flex justify-between items-center mb-6">
                        <h2 className="text-2xl font-bold text-gray-800">Add a Teacher</h2>
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

                    <form onSubmit={handleTeacherSubmit} className="space-y-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">Username</label>
                            <input
                                type="text"
                                value={teacherData.username}
                                onChange={(e) => setTeacherData({ ...teacherData, username: e.target.value })}
                                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                placeholder="Enter Username"
                                required
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">Email</label>
                            <input
                                type="email"
                                value={teacherData.email}
                                onChange={(e) => setTeacherData({ ...teacherData, email: e.target.value })}
                                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                placeholder="Enter Email"
                                required
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">Password</label>
                            <input
                                type="password"
                                value={teacherData.password}
                                onChange={(e) => setTeacherData({ ...teacherData, password: e.target.value })}
                                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                placeholder="Enter Password"
                                required
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">User Type</label>
                            <select
                                value={teacherData.usertype}
                                onChange={(e) => setTeacherData({ ...teacherData, usertype: e.target.value })}
                                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                            >
                                <option value="Lecture">Lecture</option>
                                <option value="Admin">Admin</option>
                            </select>
                        </div>

                        <button
                            type="submit"
                            disabled={loading}
                            className="px-8 py-2 bg-blue-600 text-white rounded-lg font-semibold hover:bg-blue-700 transition-colors disabled:bg-gray-400 max-w-xs mx-auto block"
                        >
                            {loading ? 'Adding...' : 'Add Teacher'}
                        </button>
                    </form>
                </div>
            </div>

            {/* Side Panel for Create Module */}
            <div
                className={`fixed top-0 right-0 h-full w-full md:w-96 bg-white shadow-2xl transform transition-transform duration-300 ease-in-out z-50 ${
                    sidePanelOpen === 'module' ? 'translate-x-0' : 'translate-x-full'
                }`}
            >
                <div className="p-6 h-full overflow-y-auto">
                    <div className="flex justify-between items-center mb-6">
                        <h2 className="text-2xl font-bold text-gray-800">Create the Module</h2>
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

                    <form onSubmit={handleModuleSubmit} className="space-y-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">Module Name</label>
                            <input
                                type="text"
                                value={moduleData.moduleName}
                                onChange={(e) => setModuleData({ ...moduleData, moduleName: e.target.value })}
                                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent"
                                placeholder="Enter the Module Name"
                                required
                            />
                        </div>

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">Module ID</label>
                            <input
                                type="text"
                                value={moduleData.moduleId}
                                onChange={(e) => setModuleData({ ...moduleData, moduleId: e.target.value.toUpperCase() })}
                                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent"
                                placeholder="Enter the Module ID (e.g., SE101, CS201)"
                                required
                            />
                            <p className="text-xs text-gray-500 mt-1">Only capital letters (A-Z) and digits (0-9) allowed</p>
                        </div>

                        <button
                            type="submit"
                            disabled={loading}
                            className="px-8 py-2 bg-green-600 text-white rounded-lg font-semibold hover:bg-green-700 transition-colors disabled:bg-gray-400 max-w-xs mx-auto block"
                        >
                            {loading ? 'Creating...' : 'Create Module'}
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

            {/* Edit Module Dialog */}
            {showEditModuleDialog && editingModule && (
                <div className="fixed inset-0 bg-black bg-opacity-50 z-[60] flex items-center justify-center p-4">
                    <div className="bg-white rounded-xl shadow-2xl w-full max-w-md p-6">
                        <div className="flex justify-between items-center mb-6">
                            <h3 className="text-xl font-bold text-gray-800">Edit Module</h3>
                            <button
                                onClick={() => {
                                    setShowEditModuleDialog(false);
                                    setEditingModule(null);
                                    setModuleData({ moduleId: '', moduleName: '' });
                                }}
                                className="text-gray-500 hover:text-gray-700"
                            >
                                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                                </svg>
                            </button>
                        </div>

                        <form onSubmit={handleEditModule} className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-2">Module Name</label>
                                <input
                                    type="text"
                                    value={moduleData.moduleName}
                                    onChange={(e) => setModuleData({ ...moduleData, moduleName: e.target.value })}
                                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                    placeholder="Enter the Module Name"
                                    required
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-2">Module ID</label>
                                <input
                                    type="text"
                                    value={moduleData.moduleId}
                                    onChange={(e) => setModuleData({ ...moduleData, moduleId: e.target.value.toUpperCase() })}
                                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                    placeholder="Enter the Module ID (e.g., SE101, CS201)"
                                    required
                                />
                                <p className="text-xs text-gray-500 mt-1">Only capital letters (A-Z) and digits (0-9) allowed</p>
                            </div>

                            <button
                                type="submit"
                                disabled={loading}
                                className="w-full bg-blue-600 text-white py-3 rounded-lg font-semibold hover:bg-blue-700 transition-colors disabled:bg-gray-400"
                            >
                                {loading ? 'Updating...' : 'Update Module'}
                            </button>
                        </form>
                    </div>
                </div>
            )}

            <Footer />
        </div>
    );
}
