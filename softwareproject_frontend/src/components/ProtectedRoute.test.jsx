import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { ProtectedRoute } from './ProtectedRoute'
import authService from '../services/authService'

/**
 * Component tests for ProtectedRoute's role gate. Drives real localStorage through
 * authService.storeLogin/logout rather than mocking authService, so the test exercises
 * the actual contract between the two (including the 2-hour token-expiry check),
 * not just ProtectedRoute in isolation.
 */
function renderProtected(requiredRole) {
  return render(
    <MemoryRouter initialEntries={['/protected']}>
      <Routes>
        <Route path="/loginpage" element={<div>Login Page</div>} />
        <Route path="/" element={<div>Landing Page</div>} />
        <Route
          path="/protected"
          element={
            <ProtectedRoute requiredRole={requiredRole}>
              <div>Protected Content</div>
            </ProtectedRoute>
          }
        />
      </Routes>
    </MemoryRouter>
  )
}

describe('ProtectedRoute', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  afterEach(() => {
    localStorage.clear()
  })

  it('redirects to the login page when no one is logged in', () => {
    renderProtected()

    expect(screen.getByText('Login Page')).toBeInTheDocument()
    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument()
  })

  it('renders the protected content once logged in with no role requirement', () => {
    authService.storeLogin('fake-jwt', 'lecturer1', 'lecture')

    renderProtected()

    expect(screen.getByText('Protected Content')).toBeInTheDocument()
  })

  it('redirects to the landing page when the logged-in user lacks the required role', () => {
    authService.storeLogin('fake-jwt', 'lecturer1', 'lecture')

    renderProtected('admin')

    expect(screen.getByText('Landing Page')).toBeInTheDocument()
    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument()
  })

  it('renders the protected content when the role matches case-insensitively', () => {
    authService.storeLogin('fake-jwt', 'admin1', 'Admin')

    renderProtected('admin')

    expect(screen.getByText('Protected Content')).toBeInTheDocument()
  })

  it('redirects to the login page once the stored token has expired', () => {
    authService.storeLogin('fake-jwt', 'lecturer1', 'lecture')
    localStorage.setItem('tokenExpiry', String(Date.now() - 1000))

    renderProtected()

    expect(screen.getByText('Login Page')).toBeInTheDocument()
  })
})
