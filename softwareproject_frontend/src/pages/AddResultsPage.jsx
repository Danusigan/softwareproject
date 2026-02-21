import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';

export default function AddResultsPage() {
    const { loId } = useParams();
    const navigate = useNavigate();
    const [batch, setBatch] = useState('');
    const [academicYear, setAcademicYear] = useState('');
    const [file, setFile] = useState(null);
    const [loading, setLoading] = useState(false);
    const [lo, setLo] = useState(null);
    const [dragActive, setDragActive] = useState(false);
    const [message, setMessage] = useState({ type: '', text: '' });
    const loNumber = sessionStorage.getItem('currentLoNumber') || '';
    const fileInputRef = useRef(null);

    const batches = ['20', '21', '22', '23', '24', '25'];

    useEffect(() => {
        fetchLODetails();
    }, [loId]);

    const fetchLODetails = async () => {
        try {
            const token = localStorage.getItem('token');
            const res = await axios.get(`http://localhost:8080/api/lospos/${loId}`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            setLo(res.data);
        } catch (err) {
            console.error('Error fetching LO details:', err);
        }
    };

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
        if (e.dataTransfer.files && e.dataTransfer.files[0]) {
            const droppedFile = e.dataTransfer.files[0];
            if (droppedFile.name.endsWith('.xlsx') || droppedFile.name.endsWith('.xls') || droppedFile.name.endsWith('.csv')) {
                setFile(droppedFile);
            } else {
                setMessage({ type: 'error', text: 'Please upload an Excel or CSV file.' });
            }
        }
    };

    const handleChange = (e) => {
        if (e.target.files && e.target.files[0]) {
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
        if (!academicYear) {
            alert('Please select an academic year');
            return;
        }
        if (!file) {
            alert('Please select a file');
            return;
        }

        setLoading(true);
        setMessage({ type: '', text: '' });

        try {
            const token = localStorage.getItem('token');
            let currentAssignmentId;

            // Find if an assignment for the specific batch and academic year already exists
            const existingAssignment = lo?.assignments?.find(a => a.academicYear === academicYear && a.batch === batch);

            if (existingAssignment) {
                currentAssignmentId = existingAssignment.assignmentId;
            } else {
                // Create a unique assignment ID for this batch + year
                currentAssignmentId = `${loId}-${batch}-${academicYear.replace('/', '-')}`;

                const formDataAssign = new FormData();
                formDataAssign.append('assignmentId', currentAssignmentId);
                formDataAssign.append('assignmentName', `LO ${loNumber} - ${academicYear} (Batch ${batch})`);
                formDataAssign.append('academicYear', academicYear);
                formDataAssign.append('batch', batch);

                // Create the assignment link
                const assignRes = await axios.post(
                    `http://localhost:8080/api/assignments/${loId}/add`,
                    formDataAssign,
                    { headers: { 'Authorization': `Bearer ${token}` } }
                );

                // Update local lo state to include this new assignment
                setLo(prev => ({
                    ...prev,
                    assignments: [...(prev.assignments || []), assignRes.data]
                }));
            }

            const formData = new FormData();
            formData.append('excelFile', file);
            formData.append('batch', batch);
            formData.append('loNumber', loNumber);
            formData.append('academicYear', academicYear);

            // Import marks using the linked assignment ID
            const res = await axios.post(
                `http://localhost:8080/api/assignments/${currentAssignmentId}/import-marks-obe`,
                formData,
                {
                    headers: {
                        'Authorization': `Bearer ${token}`,
                        'Content-Type': 'multipart/form-data'
                    }
                }
            );

            setMessage({ type: 'success', text: 'Results uploaded successfully!' });
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
                        Results Portal
                    </span>
                    <h1 className="heading-xl bg-clip-text text-transparent bg-gradient-to-r from-slate-900 to-slate-600">
                        Upload Activity Results
                        {loNumber && <span className="text-indigo-600 ml-2">LO {loNumber}</span>}
                    </h1>
                    <p className="mt-4 text-slate-500 max-w-md mx-auto">
                        Seamlessly import student marks and track performance outcomes across different batches.
                    </p>
                </div>

                {/* Form Section */}
                <div className="w-full max-w-2xl space-y-8">

                    {/* Batch & Year Grid */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <div className="space-y-2">
                            <label className="text-sm font-bold text-slate-700 ml-1">
                                Selection Batch
                            </label>
                            <select
                                value={batch}
                                onChange={(e) => setBatch(e.target.value)}
                                className="input-field appearance-none"
                                style={{ backgroundImage: `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='%2364748b'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M19 9l-7 7-7-7'%3E%3C/path%3E%3C/svg%3E")`, backgroundRepeat: 'no-repeat', backgroundPosition: 'right 1rem center', backgroundSize: '1.25rem' }}
                            >
                                <option value="" disabled>Select Batch Number</option>
                                {batches.map(v => (
                                    <option key={v} value={v}>Batch {v}</option>
                                ))}
                            </select>
                        </div>

                        <div className="space-y-2">
                            <label className="text-sm font-bold text-slate-700 ml-1">
                                Academic Year
                            </label>
                            <select
                                value={academicYear}
                                onChange={(e) => setAcademicYear(e.target.value)}
                                className="input-field appearance-none"
                                style={{ backgroundImage: `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 24 24' stroke='%2364748b'%3E%3Cpath stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M19 9l-7 7-7-7'%3E%3C/path%3E%3C/svg%3E")`, backgroundRepeat: 'no-repeat', backgroundPosition: 'right 1rem center', backgroundSize: '1.25rem' }}
                            >
                                <option value="" disabled>Select Year</option>
                                {['2022/23', '2023/24', '2024/25', '2025/26'].map(y => (
                                    <option key={y} value={y}>{y}</option>
                                ))}
                            </select>
                        </div>
                    </div>

                    {/* Upload Box */}
                    <div className="space-y-2">
                        <label className="text-sm font-bold text-slate-700 ml-1">
                            Document Upload
                        </label>
                        <div
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
                                    {file ? file.name : 'Drop your file here'}
                                </h3>
                                <p className="text-slate-500 text-sm">
                                    {file ? `${(file.size / 1024).toFixed(1)} KB` : 'Click to browse or drag & drop (Excel, CSV)'}
                                </p>
                            </div>

                            {file && (
                                <button
                                    onClick={clearFile}
                                    className="mt-6 px-4 py-2 bg-slate-100 text-slate-600 rounded-lg text-xs font-bold hover:bg-red-50 hover:text-red-500 transition-colors"
                                >
                                    Select Different File
                                </button>
                            )}
                        </div>
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
                            disabled={loading || !file || !batch || !academicYear}
                            className={`w-full py-4 px-8 rounded-2xl text-white font-bold text-lg shadow-xl shadow-indigo-100 transition-all duration-300 transform active:scale-95 flex items-center justify-center gap-3
                                ${loading || !file || !batch || !academicYear
                                    ? 'bg-slate-300 cursor-not-allowed shadow-none'
                                    : 'bg-indigo-600 hover:bg-indigo-700 hover:shadow-indigo-200'}`}
                        >
                            {loading && (
                                <svg className="animate-spin h-5 w-5 text-white" fill="none" viewBox="0 0 24 24">
                                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                                </svg>
                            )}
                            {loading ? 'Processing Data...' : 'Confirm and Upload'}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}
