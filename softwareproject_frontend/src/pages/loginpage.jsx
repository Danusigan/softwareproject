import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import axios from 'axios';
import authService from '../services/authService';

/* ------------------------------------------------------------------ */
/*  TOP INFO BAR  (phone / email on the left, "Log in" on the right)   */
/* ------------------------------------------------------------------ */
function TopBar() {
    return (
        <div className="bg-[#1e3a8a] text-white text-sm">
            <div className="max-w-7xl mx-auto px-6 sm:px-10 h-11 flex items-center justify-between">
                <div className="flex items-center gap-6 flex-wrap">
                    <span className="flex items-center gap-2">
                        <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24">
                            <path d="M6.6 10.8a15.1 15.1 0 006.6 6.6l2.2-2.2a1 1 0 011-.25c1.1.37 2.3.57 3.6.57a1 1 0 011 1V20a1 1 0 01-1 1C10.6 21 3 13.4 3 4a1 1 0 011-1h3.5a1 1 0 011 1c0 1.3.2 2.5.57 3.6a1 1 0 01-.25 1L6.6 10.8z" />
                        </svg>
                        Call us : +(94) 91 224 5765-7
                    </span>
                    <span className="hidden sm:flex items-center gap-2">
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" d="M3 8l9 6 9-6M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                        </svg>
                        E-mail : ar@eng.ruh.ac.lk
                    </span>
                </div>
                <Link to="/loginpage" className="hover:text-blue-200 transition-colors">Log in</Link>
            </div>
        </div>
    );
}

/* ------------------------------------------------------------------ */
/*  WHITE HEADER  (logo on the left, Home / Login on the right)        */
/* ------------------------------------------------------------------ */
function LoginHeader() {
    const navigate = useNavigate();
    return (
        <header className="bg-white border-b border-slate-200 shadow-sm">
            <div className="max-w-7xl mx-auto px-6 sm:px-10 h-24 flex items-center justify-between">
                <button
                    type="button"
                    onClick={() => navigate('/')}
                    className="flex items-center gap-4 group"
                    aria-label="Go to home"
                >
                    <div className="w-14 h-14 bg-[#1e40af] rounded-xl flex items-center justify-center shadow-md group-hover:scale-105 transition-transform">
                        <span className="text-white font-black text-xl tracking-tighter">LO</span>
                    </div>
                    <div className="border-l border-slate-200 pl-4 text-left">
                        <h1 className="text-xl font-black tracking-tight leading-none text-[#1e3a8a] mb-1">LO-PO ANALYTICS</h1>
                        <p className="text-[10px] font-bold uppercase tracking-[0.15em] text-slate-500">Faculty of Engineering</p>
                        <p className="text-[10px] font-medium uppercase tracking-[0.1em] text-slate-400">University of Ruhuna</p>
                    </div>
                </button>

                <nav className="flex items-center gap-8">
                    <Link to="/" className="text-base font-medium text-slate-700 hover:text-[#1e40af] transition-colors">Home</Link>
                    <Link
                        to="/loginpage"
                        className="bg-[#1e40af] text-white px-6 py-2.5 rounded-xl text-xs font-black uppercase tracking-widest hover:bg-[#1e3a8a] transition-all shadow-md"
                    >
                        Login
                    </Link>
                </nav>
            </div>
        </header>
    );
}

/* ------------------------------------------------------------------ */
/*  DARK FOOTER  (logo + description | Contact Us | copyright bar)     */
/* ------------------------------------------------------------------ */
function LoginFooter() {
    return (
        <footer className="bg-[#2b2b2b] text-white">
            <div className="max-w-7xl mx-auto px-6 sm:px-10 py-14 grid grid-cols-1 md:grid-cols-2 gap-12">
                <div>
                    <div className="flex items-center gap-4 mb-6">
                        <div className="w-14 h-14 bg-[#1e40af] rounded-xl flex items-center justify-center">
                            <span className="text-white font-black text-xl tracking-tighter">LO</span>
                        </div>
                        <h3 className="text-2xl font-bold tracking-tight">LO-PO Analytics</h3>
                    </div>
                    <p className="text-slate-300 leading-relaxed">
                        The Faculty of Engineering, University of Ruhuna uses this system to map Learning
                        Outcomes to Program Outcomes, compute PO attainment from assessment results and
                        generate accreditation reports, in line with the Washington Accord.
                    </p>
                </div>

                <div>
                    <h3 className="text-3xl font-bold text-blue-400 mb-5">Contact Us</h3>
                    <p className="text-slate-200 mb-4">Faculty of Engineering, Hapugala, Galle, Sri Lanka.</p>
                    <p className="flex items-center gap-3 text-slate-200 mb-3">
                        <svg className="w-5 h-5 text-blue-300" fill="currentColor" viewBox="0 0 24 24">
                            <path d="M6.6 10.8a15.1 15.1 0 006.6 6.6l2.2-2.2a1 1 0 011-.25c1.1.37 2.3.57 3.6.57a1 1 0 011 1V20a1 1 0 01-1 1C10.6 21 3 13.4 3 4a1 1 0 011-1h3.5a1 1 0 011 1c0 1.3.2 2.5.57 3.6a1 1 0 01-.25 1L6.6 10.8z" />
                        </svg>
                        Phone : +(94) 91 224 5765-7
                    </p>
                    <p className="flex items-center gap-3 text-slate-200">
                        <svg className="w-5 h-5 text-blue-300" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" d="M3 8l9 6 9-6M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                        </svg>
                        Email : <a href="mailto:ar@eng.ruh.ac.lk" className="underline hover:text-blue-300">ar@eng.ruh.ac.lk</a>
                    </p>
                </div>
            </div>
            <div className="bg-black/40 text-center text-slate-400 text-sm py-3">
                Copyright © Faculty of Engineering-2026. LO-PO Analytics.
            </div>
        </footer>
    );
}

