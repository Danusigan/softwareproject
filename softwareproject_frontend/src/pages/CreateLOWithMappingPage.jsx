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
        batch: new Date().getFullYear().toString().slice(-2) // Default to current year
    });
    
    // Mapping state
    const [mappings, setMappings] = useState({});
    const [mappingRemarks, setMappingRemarks] = useState('');
    
    // UI state
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    
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

    useEffect(() => {
        fetchFormData();
    }, [moduleId]);

    const fetchFormData = async () => {
        try {
            setLoading(true);
            const token = localStorage.getItem('token');
            
            // Get form data with suggestions
            const response = await axios.get(`http://localhost:8080/api/los-with-mapping/form-data/${moduleId}`, {
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
            const moduleResponse = await axios.get(`http://localhost:8080/api/modules/${moduleId}`, {
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
            const response = await axios.get(`http://localhost:8080/api/lo-po-mapping/suggestions`, {
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

            const response = await axios.post('http://localhost:8080/api/los-with-mapping/create', requestData, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (response.data.status === 'SUCCESS') {
                setSuccess('Learning Outcome created successfully with PO mappings!');
                setTimeout(() => {
                    navigate(`/modules`);
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
        <div className="min-h-screen bg-[#f8fafc] flex flex-col relative overflow-hidden">
            {/* Background decorations */}
            <div className="absolute top-0 right-0 w-[500px] h-[500px] bg-indigo-500/5 rounded-full blur-[120px] pointer-events-none" />
            <div className="absolute bottom-0 left-0 w-[500px] h-[500px] bg-emerald-500/5 rounded-full blur-[120px] pointer-events-none" />

            <Header />

            <main className="flex-1 max-w-7xl mx-auto px-6 py-12 w-full relative z-10">
                {/* Header */}
                <div className="flex justify-between items-center mb-8">
                    <div>
                        <button
                            onClick={() => navigate(-1)}
                            className="group flex items-center text-slate-500 hover:text-indigo-600 font-bold transition-all duration-300 mb-4"
                        >
                            <div className="p-2 bg-white rounded-xl shadow-sm border border-slate-100 mr-3 group-hover:bg-indigo-50 group-hover:border-indigo-100 transition-colors">
                                <svg className="w-5 h-5 group-hover:-translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
                                </svg>
                            </div>
                            Back to Modules
                        </button>
                        <h1 className="heading-xl bg-clip-text text-transparent bg-gradient-to-r from-slate-900 to-slate-600">
                            Create Learning Outcome
                        </h1>
                        <p className="text-slate-500 font-medium mt-2">
                            Create a new Learning Outcome with Program Outcome mappings for {module?.moduleName || moduleId}
                        </p>
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

                <form onSubmit={handleSubmit}>
                    <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                        {/* Left Column - LO Details */}
                        <div className="lg:col-span-2 space-y-6">
                            {/* Learning Outcome Information */}
                            <div className="glass-card rounded-2xl p-8">
                                <h3 className="text-xl font-bold text-slate-900 mb-6">Learning Outcome Details</h3>
                                
                                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                    <div>
                                        <label className="block text-sm font-semibold text-slate-700 mb-2">LO ID *</label>
                                        <input
                                            type="text"
                                            className="input-field"
                                            value={formData.id}
                                            onChange={(e) => handleFormDataChange('id', e.target.value)}
                                            placeholder="e.g., LO1, LO2"
                                            required
                                        />
                                    </div>

                                    <div>
                                        <label className="block text-sm font-semibold text-slate-700 mb-2">Batch Year *</label>
                                        <input
                                            type="text"
                                            className="input-field"
                                            value={formData.batch}
                                            onChange={(e) => handleFormDataChange('batch', e.target.value)}
                                            placeholder="e.g., 24, 25"
                                            required
                                        />
                                    </div>

                                    <div className="md:col-span-2">
                                        <label className="block text-sm font-semibold text-slate-700 mb-2">LO Name *</label>
                                        <input
                                            type="text"
                                            className="input-field"
                                            value={formData.name}
                                            onChange={(e) => handleFormDataChange('name', e.target.value)}
                                            placeholder="Brief name for the learning outcome"
                                            required
                                        />
                                    </div>

                                    <div className="md:col-span-2">
                                        <label className="block text-sm font-semibold text-slate-700 mb-2">LO Description *</label>
                                        <textarea
                                            className="input-field"
                                            rows="4"
                                            value={formData.description}
                                            onChange={(e) => handleFormDataChange('description', e.target.value)}
                                            placeholder="Detailed description of what students will be able to do..."
                                            required
                                        />
                                        <p className="text-xs text-slate-500 mt-1">
                                            Tip: Use action verbs like 'Design', 'Analyze', 'Evaluate' for better mapping suggestions
                                        </p>
                                    </div>
                                </div>
                            </div>

                            {/* Program Outcome Mappings */}
                            <div className="glass-card rounded-2xl p-8">
                                <div className="flex justify-between items-center mb-6">
                                    <h3 className="text-xl font-bold text-slate-900">Program Outcome Mappings</h3>
                                    <div className="flex gap-2">
                                        <button
                                            type="button"
                                            onClick={applySuggestions}
                                            className="btn-secondary text-sm"
                                        >
                                            Apply Suggestions
                                        </button>
                                        <button
                                            type="button"
                                            onClick={clearAllMappings}
                                            className="text-slate-400 hover:text-red-600 transition-colors text-sm"
                                        >
                                            Clear All
                                        </button>
                                    </div>
                                </div>

                                {Object.keys(groupedPOs).map(category => (
                                    <div key={category} className="mb-8">
                                        <div className="flex items-center mb-4">
                                            <span className={`px-3 py-1 rounded-full text-sm font-medium ${poCategories[category]}`}>
                                                {category}
                                            </span>
                                            <div className="flex-1 h-px bg-slate-200 ml-4"></div>
                                        </div>

                                        <div className="grid grid-cols-1 gap-4">
                                            {groupedPOs[category].map(po => (
                                                <div key={po.poId} className="border border-slate-200 rounded-xl p-4 hover:border-indigo-200 transition-colors">
                                                    <div className="flex items-start gap-4">
                                                        <div className="flex-1">
                                                            <div className="flex items-center gap-2 mb-2">
                                                                <h4 className="font-semibold text-slate-900">{po.code}</h4>
                                                                <span className="text-slate-600">-</span>
                                                                <h5 className="font-medium text-slate-700">{po.title}</h5>
                                                                {suggestedMappings[po.poId] > 0 && (
                                                                    <span className="px-2 py-1 bg-blue-100 text-blue-800 text-xs rounded-full">
                                                                        Suggested: {suggestedMappings[po.poId]}
                                                                    </span>
                                                                )}
                                                            </div>
                                                            <p className="text-sm text-slate-600 line-clamp-2">{po.description}</p>
                                                        </div>
                                                        
                                                        <div className="flex-shrink-0">
                                                            <select
                                                                value={mappings[po.poId] || 0}
                                                                onChange={(e) => handleMappingChange(po.poId, e.target.value)}
                                                                className={`input-field min-w-[200px] ${
                                                                    mappings[po.poId] > 0 
                                                                        ? weightOptions.find(opt => opt.value === mappings[po.poId])?.color 
                                                                        : 'text-gray-400'
                                                                }`}
                                                            >
                                                                {weightOptions.map(option => (
                                                                    <option key={option.value} value={option.value}>
                                                                        {option.label}
                                                                    </option>
                                                                ))}
                                                            </select>
                                                        </div>
                                                    </div>
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                ))}

                                {/* Mapping Remarks */}
                                <div className="mt-6">
                                    <label className="block text-sm font-semibold text-slate-700 mb-2">Mapping Remarks</label>
                                    <textarea
                                        className="input-field"
                                        rows="3"
                                        value={mappingRemarks}
                                        onChange={(e) => setMappingRemarks(e.target.value)}
                                        placeholder="Provide justification for the mapping choices..."
                                    />
                                </div>
                            </div>
                        </div>

                        {/* Right Column - Summary & Actions */}
                        <div className="space-y-6">
                            {/* Mapping Summary */}
                            <div className="glass-card rounded-2xl p-6 sticky top-6">
                                <h3 className="text-lg font-bold text-slate-900 mb-4">Mapping Summary</h3>
                                
                                <div className="space-y-4">
                                    <div className="flex justify-between items-center">
                                        <span className="text-sm text-slate-600">Total Mappings:</span>
                                        <span className={`font-semibold ${mappingStats.total >= 2 && mappingStats.total <= 5 ? 'text-green-600' : 'text-red-600'}`}>
                                            {mappingStats.total}
                                        </span>
                                    </div>
                                    
                                    <div className="space-y-2">
                                        <div className="flex justify-between items-center">
                                            <span className="text-sm text-red-600">High (3):</span>
                                            <span className="font-semibold">{mappingStats.high}</span>
                                        </div>
                                        <div className="flex justify-between items-center">
                                            <span className="text-sm text-orange-600">Medium (2):</span>
                                            <span className="font-semibold">{mappingStats.medium}</span>
                                        </div>
                                        <div className="flex justify-between items-center">
                                            <span className="text-sm text-yellow-600">Low (1):</span>
                                            <span className="font-semibold">{mappingStats.low}</span>
                                        </div>
                                    </div>

                                    <hr className="border-slate-200" />

                                    <div className="space-y-2">
                                        <h4 className="font-semibold text-slate-700">Validation:</h4>
                                        {mappingStats.total < 2 && (
                                            <p className="text-xs text-red-600">⚠ Need at least 2 mappings</p>
                                        )}
                                        {mappingStats.total > 5 && (
                                            <p className="text-xs text-red-600">⚠ Maximum 5 mappings allowed</p>
                                        )}
                                        {mappingStats.high === 0 && mappingStats.total > 0 && (
                                            <p className="text-xs text-red-600">⚠ Need at least one High (3) mapping</p>
                                        )}
                                        {mappingStats.total >= 2 && mappingStats.total <= 5 && mappingStats.high > 0 && (
                                            <p className="text-xs text-green-600">✓ Mapping rules satisfied</p>
                                        )}
                                    </div>

                                    {activeMappings.length > 0 && (
                                        <>
                                            <hr className="border-slate-200" />
                                            <div>
                                                <h4 className="font-semibold text-slate-700 mb-2">Active Mappings:</h4>
                                                <div className="space-y-1">
                                                    {activeMappings.map(([poId, weight]) => {
                                                        const po = allPOs.find(p => p.poId === poId);
                                                        return (
                                                            <div key={poId} className="flex justify-between items-center text-xs">
                                                                <span className="text-slate-600">{po?.code}</span>
                                                                <span className={`font-semibold ${
                                                                    weight === 3 ? 'text-red-600' : 
                                                                    weight === 2 ? 'text-orange-600' : 
                                                                    'text-yellow-600'
                                                                }`}>
                                                                    {weight}
                                                                </span>
                                                            </div>
                                                        );
                                                    })}
                                                </div>
                                            </div>
                                        </>
                                    )}
                                </div>

                                <div className="mt-6 space-y-3">
                                    <button
                                        type="submit"
                                        disabled={saving || validateForm()}
                                        className="btn-primary w-full disabled:opacity-50 disabled:cursor-not-allowed"
                                    >
                                        {saving ? (
                                            <div className="flex items-center justify-center gap-2">
                                                <div className="animate-spin rounded-full h-4 w-4 border-2 border-white border-t-transparent"></div>
                                                Creating...
                                            </div>
                                        ) : (
                                            'Create Learning Outcome'
                                        )}
                                    </button>

                                    <button
                                        type="button"
                                        onClick={() => navigate(-1)}
                                        className="btn-secondary w-full"
                                    >
                                        Cancel
                                    </button>
                                </div>
                            </div>

                            {/* Mapping Guidelines */}
                            <div className="glass-card rounded-2xl p-6">
                                <h3 className="text-lg font-bold text-slate-900 mb-4">Mapping Guidelines</h3>
                                <div className="space-y-3 text-sm text-slate-600">
                                    <div className="flex items-start gap-2">
                                        <span className="text-red-600 font-semibold">3:</span>
                                        <span>High correlation - direct, strong contribution</span>
                                    </div>
                                    <div className="flex items-start gap-2">
                                        <span className="text-orange-600 font-semibold">2:</span>
                                        <span>Medium correlation - moderate contribution</span>
                                    </div>
                                    <div className="flex items-start gap-2">
                                        <span className="text-yellow-600 font-semibold">1:</span>
                                        <span>Low correlation - minimal contribution</span>
                                    </div>
                                    <div className="flex items-start gap-2">
                                        <span className="text-gray-400 font-semibold">0:</span>
                                        <span>No correlation - not applicable</span>
                                    </div>
                                    
                                    <hr className="border-slate-200 my-3" />
                                    
                                    <div className="space-y-1">
                                        <p className="font-medium text-slate-700">Requirements:</p>
                                        <ul className="space-y-1 text-xs">
                                            <li>• 2-5 total mappings</li>
                                            <li>• At least one High (3) mapping</li>
                                            <li>• Focus on most relevant POs</li>
                                            <li>• Consider assessment feasibility</li>
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