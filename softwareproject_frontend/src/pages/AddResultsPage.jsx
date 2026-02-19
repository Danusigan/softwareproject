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
        <div className="min-h-screen bg-gray-100 flex items-center justify-center p-4">
            {/* Main Container - The Blue Frame from screenshot */}
            <div className="w-full max-w-4xl bg-white border-[3px] border-[#3b82f6] shadow-2xl relative overflow-hidden flex flex-col items-center py-10 px-6 sm:px-12 md:px-20 min-h-[600px]">

                {/* Close Button 'X' (Top Right) */}
                <button
                    onClick={() => navigate(-1)}
                    className="absolute top-4 right-6 text-black hover:text-gray-600 transition-colors z-10"
                >
                    <svg className="w-12 h-12" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M6 18L18 6M6 6l12 12" />
                    </svg>
                </button>

                {/* Header Title */}
                <h1 className="text-4xl sm:text-5xl font-bold text-center mb-16 text-black tracking-tight mt-4">
                    Add Your activity results here {loNumber && <span className="text-blue-600"> (LO {loNumber})</span>}
                </h1>

                {/* Content Section */}
                <div className="w-full space-y-12 flex flex-col items-start max-w-2xl mx-auto">

                    {/* Batch Selection section */}
                    <div className="w-full flex flex-col sm:flex-row gap-8">
                        <div className="flex-1 space-y-4">
                            <label className="text-3xl sm:text-4xl font-normal text-black block text-left">
                                The batch
                            </label>
                            <select
                                value={batch}
                                onChange={(e) => setBatch(e.target.value)}
                                className="w-full bg-[#f0f5ff] text-[#6b7280] px-6 py-4 rounded-sm border-none shadow-sm focus:ring-0 text-xl font-serif italic"
                            >
                                <option value="" disabled>Select batch</option>
                                {batches.map(v => (
                                    <option key={v} value={v} className="not-italic text-black font-sans">{v}</option>
                                ))}
                            </select>
                        </div>

                        <div className="flex-1 space-y-4">
                            <label className="text-3xl sm:text-4xl font-normal text-black block text-left">
                                Academic Year
                            </label>
                            <select
                                value={academicYear}
                                onChange={(e) => setAcademicYear(e.target.value)}
                                className="w-full bg-[#f0f5ff] text-[#6b7280] px-6 py-4 rounded-sm border-none shadow-sm focus:ring-0 text-xl font-serif italic"
                            >
                                <option value="" disabled>Select year</option>
                                <option value="2022/23" className="not-italic text-black font-sans">2022/23</option>
                                <option value="2023/24" className="not-italic text-black font-sans">2023/24</option>
                                <option value="2024/25" className="not-italic text-black font-sans">2024/25</option>
                                <option value="2025/26" className="not-italic text-black font-sans">2025/26</option>
                            </select>
                        </div>
                    </div>

                    {/* Upload Results section */}
                    <div className="w-full space-y-4">
                        <label className="text-3xl sm:text-4xl font-normal text-black block text-left">
                            Upload the results
                        </label>

                        {/* Drag and Drop Container */}
                        <div className="relative w-full">
                            {/* Inner Cross button to clear file if needed (from screenshot) */}
                            {file && (
                                <button
                                    onClick={clearFile}
                                    className="absolute -top-6 left-[50%] transform -translate-x-1/2 text-black hover:text-red-500 z-20"
                                >
                                    <svg className="w-6 h-6 font-bold" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M6 18L18 6M6 6l12 12" />
                                    </svg>
                                </button>
                            )}
                            {/* The specific 'x' from the design above the text area */}
                            {!file && (
                                <span className="absolute -top-8 left-1/2 -translate-x-1/2 text-black text-2xl font-bold">×</span>
                            )}

                            <div
                                className={`w-full h-80 bg-[#f0f5ff] transition-all duration-300 flex items-center justify-center cursor-pointer border border-transparent
                                    ${dragActive ? 'bg-blue-50 ring-2 ring-blue-300' : ''}`}
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

                                <div className="text-center p-4">
                                    <h3 className="text-5xl sm:text-6xl font-serif font-light text-black px-4 leading-[1.3] text-center max-w-lg">
                                        {file ? file.name : 'Drag and drop the file here'}
                                    </h3>
                                    {loading && <div className="mt-4 animate-pulse text-blue-600 font-bold">Uploading...</div>}
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Success/Error Message */}
                    {message.text && (
                        <div className={`w-full text-center p-3 rounded-md text-lg font-semibold ${message.type === 'success' ? 'text-green-600' : 'text-red-600'}`}>
                            {message.text}
                        </div>
                    )}

                    {/* Action Button */}
                    <div className="w-full flex justify-center py-6">
                        <button
                            onClick={handleUpload}
                            disabled={loading || !file || !batch || !academicYear}
                            className={`px-16 py-4 rounded-xl text-white font-medium text-2xl shadow-md transition-all transform active:scale-95
                                ${loading || !file || !batch || !academicYear
                                    ? 'bg-[#1d63ed] opacity-60 cursor-not-allowed'
                                    : 'bg-[#1d63ed] hover:bg-[#1557d1] hover:shadow-xl'}`}
                        >
                            Upload the file
                        </button>
                    </div>
                </div>
            </div>

            {/* Global style for fonts if not already loaded */}
            <style dangerouslySetInnerHTML={{
                __html: `
                @import url('https://fonts.googleapis.com/css2?family=Playfair+Display:ital,wght@0,400;0,700;1,400&family=Inter:wght@400;700&display=swap');
                
                .font-serif {
                    font-family: 'Playfair Display', serif;
                }
                .font-sans {
                    font-family: 'Inter', sans-serif;
                }
            `}} />
        </div>
    );
}
