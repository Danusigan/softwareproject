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

        try {
            const res = await axios.post('http://localhost:8080/api/auth/login', {
                userID: username,
                password
            });

            // Handle successful response
            if (res.data && res.data.status === "SUCCESS") {
                const loggedInUsername = res.data.userId;
                const userType = res.data.userType;
                const token = res.data.token;
                
                // Store user data and token
                localStorage.setItem('username', loggedInUsername);
                localStorage.setItem('userType', userType);
                localStorage.setItem('token', token);

                if (rememberMe) {
                    localStorage.setItem('rememberMe', 'true');
                } else {
                    localStorage.removeItem('rememberMe');
                }

                // Navigate based on user role
                if (userType === 'Superadmin' || userType === 'Admin') {
                    navigate('/admin-dashboard');
                } else if (userType === 'Lecture') {
                    navigate('/lecturer-dashboard');
                } else {
                    navigate('/');
                }
            } else {
                setError(res.data.message || 'Login failed. Invalid response from server.');
            }
        } catch (err) {
            console.error("Login Error:", err.response ? err.response.data : err.message);
            setError('Login failed. Please check your username and password.'); 
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-gray-50 py-8">
            {/* Header */}
            <Header />

            {/* Navigation */}
            <div className="max-w-4xl mx-auto px-4 mb-8">
                <div className="flex justify-center space-x-8">
                    <Link to="/" className="text-gray-700 hover:text-blue-600">Home</Link>
                    <a href="#" className="text-blue-600 font-semibold border-b-2 border-blue-600">Login</a>
                </div>
            </div>

            {/* Main Content */}
            <div className="max-w-6xl mx-auto px-4">
                <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">

                    {/* Login Form */}
                    <div className="bg-white p-8 rounded-lg shadow-md">
                        <h2 className="text-2xl font-bold text-center mb-6">Login</h2>

                        {/* Error Message */}
                        {error && (
                            <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
                                {error}
                            </div>
                        )}

                        <div className="space-y-4">
                            <form onSubmit={handleLogin}>
                                {/* User Role Dropdown - ABOVE username and password */}
                                <div className="mb-4">
                                    <div className="flex items-center mb-2">
                                        <label className="font-medium">Select User Role</label>
                                    </div>
                                    <select
                                        className="w-full p-3 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white"
                                        value={userRole}
                                        onChange={(e) => setUserRole(e.target.value)}
                                        required
                                    >
                                        <option value="">-- Select User Role --</option>
                                        <option value="Superadmin">Superadmin</option>
                                        <option value="Admin">Admin</option>
                                        <option value="Lecture">Lecture</option>
                                    </select>
                                </div>

                                {/* Username Field */}
                                <div className="mb-4">
                                    <div className="flex items-center mb-2">
                                        <label className="font-medium">Enter Your Username</label>
                                    </div>
                                    <input
                                        type="text"
                                        className="w-full p-3 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                                        placeholder="Username"
                                        value={username}
                                        onChange={(e) => setUsername(e.target.value)}
                                        required
                                    />
                                </div>

                                {/* Password Field */}
                                <div className="mb-4">
                                    <div className="flex items-center mb-2">
                                        <label className="font-medium">Enter Your Password</label>
                                    </div>
                                    <input
                                        type="password"
                                        className="w-full p-3 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                                        placeholder="Password"
                                        value={password}
                                        onChange={(e) => setPassword(e.target.value)}
                                        required
                                    />
                                </div>

                                <div className="flex justify-between items-center mb-4">
                                    <label className="flex items-center">
                                        <input
                                            type="checkbox"
                                            className="mr-2"
                                            checked={rememberMe}
                                            onChange={(e) => setRememberMe(e.target.checked)}
                                        />
                                        <span>Remember me?</span>
                                    </label>
                                    <Link to="/forgottenpassword" className="text-blue-600 text-sm hover:underline">Forget Password</Link>
                                </div>

                                <button
                                    type="submit"
                                    className="w-full bg-blue-700 text-white p-3 rounded font-semibold hover:bg-blue-800 transition-colors disabled:bg-gray-400"
                                    disabled={isLoading}
                                >
                                    {isLoading ? 'Logging in...' : 'Login'}
                                </button>
                            </form>
                        </div>
                    </div>

                    {/* Right Side Image */}
                    <div className="hidden lg:flex items-center justify-center">
                        <div className="bg-gradient-to-br from-blue-100 to-gray-100 rounded-lg p-8 w-full h-96 flex items-center justify-center border border-gray-300">
                            <div className="text-center">
                                <div className="text-6xl mb-4">🔐</div>
                                <p className="text-blue-600 font-semibold">Secure Login Portal</p>
                                <p className="text-gray-600 mt-2">Access your dashboard</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <Footer />
        </div>
    )
}
