import { useState, useEffect } from 'react'
import axios from 'axios'
import authService from '../services/authService'
import './cqiPlans.css'

export default function CqiPlansDisplay({ batch, onRefresh }) {
  const [allPlans, setAllPlans] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [updating, setUpdating] = useState(null)

  useEffect(() => {
    if (batch) loadPlans()
  }, [batch])

  const loadPlans = async () => {
    setLoading(true)
    setError('')
    try {
      const response = await axios.get(`/api/cqi/batch/${batch}`, {
        headers: { Authorization: `Bearer ${authService.getToken()}` }
      })
      if (response.data.status === 'SUCCESS') {
        setAllPlans(response.data.data || [])
      }
    } catch (e) {
      setError(e.response?.data?.message || 'Failed to load CQI plans')
    } finally {
      setLoading(false)
    }
  }

  // Separate PO and LO plans
  // Check both losId and los (backend might return different field names)
  const poPlans = allPlans.filter(plan => {
    const hasLo = plan.losId || plan.los
    const hasPo = plan.poId || plan.po
    return !hasLo && hasPo
  })
  const loPlans = allPlans.filter(plan => {
    return plan.losId || plan.los
  })

  const updateStatus = async (planId, newStatus) => {
    setUpdating(planId)
    try {
      const response = await axios.put(`/api/cqi/${planId}/status`,
        { status: newStatus },
        { headers: { Authorization: `Bearer ${authService.getToken()}` } }
      )
      if (response.data.status === 'SUCCESS') {
        await loadPlans()
        onRefresh?.()
      }
    } catch (e) {
      setError(e.response?.data?.message || 'Failed to update plan status')
    } finally {
      setUpdating(null)
    }
  }

  const statusBadge = (status) => {
    const classes = {
      'IN_PROGRESS': 'badge-in-progress',
      'COMPLETED': 'badge-completed',
      'PLANNED': 'badge-planned',
      'CLOSED': 'badge-closed'
    }
    return classes[status] || 'badge-default'
  }

  if (loading) return <div className="cqi-plans-loading">Loading CQI plans…</div>
  if (error) return <div className="alert alert-error">{error}</div>
  if (!poPlans.length && !loPlans.length) return <div className="cqi-plans-empty">No CQI plans created yet for this batch.</div>

  return (
    <div className="cqi-plans-container">
      {/* PO CQI Plans Section */}
      <div className="cqi-section cqi-po-section">
        <div className="section-header">
          <h3>🎯 CQI Plans for Batch {batch}</h3>
          <span className="section-badge">PROGRAM OUTCOMES ONLY</span>
        </div>
        {poPlans.length === 0 ? (
          <div className="cqi-plans-empty">No PO CQI plans created yet.</div>
        ) : (
          <div className="cqi-plans-grid">
            {poPlans.map(plan => (
              <div key={plan.id} className="cqi-plan-card">
                <div className="plan-header">
                  <div className="plan-title">
                    <h4>{plan.poCode || 'CQI Plan'}</h4>
                    <span className={`badge ${statusBadge(plan.status)}`}>{plan.status}</span>
                  </div>
                  <small className="plan-date">{new Date(plan.createdAt).toLocaleDateString()}</small>
                </div>

                <div className="plan-metrics">
                  {plan.attainmentScore !== null && (
                    <div className="metric">
                      <span className="label">Current:</span>
                      <span className="value">{plan.attainmentScore.toFixed(2)}%</span>
                    </div>
                  )}
                  {plan.targetAttainment !== null && (
                    <div className="metric">
                      <span className="label">Target:</span>
                      <span className="value">{plan.targetAttainment.toFixed(2)}%</span>
                    </div>
                  )}
                </div>

                {plan.actionPlan && (
                  <div className="plan-actions">
                    <p className="actions-label">Planned Actions:</p>
                    <p className="actions-text">{plan.actionPlan}</p>
                  </div>
                )}

                <div className="plan-footer">
                  <small>Created by: {plan.createdBy}</small>
                </div>

                {plan.status !== 'COMPLETED' && plan.status !== 'CLOSED' && (
                  <div className="plan-actions-buttons">
                    {plan.status === 'IN_PROGRESS' && (
                      <button
                        className="btn-small btn-success"
                        onClick={() => updateStatus(plan.id, 'COMPLETED')}
                        disabled={updating === plan.id}
                      >
                        Mark Completed
                      </button>
                    )}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* LO CQI Review History Section */}
      {loPlans.length > 0 && (
        <div className="cqi-section cqi-history">
          <div className="section-header">
            <h3>📋 CQI Review History</h3>
            <span className="section-badge section-badge-history">LEARNING OUTCOMES - READ ONLY</span>
          </div>
          <p className="cqi-history-note">Historical CQI records for Learning Outcomes - read-only audit trail for reference</p>
          <div className="cqi-plans-grid">
            {loPlans.map(plan => (
              <div key={plan.id} className="cqi-plan-card cqi-history-card">
                <div className="plan-header">
                  <div className="plan-title">
                    <h4>LO: {plan.losId}</h4>
                    <span className={`badge ${statusBadge(plan.status)}`}>{plan.status}</span>
                  </div>
                  <small className="plan-date">{new Date(plan.createdAt).toLocaleDateString()}</small>
                </div>

                <div className="plan-metrics">
                  {plan.attainmentScore !== null && (
                    <div className="metric">
                      <span className="label">Current:</span>
                      <span className="value">{plan.attainmentScore.toFixed(2)}%</span>
                    </div>
                  )}
                  {plan.targetAttainment !== null && (
                    <div className="metric">
                      <span className="label">Target:</span>
                      <span className="value">{plan.targetAttainment.toFixed(2)}%</span>
                    </div>
                  )}
                </div>

                {plan.actionPlan && (
                  <div className="plan-actions">
                    <p className="actions-label">Actions:</p>
                    <p className="actions-text">{plan.actionPlan}</p>
                  </div>
                )}

                <div className="plan-footer">
                  <small>Created by: {plan.createdBy}</small>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
