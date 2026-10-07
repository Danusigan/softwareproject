import { Link, useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import authService from '../services/authService';

export default function Header() {
  const navigate = useNavigate();
  const [user, setUser] = useState(null);

  useEffect(() => {
    // ✅ Check login status
    setUser(authService.getUserInfo());

    // ✅ Auto-logout when token expires
    const interval = setInterval(() => {
      if (authService.isLoggedIn()) {
        const remaining = authService.getTimeRemaining();
        if (remaining <= 0) {
          handleLogout();
          return;
        }
      }
      setUser(authService.getUserInfo());
    }, 5000); // Check every 5 seconds

    return () => clearInterval(interval);
  }, [navigate]);

  const handleLogout = () => {
    authService.logout();
    setUser(null);
    navigate('/loginpage', { replace: true });
  };

  return (
    <nav className="bg-[#1e40af] text-white shadow-lg sticky top-0 z-30">
      <div className="max-w-7xl mx-auto px-4 sm:px-10">
        <div className="flex justify-between items-center h-16 sm:h-24 gap-2">
          {/* Logo and Branding */}
          <button
            type="button"
            className="flex items-center gap-3 sm:gap-5 cursor-pointer group min-w-0"
            onClick={() => navigate('/')}
            aria-label="Go to home"
          >
            <div className="bg-white p-1.5 sm:p-2.5 rounded-xl shrink-0 shadow-md group-hover:scale-105 transition-transform duration-300">
              <div className="w-8 h-8 sm:w-11 sm:h-11 bg-[#1e3a8a] rounded-lg flex items-center justify-center">
                <span className="text-white font-black text-base sm:text-xl tracking-tighter">LO</span>
              </div>
            </div>
            <div className="border-l border-white/20 pl-3 sm:pl-5 text-left min-w-0">
              <h1 className="text-[11px] sm:text-xl font-black tracking-tight leading-none mb-1 text-white truncate">LO-PO ANALYTICS</h1>
              <div className="hidden sm:flex flex-col">
                <span className="text-[10px] font-bold uppercase tracking-[0.15em] text-blue-100">Faculty of Engineering</span>
                <span className="text-[10px] font-medium uppercase tracking-[0.1em] text-blue-200/80">University of Ruhuna</span>
              </div>
            </div>
          </button>

          {/* Navigation Links */}
          <div className="flex items-center gap-2 sm:gap-10 shrink-0">
            <Link to="/" className="text-xs sm:text-sm font-bold hover:text-blue-200 transition-colors uppercase tracking-wider sm:tracking-widest relative group/link">
              <span>Home</span>
              <span className="absolute -bottom-1 left-0 w-0 h-0.5 bg-blue-300 group-hover/link:w-full transition-all duration-300"></span>
            </Link>

            {user ? (
              /* Logged in user */
              <div className="flex items-center gap-6 pl-8 border-l border-white/20">
                {['admin', 'superadmin'].includes(user.userType?.toLowerCase()) && (
                  <Link to="/admin-dashboard" className="text-xs font-bold">Administration</Link>
                )}
                {['lecture', 'admin', 'superadmin'].includes(user.userType?.toLowerCase()) && (
                  <Link to="/lecturer-dashboard" className="text-xs font-bold">Module workspace</Link>
                )}
                               <Link to="/profile" className="flex items-center gap-3 group/profile" aria-label="Open my profile">
                                 <div className="w-10 h-10 rounded-full bg-white/20 flex items-center justify-center text-sm font-black text-white group-hover/profile:bg-white/30 transition-colors">
                                   {(user.username || '?').slice(0, 2).toUpperCase()}
                                 </div>
                                 <div className="text-right hidden sm:block">
                                   <p className="text-[10px] font-bold text-blue-200 uppercase tracking-widest leading-none mb-1">My profile</p>
                                   <p className="text-sm font-black text-white">{user.username}</p>
                                 </div>
                               </Link>

                <button
                  onClick={handleLogout}
                  className="bg-red-500 text-white px-3 sm:px-6 py-2 sm:py-2.5 rounded-xl text-xs font-black uppercase tracking-widest hover:bg-red-600 transition-all shadow-md hover:shadow-lg active:scale-95"
                >
                  Logout
                </button>
              </div>
            ) : (
              /* Not logged in */
              <Link
                to="/loginpage"
                className="bg-white text-[#1e40af] px-4 sm:px-8 py-2 sm:py-2.5 rounded-xl text-xs font-black uppercase tracking-widest hover:bg-blue-50 transition-all shadow-md hover:shadow-lg active:scale-95"
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
