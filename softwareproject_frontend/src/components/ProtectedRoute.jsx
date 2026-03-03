// Protected Route - Ensures user is logged in before accessing a page
import React from 'react';
import { Navigate } from 'react-router-dom';
import authService from '../services/authService';

export const ProtectedRoute = ({ children, requiredRole = null }) => {
  // Check if user is logged in
  if (!authService.isLoggedIn()) {
    return <Navigate to="/loginpage" replace />;
  }

  // If a specific role is required, check it
  if (requiredRole) {
    const userInfo = authService.getUserInfo();
    const userRole = userInfo?.userType?.toLowerCase()?.trim();
    const requiredRoleLower = requiredRole.toLowerCase();

    if (userRole !== requiredRoleLower) {
      return <Navigate to="/" replace />;
    }
  }

  return children;
};

export default ProtectedRoute;
