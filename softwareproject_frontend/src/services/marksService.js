import axios from 'axios'

const BASE_URL = 'http://localhost:8080'

export const marksService = {
  async getModule(moduleId, config = {}) {
    return axios.get(`${BASE_URL}/api/modules/${moduleId}`, config)
  },

  async getModuleLos(moduleId, config = {}) {
    return axios.get(`${BASE_URL}/api/lospos/module/${moduleId}`, config)
  },

  async downloadTemplate({ losIds, markType, batch }, config = {}) {
    return axios.post(
      `${BASE_URL}/api/obe/template/marks`,
      { losIds, markType, batch },
      { ...config, responseType: 'blob' }
    )
  },

  async exportMarks({ losIds, markType, batch, threshold }, config = {}) {
    return axios.post(
      `${BASE_URL}/api/obe/export/marks`,
      { losIds, markType, batch, threshold },
      { ...config, responseType: 'blob' }
    )
  },

  async uploadBulk({ excelFile, losIds, batch, markType }, config = {}) {
    const formData = new FormData()
    formData.append('excelFile', excelFile)
    formData.append('losIds', Array.isArray(losIds) ? losIds.join(',') : losIds)
    formData.append('batch', batch)
    formData.append('markType', markType)

    return axios.post(`${BASE_URL}/api/obe/marks/upload-bulk`, formData, {
      ...config,
      headers: config.headers || {},
    })
  },

  async uploadQuestionWise({ excelFile, templateId, batch, markType }, config = {}) {
    const formData = new FormData()
    formData.append('excelFile', excelFile)
    formData.append('templateId', templateId)
    formData.append('batch', batch)
    formData.append('markType', markType)

    return axios.post(`${BASE_URL}/api/obe/marks/upload-question-wise`, formData, {
      ...config,
      headers: config.headers || {},
    })
  },

  async getPOAttainment({ losIds, markType, batch, threshold }, config = {}) {
    return axios.post(
      `${BASE_URL}/api/obe/po-attainment`,
      { losIds, markType, batch, threshold },
      config
    )
  },

  async exportPOAttainment({ losIds, markType, batch, threshold }, config = {}) {
    return axios.post(
      `${BASE_URL}/api/obe/export/po-attainment`,
      { losIds, markType, batch, threshold },
      { ...config, responseType: 'blob' }
    )
  },

  async downloadQuestionMarkTemplate({ templateId, numberOfQuestions, questionMappings }, config = {}) {
    return axios.post(
      `${BASE_URL}/api/obe/template/marks-question-wise?templateId=${templateId || ''}`,
      {
        numberOfQuestions,
        questionMappings,
      },
      { ...config, responseType: 'blob' }
    )
  },

  async exportMarksWithPerLoThreshold({ losIds, markType, batch, loThresholds }, config = {}) {
    return axios.post(
      `${BASE_URL}/api/obe/export/marks-per-lo-threshold`,
      { losIds, markType, batch, loThresholds },
      { ...config, responseType: 'blob' }
    )
  },
}

  async listTemplatesByModule(moduleId, config = {}) {
    return axios.get(`${BASE_URL}/api/obe/assessment/templates/${moduleId}`, config)
  },

  async getTemplate(templateId, config = {}) {
    return axios.get(`${BASE_URL}/api/obe/assessment/template/${templateId}`, config)
  },
}

export default marksService