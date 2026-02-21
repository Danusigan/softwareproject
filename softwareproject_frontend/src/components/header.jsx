"use client"
import { Link, useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';

export default function Header() {
  const navigate = useNavigate();
  const [user, setUser] = useState(null);

  useEffect(() => {
    const username = localStorage.getItem('username');
    const userType = localStorage.getItem('userType');
    if (username && userType) {
      setUser({ username, userType });
    }
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('username');
    localStorage.removeItem('userType');
    localStorage.removeItem('token');
    localStorage.removeItem('rememberMe');
    setUser(null);
    navigate('/');
  };

  return (
    <nav className="bg-[#1e40af] text-white shadow-lg sticky top-0 z-[100]">
      <div className="max-w-7xl mx-auto px-6 sm:px-10">
        <div className="flex justify-between items-center h-24">
          {/* Logo and Branding */}
          <div className="flex items-center gap-5 cursor-pointer group" onClick={() => navigate('/')}>
            <div className="bg-white p-2.5 rounded-xl shadow-md group-hover:scale-105 transition-transform duration-300">
              <div className="w-11 h-11 bg-[#1e3a8a] rounded-lg flex items-center justify-center">
                <span className="text-white font-black text-xl tracking-tighter">LO</span>
              </div>
            </div>
            <div className="border-l border-white/20 pl-5">
              <h1 className="text-xl font-black tracking-tight leading-none mb-1">LO-PO ANALYTICS</h1>
              <div className="flex flex-col">
                <span className="text-[10px] font-bold uppercase tracking-[0.15em] text-blue-100">Faculty of Engineering</span>
                <span className="text-[10px] font-medium uppercase tracking-[0.1em] text-blue-200/80">University of Ruhuna</span>
              </div>
            </div>
          </div>

          {/* Navigation Links */}
          <div className="flex items-center gap-10">
            <Link to="/" className="text-sm font-bold hover:text-blue-200 transition-colors uppercase tracking-widest relative group/link">
              Home
              <span className="absolute -bottom-1 left-0 w-0 h-0.5 bg-blue-300 group-hover/link:w-full transition-all duration-300"></span>
            </Link>

            {user ? (
              /* Logged in user */
              <div className="flex items-center gap-6 pl-8 border-l border-white/20">
                <div className="text-right hidden sm:block">
                  <p className="text-[10px] font-bold text-blue-200 uppercase tracking-widest leading-none mb-1">Account</p>
                  <p className="text-sm font-black text-white">{user.username}</p>
                </div>

                <button
                  onClick={handleLogout}
                  className="bg-red-500 text-white px-6 py-2.5 rounded-xl text-xs font-black uppercase tracking-widest hover:bg-red-600 transition-all shadow-md hover:shadow-lg active:scale-95"
                >
                  Logout
                </button>
              </div>
            ) : (
              /* Not logged in */
              <Link
                to="/loginpage"
                className="bg-white text-[#1e40af] px-8 py-2.5 rounded-xl text-xs font-black uppercase tracking-widest hover:bg-blue-50 transition-all shadow-md hover:shadow-lg active:scale-95"
              >
                Login
              </Link>
            )}
          </div>
        </div>
      </div>
    </nav>
  )
}
