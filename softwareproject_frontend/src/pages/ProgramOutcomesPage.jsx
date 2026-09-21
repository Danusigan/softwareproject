import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../components/header';
import Footer from '../components/footer';
import axios from 'axios';

export default function ProgramOutcomesPage() {
    const [pos, setPOs] = useState([]);
    const [filteredPOs, setFilteredPOs] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedPO, setSelectedPO] = useState(null);
    const [showModal, setShowModal] = useState(false);
    const [modalType, setModalType] = useState('view'); // 'view', 'edit', 'create', 'delete'
    const [showInactive, setShowInactive] = useState(false);
    const [filterCategory, setFilterCategory] = useState('all');
    const [searchTerm, setSearchTerm] = useState('');
    const navigate = useNavigate();

    const categories = [
        'all',
        'Knowledge & Understanding',
        'Intellectual Skills',
        'Practical Skills',
        'Professional Skills',
        'Attitudes & Values',
        'Interpersonal Skills',
        'Communication Skills',
        'Management Skills',
        'Learning Skills'
    ];

    const [formData, setFormData] = useState({
        poId: '',
        title: '',
        description: '',
        category: '',
        performanceIndicators: '',
        displayOrder: ''
    });

    // Verify admin access on mount
    useEffect(() => {
        const userType = localStorage.getItem('userType');
        const normalizedType = userType?.toLowerCase?.() || '';
        const isAdmin = normalizedType === 'admin' || normalizedType === 'superadmin';

        if (!isAdmin) {
            navigate('/');
            return;
        }

        fetchPOs();
    }, [navigate]);

    // Filter POs when filters change
    useEffect(() => {
        filterPOs();
    }, [pos, showInactive, filterCategory, searchTerm]);

    const fetchPOs = async () => {
        try {
            setLoading(true);
            const token = localStorage.getItem('token');
            const response = await axios.get('/api/program-outcomes/all-including-inactive', {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            
            if (response.data.status === 'SUCCESS') {
                setPOs(response.data.data || []);
            }
        } catch (err) {
            console.error('Error fetching POs:', err);
            setPOs([]);
        } finally {
            setLoading(false);
        }
    };

    const filterPOs = () => {
        let filtered = pos;

        // Filter by active status
        if (!showInactive) {
            filtered = filtered.filter(po => po.isActive);
        }

        // Filter by category
        if (filterCategory !== 'all') {
            filtered = filtered.filter(po => po.category === filterCategory);
        }

        // Filter by search term
        if (searchTerm) {
            const term = searchTerm.toLowerCase();
            filtered = filtered.filter(po => 
                po.poId?.toLowerCase().includes(term) ||
                po.code?.toLowerCase().includes(term) ||
                po.title?.toLowerCase().includes(term) ||
                po.description?.toLowerCase().includes(term)
            );
        }

        setFilteredPOs(filtered);
    };

    const handleCreateClick = () => {
        setSelectedPO(null);
        setFormData({
            poId: '',
            title: '',
            description: '',
            category: categories[1], // Default to first category
            performanceIndicators: '',
            displayOrder: (pos.length + 1).toString()
        });
        setModalType('create');
        setShowModal(true);
    };

    const handleEditClick = (po) => {
        setSelectedPO(po);
        setFormData({
            poId: po.poId,
            title: po.title,
            description: po.description,
            category: po.category,
            performanceIndicators: po.performanceIndicators,
            displayOrder: po.displayOrder?.toString() || ''
        });
        setModalType('edit');
        setShowModal(true);
    };

    const handleViewClick = (po) => {
        setSelectedPO(po);
        setModalType('view');
        setShowModal(true);
    };

    const handleDeleteClick = (po) => {
        setSelectedPO(po);
        setModalType('delete');
        setShowModal(true);
    };

    const handleFormSubmit = async (e) => {
        e.preventDefault();
        
        try {
            const token = localStorage.getItem('token');
            const normalizedPoId = formData.poId?.trim().toUpperCase();
            const submitData = {
                ...formData,
                poId: normalizedPoId,
                displayOrder: parseInt(formData.displayOrder) || (pos.length + 1)
            };

            let response;
            if (modalType === 'create') {
                response = await axios.post('/api/program-outcomes/create', submitData, {
                    headers: { 'Authorization': `Bearer ${token}` }
                });
            } else if (modalType === 'edit') {
                response = await axios.put(`/api/program-outcomes/${normalizedPoId || selectedPO.poId}`, submitData, {
                    headers: { 'Authorization': `Bearer ${token}` }
                });
            }

            if (response.data.status === 'SUCCESS') {
                await fetchPOs();
                setShowModal(false);
                alert(response.data.message);
            }
        } catch (err) {
            console.error('Error saving PO:', err);
            alert(err.response?.data?.message || 'Error saving Program Outcome');
        }
    };

    const handleDelete = async () => {
        try {
            const token = localStorage.getItem('token');
            const response = await axios.delete(`/api/program-outcomes/${selectedPO.poId}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (response.data.status === 'SUCCESS') {
                await fetchPOs();
                setShowModal(false);
                alert(response.data.message);
            }
        } catch (err) {
            console.error('Error deleting PO:', err);
            alert(err.response?.data?.message || 'Error deleting Program Outcome');
        }
    };

    const handleRestore = async (po) => {
        try {
            const token = localStorage.getItem('token');
            const response = await axios.put(`/api/program-outcomes/${po.poId}/restore`, {}, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (response.data.status === 'SUCCESS') {
                await fetchPOs();
                alert(response.data.message);
            }
        } catch (err) {
            console.error('Error restoring PO:', err);
            alert(err.response?.data?.message || 'Error restoring Program Outcome');
        }
    };

    const initializeDefaults = async () => {
        try {
            const token = localStorage.getItem('token');
            const response = await axios.post('/api/program-outcomes/initialize-defaults', {}, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (response.data.status === 'SUCCESS') {
                await fetchPOs();
                alert('Default Washington Accord Program Outcomes initialized successfully!');
            }
        } catch (err) {
            console.error('Error initializing defaults:', err);
            alert(err.response?.data?.message || 'Error initializing default POs');
        }
    };

    const getPOCategoryColor = (category) => {
        const colors = {
            'Knowledge & Understanding': 'bg-blue-100 text-blue-800',
            'Intellectual Skills': 'bg-purple-100 text-purple-800',
            'Practical Skills': 'bg-green-100 text-green-800',
            'Professional Skills': 'bg-orange-100 text-orange-800',
            'Attitudes & Values': 'bg-red-100 text-red-800',
            'Interpersonal Skills': 'bg-yellow-100 text-yellow-800',
            'Communication Skills': 'bg-indigo-100 text-indigo-800',
            'Management Skills': 'bg-pink-100 text-pink-800',
            'Learning Skills': 'bg-gray-100 text-gray-800'
        };
        return colors[category] || 'bg-gray-100 text-gray-800';
    };

    if (loading) {
        return (
            <div className="min-h-screen bg-[#f8fafc] flex flex-col">
                <Header />
                <main className="flex-1 flex items-center justify-center">
                    <div className="glass-card p-8 rounded-2xl">
                        <div className="animate-spin rounded-full h-12 w-12 border-4 border-indigo-500 border-t-transparent mx-auto mb-4"></div>
                        <p className="text-slate-600 font-medium">Loading Program Outcomes...</p>
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

            <main className="flex-1 max-w-7xl mx-auto px-6 py-12 w-full relative z-10 animate-in fade-in duration-700">
                {/* Header Section */}
                <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-6 mb-8">
                    <div className="space-y-2">
                        <h1 className="heading-xl bg-clip-text text-transparent bg-gradient-to-r from-slate-900 to-slate-600">
                            Program Outcomes Management
                        </h1>
                        <p className="text-slate-500 font-medium">
                            Manage Washington Accord Program Outcomes and custom institutional outcomes
                        </p>
                    </div>
                    
                    <div className="flex gap-3">
                        <button
                            onClick={initializeDefaults}
                            className="btn-secondary flex items-center gap-2"
                        >
                            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                            </svg>
                            Initialize Defaults
                        </button>
                        <button 
                            onClick={handleCreateClick}
                            className="btn-primary flex items-center gap-2"
                        >
                            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                            </svg>
                            Create New PO
                        </button>
                    </div>
                </div>

                {/* Filters */}
                <div className="glass-card rounded-2xl p-6 mb-8">
                    <div className="flex flex-col lg:flex-row gap-4">
                        {/* Search */}
                        <div className="flex-1">
                            <label className="block text-sm font-semibold text-slate-700 mb-2">Search</label>
                            <div className="relative">
                                <input
                                    type="text"
                                    placeholder="Search by code, title, or description..."
                                    className="input-field pl-10 w-full"
                                    value={searchTerm}
                                    onChange={(e) => setSearchTerm(e.target.value)}
                                />
                                <svg className="w-5 h-5 text-slate-400 absolute left-3 top-1/2 transform -translate-y-1/2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                                </svg>
                            </div>
                        </div>

                        {/* Category Filter */}
                        <div>
                            <label className="block text-sm font-semibold text-slate-700 mb-2">Category</label>
                            <select
                                className="input-field min-w-[200px]"
                                value={filterCategory}
                                onChange={(e) => setFilterCategory(e.target.value)}
                            >
                                {categories.map(cat => (
                                    <option key={cat} value={cat}>
                                        {cat === 'all' ? 'All Categories' : cat}
                                    </option>
                                ))}
                            </select>
                        </div>

                        {/* Show Inactive Toggle */}
                        <div className="flex items-end">
                            <label className="flex items-center gap-2 cursor-pointer">
                                <input
                                    type="checkbox"
                                    className="w-4 h-4 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500"
                                    checked={showInactive}
                                    onChange={(e) => setShowInactive(e.target.checked)}
                                />
                                <span className="text-sm font-medium text-slate-700">Show Inactive</span>
                            </label>
                        </div>
                    </div>

                    {/* Stats */}
                    <div className="flex gap-6 mt-4 text-sm text-slate-600">
                        <span>Total: <strong className="text-slate-900">{filteredPOs.length}</strong></span>
                        <span>Default: <strong className="text-blue-600">{filteredPOs.filter(po => po.isDefault).length}</strong></span>
                        <span>Custom: <strong className="text-green-600">{filteredPOs.filter(po => !po.isDefault).length}</strong></span>
                        <span>Active: <strong className="text-emerald-600">{filteredPOs.filter(po => po.isActive).length}</strong></span>
                    </div>
                </div>

                {/* PO Grid */}
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {filteredPOs.map((po) => (
                        <div 
                            key={po.poId} 
                            className={`glass-card rounded-xl p-6 hover:shadow-lg transition-all duration-300 cursor-pointer border-l-4 ${
                                po.isDefault ? 'border-l-blue-500' : 'border-l-green-500'
                            } ${!po.isActive ? 'opacity-60' : ''}`}
                            onClick={() => handleViewClick(po)}
                        >
                            {/* Header */}
                            <div className="flex justify-between items-start mb-3">
                                <div>
                                    <h3 className="font-bold text-slate-900 text-lg">{po.poId || po.code}</h3>
                                    <div className="flex gap-2 mt-1">
                                        <span className={`text-xs px-2 py-1 rounded-full font-medium ${po.isDefault ? 'bg-blue-100 text-blue-800' : 'bg-green-100 text-green-800'}`}>
                                            {po.isDefault ? 'Default' : 'Custom'}
                                        </span>
                                        {!po.isActive && (
                                            <span className="text-xs px-2 py-1 rounded-full font-medium bg-red-100 text-red-800">
                                                Inactive
                                            </span>
                                        )}
                                    </div>
                                </div>
                                <div className="flex gap-1">
                                    <button 
                                        onClick={(e) => { e.stopPropagation(); handleEditClick(po); }}
                                        className="p-2 text-slate-400 hover:text-indigo-600 transition-colors"
                                    >
                                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                                        </svg>
                                    </button>
                                    {!po.isActive ? (
                                        <button 
                                            onClick={(e) => { e.stopPropagation(); handleRestore(po); }}
                                            className="p-2 text-slate-400 hover:text-green-600 transition-colors"
                                            title="Restore"
                                        >
                                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                                            </svg>
                                        </button>
                                    ) : (
                                        <button 
                                            onClick={(e) => { e.stopPropagation(); handleDeleteClick(po); }}
                                            className="p-2 text-slate-400 hover:text-red-600 transition-colors"
                                        >
                                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                            </svg>
                                        </button>
                                    )}
                                </div>
                            </div>

                            {/* Category */}
                            <div className="mb-3">
                                <span className={`text-xs px-2 py-1 rounded-full font-medium ${getPOCategoryColor(po.category)}`}>
                                    {po.category}
                                </span>
                            </div>

                            {/* Title */}
                            <h4 className="font-semibold text-slate-800 mb-2 line-clamp-2">{po.title}</h4>

                            {/* Description */}
                            <p className="text-slate-600 text-sm line-clamp-3">{po.description}</p>

                            {/* Display Order */}
                            {po.displayOrder && (
                                <div className="mt-3 text-xs text-slate-400">
                                    Order: {po.displayOrder}
                                </div>
                            )}
                        </div>
                    ))}
                </div>

                {/* Empty State */}
                {filteredPOs.length === 0 && (
                    <div className="text-center py-16">
                        <div className="glass-card rounded-2xl p-12 max-w-md mx-auto">
                            <svg className="w-16 h-16 mx-auto text-slate-300 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                            </svg>
                            <h3 className="text-lg font-semibold text-slate-900 mb-2">No Program Outcomes Found</h3>
                            <p className="text-slate-600 mb-6">
                                {pos.length === 0 
                                    ? "Start by initializing the default Washington Accord Program Outcomes."
                                    : "Try adjusting your filters or search criteria."
                                }
                            </p>
                            {pos.length === 0 && (
                                <button onClick={initializeDefaults} className="btn-primary">
                                    Initialize Default POs
                                </button>
                            )}
                        </div>
                    </div>
                )}
            </main>

            {/* Modal */}
            {showModal && (
                <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50" onClick={() => setShowModal(false)}>
                    <div className="glass-card rounded-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto" onClick={e => e.stopPropagation()}>
                        {modalType === 'view' && selectedPO && (
                            <div className="p-8">
                                <div className="flex justify-between items-start mb-6">
                                    <h3 className="text-2xl font-bold text-slate-900">{selectedPO.poId || selectedPO.code} - {selectedPO.title}</h3>
                                    <button onClick={() => setShowModal(false)} className="p-2 text-slate-400 hover:text-slate-600">
                                        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                                        </svg>
                                    </button>
                                </div>
                                
                                <div className="space-y-4">
                                    <div>
                                        <label className="block text-sm font-semibold text-slate-700 mb-1">Category</label>
                                        <span className={`inline-block px-3 py-1 rounded-full text-sm font-medium ${getPOCategoryColor(selectedPO.category)}`}>
                                            {selectedPO.category}
                                        </span>
                                    </div>
                                    
                                    <div>
                                        <label className="block text-sm font-semibold text-slate-700 mb-1">Description</label>
                                        <p className="text-slate-600 whitespace-pre-wrap">{selectedPO.description}</p>
                                    </div>
                                    
                                    {selectedPO.performanceIndicators && (
                                        <div>
                                            <label className="block text-sm font-semibold text-slate-700 mb-1">Performance Indicators</label>
                                            <p className="text-slate-600 whitespace-pre-wrap">{selectedPO.performanceIndicators}</p>
                                        </div>
                                    )}
                                    
                                    <div className="flex gap-4 text-sm text-slate-500">
                                        <span>Type: <strong className={selectedPO.isDefault ? 'text-blue-600' : 'text-green-600'}>
                                            {selectedPO.isDefault ? 'Default' : 'Custom'}
                                        </strong></span>
                                        <span>Status: <strong className={selectedPO.isActive ? 'text-green-600' : 'text-red-600'}>
                                            {selectedPO.isActive ? 'Active' : 'Inactive'}
                                        </strong></span>
                                        <span>Order: <strong>{selectedPO.displayOrder}</strong></span>
                                    </div>
                                </div>
                                
                                <div className="flex gap-3 mt-8">
                                    <button 
                                        onClick={() => { setShowModal(false); handleEditClick(selectedPO); }}
                                        className="btn-secondary flex-1"
                                    >
                                        Edit PO
                                    </button>
                                    <button onClick={() => setShowModal(false)} className="btn-primary flex-1">
                                        Close
                                    </button>
                                </div>
                            </div>
                        )}

                        {(modalType === 'create' || modalType === 'edit') && (
                            <form onSubmit={handleFormSubmit} className="p-8">
                                <div className="flex justify-between items-center mb-6">
                                    <h3 className="text-2xl font-bold text-slate-900">
                                        {modalType === 'create' ? 'Create New Program Outcome' : 'Edit Program Outcome'}
                                    </h3>
                                    <button type="button" onClick={() => setShowModal(false)} className="p-2 text-slate-400 hover:text-slate-600">
                                        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                                        </svg>
                                    </button>
                                </div>

                                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                    <div>
                                        <label className="block text-sm font-semibold text-slate-700 mb-2">PO ID *</label>
                                        <input
                                            type="text"
                                            className="input-field"
                                            value={formData.poId}
                                            onChange={(e) => setFormData({...formData, poId: e.target.value})}
                                            placeholder="e.g., PO13"
                                            disabled={modalType === 'edit' && !!selectedPO?.poId}
                                            required
                                        />
                                    </div>

                                    <div className="md:col-span-2">
                                        <label className="block text-sm font-semibold text-slate-700 mb-2">Title *</label>
                                        <input
                                            type="text"
                                            className="input-field"
                                            value={formData.title}
                                            onChange={(e) => setFormData({...formData, title: e.target.value})}
                                            placeholder="e.g., Innovation and Entrepreneurship"
                                            required
                                        />
                                    </div>

                                    <div>
                                        <label className="block text-sm font-semibold text-slate-700 mb-2">Category *</label>
                                        <select
                                            className="input-field"
                                            value={formData.category}
                                            onChange={(e) => setFormData({...formData, category: e.target.value})}
                                            required
                                        >
                                            <option value="">Select Category</option>
                                            {categories.slice(1).map(cat => (
                                                <option key={cat} value={cat}>{cat}</option>
                                            ))}
                                        </select>
                                    </div>

                                    <div>
                                        <label className="block text-sm font-semibold text-slate-700 mb-2">Display Order</label>
                                        <input
                                            type="number"
                                            className="input-field"
                                            value={formData.displayOrder}
                                            onChange={(e) => setFormData({...formData, displayOrder: e.target.value})}
                                            min="1"
                                        />
                                    </div>

                                    <div className="md:col-span-2">
                                        <label className="block text-sm font-semibold text-slate-700 mb-2">Description *</label>
                                        <textarea
                                            className="input-field"
                                            rows="4"
                                            value={formData.description}
                                            onChange={(e) => setFormData({...formData, description: e.target.value})}
                                            placeholder="Detailed description of the program outcome..."
                                            required
                                        />
                                    </div>

                                    <div className="md:col-span-2">
                                        <label className="block text-sm font-semibold text-slate-700 mb-2">Performance Indicators</label>
                                        <textarea
                                            className="input-field"
                                            rows="3"
                                            value={formData.performanceIndicators}
                                            onChange={(e) => setFormData({...formData, performanceIndicators: e.target.value})}
                                            placeholder="Measurable indicators for this outcome..."
                                        />
                                    </div>
                                </div>

                                <div className="flex gap-3 mt-8">
                                    <button type="button" onClick={() => setShowModal(false)} className="btn-secondary flex-1">
                                        Cancel
                                    </button>
                                    <button type="submit" className="btn-primary flex-1">
                                        {modalType === 'create' ? 'Create PO' : 'Update PO'}
                                    </button>
                                </div>
                            </form>
                        )}

                        {modalType === 'delete' && selectedPO && (
                            <div className="p-8">
                                <div className="flex justify-between items-start mb-6">
                                    <h3 className="text-2xl font-bold text-slate-900">Confirm Deletion</h3>
                                    <button onClick={() => setShowModal(false)} className="p-2 text-slate-400 hover:text-slate-600">
                                        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                                        </svg>
                                    </button>
                                </div>
                                
                                <div className="mb-6">
                                    <p className="text-slate-600 mb-4">
                                        Are you sure you want to {selectedPO.isDefault ? 'deactivate' : 'delete'} the following Program Outcome?
                                    </p>
                                    
                                    <div className="glass-card rounded-lg p-4 bg-red-50 border border-red-200">
                                        <h4 className="font-semibold text-slate-900">{selectedPO.poId || selectedPO.code} - {selectedPO.title}</h4>
                                        <p className="text-slate-600 text-sm mt-1">{selectedPO.description}</p>
                                    </div>
                                    
                                    {selectedPO.isDefault && (
                                        <p className="text-amber-600 text-sm mt-3">
                                            <strong>Note:</strong> Default Washington Accord POs cannot be permanently deleted, only deactivated.
                                        </p>
                                    )}
                                </div>
                                
                                <div className="flex gap-3">
                                    <button onClick={() => setShowModal(false)} className="btn-secondary flex-1">
                                        Cancel
                                    </button>
                                    <button onClick={handleDelete} className="bg-red-600 hover:bg-red-700 text-white px-6 py-3 rounded-xl font-semibold transition-colors flex-1">
                                        {selectedPO.isDefault ? 'Deactivate' : 'Delete'} PO
                                    </button>
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            )}

            <Footer />
        </div>
    );
}