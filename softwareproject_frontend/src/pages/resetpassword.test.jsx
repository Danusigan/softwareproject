import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import axios from 'axios'
import ResetPasswordPage from './resetpassword'
import { passwordError } from '../utils/accountValidation'

vi.mock('axios', () => ({ default: { post: vi.fn() } }))
beforeEach(() => { vi.clearAllMocks(); localStorage.clear() })

function submit(password) {
  render(<MemoryRouter initialEntries={['/resetpassword?token=test-token']}><ResetPasswordPage /></MemoryRouter>)
  fireEvent.change(screen.getByLabelText('New Password'), { target: { value: password } })
  fireEvent.change(screen.getByLabelText('Confirm Password'), { target: { value: password } })
  fireEvent.click(screen.getByRole('button', { name: 'Reset Password' }))
}

describe('password validation in recovery', () => {
  it.each(['lowercase123', 'ALLUPPER123', 'NoDigitsHere', 'Aa1' + 'é'.repeat(35)])(
    'rejects an invalid new password before sending a reset request (%s)', (password) => {
      submit(password)
      expect(axios.post).not.toHaveBeenCalled()
      expect(screen.getAllByText(passwordError(password)).length).toBeGreaterThan(0)
    },
  )
  it('rejects control characters in raw password values', () => {
    // Browser password inputs remove newlines before onChange; API clients still need this rule.
    expect(passwordError('Ab1\nhello')).not.toBe('')
  })
  it('allows the exact BCrypt byte boundary without trimming spaces', async () => {
    const password = ' Aa1' + 'x'.repeat(67) + ' '
    axios.post.mockResolvedValue({ data: { message: 'Password reset successful.' } })
    submit(password)
    await waitFor(() => expect(axios.post).toHaveBeenCalledWith('/api/auth/reset-password', { token: 'test-token', newPassword: password }))
    expect(await screen.findByText('Password reset successful.')).toBeInTheDocument()
  })
})
