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
        <div className="min-h-screen bg-[#f8fafc] flex flex-col relative overflow-hidden">
            <Header />

            {/* Background Decorations */}
            <div className="absolute top-0 right-0 w-[600px] h-[600px] bg-indigo-500/5 rounded-full blur-[120px] pointer-events-none" />
            <div className="absolute bottom-0 left-0 w-[600px] h-[600px] bg-emerald-500/5 rounded-full blur-[120px] pointer-events-none" />

            <main className="flex-1 max-w-7xl mx-auto px-6 py-12 w-full relative z-10 animate-in fade-in duration-700">
                <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-6 mb-12">
                    <div className="space-y-2">
                        <span className="px-4 py-1.5 bg-indigo-50 text-indigo-600 rounded-full text-[10px] font-black tracking-widest uppercase inline-block">
                            Course Repository
                        </span>
                        <h1 className="heading-xl bg-clip-text text-transparent bg-gradient-to-r from-slate-900 to-slate-600">
                            Module Management
                        </h1>
                    </div>
                </div>

                {loading ? (
                    <div className="flex flex-col items-center justify-center py-24 glass-card rounded-[2.5rem]">
                        <div className="relative">
                            <div className="w-16 h-16 border-4 border-indigo-100 rounded-full"></div>
                            <div className="w-16 h-16 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin absolute top-0 left-0"></div>
                        </div>
                        <p className="text-slate-500 font-bold mt-8 tracking-wide">Syncing module database...</p>
                    </div>
                ) : (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
                        {modules.map((module, index) => (
                            <div key={module.moduleId} className="glass-card group rounded-[2.5rem] overflow-hidden transition-all duration-500 hover:shadow-2xl hover:shadow-indigo-500/10 hover:-translate-y-2 border-slate-100 bg-white/40">
                                {/* Module Header Visual */}
                                <div
                                    className={`h-40 cursor-pointer overflow-hidden relative group-hover:scale-[1.02] transition-transform duration-700 ${moduleColors[index % moduleColors.length]} opacity-80`}
                                    onClick={() => handleModuleClick(module)}
                                >
                                    <div className="absolute inset-0 bg-gradient-to-br from-white/20 to-transparent"></div>
                                    <div className="absolute bottom-6 left-8">
                                        <div className="px-3 py-1 bg-white/20 backdrop-blur-md rounded-lg text-white text-[10px] font-black uppercase tracking-widest border border-white/20">
                                            {module.moduleId}
                                        </div>
                                    </div>
                                    {/* Abstract shapes for premium feel */}
                                    <div className="absolute -top-10 -right-10 w-40 h-40 bg-white/10 rounded-full blur-3xl"></div>
                                </div>

                                {/* Module Content */}
                                <div className="p-8">
                                    <h3 className="text-xl font-black text-slate-800 mb-8 leading-tight group-hover:text-indigo-600 transition-colors min-h-[3.5rem]">
                                        {module.moduleName}
                                    </h3>

                                    <div className="space-y-4">
                                        <button
                                            onClick={() => handleModuleClick(module)}
                                            className="w-full btn-primary flex items-center justify-center gap-2 group/btn"
                                        >
                                            Explore LOs
                                            <svg className="w-4 h-4 group-hover/btn:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M13 7l5 5-5 5M6 7l5 5-5 5" />
                                            </svg>
                                        </button>

                                        <div className="flex gap-4">
                                            <button
                                                onClick={(e) => handleEdit(module, e)}
                                                className="flex-1 btn-secondary text-xs font-black uppercase tracking-widest py-3 border-slate-100"
                                            >
                                                Edit
                                            </button>
                                            <button
                                                onClick={(e) => handleDelete(module, e)}
                                                className="flex-1 px-4 py-3 bg-red-50 text-red-500 rounded-xl text-xs font-black uppercase tracking-widest hover:bg-red-500 hover:text-white transition-all duration-300 transform active:scale-95"
                                            >
                                                Delete
                                            </button>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </main>

            {/* View Modal */}
            {modalType === 'view' && selectedModule && (
                <ModuleModal
                    module={selectedModule}
                    onClose={() => {
                        setModalType(null);
                        setSelectedModule(null);
                    }}
                />
            )}

            {/* Edit Modal */}
            {modalType === 'edit' && selectedModule && (
                <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-md z-[100] flex items-center justify-center p-6 animate-in zoom-in-95 duration-300">
                    <div className="bg-white rounded-[2.5rem] shadow-2xl w-full max-w-lg overflow-hidden border border-slate-100">
                        <div className="p-10">
                            <div className="flex justify-between items-center mb-10">
                                <div>
                                    <span className="text-[10px] font-black text-indigo-600 uppercase tracking-widest mb-1 block">Editor</span>
                                    <h3 className="heading-lg">Modify Module</h3>
                                </div>
                                <button
                                    onClick={() => setModalType(null)}
                                    className="p-3 bg-slate-50 text-slate-400 hover:bg-slate-100 hover:text-slate-600 rounded-2xl transition-colors"
                                >
                                    <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M6 18L18 6M6 6l12 12" />
                                    </svg>
                                </button>
                            </div>

                            <form onSubmit={(e) => {
                                e.preventDefault();
                                const formData = new FormData(e.target);
                                handleUpdate({
                                    moduleId: selectedModule.moduleId,
                                    moduleName: formData.get('moduleName')
                                });
                            }} className="space-y-8">
                                <div className="space-y-3">
                                    <label className="text-xs font-black text-slate-500 uppercase tracking-widest ml-1">Unique Module Identifier</label>
                                    <input
                                        type="text"
                                        value={selectedModule.moduleId}
                                        disabled
                                        className="input-field bg-slate-50 opacity-60 cursor-not-allowed font-black text-slate-400"
                                    />
                                </div>

                                <div className="space-y-3">
                                    <label className="text-xs font-black text-slate-500 uppercase tracking-widest ml-1">Module Designation</label>
                                    <input
                                        type="text"
                                        name="moduleName"
                                        defaultValue={selectedModule.moduleName}
                                        className="input-field py-4 text-lg font-bold"
                                        required
                                    />
                                </div>

                                <div className="pt-4 flex gap-4">
                                    <button
                                        type="button"
                                        onClick={() => setModalType(null)}
                                        className="btn-secondary flex-1 py-4 font-black uppercase text-xs tracking-widest text-slate-400"
                                    >
                                        Cancel
                                    </button>
                                    <button
                                        type="submit"
                                        className="btn-primary flex-1 py-4 font-black uppercase text-xs tracking-widest shadow-xl shadow-indigo-500/20"
                                    >
                                        Save Changes
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            )}

            {/* Delete Confirmation Modal */}
            {modalType === 'delete' && selectedModule && (
                <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-md z-[100] flex items-center justify-center p-6 animate-in zoom-in-95 duration-300">
                    <div className="bg-white rounded-[2.5rem] shadow-2xl w-full max-w-md overflow-hidden border border-red-50">
                        <div className="p-10 text-center">
                            <div className="w-20 h-20 bg-red-50 text-red-500 rounded-3xl flex items-center justify-center mx-auto mb-8">
                                <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                </svg>
                            </div>

                            <h2 className="heading-lg text-slate-900 mb-4">Confirm Deletion</h2>
                            <p className="text-slate-500 leading-relaxed mb-10 text-sm">
                                You are about to permanently remove <span className="text-red-600 font-bold">{selectedModule.moduleName}</span>. All associated Learning Outcomes and results will be lost.
                            </p>

                            <div className="flex gap-4">
                                <button
                                    onClick={() => setModalType(null)}
                                    className="btn-secondary flex-1 py-4 font-black uppercase text-xs tracking-widest text-slate-400"
                                >
                                    Cancel
                                </button>
                                <button
                                    onClick={confirmDelete}
                                    className="px-6 py-4 bg-red-500 text-white rounded-2xl font-black uppercase text-xs tracking-widest hover:bg-red-600 transition-colors shadow-xl shadow-red-500/20 flex-1 transform active:scale-95"
                                >
                                    Delete Module
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            <Footer />
        </div>
    );
}
