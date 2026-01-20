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
    <nav className="bg-blue-700 text-white ">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-24">
          {/* Logo and Branding */}
          <div className="flex items-center gap-4">
            <div className="bg-white p-2 rounded">
              <div className="w-12 h-12 bg-blue-900 rounded flex items-center justify-center">
                <span className="text-white font-bold text-lg">LO</span>
              </div>
            </div>
            <div>
              <h1 className="text-lg font-bold">LO-PO</h1>
              <p className="text-sm">Faculty of Engineering</p>
              <p className="text-sm">University of Ruhuna</p>
            </div>
          </div>

          {/* Navigation Links */}
          <div className="flex items-center gap-8">
            <Link to="/" className="hover:text-blue-200 transition">Home</Link>

            {user ? (
              /* Logged in user */
              <div className="flex items-center gap-4">
                <span className="text-sm">
                  Welcome, <strong>{user.username}</strong> ({user.userType})
                </span>
                <button 
                  onClick={handleLogout}
                  className="bg-red-500 text-white px-4 py-2 rounded hover:bg-red-600 transition"
                >
                  Logout
                </button>
              </div>
            ) : (
              /* Not logged in */
              <>
                <Link 
                  to="/loginpage" 
                  className="bg-white text-blue-700 px-6 py-2 rounded font-semibold hover:bg-blue-50 transition"
                >
                  Login
                </Link>
              </>
            )}
          </div>
        </div>
      </div>
    </nav>
  )
}
