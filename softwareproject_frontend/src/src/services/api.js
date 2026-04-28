/**
 * api.js — Complete frontend service layer
 * Mirrors every backend REST endpoint exactly.
 *
 * Base URL: http://localhost:8080
 * Auth: JWT Bearer token (stored by authService, injected by axiosSetup interceptor)
 *
 * Controllers covered:
 *   UserRestController      → /api/auth/...
 *   ModuleRestController    → /api/modules/...
 *   LosRestController       → /api/lospos/...
 *   ProgramOutcomeRestController → /api/program-outcomes/...
 *   LOPOMappingRestController    → /api/lo-po-mapping/...
 *   IntegratedLORestController   → /api/los-with-mapping/...
 *   OBEController           → /api/obe/...
 */

import axios from 'axios';

const BASE_URL = 'http://localhost:8080';

// ─────────────────────────────────────────────────────────────────────────────
// AUTH  /api/auth
// ─────────────────────────────────────────────────────────────────────────────
export const authAPI = {
  /**
   * Login — no token required.
   * POST /api/auth/login
   * Body: { username: string, password: string }
   * Returns: { token, userId, email, userType, message, status }
   */
  login(username, password) {
    return axios.post(`${BASE_URL}/api/auth/login`, {
      username,   // maps to User.getUserID() (field name: User_ID)
      password,
    });
  },

  /**
   * Create a test admin user — no token required (for dev/seeding).
   * POST /api/auth/create-test-user
   * Returns: { userId:"admin", email, userType:"admin", loginInfo, status }
   */
  createTestUser() {
    return axios.post(`${BASE_URL}/api/auth/create-test-user`);
  },

  /**
   * Add an admin user — superadmin token required.
   * POST /api/auth/add-admin
   * Body: { username, password, email, usertype: "admin" }
   */
  addAdmin(userData) {
    return axios.post(`${BASE_URL}/api/auth/add-admin`, {
      ...userData,
      usertype: 'admin',
    });
  },

  /**
   * Add a lecturer — admin/superadmin token required.
   * POST /api/auth/add-lecture
   * Body: { username, password, email, usertype: "lecture" }
   */
  addLecturer(userData) {
    return axios.post(`${BASE_URL}/api/auth/add-lecture`, {
      ...userData,
      usertype: 'lecture',
    });
  },

  /**
   * Generic add-user (admin or lecture) — role-checked server-side.
   * POST /api/auth/add-user
   * Body: { username, password, email, usertype: "admin"|"lecture" }
   */
  addUser(userData) {
    return axios.post(`${BASE_URL}/api/auth/add-user`, userData);
  },

  /**
   * Debug — fetch user info by username (no auth needed, dev only).
   * GET /api/auth/debug/user/:username
   */
  debugGetUser(username) {
    return axios.get(`${BASE_URL}/api/auth/debug/user/${username}`);
  },
};

// ─────────────────────────────────────────────────────────────────────────────
// MODULES  /api/modules
// ─────────────────────────────────────────────────────────────────────────────
export const modulesAPI = {
  /**
   * Create a module — admin only.
   * POST /api/modules/create
   * Body: { moduleId: string, moduleName: string }
   * Returns: { data: Module, moduleId, status }
   */
  createModule(moduleData) {
    return axios.post(`${BASE_URL}/api/modules/create`, moduleData);
  },

  /**
   * Get all modules — authenticated.
   * GET /api/modules/all
   * Returns: { data: Module[], status }
   */
  getAllModules() {
    return axios.get(`${BASE_URL}/api/modules/all`);
  },

  /**
   * Get one module by ID.
   * GET /api/modules/:id
   * Returns: { data: Module, moduleId, status }
   */
  getModuleById(moduleId) {
    return axios.get(`${BASE_URL}/api/modules/${moduleId}`);
  },

  /**
   * Update a module — admin only.
   * PUT /api/modules/:id
   * Body: { moduleId?, moduleName? }
   */
  updateModule(moduleId, moduleData) {
    return axios.put(`${BASE_URL}/api/modules/${moduleId}`, moduleData);
  },

  /**
   * Delete a module — admin only.
   * DELETE /api/modules/:id
   */
  deleteModule(moduleId) {
    return axios.delete(`${BASE_URL}/api/modules/${moduleId}`);
  },
};

