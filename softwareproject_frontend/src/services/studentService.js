import axios from 'axios'

const BASE_URL = ''

export const studentService = {
  async uploadStudents({ file }, config = {}) {
    const formData = new FormData()
    formData.append('file', file)
    return axios.post(`${BASE_URL}/api/students/upload`, formData, { ...config, headers: config.headers || {} })
  },

  async downloadTemplate(config = {}) {
    return axios.get(`${BASE_URL}/api/students/template`, { ...config, responseType: 'blob' })
  },

  async create(student, config = {}) {
    return axios.post(`${BASE_URL}/api/students`, student, config)
  },

  async update(id, student, config = {}) {
    return axios.put(`${BASE_URL}/api/students`, student, { ...config, params: { id } })
  },

  async remove(id, config = {}) {
    return axios.delete(`${BASE_URL}/api/students`, { ...config, params: { id } })
  },

  async restore(id, config = {}) {
    return axios.post(`${BASE_URL}/api/students/restore`, null, { ...config, params: { id } })
  },

  async listDeleted(config = {}) {
    return axios.get(`${BASE_URL}/api/students`, { ...config, params: { deleted: true } })
  },

  async list({ batch, academicYear } = {}, config = {}) {
    const params = {}
    if (batch) params.batch = batch
    if (academicYear) params.academicYear = academicYear
    return axios.get(`${BASE_URL}/api/students`, { ...config, params })
  },
}

export default studentService
