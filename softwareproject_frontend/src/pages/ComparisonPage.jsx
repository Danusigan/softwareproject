import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Header from '../components/header';
import Footer from '../components/footer';
import axios from 'axios';

export default function ComparisonPage() {
    const { loId } = useParams();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(true);
    const [trendData, setTrendData] = useState([]);
    const [loName, setLoName] = useState('');
    const [error, setError] = useState(null);

    useEffect(() => {
        fetchData();
    }, [loId]);

    const fetchData = async () => {
        try {
            setLoading(true);
            const token = localStorage.getItem('token');

            // 1. Fetch LO details to get its name and module ID
            const loRes = await axios.get(`http://localhost:8080/api/lospos/${loId}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            const lo = loRes.data;
            setLoName(lo.loDescription || lo.name);
            const moduleId = lo.moduleId; // Exposed via @JsonProperty in Model

            // 2. Fetch LO Trend data for the module
            const trendRes = await axios.get(`http://localhost:8080/api/obe/analysis/trend/lo/${moduleId}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            // The response is Map<String, List<Map<String, Object>>> where key is "loId - loName"
            const loKey = Object.keys(trendRes.data).find(key => key.startsWith(lo.id + " -"));

            if (loKey && trendRes.data[loKey]) {
                setTrendData(trendRes.data[loKey]);
            } else {
                setTrendData([]);
            }

        } catch (err) {
            console.error('Error fetching trend data:', err);
            setError('Could not load comparison data');
        } finally {
            setLoading(false);
        }
    };

    const maxAvg = trendData.length > 0 ? Math.max(...trendData.map(d => d.average), 100) : 100;

    return (
        <div className="min-h-screen bg-[#f8fafc] flex flex-col relative overflow-hidden">
            <Header />

            {/* Background Decorations */}
            <div className="absolute top-0 right-0 w-[500px] h-[500px] bg-indigo-500/5 rounded-full blur-[120px] pointer-events-none" />
            <div className="absolute bottom-0 left-0 w-[500px] h-[500px] bg-emerald-500/5 rounded-full blur-[120px] pointer-events-none" />

            <main className="flex-1 max-w-7xl mx-auto px-6 py-12 w-full relative z-10 animate-in fade-in duration-700">
                <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-6 mb-12">
                    <div className="space-y-2">
                        <button
                            onClick={() => navigate(-1)}
                            className="group flex items-center text-slate-500 hover:text-indigo-600 font-bold transition-all duration-300"
                        >
                            <div className="p-2 bg-white rounded-xl shadow-sm border border-slate-100 mr-3 group-hover:bg-indigo-50 group-hover:border-indigo-100 transition-colors">
                                <svg className="w-5 h-5 group-hover:-translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
                                </svg>
                            </div>
                            Back to Details
                        </button>
                        <h1 className="heading-xl bg-clip-text text-transparent bg-gradient-to-r from-slate-900 to-slate-600">
                            Performance Analytics
                        </h1>
                    </div>
                </div>

                {loading ? (
                    <div className="flex flex-col items-center justify-center py-24 glass-card rounded-[2rem]">
                        <div className="relative">
                            <div className="w-16 h-16 border-4 border-indigo-100 rounded-full"></div>
                            <div className="w-16 h-16 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin absolute top-0 left-0"></div>
                        </div>
                        <p className="text-slate-500 font-bold mt-8 tracking-wide">Compiling comparative data...</p>
                    </div>
                ) : error ? (
                    <div className="glass-card bg-red-50/50 border-red-100 p-12 rounded-[2rem] text-center">
                        <div className="w-16 h-16 bg-red-100 text-red-600 rounded-2xl flex items-center justify-center mx-auto mb-6">
                            <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                            </svg>
                        </div>
                        <h2 className="text-xl font-bold text-red-800 mb-2">Analysis Failed</h2>
                        <p className="text-red-600/70">{error}</p>
                    </div>
                ) : trendData.length === 0 ? (
                    <div className="glass-card p-16 rounded-[2rem] text-center max-w-2xl mx-auto border-dashed border-2">
                        <div className="w-20 h-20 bg-slate-50 text-slate-300 rounded-3xl flex items-center justify-center mx-auto mb-8">
                            <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                            </svg>
                        </div>
                        <h2 className="heading-lg mb-4">No data available yet</h2>
                        <p className="text-slate-500 leading-relaxed">
                            To generate performance insights, please upload activity results for at least two different batches for <span className="text-indigo-600 font-bold">{loName}</span>.
                        </p>
                    </div>
                ) : (
                    <div className="space-y-10">
                        {/* Summary Header */}
                        <div className="glass-card p-8 rounded-[2rem] border-l-8 border-l-indigo-500 flex flex-col md:flex-row md:items-center justify-between gap-6">
                            <div>
                                <span className="text-xs font-bold text-indigo-600 uppercase tracking-[0.2em] mb-2 block">Learning Outcome Focus</span>
                                <h2 className="heading-lg">{loName}</h2>
                                <p className="text-slate-500 mt-1">Cross-batch attainment progress visualization</p>
                            </div>
                            <div className="flex gap-4">
                                <div className="px-6 py-3 bg-white/50 rounded-2xl border border-white/50 shadow-sm">
                                    <div className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">Total Batches</div>
                                    <div className="text-2xl font-black text-slate-800">{trendData.length}</div>
                                </div>
                                <div className="px-6 py-3 bg-white/50 rounded-2xl border border-emerald-100 shadow-sm">
                                    <div className="text-[10px] font-bold text-emerald-500 uppercase tracking-wider mb-1">Latest Avg.</div>
                                    <div className="text-2xl font-black text-emerald-600">{trendData[trendData.length - 1].average.toFixed(1)}%</div>
                                </div>
                            </div>
                        </div>

                        {/* Chart Area */}
                        <div className="glass-card p-10 rounded-[2.5rem] relative overflow-hidden">
                            <div className="absolute top-0 right-0 p-8">
                                <div className="flex items-center gap-6">
                                    <div className="flex items-center gap-2">
                                        <div className="w-3 h-3 bg-emerald-400 rounded-full shadow-[0_0_8px_rgba(52,211,153,0.5)]"></div>
                                        <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Improved</span>
                                    </div>
                                    <div className="flex items-center gap-2">
                                        <div className="w-3 h-3 bg-red-400 rounded-full shadow-[0_0_8px_rgba(248,113,113,0.5)]"></div>
                                        <span className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Declined</span>
                                    </div>
                                </div>
                            </div>

                            <div className="h-[400px] w-full relative flex items-end justify-around px-8 pt-20 pb-10">
                                {/* Grid Lines */}
                                <div className="absolute inset-0 flex flex-col justify-between pt-20 pb-20 px-8 pointer-events-none">
                                    {[100, 75, 50, 25, 0].map(val => (
                                        <div key={val} className="border-t border-slate-100 w-full relative">
                                            <span className="absolute -left-10 -top-2 text-[10px] font-black text-slate-300 tracking-tighter">{val}%</span>
                                        </div>
                                    ))}
                                </div>

                                {/* Bars */}
                                {trendData.map((data, idx) => (
                                    <div key={data.year} className="flex flex-col items-center group relative w-full h-full justify-end max-w-[140px]">
                                        {/* Status Tooltip */}
                                        <div className={`absolute bottom-full mb-6 px-4 py-2 rounded-xl text-xs font-bold whitespace-nowrap opacity-0 group-hover:opacity-100 transition-all duration-300 shadow-xl border z-20 
                                            ${data.status === 'IMPROVED' ? 'bg-white text-emerald-600 border-emerald-100' :
                                                data.status === 'DECLINED' ? 'bg-white text-red-600 border-red-100' :
                                                    'bg-white text-indigo-600 border-indigo-100'}`}>
                                            <div className="flex items-center gap-2">
                                                <span className="uppercase tracking-widest">{data.status}</span>
                                                {data.delta && <span className="bg-slate-50 px-2 py-0.5 rounded-lg border border-slate-100">{data.delta > 0 ? '+' : ''}{data.delta.toFixed(1)}%</span>}
                                            </div>
                                            {/* Arrow down */}
                                            <div className="absolute top-full left-1/2 -translate-x-1/2 border-8 border-transparent border-t-white drop-shadow-sm"></div>
                                        </div>

                                        {/* The Bar */}
                                        <div
                                            style={{ height: `${data.average}%` }}
                                            className={`w-[60%] rounded-2xl transition-all duration-1000 shadow-lg relative group-hover:scale-x-110
                                                ${data.status === 'IMPROVED' ? 'bg-gradient-to-t from-emerald-500 to-emerald-400 shadow-emerald-200/50' :
                                                    data.status === 'DECLINED' ? 'bg-gradient-to-t from-red-500 to-red-400 shadow-red-200/50' :
                                                        'bg-gradient-to-t from-indigo-500 to-indigo-400 shadow-indigo-200/50'}`}
                                        >
                                            {/* Glass Overlay on bar */}
                                            <div className="absolute inset-x-0 top-0 h-1/2 bg-white/20 rounded-t-2xl pointer-events-none"></div>

                                            {/* Value on hover */}
                                            <div className="absolute -top-10 left-1/2 -translate-x-1/2 font-black text-slate-800 text-sm pointer-events-none transition-transform group-hover:scale-125">
                                                {data.average.toFixed(1)}%
                                            </div>
                                        </div>

                                        <div className="mt-8 text-center space-y-1">
                                            <div className="text-[10px] font-black text-slate-400 uppercase tracking-widest">Year {data.year.split(' ')[0]}</div>
                                            <div className="text-xs font-bold text-slate-700">{data.year}</div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>

                        {/* Detailed Cards */}
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
                            {trendData.map((data) => (
                                <div key={data.year} className="glass-card hover:bg-white transition-all duration-500 group rounded-[2rem] p-8 border-slate-100 hover:shadow-2xl hover:shadow-indigo-500/5 hover:-translate-y-2">
                                    <div className="flex justify-between items-center mb-8">
                                        <div className="px-4 py-1.5 bg-slate-50 text-slate-600 rounded-xl text-[10px] font-black uppercase tracking-widest border border-slate-100 group-hover:bg-indigo-50 group-hover:text-indigo-600 group-hover:border-indigo-100 transition-colors">
                                            {data.year}
                                        </div>
                                        <div className={`w-10 h-10 rounded-xl flex items-center justify-center
                                            ${data.status === 'IMPROVED' ? 'bg-emerald-50 text-emerald-600' :
                                                data.status === 'DECLINED' ? 'bg-red-50 text-red-600' :
                                                    'bg-blue-50 text-blue-600'}`}>
                                            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d={data.status === 'IMPROVED' ? "M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" : data.status === 'DECLINED' ? "M13 17h8m0 0v-8m0 8l-8-8-4 4-6-6" : "M5 12h14"} />
                                            </svg>
                                        </div>
                                    </div>

                                    <div className="space-y-1">
                                        <div className="text-5xl font-black text-slate-900 tracking-tighter">
                                            {data.average.toFixed(1)}<span className="text-2xl text-slate-300 ml-1">%</span>
                                        </div>
                                        <p className="text-slate-400 text-[10px] font-bold uppercase tracking-widest">Average Attainment Score</p>
                                    </div>

                                    {data.delta !== undefined && (
                                        <div className="mt-8 pt-6 border-t border-slate-50 flex items-center justify-between">
                                            <span className="text-slate-500 text-xs font-medium">Progress Trend</span>
                                            <div className={`flex items-center font-black text-sm
                                                ${data.delta >= 0 ? 'text-emerald-500' : 'text-red-500'}`}>
                                                <svg className={`w-4 h-4 mr-1 ${data.delta < 0 ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M5 15l7-7 7 7" />
                                                </svg>
                                                {Math.abs(data.delta).toFixed(1)}%
                                            </div>
                                        </div>
                                    )}
                                </div>
                            ))}
                        </div>
                    </div>
                )}
            </main>

            <Footer />
        </div>
    );
}
