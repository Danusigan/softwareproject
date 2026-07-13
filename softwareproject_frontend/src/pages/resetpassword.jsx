import React, { useState } from 'react';
import Header from '../components/header';
import Footer from '../components/footer';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import axios from 'axios';

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') || '';
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [message, setMessage] = useState({ type: '', text: '' });
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage({ type: '', text: '' });

    if (!token) {
      setMessage({ type: 'error', text: 'Reset link is missing or invalid.' });
      return;
    }
    if (newPassword !== confirmPassword) {
      setMessage({ type: 'error', text: 'Passwords do not match.' });
      return;
    }
    if (newPassword.length < 6) {
      setMessage({ type: 'error', text: 'Password must be at least 6 characters.' });
      return;
    }

    setIsLoading(true);
    try {
      const res = await axios.post('http://localhost:8080/api/auth/reset-password', {
        token,
        newPassword
      });
      setMessage({ type: 'success', text: res.data?.message || 'Password reset successful.' });
      setNewPassword('');
      setConfirmPassword('');
      setTimeout(() => navigate('/loginpage', { replace: true }), 2000);
    } catch (err) {
      setMessage({
        type: 'error',
        text: err.response?.data?.message || 'Something went wrong. Please try again.'
      });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col relative overflow-hidden">
      <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-indigo-500/10 rounded-full blur-[120px] pointer-events-none" />
      <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-sky-500/10 rounded-full blur-[120px] pointer-events-none" />

      <Header />

      <main className="flex-1 flex items-center justify-center p-6 relative z-10 animate-in fade-in zoom-in-95 duration-700">
        <div className="w-full max-w-md bg-white/95 backdrop-blur rounded-3xl border border-slate-200/80 overflow-hidden shadow-xl shadow-slate-900/5">
          <div className="p-8 md:p-10 flex flex-col justify-center">
            <div className="mb-8 text-center">
              <h2 className="text-3xl font-semibold tracking-tight text-slate-900 mb-3">Reset Password</h2>
              <p className="text-slate-600 text-base">
                Choose a new password for your account.
              </p>
            </div>

            {!token && (
              <div className="mb-6 p-3.5 rounded-xl border bg-red-50 border-red-200 text-red-700">
                <p className="text-sm font-medium">
                  This reset link is missing its token. Please use the link from your email, or request a new one.
                </p>
              </div>
            )}

            {message.text && (
              <div className={`mb-6 p-3.5 rounded-xl border ${
                message.type === 'success'
                  ? 'bg-emerald-50 border-emerald-200 text-emerald-700'
                  : 'bg-red-50 border-red-200 text-red-700'
              }`}>
                <p className="text-sm font-medium">{message.text}</p>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-5">
              <div className="space-y-2">
                <label htmlFor="newPassword" className="text-sm font-medium text-slate-700 ml-1">New Password</label>
                <input
                  id="newPassword"
                  type="password"
                  className="input-field bg-white"
                  placeholder="Enter new password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  required
                />
              </div>

              <div className="space-y-2">
                <label htmlFor="confirmPassword" className="text-sm font-medium text-slate-700 ml-1">Confirm Password</label>
                <input
                  id="confirmPassword"
                  type="password"
                  className="input-field bg-white"
                  placeholder="Re-enter new password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  required
                />
              </div>

              <button
                type="submit"
                className="w-full py-3.5 px-8 rounded-xl text-white font-medium text-base shadow-lg transition-all duration-300 transform active:scale-95 bg-indigo-600 hover:bg-indigo-700 hover:shadow-indigo-200/80 disabled:bg-slate-300 disabled:shadow-none"
                disabled={isLoading || !token}
              >
                {isLoading ? 'Resetting...' : 'Reset Password'}
              </button>
            </form>

            <div className="mt-6 flex items-center justify-between text-sm">
              <Link to="/forgottenpassword" className="text-slate-600 hover:text-slate-800 transition-colors">
                Request new link
              </Link>
              <Link to="/loginpage" className="text-indigo-600 hover:text-indigo-700 transition-colors font-medium">
                Back to Login
              </Link>
            </div>
          </div>
        </div>
      </main>

      <Footer />
    </div>
  );
}
