import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import axios from 'axios'
import authService from '../services/authService'
import { getInbox, markRead, markAllRead, changePassword } from '../services/profileService'
import ProfileView from '../components/ProfileView'

export default function LecturerProfilePage() {
  const navigate = useNavigate()
  const user = authService.getUserInfo() || {}
  const role = (user.userType || '').toLowerCase().trim()
  const isLecturer = role === 'lecture'
  const roleLabel = { lecture: 'Lecturer', admin: 'Admin', superadmin: 'Super Admin' }[role] || 'User'
  const [modules, setModules] = useState([])
  const [activity, setActivity] = useState(null)
  const [inbox, setInbox] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadModules = async () => {
    try {
      const r = await axios.get('/api/modules/all')
      const list = r.data?.data || r.data || []
      setModules(Array.isArray(list) ? list : [])
    } catch (e) {
      setError(e.response?.data?.message || 'Could not load modules.')
    } finally { setLoading(false) }
  }
  const loadActivity = async () => {
    try { setActivity((await axios.get('/api/profile/login-activity')).data) } catch { /* blank */ }
  }
  const loadInbox = async () => {
    try { setInbox(await getInbox()) } catch { /* blank */ }
  }

  useEffect(() => {
    loadActivity()
    if (isLecturer) { loadModules(); loadInbox() }
  }, [])

  return (
    <ProfileView
      name={user.username}
      subtitle={roleLabel}
      details={[
        { label: 'Username', value: user.username },
        { label: 'Email address', value: user.email },
        { label: 'Role', value: roleLabel },
      ]}
      modules={isLecturer ? modules : undefined}
      loading={loading}
      error={error}
      loginActivity={activity}
      inbox={isLecturer ? inbox : undefined}
      onMarkRead={async id => { await markRead(id); loadInbox() }}
      onMarkAllRead={async () => { await markAllRead(); loadInbox() }}
      onChangePassword={changePassword}
      onCreateModule={isLecturer ? async form => { await axios.post('/api/modules/create', form); await loadModules() } : undefined}
      onModuleClick={m => navigate(`/marks-workbench/${m.moduleId}`)}
    />
  )
}
