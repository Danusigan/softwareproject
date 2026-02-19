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
        <div className="min-h-screen bg-gray-50 flex flex-col">
            <Header />

            <main className="flex-1 max-w-7xl mx-auto px-4 py-12 w-full">
                {loading ? (
                    <div className="flex flex-col items-center justify-center py-20">
                        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mb-4"></div>
                        <p className="text-gray-500 text-lg">Loading details...</p>
                    </div>
                ) : error ? (
                    <div className="text-center py-20">
                        <div className="bg-red-50 text-red-700 p-6 rounded-xl border border-red-200 inline-block max-w-md">
                            <svg className="w-12 h-12 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                            </svg>
                            <h2 className="text-xl font-bold mb-2">Error</h2>
                            <p>{error}</p>
                            <button
                                onClick={() => navigate(-1)}
                                className="mt-6 px-6 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors"
                            >
                                Go Back
                            </button>
                        </div>
                    </div>
                ) : (
                    <div className="text-center">
                        <h1 className="text-3xl md:text-4xl font-bold text-gray-800 mb-12 max-w-4xl mx-auto leading-tight">
                            You are now in {lo?.id?.includes('_') ? `LO ${lo.id.split('_').pop()}` : lo?.id} : {lo?.loDescription || lo?.name} check your activities here
                        </h1>

                        <div className="flex flex-col items-center gap-6 mt-12">
                            <button
                                onClick={() => navigate(`/lo-detail/${loId}/add-results`)}
                                className="w-full max-w-sm py-4 bg-[#4ade80] text-black font-bold text-xl rounded-xl shadow-lg hover:bg-[#22c55e] transition-all transform hover:scale-105 active:scale-95">
                                + Add new results here
                            </button>
                            <button
                                onClick={() => navigate(`/lo-detail/${loId}/comparisons`)}
                                className="w-full max-w-sm py-4 bg-[#4ade80] text-black font-bold text-xl rounded-xl shadow-lg hover:bg-[#22c55e] transition-all transform hover:scale-105 active:scale-95">
                                See the comparisions
                            </button>
                        </div>
                    </div>
                )}
            </main>

            <Footer />
        </div>
    );
}
