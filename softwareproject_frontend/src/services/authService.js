// Auth Service - Manages login state and session expiration
export const authService = {
  // ✅ Check if user is logged in (token exists and not expired)
  isLoggedIn() {
    const token = localStorage.getItem('token');
    const isLoggedIn = localStorage.getItem('isLoggedIn') === 'true';

    if (!token || !isLoggedIn) {
      return false;
    }

    // Check if token is expired (simple check - compare stored expiration)
    const tokenExpiry = localStorage.getItem('tokenExpiry');
    if (tokenExpiry && Date.now() > Number.parseInt(tokenExpiry, 10)) {
      this.logout();
      return false;
    }

    return true;
  },

  // ✅ Get stored token
  getToken() {
    if (this.isLoggedIn()) {
      return localStorage.getItem('token');
    }
    return null;
  },

  // ✅ Get user info (username, userType)
  getUserInfo() {
    if (!this.isLoggedIn()) {
      return null;
    }
    return {
      username: localStorage.getItem('username'),
      userType: localStorage.getItem('userType'),
      token: localStorage.getItem('token')
    };
  },

  // ✅ Store login data with 2-hour expiration
  storeLogin(token, username, userType, rememberMe = false) {
    // Store token and user info
    localStorage.setItem('token', token);
    localStorage.setItem('username', username);
    localStorage.setItem('userType', userType);
    localStorage.setItem('isLoggedIn', 'true');

    // Calculate and store token expiry (2 hours from now)
    const expiryTime = Date.now() + 2 * 60 * 60 * 1000; // 2 hours in milliseconds
    localStorage.setItem('tokenExpiry', expiryTime.toString());

    // Handle remember me
    if (rememberMe) {
      localStorage.setItem('rememberMe', 'true');
    } else {
      localStorage.removeItem('rememberMe');
    }
  },

  // ✅ Clear all login data
  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('userType');
    localStorage.removeItem('isLoggedIn');
    localStorage.removeItem('tokenExpiry');
    localStorage.removeItem('rememberMe');
  },

  // ✅ Get time remaining until logout (in seconds)
  getTimeRemaining() {
    const tokenExpiry = localStorage.getItem('tokenExpiry');
    if (!tokenExpiry) {
      return 0;
    }
    const remaining = Number.parseInt(tokenExpiry, 10) - Date.now();
    return Math.max(0, Math.floor(remaining / 1000));
  }
};

export default authService;
