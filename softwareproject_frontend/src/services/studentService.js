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

  async list({ batch, academicYear } = {}, config = {}) {
    const params = {}
    if (batch) params.batch = batch
    if (academicYear) params.academicYear = academicYear
    return axios.get(`${BASE_URL}/api/students`, { ...config, params })
  },
}

export default studentService
