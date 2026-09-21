import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import Header from '../components/header';
import Footer from '../components/footer';
import axios from 'axios';

export default function CreateLOWithMappingPage() {
    const { moduleId } = useParams();
    const navigate = useNavigate();
    
    // Form state
    const [formData, setFormData] = useState({
        id: '',
        name: '',
        description: '',
    });
    
    // Mapping state
    const [mappings, setMappings] = useState({});
    const [mappingRemarks, setMappingRemarks] = useState('');
    
    // UI state
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [expandedPOs, setExpandedPOs] = useState({});
    
    // Data state
    const [allPOs, setAllPOs] = useState([]);
    const [suggestedMappings, setSuggestedMappings] = useState({});
    const [module, setModule] = useState(null);

    // Weight options
    const weightOptions = [
        { value: 0, label: '0 - No Correlation', color: 'text-gray-400' },
        { value: 1, label: '1 - Low Correlation', color: 'text-yellow-600' },
        { value: 2, label: '2 - Medium Correlation', color: 'text-orange-600' },
        { value: 3, label: '3 - High Correlation', color: 'text-red-600' }
    ];

    // PO categories for grouping
    const poCategories = {
        'Knowledge & Understanding': 'bg-slate-100 text-slate-700 border border-slate-200',
        'Intellectual Skills': 'bg-indigo-50 text-indigo-700 border border-indigo-100',
        'Practical Skills': 'bg-emerald-50 text-emerald-700 border border-emerald-100',
        'Professional Skills': 'bg-amber-50 text-amber-700 border border-amber-100',
        'Attitudes & Values': 'bg-rose-50 text-rose-700 border border-rose-100',
        'Interpersonal Skills': 'bg-yellow-50 text-yellow-700 border border-yellow-100',
        'Communication Skills': 'bg-sky-50 text-sky-700 border border-sky-100',
        'Management Skills': 'bg-violet-50 text-violet-700 border border-violet-100',
        'Learning Skills': 'bg-gray-100 text-gray-700 border border-gray-200'
    };

    useEffect(() => {
        fetchFormData();
    }, [moduleId]);

    const fetchFormData = async () => {
        try {
            setLoading(true);
            const token = localStorage.getItem('token');
            
            // Get form data with suggestions
            const response = await axios.get(`/api/los-with-mapping/form-data/${moduleId}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (response.data.status === 'SUCCESS') {
                const data = response.data.data;
                setAllPOs(data.allProgramOutcomes || []);
                setSuggestedMappings(data.suggestedMappings || {});
                
                // Initialize mappings with suggestions
                setMappings(data.suggestedMappings || {});
            }

            // Get module details
            const moduleResponse = await axios.get(`/api/modules/${moduleId}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            
            if (moduleResponse.data.status === 'SUCCESS') {
                setModule(moduleResponse.data.data);
            }

        } catch (err) {
            console.error('Error fetching form data:', err);
            setError('Failed to load form data');
        } finally {
            setLoading(false);
        }
    };

    const handleFormDataChange = (field, value) => {
        setFormData(prev => ({
            ...prev,
            [field]: value
        }));

        // If description changes, get new suggestions
        if (field === 'description' && value.length > 10) {
            getSuggestedMappingsForDescription(value);
        }
    };

    const getSuggestedMappingsForDescription = async (description) => {
        try {
            const token = localStorage.getItem('token');
            const response = await axios.get(`/api/lo-po-mapping/suggestions`, {
                headers: { 'Authorization': `Bearer ${token}` },
                params: { moduleId, loDescription: description }
            });

            if (response.data.status === 'SUCCESS') {
                const newSuggestions = {};
                const data = response.data.data;
                
                Object.keys(data).forEach(poId => {
                    newSuggestions[poId] = data[poId].suggestedWeight;
                });

                setSuggestedMappings(newSuggestions);
                
                // Update current mappings with new suggestions (only if not manually modified)
                setMappings(prev => {
                    const updated = { ...prev };
                    Object.keys(newSuggestions).forEach(poId => {
                        if (updated[poId] === undefined) {
                            updated[poId] = newSuggestions[poId];
                        }
                    });
                    return updated;
                });
            }
        } catch (err) {
            console.warn('Failed to get updated suggestions:', err);
        }
    };

    const handleMappingChange = (poId, weight) => {
        setMappings(prev => ({
            ...prev,
            [poId]: parseInt(weight)
        }));
    };

    const togglePOExpanded = (poId) => {
        setExpandedPOs(prev => ({
            ...prev,
            [poId]: !prev[poId]
        }));
    };

    const applySuggestions = () => {
        setMappings({ ...suggestedMappings });
    };

    const clearAllMappings = () => {
        setMappings({});
    };

    const validateForm = () => {
        if (!formData.id.trim()) return 'LO ID is required';
        if (!formData.name.trim()) return 'LO Name is required';
        if (!formData.description.trim()) return 'LO Description is required';
        // Note: batch is no longer part of LO — it belongs to mark uploads
        
        // Validate mappings
        const activeMappings = Object.entries(mappings).filter(([_, weight]) => weight > 0);
        
        if (activeMappings.length < 2) return 'At least 2 PO mappings are required';
        if (activeMappings.length > 5) return 'Maximum 5 PO mappings allowed';
        
        const hasPrimaryFocus = activeMappings.some(([_, weight]) => weight >= 3);
        if (!hasPrimaryFocus) return 'At least one mapping must have High Correlation (weight 3)';
        
        return null;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        
        const validationError = validateForm();
        if (validationError) {
            setError(validationError);
            return;
        }

        try {
            setSaving(true);
            setError('');
            
            const token = localStorage.getItem('token');
            
            // Filter out zero weights
            const cleanedMappings = {};
            Object.entries(mappings).forEach(([poId, weight]) => {
                if (weight > 0) {
                    cleanedMappings[poId] = weight;
                }
            });

            const requestData = {
                moduleId,
                learningOutcome: formData,
                mappings: cleanedMappings,
                mappingRemarks
            };

            const response = await axios.post('/api/los-with-mapping/create', requestData, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (response.data.status === 'SUCCESS') {
                setSuccess('Learning Outcome created successfully with PO mappings!');
                setTimeout(() => {
                    navigate('/lecturer-dashboard');
                }, 2000);
            }

        } catch (err) {
            console.error('Error creating LO:', err);
            setError(err.response?.data?.message || 'Failed to create Learning Outcome');
        } finally {
            setSaving(false);
        }
    };

    if (loading) {
        return (
            <div className="min-h-screen bg-[#f8fafc] flex flex-col">
                <Header />
                <main className="flex-1 flex items-center justify-center">
                    <div className="glass-card p-8 rounded-2xl">
                        <div className="animate-spin rounded-full h-12 w-12 border-4 border-indigo-500 border-t-transparent mx-auto mb-4"></div>
                        <p className="text-slate-600 font-medium">Loading form data...</p>
                    </div>
                </main>
                <Footer />
            </div>
        );
    }

    const activeMappings = Object.entries(mappings).filter(([_, weight]) => weight > 0);
    const mappingStats = {
        total: activeMappings.length,
        high: activeMappings.filter(([_, weight]) => weight === 3).length,
        medium: activeMappings.filter(([_, weight]) => weight === 2).length,
        low: activeMappings.filter(([_, weight]) => weight === 1).length
    };

    // Group POs by category
    const groupedPOs = allPOs.reduce((groups, po) => {
        const category = po.category || 'Other';
        if (!groups[category]) groups[category] = [];
        groups[category].push(po);
        return groups;
    }, {});

    return (
        <div className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-slate-50 flex flex-col relative overflow-hidden">
            {/* Subtle Background Decorations */}
            <div className="absolute top-0 right-0 w-96 h-96 bg-indigo-500/8 rounded-full blur-3xl pointer-events-none" />
            <div className="absolute bottom-0 left-0 w-96 h-96 bg-blue-500/5 rounded-full blur-3xl pointer-events-none" />

            <Header />

            <main className="flex-1 max-w-6xl mx-auto px-6 py-12 w-full relative z-10">
                {/* Page Header */}
                <div className="mb-10">
                    <button
                        onClick={() => navigate(-1)}
                        className="flex items-center gap-2 text-indigo-600 hover:text-indigo-700 font-medium text-sm mb-6 transition-colors group"
                    >
                        <svg className="w-5 h-5 group-hover:-translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
                        </svg>
                        Back to Modules
                    </button>
                    
                    <div>
                        <h1 className="text-4xl font-bold text-slate-900 mb-3">Create Learning Outcome</h1>
                        <p className="text-slate-600 text-base max-w-2xl">
                            Define a learning outcome and map it to the program outcomes for <span className="font-semibold text-indigo-600">{module?.moduleName || moduleId}</span>
                        </p>
                    </div>
                </div>

                {/* Messages */}
                {error && (
                    <div className="bg-red-50 border-l-4 border-red-500 rounded-lg p-4 mb-6 flex items-start gap-3 animate-in slide-in-from-top">
                        <svg className="w-6 h-6 text-red-600 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                        <p className="text-red-800 font-medium">{error}</p>
                    </div>
                )}

                {success && (
                    <div className="bg-green-50 border-l-4 border-green-500 rounded-lg p-4 mb-6 flex items-start gap-3 animate-in slide-in-from-top">
                        <svg className="w-6 h-6 text-green-600 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                        </svg>
                        <p className="text-green-800 font-medium">{success}</p>
                    </div>
                )}

                <form onSubmit={handleSubmit}>
                    <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                        {/* Left Column - Form */}
                        <div className="lg:col-span-2 space-y-7">
                            {/* Learning Outcome Details Card */}
                            <div className="bg-white rounded-2xl p-8 border border-slate-200/50 shadow-sm hover:shadow-md transition-shadow">
                                <div className="flex items-center gap-3 mb-6">
                                    <div className="w-10 h-10 rounded-lg bg-indigo-100 flex items-center justify-center">
                                        <svg className="w-6 h-6 text-indigo-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                                        </svg>
                                    </div>
                                    <h2 className="text-xl font-bold text-slate-900">Learning Outcome Details</h2>
                                </div>
                                
                                <div className="space-y-6">
                                    <div>
                                        <label className="block text-sm font-semibold text-slate-700 mb-3">LO ID <span className="text-red-500">*</span></label>
                                        <input
                                            type="text"
                                            className="w-full px-4 py-3 rounded-lg border border-slate-200 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 outline-none transition-all bg-white hover:border-slate-300"
                                            value={formData.id}
                                            onChange={(e) => handleFormDataChange('id', e.target.value)}
                                            placeholder="e.g., LO1"
                                            required
                                        />
                                        <p className="text-xs text-slate-500 mt-2">Unique identifier for this learning outcome</p>
                                    </div>

                                    <div>
                                        <label className="block text-sm font-semibold text-slate-700 mb-3">LO Name <span className="text-red-500">*</span></label>
                                        <input
                                            type="text"
                                            className="w-full px-4 py-3 rounded-lg border border-slate-200 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 outline-none transition-all bg-white hover:border-slate-300"
                                            value={formData.name}
                                            onChange={(e) => handleFormDataChange('name', e.target.value)}
                                            placeholder="Concise title for this learning outcome"
                                            required
                                        />
                                        <p className="text-xs text-slate-500 mt-2">Make it clear and descriptive</p>
                                    </div>

                                    <div>
                                        <label className="block text-sm font-semibold text-slate-700 mb-3">LO Description <span className="text-red-500">*</span></label>
                                        <textarea
                                            className="w-full px-4 py-3 rounded-lg border border-slate-200 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 outline-none transition-all bg-white hover:border-slate-300 resize-none"
                                            rows="4"
                                            value={formData.description}
                                            onChange={(e) => handleFormDataChange('description', e.target.value)}
                                            placeholder="What should students be able to do? Use action verbs like: Design, Analyze, Evaluate, Create..."
                                            required
                                        />
                                        <p className="text-xs text-slate-500 mt-2">
                                            💡 Tip: Use strong action verbs (Design, Analyze, Evaluate) to get better PO suggestions
                                        </p>
                                    </div>
                                </div>
                            </div>

                            {/* Program Outcome Mappings Card */}
                            <div className="bg-white rounded-2xl p-8 border border-slate-200/50 shadow-sm hover:shadow-md transition-shadow">
                                <div className="flex items-center justify-between mb-6">
                                    <div className="flex items-center gap-3">
                                        <div className="w-10 h-10 rounded-lg bg-blue-100 flex items-center justify-center">
                                            <svg className="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
                                            </svg>
                                        </div>
                                        <h2 className="text-xl font-bold text-slate-900">Map Program Outcomes</h2>
                                    </div>
                                    <div className="flex gap-2 flex-wrap justify-end">
                                        <button
                                            type="button"
                                            onClick={applySuggestions}
                                            className="px-4 py-2 text-sm font-medium rounded-lg bg-indigo-50 text-indigo-700 border border-indigo-200 hover:bg-indigo-100 transition-colors"
                                        >
                                            Apply Suggestions
                                        </button>
                                        <button
                                            type="button"
                                            onClick={clearAllMappings}
                                            className="px-4 py-2 text-sm font-medium rounded-lg text-slate-600 hover:bg-slate-50 transition-colors"
                                        >
                                            Clear
                                        </button>
                                    </div>
                                </div>

                                <p className="text-sm text-slate-600 mb-6 pb-6 border-b border-slate-200">
                                    Click on each program outcome to expand and set the mapping correlation level.
                                </p>

                                {/* Program Outcomes by Category */}
                                <div className="space-y-4">
                                    {Object.keys(groupedPOs).map(category => (
                                        <div key={category} className="space-y-3">
                                            {/* Category Header */}
                                            <div className="flex items-center gap-3 px-4 py-2">
                                                <div className={`h-2 w-2 rounded-full ${
                                                    category === 'Knowledge & Understanding' ? 'bg-slate-500' :
                                                    category === 'Intellectual Skills' ? 'bg-indigo-500' :
                                                    category === 'Practical Skills' ? 'bg-emerald-500' :
                                                    category === 'Professional Skills' ? 'bg-amber-500' :
                                                    category === 'Attitudes & Values' ? 'bg-rose-500' :
                                                    category === 'Interpersonal Skills' ? 'bg-yellow-500' :
                                                    category === 'Communication Skills' ? 'bg-sky-500' :
                                                    category === 'Management Skills' ? 'bg-violet-500' :
                                                    'bg-gray-500'
                                                }`}></div>
                                                <h3 className="font-semibold text-slate-900 text-sm tracking-tight">{category}</h3>
                                                <span className="ml-auto text-xs font-medium text-slate-500 bg-slate-100/80 px-2 py-1 rounded">
                                                    {groupedPOs[category].length}
                                                </span>
                                            </div>

                                            {/* PO Items */}
                                            <div className="space-y-2 pl-4">
                                                {groupedPOs[category].map(po => {
                                                    const isExpanded = expandedPOs[po.poId];
                                                    const isMapped = mappings[po.poId] > 0;
                                                    
                                                    return (
                                                        <div
                                                            key={po.poId}
                                                            className="overflow-hidden rounded-lg border border-slate-200 hover:border-indigo-300 transition-all hover:shadow-md"
                                                        >
                                                            {/* PO Header - Clickable */}
                                                            <button
                                                                type="button"
                                                                onClick={() => togglePOExpanded(po.poId)}
                                                                className="w-full px-4 py-3 text-left bg-white hover:bg-slate-50/50 transition-colors flex items-start justify-between gap-3 group"
                                                            >
                                                                <div className="flex-1 min-w-0">
                                                                    <div className="flex items-center gap-2 flex-wrap">
                                                                        <span className="font-semibold text-slate-900 group-hover:text-indigo-600 transition-colors">{po.code}</span>
                                                                        <span className="hidden sm:inline text-slate-400">—</span>
                                                                        <span className="font-medium text-slate-700 truncate">{po.title}</span>
                                                                        {isMapped && (
                                                                            <span className={`ml-auto sm:ml-2 text-xs font-bold px-2 py-1 rounded-full ${
                                                                                mappings[po.poId] === 3 ? 'bg-red-100 text-red-700' :
                                                                                mappings[po.poId] === 2 ? 'bg-orange-100 text-orange-700' :
                                                                                'bg-yellow-100 text-yellow-700'
                                                                            }`}>
                                                                                {mappings[po.poId]}
                                                                            </span>
                                                                        )}
                                                                    </div>
                                                                </div>
                                                                
                                                                <div className="flex-shrink-0">
                                                                    <svg
                                                                        className={`w-5 h-5 text-slate-400 transition-transform group-hover:text-indigo-600 ${isExpanded ? 'rotate-180' : ''}`}
                                                                        fill="none"
                                                                        stroke="currentColor"
                                                                        viewBox="0 0 24 24"
                                                                    >
                                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 14l-7 7m0 0l-7-7m7 7V3" />
                                                                    </svg>
                                                                </div>
                                                            </button>

                                                            {/* PO Details - Expandable */}
                                                            {isExpanded && (
                                                                <div className="border-t border-slate-200 bg-slate-50/50 px-4 py-3 space-y-3 animate-in slide-in-from-top-2">
                                                                    <div>
                                                                        <p className="text-xs font-medium text-slate-600 mb-1">Description</p>
                                                                        <p className="text-sm text-slate-700 leading-relaxed">{po.description}</p>
                                                                    </div>

                                                                    {suggestedMappings[po.poId] > 0 && (
                                                                        <div className="bg-indigo-50 border border-indigo-200 rounded-lg px-3 py-2">
                                                                            <p className="text-xs font-semibold text-indigo-700 flex items-center gap-1">
                                                                                <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                                                                                    <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                                                                                </svg>
                                                                                Suggested: Level {suggestedMappings[po.poId]}
                                                                            </p>
                                                                        </div>
                                                                    )}

                                                                    <div>
                                                                        <label className="text-xs font-semibold text-slate-700 block mb-2">Correlation Level</label>
                                                                        <select
                                                                            value={mappings[po.poId] || 0}
                                                                            onChange={(e) => handleMappingChange(po.poId, e.target.value)}
                                                                            className="w-full px-3 py-2 rounded-lg border border-slate-300 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 outline-none text-sm font-medium bg-white hover:border-slate-400 transition-colors cursor-pointer"
                                                                        >
                                                                            <option value="0" className="text-gray-400">No Correlation (0)</option>
                                                                            <option value="1" className="text-yellow-600">Low Correlation (1)</option>
                                                                            <option value="2" className="text-orange-600">Medium Correlation (2)</option>
                                                                            <option value="3" className="text-red-600">High Correlation (3)</option>
                                                                        </select>
                                                                    </div>
                                                                </div>
                                                            )}
                                                        </div>
                                                    );
                                                })}
                                            </div>
                                        </div>
                                    ))}
                                </div>

                                {/* Mapping Remarks */}
                                <div className="mt-8 pt-8 border-t border-slate-200">
                                    <label className="block text-sm font-semibold text-slate-700 mb-3">Mapping Remarks (Optional)</label>
                                    <textarea
                                        className="w-full px-4 py-3 rounded-lg border border-slate-200 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 outline-none transition-all bg-white hover:border-slate-300 resize-none"
                                        rows="3"
                                        value={mappingRemarks}
                                        onChange={(e) => setMappingRemarks(e.target.value)}
                                        placeholder="Add notes about why these POs are mapped to this LO..."
                                    />
                                </div>
                            </div>
                        </div>

                        {/* Right Column - Summary & Actions */}
                        <div className="space-y-6">
                            {/* Mapping Summary - Sticky */}
                            <div className="bg-gradient-to-br from-indigo-50 to-blue-50 rounded-2xl p-6 border border-indigo-200/50 shadow-sm sticky top-24">
                                <h3 className="font-bold text-lg text-slate-900 mb-4 flex items-center gap-2">
                                    <svg className="w-5 h-5 text-indigo-600" fill="currentColor" viewBox="0 0 20 20">
                                        <path d="M9 2a1 1 0 000 2h2a1 1 0 100-2H9z"></path>
                                        <path fillRule="evenodd" d="M4 5a2 2 0 012-2 1 1 0 000 2H3a1 1 0 000 2h12a1 1 0 100-2h3a2 2 0 012 2v9a2 2 0 01-2 2H6a2 2 0 01-2-2V5zm12 4a1 1 0 100 2H4a1 1 0 100-2h12z" clipRule="evenodd"></path>
                                    </svg>
                                    Mapping Summary
                                </h3>
                                
                                <div className="space-y-4">
                                    {/* Total Mappings */}
                                    <div className="bg-white rounded-lg px-4 py-3 flex justify-between items-center">
                                        <span className="text-sm font-medium text-slate-700">Total Mappings</span>
                                        <span className={`font-bold text-lg ${
                                            mappingStats.total >= 2 && mappingStats.total <= 5 ? 'text-green-600' : 'text-slate-600'
                                        }`}>
                                            {mappingStats.total}/5
                                        </span>
                                    </div>

                                    {/* Mapping Breakdown */}
                                    <div className="space-y-2 text-sm">
                                        <div className="flex justify-between items-center px-3 py-2 bg-white/50 rounded-lg">
                                            <div className="flex items-center gap-2">
                                                <div className="w-3 h-3 rounded-full bg-red-500"></div>
                                                <span className="text-slate-700">High (3)</span>
                                            </div>
                                            <span className="font-bold text-slate-900">{mappingStats.high}</span>
                                        </div>
                                        <div className="flex justify-between items-center px-3 py-2 bg-white/50 rounded-lg">
                                            <div className="flex items-center gap-2">
                                                <div className="w-3 h-3 rounded-full bg-orange-500"></div>
                                                <span className="text-slate-700">Medium (2)</span>
                                            </div>
                                            <span className="font-bold text-slate-900">{mappingStats.medium}</span>
                                        </div>
                                        <div className="flex justify-between items-center px-3 py-2 bg-white/50 rounded-lg">
                                            <div className="flex items-center gap-2">
                                                <div className="w-3 h-3 rounded-full bg-yellow-500"></div>
                                                <span className="text-slate-700">Low (1)</span>
                                            </div>
                                            <span className="font-bold text-slate-900">{mappingStats.low}</span>
                                        </div>
                                    </div>

                                    {/* Status Indicator */}
                                    <div className="mt-4 p-3 rounded-lg bg-white">
                                        {mappingStats.total < 2 && (
                                            <p className="text-xs font-medium text-red-700 flex items-center gap-2">
                                                <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                                                    <path fillRule="evenodd" d="M13.477 14.89A6 6 0 015.11 2.476M17.618 17.618a7 7 0 10-9.9-9.9M7 7a3 3 0 11-6 0 3 3 0 016 0z" clipRule="evenodd"></path>
                                                </svg>
                                                Min. 2 mappings
                                            </p>
                                        )}
                                        {mappingStats.total > 5 && (
                                            <p className="text-xs font-medium text-red-700 flex items-center gap-2">
                                                <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                                                    <path fillRule="evenodd" d="M13.477 14.89A6 6 0 015.11 2.476M17.618 17.618a7 7 0 10-9.9-9.9M7 7a3 3 0 11-6 0 3 3 0 016 0z" clipRule="evenodd"></path>
                                                </svg>
                                                Max. 5 mappings
                                            </p>
                                        )}
                                        {mappingStats.high === 0 && mappingStats.total > 0 && (
                                            <p className="text-xs font-medium text-red-700 flex items-center gap-2">
                                                <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                                                    <path fillRule="evenodd" d="M13.477 14.89A6 6 0 015.11 2.476M17.618 17.618a7 7 0 10-9.9-9.9M7 7a3 3 0 11-6 0 3 3 0 016 0z" clipRule="evenodd"></path>
                                                </svg>
                                                Need 1 High mapping
                                            </p>
                                        )}
                                        {mappingStats.total >= 2 && mappingStats.total <= 5 && mappingStats.high > 0 && (
                                            <p className="text-xs font-medium text-green-700 flex items-center gap-2">
                                                <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                                                    <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd"></path>
                                                </svg>
                                                Ready to submit
                                            </p>
                                        )}
                                    </div>
                                </div>

                                {/* Action Buttons */}
                                <div className="mt-6 space-y-3">
                                    <button
                                        type="submit"
                                        disabled={saving || validateForm()}
                                        className="w-full py-3 px-4 rounded-lg bg-indigo-600 hover:bg-indigo-700 disabled:bg-slate-300 disabled:cursor-not-allowed text-white font-semibold transition-all duration-200 shadow-md hover:shadow-lg active:scale-95 flex items-center justify-center gap-2"
                                    >
                                        {saving ? (
                                            <>
                                                <div className="animate-spin rounded-full h-4 w-4 border-2 border-white border-t-transparent"></div>
                                                Creating...
                                            </>
                                        ) : (
                                            <>
                                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" />
                                                </svg>
                                                Create LO
                                            </>
                                        )}
                                    </button>

                                    <button
                                        type="button"
                                        onClick={() => navigate(-1)}
                                        className="w-full py-3 px-4 rounded-lg border border-slate-300 text-slate-700 font-semibold hover:bg-slate-50 transition-colors"
                                    >
                                        Cancel
                                    </button>
                                </div>
                            </div>

                            {/* Guidelines Card */}
                            <div className="bg-white rounded-2xl p-6 border border-slate-200/50 shadow-sm">
                                <h3 className="font-bold text-slate-900 mb-4 flex items-center gap-2">
                                    <svg className="w-5 h-5 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                                        <path d="M18 5v8a2 2 0 01-2 2h-5l-5 4v-4H4a2 2 0 01-2-2V5a2 2 0 012-2h12a2 2 0 012 2z"></path>
                                    </svg>
                                    Quick Guide
                                </h3>
                                
                                <div className="space-y-3 text-sm">
                                    <div>
                                        <p className="font-semibold text-red-600 mb-1">Level 3 - High</p>
                                        <p className="text-slate-600 text-xs">Direct & strong contribution</p>
                                    </div>
                                    <div>
                                        <p className="font-semibold text-orange-600 mb-1">Level 2 - Medium</p>
                                        <p className="text-slate-600 text-xs">Moderate contribution</p>
                                    </div>
                                    <div>
                                        <p className="font-semibold text-yellow-600 mb-1">Level 1 - Low</p>
                                        <p className="text-slate-600 text-xs">Minimal contribution</p>
                                    </div>

                                    <div className="pt-3 border-t border-slate-200 mt-3">
                                        <p className="font-semibold text-slate-700 mb-2">Requirements</p>
                                        <ul className="space-y-1 text-slate-600">
                                            <li className="text-xs flex items-start gap-2">
                                                <span className="text-blue-600 mt-0.5">✓</span>
                                                <span>2-5 mappings</span>
                                            </li>
                                            <li className="text-xs flex items-start gap-2">
                                                <span className="text-blue-600 mt-0.5">✓</span>
                                                <span>Min 1 Level 3</span>
                                            </li>
                                        </ul>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </form>
            </main>

            <Footer />
        </div>
    );
}