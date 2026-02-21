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
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-md z-[100] flex items-center justify-center p-4 md:p-8 animate-in fade-in duration-500">
            {/* Background Decorative Element */}
            <div className="absolute top-[20%] right-[20%] w-[30%] h-[30%] bg-indigo-500/10 rounded-full blur-[100px] pointer-events-none" />

            <div className="glass-card bg-white/70 rounded-[2.5rem] shadow-2xl w-full max-w-2xl max-h-[85vh] flex flex-col overflow-hidden relative animate-in zoom-in-95 duration-500">
                {/* Header */}
                <div className="px-10 py-8 border-b border-slate-100 flex items-start justify-between relative overflow-hidden">
                    <div className="relative z-10">
                        <span className="px-3 py-1 bg-indigo-50 text-indigo-600 rounded-lg text-[10px] font-black tracking-widest uppercase inline-block mb-3 border border-indigo-100">
                            {module.moduleId}
                        </span>
                        <h2 className="text-3xl font-black text-slate-900 tracking-tight leading-tight">
                            {module.moduleName}
                        </h2>
                    </div>
                    <button
                        onClick={onClose}
                        className="p-3 bg-slate-50 text-slate-400 hover:bg-slate-100 hover:text-slate-600 rounded-2xl transition-all duration-300 relative z-10 group"
                    >
                        <svg className="w-6 h-6 transform group-hover:rotate-90 transition-transform duration-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M6 18L18 6M6 6l12 12" />
                        </svg>
                    </button>

                    {/* Abstract Header Shape */}
                    <div className="absolute top-0 right-0 w-32 h-32 bg-indigo-500/5 rounded-full -mr-16 -mt-16 blur-3xl"></div>
                </div>

                {/* Content */}
                <div className="flex-1 overflow-y-auto p-10 scrollbar-thin scrollbar-thumb-slate-200">
                    <div className="flex items-center gap-4 mb-8">
                        <h3 className="text-sm font-black text-slate-400 uppercase tracking-[0.2em] flex items-center gap-3">
                            <svg className="w-5 h-5 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                            </svg>
                            Curriculum Framework
                        </h3>
                        <div className="h-px flex-1 bg-slate-100"></div>
                    </div>

                    {loading ? (
                        <div className="flex flex-col items-center justify-center py-20 grayscale opacity-50">
                            <div className="relative">
                                <div className="w-12 h-12 border-4 border-indigo-50 rounded-full"></div>
                                <div className="w-12 h-12 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin absolute top-0 left-0"></div>
                            </div>
                            <p className="text-slate-500 font-bold mt-6 tracking-wide text-xs uppercase">Fetching Objectives...</p>
                        </div>
                    ) : los.length > 0 ? (
                        <div className="grid grid-cols-1 gap-5">
                            {los.map((lo) => (
                                <div
                                    key={lo.id}
                                    onClick={() => handleLOClick(lo.id)}
                                    className="p-6 bg-white/40 border border-white hover:border-indigo-200 rounded-[2rem] hover:shadow-xl hover:shadow-indigo-500/5 transition-all duration-300 cursor-pointer group flex items-start gap-6"
                                >
                                    <div className="w-12 h-12 bg-indigo-50 text-indigo-600 rounded-2xl flex items-center justify-center flex-shrink-0 group-hover:bg-indigo-600 group-hover:text-white transition-all duration-300 font-black text-xs">
                                        {lo.id?.includes('_') ? lo.id.split('_').pop() : 'LO'}
                                    </div>
                                    <div className="flex-1 space-y-2">
                                        <div className="flex items-center justify-between">
                                            <span className="text-[10px] font-black text-indigo-400 uppercase tracking-widest">Learning Outcome</span>
                                            <svg className="w-5 h-5 text-slate-300 group-hover:text-indigo-600 transform group-hover:translate-x-1 transition-all" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M17 8l4 4m0 0l-4 4m4-4H3" />
                                            </svg>
                                        </div>
                                        <p className="text-slate-700 leading-relaxed font-bold group-hover:text-slate-900 transition-colors">
                                            {lo.name}
                                        </p>
                                    </div>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <div className="text-center py-20 bg-slate-50/50 rounded-[2.5rem] border-2 border-dashed border-slate-100">
                            <div className="w-16 h-16 bg-white rounded-2xl flex items-center justify-center mx-auto mb-6 text-slate-200">
                                <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                                </svg>
                            </div>
                            <h3 className="text-lg font-black text-slate-400 uppercase tracking-widest mb-2">No Objectives Found</h3>
                            <p className="text-slate-400 text-sm px-10 leading-relaxed">This module currently has no learning objectives defined in the curriculum framework.</p>
                        </div>
                    )}
                </div>

                {/* Footer */}
                <div className="px-10 py-6 bg-slate-50/50 border-t border-slate-100 flex justify-end">
                    <button
                        onClick={onClose}
                        className="btn-secondary px-8 py-3 text-xs font-black uppercase tracking-[0.2em] border-slate-200 text-slate-500 hover:bg-white"
                    >
                        Dismiss Overlay
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ModuleModal;
