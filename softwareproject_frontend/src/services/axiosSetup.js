// Axios Setup - Configure interceptors for token management
import axios from 'axios';
import authService from './authService';

export const setupAxiosInterceptors = (navigate) => {
  // ✅ Request Interceptor - Add token to every request
  axios.interceptors.request.use(
    (config) => {
      const token = authService.getToken();

      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }

      return config;
    },
    (error) => {
      return Promise.reject(error);
    }
  );

  // ✅ Response Interceptor - Handle 401 (expired token) and redirect to login
  axios.interceptors.response.use(
    (response) => {
      return response;
    },
    (error) => {
      if (error.response?.status === 401) {
        // Token expired or invalid
        console.warn('Token expired or invalid. Logging out...');
        authService.logout();

        // Navigate to login page
        if (navigate) {
          navigate('/loginpage', { replace: true });
        }

        // Show message to user
        globalThis.dispatchEvent(
          new CustomEvent('authExpired', {
            detail: { message: 'Your session has expired. Please login again.' }
          })
        );
      }

      return Promise.reject(error);
    }
  );
};

export default axios;
