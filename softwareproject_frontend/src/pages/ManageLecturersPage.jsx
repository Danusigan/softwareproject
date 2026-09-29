import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../components/header';
import Footer from '../components/footer';
import MultiSelectAutocomplete from '../components/MultiSelectAutocomplete';
import axios from 'axios';

export default function ManageLecturersPage() {
    const navigate = useNavigate();
    const [sidePanelOpen, setSidePanelOpen] = useState(false);
    const [lecturers, setLecturers] = useState([]);
    const [modules, setModules] = useState([]);
    const [showEditLecturerDialog, setShowEditLecturerDialog] = useState(false);
    const [editingLecturer, setEditingLecturer] = useState(null);
    const [lecturerEditData, setLecturerEditData] = useState({ email: '', assignedModuleIds: [] });
    const [lecturerFilter, setLecturerFilter] = useState('');

    const [teacherData, setTeacherData] = useState({
        username: '',
        email: '',
        password: '',
        usertype: 'Lecture'
    });

    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState({ type: '', text: '' });

    const moduleOptions = modules.map(m => ({ value: m.moduleId, label: m.moduleName, sublabel: m.moduleId }));

    const filteredLecturers = lecturers.filter(l => {
        const q = lecturerFilter.trim().toLowerCase();
        if (!q) return true;
        return l.username.toLowerCase().includes(q) || l.email.toLowerCase().includes(q);
    });

    // Verify user is admin on mount
    useEffect(() => {
        const userType = localStorage.getItem('userType');
        const normalizedType = userType?.toLowerCase?.() || '';
        const isAdmin = normalizedType === 'admin' || normalizedType === 'superadmin' || normalizedType === 'super admin' || normalizedType === 'super-admin';

        if (!isAdmin) {
            navigate('/');
        }
    }, [navigate]);

    useEffect(() => {
        fetchLecturers();
        fetchModules();
    }, []);

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

    const fetchModules = async () => {
        try {
            const token = localStorage.getItem('token');
            const res = await axios.get('/api/modules/all', {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            setModules(res.data.data || []);
        } catch (err) {
            console.error('Failed to fetch modules:', err);
            setModules([]);
        }
    };

    const handleTeacherSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setMessage({ type: '', text: '' });

        try {
            const token = localStorage.getItem('token');
            const res = await axios.post(
                '/api/auth/add-user',
                {
                    userID: teacherData.username,
                    email: teacherData.email,
                    password: teacherData.password,
                    usertype: teacherData.usertype
                },
                {
                    headers: { 'Authorization': `Bearer ${token}` }
                }
            );

            if (res.data.status === 'SUCCESS') {
                setMessage({ type: 'success', text: 'Teacher added successfully!' });
                setTeacherData({ username: '', email: '', password: '', usertype: 'Lecture' });
                fetchLecturers();
                setTimeout(() => setSidePanelOpen(false), 2000);
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

    const openEditLecturerDialog = (lecturer) => {
        setEditingLecturer(lecturer);
        setLecturerEditData({ email: lecturer.email, assignedModuleIds: lecturer.assignedModuleIds || [] });
        setShowEditLecturerDialog(true);
    };

    const handleEditLecturer = async (e) => {
        e.preventDefault();
        setLoading(true);
        setMessage({ type: '', text: '' });

        try {
            const token = localStorage.getItem('token');
            const headers = { 'Authorization': `Bearer ${token}` };

            await axios.put(
                `/api/auth/lecturers/${editingLecturer.username}`,
                { email: lecturerEditData.email },
                { headers }
            );
            await axios.put(
                `/api/auth/lecturers/${editingLecturer.username}/modules`,
                { moduleIds: lecturerEditData.assignedModuleIds },
                { headers }
            );

            setMessage({ type: 'success', text: 'Lecturer updated successfully!' });
            setShowEditLecturerDialog(false);
            setEditingLecturer(null);
            fetchLecturers();
            fetchModules();
        } catch (err) {
            setMessage({
                type: 'error',
                text: err.response?.data?.message || 'Failed to update lecturer'
            });
        } finally {
            setLoading(false);
        }
    };

    const handleDeleteLecturer = async (username) => {
        if (!window.confirm(`Are you sure you want to delete lecturer "${username}"? This removes their module assignments too.`)) return;

        try {
            const token = localStorage.getItem('token');
            await axios.delete(`/api/auth/lecturers/${username}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            setMessage({ type: 'success', text: 'Lecturer deleted successfully!' });
            fetchLecturers();
            fetchModules();
        } catch (err) {
            setMessage({
                type: 'error',
                text: err.response?.data?.message || 'Failed to delete lecturer'
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
                            onClick={() => navigate('/admin-dashboard')}
                            className="p-2 text-gray-600 hover:bg-gray-200 rounded-lg transition-colors"
                            title="Back to Dashboard"
                        >
                            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
                            </svg>
                        </button>
                        <h1 className="text-3xl font-bold text-gray-800">Manage Lecturers</h1>
                    </div>
                    <button
                        onClick={() => setSidePanelOpen(true)}
                        className="px-5 py-2 bg-blue-600 text-white rounded-lg font-semibold hover:bg-blue-700 transition-colors flex items-center gap-2"
                    >
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" />
                        </svg>
                        Add Lecturer
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
                            value={lecturerFilter}
                            onChange={(e) => setLecturerFilter(e.target.value)}
                            placeholder="Filter by username or email..."
                            className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                        />
                    </div>
                </div>

                {lecturers.length === 0 ? (
                    <div className="bg-white rounded-xl shadow-lg p-8 text-center text-gray-500">
                        No lecturers available. Click "Add Lecturer" above to add one.
                    </div>
                ) : filteredLecturers.length === 0 ? (
                    <div className="bg-white rounded-xl shadow-lg p-8 text-center text-gray-500">
                        No lecturers match "{lecturerFilter}".
                    </div>
                ) : (
                    <div className="bg-white rounded-xl shadow-lg overflow-hidden">
                        <table className="w-full text-left">
                            <thead className="bg-gray-50 border-b border-gray-200">
                                <tr>
                                    <th className="px-6 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider">Username</th>
                                    <th className="px-6 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider">Email</th>
                                    <th className="px-6 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider">Assigned Modules</th>
                                    <th className="px-6 py-3 text-xs font-semibold text-gray-500 uppercase tracking-wider text-right">Actions</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-100">
                                {filteredLecturers.map((lecturer) => (
                                    <tr key={lecturer.username} className="hover:bg-gray-50 transition-colors">
                                        <td className="px-6 py-4 font-medium text-gray-800">{lecturer.username}</td>
                                        <td className="px-6 py-4 text-gray-600">{lecturer.email}</td>
                                        <td className="px-6 py-4 text-gray-500 text-sm">
                                            {lecturer.assignedModuleIds?.length
                                                ? lecturer.assignedModuleIds.join(', ')
                                                : 'No modules assigned'}
                                        </td>
                                        <td className="px-6 py-4 text-right">
                                            <div className="flex justify-end gap-2">
                                                <button
                                                    onClick={() => openEditLecturerDialog(lecturer)}
                                                    className="p-2 text-blue-600 hover:bg-blue-100 rounded-lg transition-colors"
                                                    title="Edit Lecturer"
                                                >
                                                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                                                    </svg>
                                                </button>
                                                <button
                                                    onClick={() => handleDeleteLecturer(lecturer.username)}
                                                    className="p-2 text-red-600 hover:bg-red-100 rounded-lg transition-colors"
                                                    title="Delete Lecturer"
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

            {/* Side Panel for Add Teacher */}
            <div
                className={`fixed top-0 right-0 h-full w-full md:w-96 bg-white shadow-2xl transform transition-transform duration-300 ease-in-out z-50 ${
                    sidePanelOpen ? 'translate-x-0' : 'translate-x-full'
                }`}
            >
                <div className="p-6 h-full overflow-y-auto">
                    <div className="flex justify-between items-center mb-6">
                        <h2 className="text-2xl font-bold text-gray-800">Add a Teacher</h2>
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

            {/* Overlay */}
            {sidePanelOpen && (
                <div
                    onClick={() => setSidePanelOpen(false)}
                    className="fixed inset-0 bg-black bg-opacity-50 z-40"
                />
            )}

            {/* Edit Lecturer Dialog */}
            {showEditLecturerDialog && editingLecturer && (
                <div className="fixed inset-0 bg-black bg-opacity-50 z-[60] flex items-center justify-center p-4">
                    <div className="bg-white rounded-xl shadow-2xl w-full max-w-md p-6">
                        <div className="flex justify-between items-center mb-6">
                            <h3 className="text-xl font-bold text-gray-800">Edit Lecturer</h3>
                            <button
                                onClick={() => {
                                    setShowEditLecturerDialog(false);
                                    setEditingLecturer(null);
                                }}
                                className="text-gray-500 hover:text-gray-700"
                            >
                                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                                </svg>
                            </button>
                        </div>

                        <form onSubmit={handleEditLecturer} className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-2">Username</label>
                                <input
                                    type="text"
                                    value={editingLecturer.username}
                                    disabled
                                    className="w-full px-4 py-2 border border-gray-200 bg-gray-100 text-gray-500 rounded-lg cursor-not-allowed"
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-2">Email</label>
                                <input
                                    type="email"
                                    value={lecturerEditData.email}
                                    onChange={(e) => setLecturerEditData({ ...lecturerEditData, email: e.target.value })}
                                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                    required
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-2">Assigned Modules</label>
                                <MultiSelectAutocomplete
                                    options={moduleOptions}
                                    selectedValues={lecturerEditData.assignedModuleIds}
                                    onChange={(moduleIds) => setLecturerEditData({ ...lecturerEditData, assignedModuleIds: moduleIds })}
                                    placeholder="Type a module name..."
                                    emptyHint="Leave empty to not restrict this lecturer to specific modules."
                                />
                            </div>

                            <button
                                type="submit"
                                disabled={loading}
                                className="w-full bg-blue-600 text-white py-3 rounded-lg font-semibold hover:bg-blue-700 transition-colors disabled:bg-gray-400"
                            >
                                {loading ? 'Updating...' : 'Update Lecturer'}
                            </button>
                        </form>
                    </div>
                </div>
            )}

            <Footer />
        </div>
    );
}
