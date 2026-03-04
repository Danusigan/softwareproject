import React, { useState, useRef, useEffect } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import axios from 'axios';

export default function AddResultsPage() {
    const { loId } = useParams();
    const navigate = useNavigate();
    const location = useLocation();
    const [batch, setBatch] = useState('');
    const [file, setFile] = useState(null);
    const [loading, setLoading] = useState(false);
    const [dragActive, setDragActive] = useState(false);
    const [message, setMessage] = useState({ type: '', text: '' });
    const [isEditMode, setIsEditMode] = useState(false);
    const [existingFileName, setExistingFileName] = useState('');
    const loNumber = sessionStorage.getItem('currentLoNumber') || '';
    const fileInputRef = useRef(null);

    const batches = ['20', '21', '22', '23', '24', '25'];

    // Check if we're in edit mode and pre-populate data
    useEffect(() => {
        if (location.state?.editMode) {
            setIsEditMode(true);
            setBatch(location.state.batch || '');
            setExistingFileName(location.state.fileName || '');
        }
    }, [location.state]);

    const handleDrag = (e) => {
        e.preventDefault();
        e.stopPropagation();
        if (e.type === "dragenter" || e.type === "dragover") {
            setDragActive(true);
        } else if (e.type === "dragleave") {
            setDragActive(false);
        }
    };

    const handleDrop = (e) => {
        e.preventDefault();
        e.stopPropagation();
        setDragActive(false);
        if (e.dataTransfer.files?.[0]) {
            const droppedFile = e.dataTransfer.files[0];
            if (droppedFile.name.endsWith('.xlsx') || droppedFile.name.endsWith('.xls') || droppedFile.name.endsWith('.csv')) {
                setFile(droppedFile);
            } else {
                setMessage({ type: 'error', text: 'Please upload an Excel or CSV file.' });
            }
        }
    };

    const handleChange = (e) => {
        if (e.target.files?.[0]) {
            setFile(e.target.files[0]);
        }
    };

    const clearFile = (e) => {
        e.stopPropagation();
        setFile(null);
        if (fileInputRef.current) {
            fileInputRef.current.value = '';
        }
    };

    const onButtonClick = () => {
        fileInputRef.current.click();
    };

    const handleUpload = async (e) => {
        e.preventDefault();
        if (!batch) {
            alert('Please select a batch');
            return;
        }

        setLoading(true);
        setMessage({ type: '', text: '' });

        try {
            const token = localStorage.getItem('token');

            // If in edit mode and no new file selected, update batch only
            if (isEditMode && !file) {
                // Update batch for existing records
                await axios.put(
                    `http://localhost:8080/api/lospos/${loId}/batch/update`,
                    { oldBatch: location.state.batch, newBatch: batch },
                    {
                        headers: {
                            'Authorization': `Bearer ${token}`,
                            'Content-Type': 'application/json'
                        }
                    }
                );

                setMessage({ type: 'success', text: 'Batch updated successfully!' });
                setTimeout(() => navigate(-1), 1500);
                return;
            }

            // File upload flow (new or replacement)
            if (!file) {
                alert('Please select a file');
                return;
            }

            const formData = new FormData();
            formData.append('excelFile', file);
            formData.append('batch', batch);
            formData.append('loNumber', loNumber);

            // If editing with new file, first delete old batch
            if (isEditMode && location.state.batch) {
                await axios.delete(
                    `http://localhost:8080/api/lospos/${loId}/batch/${location.state.batch}`,
                    {
                        headers: {
                            'Authorization': `Bearer ${token}`
                        }
                    }
                );
            }

            // Import marks directly for the selected LO
            await axios.post(
                `http://localhost:8080/api/lospos/${loId}/marks/import-obe`,
                formData,
                {
                    headers: {
                        'Authorization': `Bearer ${token}`,
                        'Content-Type': 'multipart/form-data'
                    }
                }
            );

            setMessage({ type: 'success', text: 'Results ' + (isEditMode ? 'updated' : 'uploaded') + ' successfully!' });
            setTimeout(() => navigate(-1), 1500);
        } catch (err) {
            console.error('Upload flow failed:', err);
            setMessage({
                type: 'error',
                text: err.response?.data?.message || err.response?.data || 'Failed to process results'
            });
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-[#f8fafc] flex items-center justify-center p-6 relative overflow-hidden">
            {/* Background Decorative Elements */}
            <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-indigo-500/10 rounded-full blur-[120px] pointer-events-none" />
            <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-emerald-500/10 rounded-full blur-[120px] pointer-events-none" />

            <div className="w-full max-w-4xl glass-card rounded-[2rem] relative overflow-hidden flex flex-col items-center py-12 px-8 sm:px-16 animate-in fade-in slide-in-from-bottom-4 duration-700">

                {/* Close Button */}
                <button
                    onClick={() => navigate(-1)}
                    className="absolute top-6 right-8 p-2 hover:bg-slate-100 rounded-full transition-all duration-300 text-slate-400 hover:text-slate-600 group"
                >
                    <svg className="w-8 h-8 group-hover:rotate-90 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                    </svg>
                </button>

                {/* Header Title */}
                <div className="text-center mb-12">
                    <span className="px-4 py-1.5 bg-indigo-50 text-indigo-600 rounded-full text-sm font-bold tracking-wider uppercase mb-4 inline-block">
                        {isEditMode ? 'Edit Results' : 'Results Portal'}
                    </span>
                    <h1 className="heading-xl bg-clip-text text-transparent bg-gradient-to-r from-slate-900 to-slate-600">
                        {isEditMode ? `Update Batch ${batch} Results` : 'Upload Activity Results'}
                        {!isEditMode && loNumber && <span className="text-indigo-600 ml-2">LO {loNumber}</span>}
                    </h1>
                    <p className="mt-4 text-slate-500 max-w-md mx-auto">
                        Seamlessly import student marks and track performance outcomes across different batches.
                    </p>
                </div>

                {/* Form Section */}
                <div className="w-full max-w-2xl space-y-8">

                    {/* Batch Selection */}
                    <div className="space-y-2">
                        <label htmlFor="batch" className="text-sm font-bold text-slate-700 ml-1">
                            Selection Batch
                        </label>
                        <select
                            id="batch"
                            value={batch}
                            onChange={(e) => setBatch(e.target.value)}
                            className="input-field appearance-none w-full"
                            style={{ backgroundImage: `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='%2364748b'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M19 9l-7 7-7-7'%3E%3C/path%3E%3C/svg%3E")`, backgroundRepeat: 'no-repeat', backgroundPosition: 'right 1rem center', backgroundSize: '1.25rem' }}
                        >
                            <option value="" disabled>Select Batch Number</option>
                            {batches.map(v => (
                                <option key={v} value={v}>Batch {v}</option>
                            ))}
                        </select>
                    </div>

                    {/* Upload Box */}
                    <div className="space-y-2">
                        <label htmlFor="resultFile" className="text-sm font-bold text-slate-700 ml-1">
                            Document Upload
                        </label>
                        <button
                            type="button"
                            className={`w-full group p-10 border-2 border-dashed rounded-[2rem] transition-all duration-300 flex flex-col items-center justify-center cursor-pointer
                                ${dragActive ? 'border-indigo-500 bg-indigo-50/30' : 'border-slate-200 hover:border-indigo-400 bg-white/30'}`}
                            onDragEnter={handleDrag}
                            onDragLeave={handleDrag}
                            onDragOver={handleDrag}
                            onDrop={handleDrop}
                            onClick={onButtonClick}
                        >
                            <input
                                ref={fileInputRef}
                                id="resultFile"
                                type="file"
                                className="hidden"
                                onChange={handleChange}
                                accept=".xlsx,.xls,.csv"
                            />

                            <div className="w-20 h-20 bg-indigo-50 rounded-2xl flex items-center justify-center mb-6 text-indigo-600 transition-transform group-hover:scale-110 duration-300">
                                {file ? (
                                    <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                                    </svg>
                                ) : (
                                    <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                                    </svg>
                                )}
                            </div>

                            <div className="text-center">
                                <h3 className="text-xl font-bold text-slate-800 mb-2">
                                    {file ? file.name : (isEditMode && existingFileName ? existingFileName : 'Drop your file here')}
                                </h3>
                                <p className="text-slate-500 text-sm">
                                    {file ? `${(file.size / 1024).toFixed(1)} KB` : (isEditMode && existingFileName ? 'Current file - Upload new to replace' : 'Click to browse or drag & drop (Excel, CSV)')}
                                </p>
                            </div>

                        </button>
                        {file && (
                            <button
                                type="button"
                                onClick={clearFile}
                                className="mt-6 px-4 py-2 bg-slate-100 text-slate-600 rounded-lg text-xs font-bold hover:bg-red-50 hover:text-red-500 transition-colors"
                            >
                                Select Different File
                            </button>
                        )}
                    </div>

                    {/* Notification Message */}
                    {message.text && (
                        <div className={`flex items-center gap-3 p-4 rounded-xl text-sm font-medium animate-in fade-in slide-in-from-top-2 duration-300
                            ${message.type === 'success' ? 'bg-emerald-50 text-emerald-700' : 'bg-red-50 text-red-700'}`}>
                            <svg className="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d={message.type === 'success' ? "M5 13l4 4L19 7" : "M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"} />
                            </svg>
                            {message.text}
                        </div>
                    )}

                    {/* Action Button */}
                    <div className="pt-4">
                        <button
                            onClick={handleUpload}
                            disabled={loading || !batch || (!file && !(isEditMode && existingFileName))}
                            className={`w-full py-4 px-8 rounded-2xl text-white font-bold text-lg shadow-xl shadow-indigo-100 transition-all duration-300 transform active:scale-95 flex items-center justify-center gap-3
                                ${loading || (!file && !(isEditMode && existingFileName)) || !batch
                                    ? 'bg-slate-300 cursor-not-allowed shadow-none'
                                    : 'bg-indigo-600 hover:bg-indigo-700 hover:shadow-indigo-200'}`}
                        >
                            {loading && (
                                <svg className="animate-spin h-5 w-5 text-white" fill="none" viewBox="0 0 24 24">
                                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                                </svg>
                            )}
                            {loading ? 'Processing Data...' : (isEditMode ? 'Update Results' : 'Confirm and Upload')}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}
