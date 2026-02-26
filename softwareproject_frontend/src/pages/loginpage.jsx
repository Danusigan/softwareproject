"use client"
import Header from '../components/header'
import Footer from '../components/footer'
import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import axios from 'axios';

export default function LoginPage() {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [userRole, setUserRole] = useState('');
    const [rememberMe, setRememberMe] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');
    const navigate = useNavigate();

    const handleLogin = async (e) => {
        e.preventDefault();
        setIsLoading(true);
        setError('');

        // ✅ FIX 1: Validate role is selected
        if (!userRole) {
            setError('Please select a user role.');
            setIsLoading(false);
            return;
        }

        try {
            const res = await axios.post('http://localhost:8080/api/auth/login', {
                userID: username,
                password,
                userType: userRole  // ✅ FIX 2: Send userRole to backend
            });

            console.log('=== LOGIN RESPONSE ===');
            console.log('Full Response:', res.data);

            if (res.data && res.data.status === "SUCCESS") {
                const loggedInUsername = res.data.userId;
                const userType = res.data.userType || userRole; // ✅ FIX 3: Fallback to selected role
                const token = res.data.token;

                // Store user data and token
                localStorage.setItem('username', loggedInUsername);
                localStorage.setItem('userType', userType);
                localStorage.setItem('token', token);
                localStorage.setItem('isLoggedIn', 'true'); // ✅ FIX 4: Store login state

                if (rememberMe) {
                    localStorage.setItem('rememberMe', 'true');
                } else {
                    localStorage.removeItem('rememberMe');
                }

                // ✅ FIX 5: Normalize userType for comparison
                const normalizedType = userType?.toLowerCase?.().trim() || '';

                console.log('Navigating for userType:', normalizedType);

                if (normalizedType === 'superadmin' || normalizedType === 'super admin' || normalizedType === 'super-admin') {
                    navigate('/super-admin-dashboard', { replace: true });
                } else if (normalizedType === 'admin') {
                    navigate('/admin-dashboard', { replace: true });
                } else if (normalizedType === 'lecture' || normalizedType === 'lecturer') {
                    navigate('/lecturer-dashboard', { replace: true });
                } else {
                    console.warn('Unknown userType:', userType);
                    navigate('/', { replace: true });
                }

            } else {
                setError(res.data.message || 'Login failed. Invalid response from server.');
            }
        } catch (err) {
            console.error('=== LOGIN ERROR ===');
            console.error('Error:', err.response?.data || err.message);

            const backendMessage = err.response?.data?.message;
            if (backendMessage) {
                setError(backendMessage);
            } else if (err.response?.status === 401) {
                setError('Login failed. Incorrect username, password, or user role.');
            } else if (err.response?.status === 403) {
                setError('Access denied. Your role does not match the selected profile.');
            } else {
                setError('Login failed. Please check your credentials.');
            }
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-[#f8fafc] flex flex-col relative overflow-hidden">
            {/* Background Decorative Elements */}
            <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-indigo-500/5 rounded-full blur-[120px] pointer-events-none" />
            <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-emerald-500/5 rounded-full blur-[120px] pointer-events-none" />

            <Header />

            <main className="flex-1 flex items-center justify-center p-6 relative z-10 animate-in fade-in zoom-in-95 duration-700">
                <div className="w-full max-w-lg glass-card rounded-[2.5rem] overflow-hidden shadow-2xl shadow-indigo-500/10">

                    {/* Login Form Section */}
                    <div className="p-10 md:p-16 flex flex-col justify-center">
                        <div className="mb-10 text-center">
                            <h2 className="heading-xl mb-4 text-slate-900">Welcome Back</h2>
                            <p className="text-slate-500 font-medium">Please enter your credentials to access the analytics terminal.</p>
                        </div>

                        {/* Error Message */}
                        {error && (
                            <div className="bg-red-50/50 border border-red-100 text-red-600 px-6 py-4 rounded-2xl mb-8 flex items-center gap-3 animate-shake">
                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                                </svg>
                                <span className="text-sm font-bold">{error}</span>
                            </div>
                        )}

                        <form onSubmit={handleLogin} className="space-y-6">
                            {/* User Role */}
                            <div className="space-y-2">
                                <label className="text-xs font-black text-slate-400 uppercase tracking-widest ml-1">Identity Profile</label>
                                <select
                                    className="input-field appearance-none bg-white/50"
                                    value={userRole}
                                    onChange={(e) => setUserRole(e.target.value)}
                                    required
                                >
                                    <option value="">Select User Role</option>
                                    <option value="superadmin">Superadmin</option>
                                    <option value="admin">Admin</option>
                                    <option value="lecture">Lecturer</option>
                                </select>
                            </div>

                            {/* Username */}
                            <div className="space-y-2">
                                <label className="text-xs font-black text-slate-400 uppercase tracking-widest ml-1">Username</label>
                                <input
                                    type="text"
                                    className="input-field"
                                    placeholder="yourId@domain.com"
                                    value={username}
                                    onChange={(e) => setUsername(e.target.value)}
                                    required
                                />
                            </div>

                            {/* Password */}
                            <div className="space-y-2">
                                <label className="text-xs font-black text-slate-400 uppercase tracking-widest ml-1">Access Token</label>
                                <input
                                    type="password"
                                    className="input-field"
                                    placeholder="••••••••"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    required
                                />
                            </div>

                            <div className="flex justify-between items-center py-2">
                                <label className="flex items-center gap-3 cursor-pointer group">
                                    <div className="relative flex items-center">
                                        <input
                                            type="checkbox"
                                            className="peer sr-only"
                                            checked={rememberMe}
                                            onChange={(e) => setRememberMe(e.target.checked)}
                                        />
                                        <div className="w-5 h-5 border-2 border-slate-200 rounded-lg peer-checked:bg-indigo-600 peer-checked:border-indigo-600 transition-all duration-300 shadow-sm" />
                                        <svg className="absolute w-3 h-3 text-white left-1 opacity-0 peer-checked:opacity-100 transition-opacity" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="4" d="M5 13l4 4L19 7" />
                                        </svg>
                                    </div>
                                    <span className="text-sm font-bold text-slate-500 group-hover:text-slate-700 transition-colors">Keep me signed in</span>
                                </label>
                                <Link to="/forgottenpassword" title="Forget Password?" className="text-sm font-black text-indigo-600 hover:text-indigo-700 transition-colors uppercase tracking-widest">Forgot Access?</Link>
                            </div>

                            <button
                                type="submit"
                                className={`w-full py-4 px-8 rounded-2xl text-white font-bold text-sm uppercase tracking-[0.2em] shadow-xl transition-all duration-300 transform active:scale-95 flex items-center justify-center gap-3
                                    ${isLoading
                                        ? 'bg-slate-300 cursor-not-allowed shadow-none'
                                        : 'bg-indigo-600 hover:bg-indigo-700 hover:shadow-indigo-200'}`}
                                disabled={isLoading}
                            >
                                {isLoading ? (
                                    <>
                                        <svg className="animate-spin h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                                        </svg>
                                        Authorizing...
                                    </>
                                ) : 'Initialize Access'}
                            </button>
                        </form>
                    </div>
                </div>
            </main>

            <Footer />
        </div>
    );
}