export const PASSWORD_RULE = 'Use at least 8 characters with uppercase, lowercase and a number. Maximum 72 bytes (non-English characters may use more than one byte).'
export const USERNAME_RULE = 'Use 3–64 letters, numbers, dots, underscores or hyphens; start with a letter or number.'

export function passwordError(value) {
  return typeof value !== 'string' || value.length < 8 || new TextEncoder().encode(value).length > 72
    || /[\x00-\x1f\x7f-\x9f]/.test(value) || !/[A-Z]/.test(value) || !/[a-z]/.test(value) || !/[0-9]/.test(value)
    ? PASSWORD_RULE : ''
}

export function usernameError(value) {
  return /^[A-Za-z0-9][A-Za-z0-9._-]{2,63}$/.test(value || '') ? '' : USERNAME_RULE
}
