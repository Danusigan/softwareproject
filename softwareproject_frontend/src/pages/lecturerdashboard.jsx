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
            const res = await axios.get(`http://localhost:8080/api/lospos/module/${moduleId}`, {
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
                `http://localhost:8080/api/lospos/${selectedModule.moduleId}/add`,
                {
                    id: loData.loNumber,
                    name: `LO ${loData.loNumber}`,
                    loDescription: loData.description
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
            const errorMsg = err.response?.data || 'Failed to add LO';
            setMessage({
                type: 'error',
                text: errorMsg
            });
            alert(typeof errorMsg === 'object' ? JSON.stringify(errorMsg) : errorMsg);
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
                `http://localhost:8080/api/lospos/${editingLo.id}`,
                {
                    id: loData.loNumber,
                    name: `LO ${loData.loNumber}`,
                    loDescription: loData.description
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
            await axios.delete(`http://localhost:8080/api/lospos/${loId}`, {
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
        setLoData({ loNumber: lo.id?.includes('_') ? lo.id.split('_').pop() : lo.id, description: lo.loDescription || lo.name });
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
        <div className="min-h-screen bg-[#f8fafc] flex flex-col relative overflow-hidden">
            <Header />

            {/* Background Decorations */}
            <div className="absolute top-[-10%] right-[-10%] w-[40%] h-[40%] bg-indigo-500/5 rounded-full blur-[120px] pointer-events-none" />
            <div className="absolute bottom-[-10%] left-[-10%] w-[40%] h-[40%] bg-emerald-500/5 rounded-full blur-[120px] pointer-events-none" />

            <main className="flex-1 max-w-7xl mx-auto px-6 py-12 w-full relative z-10 animate-in fade-in duration-700">
                <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-6 mb-12">
                    <div className="space-y-2">
                        <span className="px-4 py-1.5 bg-indigo-50 text-indigo-600 rounded-full text-[10px] font-black tracking-widest uppercase inline-block">
                            Instructor Workspace
                        </span>
                        <h1 className="heading-xl bg-clip-text text-transparent bg-gradient-to-r from-slate-900 to-slate-600">
                            Lecturer Dashboard
                        </h1>
                    </div>
                </div>

                {message.text && (
                    <div className={`mb-8 flex items-center gap-3 p-4 rounded-2xl text-sm font-bold animate-in slide-in-from-top-4 duration-300
                        ${message.type === 'success' ? 'bg-emerald-50 text-emerald-700' : 'bg-red-50 text-red-700'}`}>
                        <svg className="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d={message.type === 'success' ? "M5 13l4 4L19 7" : "M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"} />
                        </svg>
                        {message.text}
                    </div>
                )}

                {/* Modules Grid */}
                <div className="space-y-8">
                    <div className="flex items-center gap-4">
                        <h2 className="heading-lg text-slate-800">Your Assigned Modules</h2>
                        <div className="h-px flex-1 bg-slate-100"></div>
                        <span className="bg-slate-100 px-3 py-1 rounded-lg text-[10px] font-black text-slate-400 uppercase tracking-widest">
                            {modules.length} Total
                        </span>
                    </div>

                    {modules.length === 0 ? (
                        <div className="glass-card rounded-[2.5rem] p-20 text-center border-dashed border-2">
                            <div className="w-20 h-20 bg-slate-50 text-slate-200 rounded-3xl flex items-center justify-center mx-auto mb-6">
                                <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                                </svg>
                            </div>
                            <h3 className="text-xl font-bold text-slate-400">No modules assigned yet</h3>
                        </div>
                    ) : (
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
                            {modules.map((module) => (
                                <div
                                    key={module.moduleId}
                                    onClick={() => setSelectedModule(module)}
                                    className="glass-card group rounded-[2.5rem] p-8 cursor-pointer transition-all duration-500 hover:shadow-2xl hover:shadow-indigo-500/10 hover:-translate-y-2 border-slate-100 hover:border-indigo-200 bg-white/40"
                                >
                                    <div className="flex justify-between items-start mb-8">
                                        <div className="w-16 h-16 bg-indigo-50 text-indigo-600 rounded-2xl flex items-center justify-center group-hover:scale-110 group-hover:bg-indigo-600 group-hover:text-white transition-all duration-500 shadow-sm">
                                            <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                                            </svg>
                                        </div>
                                        <div className="px-3 py-1 bg-slate-50 text-[10px] font-black text-slate-400 rounded-lg border border-slate-100 group-hover:bg-indigo-50 group-hover:text-indigo-600 transition-colors">
                                            ID: {module.moduleId}
                                        </div>
                                    </div>
                                    <h3 className="text-2xl font-black text-slate-800 leading-tight group-hover:text-indigo-600 transition-colors">
                                        {module.moduleName}
                                    </h3>
                                    <div className="mt-8 flex items-center justify-between text-slate-400">
                                        <span className="text-[10px] font-bold uppercase tracking-widest">Active Curriculum</span>
                                        <div className="p-2 bg-slate-50 rounded-full group-hover:bg-indigo-50 group-hover:text-indigo-600 transition-colors">
                                            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M9 5l7 7-7 7" />
                                            </svg>
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </main>

            {/* Module Detail Panel */}
            {selectedModule && (
                <>
                    <div
                        className="fixed top-0 right-0 h-full w-full md:w-[600px] glass-card-dark text-white shadow-[-20px_0_60px_rgba(0,0,0,0.2)] transform transition-transform duration-500 ease-out z-[60] flex flex-col"
                        style={{ borderLeft: '1px solid rgba(255,255,255,0.05)' }}
                    >
                        {/* Panel Header */}
                        <div className="p-8 pb-6 border-b border-white/5 space-y-6">
                            <div className="flex justify-between items-start">
                                <div className="space-y-1">
                                    <span className="text-[10px] font-black text-indigo-400 uppercase tracking-widest">Selected Module</span>
                                    <h2 className="text-3xl font-black tracking-tight">{selectedModule.moduleName}</h2>
                                    <p className="text-indigo-300 text-xs font-bold uppercase tracking-widest">Code: {selectedModule.moduleId}</p>
                                </div>
                                <button
                                    onClick={closeModulePanel}
                                    className="p-2 hover:bg-white/10 rounded-xl transition-colors text-white/50 hover:text-white"
                                >
                                    <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                                    </svg>
                                </button>
                            </div>

                            {/* Panel Actions */}
                            <div className="pt-2">
                                <button
                                    onClick={() => setShowAddLoDialog(true)}
                                    className="w-full flex items-center justify-center gap-3 py-4 bg-emerald-500 hover:bg-emerald-600 text-white rounded-2xl font-black text-sm uppercase tracking-widest transition-all duration-300 shadow-xl shadow-emerald-500/10 transform active:scale-[0.98]"
                                >
                                    <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M12 4v16m8-8H4" />
                                    </svg>
                                    Add Learning Outcome
                                </button>
                            </div>
                        </div>

                        {/* LOs List Area */}
                        <div className="flex-1 overflow-y-auto p-8 space-y-6 scrollbar-thin scrollbar-thumb-white/10">
                            <div className="flex items-center gap-4 text-white/30 mb-8">
                                <span className="text-[10px] font-black uppercase tracking-[0.3em]">Module Learning Outcomes</span>
                                <div className="h-px flex-1 bg-white/5"></div>
                            </div>

                            {los.length === 0 ? (
                                <div className="text-center py-20 px-10 glass-card bg-white/5 border-white/5 rounded-[2rem]">
                                    <div className="w-16 h-16 bg-white/5 rounded-2xl flex items-center justify-center mx-auto mb-6 text-white/20">
                                        <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                                        </svg>
                                    </div>
                                    <p className="text-white/40 text-sm font-medium leading-relaxed">
                                        No Learning Outcomes (LOs) defined for this module. Start by adding your first educational objective.
                                    </p>
                                </div>
                            ) : (
                                <div className="grid grid-cols-1 gap-5">
                                    {los.map((lo) => (
                                        <div
                                            key={lo.id}
                                            className="group relative bg-white/5 border border-white/5 rounded-3xl p-6 transition-all duration-300 hover:bg-white/10 hover:border-white/10 cursor-pointer"
                                            onClick={() => {
                                                const loNum = lo.id?.includes('_') ? lo.id.split('_').pop() : lo.id;
                                                sessionStorage.setItem('currentLoNumber', loNum);
                                                navigate(`/lo-detail/${lo.id}`);
                                            }}
                                        >
                                            <div className="flex justify-between items-start gap-4">
                                                <div className="space-y-4 flex-1">
                                                    <div className="flex items-center gap-3">
                                                        <span className="px-4 py-1.5 bg-indigo-500/20 text-indigo-300 rounded-xl text-[10px] font-black uppercase tracking-widest border border-indigo-500/20">
                                                            {lo.id?.includes('_') ? `LO ${lo.id.split('_').pop()}` : lo.id}
                                                        </span>
                                                        <div className="h-px w-8 bg-white/5"></div>
                                                    </div>
                                                    <p className="text-lg font-bold text-white/90 leading-snug group-hover:text-white transition-colors">
                                                        {lo.loDescription || lo.name}
                                                    </p>
                                                </div>

                                                <div className="flex gap-2">
                                                    <button
                                                        onClick={(e) => { e.stopPropagation(); openEditDialog(lo); }}
                                                        className="w-10 h-10 flex items-center justify-center bg-white/5 text-white/40 hover:bg-indigo-500 hover:text-white rounded-xl transition-all duration-300"
                                                    >
                                                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                                                        </svg>
                                                    </button>
                                                    <button
                                                        onClick={(e) => { e.stopPropagation(); handleDeleteLo(lo.id); }}
                                                        className="w-10 h-10 flex items-center justify-center bg-white/5 text-white/40 hover:bg-red-500 hover:text-white rounded-xl transition-all duration-300"
                                                    >
                                                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                                        </svg>
                                                    </button>
                                                </div>
                                            </div>

                                            <div className="mt-6 flex items-center gap-3 text-white/20 text-[10px] font-black uppercase tracking-widest">
                                                <span>View detailed metrics</span>
                                                <div className="h-px flex-1 bg-white/5"></div>
                                                <svg className="w-4 h-4 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M17 8l4 4m0 0l-4 4m4-4H3" />
                                                </svg>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>

                    {/* Overlay */}
                    <div
                        onClick={closeModulePanel}
                        className="fixed inset-0 bg-slate-900/60 backdrop-blur-md z-[55] animate-in fade-in duration-500"
                    />
                </>
            )}

            {/* Add LO Modal */}
            {showAddLoDialog && selectedModule && (
                <div className="fixed inset-0 bg-slate-950/40 backdrop-blur-xl z-[100] flex items-center justify-center p-6 animate-in zoom-in-95 duration-300">
                    <div className="bg-white rounded-[2.5rem] shadow-2xl w-full max-w-lg overflow-hidden border border-slate-100">
                        <div className="p-10">
                            <div className="flex justify-between items-center mb-10">
                                <div>
                                    <span className="text-[10px] font-black text-indigo-600 uppercase tracking-widest mb-1 block">New Objective</span>
                                    <h3 className="heading-lg">Create Learning Outcome</h3>
                                </div>
                                <button
                                    onClick={() => { setShowAddLoDialog(false); setLoData({ loNumber: '', description: '' }); }}
                                    className="p-3 bg-slate-50 text-slate-400 hover:bg-slate-100 hover:text-slate-600 rounded-2xl transition-colors"
                                >
                                    <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M6 18L18 6M6 6l12 12" />
                                    </svg>
                                </button>
                            </div>

                            <form onSubmit={handleAddLo} className="space-y-8">
                                <div className="space-y-3">
                                    <label className="text-xs font-black text-slate-500 uppercase tracking-widest ml-1">Learning Outcome Identifier</label>
                                    <input
                                        type="text"
                                        value={loData.loNumber}
                                        onChange={(e) => setLoData({ ...loData, loNumber: e.target.value })}
                                        className="input-field py-4 text-xl font-black text-indigo-600"
                                        placeholder="e.g. 01"
                                        required
                                    />
                                </div>

                                <div className="space-y-3">
                                    <label className="text-xs font-black text-slate-500 uppercase tracking-widest ml-1">Competency Description</label>
                                    <textarea
                                        value={loData.description}
                                        onChange={(e) => setLoData({ ...loData, description: e.target.value })}
                                        className="input-field min-h-[160px] resize-none leading-relaxed"
                                        placeholder="Describe the expected knowledge or skill level students should attain..."
                                        required
                                    />
                                </div>

                                <div className="pt-4 flex gap-4">
                                    <button
                                        type="button"
                                        onClick={() => { setShowAddLoDialog(false); setLoData({ loNumber: '', description: '' }); }}
                                        className="btn-secondary flex-1 py-4 font-black uppercase text-xs tracking-widest"
                                    >
                                        Dismiss
                                    </button>
                                    <button
                                        type="submit"
                                        disabled={loading}
                                        className="btn-success flex-1 py-4 font-black uppercase text-xs tracking-widest shadow-xl shadow-emerald-500/20"
                                    >
                                        {loading ? 'Processing...' : 'Provision LO'}
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            )}

            {/* Edit LO Modal */}
            {showEditLoDialog && editingLo && (
                <div className="fixed inset-0 bg-slate-950/40 backdrop-blur-xl z-[100] flex items-center justify-center p-6 animate-in zoom-in-95 duration-300">
                    <div className="bg-white rounded-[2.5rem] shadow-2xl w-full max-w-lg overflow-hidden border border-slate-100">
                        <div className="p-10">
                            <div className="flex justify-between items-center mb-10">
                                <div>
                                    <span className="text-[10px] font-black text-indigo-600 uppercase tracking-widest mb-1 block">Modification</span>
                                    <h3 className="heading-lg">Edit Learning Outcome</h3>
                                </div>
                                <button
                                    onClick={() => { setShowEditLoDialog(false); setEditingLo(null); setLoData({ loNumber: '', description: '' }); }}
                                    className="p-3 bg-slate-50 text-slate-400 hover:bg-slate-100 hover:text-slate-600 rounded-2xl transition-colors"
                                >
                                    <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M6 18L18 6M6 6l12 12" />
                                    </svg>
                                </button>
                            </div>

                            <form onSubmit={handleEditLo} className="space-y-8">
                                <div className="space-y-3">
                                    <label className="text-xs font-black text-slate-500 uppercase tracking-widest ml-1">Learning Outcome Identifier</label>
                                    <input
                                        type="text"
                                        value={loData.loNumber}
                                        onChange={(e) => setLoData({ ...loData, loNumber: e.target.value })}
                                        className="input-field py-4 text-xl font-black text-indigo-600"
                                        placeholder="e.g. 01"
                                        required
                                    />
                                </div>

                                <div className="space-y-3">
                                    <label className="text-xs font-black text-slate-500 uppercase tracking-widest ml-1">Competency Description</label>
                                    <textarea
                                        value={loData.description}
                                        onChange={(e) => setLoData({ ...loData, description: e.target.value })}
                                        className="input-field min-h-[160px] resize-none leading-relaxed"
                                        placeholder="Enter updated LO description..."
                                        required
                                    />
                                </div>

                                <div className="pt-4 flex gap-4">
                                    <button
                                        type="button"
                                        onClick={() => { setShowEditLoDialog(false); setEditingLo(null); setLoData({ loNumber: '', description: '' }); }}
                                        className="btn-secondary flex-1 py-4 font-black uppercase text-xs tracking-widest"
                                    >
                                        Discard
                                    </button>
                                    <button
                                        type="submit"
                                        disabled={loading}
                                        className="btn-primary flex-1 py-4 font-black uppercase text-xs tracking-widest shadow-xl shadow-indigo-500/20"
                                    >
                                        {loading ? 'Refining...' : 'Update LO'}
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            )}

            <Footer />
        </div>
    );
}
