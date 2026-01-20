import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../components/header';
import Footer from '../components/footer';
import axios from 'axios';
import ModuleModal from '../components/ModuleModal';

export default function ModulesPage() {
    const [modules, setModules] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedModule, setSelectedModule] = useState(null);
    const [modalType, setModalType] = useState(null); // 'edit', 'delete', 'view'
    const navigate = useNavigate();

    const moduleColors = [
        'bg-red-300',
        'bg-orange-300',
        'bg-green-300',
        'bg-purple-300',
        'bg-blue-300'
    ];

    useEffect(() => {
        fetchModules();
    }, []);

    const fetchModules = async () => {
        try {
            const res = await axios.get('http://localhost:8080/api/modules/all');
            setModules(res.data);
        } catch (err) {
            console.error('Error fetching modules:', err);
        } finally {
            setLoading(false);
        }
    };

    const handleModuleClick = (module) => {
        setSelectedModule(module);
        setModalType('view');
    };

    const handleEdit = (module, e) => {
        e.stopPropagation();
        setSelectedModule(module);
        setModalType('edit');
    };

    const handleDelete = (module, e) => {
        e.stopPropagation();
        setSelectedModule(module);
        setModalType('delete');
    };

    const confirmDelete = async () => {
        try {
            const token = localStorage.getItem('token');
            await axios.delete(
                `http://localhost:8080/api/modules/${selectedModule.moduleId}`,
                {
                    headers: { 'Authorization': `Bearer ${token}` }
                }
            );
            setModules(modules.filter(m => m.moduleId !== selectedModule.moduleId));
            setModalType(null);
            setSelectedModule(null);
        } catch (err) {
            console.error('Error deleting module:', err);
            alert(err.response?.data || 'Failed to delete module');
        }
    };

    const handleUpdate = async (updatedData) => {
        try {
            const token = localStorage.getItem('token');
            await axios.put(
                `http://localhost:8080/api/modules/${selectedModule.moduleId}`,
                updatedData,
                {
                    headers: { 'Authorization': `Bearer ${token}` }
                }
            );
            fetchModules();
            setModalType(null);
            setSelectedModule(null);
        } catch (err) {
            console.error('Error updating module:', err);
            alert(err.response?.data || 'Failed to update module');
        }
    };

    return (
        <div className="min-h-screen bg-gray-50">
            <Header />

            <div className="max-w-7xl mx-auto px-4 py-8">
                <div className="flex items-center justify-between mb-8">
                    <h1 className="text-3xl font-bold text-gray-800">My Courses</h1>
                    <div className="space-x-4">
                        <button
                            onClick={() => navigate('/')}
                            className="px-6 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition-colors"
                        >
                            Home
                        </button>
                    </div>
                </div>

                {loading ? (
                    <div className="text-center py-12">
                        <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
                    </div>
                ) : (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                        {modules.map((module, index) => (
                            <div key={module.moduleId} className="bg-white rounded-lg shadow-lg overflow-hidden">
                                {/* Module Color Header */}
                                <div
                                    className={`${moduleColors[index % moduleColors.length]} h-32 cursor-pointer hover:opacity-90 transition-opacity`}
                                    onClick={() => handleModuleClick(module)}
                                />
                                
                                {/* Module Info */}
                                <div className="p-4">
                                    <h3 className="text-lg font-bold text-gray-800 mb-2">{module.moduleId} {module.moduleName}</h3>
                                    
                                    {/* Action Buttons */}
                                    <div className="flex gap-2 mt-4">
                                        <button
                                            onClick={() => handleModuleClick(module)}
                                            className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors text-sm font-medium"
                                        >
                                            View LOs
                                        </button>
                                        <button
                                            onClick={(e) => handleEdit(module, e)}
                                            className="flex-1 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors text-sm font-medium"
                                        >
                                            Edit
                                        </button>
                                        <button
                                            onClick={(e) => handleDelete(module, e)}
                                            className="flex-1 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors text-sm font-medium"
                                        >
                                            Delete
                                        </button>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>

            {/* Modals */}
            {modalType === 'view' && selectedModule && (
                <ModuleModal
                    module={selectedModule}
                    onClose={() => {
                        setModalType(null);
                        setSelectedModule(null);
                    }}
                />
            )}

            {modalType === 'edit' && selectedModule && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                    <div className="bg-white rounded-xl p-6 w-full max-w-md">
                        <h2 className="text-2xl font-bold mb-4">Edit Module</h2>
                        <form onSubmit={(e) => {
                            e.preventDefault();
                            const formData = new FormData(e.target);
                            handleUpdate({
                                moduleId: selectedModule.moduleId,
                                moduleName: formData.get('moduleName')
                            });
                        }}>
                            <div className="mb-4">
                                <label className="block text-sm font-medium text-gray-700 mb-2">Module ID</label>
                                <input
                                    type="text"
                                    value={selectedModule.moduleId}
                                    disabled
                                    className="w-full px-4 py-2 border border-gray-300 rounded-lg bg-gray-100"
                                />
                            </div>
                            <div className="mb-4">
                                <label className="block text-sm font-medium text-gray-700 mb-2">Module Name</label>
                                <input
                                    type="text"
                                    name="moduleName"
                                    defaultValue={selectedModule.moduleName}
                                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500"
                                    required
                                />
                            </div>
                            <div className="flex gap-2">
                                <button
                                    type="button"
                                    onClick={() => setModalType(null)}
                                    className="flex-1 px-4 py-2 bg-gray-300 text-gray-700 rounded-lg hover:bg-gray-400"
                                >
                                    Cancel
                                </button>
                                <button
                                    type="submit"
                                    className="flex-1 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700"
                                >
                                    Update
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {modalType === 'delete' && selectedModule && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                    <div className="bg-white rounded-xl p-6 w-full max-w-md">
                        <h2 className="text-2xl font-bold mb-4 text-red-600">Delete Module</h2>
                        <p className="mb-6">
                            Are you sure you want to delete <strong>{selectedModule.moduleName}</strong>? This action cannot be undone.
                        </p>
                        <div className="flex gap-2">
                            <button
                                onClick={() => setModalType(null)}
                                className="flex-1 px-4 py-2 bg-gray-300 text-gray-700 rounded-lg hover:bg-gray-400"
                            >
                                Cancel
                            </button>
                            <button
                                onClick={confirmDelete}
                                className="flex-1 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700"
                            >
                                Delete
                            </button>
                        </div>
                    </div>
                </div>
            )}

            <Footer />
        </div>
    );
}
