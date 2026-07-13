import React, { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Header from '../components/header';
import Footer from '../components/footer';
import axios from 'axios';

// ─── Palette ──────────────────────────────────────────────────────────────────
const COLORS = ['#6366f1', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4'];

// ─── SVG Bar Chart ────────────────────────────────────────────────────────────
function BarChart({ data, valueKey, color }) {
    if (!data || !data.length) return (
        <div className="h-52 flex items-center justify-center text-slate-400 text-sm">No data to display</div>
    );

    const W = 560, H = 220, PL = 44, PR = 16, PT = 28, PB = 44;
    const innerH = H - PT - PB;
    const innerW = W - PL - PR;
    const slotW = innerW / data.length;
    const barW = Math.min(slotW * 0.55, 70);
    const gridLines = [0, 25, 50, 75, 100];

    return (
        <svg viewBox={`0 0 ${W} ${H}`} className="w-full">
            {gridLines.map(v => {
                const y = PT + innerH - (v / 100) * innerH;
                return (
                    <g key={v}>
                        <line x1={PL} y1={y} x2={W - PR} y2={y} stroke="#e2e8f0" strokeWidth="1" />
                        <text x={PL - 6} y={y + 4} textAnchor="end" fontSize="9" fill="#94a3b8">{v}%</text>
                    </g>
                );
            })}
            {data.map((d, i) => {
                const val = Math.max(0, Math.min(100, Number(d[valueKey]) || 0));
                const bH = Math.max(2, (val / 100) * innerH);
                const x = PL + i * slotW + (slotW - barW) / 2;
                const y = PT + innerH - bH;
                return (
                    <g key={i}>
                        <rect x={x} y={y} width={barW} height={bH} fill={color} rx="6" opacity="0.85" />
                        <text x={x + barW / 2} y={Math.max(PT + 4, y - 5)} textAnchor="middle" fontSize="10" fill="#1e293b" fontWeight="700">
                            {val.toFixed(1)}%
                        </text>
                        <text x={x + barW / 2} y={H - 4} textAnchor="middle" fontSize="9" fill="#64748b">
                            {d.year || d.batch}
                        </text>
                    </g>
                );
            })}
        </svg>
    );
}

// ─── SVG Pass/Fail Pie Chart ──────────────────────────────────────────────────
function PassFailPieChart({ passRate }) {
    const CX = 130, CY = 130, R = 108;
    const pass = Math.max(0, Math.min(100, Number(passRate) || 0));
    const fail = 100 - pass;

    // Full-pass: single green circle
    if (fail < 0.5) {
        return (
            <svg viewBox="0 0 260 260" className="w-full max-w-[220px] mx-auto">
                <circle cx={CX} cy={CY} r={R} fill="#10b981" opacity="0.88" />
                <text x={CX} y={CY - 10} textAnchor="middle" dominantBaseline="middle" fontSize="22" fill="white" fontWeight="900">{pass.toFixed(1)}%</text>
                <text x={CX} y={CY + 16} textAnchor="middle" dominantBaseline="middle" fontSize="11" fill="white" opacity="0.85">Pass</text>
            </svg>
        );
    }
    // Full-fail: single red circle
    if (pass < 0.5) {
        return (
            <svg viewBox="0 0 260 260" className="w-full max-w-[220px] mx-auto">
                <circle cx={CX} cy={CY} r={R} fill="#ef4444" opacity="0.88" />
                <text x={CX} y={CY - 10} textAnchor="middle" dominantBaseline="middle" fontSize="22" fill="white" fontWeight="900">0%</text>
                <text x={CX} y={CY + 16} textAnchor="middle" dominantBaseline="middle" fontSize="11" fill="white" opacity="0.85">Pass</text>
            </svg>
        );
    }

    // Two slices
    const passAngle = (pass / 100) * 2 * Math.PI;
    const a0 = -Math.PI / 2;
    const a1Pass = a0 + passAngle;
    const safeA1 = (Math.abs(passAngle - 2 * Math.PI) < 1e-6) ? a1Pass - 1e-5 : a1Pass;

    const px0 = CX + R * Math.cos(a0),    py0 = CY + R * Math.sin(a0);
    const px1 = CX + R * Math.cos(safeA1), py1 = CY + R * Math.sin(safeA1);
    const largePass = passAngle > Math.PI ? 1 : 0;

    // Fail slice goes the other way
    const fx0 = px1, fy0 = py1;
    const fx1 = px0, fy1 = py0;
    const largeFail = passAngle < Math.PI ? 1 : 0;

    const passMid = a0 + passAngle / 2;
    const failMid = a0 + passAngle + (2 * Math.PI - passAngle) / 2;
    const plx = CX + R * 0.62 * Math.cos(passMid), ply = CY + R * 0.62 * Math.sin(passMid);
    const flx = CX + R * 0.62 * Math.cos(failMid), fly = CY + R * 0.62 * Math.sin(failMid);

    return (
        <svg viewBox="0 0 260 260" className="w-full max-w-[220px] mx-auto">
            {/* Pass slice — green */}
            <path d={`M${CX},${CY} L${px0},${py0} A${R},${R} 0 ${largePass} 1 ${px1},${py1} Z`}
                fill="#10b981" opacity="0.88" stroke="white" strokeWidth="2" />
            {/* Fail slice — red */}
            <path d={`M${CX},${CY} L${fx0},${fy0} A${R},${R} 0 ${largeFail} 1 ${fx1},${fy1} Z`}
                fill="#ef4444" opacity="0.88" stroke="white" strokeWidth="2" />
            {/* Pass label */}
            {passAngle > 0.4 && (
                <text x={plx} y={ply} textAnchor="middle" dominantBaseline="middle" fontSize="12" fill="white" fontWeight="800">
                    {pass.toFixed(1)}%
                </text>
            )}
            {/* Fail label */}
            {(2 * Math.PI - passAngle) > 0.4 && (
                <text x={flx} y={fly} textAnchor="middle" dominantBaseline="middle" fontSize="12" fill="white" fontWeight="800">
                    {fail.toFixed(1)}%
                </text>
            )}
        </svg>
    );
}

// ─── SVG Line Chart ───────────────────────────────────────────────────────────
function LineChart({ data }) {
    if (!data || !data.length) return (
        <div className="h-52 flex items-center justify-center text-slate-400 text-sm">No data to display</div>
    );

    const W = 560, H = 220, PL = 44, PR = 16, PT = 28, PB = 56;
    const innerH = H - PT - PB;
    const innerW = W - PL - PR;
    const n = data.length;

    const pts = data.map((d, i) => ({
        x: PL + (n === 1 ? innerW / 2 : (i / (n - 1)) * innerW),
        y: PT + innerH - (Math.max(0, Math.min(100, Number(d.value) || 0)) / 100) * innerH,
        ...d,
    }));

    const polyline = pts.map(p => `${p.x},${p.y}`).join(' ');

    return (
        <svg viewBox={`0 0 ${W} ${H}`} className="w-full">
            {[0, 25, 50, 75, 100].map(v => {
                const y = PT + innerH - (v / 100) * innerH;
                return (
                    <g key={v}>
                        <line x1={PL} y1={y} x2={W - PR} y2={y} stroke="#e2e8f0" strokeWidth="1" />
                        <text x={PL - 6} y={y + 4} textAnchor="end" fontSize="9" fill="#94a3b8">{v}%</text>
                    </g>
                );
            })}
            {/* Area fill */}
            {pts.length > 1 && (
                <polygon
                    points={`${pts[0].x},${PT + innerH} ${polyline} ${pts[pts.length - 1].x},${PT + innerH}`}
                    fill="#6366f1" opacity="0.08"
                />
            )}
            {pts.length > 1 && (
                <polyline points={polyline} fill="none" stroke="#6366f1" strokeWidth="2.5" strokeLinejoin="round" strokeLinecap="round" />
            )}
            {pts.map((p, i) => (
                <g key={i}>
                    <circle cx={p.x} cy={p.y} r="5.5" fill="white" stroke="#6366f1" strokeWidth="2.5" />
                    <circle cx={p.x} cy={p.y} r="2.5" fill="#6366f1" />
                    <text x={p.x} y={p.y - 12} textAnchor="middle" fontSize="10" fill="#1e293b" fontWeight="700">
                        {Number(p.value).toFixed(1)}%
                    </text>
                    {/* Rotated X label */}
                    <text
                        x={p.x} y={PT + innerH + 14}
                        textAnchor="end" fontSize="8" fill="#64748b"
                        transform={`rotate(-35,${p.x},${PT + innerH + 14})`}
                    >
                        {p.label}
                    </text>
                </g>
            ))}
        </svg>
    );
}

// ─── Helpers ──────────────────────────────────────────────────────────────────
function mean(arr) {
    if (!arr.length) return 0;
    return arr.reduce((s, v) => s + v, 0) / arr.length;
}

// ─── Main Page ────────────────────────────────────────────────────────────────
export default function ComparisonPage() {
    const { loId } = useParams();
    const navigate = useNavigate();

    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const [loName, setLoName] = useState('');
    const [loIdDecoded, setLoIdDecoded] = useState('');

    // Raw API data
    const [allTrendData, setAllTrendData] = useState({});   // Map<LOkey, [{batch,year,average,...}]>
    const [allPassRate, setAllPassRate] = useState({});     // Map<LOkey, [{batch,year,passRate,...}]>

    // UI state
    const [metric, setMetric] = useState('average');         // 'average' | 'passRate'
    const [selectedBatch, setSelectedBatch] = useState(null);
    const [loMarksAvailable, setLoMarksAvailable] = useState([]);  // [{batch, markType, markCount}]
    const [moduleId, setModuleId] = useState(null);

    useEffect(() => {
        const load = async () => {
            try {
                setLoading(true);
                setError(null);
                const token = localStorage.getItem('token');
                const hdrs = { Authorization: `Bearer ${token}` };

                // 1. Fetch LO details
                const loRes = await axios.get(
                    `/api/lospos/${encodeURIComponent(loId)}`,
                    { headers: hdrs }
                );
                const lo = loRes.data.data || loRes.data;
                setLoName(lo.description || lo.name || loId);
                setLoIdDecoded(lo.id || loId);
                const modId = lo.moduleId || loRes.data.moduleId;
                setModuleId(modId);

                if (!modId) throw new Error('Could not determine module for this LO');

                // 2. Fetch trend, pass-rate, and available marks for this LO in parallel
                const [trendRes, passRes, marksRes] = await Promise.all([
                    axios.get(`/api/obe/analysis/trend/lo/${modId}`, { headers: hdrs }),
                    axios.get(`/api/obe/analysis/pass-rate/lo/${modId}?threshold=50`, { headers: hdrs }),
                    axios.get(`/api/obe/marks/available/lo/${encodeURIComponent(loId)}`, { headers: hdrs }),
                ]);

                setAllTrendData(trendRes.data || {});
                setAllPassRate(passRes.data || {});
                setLoMarksAvailable(marksRes.data?.data || []);
            } catch (err) {
                console.error(err);
                setError('Could not load comparison data. Make sure marks have been uploaded for this module.');
            } finally {
                setLoading(false);
            }
        };
        load();
    }, [loId]);

    // Find this LO's key in the maps
    const loTrendKey = useMemo(() => {
        if (!loIdDecoded || !allTrendData) return null;
        return Object.keys(allTrendData).find(k => k.startsWith(loIdDecoded + ' -')) || null;
    }, [loIdDecoded, allTrendData]);

    const loPassKey = useMemo(() => {
        if (!loIdDecoded || !allPassRate) return null;
        return Object.keys(allPassRate).find(k => k.startsWith(loIdDecoded + ' -')) || null;
    }, [loIdDecoded, allPassRate]);

    // This LO's trend data — last 5 batches
    const loTrend = useMemo(() => {
        if (!loTrendKey) return [];
        return (allTrendData[loTrendKey] || []).slice(-5);
    }, [loTrendKey, allTrendData]);

    // This LO's pass rate data — last 5 batches
    const loPassRate = useMemo(() => {
        if (!loPassKey) return [];
        return (allPassRate[loPassKey] || []).slice(-5);
    }, [loPassKey, allPassRate]);

    // Bar chart data based on selected metric
    const barData = useMemo(() => {
        if (metric === 'average') {
            return loTrend.map(d => ({ batch: d.batch, year: d.year, value: d.average }));
        }
        return loPassRate.map(d => ({ batch: d.batch, year: d.year, value: d.passRate }));
    }, [metric, loTrend, loPassRate]);

    // Pie chart: pass rate for the selected batch (or latest batch)
    const activePieBatch = selectedBatch ?? (loPassRate.length > 0 ? loPassRate[loPassRate.length - 1].batch : null);
    const activePieBatchRow = useMemo(() => loPassRate.find(d => String(d.batch) === String(activePieBatch)), [loPassRate, activePieBatch]);

    // Line chart data — ALL LOs in module for the selected batch
    const lineData = useMemo(() => {
        if (!selectedBatch || !allTrendData) return [];
        return Object.entries(allTrendData)
            .map(([key, arr]) => {
                const loLabel = key.split(' - ')[0];
                const row = arr.find(r => String(r.batch) === String(selectedBatch));
                return { label: loLabel, value: row ? Number(row.average) : 0 };
            })
            .filter(d => d.value > 0)
            .sort((a, b) => a.label.localeCompare(b.label));
    }, [selectedBatch, allTrendData]);

    const downloadBatchMarks = async (batchVal, markTypeVal) => {
        try {
            const token = localStorage.getItem('token');
            const res = await fetch(
                `/api/obe/marks/export/module/${moduleId}?batch=${batchVal}&markType=${markTypeVal}&threshold=50`,
                { headers: { Authorization: `Bearer ${token}` } }
            );
            if (!res.ok) return;
            const blob = await res.blob();
            const cd = res.headers.get('content-disposition');
            const m = cd?.match(/filename\*?=(?:UTF-8''|")?([^";]+)/i);
            const fname = m?.[1] ? decodeURIComponent(m[1].replace(/"/g, '').trim()) : `marks_${batchVal}_${markTypeVal}.xlsx`;
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a'); a.href = url; a.download = fname;
            document.body.appendChild(a); a.click(); a.remove(); URL.revokeObjectURL(url);
        } catch { /* ignore */ }
    };

    const hasData = loTrend.length > 0 || loPassRate.length > 0;
    const latestAvg = loTrend.length ? loTrend[loTrend.length - 1].average : null;
    const latestPass = loPassRate.length ? loPassRate[loPassRate.length - 1].passRate : null;

    return (
        <div className="min-h-screen bg-[#f8fafc] flex flex-col relative overflow-hidden">
            <Header />
            <div className="absolute top-0 right-0 w-[500px] h-[500px] bg-indigo-500/5 rounded-full blur-[120px] pointer-events-none" />
            <div className="absolute bottom-0 left-0 w-[500px] h-[500px] bg-emerald-500/5 rounded-full blur-[120px] pointer-events-none" />

            <main className="flex-1 max-w-7xl mx-auto px-6 py-12 w-full relative z-10 animate-in fade-in duration-700">

                {/* Page header */}
                <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-6 mb-12">
                    <div className="space-y-2">
                        <button
                            onClick={() => navigate(-1)}
                            className="group flex items-center text-slate-500 hover:text-indigo-600 font-bold transition-all duration-300"
                        >
                            <div className="p-2 bg-white rounded-xl shadow-sm border border-slate-100 mr-3 group-hover:bg-indigo-50 transition-colors">
                                <svg className="w-5 h-5 group-hover:-translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
                                </svg>
                            </div>
                            Back
                        </button>
                        <h1 className="heading-xl bg-clip-text text-transparent bg-gradient-to-r from-slate-900 to-slate-600">
                            Performance Analytics
                        </h1>
                    </div>
                </div>

                {loading ? (
                    <div className="flex flex-col items-center justify-center py-24 glass-card rounded-[2rem]">
                        <div className="relative">
                            <div className="w-16 h-16 border-4 border-indigo-100 rounded-full" />
                            <div className="w-16 h-16 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin absolute top-0 left-0" />
                        </div>
                        <p className="text-slate-500 font-bold mt-8 tracking-wide">Compiling comparative data…</p>
                    </div>
                ) : error ? (
                    <div className="glass-card bg-red-50/50 border-red-100 p-12 rounded-[2rem] text-center">
                        <div className="w-16 h-16 bg-red-100 text-red-600 rounded-2xl flex items-center justify-center mx-auto mb-6">
                            <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                            </svg>
                        </div>
                        <h2 className="text-xl font-bold text-red-800 mb-2">Analysis Failed</h2>
                        <p className="text-red-600/70">{error}</p>
                    </div>
                ) : !hasData ? (
                    <div className="glass-card p-16 rounded-[2rem] text-center max-w-2xl mx-auto border-dashed border-2">
                        <div className="w-20 h-20 bg-slate-50 text-slate-300 rounded-3xl flex items-center justify-center mx-auto mb-8">
                            <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                            </svg>
                        </div>
                        <h2 className="heading-lg mb-4">No data available yet</h2>
                        <p className="text-slate-500 leading-relaxed">
                            Upload student marks for <span className="text-indigo-600 font-bold">{loName}</span> using the Marks &amp; Analytics workflow to see charts here.
                        </p>
                    </div>
                ) : (
                    <div className="space-y-8">

                        {/* ── Summary header ── */}
                        <div className="glass-card p-8 rounded-[2rem] border-l-8 border-l-indigo-500 flex flex-col md:flex-row md:items-center justify-between gap-6">
                            <div>
                                <span className="text-xs font-bold text-indigo-600 uppercase tracking-[0.2em] mb-2 block">Learning Outcome</span>
                                <h2 className="heading-lg">{loName}</h2>
                                <p className="text-slate-500 mt-1 text-sm">Cross-batch performance visualization · last {Math.max(loTrend.length, loPassRate.length)} batches</p>
                            </div>
                            <div className="flex gap-4 flex-shrink-0">
                                {latestAvg !== null && (
                                    <div className="px-6 py-3 bg-white/60 rounded-2xl border border-indigo-100 shadow-sm text-center">
                                        <div className="text-[10px] font-bold text-indigo-500 uppercase tracking-wider mb-1">Latest Avg</div>
                                        <div className="text-2xl font-black text-indigo-600">{Number(latestAvg).toFixed(1)}%</div>
                                    </div>
                                )}
                                {latestPass !== null && (
                                    <div className="px-6 py-3 bg-white/60 rounded-2xl border border-emerald-100 shadow-sm text-center">
                                        <div className="text-[10px] font-bold text-emerald-500 uppercase tracking-wider mb-1">Pass Rate</div>
                                        <div className="text-2xl font-black text-emerald-600">{Number(latestPass).toFixed(1)}%</div>
                                    </div>
                                )}
                            </div>
                        </div>

                        {/* ── Bar Chart ── */}
                        <div className="glass-card p-8 rounded-[2.5rem] border-slate-100">
                            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
                                <div>
                                    <span className="text-[10px] font-black text-indigo-600 uppercase tracking-[0.2em] mb-1 block">Last {barData.length} Batches</span>
                                    <h2 className="text-xl font-black text-slate-800">Batch Performance Trend</h2>
                                </div>
                                <select
                                    value={metric}
                                    onChange={e => setMetric(e.target.value)}
                                    className="text-sm border border-slate-200 rounded-xl px-4 py-2 bg-white font-semibold text-slate-700 focus:outline-none focus:ring-2 focus:ring-indigo-300"
                                >
                                    <option value="average">Average Score</option>
                                    <option value="passRate">Pass Rate %</option>
                                </select>
                            </div>
                            <BarChart
                                data={barData}
                                valueKey="value"
                                color={metric === 'average' ? '#6366f1' : '#10b981'}
                            />
                            {/* Legend */}
                            <div className="flex items-center gap-3 mt-4 justify-end">
                                <span className="w-3 h-3 rounded-full" style={{ backgroundColor: metric === 'average' ? '#6366f1' : '#10b981' }} />
                                <span className="text-xs font-semibold text-slate-500">
                                    {metric === 'average' ? 'Average Score (%)' : 'Pass Rate (%, threshold 50%)'}
                                </span>
                            </div>
                        </div>

                        {/* ── Pie + Line ── */}
                        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">

                            {/* Pie chart */}
                            <div className="glass-card p-8 rounded-[2.5rem] border-slate-100">
                                <div className="mb-4">
                                    <span className="text-[10px] font-black text-emerald-600 uppercase tracking-[0.2em] mb-1 block">Pass vs Fail</span>
                                    <h2 className="text-xl font-black text-slate-800">
                                        {activePieBatchRow
                                            ? `Batch ${activePieBatch} — Pass Rate`
                                            : 'Select a batch'}
                                    </h2>
                                    <p className="text-xs text-slate-400 mt-1">Select a batch below to drill into all LOs</p>
                                </div>

                                {/* Batch selector pills */}
                                <div className="flex flex-wrap gap-2 mb-4">
                                    {loPassRate.map(d => (
                                        <button key={d.batch}
                                            onClick={() => setSelectedBatch(prev => prev === d.batch ? null : d.batch)}
                                            className={`px-3 py-1.5 rounded-xl text-[11px] font-black transition-all border ${
                                                String(activePieBatch) === String(d.batch)
                                                    ? 'bg-slate-800 text-white border-slate-800'
                                                    : 'bg-white text-slate-600 border-slate-200 hover:border-slate-400'
                                            }`}>
                                            {d.year || d.batch}
                                        </button>
                                    ))}
                                </div>

                                {!activePieBatchRow ? (
                                    <div className="h-52 flex items-center justify-center text-slate-400 text-sm">
                                        No pass rate data. Upload marks first.
                                    </div>
                                ) : (
                                    <>
                                        <PassFailPieChart passRate={activePieBatchRow.passRate} />
                                        {/* Legend */}
                                        <div className="flex gap-4 mt-4 justify-center">
                                            <div className="flex items-center gap-2">
                                                <span className="w-3 h-3 rounded-full bg-emerald-500 flex-shrink-0" />
                                                <span className="text-sm font-bold text-slate-700">Pass — {Number(activePieBatchRow.passRate).toFixed(1)}%</span>
                                            </div>
                                            <div className="flex items-center gap-2">
                                                <span className="w-3 h-3 rounded-full bg-red-500 flex-shrink-0" />
                                                <span className="text-sm font-bold text-slate-700">Fail — {(100 - Number(activePieBatchRow.passRate)).toFixed(1)}%</span>
                                            </div>
                                        </div>
                                    </>
                                )}
                            </div>

                            {/* Line chart */}
                            <div className="glass-card p-8 rounded-[2.5rem] border-slate-100">
                                <div className="mb-5">
                                    <span className="text-[10px] font-black text-amber-600 uppercase tracking-[0.2em] mb-1 block">LO Breakdown</span>
                                    <h2 className="text-xl font-black text-slate-800">
                                        {selectedBatch
                                            ? `${selectedBatch}nd Batch — All LOs`
                                            : 'Select a batch from the pie chart'}
                                    </h2>
                                    <p className="text-xs text-slate-400 mt-1">Average score per Learning Outcome in this module</p>
                                </div>

                                {!selectedBatch ? (
                                    <div className="h-52 flex flex-col items-center justify-center text-slate-300 gap-4">
                                        <svg className="w-14 h-14" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M15 15l-2 5L9 9l11 4-5 2zm0 0l5 5" />
                                        </svg>
                                        <span className="text-sm font-medium text-slate-400">Click a pie segment to drill down</span>
                                    </div>
                                ) : lineData.length === 0 ? (
                                    <div className="h-52 flex items-center justify-center text-slate-400 text-sm">
                                        No data for batch {selectedBatch}.
                                    </div>
                                ) : (
                                    <LineChart data={lineData} />
                                )}
                            </div>
                        </div>

                        {/* ── Available Marks per Batch ── */}
                        {loMarksAvailable.length > 0 && (
                            <div>
                                <h3 className="text-lg font-black text-slate-700 mb-4 px-1">Available Uploaded Marks</h3>
                                <div className="glass-card p-6 rounded-[2rem] border-slate-100">
                                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                                        {Object.entries(
                                            loMarksAvailable.reduce((acc, m) => {
                                                const b = String(m.batch);
                                                if (!acc[b]) acc[b] = [];
                                                acc[b].push(m);
                                                return acc;
                                            }, {})
                                        ).sort(([a], [b]) => b.localeCompare(a)).map(([batchVal, entries]) => (
                                            <div key={batchVal} className="rounded-2xl border border-slate-200 bg-white/80 p-4">
                                                <div className="flex items-center gap-2 mb-3">
                                                    <span className="w-8 h-8 rounded-lg bg-indigo-600 text-white text-xs font-black flex items-center justify-center">{batchVal}</span>
                                                    <span className="font-black text-slate-800">{batchVal}th Batch</span>
                                                </div>
                                                <div className="space-y-2">
                                                    {entries.map(entry => (
                                                        <div key={entry.markType} className="flex items-center justify-between">
                                                            <div>
                                                                <span className={`text-[10px] font-black uppercase tracking-wider px-2 py-0.5 rounded ${entry.markType === 'FINAL_EXAM' ? 'bg-indigo-50 text-indigo-700' : 'bg-emerald-50 text-emerald-700'}`}>
                                                                    {entry.markType === 'FINAL_EXAM' ? 'Final Exam' : 'Assignment'}
                                                                </span>
                                                                <span className="ml-2 text-xs text-slate-500">{entry.markCount} records</span>
                                                            </div>
                                                            <button
                                                                onClick={() => downloadBatchMarks(batchVal, entry.markType)}
                                                                className="flex items-center gap-1 px-2 py-1 rounded-lg bg-slate-100 text-slate-600 text-[10px] font-bold hover:bg-indigo-50 hover:text-indigo-700 transition-colors">
                                                                <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                                                                </svg>
                                                                Excel
                                                            </button>
                                                        </div>
                                                    ))}
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            </div>
                        )}

                        {/* ── Detail cards (original, kept) ── */}
                        {loTrend.length > 0 && (
                            <div>
                                <h3 className="text-lg font-black text-slate-700 mb-4 px-1">Batch Detail Cards</h3>
                                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                                    {loTrend.map(data => (
                                        <div key={data.year} className="glass-card hover:bg-white transition-all duration-500 group rounded-[2rem] p-8 border-slate-100 hover:shadow-2xl hover:shadow-indigo-500/5 hover:-translate-y-2">
                                            <div className="flex justify-between items-center mb-8">
                                                <div className="px-4 py-1.5 bg-slate-50 text-slate-600 rounded-xl text-[10px] font-black uppercase tracking-widest border border-slate-100 group-hover:bg-indigo-50 group-hover:text-indigo-600 transition-colors">
                                                    {data.year}
                                                </div>
                                                <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${data.status === 'IMPROVED' ? 'bg-emerald-50 text-emerald-600' : data.status === 'DECLINED' ? 'bg-red-50 text-red-600' : 'bg-blue-50 text-blue-600'}`}>
                                                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d={
                                                            data.status === 'IMPROVED' ? 'M13 7h8m0 0v8m0-8l-8 8-4-4-6 6' :
                                                            data.status === 'DECLINED' ? 'M13 17h8m0 0v-8m0 8l-8-8-4 4-6-6' :
                                                            'M5 12h14'
                                                        } />
                                                    </svg>
                                                </div>
                                            </div>
                                            <div className="space-y-1">
                                                <div className="text-5xl font-black text-slate-900 tracking-tighter">
                                                    {Number(data.average).toFixed(1)}<span className="text-2xl text-slate-300 ml-1">%</span>
                                                </div>
                                                <p className="text-slate-400 text-[10px] font-bold uppercase tracking-widest">Average Attainment Score</p>
                                            </div>
                                            {data.delta !== undefined && data.delta !== null && (
                                                <div className="mt-8 pt-6 border-t border-slate-50 flex items-center justify-between">
                                                    <span className="text-slate-500 text-xs font-medium">Progress vs previous</span>
                                                    <div className={`flex items-center font-black text-sm ${Number(data.delta) >= 0 ? 'text-emerald-500' : 'text-red-500'}`}>
                                                        <svg className={`w-4 h-4 mr-1 ${Number(data.delta) < 0 ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M5 15l7-7 7 7" />
                                                        </svg>
                                                        {Math.abs(Number(data.delta)).toFixed(1)}%
                                                    </div>
                                                </div>
                                            )}
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}

                    </div>
                )}
            </main>
            <Footer />
        </div>
    );
}
