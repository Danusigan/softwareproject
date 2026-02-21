import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Header from '../components/header';
import Footer from '../components/footer';
import axios from 'axios';

export default function LODetailPage() {
    const { loId } = useParams();
    const [lo, setLo] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const navigate = useNavigate();

    useEffect(() => {
        fetchLODetails();
    }, [loId]);

    const fetchLODetails = async () => {
        try {
            setLoading(true);
            const token = localStorage.getItem('token');
            const res = await axios.get(`http://localhost:8080/api/lospos/${loId}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            setLo(res.data);
        } catch (err) {
            console.error('Error fetching LO details:', err);
            setError('Failed to load learning outcome details');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-[#f8fafc] flex flex-col relative overflow-hidden">
            <Header />

            {/* Background Decorations */}
            <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-indigo-500/10 rounded-full blur-[120px] pointer-events-none" />
            <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-emerald-500/10 rounded-full blur-[120px] pointer-events-none" />

            <main className="flex-1 flex flex-col items-center justify-center max-w-7xl mx-auto px-6 py-12 w-full relative z-10 animate-in fade-in duration-700">
                {loading ? (
                    <div className="flex flex-col items-center justify-center py-20 grayscale opacity-50">
                        <div className="relative">
                            <div className="w-16 h-16 border-4 border-indigo-100 rounded-full"></div>
                            <div className="w-16 h-16 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin absolute top-0 left-0"></div>
                        </div>
                        <p className="text-slate-500 font-bold mt-8 tracking-wide">Syncing data...</p>
                    </div>
                ) : error ? (
                    <div className="text-center animate-in zoom-in-95 duration-300">
                        <div className="glass-card p-12 rounded-[2.5rem] border-red-50 max-w-md mx-auto">
                            <div className="w-20 h-20 bg-red-50 text-red-500 rounded-3xl flex items-center justify-center mx-auto mb-8">
                                <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                                </svg>
                            </div>
                            <h2 className="heading-lg text-slate-900 mb-2">Sync Error</h2>
                            <p className="text-slate-500 mb-8">{error}</p>
                            <button
                                onClick={() => navigate(-1)}
                                className="w-full btn-secondary py-4 font-black uppercase text-xs tracking-widest text-slate-500"
                            >
                                Revert to Safety
                            </button>
                        </div>
                    </div>
                ) : (
                    <div className="w-full max-w-5xl text-center space-y-16">
                        {/* Hero Section */}
                        <div className="space-y-6 animate-in slide-in-from-bottom-8 duration-700">
                            <div className="inline-flex items-center gap-3 px-4 py-2 bg-white/50 backdrop-blur-md rounded-full border border-white/50 shadow-sm animate-bounce-subtle">
                                <div className="w-2 h-2 bg-indigo-500 rounded-full animate-pulse"></div>
                                <span className="text-[10px] font-black text-indigo-600 uppercase tracking-widest">
                                    {lo?.id?.includes('_') ? `Objective ${lo.id.split('_').pop()}` : lo?.id} Hub
                                </span>
                            </div>
                            <h1 className="heading-xl bg-clip-text text-transparent bg-gradient-to-r from-slate-900 via-indigo-900 to-slate-900 leading-[1.1] pb-2">
                                {lo?.loDescription || lo?.name}
                            </h1>
                            <p className="text-slate-500 text-lg font-medium max-w-2xl mx-auto leading-relaxed">
                                Manage performance metrics and execute comparisons for this learning outcome.
                            </p>
                        </div>

                        {/* Action Cards */}
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8 animate-in slide-in-from-bottom-12 duration-1000 delay-200">
                            {/* Add Results Card */}
                            <button
                                onClick={() => navigate(`/lo-detail/${loId}/add-results`)}
                                className="glass-card group p-10 rounded-[3rem] text-left hover:bg-white transition-all duration-500 hover:shadow-2xl hover:shadow-indigo-500/10 hover:-translate-y-2 border-slate-100 relative overflow-hidden"
                            >
                                <div className="absolute top-0 right-0 p-8 opacity-5 group-hover:opacity-10 transition-opacity">
                                    <svg className="w-32 h-32 text-indigo-900" fill="currentColor" viewBox="0 0 24 24">
                                        <path d="M12 4V20M20 12H4" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                                    </svg>
                                </div>
                                <div className="w-16 h-16 bg-emerald-50 text-emerald-600 rounded-2xl flex items-center justify-center mb-8 group-hover:bg-emerald-600 group-hover:text-white transition-all duration-500 shadow-sm">
                                    <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" />
                                    </svg>
                                </div>
                                <h3 className="text-2xl font-black text-slate-800 mb-3 group-hover:text-indigo-600 transition-colors">Data Ingestion</h3>
                                <p className="text-slate-500 font-medium leading-relaxed mb-8">
                                    Upload batch assessment data and process student attainment levels.
                                </p>
                                <div className="flex items-center gap-2 text-indigo-600 font-black text-xs uppercase tracking-widest">
                                    Initialize Upload
                                    <svg className="w-4 h-4 transform group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M14 5l7 7-7 7M3 12h18" />
                                    </svg>
                                </div>
                            </button>

                            {/* Comparison Card */}
                            <button
                                onClick={() => navigate(`/lo-detail/${loId}/comparisons`)}
                                className="glass-card group p-10 rounded-[3rem] text-left hover:bg-white transition-all duration-500 hover:shadow-2xl hover:shadow-indigo-500/10 hover:-translate-y-2 border-slate-100 relative overflow-hidden"
                            >
                                <div className="absolute top-0 right-0 p-8 opacity-5 group-hover:opacity-10 transition-opacity">
                                    <svg className="w-32 h-32 text-indigo-900" fill="currentColor" viewBox="0 0 24 24">
                                        <path d="M7 12L17 12M17 12L13 8M17 12L13 16" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                                    </svg>
                                </div>
                                <div className="w-16 h-16 bg-indigo-50 text-indigo-600 rounded-2xl flex items-center justify-center mb-8 group-hover:bg-indigo-600 group-hover:text-white transition-all duration-500 shadow-sm">
                                    <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                                    </svg>
                                </div>
                                <h3 className="text-2xl font-black text-slate-800 mb-3 group-hover:text-indigo-600 transition-colors">Trend Synthesis</h3>
                                <p className="text-slate-500 font-medium leading-relaxed mb-8">
                                    Project comparative analytics and visualize cross-batch performance trends.
                                </p>
                                <div className="flex items-center gap-2 text-indigo-600 font-black text-xs uppercase tracking-widest">
                                    Launch Analysis
                                    <svg className="w-4 h-4 transform group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M14 5l7 7-7 7M3 12h18" />
                                    </svg>
                                </div>
                            </button>

                            {/* Lecturer Dashboard Card */}
                            <button
                                onClick={() => navigate('/lecturer-dashboard')}
                                className="glass-card group p-10 rounded-[3rem] text-left hover:bg-white transition-all duration-500 hover:shadow-2xl hover:shadow-indigo-500/10 hover:-translate-y-2 border-slate-100 relative overflow-hidden"
                            >
                                <div className="absolute top-0 right-0 p-8 opacity-5 group-hover:opacity-10 transition-opacity">
                                    <svg className="w-32 h-32 text-indigo-900" fill="currentColor" viewBox="0 0 24 24">
                                        <path d="M3 12L21 12M21 12L15 6M21 12L15 18" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                                    </svg>
                                </div>
                                <div className="w-16 h-16 bg-slate-50 text-slate-600 rounded-2xl flex items-center justify-center mb-8 group-hover:bg-slate-900 group-hover:text-white transition-all duration-500 shadow-sm">
                                    <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
                                    </svg>
                                </div>
                                <h3 className="text-2xl font-black text-slate-800 mb-3 group-hover:text-indigo-600 transition-colors">Instructor Hub</h3>
                                <p className="text-slate-500 font-medium leading-relaxed mb-8">
                                    Return to your primary workspace and manage all course assignments.
                                </p>
                                <div className="flex items-center gap-2 text-indigo-600 font-black text-xs uppercase tracking-widest">
                                    Workspace Home
                                    <svg className="w-4 h-4 transform group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M14 5l7 7-7 7M3 12h18" />
                                    </svg>
                                </div>
                            </button>
                        </div>
                    </div>
                )}
            </main>

            <Footer />
        </div>
    );
}
