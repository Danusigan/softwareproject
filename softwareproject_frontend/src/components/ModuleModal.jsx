import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

const ModuleModal = ({ module, onClose }) => {
    const [los, setLos] = useState([]);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();

    useEffect(() => {
        const fetchLOs = async () => {
            try {
                const token = localStorage.getItem('token');
                const response = await axios.get(`http://localhost:8080/api/lospos/module/${module.moduleId}`, {
                    headers: { 'Authorization': `Bearer ${token}` }
                });
                setLos(response.data);
            } catch (error) {
                console.error('Error fetching LOs:', error);
            } finally {
                setLoading(false);
            }
        };

        if (module && module.moduleId) {
            fetchLOs();
        }
    }, [module]);

    const handleLOClick = (loId) => {
        onClose(); // Close the modal before navigating
        navigate(`/lo-detail/${loId}`);
    };

    if (!module) return null;

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-xl shadow-2xl w-full max-w-2xl max-h-[80vh] flex flex-col overflow-hidden animate-in fade-in zoom-in duration-200">
                {/* Header */}
                <div className="px-6 py-4 border-b border-gray-100 flex items-center justify-between bg-gray-50">
                    <div>
                        <h2 className="text-xl font-bold text-gray-800">{module.moduleName}</h2>
                        <p className="text-sm text-gray-500 font-medium">{module.moduleId}</p>
                    </div>
                    <button
                        onClick={onClose}
                        className="p-1 hover:bg-gray-200 rounded-full transition-colors text-gray-500 hover:text-gray-700"
                    >
                        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                        </svg>
                    </button>
                </div>

                {/* Content */}
                <div className="flex-1 overflow-y-auto p-6">
                    <h3 className="text-lg font-semibold text-gray-700 mb-4 flex items-center gap-2">
                        <svg className="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5s3.332.477 4.5 1.253v13C19.832 18.477 18.246 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                        </svg>
                        Learning Objectives
                    </h3>

                    {loading ? (
                        <div className="flex flex-col items-center justify-center py-12">
                            <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-600 mb-4"></div>
                            <p className="text-gray-500">Loading learning objectives...</p>
                        </div>
                    ) : los.length > 0 ? (
                        <div className="space-y-4">
                            {los.map((lo) => (
                                <div
                                    key={lo.id}
                                    onClick={() => handleLOClick(lo.id)}
                                    className="p-4 bg-blue-50 border border-blue-100 rounded-lg hover:shadow-md transition-shadow cursor-pointer hover:bg-blue-100 group"
                                >
                                    <div className="flex items-start gap-3">
                                        <span className="px-2 py-1 bg-blue-600 text-white text-xs font-bold rounded mt-0.5 whitespace-nowrap group-hover:bg-blue-700">
                                            {lo.id?.includes('_') ? `LO ${lo.id.split('_').pop()}` : lo.id}
                                        </span>
                                        <p className="text-gray-700 leading-relaxed font-medium group-hover:text-blue-900">
                                            {lo.name}
                                        </p>
                                    </div>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <div className="text-center py-12 bg-gray-50 rounded-xl border-2 border-dashed border-gray-200">
                            <svg className="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                            </svg>
                            <h3 className="mt-2 text-sm font-medium text-gray-900">No Learning Objectives</h3>
                            <p className="mt-1 text-sm text-gray-500">No learning objectives found for this module.</p>
                        </div>
                    )}
                </div>

                {/* Footer */}
                <div className="px-6 py-4 bg-gray-50 border-t border-gray-100 flex justify-end">
                    <button
                        onClick={onClose}
                        className="px-6 py-2 bg-white border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors font-medium shadow-sm"
                    >
                        Close
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ModuleModal;
