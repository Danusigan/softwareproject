import Header from '../components/header'
import Footer from '../components/footer'
import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import axios from 'axios';
import authService from '../services/authService';

export default function LoginPage() {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [rememberMe, setRememberMe] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');
    const navigate = useNavigate();

    const handleLogin = async (e) => {
        e.preventDefault();
        setIsLoading(true);
        setError('');

        try {
            const res = await axios.post('http://localhost:8080/api/auth/login', {
                userID: username,
                password
            });

            console.log('=== LOGIN RESPONSE ===');
            console.log('Full Response:', res.data);

            if (res.data?.status === 'SUCCESS') {
                const loggedInUsername = res.data.userId;
                const userType = res.data.userType;
                const token = res.data.token;

                // ✅ Use authService to store login with 2-hour expiration
                authService.storeLogin(token, loggedInUsername, userType, rememberMe);

                // ✅ Normalize userType for comparison
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
                setError('Login failed. Incorrect username or password.');
            } else if (err.response?.status === 403) {
                setError('Access denied.');
            } else {
                setError('Login failed. Please check your credentials.');
            }
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-slate-50 flex flex-col relative overflow-hidden">
            {/* Background Decorative Elements */}
            <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-indigo-500/10 rounded-full blur-[120px] pointer-events-none" />
            <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-sky-500/10 rounded-full blur-[120px] pointer-events-none" />

            <Header />

            <main className="flex-1 flex items-center justify-center p-6 relative z-10 animate-in fade-in zoom-in-95 duration-700">
                <div className="w-full max-w-md bg-white/95 backdrop-blur rounded-3xl border border-slate-200/80 overflow-hidden shadow-xl shadow-slate-900/5">

                    {/* Login Form Section */}
                    <div className="p-8 md:p-10 flex flex-col justify-center">
                        <div className="mb-8 text-center">
                            <h2 className="text-4xl font-semibold tracking-tight text-slate-900 mb-3">Welcome Back</h2>
                            <p className="text-slate-600 text-base">Sign in to continue to the LO-PO analytics dashboard.</p>
                        </div>

                        {/* Error Message */}
                        {error && (
                            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-xl mb-6 flex items-center gap-3 animate-shake">
                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                                </svg>
                                <span className="text-sm font-medium">{error}</span>
                            </div>
                        )}

                        <form onSubmit={handleLogin} className="space-y-5">
                            {/* Username */}
                            <div className="space-y-2">
                                <label htmlFor="username" className="text-sm font-medium text-slate-700 ml-1">Username</label>
                                <input
                                    id="username"
                                    type="text"
                                    className="input-field"
                                    placeholder="Enter your username"
                                    value={username}
                                    onChange={(e) => setUsername(e.target.value)}
                                    required
                                />
                            </div>

                            {/* Password */}
                            <div className="space-y-2">
                                <label htmlFor="password" className="text-sm font-medium text-slate-700 ml-1">Password</label>
                                <input
                                    id="password"
                                    type="password"
                                    className="input-field"
                                    placeholder="Enter your password"
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
                                    <span className="text-sm font-medium text-slate-600 group-hover:text-slate-800 transition-colors">Remember me</span>
                                </label>
                                <Link to="/forgottenpassword" title="Forgot Password?" className="text-sm font-medium text-indigo-600 hover:text-indigo-700 transition-colors">Forgot password?</Link>
                            </div>

                            <button
                                type="submit"
                                className={`w-full py-3.5 px-8 rounded-xl text-white font-medium text-base shadow-lg transition-all duration-300 transform active:scale-95 flex items-center justify-center gap-3
                                    ${isLoading
                                        ? 'bg-slate-300 cursor-not-allowed shadow-none'
                                        : 'bg-indigo-600 hover:bg-indigo-700 hover:shadow-indigo-200/80'}`}
                                disabled={isLoading}
                            >
                                {isLoading ? (
                                    <>
                                        <svg className="animate-spin h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                                        </svg>
                                        Signing in...
                                    </>
                                ) : 'Login'}
                            </button>
                        </form>
                    </div>
                </div>
            </main>

            <Footer />
        </div>
    );
}
