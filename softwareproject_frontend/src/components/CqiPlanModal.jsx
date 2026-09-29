import { useState } from 'react'
import axios from 'axios'
import authService from '../services/authService'
import './cqiPlans.css'

export default function CqiPlanModal({ batch, po, currentAttainment, onClose, onSuccess }) {
  const [plannedActions, setPlannedActions] = useState('')
  const [targetAttainment, setTargetAttainment] = useState(currentAttainment ? Math.min(currentAttainment + 10, 100) : 80)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      const response = await axios.post('/api/cqi/po/create', {
        poId: po.poId,
        batch,
        currentAttainment,
        targetAttainment: parseFloat(targetAttainment),
        plannedActions,
      }, {
        headers: { Authorization: `Bearer ${authService.getToken()}` }
      })

      if (response.data.status === 'SUCCESS') {
        onSuccess?.(response.data.data)
        onClose()
      }
    } catch (e) {
      setError(e.response?.data?.message || 'Failed to create CQI plan')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>Create CQI Plan for {po.code}</h2>
          <button className="modal-close" onClick={onClose}>&times;</button>
        </div>

        <form onSubmit={handleSubmit} className="cqi-form">
          {error && <div className="alert alert-error">{error}</div>}

          <div className="form-group">
            <label>Program Outcome</label>
            <div className="form-display">
              <strong>{po.code}</strong> — {po.title}
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label>Current Attainment (%)</label>
              <input type="text" value={currentAttainment?.toFixed(2) || '—'} disabled className="form-control-disabled" />
            </div>
            <div className="form-group">
              <label>Target Attainment (%)</label>
              <input
                type="number"
                min="0"
                max="100"
                step="0.1"
                value={targetAttainment}
                onChange={(e) => setTargetAttainment(e.target.value)}
                required
              />
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="planned-actions">Planned Actions</label>
            <textarea
              id="planned-actions"
              rows="6"
              value={plannedActions}
              onChange={(e) => setPlannedActions(e.target.value)}
              placeholder="Describe the improvement actions, timeline, and responsible parties..."
              required
            />
          </div>

          <div className="form-actions">
            <button type="button" onClick={onClose} disabled={loading} className="btn btn-secondary">
              Cancel
            </button>
            <button type="submit" disabled={loading} className="btn btn-primary">
              {loading ? 'Creating…' : 'Create Plan'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
