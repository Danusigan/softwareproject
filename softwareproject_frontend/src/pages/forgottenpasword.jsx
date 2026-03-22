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
    <div className="min-h-screen bg-slate-50 flex flex-col relative overflow-hidden">
      <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-indigo-500/10 rounded-full blur-[120px] pointer-events-none" />
      <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-sky-500/10 rounded-full blur-[120px] pointer-events-none" />

      <Header />

      <main className="flex-1 flex items-center justify-center p-6 relative z-10 animate-in fade-in zoom-in-95 duration-700">
        <div className="w-full max-w-md bg-white/95 backdrop-blur rounded-3xl border border-slate-200/80 overflow-hidden shadow-xl shadow-slate-900/5">
          <div className="p-8 md:p-10 flex flex-col justify-center">
            <div className="mb-8 text-center">
              <h2 className="text-3xl font-semibold tracking-tight text-slate-900 mb-3">Forgot Password</h2>
              <p className="text-slate-600 text-base">
                Enter your email and we will send reset instructions.
              </p>
            </div>

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
                <label htmlFor="email" className="text-sm font-medium text-slate-700 ml-1">Email Address</label>
                <input
                  id="email"
                  type="email"
                  className="input-field bg-white"
                  placeholder="Enter your email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                />
              </div>

              <button
                type="submit"
                className="w-full py-3.5 px-8 rounded-xl text-white font-medium text-base shadow-lg transition-all duration-300 transform active:scale-95 bg-indigo-600 hover:bg-indigo-700 hover:shadow-indigo-200/80 disabled:bg-slate-300 disabled:shadow-none"
                disabled={isLoading}
              >
                {isLoading ? 'Sending...' : 'Send Reset Link'}
              </button>
            </form>

            <div className="mt-6 flex items-center justify-between text-sm">
              <Link to="/" className="text-slate-600 hover:text-slate-800 transition-colors">
                Home
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
