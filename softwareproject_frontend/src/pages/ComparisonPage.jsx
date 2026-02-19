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
        <div className="min-h-screen bg-gray-50 flex flex-col font-sans">
            <Header />

            <main className="flex-1 max-w-7xl mx-auto px-4 py-12 w-full">
                <div className="flex justify-between items-center mb-12">
                    <button
                        onClick={() => navigate(-1)}
                        className="flex items-center text-blue-600 hover:text-blue-800 font-semibold transition-colors"
                    >
                        <svg className="w-6 h-6 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
                        </svg>
                        Back to Details
                    </button>
                    <h1 className="text-3xl font-bold text-gray-800">Performance Comparison</h1>
                    <div className="w-24"></div> {/* Spacer */}
                </div>

                {loading ? (
                    <div className="flex flex-col items-center justify-center py-20">
                        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mb-4"></div>
                        <p className="text-gray-500 text-lg">Generating comparisons...</p>
                    </div>
                ) : error ? (
                    <div className="bg-red-50 text-red-700 p-8 rounded-2xl border border-red-200 text-center">
                        <p className="text-xl">{error}</p>
                    </div>
                ) : trendData.length === 0 ? (
                    <div className="bg-white p-12 rounded-3xl shadow-sm border border-gray-100 text-center">
                        <h2 className="text-2xl font-bold mb-4">No data available yet</h2>
                        <p className="text-gray-500 max-w-md mx-auto">
                            Please upload results for at least two different batches to see performance trends for {loName}.
                        </p>
                    </div>
                ) : (
                    <div className="space-y-12">
                        {/* Summary Header */}
                        <div className="bg-white p-8 rounded-3xl shadow-sm border border-gray-100">
                            <h2 className="text-2xl font-bold text-gray-800 mb-2">{loName}</h2>
                            <p className="text-gray-500">Showing attainment progress across batches</p>
                        </div>

                        {/* Chart Area */}
                        <div className="bg-white p-8 rounded-3xl shadow-sm border border-gray-100">
                            <div className="h-80 w-full relative flex items-end justify-around px-4 pt-10">
                                {/* Grid Lines */}
                                <div className="absolute inset-0 flex flex-col justify-between py-10 opacity-10">
                                    {[100, 75, 50, 25, 0].map(val => (
                                        <div key={val} className="border-t border-gray-800 w-full relative">
                                            <span className="absolute -left-10 -top-3 text-xs font-bold">{val}%</span>
                                        </div>
                                    ))}
                                </div>

                                {/* Bars */}
                                {trendData.map((data, idx) => (
                                    <div key={data.year} className="flex flex-col items-center group relative w-full max-w-[100px]">
                                        {/* Status Tag */}
                                        <div className={`absolute -top-12 px-3 py-1 rounded-full text-xs font-bold transform -translate-y-2 opacity-0 group-hover:opacity-100 group-hover:translate-y-0 transition-all duration-300 shadow-sm
                                            ${data.status === 'IMPROVED' ? 'bg-green-100 text-green-700' :
                                                data.status === 'DECLINED' ? 'bg-red-100 text-red-700' :
                                                    'bg-blue-100 text-blue-700'}`}>
                                            {data.status} {data.delta && `(${data.delta.toFixed(1)}%)`}
                                        </div>

                                        {/* The Bar */}
                                        <div
                                            style={{ height: `${(data.average / 100) * 100}%` }}
                                            className={`w-full max-w-[60px] rounded-t-xl transition-all duration-700 shadow-md transform origin-bottom hover:scale-105
                                                ${data.status === 'IMPROVED' ? 'bg-[#4ade80]' :
                                                    data.status === 'DECLINED' ? 'bg-[#f87171]' :
                                                        'bg-[#60a5fa]'}`}
                                        >
                                            <div className="w-full text-center mt-2 text-white font-bold text-sm">
                                                {data.average.toFixed(1)}%
                                            </div>
                                        </div>

                                        <div className="mt-4 font-bold text-gray-700">{data.year}</div>
                                    </div>
                                ))}
                            </div>
                        </div>

                        {/* Detailed Cards */}
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
                            {trendData.map((data) => (
                                <div key={data.year} className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 hover:shadow-md transition-shadow">
                                    <div className="flex justify-between items-center mb-4">
                                        <span className="bg-gray-100 px-3 py-1 rounded-lg text-sm font-bold text-gray-600">{data.year}</span>
                                        <span className={`px-3 py-1 rounded-full text-xs font-bold
                                            ${data.status === 'IMPROVED' ? 'bg-green-100 text-green-700' :
                                                data.status === 'DECLINED' ? 'bg-red-100 text-red-700' :
                                                    'bg-blue-100 text-blue-700'}`}>
                                            {data.status}
                                        </span>
                                    </div>
                                    <div className="text-4xl font-bold text-gray-800 mb-2">{data.average.toFixed(1)}%</div>
                                    <p className="text-gray-500 text-sm">Average Score Attainment</p>
                                    {data.delta !== undefined && (
                                        <div className={`mt-4 flex items-center text-sm font-bold
                                            ${data.delta >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                                            <svg className={`w-4 h-4 mr-1 ${data.delta < 0 ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M5 15l7-7 7 7" />
                                            </svg>
                                            {Math.abs(data.delta).toFixed(1)}% from previous batch
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
