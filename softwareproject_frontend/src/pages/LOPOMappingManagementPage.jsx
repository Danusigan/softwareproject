import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import Header from '../components/header';
import Footer from '../components/footer';
import axios from 'axios';

export default function LOPOMappingManagementPage() {
    const navigate = useNavigate();

    const normalizeRole = (role) => {
        if (!role) return '';
        return String(role).toUpperCase().replace(/[-\s]/g, '');
    };
    
    // State management
    const [mappings, setMappings] = useState([]);
    const [modules, setModules] = useState([]);
    const [stats, setStats] = useState({});
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    
    // Filter state
    const [filters, setFilters] = useState({
        moduleId: '',
        status: '',
        batch: '',
        search: ''
    });
    
    // View mode
    const [viewMode, setViewMode] = useState('list'); // 'list', 'grid', 'matrix'
    
    // Modal state
    const [selectedMapping, setSelectedMapping] = useState(null);
    const [showApprovalModal, setShowApprovalModal] = useState(false);
    const [approvalForm, setApprovalForm] = useState({
        status: '',
        remarks: ''
    });
    const [selectedEditableMapping, setSelectedEditableMapping] = useState(null);
    const [showEditMappingModal, setShowEditMappingModal] = useState(false);
    const [editMappingForm, setEditMappingForm] = useState({
        weight: 1,
        lecturerRemarks: ''
    });

    // User role
    const [userRole, setUserRole] = useState('');

    useEffect(() => {
        // Get user role from persisted login first, then token payload fallback.
        const storedRole = localStorage.getItem('userType');
        if (storedRole) {
            setUserRole(normalizeRole(storedRole));
        } else {
            const token = localStorage.getItem('token');
            if (token) {
                try {
                    const payload = JSON.parse(atob(token.split('.')[1]));
                    const tokenRole = payload.role || payload.userType || payload.usertype || 'LECTURER';
                    setUserRole(normalizeRole(tokenRole));
                } catch (err) {
                    console.warn('Could not parse token:', err);
                    setUserRole('LECTURER');
                }
            }
        }
        
        fetchData();
    }, []);

    useEffect(() => {
        if (!loading) {
            fetchMappings();
        }
    }, [filters]);

    const fetchData = async () => {
        try {
            setLoading(true);
            const token = localStorage.getItem('token');

            // Keep partial successes if one endpoint fails.
            const [mappingsRes, modulesRes, statsRes] = await Promise.allSettled([
                axios.get('http://localhost:8080/api/lo-po-mapping/all', {
                    headers: { 'Authorization': `Bearer ${token}` }
                }),
                axios.get('http://localhost:8080/api/modules/all', {
                    headers: { 'Authorization': `Bearer ${token}` }
                }),
                axios.get('http://localhost:8080/api/lo-po-mapping/statistics', {
                    headers: { 'Authorization': `Bearer ${token}` }
                })
            ]);

            if (mappingsRes.status === 'fulfilled' && mappingsRes.value.data.status === 'SUCCESS') {
                setMappings(normalizeMappings(mappingsRes.value.data.data));
            }

            if (modulesRes.status === 'fulfilled' && modulesRes.value.data.status === 'SUCCESS') {
                setModules(modulesRes.value.data.data || []);
            }

            if (statsRes.status === 'fulfilled' && statsRes.value.data.status === 'SUCCESS') {
                setStats(statsRes.value.data.data || {});
            }

            const allFailed = [mappingsRes, modulesRes, statsRes].every(res => res.status === 'rejected');
            if (allFailed) {
                const firstFailure = [mappingsRes, modulesRes, statsRes].find(res => res.status === 'rejected');
                const failureMessage = firstFailure?.reason?.response?.data?.message || firstFailure?.reason?.message;
                setError(failureMessage || 'Failed to load LO-PO mappings');
            }

        } catch (err) {
            console.error('Error fetching data:', err);
            setError('Failed to load LO-PO mappings');
        } finally {
            setLoading(false);
        }
    };

    const fetchMappings = async () => {
        try {
            const token = localStorage.getItem('token');
            let url = 'http://localhost:8080/api/lo-po-mapping/all';
            
            // Add filters
            const params = new URLSearchParams();
            if (filters.moduleId) params.append('moduleId', filters.moduleId);
            if (filters.status) params.append('status', filters.status);
            if (filters.batch) params.append('batch', filters.batch);
            if (filters.search) params.append('search', filters.search);
            
            if (params.toString()) {
                url += `?${params.toString()}`;
            }

            const response = await axios.get(url, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (response.data.status === 'SUCCESS') {
                setMappings(normalizeMappings(response.data.data));
            }
        } catch (err) {
            console.error('Error fetching mappings:', err);
        }
    };

    const handleApproveMapping = async (mappingId, action) => {
        if (userRole !== 'ADMIN' && userRole !== 'SUPERADMIN') {
            setError('Only admins can approve mappings');
            return;
        }

        const currentMapping = mappings.find(m => m.id === mappingId);
        setSelectedMapping(currentMapping);
        setApprovalForm({
            status: action,
            remarks: ''
        });
        setShowApprovalModal(true);
    };

    const submitApproval = async () => {
        const normalizedAction = normalizeRole(approvalForm.status);
        if (normalizedAction === 'REJECTED' && !approvalForm.remarks.trim()) {
            setError('Rejection remarks are required so lecturers can fix the mapping.');
            return;
        }

        try {
            const token = localStorage.getItem('token');

            const actionEndpoint = normalizedAction === 'APPROVED' ? 'approve' : 'reject';
            const mappingIdentifier = selectedMapping.mappingId || selectedMapping.id;

            await axios.put(
                `http://localhost:8080/api/lo-po-mapping/admin/${mappingIdentifier}/${actionEndpoint}`,
                { adminRemarks: approvalForm.remarks },
                {
                    headers: { 'Authorization': `Bearer ${token}` }
                }
            );

            setSuccess(`Mapping ${approvalForm.status.toLowerCase()} successfully`);
            setShowApprovalModal(false);
            setSelectedMapping(null);
            setApprovalForm({ status: '', remarks: '' });
            fetchMappings(); // Refresh mappings
            
        } catch (err) {
            console.error('Error updating mapping approval:', err);
            setError(err.response?.data?.message || 'Failed to update mapping approval');
        }
    };

    const handleEditRejectedMapping = (mapping) => {
        setSelectedEditableMapping(mapping);
        setEditMappingForm({
            weight: mapping.correlationWeight || 1,
            lecturerRemarks: mapping.lecturerRemarks || ''
        });
        setShowEditMappingModal(true);
    };

    const submitRejectedMappingEdit = async () => {
        try {
            const token = localStorage.getItem('token');
            const mappingIdentifier = selectedEditableMapping.mappingId || selectedEditableMapping.id;

            await axios.put(
                `http://localhost:8080/api/lo-po-mapping/${mappingIdentifier}`,
                {
                    weight: Number(editMappingForm.weight),
                    lecturerRemarks: editMappingForm.lecturerRemarks
                },
                {
                    headers: { 'Authorization': `Bearer ${token}` }
                }
            );

            setSuccess('Rejected mapping updated successfully');
            setShowEditMappingModal(false);
            setSelectedEditableMapping(null);
            setEditMappingForm({ weight: 1, lecturerRemarks: '' });
            fetchMappings();
            fetchData();
        } catch (err) {
            console.error('Error updating rejected mapping:', err);
            setError(err.response?.data?.message || 'Failed to update rejected mapping');
        }
    };

    const handleDeleteRejectedMapping = async (mapping) => {
        const mappingIdentifier = mapping.mappingId || mapping.id;
        if (!window.confirm('Delete this rejected LO-PO mapping?')) return;

        try {
            const token = localStorage.getItem('token');
            await axios.delete(`http://localhost:8080/api/lo-po-mapping/${mappingIdentifier}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            setSuccess('Rejected mapping deleted successfully');
            fetchMappings();
            fetchData();
        } catch (err) {
            console.error('Error deleting rejected mapping:', err);
            setError(err.response?.data?.message || 'Failed to delete rejected mapping');
        }
    };

    const getStatusBadge = (status) => {
        const badges = {
            'PENDING': 'bg-yellow-100 text-yellow-800 border-yellow-200',
            'APPROVED': 'bg-green-100 text-green-800 border-green-200',
            'REJECTED': 'bg-red-100 text-red-800 border-red-200'
        };
        
        return badges[status] || 'bg-gray-100 text-gray-800 border-gray-200';
    };

    const getWeightBadge = (weight) => {
        const badges = {
            1: 'bg-yellow-100 text-yellow-800',
            2: 'bg-orange-100 text-orange-800',
            3: 'bg-red-100 text-red-800'
        };
        
        return badges[weight] || 'bg-gray-100 text-gray-800';
    };

    const getWeightLabel = (weight) => {
        const labels = {
            1: 'Low',
            2: 'Medium',
            3: 'High'
        };
        
        return labels[weight] || 'None';
    };

    const normalizeMappings = (rawMappings) => {
        return (rawMappings || []).map((mapping) => ({
            ...mapping,
            mappingId: mapping?.mappingId ?? mapping?.id,
            learningOutcome: mapping?.learningOutcome || {},
            programOutcome: mapping?.programOutcome || {},
            approvalStatus: mapping?.approvalStatus || mapping?.status || 'PENDING',
            correlationWeight: mapping?.correlationWeight ?? mapping?.weight ?? 0
        }));
    };

    const getAdminFeedback = (mapping) => {
        return mapping?.adminRemarks || mapping?.remarks || mapping?.reviewComment || '';
    };

    const formatCoverage = (value) => {
        const numeric = Number(value || 0);
        if (Number.isNaN(numeric)) return '0';
        return numeric.toFixed(1).replace(/\.0$/, '');
    };

    // Group mappings by LO for matrix view
    const groupedMappings = mappings.reduce((acc, mapping) => {
        const loId = mapping.learningOutcome?.id || mapping.learningOutcomeId || 'UNKNOWN-LO';
        const loName = mapping.learningOutcome?.name || 'Unknown Learning Outcome';
        const key = `${loId}-${loName}`;
        if (!acc[key]) {
            acc[key] = {
                lo: {
                    id: loId,
                    name: loName,
                    description: mapping.learningOutcome?.description || ''
                },
                mappings: []
            };
        }
        acc[key].mappings.push(mapping);
        return acc;
    }, {});

    const isAdminUser = userRole === 'ADMIN' || userRole === 'SUPERADMIN';
    const canEditRejected = !isAdminUser;

    if (loading) {
        return (
            <div className="min-h-screen bg-[#f8fafc] flex flex-col">
                <Header />
                <main className="flex-1 flex items-center justify-center">
                    <div className="glass-card p-8 rounded-2xl">
                        <div className="animate-spin rounded-full h-12 w-12 border-4 border-indigo-500 border-t-transparent mx-auto mb-4"></div>
                        <p className="text-slate-600 font-medium">Loading LO-PO mappings...</p>
                    </div>
                </main>
                <Footer />
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-[#f8fafc] flex flex-col relative overflow-hidden">
            {/* Background decorations */}
            <div className="absolute top-0 right-0 w-[500px] h-[500px] bg-indigo-500/5 rounded-full blur-[120px] pointer-events-none" />
            <div className="absolute bottom-0 left-0 w-[500px] h-[500px] bg-emerald-500/5 rounded-full blur-[120px] pointer-events-none" />

            <Header />

            <main className="flex-1 max-w-7xl mx-auto px-6 py-12 w-full relative z-10">
                {/* Header */}
                <div className="flex justify-between items-center mb-8">
                    <div>
                        <h1 className="heading-xl bg-clip-text text-transparent bg-gradient-to-r from-slate-900 to-slate-600">
                            LO-PO Mapping Management
                        </h1>
                        <p className="text-slate-500 font-medium mt-2">
                            Manage Learning Outcome to Program Outcome mappings and approvals
                        </p>
                    </div>
                    
                    <div className="flex gap-3">
                        <Link 
                            to="/modules" 
                            className="btn-secondary"
                        >
                            <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 6h16M4 12h16M4 18h16" />
                            </svg>
                            View Modules
                        </Link>
                    </div>
                </div>

                {/* Messages */}
                {error && (
                    <div className="glass-card rounded-xl p-4 mb-6 bg-red-50 border border-red-200">
                        <div className="flex items-center">
                            <svg className="w-5 h-5 text-red-600 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                            <p className="text-red-800 font-medium">{error}</p>
                        </div>
                    </div>
                )}

                {success && (
                    <div className="glass-card rounded-xl p-4 mb-6 bg-green-50 border border-green-200">
                        <div className="flex items-center">
                            <svg className="w-5 h-5 text-green-600 mr-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                            </svg>
                            <p className="text-green-800 font-medium">{success}</p>
                        </div>
                    </div>
                )}

                {/* Statistics Cards */}
                <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
                    <div className="glass-card rounded-2xl p-6">
                        <div className="flex items-center justify-between">
                            <div>
                                <p className="text-sm font-medium text-slate-600">Total Mappings</p>
                                <p className="text-2xl font-bold text-slate-900 mt-1">{stats.totalMappings || 0}</p>
                            </div>
                            <div className="p-3 bg-indigo-100 rounded-xl">
                                <svg className="w-6 h-6 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                                </svg>
                            </div>
                        </div>
                    </div>

                    <div className="glass-card rounded-2xl p-6">
                        <div className="flex items-center justify-between">
                            <div>
                                <p className="text-sm font-medium text-slate-600">Pending Approval</p>
                                <p className="text-2xl font-bold text-yellow-600 mt-1">{stats.pendingMappings || 0}</p>
                            </div>
                            <div className="p-3 bg-yellow-100 rounded-xl">
                                <svg className="w-6 h-6 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                                </svg>
                            </div>
                        </div>
                    </div>

                    <div className="glass-card rounded-2xl p-6">
                        <div className="flex items-center justify-between">
                            <div>
                                <p className="text-sm font-medium text-slate-600">Approved</p>
                                <p className="text-2xl font-bold text-green-600 mt-1">{stats.approvedMappings || 0}</p>
                            </div>
                            <div className="p-3 bg-green-100 rounded-xl">
                                <svg className="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                                </svg>
                            </div>
                        </div>
                    </div>

                    <div className="glass-card rounded-2xl p-6">
                        <div className="flex items-center justify-between">
                            <div>
                                <p className="text-sm font-medium text-slate-600">Coverage</p>
                                <p className="text-2xl font-bold text-indigo-600 mt-1">
                                    {formatCoverage(stats.coveragePercentage)}%
                                </p>
                            </div>
                            <div className="p-3 bg-indigo-100 rounded-xl">
                                <svg className="w-6 h-6 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M11 3.055A9.001 9.001 0 1020.945 13H11V3.055z" />
                                </svg>
                            </div>
                        </div>
                    </div>
                </div>

                {/* Filters and View Controls */}
                <div className="glass-card rounded-2xl p-6 mb-8">
                    <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
                        {/* Filters */}
                        <div className="flex flex-col sm:flex-row gap-4 flex-1">
                            <select
                                value={filters.moduleId}
                                onChange={(e) => setFilters(prev => ({ ...prev, moduleId: e.target.value }))}
                                className="input-field"
                            >
                                <option value="">All Modules</option>
                                {modules.map(module => (
                                    <option key={module.moduleId} value={module.moduleId}>
                                        {module.moduleName}
                                    </option>
                                ))}
                            </select>

                            <select
                                value={filters.status}
                                onChange={(e) => setFilters(prev => ({ ...prev, status: e.target.value }))}
                                className="input-field"
                            >
                                <option value="">All Status</option>
                                <option value="PENDING">Pending</option>
                                <option value="APPROVED">Approved</option>
                                <option value="REJECTED">Rejected</option>
                            </select>

                            <input
                                type="text"
                                placeholder="Search LO name..."
                                value={filters.search}
                                onChange={(e) => setFilters(prev => ({ ...prev, search: e.target.value }))}
                                className="input-field"
                            />
                        </div>

                        {/* View Controls */}
                        <div className="flex gap-2">
                            <button
                                onClick={() => setViewMode('list')}
                                className={`px-3 py-2 rounded-lg font-medium transition-colors ${
                                    viewMode === 'list' 
                                        ? 'bg-indigo-100 text-indigo-700' 
                                        : 'text-slate-600 hover:bg-slate-100'
                                }`}
                            >
                                List
                            </button>
                            <button
                                onClick={() => setViewMode('grid')}
                                className={`px-3 py-2 rounded-lg font-medium transition-colors ${
                                    viewMode === 'grid' 
                                        ? 'bg-indigo-100 text-indigo-700' 
                                        : 'text-slate-600 hover:bg-slate-100'
                                }`}
                            >
                                Grid
                            </button>
                            <button
                                onClick={() => setViewMode('matrix')}
                                className={`px-3 py-2 rounded-lg font-medium transition-colors ${
                                    viewMode === 'matrix' 
                                        ? 'bg-indigo-100 text-indigo-700' 
                                        : 'text-slate-600 hover:bg-slate-100'
                                }`}
                            >
                                Matrix
                            </button>
                        </div>
                    </div>
                </div>

                {/* Content based on view mode */}
                {viewMode === 'list' && (
                    <div className="glass-card rounded-2xl overflow-hidden">
                        <div className="overflow-x-auto">
                            <table className="w-full">
                                <thead className="bg-slate-50 border-b border-slate-200">
                                    <tr>
                                        <th className="text-left py-4 px-6 font-semibold text-slate-700">Learning Outcome</th>
                                        <th className="text-left py-4 px-6 font-semibold text-slate-700">Module</th>
                                        <th className="text-left py-4 px-6 font-semibold text-slate-700">Program Outcome</th>
                                        <th className="text-left py-4 px-6 font-semibold text-slate-700">Weight</th>
                                        <th className="text-left py-4 px-6 font-semibold text-slate-700">Status</th>
                                        <th className="text-left py-4 px-6 font-semibold text-slate-700">Created</th>
                                        {(isAdminUser || canEditRejected) && (
                                            <th className="text-left py-4 px-6 font-semibold text-slate-700">Actions</th>
                                        )}
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-slate-200">
                                    {mappings.map((mapping) => (
                                        <tr key={mapping.mappingId} className="hover:bg-slate-50">
                                            <td className="py-4 px-6">
                                                <div>
                                                    <p className="font-semibold text-slate-900">
                                                        {mapping.learningOutcome?.id || mapping.learningOutcomeId || 'N/A'}
                                                    </p>
                                                    <p className="text-sm text-slate-600">
                                                        {mapping.learningOutcome?.name || 'Unknown Learning Outcome'}
                                                    </p>
                                                </div>
                                            </td>
                                            <td className="py-4 px-6">
                                                <span className="text-slate-700">
                                                    {mapping.module?.moduleName || 'N/A'}
                                                </span>
                                            </td>
                                            <td className="py-4 px-6">
                                                <div>
                                                    <p className="font-medium text-slate-900">
                                                        {mapping.programOutcome?.code || mapping.programOutcomeCode || 'N/A'}
                                                    </p>
                                                    <p className="text-sm text-slate-600">
                                                        {mapping.programOutcome?.title || 'Unknown Program Outcome'}
                                                    </p>
                                                </div>
                                            </td>
                                            <td className="py-4 px-6">
                                                <span className={`px-3 py-1 rounded-full text-sm font-medium ${getWeightBadge(mapping.correlationWeight)}`}>
                                                    {mapping.correlationWeight} - {getWeightLabel(mapping.correlationWeight)}
                                                </span>
                                            </td>
                                            <td className="py-4 px-6">
                                                <span className={`px-3 py-1 rounded-full text-sm font-medium border ${getStatusBadge(mapping.approvalStatus)}`}>
                                                    {mapping.approvalStatus}
                                                </span>
                                                {mapping.approvalStatus === 'REJECTED' && getAdminFeedback(mapping) && (
                                                    <p className="mt-2 text-xs text-red-700 max-w-xs leading-relaxed">
                                                        Admin feedback: {getAdminFeedback(mapping)}
                                                    </p>
                                                )}
                                            </td>
                                            <td className="py-4 px-6 text-sm text-slate-600">
                                                        {new Date(mapping.mappedAt || mapping.createdDate || mapping.updatedAt || Date.now()).toLocaleDateString()}
                                            </td>
                                            {isAdminUser && mapping.approvalStatus === 'PENDING' && (
                                                <td className="py-4 px-6">
                                                    <div className="flex gap-2">
                                                        <button
                                                            onClick={() => handleApproveMapping(mapping.mappingId, 'APPROVED')}
                                                            className="text-green-600 hover:text-green-700 font-medium text-sm"
                                                        >
                                                            Approve
                                                        </button>
                                                        <button
                                                            onClick={() => handleApproveMapping(mapping.mappingId, 'REJECTED')}
                                                            className="text-red-600 hover:text-red-700 font-medium text-sm"
                                                        >
                                                            Reject
                                                        </button>
                                                    </div>
                                                </td>
                                            )}
                                            {!isAdminUser && mapping.approvalStatus === 'REJECTED' && (
                                                <td className="py-4 px-6">
                                                    <div className="flex gap-2">
                                                        <button
                                                            onClick={() => handleEditRejectedMapping(mapping)}
                                                            className="text-indigo-600 hover:text-indigo-700 font-medium text-sm"
                                                        >
                                                            Edit
                                                        </button>
                                                        <button
                                                            onClick={() => handleDeleteRejectedMapping(mapping)}
                                                            className="text-red-600 hover:text-red-700 font-medium text-sm"
                                                        >
                                                            Delete
                                                        </button>
                                                    </div>
                                                </td>
                                            )}
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    </div>
                )}

                {viewMode === 'grid' && (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                        {mappings.map((mapping) => (
                            <div key={mapping.mappingId} className="glass-card rounded-2xl p-6">
                                <div className="flex justify-between items-start mb-4">
                                    <div className="flex-1">
                                        <h3 className="font-bold text-slate-900">{mapping.learningOutcome?.id || mapping.learningOutcomeId || 'N/A'}</h3>
                                        <p className="text-sm text-slate-600 mt-1">{mapping.learningOutcome?.name || 'Unknown Learning Outcome'}</p>
                                    </div>
                                    <span className={`px-2 py-1 rounded-full text-xs font-medium border ${getStatusBadge(mapping.approvalStatus)}`}>
                                        {mapping.approvalStatus}
                                    </span>
                                </div>

                                <div className="space-y-3">
                                    <div className="flex justify-between">
                                        <span className="text-sm text-slate-600">Module:</span>
                                        <span className="text-sm font-medium">{modules.find(m => m.moduleId === mapping.learningOutcome?.moduleId)?.moduleName || 'N/A'}</span>
                                    </div>

                                    <div className="flex justify-between">
                                        <span className="text-sm text-slate-600">PO:</span>
                                        <span className="text-sm font-medium">{mapping.programOutcome?.code || mapping.programOutcomeCode || 'N/A'}</span>
                                    </div>

                                    <div className="flex justify-between">
                                        <span className="text-sm text-slate-600">Weight:</span>
                                        <span className={`px-2 py-1 rounded-full text-xs font-medium ${getWeightBadge(mapping.correlationWeight)}`}>
                                            {mapping.correlationWeight}
                                        </span>
                                    </div>

                                    <div className="flex justify-between">
                                        <span className="text-sm text-slate-600">Created:</span>
                                        <span className="text-sm">{new Date(mapping.mappedAt || mapping.createdDate || mapping.updatedAt || Date.now()).toLocaleDateString()}</span>
                                    </div>

                                    {mapping.approvalStatus === 'REJECTED' && getAdminFeedback(mapping) && (
                                        <div className="rounded-lg border border-red-200 bg-red-50 px-3 py-2">
                                            <p className="text-xs font-semibold text-red-700 mb-1">Admin feedback</p>
                                            <p className="text-xs text-red-700 leading-relaxed">{getAdminFeedback(mapping)}</p>
                                        </div>
                                    )}
                                </div>

                                {isAdminUser && mapping.approvalStatus === 'PENDING' && (
                                    <div className="flex gap-2 mt-4 pt-4 border-t border-slate-200">
                                        <button
                                            onClick={() => handleApproveMapping(mapping.mappingId, 'APPROVED')}
                                            className="btn-secondary text-green-600 hover:text-green-700 text-sm flex-1"
                                        >
                                            Approve
                                        </button>
                                        <button
                                            onClick={() => handleApproveMapping(mapping.mappingId, 'REJECTED')}
                                            className="btn-secondary text-red-600 hover:text-red-700 text-sm flex-1"
                                        >
                                            Reject
                                        </button>
                                    </div>
                                )}

                                {!isAdminUser && mapping.approvalStatus === 'REJECTED' && (
                                    <div className="flex gap-2 mt-4 pt-4 border-t border-slate-200">
                                        <button
                                            onClick={() => handleEditRejectedMapping(mapping)}
                                            className="btn-secondary text-indigo-600 hover:text-indigo-700 text-sm flex-1"
                                        >
                                            Edit
                                        </button>
                                        <button
                                            onClick={() => handleDeleteRejectedMapping(mapping)}
                                            className="btn-secondary text-red-600 hover:text-red-700 text-sm flex-1"
                                        >
                                            Delete
                                        </button>
                                    </div>
                                )}
                            </div>
                        ))}
                    </div>
                )}

                {viewMode === 'matrix' && (
                    <div className="space-y-8">
                        {Object.entries(groupedMappings).map(([key, group]) => (
                            <div key={key} className="glass-card rounded-2xl p-6">
                                <div className="flex justify-between items-center mb-6">
                                    <div>
                                        <h3 className="text-xl font-bold text-slate-900">{group.lo.id}</h3>
                                        <p className="text-slate-600 mt-1">{group.lo.name}</p>
                                        <p className="text-sm text-slate-500 mt-1 line-clamp-2">{group.lo.description}</p>
                                    </div>
                                    <span className="text-sm text-slate-500">
                                        {group.mappings.length} PO mappings
                                    </span>
                                </div>

                                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
                                    {group.mappings.map((mapping) => (
                                        <div 
                                            key={mapping.mappingId}
                                            className="border border-slate-200 rounded-xl p-4 hover:border-indigo-200 transition-colors"
                                        >
                                            <div className="flex justify-between items-start mb-2">
                                                <h4 className="font-semibold text-slate-900">{mapping.programOutcome?.code || mapping.programOutcomeCode || 'N/A'}</h4>
                                                <span className={`px-2 py-1 rounded-full text-xs font-medium ${getWeightBadge(mapping.correlationWeight)}`}>
                                                    {mapping.correlationWeight}
                                                </span>
                                            </div>
                                            <p className="text-sm text-slate-600 mb-3 line-clamp-2">{mapping.programOutcome?.title || 'Unknown Program Outcome'}</p>
                                            
                                            <div className="flex justify-between items-center">
                                                <span className={`px-2 py-1 rounded-full text-xs font-medium border ${getStatusBadge(mapping.approvalStatus)}`}>
                                                    {mapping.approvalStatus}
                                                </span>
                                                
                                                {isAdminUser && mapping.approvalStatus === 'PENDING' && (
                                                    <div className="flex gap-1">
                                                        <button
                                                            onClick={() => handleApproveMapping(mapping.mappingId, 'APPROVED')}
                                                            className="text-green-600 hover:text-green-700 text-xs"
                                                            title="Approve"
                                                        >
                                                            ✓
                                                        </button>
                                                        <button
                                                            onClick={() => handleApproveMapping(mapping.mappingId, 'REJECTED')}
                                                            className="text-red-600 hover:text-red-700 text-xs"
                                                            title="Reject"
                                                        >
                                                            ✗
                                                        </button>
                                                    </div>
                                                )}

                                                {!isAdminUser && mapping.approvalStatus === 'REJECTED' && (
                                                    <div className="flex gap-2">
                                                        <button
                                                            onClick={() => handleEditRejectedMapping(mapping)}
                                                            className="text-indigo-600 hover:text-indigo-700 text-xs"
                                                            title="Edit rejected mapping"
                                                        >
                                                            Edit
                                                        </button>
                                                        <button
                                                            onClick={() => handleDeleteRejectedMapping(mapping)}
                                                            className="text-red-600 hover:text-red-700 text-xs"
                                                            title="Delete rejected mapping"
                                                        >
                                                            Delete
                                                        </button>
                                                    </div>
                                                )}
                                            </div>

                                            {mapping.approvalStatus === 'REJECTED' && getAdminFeedback(mapping) && (
                                                <div className="mt-3 rounded-lg border border-red-200 bg-red-50 px-3 py-2">
                                                    <p className="text-[11px] font-semibold text-red-700 mb-1">Admin feedback</p>
                                                    <p className="text-[11px] text-red-700 leading-relaxed">{getAdminFeedback(mapping)}</p>
                                                </div>
                                            )}
                                        </div>
                                    ))}
                                </div>
                            </div>
                        ))}
                    </div>
                )}

                {mappings.length === 0 && (
                    <div className="glass-card rounded-2xl p-12 text-center">
                        <svg className="w-16 h-16 text-slate-300 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                        </svg>
                        <h3 className="text-xl font-semibold text-slate-900 mb-2">No LO-PO mappings found</h3>
                        <p className="text-slate-600 mb-6">Start by creating Learning Outcomes with Program Outcome mappings.</p>
                        <Link to="/modules" className="btn-primary">
                            Go to Modules
                        </Link>
                    </div>
                )}
            </main>

            {/* Approval Modal */}
            {showApprovalModal && selectedMapping && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
                    <div className="glass-card rounded-2xl p-8 w-full max-w-md">
                        <h3 className="text-xl font-bold text-slate-900 mb-6">
                            {approvalForm.status} Mapping
                        </h3>
                        
                        <div className="space-y-4 mb-6">
                            <div>
                                <span className="text-sm text-slate-600">Learning Outcome:</span>
                                <p className="font-medium">{selectedMapping.learningOutcome?.id || selectedMapping.learningOutcomeId || 'N/A'} - {selectedMapping.learningOutcome?.name || 'Unknown Learning Outcome'}</p>
                            </div>
                            <div>
                                <span className="text-sm text-slate-600">Program Outcome:</span>
                                <p className="font-medium">{selectedMapping.programOutcome?.code || selectedMapping.programOutcomeCode || 'N/A'} - {selectedMapping.programOutcome?.title || 'Unknown Program Outcome'}</p>
                            </div>
                            <div>
                                <span className="text-sm text-slate-600">Mapping Weight:</span>
                                <p className="font-medium">{selectedMapping.correlationWeight} - {getWeightLabel(selectedMapping.correlationWeight)}</p>
                            </div>
                        </div>

                        <div className="mb-6">
                            <label className="block text-sm font-semibold text-slate-700 mb-2">
                                {approvalForm.status === 'APPROVED' ? 'Approval' : 'Rejection'} Remarks
                            </label>
                            <textarea
                                className="input-field"
                                rows="3"
                                value={approvalForm.remarks}
                                onChange={(e) => setApprovalForm(prev => ({ ...prev, remarks: e.target.value }))}
                                placeholder={`Provide reason for ${approvalForm.status.toLowerCase()}...`}
                            />
                        </div>

                        <div className="flex gap-3">
                            <button
                                onClick={submitApproval}
                                className={`btn-primary flex-1 ${
                                    approvalForm.status === 'APPROVED' ? 'bg-green-600 hover:bg-green-700' : 'bg-red-600 hover:bg-red-700'
                                }`}
                            >
                                {approvalForm.status === 'APPROVED' ? 'Approve' : 'Reject'} Mapping
                            </button>
                            <button
                                onClick={() => {
                                    setShowApprovalModal(false);
                                    setSelectedMapping(null);
                                    setApprovalForm({ status: '', remarks: '' });
                                }}
                                className="btn-secondary flex-1"
                            >
                                Cancel
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {showEditMappingModal && selectedEditableMapping && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
                    <div className="glass-card rounded-2xl p-8 w-full max-w-md">
                        <h3 className="text-xl font-bold text-slate-900 mb-6">Edit Rejected Mapping</h3>

                        <div className="space-y-4 mb-6">
                            <div>
                                <span className="text-sm text-slate-600">Learning Outcome:</span>
                                <p className="font-medium">{selectedEditableMapping.learningOutcome?.id || selectedEditableMapping.learningOutcomeId || 'N/A'} - {selectedEditableMapping.learningOutcome?.name || 'Unknown Learning Outcome'}</p>
                            </div>
                            <div>
                                <span className="text-sm text-slate-600">Program Outcome:</span>
                                <p className="font-medium">{selectedEditableMapping.programOutcome?.code || selectedEditableMapping.programOutcomeCode || 'N/A'} - {selectedEditableMapping.programOutcome?.title || 'Unknown Program Outcome'}</p>
                            </div>
                            {getAdminFeedback(selectedEditableMapping) && (
                                <div className="rounded-lg border border-red-200 bg-red-50 p-3">
                                    <p className="text-xs font-semibold text-red-700 mb-1">Admin feedback</p>
                                    <p className="text-xs text-red-700">{getAdminFeedback(selectedEditableMapping)}</p>
                                </div>
                            )}
                        </div>

                        <div className="mb-4">
                            <label className="block text-sm font-semibold text-slate-700 mb-2">Correlation Weight</label>
                            <select
                                className="input-field"
                                value={editMappingForm.weight}
                                onChange={(e) => setEditMappingForm(prev => ({ ...prev, weight: e.target.value }))}
                            >
                                <option value={1}>1 - Low</option>
                                <option value={2}>2 - Medium</option>
                                <option value={3}>3 - High</option>
                            </select>
                        </div>

                        <div className="mb-6">
                            <label className="block text-sm font-semibold text-slate-700 mb-2">Lecturer remarks (optional)</label>
                            <textarea
                                className="input-field"
                                rows="3"
                                value={editMappingForm.lecturerRemarks}
                                onChange={(e) => setEditMappingForm(prev => ({ ...prev, lecturerRemarks: e.target.value }))}
                                placeholder="Add notes for admin review"
                            />
                        </div>

                        <div className="flex gap-3">
                            <button
                                onClick={submitRejectedMappingEdit}
                                className="btn-primary flex-1"
                            >
                                Save Changes
                            </button>
                            <button
                                onClick={() => {
                                    setShowEditMappingModal(false);
                                    setSelectedEditableMapping(null);
                                    setEditMappingForm({ weight: 1, lecturerRemarks: '' });
                                }}
                                className="btn-secondary flex-1"
                            >
                                Cancel
                            </button>
                        </div>
                    </div>
                </div>
            )}

            <Footer />
        </div>
    );
}