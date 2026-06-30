import axios from 'axios'

const BASE_URL = 'http://localhost:8080'

export const cqiService = {
  async finalize({ moduleId, batch }, config = {}) {
    return axios.post(`${BASE_URL}/api/cqi/finalize/${moduleId}`, null, { ...config, params: { batch } })
  },

  async getMyPlans(config = {}) {
    return axios.get(`${BASE_URL}/api/cqi/my-plans`, config)
  },

  async submitPlan(id, dto, config = {}) {
    return axios.post(`${BASE_URL}/api/cqi/${id}/submit`, dto, config)
  },

  async getPending(config = {}) {
    return axios.get(`${BASE_URL}/api/cqi/pending`, config)
  },

  async approvePlan(id, config = {}) {
    return axios.put(`${BASE_URL}/api/cqi/${id}/approve`, null, config)
  },

  async returnPlan(id, comment, config = {}) {
    return axios.put(`${BASE_URL}/api/cqi/${id}/return`, { comment }, config)
  },

  async getModuleHistory(moduleId, config = {}) {
    return axios.get(`${BASE_URL}/api/cqi/module/${moduleId}/history`, config)
  },
}

export default cqiService