// ─────────────────────────────────────────────────────────────────────────────
// LEARNING OUTCOMES (LOs)  /api/lospos
// ─────────────────────────────────────────────────────────────────────────────
export const losAPI = {
  /**
   * Add an LO to a module — lecturer/admin only.
   * POST /api/lospos/:moduleId/add
   * Body: { id, name, description, batch }
   * Returns: { data: Los, losId, name, description, status }
   */
  addLosToModule(moduleId, losData) {
    return axios.post(`${BASE_URL}/api/lospos/${moduleId}/add`, losData);
  },

  /**
   * Get all LOs for a module.
   * GET /api/lospos/module/:moduleId
   * Returns: { data: Los[], count, status }
   */
  getLosByModuleId(moduleId) {
    return axios.get(`${BASE_URL}/api/lospos/module/${moduleId}`);
  },

  /**
   * Get one LO by ID.
   * GET /api/lospos/:id
   * Returns: { data: Los, losId, name, description, status }
   */
  getLosById(loId) {
    return axios.get(`${BASE_URL}/api/lospos/${loId}`);
  },

  /**
   * Update an LO — lecturer/admin only.
   * PUT /api/lospos/:id
   * Body: { name?, description?, batch? }
   */
  updateLos(loId, losData) {
    return axios.put(`${BASE_URL}/api/lospos/${loId}`, losData);
  },

  /**
   * Delete an LO — lecturer/admin only.
   * DELETE /api/lospos/:id
   */
  deleteLos(loId) {
    return axios.delete(`${BASE_URL}/api/lospos/${loId}`);
  },

  // ── MARKS ────────────────────────────────────────────────────────────────

  /**
   * Import student marks from Excel for a specific LO + batch.
   * POST /api/lospos/:loId/marks/import-obe
   * Multipart: excelFile (.xlsx/.xls), batch (string), loNumber? (string)
   * Returns: { message, details, loId, batch, fileName, format, status }
   */
  importMarks(loId, excelFile, batch, loNumber = '') {
    const formData = new FormData();
    formData.append('excelFile', excelFile);
    formData.append('batch', batch);
    if (loNumber) formData.append('loNumber', loNumber);
    return axios.post(`${BASE_URL}/api/lospos/${loId}/marks/import-obe`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  /**
   * Get all marks for an LO (all batches combined, legacy view).
   * GET /api/lospos/:loId/marks
   * Returns: { data: [{ id, studentId, studentName, score }], count, status }
   */
  getMarksByLo(loId) {
    return axios.get(`${BASE_URL}/api/lospos/${loId}/marks`);
  },

  /**
   * Update a single student mark.
   * PUT /api/lospos/:loId/marks/:markId
   * Body: { score: number (0–100) }
   */
  updateMark(loId, markId, score) {
    return axios.put(`${BASE_URL}/api/lospos/${loId}/marks/${markId}`, { score });
  },

  /**
   * Delete a single student mark.
   * DELETE /api/lospos/:loId/marks/:markId
   */
  deleteMark(loId, markId) {
    return axios.delete(`${BASE_URL}/api/lospos/${loId}/marks/${markId}`);
  },

  // ── BATCHES ───────────────────────────────────────────────────────────────

  /**
   * Get all batches (with record counts) for an LO.
   * GET /api/lospos/:loId/batches
   * Returns: { data: [{ batch, batchLabel, recordCount }], count, status }
   */
  getBatchesByLo(loId) {
    return axios.get(`${BASE_URL}/api/lospos/${loId}/batches`);
  },

  /**
   * Get all marks for a specific batch within an LO.
   * GET /api/lospos/:loId/batches/:batch/marks
   * Returns: { data: [{ id, studentId, studentName, score, batch }], count, status }
   */
  getMarksByLoBatch(loId, batch) {
    return axios.get(`${BASE_URL}/api/lospos/${loId}/batches/${batch}/marks`);
  },

  /**
   * Update (rename) a batch number for an LO.
   * PUT /api/lospos/:loId/batch/update
   * Body: { oldBatch: string, newBatch: string }
   */
  updateBatch(loId, oldBatch, newBatch) {
    return axios.put(`${BASE_URL}/api/lospos/${loId}/batch/update`, { oldBatch, newBatch });
  },

  /**
   * Delete all marks for a specific batch — lecturer/admin only.
   * DELETE /api/lospos/:loId/batch/:batch   (legacy endpoint)
   */
  deleteBatchLegacy(loId, batch) {
    return axios.delete(`${BASE_URL}/api/lospos/${loId}/batch/${batch}`);
  },

  /**
   * Delete all marks for a specific batch — lecturer/admin only.
   * DELETE /api/lospos/:loId/batches/:batch  (primary endpoint)
   */
  deleteBatch(loId, batch) {
    return axios.delete(`${BASE_URL}/api/lospos/${loId}/batches/${batch}`);
  },

  // ── EXCEL EXPORT ──────────────────────────────────────────────────────────

  /**
   * Export LO results as Excel file (pass/fail analysis).
   * POST /api/lospos/export-excel
   * Body: {
   *   los: string[],           // LO title names
   *   students: [{ indexNo, marks: number[] }],
   *   passThreshold?: number   // default 50
   * }
   * Returns: binary .xlsx blob
   */
  exportExcel(losNames, students, passThreshold = 50) {
    return axios.post(
      `${BASE_URL}/api/lospos/export-excel`,
      { los: losNames, students, passThreshold },
      { responseType: 'blob' }
    );
  },
};

// ─────────────────────────────────────────────────────────────────────────────
// PROGRAM OUTCOMES (POs)  /api/program-outcomes
// ─────────────────────────────────────────────────────────────────────────────
export const programOutcomesAPI = {
  /**
   * Create a PO — admin only.
   * POST /api/program-outcomes/create
   * Body: { poId, code, title, description, category, performanceIndicators, isDefault? }
   */
  createPO(poData) {
    return axios.post(`${BASE_URL}/api/program-outcomes/create`, poData);
  },

  /**
   * Get all active POs — public/authenticated.
   * GET /api/program-outcomes/all
   * Returns: { data: ProgramOutcome[], status }
   */
  getAllActivePOs() {
    return axios.get(`${BASE_URL}/api/program-outcomes/all`);
  },

  /**
   * Get all POs including inactive — admin only.
   * GET /api/program-outcomes/all-including-inactive
   */
  getAllPOs() {
    return axios.get(`${BASE_URL}/api/program-outcomes/all-including-inactive`);
  },

  /**
   * Get a single PO by ID.
   * GET /api/program-outcomes/:poId
   */
  getPOById(poId) {
    return axios.get(`${BASE_URL}/api/program-outcomes/${poId}`);
  },

  /**
   * Get default Washington Accord POs (PO1–PO12).
   * GET /api/program-outcomes/defaults
   */
  getDefaultPOs() {
    return axios.get(`${BASE_URL}/api/program-outcomes/defaults`);
  },

  /**
   * Get custom (non-default) POs.
   * GET /api/program-outcomes/custom
   */
  getCustomPOs() {
    return axios.get(`${BASE_URL}/api/program-outcomes/custom`);
  },

  /**
   * Get POs filtered by category.
   * GET /api/program-outcomes/by-category/:category
   */
  getPOsByCategory(category) {
    return axios.get(`${BASE_URL}/api/program-outcomes/by-category/${category}`);
  },

  /**
   * Update a PO — admin only.
   * PUT /api/program-outcomes/:poId
   * Body: { title?, description?, category?, performanceIndicators?, isActive? }
   */
  updatePO(poId, poData) {
    return axios.put(`${BASE_URL}/api/program-outcomes/${poId}`, poData);
  },

  /**
   * Soft-delete (deactivate) a PO — admin only.
   * DELETE /api/program-outcomes/:poId
   */
  deletePO(poId) {
    return axios.delete(`${BASE_URL}/api/program-outcomes/${poId}`);
  },

  /**
   * Hard-delete a PO permanently — admin only.
   * DELETE /api/program-outcomes/:poId/permanent
   */
  hardDeletePO(poId) {
    return axios.delete(`${BASE_URL}/api/program-outcomes/${poId}/permanent`);
  },

  /**
   * Restore a soft-deleted PO — admin only.
   * PUT /api/program-outcomes/:poId/restore
   */
  restorePO(poId) {
    return axios.put(`${BASE_URL}/api/program-outcomes/${poId}/restore`);
  },

  /**
   * Initialize the 12 Washington Accord default POs — admin only.
   * POST /api/program-outcomes/initialize-defaults
   */
  initializeDefaults() {
    return axios.post(`${BASE_URL}/api/program-outcomes/initialize-defaults`);
  },

  /**
   * Reorder POs — admin only.
   * PUT /api/program-outcomes/reorder
   * Body: string[]  (ordered list of poIds)
   */
  reorderPOs(orderedPoIds) {
    return axios.put(`${BASE_URL}/api/program-outcomes/reorder`, orderedPoIds);
  },

  /**
   * Health check for the PO subsystem.
   * GET /api/program-outcomes/health
   */
  healthCheck() {
    return axios.get(`${BASE_URL}/api/program-outcomes/health`);
  },
};

// ─────────────────────────────────────────────────────────────────────────────
// LO-PO MAPPINGS  /api/lo-po-mapping
// ─────────────────────────────────────────────────────────────────────────────
export const mappingsAPI = {
  /**
   * Get all LO-PO mappings with optional filters — lecturer/admin.
   * GET /api/lo-po-mapping/all
   * Params: moduleId?, status?, batch?, search?
   */
  getAllMappings({ moduleId, status, batch, search } = {}) {
    return axios.get(`${BASE_URL}/api/lo-po-mapping/all`, {
      params: { moduleId, status, batch, search },
    });
  },

  /**
   * Get global mapping statistics — lecturer/admin.
   * GET /api/lo-po-mapping/statistics
   * Returns: { data: { totalMappings, pendingMappings, approvedMappings, coveragePercentage } }
   */
  getStatistics() {
    return axios.get(`${BASE_URL}/api/lo-po-mapping/statistics`);
  },

  /**
   * Get mapping statistics for a specific module — lecturer/admin.
   * GET /api/lo-po-mapping/statistics/:moduleId
   */
  getStatisticsByModule(moduleId) {
    return axios.get(`${BASE_URL}/api/lo-po-mapping/statistics/${moduleId}`);
  },

  /**
   * Get mapping suggestions for an LO (based on module type / LO description).
   * GET /api/lo-po-mapping/suggestions?moduleId=&loDescription=
   */
  getSuggestions(moduleId, loDescription = '') {
    return axios.get(`${BASE_URL}/api/lo-po-mapping/suggestions`, {
      params: { moduleId, loDescription },
    });
  },

  /**
   * Create mappings for an LO — lecturer/admin.
   * POST /api/lo-po-mapping/create?loId=
   * Body: { mappings: { [poId]: weight (0–3) }, remarks?: string }
   *
   * Weight scale: 0 = No correlation, 1 = Low, 2 = Medium, 3 = High
   */
  createMappings(loId, mappings, remarks = '') {
    return axios.post(
      `${BASE_URL}/api/lo-po-mapping/create`,
      { mappings, remarks },
      { params: { loId } }
    );
  },

  /**
   * Get all mappings for a specific LO — lecturer/admin.
   * GET /api/lo-po-mapping/lo/:loId
   */
  getMappingsForLO(loId) {
    return axios.get(`${BASE_URL}/api/lo-po-mapping/lo/${loId}`);
  },

  /**
   * Get all mappings for a specific module — lecturer/admin.
   * GET /api/lo-po-mapping/module/:moduleId
   */
  getMappingsForModule(moduleId) {
    return axios.get(`${BASE_URL}/api/lo-po-mapping/module/${moduleId}`);
  },

  /**
   * Update a mapping (weight / remarks) if not yet approved — lecturer.
   * PUT /api/lo-po-mapping/:mappingId
   * Body: { weight: number (0–3), lecturerRemarks?: string }
   */
  updateMapping(mappingId, weight, lecturerRemarks = '') {
    return axios.put(`${BASE_URL}/api/lo-po-mapping/${mappingId}`, {
      weight,
      lecturerRemarks,
    });
  },

  /**
   * Delete a mapping if not yet approved — lecturer.
   * DELETE /api/lo-po-mapping/:mappingId
   */
  deleteMapping(mappingId) {
    return axios.delete(`${BASE_URL}/api/lo-po-mapping/${mappingId}`);
  },

  /**
   * Get all pending mappings awaiting admin review — admin only.
   * GET /api/lo-po-mapping/admin/pending
   */
  getPendingMappings() {
    return axios.get(`${BASE_URL}/api/lo-po-mapping/admin/pending`);
  },

  /**
   * Approve a single mapping — admin only.
   * PUT /api/lo-po-mapping/admin/:mappingId/approve
   * Body: { adminRemarks?: string }
   */
  approveMapping(mappingId, adminRemarks = '') {
    return axios.put(`${BASE_URL}/api/lo-po-mapping/admin/${mappingId}/approve`, {
      adminRemarks,
    });
  },

  /**
   * Reject a mapping — admin only. adminRemarks is required.
   * PUT /api/lo-po-mapping/admin/:mappingId/reject
   * Body: { adminRemarks: string }
   */
  rejectMapping(mappingId, adminRemarks) {
    return axios.put(`${BASE_URL}/api/lo-po-mapping/admin/${mappingId}/reject`, {
      adminRemarks,
    });
  },

  /**
   * Bulk approve all pending mappings for an LO — admin only.
   * PUT /api/lo-po-mapping/admin/lo/:loId/approve-all
   * Body: { adminRemarks?: string }
   */
  bulkApproveMappingsForLO(loId, adminRemarks = 'Bulk approved') {
    return axios.put(`${BASE_URL}/api/lo-po-mapping/admin/lo/${loId}/approve-all`, {
      adminRemarks,
    });
  },

  /**
   * Get comprehensive mapping report for a module — admin only.
   * GET /api/lo-po-mapping/admin/report/:moduleId
   */
  getModuleReport(moduleId) {
    return axios.get(`${BASE_URL}/api/lo-po-mapping/admin/report/${moduleId}`);
  },

  /**
   * Get all available POs for the mapping interface (no auth required).
   * GET /api/lo-po-mapping/program-outcomes
   */
  getProgramOutcomes() {
    return axios.get(`${BASE_URL}/api/lo-po-mapping/program-outcomes`);
  },

  /**
   * Health check for the mapping subsystem.
   * GET /api/lo-po-mapping/health
   */
  healthCheck() {
    return axios.get(`${BASE_URL}/api/lo-po-mapping/health`);
  },
};

// ─────────────────────────────────────────────────────────────────────────────
// INTEGRATED LO + MAPPING  /api/los-with-mapping
// ─────────────────────────────────────────────────────────────────────────────
export const integratedLOAPI = {
  /**
   * Create an LO and its PO mappings in one transaction — lecturer/admin.
   * POST /api/los-with-mapping/create
   * Body: {
   *   moduleId: string,
   *   learningOutcome: { id, name, description, batch },
   *   mappings: { [poId]: weight },
   *   mappingRemarks?: string
   * }
   * Returns: { data: { learningOutcome, mappings, mappingCount }, status }
   */
  createLOWithMappings(moduleId, learningOutcome, mappings, mappingRemarks = '') {
    return axios.post(`${BASE_URL}/api/los-with-mapping/create`, {
      moduleId,
      learningOutcome,
      mappings,
      mappingRemarks,
    });
  },

  /**
   * Get form data (suggested mappings + all POs) for LO creation — lecturer.
   * GET /api/los-with-mapping/form-data/:moduleId
   * Returns: { data: { suggestedMappings, allProgramOutcomes, moduleId }, status }
   */
  getCreateFormData(moduleId) {
    return axios.get(`${BASE_URL}/api/los-with-mapping/form-data/${moduleId}`);
  },

  /**
   * Get comprehensive LO details with its mappings and statistics.
   * GET /api/los-with-mapping/:loId/details
   * Returns: { data: { learningOutcome, mappings, statistics }, status }
   */
  getLOWithMappings(loId) {
    return axios.get(`${BASE_URL}/api/los-with-mapping/${loId}/details`);
  },

  /**
   * Update all mappings for an LO (replaces pending/rejected, cannot touch approved).
   * PUT /api/los-with-mapping/:loId/mappings
   * Body: { mappings: { [poId]: weight }, mappingRemarks?: string }
   */
  updateLOMappings(loId, mappings, mappingRemarks = 'Updated mappings') {
    return axios.put(`${BASE_URL}/api/los-with-mapping/${loId}/mappings`, {
      mappings,
      mappingRemarks,
    });
  },

  /**
   * Get module overview: all LOs with mapping status and statistics.
   * GET /api/los-with-mapping/module/:moduleId/overview
   * Returns: { data: { moduleId, learningOutcomes, totalLOs, mappingStatistics, detailedReport }, status }
   */
  getModuleOverview(moduleId) {
    return axios.get(`${BASE_URL}/api/los-with-mapping/module/${moduleId}/overview`);
  },
};

// ─────────────────────────────────────────────────────────────────────────────
// OBE CONTROLLER  /api/obe
// ─────────────────────────────────────────────────────────────────────────────
export const obeAPI = {
  // ── PROGRAM OUTCOMES (admin-gated alternative endpoints) ─────────────────

  /**
   * Create PO — admin only.
   * POST /api/obe/po/create
   * Body: ProgramOutcome fields
   */
  createPO(poData) {
    return axios.post(`${BASE_URL}/api/obe/po/create`, poData);
  },

  /**
   * Get all POs — admin only.
   * GET /api/obe/po/all
   */
  getAllPOs() {
    return axios.get(`${BASE_URL}/api/obe/po/all`);
  },

  /**
   * Get one PO by ID — admin only.
   * GET /api/obe/po/:poId
   */
  getPOById(poId) {
    return axios.get(`${BASE_URL}/api/obe/po/${poId}`);
  },

  /**
   * Update PO description — admin only.
   * PUT /api/obe/po/:poId
   * Body: { description }
   */
  updatePO(poId, poData) {
    return axios.put(`${BASE_URL}/api/obe/po/${poId}`, poData);
  },

  /**
   * Delete PO — admin only.
   * DELETE /api/obe/po/:poId
   */
  deletePO(poId) {
    return axios.delete(`${BASE_URL}/api/obe/po/${poId}`);
  },

  // ── MAPPINGS ─────────────────────────────────────────────────────────────

  /**
   * Bulk-save LO-PO mappings as PENDING — lecturer only.
   * POST /api/obe/mappings/bulk-save
   * Body: OutcomeMapping[]  — each must have learningOutcome.id and programOutcome.id
   */
  bulkSaveMappings(mappings) {
    return axios.post(`${BASE_URL}/api/obe/mappings/bulk-save`, mappings);
  },

  /**
   * Approve a mapping — admin only.
   * PUT /api/obe/admin/approve-mapping/:id
   */
  approveMapping(mappingId) {
    return axios.put(`${BASE_URL}/api/obe/admin/approve-mapping/${mappingId}`);
  },

  // ── MARKS ────────────────────────────────────────────────────────────────

  /**
   * Upload marks Excel file for an LO (simple format) — lecturer only.
   * POST /api/obe/marks/upload/:losId
   * Multipart: file
   */
  uploadMarks(losId, file) {
    const formData = new FormData();
    formData.append('file', file);
    return axios.post(`${BASE_URL}/api/obe/marks/upload/${losId}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  /**
   * Bulk upload marks for multiple LOs in one Excel file — lecturer only.
   * POST /api/obe/marks/upload-bulk
   * Multipart: excelFile, losIds (comma-separated), batch, markType?
   * markType: "FINAL_EXAM" | "ASSIGNMENT" (default: "FINAL_EXAM")
   */
  uploadMarksBulk(excelFile, losIds, batch, markType = 'FINAL_EXAM') {
    const formData = new FormData();
    formData.append('excelFile', excelFile);
    formData.append('losIds', Array.isArray(losIds) ? losIds.join(',') : losIds);
    formData.append('batch', batch);
    formData.append('markType', markType);
    return axios.post(`${BASE_URL}/api/obe/marks/upload-bulk`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  // ── REPORTS & ANALYSIS ────────────────────────────────────────────────────

  /**
   * Get PO attainment report for a module (flat JSON for charts) — lecturer.
   * GET /api/obe/reports/course/:moduleId
   * Returns: { [poId]: attainmentPercentage }
   */
  getCourseReport(moduleId) {
    return axios.get(`${BASE_URL}/api/obe/reports/course/${moduleId}`);
  },

  /**
   * Get module trend analysis across batches — lecturer.
   * GET /api/obe/analysis/trend/:moduleId
   */
  getModuleTrend(moduleId) {
    return axios.get(`${BASE_URL}/api/obe/analysis/trend/${moduleId}`);
  },

  /**
   * Get LO-level trend analysis for a module — lecturer.
   * GET /api/obe/analysis/trend/lo/:moduleId
   */
  getLOTrend(moduleId) {
    return axios.get(`${BASE_URL}/api/obe/analysis/trend/lo/${moduleId}`);
  },

  // ── EXCEL EXPORT & TEMPLATES ──────────────────────────────────────────────

  /**
   * Export marks report as Excel — lecturer.
   * POST /api/obe/export/marks
   * Body: { losIds: string[], markType: "FINAL_EXAM"|"ASSIGNMENT", batch: string, threshold?: number }
   * Returns: binary .xlsx blob
   */
  exportMarks(losIds, markType, batch, threshold = 50) {
    return axios.post(
      `${BASE_URL}/api/obe/export/marks`,
      { losIds, markType, batch, threshold },
      { responseType: 'blob' }
    );
  },

  /**
   * Generate empty Excel template for mark entry — lecturer.
   * POST /api/obe/template/marks
   * Body: { losIds: string[], markType: string, batch: string }
   * Returns: binary .xlsx blob
   */
  generateMarkTemplate(losIds, markType, batch) {
    return axios.post(
      `${BASE_URL}/api/obe/template/marks`,
      { losIds, markType, batch },
      { responseType: 'blob' }
    );
  },
};

// ─────────────────────────────────────────────────────────────────────────────
// HELPER UTILITIES
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Download a blob response as a file.
 * Usage: downloadBlob(response.data, 'report.xlsx')
 */
export function downloadBlob(blob, filename) {
  const url = window.URL.createObjectURL(new Blob([blob]));
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', filename);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}

/**
 * Mapping weight labels (backend scale: 0–3)
 */
export const WEIGHT_LABELS = {
  0: 'No correlation',
  1: 'Low',
  2: 'Medium',
  3: 'High',
};

/**
 * Mark types supported by the backend
 */
export const MARK_TYPES = {
  FINAL_EXAM: 'FINAL_EXAM',
  ASSIGNMENT: 'ASSIGNMENT',
};

/**
 * User roles
 */
export const USER_ROLES = {
  SUPERADMIN: 'superadmin',
  ADMIN: 'admin',
  LECTURE: 'lecture',
};

/**
 * Mapping approval statuses
 */
export const MAPPING_STATUS = {
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
};
