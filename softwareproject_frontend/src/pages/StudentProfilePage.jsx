import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import axios from 'axios'
import authService from '../services/authService'
import studentService from '../services/studentService'
import { getInbox, markRead, markAllRead, changePassword } from '../services/profileService'
import ProfileView from '../components/ProfileView'

export default function StudentProfilePage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const me = authService.getUserInfo()?.username
  const studentId = params.get('id') || me
  const isOwn = studentId === me
  const [student, setStudent] = useState(null)
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
    } catch { /* ignore */ }
  }
  const loadInbox = async () => {
    try { setInbox(await getInbox()) } catch { /* ignore */ }
  }

  useEffect(() => {
    const load = async () => {
      try {
        const r = await studentService.list({}, { headers: { Authorization: `Bearer ${authService.getToken()}` } })
        setStudent((r.data?.data || []).find(s => s.studentId === studentId) || null)
      } catch (e) {
        setError(e.response?.data?.message || 'Could not load student details.')
      }
      await loadModules()
      try {
        setActivity((await axios.get('/api/profile/login-activity', { params: { username: studentId } })).data)
      } catch { /* ignore */ }
      if (isOwn) await loadInbox()
      setLoading(false)
    }
    load()
  }, [studentId])

  return (
    <ProfileView
      name={student?.studentName || studentId}
      subtitle="Student"
      details={[
        { label: 'Student ID', value: student?.studentId },
        { label: 'Email address', value: student?.email },
        { label: 'Academic Year', value: student?.academicYear },
        { label: 'Batch', value: student?.batch },
      ]}
      modules={modules}
      loading={loading}
      error={error}
      loginActivity={activity}
      inbox={isOwn ? inbox : undefined}
      onMarkRead={async id => { await markRead(id); loadInbox() }}
      onMarkAllRead={async () => { await markAllRead(); loadInbox() }}
      onChangePassword={isOwn ? changePassword : undefined}
      onCreateModule={async form => { await axios.post('/api/modules/create', form); await loadModules() }}
      onModuleClick={m => navigate(`/marks-workbench/${m.moduleId}`)}
      onSendMessage={isOwn ? undefined : content => axios.post('/api/messages', { recipient: studentId, content })}
    />
  )
}
