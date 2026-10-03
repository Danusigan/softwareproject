import { useState } from 'react'
import Header from './header'
import Footer from './footer'

function Card({ title, action, children }) {
  return (
    <div className="bg-white rounded-xl border border-gray-200 p-5 mb-5">
      <div className="flex items-center justify-between mb-3">
        <h2 className="text-lg text-orange-500">{title}</h2>
        {action}
      </div>
      {children}
    </div>
  )
}

const fmt = v => (v ? new Date(v).toLocaleString('en-GB', { dateStyle: 'full', timeStyle: 'short' }) : 'Never')
const ago = v => {
  if (!v) return ''
  const mins = Math.floor((Date.now() - new Date(v).getTime()) / 60000)
  if (mins < 2) return ' (now)'
  const days = Math.floor(mins / 1440)
  return days > 0 ? ` (${days} day${days > 1 ? 's' : ''})` : ` (${Math.floor(mins / 60)} hours)`
}

export default function ProfileView({
  name, subtitle, details, modules, loading, error,
  onModuleClick, onCreateModule, loginActivity, onSendMessage,
  inbox, onMarkRead, onMarkAllRead, onChangePassword,
}) {
  const [msgOpen, setMsgOpen] = useState(false)
  const [text, setText] = useState('')
  const [sending, setSending] = useState(false)
  const [note, setNote] = useState('')

  const [modOpen, setModOpen] = useState(false)
  const [modForm, setModForm] = useState({ moduleId: '', moduleName: '' })
  const [modErr, setModErr] = useState('')
  const [modBusy, setModBusy] = useState(false)

  const [pw, setPw] = useState({ current: '', next: '', confirm: '' })
  const [pwMsg, setPwMsg] = useState({ type: '', text: '' })
  const [pwBusy, setPwBusy] = useState(false)

  const unread = (inbox || []).filter(m => !m.readFlag).length

  const send = async () => {
    if (!text.trim()) return
    setSending(true); setNote('')
    try {
      await onSendMessage(text)
      setText(''); setMsgOpen(false); setNote('Message sent.')
    } catch (e) {
      setNote(e.response?.data?.message || 'Could not send the message.')
    } finally { setSending(false) }
  }

  const createModule = async () => {
    if (!modForm.moduleId.trim() || !modForm.moduleName.trim()) { setModErr('Module ID and name are required.'); return }
    setModBusy(true); setModErr('')
    try {
      await onCreateModule(modForm)
      setModForm({ moduleId: '', moduleName: '' }); setModOpen(false)
    } catch (e) {
      setModErr(e.response?.data?.message || 'Could not create the module.')
    } finally { setModBusy(false) }
  }

  const submitPassword = async () => {
    if (!pw.current || !pw.next) { setPwMsg({ type: 'error', text: 'Fill in all fields.' }); return }
    if (pw.next.length < 8) { setPwMsg({ type: 'error', text: 'New password must be at least 8 characters.' }); return }
    if (pw.next !== pw.confirm) { setPwMsg({ type: 'error', text: 'New passwords do not match.' }); return }
    setPwBusy(true); setPwMsg({ type: '', text: '' })
    try {
      await onChangePassword(pw.current, pw.next)
      setPw({ current: '', next: '', confirm: '' })
      setPwMsg({ type: 'success', text: 'Password changed.' })
    } catch (e) {
      setPwMsg({ type: 'error', text: e.response?.data?.message || 'Could not change the password.' })
    } finally { setPwBusy(false) }
  }

  const inputCls = 'w-full border border-gray-300 rounded-lg p-2 text-sm mb-2'

  return (
    <div className="min-h-screen bg-gray-50">
      <Header />
      <div className="max-w-5xl mx-auto px-4 py-8">
        <div className="flex items-center gap-4 mb-8 flex-wrap">
          <div className="w-16 h-16 rounded-full bg-blue-100 text-blue-800 flex items-center justify-center text-xl font-black">
            {(name || '?').slice(0, 2).toUpperCase()}
          </div>
          <div>
            <h1 className="text-3xl font-bold text-orange-500 uppercase">{name || 'Profile'}</h1>
            <p className="text-gray-500">{subtitle}</p>
          </div>
          {onSendMessage && (
            <button onClick={() => setMsgOpen(true)} className="ml-2 px-4 py-2 border border-gray-300 rounded-lg bg-white text-sm hover:bg-gray-50">
              Message
            </button>
          )}
        </div>

        {note && <div className="mb-5 p-3 rounded-lg bg-green-100 text-green-700">{note}</div>}
        {error && <div className="mb-5 p-4 rounded-lg bg-red-100 text-red-700">{error}</div>}

        <div className="grid md:grid-cols-2 gap-5">
          <div>
            <Card title="User details">
              {details.map(d => (
                <div key={d.label} className="mb-3">
                  <p className="text-sm font-bold text-gray-800">{d.label}</p>
                  <p className="text-sm text-gray-600">{d.value || '—'}</p>
                </div>
              ))}
            </Card>

            <Card
              title="Course details"
              action={onCreateModule && (
                <button onClick={() => setModOpen(true)} className="text-sm px-3 py-1 rounded-lg bg-orange-500 text-white font-semibold hover:bg-orange-600">
                  + Add module
                </button>
              )}
            >
              <p className="text-sm font-bold text-gray-800 mb-2">Course profiles</p>
              {loading && <p className="text-sm text-gray-400">Loading…</p>}
              {!loading && !modules.length && <p className="text-sm text-gray-400">No modules found.</p>}
              {modules.map(m => (
                <button key={m.moduleId} type="button" onClick={() => onModuleClick?.(m)}
                  className="block text-left text-sm text-green-700 hover:underline mb-1">
                  {m.moduleId} {m.moduleName || m.name}
                </button>
              ))}
            </Card>
          </div>

          <div>
            {inbox && (
              <Card
                title={`Messages${unread ? ` (${unread} new)` : ''}`}
                action={unread > 0 && (
                  <button onClick={onMarkAllRead} className="text-xs text-blue-600 hover:underline">Mark all as read</button>
                )}
              >
                {!inbox.length && <p className="text-sm text-gray-400">No messages.</p>}
                <div className="max-h-64 overflow-y-auto">
                  {inbox.map(m => (
                    <button key={m.id} type="button" onClick={() => !m.readFlag && onMarkRead(m.id)}
                      className={`block w-full text-left p-3 mb-2 rounded-lg border text-sm ${m.readFlag ? 'bg-white border-gray-200' : 'bg-orange-50 border-orange-200'}`}>
                      <div className="flex justify-between text-xs text-gray-500 mb-1">
                        <span className="font-semibold text-gray-700">From: {m.sender}</span>
                        <span>{m.sentAt ? new Date(m.sentAt).toLocaleString('en-GB') : ''}</span>
                      </div>
                      <p className="text-gray-700 whitespace-pre-line">{m.content}</p>
                    </button>
                  ))}
                </div>
              </Card>
            )}

            <Card title="Login activity">
              <p className="text-sm font-bold text-gray-800">First access to site</p>
              <p className="text-sm text-gray-600 mb-3">{fmt(loginActivity?.firstAccess)}{ago(loginActivity?.firstAccess)}</p>
              <p className="text-sm font-bold text-gray-800">Last access to site</p>
              <p className="text-sm text-gray-600">{fmt(loginActivity?.lastAccess)}{ago(loginActivity?.lastAccess)}</p>
            </Card>

            {onChangePassword && (
              <Card title="Reset password">
                <input type="password" placeholder="Current password" value={pw.current}
                  onChange={e => setPw({ ...pw, current: e.target.value })} className={inputCls} />
                <input type="password" placeholder="New password (min 8 characters)" value={pw.next}
                  onChange={e => setPw({ ...pw, next: e.target.value })} className={inputCls} />
                <input type="password" placeholder="Confirm new password" value={pw.confirm}
                  onChange={e => setPw({ ...pw, confirm: e.target.value })} className={inputCls} />
                {pwMsg.text && (
                  <p className={`text-sm mb-2 ${pwMsg.type === 'success' ? 'text-green-600' : 'text-red-600'}`}>{pwMsg.text}</p>
                )}
                <button onClick={submitPassword} disabled={pwBusy}
                  className="px-4 py-2 rounded-lg bg-indigo-600 text-white text-sm font-semibold disabled:opacity-50">
                  {pwBusy ? 'Saving…' : 'Change password'}
                </button>
              </Card>
            )}
          </div>
        </div>
      </div>

      {msgOpen && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50" onClick={() => setMsgOpen(false)}>
          <div className="bg-white rounded-xl p-6 w-full max-w-md" onClick={e => e.stopPropagation()}>
            <h3 className="text-lg font-bold mb-3">Message to {name}</h3>
            <textarea value={text} onChange={e => setText(e.target.value)} rows={5}
              className="w-full border border-gray-300 rounded-lg p-3 text-sm" placeholder="Type your message…" />
            {note && <p className="text-sm text-red-600 mt-2">{note}</p>}
            <div className="flex justify-end gap-2 mt-4">
              <button onClick={() => setMsgOpen(false)} className="px-4 py-2 rounded-lg border border-gray-300 text-sm">Cancel</button>
              <button onClick={send} disabled={sending || !text.trim()}
                className="px-4 py-2 rounded-lg bg-indigo-600 text-white text-sm font-semibold disabled:opacity-50">
                {sending ? 'Sending…' : 'Send'}
              </button>
            </div>
          </div>
        </div>
      )}

      {modOpen && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50" onClick={() => setModOpen(false)}>
          <div className="bg-white rounded-xl p-6 w-full max-w-md" onClick={e => e.stopPropagation()}>
            <h3 className="text-lg font-bold mb-3">Add module</h3>
            <input value={modForm.moduleId} onChange={e => setModForm({ ...modForm, moduleId: e.target.value })}
              placeholder="Module ID (e.g. EC6304)" className="w-full border border-gray-300 rounded-lg p-3 text-sm mb-3" />
            <input value={modForm.moduleName} onChange={e => setModForm({ ...modForm, moduleName: e.target.value })}
              placeholder="Module name" className="w-full border border-gray-300 rounded-lg p-3 text-sm" />
            {modErr && <p className="text-sm text-red-600 mt-2">{modErr}</p>}
            <div className="flex justify-end gap-2 mt-4">
              <button onClick={() => setModOpen(false)} className="px-4 py-2 rounded-lg border border-gray-300 text-sm">Cancel</button>
              <button onClick={createModule} disabled={modBusy}
                className="px-4 py-2 rounded-lg bg-orange-500 text-white text-sm font-semibold disabled:opacity-50">
                {modBusy ? 'Saving…' : 'Create'}
              </button>
            </div>
          </div>
        </div>
      )}
      <Footer />
    </div>
  )
}
