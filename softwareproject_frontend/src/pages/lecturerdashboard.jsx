import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../components/header';
import Footer from '../components/footer';

export default function LecturerDashboard() {
    const navigate = useNavigate();
    const userType = localStorage.getItem('userType');

    return (
        <div className="min-h-screen bg-gray-50">
            <Header />

            <div className="max-w-7xl mx-auto px-4 py-8">
                <div className="flex items-center justify-between mb-8">
                    <h1 className="text-3xl font-bold text-gray-800">Lecturer Dashboard</h1>
                    <button
                        onClick={() => navigate('/modules')}
                        className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                    >
                        View Modules
                    </button>
                </div>

                {/* Welcome Card */}
                <div className="bg-white rounded-xl shadow-lg p-8 mb-8">
                    <h2 className="text-2xl font-bold text-gray-800 mb-4">
                        Welcome, {localStorage.getItem('username')}!
                    </h2>
                    <p className="text-gray-600 mb-4">
                        As a lecturer, you can manage learning outcomes (LOs) for your assigned modules.
                    </p>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-6">
                        <div className="bg-blue-50 p-4 rounded-lg">
                            <div className="flex items-center gap-3">
                                <div className="w-12 h-12 bg-blue-500 rounded-full flex items-center justify-center">
                                    <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                                    </svg>
                                </div>
                                <div>
                                    <h3 className="font-bold text-gray-800">View Modules</h3>
                                    <p className="text-sm text-gray-600">Browse all modules</p>
                                </div>
                            </div>
                        </div>

                        <div className="bg-green-50 p-4 rounded-lg">
                            <div className="flex items-center gap-3">
                                <div className="w-12 h-12 bg-green-500 rounded-full flex items-center justify-center">
                                    <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" />
                                    </svg>
                                </div>
                                <div>
                                    <h3 className="font-bold text-gray-800">Add LOs</h3>
                                    <p className="text-sm text-gray-600">Create learning outcomes</p>
                                </div>
                            </div>
                        </div>

                        <div className="bg-purple-50 p-4 rounded-lg">
                            <div className="flex items-center gap-3">
                                <div className="w-12 h-12 bg-purple-500 rounded-full flex items-center justify-center">
                                    <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                                    </svg>
                                </div>
                                <div>
                                    <h3 className="font-bold text-gray-800">Edit LOs</h3>
                                    <p className="text-sm text-gray-600">Update existing outcomes</p>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Quick Actions */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div
                        onClick={() => navigate('/modules')}
                        className="bg-white rounded-xl shadow-lg p-8 cursor-pointer transform transition-all duration-300 hover:scale-105 hover:shadow-2xl border-2 border-transparent hover:border-blue-500"
                    >
                        <div className="flex items-center justify-center mb-4">
                            <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center">
                                <svg className="w-8 h-8 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                                </svg>
                            </div>
                        </div>
                        <h2 className="text-2xl font-bold text-center text-gray-800 mb-2">Manage Modules</h2>
                        <p className="text-center text-gray-600">View and manage learning outcomes for your modules</p>
                    </div>

                    <div className="bg-white rounded-xl shadow-lg p-8">
                        <div className="flex items-center justify-center mb-4">
                            <div className="w-16 h-16 bg-orange-100 rounded-full flex items-center justify-center">
                                <svg className="w-8 h-8 text-orange-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                                </svg>
                            </div>
                        </div>
                        <h2 className="text-2xl font-bold text-center text-gray-800 mb-2">Statistics</h2>
                        <p className="text-center text-gray-600">View your module and LO statistics</p>
                        <div className="mt-4 text-center text-gray-500 text-sm">Coming soon...</div>
                    </div>
                </div>
            </div>

            <Footer />
        </div>
    );
}
