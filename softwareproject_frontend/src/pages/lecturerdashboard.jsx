import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../components/header';
import Footer from '../components/footer';
import axios from 'axios';

export default function LecturerDashboard() {
    const navigate = useNavigate();
    const [modules, setModules] = useState([]);
    const [selectedModule, setSelectedModule] = useState(null);
    const [los, setLos] = useState([]);
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState({ type: '', text: '' });
    const [showAddLoDialog, setShowAddLoDialog] = useState(false);
    const [showEditLoDialog, setShowEditLoDialog] = useState(false);
    const [editingLo, setEditingLo] = useState(null);
    
    const [loData, setLoData] = useState({
        loNumber: '',
        description: ''
    });

    // Fetch modules on mount
    useEffect(() => {
        fetchModules();
    }, []);

    // Fetch LOs when a module is selected
    useEffect(() => {
        if (selectedModule) {
            fetchLosForModule(selectedModule.moduleId);
        }
    }, [selectedModule]);

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

    const fetchLosForModule = async (moduleId) => {
        try {
            const token = localStorage.getItem('token');
            const res = await axios.get(`http://localhost:8080/api/los/module/${moduleId}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            setLos(res.data);
        } catch (err) {
            console.error('Failed to fetch LOs:', err);
            setLos([]);
        }
    };

    const handleAddLo = async (e) => {
        e.preventDefault();
        setLoading(true);
        setMessage({ type: '', text: '' });

        try {
            const token = localStorage.getItem('token');
            await axios.post(
                'http://localhost:8080/api/los/create',
                {
                    ...loData,
                    moduleId: selectedModule.moduleId
                },
                {
                    headers: { 'Authorization': `Bearer ${token}` }
                }
            );

            setMessage({ type: 'success', text: 'LO added successfully!' });
            setLoData({ loNumber: '', description: '' });
            setShowAddLoDialog(false);
            fetchLosForModule(selectedModule.moduleId);
        } catch (err) {
            setMessage({ 
                type: 'error', 
                text: err.response?.data || 'Failed to add LO' 
            });
        } finally {
            setLoading(false);
        }
    };

    const handleEditLo = async (e) => {
        e.preventDefault();
        setLoading(true);
        setMessage({ type: '', text: '' });

        try {
            const token = localStorage.getItem('token');
            await axios.put(
                `http://localhost:8080/api/los/update/${editingLo.id}`,
                {
                    loNumber: loData.loNumber,
                    description: loData.description,
                    moduleId: selectedModule.moduleId
                },
                {
                    headers: { 'Authorization': `Bearer ${token}` }
                }
            );

            setMessage({ type: 'success', text: 'LO updated successfully!' });
            setShowEditLoDialog(false);
            setEditingLo(null);
            setLoData({ loNumber: '', description: '' });
            fetchLosForModule(selectedModule.moduleId);
        } catch (err) {
            setMessage({ 
                type: 'error', 
                text: err.response?.data || 'Failed to update LO' 
            });
        } finally {
            setLoading(false);
        }
    };

    const handleDeleteLo = async (loId) => {
        if (!window.confirm('Are you sure you want to delete this LO?')) return;

        try {
            const token = localStorage.getItem('token');
            await axios.delete(`http://localhost:8080/api/los/delete/${loId}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            setMessage({ type: 'success', text: 'LO deleted successfully!' });
            fetchLosForModule(selectedModule.moduleId);
        } catch (err) {
            setMessage({ 
                type: 'error', 
                text: err.response?.data || 'Failed to delete LO' 
            });
        }
    };

    const openEditDialog = (lo) => {
        setEditingLo(lo);
        setLoData({ loNumber: lo.loNumber, description: lo.description });
        setShowEditLoDialog(true);
    };

    const closeModulePanel = () => {
        setSelectedModule(null);
        setLos([]);
        setShowAddLoDialog(false);
        setShowEditLoDialog(false);
        setMessage({ type: '', text: '' });
    };

    return (
        <div className="min-h-screen bg-gray-50">
            <Header />

            <div className="max-w-7xl mx-auto px-4 py-8">
                <div className="flex items-center justify-between mb-8">
                    <h1 className="text-3xl font-bold text-gray-800">Lecturer Dashboard</h1>
                </div>

                {message.text && (
                    <div className={`mb-4 p-4 rounded-lg ${
                        message.type === 'success' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
                    }`}>
                        {message.text}
                    </div>
                )}

                {/* Modules Grid */}
                <div className="mb-8">
                    <h2 className="text-2xl font-bold text-gray-800 mb-6">Your Modules</h2>
                    {modules.length === 0 ? (
                        <div className="bg-white rounded-xl shadow-lg p-8 text-center text-gray-500">
                            No modules available
                        </div>
                    ) : (
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                            {modules.map((module) => (
                                <div
                                    key={module.moduleId}
                                    onClick={() => setSelectedModule(module)}
                                    className="bg-white rounded-xl shadow-lg p-6 cursor-pointer transform transition-all duration-300 hover:scale-105 hover:shadow-2xl border-2 border-transparent hover:border-blue-500"
                                >
                                    <div className="flex items-center justify-center mb-4">
                                        <div className="w-16 h-16 bg-blue-100 rounded-lg flex items-center justify-center">
                                            <svg className="w-8 h-8 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                                            </svg>
                                        </div>
                                    </div>
                                    <h3 className="text-xl font-bold text-center text-gray-800 mb-2">
                                        {module.moduleName}
                                    </h3>
                                    <p className="text-center text-gray-600 text-sm">
                                        Module ID: {module.moduleId}
                                    </p>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </div>

            {/* Module Detail Panel */}
            {selectedModule && (
                <>
                    <div
                        className="fixed top-0 right-0 h-full w-full md:w-2/3 lg:w-1/2 bg-white shadow-2xl transform transition-transform duration-300 ease-in-out z-50 overflow-y-auto"
                    >
                        <div className="p-6">
                            <div className="flex justify-between items-center mb-6 sticky top-0 bg-white pb-4 border-b">
                                <div>
                                    <h2 className="text-2xl font-bold text-gray-800">{selectedModule.moduleName}</h2>
                                    <p className="text-sm text-gray-600">Module ID: {selectedModule.moduleId}</p>
                                </div>
                                <button
                                    onClick={closeModulePanel}
                                    className="text-gray-500 hover:text-gray-700"
                                >
                                    <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                                    </svg>
                                </button>
                            </div>

                            {/* Add LO Button */}
                            <button
                                onClick={() => setShowAddLoDialog(true)}
                                className="w-full mb-6 bg-green-600 text-white py-3 rounded-lg font-semibold hover:bg-green-700 transition-colors flex items-center justify-center gap-2"
                            >
                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" />
                                </svg>
                                Add Learning Outcome
                            </button>

                            {/* LOs List */}
                            <div className="space-y-4">
                                <h3 className="text-lg font-bold text-gray-800 mb-4">Learning Outcomes</h3>
                                {los.length === 0 ? (
                                    <div className="text-center text-gray-500 py-8">
                                        No learning outcomes yet. Click "Add Learning Outcome" to create one.
                                    </div>
                                ) : (
                                    los.map((lo) => (
                                        <div
                                            key={lo.id}
                                            className="bg-gray-50 rounded-lg p-4 border border-gray-200 hover:border-blue-300 transition-colors"
                                        >
                                            <div className="flex justify-between items-start">
                                                <div className="flex-1">
                                                    <div className="flex items-center gap-2 mb-2">
                                                        <span className="bg-blue-100 text-blue-700 px-3 py-1 rounded-full text-sm font-semibold">
                                                            LO {lo.loNumber}
                                                        </span>
                                                    </div>
                                                    <p className="text-gray-700">{lo.description}</p>
                                                </div>
                                                <div className="flex gap-2 ml-4">
                                                    {/* Edit Icon */}
                                                    <button
                                                        onClick={() => openEditDialog(lo)}
                                                        className="p-2 text-blue-600 hover:bg-blue-100 rounded-lg transition-colors"
                                                        title="Edit LO"
                                                    >
                                                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                                                        </svg>
                                                    </button>
                                                    {/* Delete Icon */}
                                                    <button
                                                        onClick={() => handleDeleteLo(lo.id)}
                                                        className="p-2 text-red-600 hover:bg-red-100 rounded-lg transition-colors"
                                                        title="Delete LO"
                                                    >
                                                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                                        </svg>
                                                    </button>
                                                </div>
                                            </div>
                                        </div>
                                    ))
                                )}
                            </div>
                        </div>
                    </div>

                    {/* Overlay */}
                    <div
                        onClick={closeModulePanel}
                        className="fixed inset-0 bg-black bg-opacity-50 z-40"
                    />
                </>
            )}

            {/* Add LO Dialog */}
            {showAddLoDialog && selectedModule && (
                <div className="fixed inset-0 bg-black bg-opacity-50 z-[60] flex items-center justify-center p-4">
                    <div className="bg-white rounded-xl shadow-2xl w-full max-w-md p-6">
                        <div className="flex justify-between items-center mb-6">
                            <h3 className="text-xl font-bold text-gray-800">Add Learning Outcome</h3>
                            <button
                                onClick={() => {
                                    setShowAddLoDialog(false);
                                    setLoData({ loNumber: '', description: '' });
                                }}
                                className="text-gray-500 hover:text-gray-700"
                            >
                                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                                </svg>
                            </button>
                        </div>

                        <form onSubmit={handleAddLo} className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-2">LO Number</label>
                                <input
                                    type="text"
                                    value={loData.loNumber}
                                    onChange={(e) => setLoData({ ...loData, loNumber: e.target.value })}
                                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent"
                                    placeholder="e.g., 1, 2, 3"
                                    required
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-2">Description</label>
                                <textarea
                                    value={loData.description}
                                    onChange={(e) => setLoData({ ...loData, description: e.target.value })}
                                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500 focus:border-transparent"
                                    placeholder="Enter LO description"
                                    rows="4"
                                    required
                                />
                            </div>

                            <button
                                type="submit"
                                disabled={loading}
                                className="w-full bg-green-600 text-white py-3 rounded-lg font-semibold hover:bg-green-700 transition-colors disabled:bg-gray-400"
                            >
                                {loading ? 'Adding...' : 'Add LO'}
                            </button>
                        </form>
                    </div>
                </div>
            )}

            {/* Edit LO Dialog */}
            {showEditLoDialog && editingLo && (
                <div className="fixed inset-0 bg-black bg-opacity-50 z-[60] flex items-center justify-center p-4">
                    <div className="bg-white rounded-xl shadow-2xl w-full max-w-md p-6">
                        <div className="flex justify-between items-center mb-6">
                            <h3 className="text-xl font-bold text-gray-800">Edit Learning Outcome</h3>
                            <button
                                onClick={() => {
                                    setShowEditLoDialog(false);
                                    setEditingLo(null);
                                    setLoData({ loNumber: '', description: '' });
                                }}
                                className="text-gray-500 hover:text-gray-700"
                            >
                                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                                </svg>
                            </button>
                        </div>

                        <form onSubmit={handleEditLo} className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-2">LO Number</label>
                                <input
                                    type="text"
                                    value={loData.loNumber}
                                    onChange={(e) => setLoData({ ...loData, loNumber: e.target.value })}
                                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                    placeholder="e.g., 1, 2, 3"
                                    required
                                />
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-2">Description</label>
                                <textarea
                                    value={loData.description}
                                    onChange={(e) => setLoData({ ...loData, description: e.target.value })}
                                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                    placeholder="Enter LO description"
                                    rows="4"
                                    required
                                />
                            </div>

                            <button
                                type="submit"
                                disabled={loading}
                                className="w-full bg-blue-600 text-white py-3 rounded-lg font-semibold hover:bg-blue-700 transition-colors disabled:bg-gray-400"
                            >
                                {loading ? 'Updating...' : 'Update LO'}
                            </button>
                        </form>
                    </div>
                </div>
            )}

            <Footer />
        </div>
    );
}
