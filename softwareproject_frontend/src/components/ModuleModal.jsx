import React, { useState, useEffect } from 'react';
import axios from 'axios';

const ModuleModal = ({ module, onClose }) => {
    const [losPosList, setLosPosList] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (module) {
            fetchLosPos();
        }
    }, [module]);

    const fetchLosPos = async () => {
        try {
            setLoading(true);
            setError(null);
            // Fetch Learning Outcomes/Positions for this module
            const response = await axios.get(`http://localhost:8080/api/lospos/module/${module.moduleId}`);
            setLosPosList(response.data);
        } catch (err) {
            console.error('Error fetching LosPos:', err);
            setError('Failed to load learning outcomes');
            setLosPosList([]);
        } finally {
            setLoading(false);
        }
    };

    const handleBackdropClick = (e) => {
        if (e.target === e.currentTarget) {
            onClose();
        }
    };

    if (!module) return null;

    return (
        <div 
            className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4"
            onClick={handleBackdropClick}
        >
            <div className="bg-white rounded-xl shadow-2xl w-full max-w-2xl max-h-[90vh] overflow-hidden">
                {/* Header */}
                <div className="bg-blue-600 text-white p-6">
                    <div className="flex items-center justify-between">
                        <div>
                            <h2 className="text-2xl font-bold">{module.moduleId}</h2>
                            <p className="text-blue-100 mt-1">{module.moduleName}</p>
                        </div>
                        <button
                            onClick={onClose}
                            className="text-white hover:text-blue-200 transition-colors"
                            aria-label="Close modal"
                        >
                            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                            </svg>
                        </button>
                    </div>
                </div>

                {/* Content */}
                <div className="p-6 overflow-y-auto max-h-[calc(90vh-120px)]">
                    <div className="mb-6">
                        <h3 className="text-lg font-semibold text-gray-800 mb-4">Learning Outcomes</h3>
                        
                        {loading ? (
                            <div className="flex items-center justify-center py-8">
                                <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
                                <span className="ml-2 text-gray-600">Loading learning outcomes...</span>
                            </div>
                        ) : error ? (
                            <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-center">
                                <svg className="w-8 h-8 text-red-400 mx-auto mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.96-.833-2.73 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z" />
                                </svg>
                                <p className="text-red-700 font-medium">{error}</p>
                            </div>
                        ) : losPosList.length > 0 ? (
                            <div className="space-y-3">
                                {losPosList.map((losPos, index) => (
                                    <div key={losPos.id || index} className="bg-gray-50 border border-gray-200 rounded-lg p-4">
                                        <div className="flex items-start justify-between">
                                            <div className="flex-1">
                                                <h4 className="font-medium text-gray-900">{losPos.name}</h4>
                                                {losPos.description && (
                                                    <p className="text-gray-600 mt-1 text-sm">{losPos.description}</p>
                                                )}
                                                {losPos.category && (
                                                    <span className="inline-block bg-blue-100 text-blue-800 text-xs px-2 py-1 rounded-full mt-2">
                                                        {losPos.category}
                                                    </span>
                                                )}
                                            </div>
                                            <span className="text-sm text-gray-500 font-medium ml-4">
                                                LO {index + 1}
                                            </span>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        ) : (
                            <div className="text-center py-8">
                                <svg className="w-12 h-12 text-gray-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                                </svg>
                                <p className="text-gray-600 font-medium">No learning outcomes found</p>
                                <p className="text-gray-500 text-sm mt-1">This module doesn't have any learning outcomes yet.</p>
                            </div>
                        )}
                    </div>
                </div>

                {/* Footer */}
                <div className="bg-gray-50 px-6 py-4 border-t border-gray-200">
                    <div className="flex justify-end">
                        <button
                            onClick={onClose}
                            className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors font-medium"
                        >
                            Close
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ModuleModal;