"use client"
import Header from '../components/header'
import Footer from '../components/footer'
import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import axios from 'axios'; 

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
            // 🚨 CRITICAL FIX: The key must be 'userID' to match the backend's User model
            const res = await axios.post('http://localhost:8080/api/auth/login', {
                userID: username, // <-- CORRECTED: Use 'userID' key
                password
            });

            // Handle successful response (backend returns a JSON object with status: 'SUCCESS')
            if (res.data && res.data.status === "SUCCESS") {
                const loggedInUsername = res.data.userId; // Retrieve username from response
                
                localStorage.setItem('username', loggedInUsername);

                if (rememberMe) {
                    localStorage.setItem('rememberMe', 'true');
                } else {
                    localStorage.removeItem('rememberMe');
                }

                // Navigate to the home page or dashboard
                navigate('/');
            } else {
                // This block handles cases where the server returns a 200 OK 
                // but the status is 'ERROR' (e.g., if you added custom logic)
                setError(res.data.message || 'Login failed. Invalid response from server.');
            }
        } catch (err) {
            console.error("Login Error:", err.response ? err.response.data : err.message);
            // This catches the 401 Unauthorized error:
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
                                <div>
                                    <div className="flex items-center mb-2">
                                        <label>Enter Your Username</label>
                                    </div>
                                    <input
                                        type="text"
                                        className="w-full p-3 border rounded"
                                        placeholder="Username"
                                        value={username}
                                        onChange={(e) => setUsername(e.target.value)}
                                        required
                                    />
                                </div>

                                <div>
                                    <div className="flex items-center mb-2">
                                        <label>Enter Your Password</label>
                                    </div>
                                    <input
                                        type="password"
                                        className="w-full p-3 border rounded"
                                        placeholder="Password"
                                        value={password}
                                        onChange={(e) => setPassword(e.target.value)}
                                        required
                                    />
                                </div>

                                <div className="flex justify-between items-center">
                                    <label className="flex items-center">
                                        <input
                                            type="checkbox"
                                            className="mr-2"
                                            checked={rememberMe}
                                            onChange={(e) => setRememberMe(e.target.checked)}
                                        />
                                        <span>Remember me?</span>
                                    </label>
                                    <a href="#" className="text-blue-600 text-sm">Forget Password</a>
                                </div>

                                <button
                                    type="submit"
                                    className="w-full bg-blue-700 text-white p-3 rounded font-semibold hover:bg-blue-800 mt-4"
                                    disabled={isLoading}
                                >
                                    {isLoading ? 'Logging in...' : 'Login'}
                                </button>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
            <Footer />
        </div>
    )
}