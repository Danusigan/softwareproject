import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../components/header';
import Footer from '../components/footer';
import MultiSelectAutocomplete from '../components/MultiSelectAutocomplete';
import axios from 'axios';

export default function AdminDashboard() {
    const [sidePanelOpen, setSidePanelOpen] = useState(null); // 'module'
    const navigate = useNavigate();
    const [modules, setModules] = useState([]);
    const [showEditModuleDialog, setShowEditModuleDialog] = useState(false);
    const [editingModule, setEditingModule] = useState(null);

    // Module form states
    const [moduleData, setModuleData] = useState({
        moduleId: '',
        moduleName: '',
        assignedLecturerUsernames: []
    });
    const [lecturers, setLecturers] = useState([]);

    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState({ type: '', text: '' });

    const lecturerOptions = lecturers.map(l => ({ value: l.username, label: l.username, sublabel: l.email }));

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

    // Fetch modules and lecturers on mount
    useEffect(() => {
        fetchModules();
        fetchLecturers();
    }, []);

    const fetchModules = async () => {
        try {
            const token = localStorage.getItem('token');
            const res = await axios.get('/api/modules/all', {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            // Backend returns {message, data, status} format
            setModules(res.data.data || []);
        } catch (err) {
            console.error('Failed to fetch modules:', err);
            setModules([]);
        }
    };

    const fetchLecturers = async () => {
        try {
            const token = localStorage.getItem('token');
            const res = await axios.get('/api/auth/lecturers', {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            setLecturers(res.data.data || []);
        } catch (err) {
            console.error('Failed to fetch lecturers:', err);
            setLecturers([]);
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
                '/api/modules/create',
                moduleData,
                {
                    headers: {
                        'Authorization': `Bearer ${token}`
                    }
                }
            );

            setMessage({ type: 'success', text: 'Module created successfully!' });
            setModuleData({ moduleId: '', moduleName: '', assignedLecturerUsernames: [] });
            fetchModules();
            setTimeout(() => setSidePanelOpen(null), 2000);
        } catch (err) {
            setMessage({
                type: 'error',
                text: err.response?.data?.message || err.response?.data?.error || 'Failed to create module'
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
                `/api/modules/${editingModule.moduleId}`,
                {
                    moduleId: moduleData.moduleId,
                    moduleName: moduleData.moduleName,
                    assignedLecturerUsernames: moduleData.assignedLecturerUsernames
                },
                {
                    headers: { 'Authorization': `Bearer ${token}` }
                }
            );

            setMessage({ type: 'success', text: 'Module updated successfully!' });
            setShowEditModuleDialog(false);
            setEditingModule(null);
            setModuleData({ moduleId: '', moduleName: '', assignedLecturerUsernames: [] });
            fetchModules();
        } catch (err) {
            setMessage({
                type: 'error',
                text: err.response?.data?.message || err.response?.data?.error || 'Failed to update module'
            });
        } finally {
            setLoading(false);
        }
    };

    const handleDeleteModule = async (moduleId) => {
        if (!window.confirm('Are you sure you want to delete this module?')) return;

        try {
            const token = localStorage.getItem('token');
            await axios.delete(`/api/modules/${moduleId}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            setMessage({ type: 'success', text: 'Module deleted successfully!' });
            fetchModules();
        } catch (err) {
            setMessage({
                type: 'error',
                text: err.response?.data?.message || err.response?.data?.error || 'Failed to delete module'
            });
        }
    };

    const openEditModuleDialog = (module) => {
        setEditingModule(module);
        setModuleData({
            moduleId: module.moduleId,
            moduleName: module.moduleName,
            assignedLecturerUsernames: module.assignedLecturerUsernames || []
        });
        setShowEditModuleDialog(true);
    };


    return (
        <div className="min-h-screen bg-gray-50">
            <Header />

            {/* Main Content */}
            <div className="max-w-7xl mx-auto px-4 py-8">
                <div className="flex items-center justify-between mb-8">
                    <h1 className="text-3xl font-bold text-gray-800">Admin Dashboard</h1>
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
                                    <p className="text-center text-xs text-gray-500 mb-4">
                                        {module.assignedLecturerUsernames?.length
                                            ? `Assigned: ${module.assignedLecturerUsernames.join(', ')}`
                                            : 'Visible to all lecturers'}
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

                {/* Quick Actions Section */}
                <div className="mt-16 pt-10 border-t-2 border-gray-200">
                    <h2 className="text-2xl font-bold text-gray-800 mb-2">Quick Actions</h2>
                    <p className="text-gray-500 mb-8">Create modules, manage users and review outcomes</p>
                </div>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
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

                    {/* Manage Lecturers Card */}
                    <div
                        onClick={() => navigate('/manage-lecturers')}
                        className="bg-white rounded-xl shadow-lg p-8 cursor-pointer transform transition-all duration-300 hover:scale-105 hover:shadow-2xl border-2 border-transparent hover:border-blue-500"
                    >
                        <div className="flex items-center justify-center mb-4">
                            <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center">
                                <svg className="w-8 h-8 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 20h5v-2a4 4 0 00-3-3.87M9 20H4v-2a4 4 0 013-3.87m6-1.13a4 4 0 10-4-4 4 4 0 004 4zm6-4a4 4 0 11-1.33-2.98" />
                                </svg>
                            </div>
                        </div>
                        <h2 className="text-2xl font-bold text-center text-gray-800 mb-2">Manage Lecturers</h2>
                        <p className="text-center text-gray-600">Add, edit & assign modules to lecturers</p>
                    </div>

                    {/* Program Outcomes Management Card */}
                    <div
                        onClick={() => navigate('/program-outcomes')}
                        className="bg-white rounded-xl shadow-lg p-8 cursor-pointer transform transition-all duration-300 hover:scale-105 hover:shadow-2xl border-2 border-transparent hover:border-purple-500"
                    >
                        <div className="flex items-center justify-center mb-4">
                            <div className="w-16 h-16 bg-purple-100 rounded-full flex items-center justify-center">
                                <svg className="w-8 h-8 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4M7.835 4.697a3.42 3.42 0 001.946-.806 3.42 3.42 0 014.438 0 3.42 3.42 0 001.946.806 3.42 3.42 0 013.138 3.138 3.42 3.42 0 00.806 1.946 3.42 3.42 0 010 4.438 3.42 3.42 0 00-.806 1.946 3.42 3.42 0 01-3.138 3.138 3.42 3.42 0 00-1.946.806 3.42 3.42 0 01-4.438 0 3.42 3.42 0 00-1.946-.806 3.42 3.42 0 01-3.138-3.138 3.42 3.42 0 00-.806-1.946 3.42 3.42 0 010-4.438 3.42 3.42 0 00.806-1.946 3.42 3.42 0 013.138-3.138z" />
                                </svg>
                            </div>
                        </div>
                        <h2 className="text-2xl font-bold text-center text-gray-800 mb-2">Program Outcomes</h2>
                        <p className="text-center text-gray-600">Manage Washington Accord POs & custom outcomes</p>
                    </div>

                    {/* LO-PO Mappings Management Card */}
                    <div
                        onClick={() => navigate('/lo-po-mappings')}
                        className="bg-white rounded-xl shadow-lg p-8 cursor-pointer transform transition-all duration-300 hover:scale-105 hover:shadow-2xl border-2 border-transparent hover:border-orange-500"
                    >
                        <div className="flex items-center justify-center mb-4">
                            <div className="w-16 h-16 bg-orange-100 rounded-full flex items-center justify-center">
                                <svg className="w-8 h-8 text-orange-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                                </svg>
                            </div>
                        </div>
                        <h2 className="text-2xl font-bold text-center text-gray-800 mb-2">LO-PO Mappings</h2>
                        <p className="text-center text-gray-600">Manage & approve Learning Outcome mappings</p>
                    </div>

                    {/* CQI Review Card */}
                    <div
                        onClick={() => navigate('/cqi-review')}
                        className="bg-white rounded-xl shadow-lg p-8 cursor-pointer transform transition-all duration-300 hover:scale-105 hover:shadow-2xl border-2 border-transparent hover:border-red-500"
                    >
                        <div className="flex items-center justify-center mb-4">
                            <div className="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center">
                                <svg className="w-8 h-8 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                                </svg>
                            </div>
                        </div>
                        <h2 className="text-2xl font-bold text-center text-gray-800 mb-2">CQI Review</h2>
                        <p className="text-center text-gray-600">Review & approve corrective action plans</p>
                    </div>
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

                        <div>
                            <label className="block text-sm font-medium text-gray-700 mb-2">Assign Lecturers</label>
                            <MultiSelectAutocomplete
                                options={lecturerOptions}
                                selectedValues={moduleData.assignedLecturerUsernames}
                                onChange={(usernames) => setModuleData({ ...moduleData, assignedLecturerUsernames: usernames })}
                                placeholder="Type a lecturer's username..."
                                emptyHint="Leave empty to keep this module visible to all lecturers."
                            />
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
                                    setModuleData({ moduleId: '', moduleName: '', assignedLecturerUsernames: [] });
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

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-2">Assign Lecturers</label>
                                <MultiSelectAutocomplete
                                    options={lecturerOptions}
                                    selectedValues={moduleData.assignedLecturerUsernames}
                                    onChange={(usernames) => setModuleData({ ...moduleData, assignedLecturerUsernames: usernames })}
                                    placeholder="Type a lecturer's username..."
                                    emptyHint="Leave empty to keep this module visible to all lecturers."
                                />
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
