import React, { useState, useEffect } from 'react';
import axios from 'axios';

export default function ModuleModal({ module, onClose }) {
    const [losPosList, setLosPosList] = useState([]);
    const [loading, setLoading] = useState(true);
    const [editingLo, setEditingLo] = useState(null);
    const [deletingLo, setDeletingLo] = useState(null);
    const [addingNew, setAddingNew] = useState(false);
    const [newLoData, setNewLoData] = useState({ id: '', name: '' });

    useEffect(() => {
        fetchLosPos();
    }, [module]);

    const fetchLosPos = async () => {
        try {
            const token = localStorage.getItem('token');
            const res = await axios.get(
                `http://localhost:8080/api/lospos/module/${module.moduleId}`,
                {
                    headers: { 'Authorization': `Bearer ${token}` }
                }
            );
            setLosPosList(res.data);
        } catch (err) {
            console.error('Error fetching LOs:', err);
        } finally {
            setLoading(false);
        }
    };

    const handleEditLo = (lo) => {
        setEditingLo({ ...lo });
    };

    const handleUpdateLo = async () => {
        try {
            const token = localStorage.getItem('token');
            await axios.put(
                `http://localhost:8080/api/lospos/${editingLo.id}`,
                { name: editingLo.name },
                {
                    headers: { 'Authorization': `Bearer ${token}` }
                }
            );
            fetchLosPos();
            setEditingLo(null);
        } catch (err) {
            console.error('Error updating LO:', err);
            alert(err.response?.data || 'Failed to update LO');
        }
    };

    const handleDeleteLo = async (loId) => {
        try {
            const token = localStorage.getItem('token');
            await axios.delete(
                `http://localhost:8080/api/lospos/${loId}`,
                {
                    headers: { 'Authorization': `Bearer ${token}` }
                }
            );
            setLosPosList(losPosList.filter(lo => lo.id !== loId));
            setDeletingLo(null);
        } catch (err) {
            console.error('Error deleting LO:', err);
            alert(err.response?.data || 'Failed to delete LO');
        }
    };

    const handleAddNewLo = async (e) => {
        e.preventDefault();
        try {
            const token = localStorage.getItem('token');
            await axios.post(
                `http://localhost:8080/api/lospos/${module.moduleId}/add`,
                newLoData,
                {
                    headers: { 'Authorization': `Bearer ${token}` }
                }
            );
            fetchLosPos();
            setAddingNew(false);
            setNewLoData({ id: '', name: '' });
        } catch (err) {
            console.error('Error adding LO:', err);
            alert(err.response?.data || 'Failed to add LO');
        }
    };

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-xl w-full max-w-3xl max-h-[90vh] overflow-hidden flex flex-col">
                {/* Header */}
                <div className="bg-blue-600 text-white p-6">
                    <div className="flex justify-between items-center">
                        <div>
                            <h2 className="text-2xl font-bold">{module.moduleId} {module.moduleName}</h2>
                            <p className="text-blue-100 mt-1">Learning Outcomes</p>
                        </div>
                        <button
                            onClick={onClose}
                            className="text-white hover:bg-blue-700 p-2 rounded-full transition-colors"
                        >
                            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                            </svg>
                        </button>
                    </div>
                </div>

                {/* Content */}
                <div className="flex-1 overflow-y-auto p-6">
                    {/* Add New Button */}
                    <div className="mb-4">
                        <button
                            onClick={() => setAddingNew(true)}
                            className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors flex items-center gap-2"
                        >
                            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" />
                            </svg>
                            Add New LO
                        </button>
                    </div>

                    {loading ? (
                        <div className="text-center py-8">
                            <div className="inline-block animate-spin rounded-full h-10 w-10 border-b-2 border-blue-600"></div>
                        </div>
                    ) : (
                        <div className="space-y-3">
                            {losPosList.map((lo) => (
                                <div
                                    key={lo.id}
                                    className="bg-gradient-to-r from-cyan-400 to-cyan-500 p-4 rounded-lg shadow-md flex items-center justify-between"
                                >
                                    <span className="text-white font-medium flex-1">
                                        {lo.id}: {lo.name}
                                    </span>
                                    <div className="flex gap-2">
                                        {/* Edit Icon */}
                                        <button
                                            onClick={() => handleEditLo(lo)}
                                            className="p-2 bg-white bg-opacity-20 hover:bg-opacity-30 rounded-lg transition-colors"
                                            title="Edit"
                                        >
                                            <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                                            </svg>
                                        </button>
                                        {/* Delete Icon */}
                                        <button
                                            onClick={() => setDeletingLo(lo)}
                                            className="p-2 bg-white bg-opacity-20 hover:bg-opacity-30 rounded-lg transition-colors"
                                            title="Delete"
                                        >
                                            <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                            </svg>
                                        </button>
                                    </div>
                                </div>
                            ))}

                            {losPosList.length === 0 && (
                                <p className="text-center text-gray-500 py-8">No learning outcomes found for this module.</p>
                            )}
                        </div>
                    )}
                </div>
            </div>

            {/* Edit LO Modal */}
            {editingLo && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-[60]">
                    <div className="bg-white rounded-xl p-6 w-full max-w-md">
                        <h3 className="text-xl font-bold mb-4">Edit Learning Outcome</h3>
                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-2">LO ID</label>
                                <input
                                    type="text"
                                    value={editingLo.id}
                                    disabled
                                    className="w-full px-4 py-2 border border-gray-300 rounded-lg bg-gray-100"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-2">LO Name</label>
                                <input
                                    type="text"
                                    value={editingLo.name}
                                    onChange={(e) => setEditingLo({ ...editingLo, name: e.target.value })}
                                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500"
                                />
                            </div>
                            <div className="flex gap-2">
                                <button
                                    onClick={() => setEditingLo(null)}
                                    className="flex-1 px-4 py-2 bg-gray-300 text-gray-700 rounded-lg hover:bg-gray-400"
                                >
                                    Cancel
                                </button>
                                <button
                                    onClick={handleUpdateLo}
                                    className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
                                >
                                    Update
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* Delete LO Confirmation Modal */}
            {deletingLo && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-[60]">
                    <div className="bg-white rounded-xl p-6 w-full max-w-md">
                        <h3 className="text-xl font-bold mb-4 text-red-600">Delete Learning Outcome</h3>
                        <p className="mb-6">
                            Are you sure you want to delete <strong>{deletingLo.name}</strong>? This action cannot be undone.
                        </p>
                        <div className="flex gap-2">
                            <button
                                onClick={() => setDeletingLo(null)}
                                className="flex-1 px-4 py-2 bg-gray-300 text-gray-700 rounded-lg hover:bg-gray-400"
                            >
                                Cancel
                            </button>
                            <button
                                onClick={() => handleDeleteLo(deletingLo.id)}
                                className="flex-1 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700"
                            >
                                Delete
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* Add New LO Modal */}
            {addingNew && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-[60]">
                    <div className="bg-white rounded-xl p-6 w-full max-w-md">
                        <h3 className="text-xl font-bold mb-4">Add New Learning Outcome</h3>
                        <form onSubmit={handleAddNewLo} className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-2">LO ID</label>
                                <input
                                    type="text"
                                    value={newLoData.id}
                                    onChange={(e) => setNewLoData({ ...newLoData, id: e.target.value })}
                                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500"
                                    placeholder="e.g., LO1"
                                    required
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-2">LO Name</label>
                                <input
                                    type="text"
                                    value={newLoData.name}
                                    onChange={(e) => setNewLoData({ ...newLoData, name: e.target.value })}
                                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-green-500"
                                    placeholder="Enter learning outcome description"
                                    required
                                />
                            </div>
                            <div className="flex gap-2">
                                <button
                                    type="button"
                                    onClick={() => {
                                        setAddingNew(false);
                                        setNewLoData({ id: '', name: '' });
                                    }}
                                    className="flex-1 px-4 py-2 bg-gray-300 text-gray-700 rounded-lg hover:bg-gray-400"
                                >
                                    Cancel
                                </button>
                                <button
                                    type="submit"
                                    className="flex-1 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700"
                                >
                                    Add
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}
