import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../components/header';
import Footer from '../components/footer';
import axios from 'axios';

export default function LecturerDashboard() {
    const navigate = useNavigate();
    const [modules, setModules] = useState([]);
    const [selectedModule, setSelectedModule] = useState(null);
    const [activeModuleMenuId, setActiveModuleMenuId] = useState(null);
    const [los, setLos] = useState([]);
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState({ type: '', text: '' });
    const [showEditLoDialog, setShowEditLoDialog] = useState(false);
    const [editingLo, setEditingLo] = useState(null);

    const [loData, setLoData] = useState({ loNumber: '', description: '' });

    const extractDisplayLoNumber = (loId) => {
        if (!loId) return '';
        const parts = String(loId).trim().split(' ');
        return parts.length > 1 ? parts[parts.length - 1] : String(loId).trim();
    };

    const getStatusForLo = (loId) => {
        const mappings = los.find(lo => lo.id === loId)?.mappings;
        if (!mappings || mappings.length === 0) return { status: 'UNMAPPED', badge: 'bg-gray-100 text-gray-800 border-gray-200' };
        if (mappings.some(m => m.status === 'PENDING')) return { status: 'PENDING', badge: 'bg-yellow-100 text-yellow-800 border-yellow-200' };
        if (mappings.every(m => m.status === 'APPROVED')) return { status: 'APPROVED', badge: 'bg-green-100 text-green-800 border-green-200' };
        if (mappings.some(m => m.status === 'REJECTED')) return { status: 'REJECTED', badge: 'bg-red-100 text-red-800 border-red-200' };
        return { status: 'MIXED', badge: 'bg-blue-100 text-blue-800 border-blue-200' };
    };

    const getRejectedFeedback = (loId) => {
        const rejected = (los.find(lo => lo.id === loId)?.mappings || []).filter(m => m.status === 'REJECTED');
        return { count: rejected.length, firstMessage: rejected.find(m => (m.adminRemarks || '').trim())?.adminRemarks || '' };
    };

    useEffect(() => { fetchModules(); }, []);

    useEffect(() => {
        if (selectedModule) fetchLosForModule(selectedModule.moduleId);
    }, [selectedModule]);

    useEffect(() => {
        if (!activeModuleMenuId) return;
        const handleClick = (e) => { if (!e.target.closest('[data-module-action-menu]')) setActiveModuleMenuId(null); };
        const handleEsc = (e) => { if (e.key === 'Escape') setActiveModuleMenuId(null); };
        document.addEventListener('click', handleClick);
        document.addEventListener('keydown', handleEsc);
        return () => { document.removeEventListener('click', handleClick); document.removeEventListener('keydown', handleEsc); };
    }, [activeModuleMenuId]);

    const fetchModules = async () => {
        try {
            const token = localStorage.getItem('token');
            const res = await axios.get('http://localhost:8080/api/modules/all', { headers: { Authorization: `Bearer ${token}` } });
            setModules(res.data.data || []);
        } catch { setModules([]); }
    };

    const fetchLosForModule = async (moduleId) => {
        try {
            const token = localStorage.getItem('token');
            const res = await axios.get(`http://localhost:8080/api/lospos/module/${moduleId}`, { headers: { Authorization: `Bearer ${token}` } });
            setLos(res.data.data || []);
        } catch { setLos([]); }
    };

    const openLoCreationInNewWindow = (moduleId) => {
        window.open(`${window.location.origin}/create-lo-mapping/${moduleId}`, '_blank', 'noopener,noreferrer');
    };

    const toggleModuleMenu = (moduleId) => setActiveModuleMenuId(prev => prev === moduleId ? null : moduleId);

    const handleEditLo = async (e) => {
        e.preventDefault();
        setLoading(true);
        try {
            const token = localStorage.getItem('token');
            await axios.put(
                `http://localhost:8080/api/lospos/${editingLo.id}`,
                { id: loData.loNumber, name: `LO ${loData.loNumber}`, description: loData.description },
                { headers: { Authorization: `Bearer ${token}` } }
            );
            setMessage({ type: 'success', text: 'LO updated successfully!' });
            setShowEditLoDialog(false);
            setEditingLo(null);
            setLoData({ loNumber: '', description: '' });
            fetchLosForModule(selectedModule.moduleId);
        } catch (err) {
            setMessage({ type: 'error', text: err.response?.data || 'Failed to update LO' });
        } finally { setLoading(false); }
    };

    const handleDeleteLo = async (loId) => {
        if (!window.confirm('Are you sure you want to delete this LO?')) return;
        try {
            const token = localStorage.getItem('token');
            await axios.delete(`http://localhost:8080/api/lospos/${loId}`, { headers: { Authorization: `Bearer ${token}` } });
            setMessage({ type: 'success', text: 'LO deleted successfully!' });
            fetchLosForModule(selectedModule.moduleId);
        } catch (err) {
            setMessage({ type: 'error', text: err.response?.data || 'Failed to delete LO' });
        }
    };

    const openEditDialog = (lo) => {
        setEditingLo(lo);
        setLoData({ loNumber: extractDisplayLoNumber(lo.id), description: lo.description || lo.name });
        setShowEditLoDialog(true);
    };

    const closeModulePanel = () => {
        setSelectedModule(null);
        setLos([]);
        setShowEditLoDialog(false);
        setMessage({ type: '', text: '' });
    };

    return (
        <div className="min-h-screen bg-[#f8fafc] flex flex-col relative overflow-hidden">
            <Header />
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
                    <div className={`mb-8 flex items-center gap-3 p-4 rounded-2xl text-sm font-bold animate-in slide-in-from-top-4 duration-300 ${message.type === 'success' ? 'bg-emerald-50 text-emerald-700' : 'bg-red-50 text-red-700'}`}>
                        <svg className="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d={message.type === 'success' ? 'M5 13l4 4L19 7' : 'M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z'} />
                        </svg>
                        {message.text}
                    </div>
                )}

                {/* Modules Grid */}
                <div className="space-y-8">
                    <div className="flex items-center gap-4">
                        <h2 className="heading-lg text-slate-800">Your Assigned Modules</h2>
                        <div className="h-px flex-1 bg-slate-100" />
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
                                    onClick={() => toggleModuleMenu(module.moduleId)}
                                    className="relative glass-card group rounded-[2.5rem] p-8 cursor-pointer transition-all duration-500 hover:shadow-2xl hover:shadow-indigo-500/10 hover:-translate-y-2 border-slate-100 hover:border-indigo-200 bg-white/40"
                                    data-module-action-menu
                                >
                                    <div className="flex justify-between items-start mb-8">
                                        <div className="w-16 h-16 bg-indigo-50 text-indigo-600 rounded-2xl flex items-center justify-center group-hover:scale-110 group-hover:bg-indigo-600 group-hover:text-white transition-all duration-500 shadow-sm">
                                            <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                                            </svg>
                                        </div>
                                        <span className="px-3 py-1.5 bg-slate-50 text-[11px] font-semibold text-slate-600 rounded-lg border border-slate-200">
                                            Select Action
                                        </span>
                                    </div>
                                    <h3 className="text-2xl font-black text-slate-800 leading-tight group-hover:text-indigo-600 transition-colors">
                                        {module.moduleName}
                                    </h3>
                                    <div className="mt-3 inline-flex items-center gap-2 px-3 py-1.5 rounded-lg bg-indigo-50 border border-indigo-100 text-indigo-700">
                                        <span className="text-[11px] font-medium">Module ID</span>
                                        <span className="text-sm font-bold tracking-wide">{module.moduleId}</span>
                                    </div>
                                    <div className="mt-8 flex items-center justify-between text-slate-400">
                                        <span className="text-[11px] font-medium">Click card to choose next step</span>
                                        <div className="p-2 bg-slate-50 rounded-full group-hover:bg-indigo-50 group-hover:text-indigo-600 transition-colors">
                                            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M9 5l7 7-7 7" />
                                            </svg>
                                        </div>
                                    </div>

                                    {/* Action Menu Dropdown */}
                                    {activeModuleMenuId === module.moduleId && (
                                        <div
                                            className="absolute left-8 right-8 top-[92px] z-20 bg-white border border-slate-200 rounded-2xl shadow-2xl shadow-slate-900/10 p-3 space-y-2 animate-in fade-in zoom-in-95 duration-200"
                                            onClick={e => e.stopPropagation()}
                                        >
                                            <button
                                                type="button"
                                                onClick={() => { openLoCreationInNewWindow(module.moduleId); setActiveModuleMenuId(null); }}
                                                className="w-full text-left px-4 py-3 rounded-xl bg-indigo-50 text-indigo-700 hover:bg-indigo-100 transition-colors flex items-center gap-3"
                                            >
                                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.2" d="M12 4v16m8-8H4" />
                                                </svg>
                                                <div>
                                                    <p className="text-sm font-semibold">Create LO</p>
                                                    <p className="text-[11px] text-indigo-600/80">Opens LO creation in a new window</p>
                                                </div>
                                            </button>

                                            <button
                                                type="button"
                                                onClick={() => { setActiveModuleMenuId(null); setSelectedModule(module); }}
                                                className="w-full text-left px-4 py-3 rounded-xl bg-sky-50 text-sky-700 hover:bg-sky-100 transition-colors flex items-center gap-3"
                                            >
                                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                                                </svg>
                                                <div>
                                                    <p className="text-sm font-semibold">View / Manage LOs</p>
                                                    <p className="text-[11px] text-sky-700/80">View existing LOs, edit or delete them</p>
                                                </div>
                                            </button>

                                            <button
                                                type="button"
                                                onClick={() => { setActiveModuleMenuId(null); navigate('/lo-po-mappings'); }}
                                                className="w-full text-left px-4 py-3 rounded-xl bg-slate-50 text-slate-700 hover:bg-slate-100 transition-colors flex items-center gap-3"
                                            >
                                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.2" d="M9 17v-6m3 6V7m3 10v-4m3 4H6a2 2 0 01-2-2V5a2 2 0 012-2h12a2 2 0 012 2v10a2 2 0 01-2 2z" />
                                                </svg>
                                                <div>
                                                    <p className="text-sm font-semibold">View LO-PO Mappings</p>
                                                    <p className="text-[11px] text-slate-500">Open approval and mapping status page</p>
                                                </div>
                                            </button>

                                            <button
                                                type="button"
                                                onClick={() => { setActiveModuleMenuId(null); navigate(`/marks-workbench/${module.moduleId}`); }}
                                                className="w-full text-left px-4 py-3 rounded-xl bg-emerald-50 text-emerald-700 hover:bg-emerald-100 transition-colors flex items-center gap-3"
                                            >
                                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.2" d="M3 7h18M3 12h18M3 17h18" />
                                                </svg>
                                                <div>
                                                    <p className="text-sm font-semibold">Marks &amp; Analytics</p>
                                                    <p className="text-[11px] text-emerald-700/80">Bulk upload, analytics charts, PO attainment</p>
                                                </div>
                                            </button>
                                        </div>
                                    )}
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </main>

            {/* Module Detail Panel */}
            {selectedModule && (
                <>
                    <div className="fixed top-0 right-0 h-full w-full md:w-[600px] glass-card-dark text-white shadow-[-20px_0_60px_rgba(0,0,0,0.2)] transition-transform duration-500 ease-out z-[60] flex flex-col"
                        style={{ borderLeft: '1px solid rgba(255,255,255,0.05)' }}>

                        {/* Panel Header */}
                        <div className="p-8 pb-6 border-b border-white/5 space-y-6">
                            <div className="flex justify-between items-start">
                                <div className="space-y-1">
                                    <span className="text-[10px] font-black text-indigo-400 uppercase tracking-widest">Selected Module</span>
                                    <h2 className="text-3xl font-black tracking-tight">{selectedModule.moduleName}</h2>
                                    <p className="text-indigo-300 text-xs font-bold uppercase tracking-widest">Code: {selectedModule.moduleId}</p>
                                </div>
                                <button onClick={closeModulePanel} className="p-2 hover:bg-white/10 rounded-xl transition-colors text-white/50 hover:text-white">
                                    <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                                    </svg>
                                </button>
                            </div>

                            {/* Panel quick actions */}
                            <div className="space-y-3 pt-2">
                                <button
                                    onClick={() => navigate(`/marks-workbench/${selectedModule.moduleId}`)}
                                    className="w-full flex items-center justify-center gap-3 py-4 bg-emerald-600 hover:bg-emerald-700 text-white rounded-2xl font-black text-sm uppercase tracking-widest transition-all duration-300 shadow-xl shadow-emerald-500/10"
                                >
                                    <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M3 7h18M3 12h18M3 17h18" />
                                    </svg>
                                    Marks &amp; Analytics
                                </button>

                                <button
                                    onClick={() => openLoCreationInNewWindow(selectedModule.moduleId)}
                                    className="w-full flex items-center justify-center gap-3 py-4 bg-indigo-500 hover:bg-indigo-600 text-white rounded-2xl font-black text-sm uppercase tracking-widest transition-all duration-300"
                                >
                                    <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M9 12h6m-6 4h6m-6-8h6m3 10H6a2 2 0 01-2-2V8a2 2 0 012-2h3.172a2 2 0 001.414-.586l.828-.828A2 2 0 0112.828 4H18a2 2 0 012 2v10a2 2 0 01-2 2z" />
                                    </svg>
                                    Create LO + PO Mapping
                                </button>

                                <button
                                    onClick={() => navigate('/lo-po-mappings')}
                                    className="w-full flex items-center justify-center gap-3 py-4 bg-white/10 hover:bg-white/15 text-white rounded-2xl font-black text-sm uppercase tracking-widest transition-all duration-300 border border-white/10"
                                >
                                    <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M13 10V3L4 14h7v7l9-11h-7z" />
                                    </svg>
                                    Review Mapping Status
                                </button>
                            </div>
                        </div>

                        {/* LOs list */}
                        <div className="flex-1 overflow-y-auto p-8 space-y-6 scrollbar-thin scrollbar-thumb-white/10">
                            <div className="flex items-center gap-4 text-white/30 mb-8">
                                <span className="text-[10px] font-black uppercase tracking-[0.3em]">Module Learning Outcomes</span>
                                <div className="h-px flex-1 bg-white/5" />
                            </div>

                            {los.length === 0 ? (
                                <div className="text-center py-20 px-10 glass-card bg-white/5 border-white/5 rounded-[2rem]">
                                    <p className="text-white/40 text-sm font-medium leading-relaxed">
                                        No Learning Outcomes defined yet. Create your first LO using the button above.
                                    </p>
                                </div>
                            ) : (
                                <div className="grid grid-cols-1 gap-5">
                                    {los.map((lo) => {
                                        const { status, badge } = getStatusForLo(lo.id);
                                        const { count: rejCount, firstMessage } = getRejectedFeedback(lo.id);
                                        return (
                                            <div
                                                key={lo.id}
                                                className="group relative bg-white/5 border border-white/5 rounded-3xl p-6 transition-all duration-300 hover:bg-white/10 hover:border-white/10 cursor-pointer"
                                                onClick={() => navigate(`/lo-detail/${lo.id}/comparisons`)}
                                            >
                                                <div className="flex justify-between items-start gap-4">
                                                    <div className="space-y-4 flex-1">
                                                        <div className="flex items-center gap-3 flex-wrap">
                                                            <span className="px-4 py-1.5 bg-indigo-500/20 text-indigo-300 rounded-xl text-[10px] font-black uppercase tracking-widest border border-indigo-500/20">
                                                                LO {extractDisplayLoNumber(lo.id)}
                                                            </span>
                                                            <span className={`px-3 py-1 rounded-lg text-[10px] font-black uppercase tracking-widest border ${badge}`}>
                                                                {status}
                                                            </span>
                                                            {rejCount > 0 && (
                                                                <span className="px-3 py-1 rounded-lg text-[10px] font-black uppercase tracking-widest border bg-red-500/20 text-red-300 border-red-500/30">
                                                                    {rejCount} Rejected
                                                                </span>
                                                            )}
                                                        </div>
                                                        <p className="text-lg font-bold text-white/90 leading-snug group-hover:text-white transition-colors">
                                                            {lo.description || lo.name}
                                                        </p>
                                                        {rejCount > 0 && (
                                                            <div className="rounded-xl border border-red-500/20 bg-red-500/10 px-4 py-3">
                                                                <p className="text-[10px] font-black uppercase tracking-widest text-red-300 mb-2">Admin feedback</p>
                                                                <p className="text-sm text-red-100/90 leading-relaxed">
                                                                    {firstMessage || 'One or more mappings were rejected. View comparisons for details.'}
                                                                </p>
                                                            </div>
                                                        )}
                                                    </div>

                                                    {/* Edit / Delete — only when not APPROVED */}
                                                    {status !== 'APPROVED' && (
                                                        <div className="flex gap-2">
                                                            <button
                                                                onClick={e => { e.stopPropagation(); openEditDialog(lo); }}
                                                                className="w-10 h-10 flex items-center justify-center bg-white/5 text-white/40 hover:bg-indigo-500 hover:text-white rounded-xl transition-all duration-300"
                                                                title="Edit LO"
                                                            >
                                                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                                                                </svg>
                                                            </button>
                                                            <button
                                                                onClick={e => { e.stopPropagation(); handleDeleteLo(lo.id); }}
                                                                className="w-10 h-10 flex items-center justify-center bg-white/5 text-white/40 hover:bg-red-500 hover:text-white rounded-xl transition-all duration-300"
                                                                title="Delete LO"
                                                            >
                                                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                                                </svg>
                                                            </button>
                                                        </div>
                                                    )}
                                                </div>

                                                <div className="mt-6 flex items-center gap-3 text-white/20 text-[10px] font-black uppercase tracking-widest">
                                                    <span>View trend analytics</span>
                                                    <div className="h-px flex-1 bg-white/5" />
                                                    <svg className="w-4 h-4 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M17 8l4 4m0 0l-4 4m4-4H3" />
                                                    </svg>
                                                </div>
                                            </div>
                                        );
                                    })}
                                </div>
                            )}
                        </div>
                    </div>

                    {/* Overlay */}
                    <div onClick={closeModulePanel} className="fixed inset-0 bg-slate-900/60 backdrop-blur-md z-[55] animate-in fade-in duration-500" />
                </>
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
                                        onChange={e => setLoData({ ...loData, loNumber: e.target.value })}
                                        className="input-field py-4 text-xl font-black text-indigo-600"
                                        placeholder="e.g. 01"
                                        required
                                    />
                                </div>
                                <div className="space-y-3">
                                    <label className="text-xs font-black text-slate-500 uppercase tracking-widest ml-1">Competency Description</label>
                                    <textarea
                                        value={loData.description}
                                        onChange={e => setLoData({ ...loData, description: e.target.value })}
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
                                        {loading ? 'Saving...' : 'Update LO'}
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
