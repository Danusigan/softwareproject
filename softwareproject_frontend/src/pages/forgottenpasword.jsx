import React, { useState } from 'react';
import Header from '../components/header';
import Footer from '../components/footer';
import { useNavigate, Link } from 'react-router-dom';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [message, setMessage] = useState({ type: '', text: '' });
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsLoading(true);
    setMessage({ type: '', text: '' });

    // Simulate password reset request
    setTimeout(() => {
      setMessage({ 
        type: 'success', 
        text: 'Password reset instructions have been sent to your email.' 
      });
      setIsLoading(false);
      setEmail('');
    }, 2000);
  };

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <Header />

      <div className="max-w-4xl mx-auto px-4 mb-8">
        <div className="flex justify-center space-x-8">
          <Link to="/" className="text-gray-700 hover:text-blue-600">Home</Link>
          <Link to="/loginpage" className="text-gray-700 hover:text-blue-600">Login</Link>
        </div>
      </div>

      <div className="max-w-md mx-auto px-4">
        <div className="bg-white p-8 rounded-lg shadow-md">
          <h2 className="text-2xl font-bold text-center mb-6">Forgot Password</h2>
          
          {message.text && (
            <div className={`mb-4 p-3 rounded-lg ${
              message.type === 'success' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
            }`}>
              {message.text}
            </div>
          )}

          <p className="text-gray-600 mb-6 text-center">
            Enter your email address and we'll send you instructions to reset your password.
          </p>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Email Address</label>
              <input
                type="email"
                className="w-full p-3 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Enter your email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>

            <button
              type="submit"
              className="w-full bg-blue-700 text-white p-3 rounded font-semibold hover:bg-blue-800 transition-colors disabled:bg-gray-400"
              disabled={isLoading}
            >
              {isLoading ? 'Sending...' : 'Reset Password'}
            </button>
          </form>

          <div className="mt-6 text-center">
            <Link to="/loginpage" className="text-blue-600 hover:underline">
              Back to Login
            </Link>
          </div>
        </div>
      </div>

      <Footer />
    </div>
  );
}
