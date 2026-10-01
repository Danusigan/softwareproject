import axios from 'axios'

export const getInbox = () => axios.get('/api/messages/inbox').then(r => r.data)
export const getUnreadCount = () => axios.get('/api/messages/unread-count').then(r => r.data.count)
export const markRead = id => axios.put(`/api/messages/${id}/read`)
export const markAllRead = () => axios.put('/api/messages/read-all')
export const changePassword = (currentPassword, newPassword) =>
  axios.post('/api/profile/change-password', { currentPassword, newPassword })
