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
                const response = await axios.get(`/api/lospos/module/${module.moduleId}`, {
                    headers: { 'Authorization': `Bearer ${token}` }
                });
                const payload = response?.data;
                const list = Array.isArray(payload) ? payload : (payload?.data || []);
                setLos(Array.isArray(list) ? list : []);
            } catch (error) {
                console.error('Error fetching LOs:', error);
                setLos([]);
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
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm z-[100] flex items-center justify-center p-4 md:p-8 animate-in fade-in duration-300">
            <div className="w-full max-w-3xl max-h-[86vh] bg-white rounded-3xl shadow-2xl border border-slate-200 overflow-hidden flex flex-col animate-in zoom-in-95 duration-300">
                <div className="px-6 md:px-8 py-6 border-b border-slate-200 flex items-start justify-between bg-slate-50">
                    <div>
                        <span className="inline-flex items-center px-3 py-1 rounded-md bg-indigo-100 text-indigo-700 text-xs font-semibold mb-3">
                            {module.moduleId}
                        </span>
                        <h2 className="text-2xl md:text-3xl font-bold text-slate-900 leading-tight">
                            {module.moduleName}
                        </h2>
                        <p className="text-sm text-slate-600 mt-2">Learning outcomes for this module</p>
                    </div>
                    <button
                        onClick={onClose}
                        className="p-2.5 rounded-xl bg-white border border-slate-200 text-slate-500 hover:text-slate-700 hover:bg-slate-100 transition-colors"
                        aria-label="Close module dialog"
                    >
                        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M6 18L18 6M6 6l12 12" />
                        </svg>
                    </button>
                </div>

                <div className="flex-1 overflow-y-auto px-6 md:px-8 py-6">
                    {loading ? (
                        <div className="flex flex-col items-center justify-center py-14">
                            <div className="w-10 h-10 border-4 border-indigo-200 border-t-indigo-600 rounded-full animate-spin" />
                            <p className="text-slate-500 mt-4 text-sm font-medium">Loading learning outcomes...</p>
                        </div>
                    ) : los.length > 0 ? (
                        <div className="space-y-3">
                            {los.map((lo) => (
                                <button
                                    key={lo.id}
                                    onClick={() => handleLOClick(lo.id)}
                                    className="w-full text-left p-4 rounded-xl border border-slate-200 hover:border-indigo-300 hover:bg-indigo-50/40 transition-all duration-200 group"
                                >
                                    <div className="flex items-start gap-4">
                                        <div className="w-11 h-11 rounded-lg bg-indigo-100 text-indigo-700 flex items-center justify-center text-xs font-bold flex-shrink-0">
                                            {lo.id?.includes('_') ? lo.id.split('_').pop() : 'LO'}
                                        </div>
                                        <div className="flex-1 min-w-0">
                                            <p className="text-xs font-semibold text-slate-500 mb-1">Learning Outcome</p>
                                            <p className="text-base font-semibold text-slate-800 group-hover:text-indigo-700 transition-colors leading-snug">
                                                {lo.name || lo.description || lo.id}
                                            </p>
                                        </div>
                                        <svg className="w-5 h-5 text-slate-400 group-hover:text-indigo-600 group-hover:translate-x-0.5 transition-all" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M9 5l7 7-7 7" />
                                        </svg>
                                    </div>
                                </button>
                            ))}
                        </div>
                    ) : (
                        <div className="text-center py-14 rounded-2xl border border-dashed border-slate-300 bg-slate-50">
                            <div className="w-14 h-14 rounded-xl bg-white border border-slate-200 text-slate-300 flex items-center justify-center mx-auto mb-4">
                                <svg className="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                                </svg>
                            </div>
                            <h3 className="text-lg font-semibold text-slate-700 mb-2">No learning outcomes found</h3>
                            <p className="text-slate-500 text-sm max-w-md mx-auto">
                                This module does not have any learning outcomes yet. Use the create action below to add the first one.
                            </p>
                        </div>
                    )}
                </div>

                <div className="px-6 md:px-8 py-5 border-t border-slate-200 bg-white">
                    <div className="flex flex-col md:flex-row md:items-center gap-3 md:justify-between">
                        <div className="flex flex-col sm:flex-row gap-3">
                            <button
                                onClick={() => {
                                    onClose();
                                    navigate(`/create-lo-mapping/${module.moduleId}`);
                                }}
                                className="inline-flex items-center justify-center px-4 py-2.5 rounded-lg bg-indigo-600 text-white text-sm font-semibold hover:bg-indigo-700 transition-colors"
                            >
                                <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" />
                                </svg>
                                Create LO with PO Mapping
                            </button>
                            <button
                                onClick={() => {
                                    onClose();
                                    navigate('/lo-po-mappings');
                                }}
                                className="inline-flex items-center justify-center px-4 py-2.5 rounded-lg border border-indigo-200 text-indigo-700 bg-indigo-50 hover:bg-indigo-100 text-sm font-semibold transition-colors"
                            >
                                <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                                </svg>
                                View LO-PO Mappings
                            </button>
                        </div>
                        <button
                            onClick={onClose}
                            className="inline-flex items-center justify-center px-4 py-2.5 rounded-lg border border-slate-300 text-slate-600 hover:bg-slate-50 text-sm font-semibold transition-colors"
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