/* ------------------------------------------------------------------ */
/*  LOGIN PAGE                                                         */
/* ------------------------------------------------------------------ */
export default function LoginPage() {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [rememberMe, setRememberMe] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');
    const navigate = useNavigate();

    // ---- login logic (unchanged) ----
    const handleLogin = async (e) => {
        e.preventDefault();
        setIsLoading(true);
        setError('');

        try {
            const res = await axios.post('/api/auth/login', {
                userID: username,
                password
            });

            if (res.data?.status === 'SUCCESS') {
                const loggedInUsername = res.data.userId;
                const userType = res.data.userType;
                const token = res.data.token;

                authService.storeLogin(token, loggedInUsername, userType, rememberMe);

                const normalizedType = userType?.toLowerCase?.().trim() || '';

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
        <div className="min-h-screen flex flex-col bg-white">
            <TopBar />
            <LoginHeader />

            {/* ---------- SPLIT SCREEN ---------- */}
            <main className="flex-1 grid grid-cols-1 lg:grid-cols-2">

                {/* LEFT: blue welcome panel (hidden on small screens) */}
                <section className="hidden lg:flex items-center justify-center bg-gradient-to-br from-[#1e40af] to-[#1e3a8a] p-12">
                    <div className="max-w-lg w-full bg-white/10 backdrop-blur rounded-2xl border border-white/20 p-10 text-center shadow-2xl">
                        <h2 className="text-4xl font-semibold text-white mb-4">Welcome to LO-PO Analytics</h2>
                        <p className="text-blue-100 leading-relaxed mb-8">
                            Track Learning Outcomes, measure Program Outcome attainment and
                            build accreditation reports, all in one place.
                        </p>
                        <p className="text-white text-lg mb-6">Built for the Faculty of Engineering</p>
                        <div className="grid grid-cols-3 gap-4 text-white">
                            <div>
                                <p className="text-2xl font-bold">12</p>
                                <p className="text-sm text-blue-200">Program Outcomes</p>
                            </div>
                            <div>
                                <p className="text-2xl font-bold">3</p>
                                <p className="text-sm text-blue-200">User Roles</p>
                            </div>
                            <div>
                                <p className="text-2xl font-bold">WA</p>
                                <p className="text-sm text-blue-200">Washington Accord</p>
                            </div>
                        </div>
                    </div>
                </section>

                {/* RIGHT: login form */}
                <section className="flex items-center justify-center px-6 sm:px-12 py-14 bg-white">
                    <div className="w-full max-w-md">
                        <h2 className="text-4xl font-semibold text-[#1e40af] mb-2">Welcome back</h2>
                        <p className="text-slate-600 mb-8">Log in to the LO-PO Analytics dashboard.</p>

                        {error && (
                            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-xl mb-6 flex items-center gap-3">
                                <svg className="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                                </svg>
                                <span className="text-sm font-medium">{error}</span>
                            </div>
                        )}

                        <form onSubmit={handleLogin} className="space-y-5">
                            <div className="space-y-2">
                                <label htmlFor="username" className="text-sm font-medium text-slate-700">Username</label>
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

                            <div className="space-y-2">
                                <label htmlFor="password" className="text-sm font-medium text-slate-700">Password</label>
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

                            <div className="flex justify-between items-center">
                                <label className="flex items-center gap-3 cursor-pointer">
                                    <input
                                        type="checkbox"
                                        className="w-4 h-4 accent-[#1e40af]"
                                        checked={rememberMe}
                                        onChange={(e) => setRememberMe(e.target.checked)}
                                    />
                                    <span className="text-sm text-slate-600">Remember me</span>
                                </label>
                                <Link to="/forgottenpassword" className="text-sm font-medium text-[#1e40af] hover:underline">
                                    Forgot password?
                                </Link>
                            </div>

                            <button
                                type="submit"
                                disabled={isLoading}
                                className={`w-full py-3.5 rounded-lg text-white font-medium text-base transition-all duration-300 active:scale-95 flex items-center justify-center gap-3
                                    ${isLoading ? 'bg-slate-300 cursor-not-allowed' : 'bg-[#1e40af] hover:bg-[#1e3a8a] shadow-lg'}`}
                            >
                                {isLoading ? (
                                    <>
                                        <svg className="animate-spin h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                                        </svg>
                                        Signing in...
                                    </>
                                ) : 'Log in'}
                            </button>
                        </form>
                    </div>
                </section>
            </main>

            <LoginFooter />
        </div>
    );
}
