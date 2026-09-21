import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import axios from 'axios';
import PropTypes from 'prop-types';
import Header from '../components/header';
import Footer from '../components/footer';

const API_BASE = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '');
const EMPTY_DISTRIBUTION = { labels: ['0-39', '40-49', '50-59', '60-69', '70-79', '80-100'], data: [0, 0, 0, 0, 0, 0] };

const percent = (value) => `${Number(value || 0).toFixed(1)}%`;
const numberValue = (value) => Number.isFinite(Number(value)) ? Number(value) : 0;

function EmptyChart({ message = 'No data matches the selected filters.' }) {
    return <div className="min-h-56 flex items-center justify-center text-center text-sm font-medium text-slate-400 px-6">{message}</div>;
}

function StatusBadge({ status }) {
    const styles = {
        TARGET_MET: 'bg-emerald-50 text-emerald-700 border-emerald-200',
        IMPROVED: 'bg-emerald-50 text-emerald-700 border-emerald-200',
        WATCH: 'bg-amber-50 text-amber-700 border-amber-200',
        STABLE: 'bg-blue-50 text-blue-700 border-blue-200',
        BASELINE: 'bg-slate-50 text-slate-600 border-slate-200',
        ACTION_REQUIRED: 'bg-red-50 text-red-700 border-red-200',
        DECLINED: 'bg-red-50 text-red-700 border-red-200',
    };
    return (
        <span className={`inline-flex rounded-full border px-2.5 py-1 text-[10px] font-black tracking-wide ${styles[status] || styles.BASELINE}`}>
            {String(status || 'BASELINE').replaceAll('_', ' ')}
        </span>
    );
}

function StatCard({ label, value, detail, tone = 'indigo' }) {
    const tones = {
        indigo: 'bg-indigo-50 text-indigo-700',
        emerald: 'bg-emerald-50 text-emerald-700',
        amber: 'bg-amber-50 text-amber-700',
        slate: 'bg-slate-100 text-slate-700',
    };
    return (
        <div className="glass-card rounded-3xl p-6 border border-slate-100">
            <div className={`inline-flex rounded-xl px-3 py-1 text-[10px] font-black uppercase tracking-[0.16em] ${tones[tone]}`}>{label}</div>
            <div className="mt-4 text-3xl font-black tracking-tight text-slate-900">{value}</div>
            <p className="mt-1 text-xs font-medium text-slate-500">{detail}</p>
        </div>
    );
}

function Panel({ title, subtitle, children, action }) {
    return (
        <section className="glass-card rounded-[2rem] border border-slate-100 p-6 md:p-8">
            <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
                <div>
                    <h2 className="text-xl font-black text-slate-900">{title}</h2>
                    <p className="mt-1 text-xs font-medium text-slate-500">{subtitle}</p>
                </div>
                {action}
            </div>
            {children}
        </section>
    );
}

function BarChart({ data, valueKey = 'actual', target, ariaLabel }) {
    if (!data?.length) return <EmptyChart />;
    const width = Math.max(620, data.length * 92);
    const height = 300;
    const margin = { top: 28, right: 22, bottom: 66, left: 48 };
    const innerWidth = width - margin.left - margin.right;
    const innerHeight = height - margin.top - margin.bottom;
    const slot = innerWidth / data.length;
    const barWidth = Math.min(52, slot * 0.58);
    const targetY = margin.top + innerHeight - (numberValue(target) / 100) * innerHeight;

    return (
        <div className="overflow-x-auto pb-2">
            <svg viewBox={`0 0 ${width} ${height}`} style={{ minWidth: `${width}px` }} className="h-[300px] w-full" role="img" aria-label={ariaLabel}>
                <title>{ariaLabel}</title>
                {[0, 25, 50, 75, 100].map((tick) => {
                    const y = margin.top + innerHeight - (tick / 100) * innerHeight;
                    return (
                        <g key={tick}>
                            <line x1={margin.left} y1={y} x2={width - margin.right} y2={y} stroke="#e2e8f0" />
                            <text x={margin.left - 9} y={y + 4} textAnchor="end" fontSize="10" fill="#64748b">{tick}%</text>
                        </g>
                    );
                })}
                {target !== undefined && (
                    <g>
                        <line x1={margin.left} y1={targetY} x2={width - margin.right} y2={targetY} stroke="#dc2626" strokeWidth="2" strokeDasharray="6 5" />
                        <text x={width - margin.right} y={Math.max(13, targetY - 6)} textAnchor="end" fontSize="10" fontWeight="700" fill="#dc2626">Target {percent(target)}</text>
                    </g>
                )}
                {data.map((item, index) => {
                    const value = Math.max(0, Math.min(100, numberValue(item[valueKey] ?? item.value)));
                    const barHeight = (value / 100) * innerHeight;
                    const x = margin.left + index * slot + (slot - barWidth) / 2;
                    const y = margin.top + innerHeight - barHeight;
                    const met = target === undefined || value >= numberValue(target);
                    return (
                        <g key={`${item.label}-${index}`}>
                            <rect x={x} y={y} width={barWidth} height={Math.max(2, barHeight)} rx="7" fill={met ? '#4f46e5' : '#f59e0b'}>
                                <title>{`${item.label}: ${percent(value)}`}</title>
                            </rect>
                            <text x={x + barWidth / 2} y={Math.max(15, y - 7)} textAnchor="middle" fontSize="10" fontWeight="800" fill="#334155">{percent(value)}</text>
                            <text x={x + barWidth / 2} y={height - 36} textAnchor="middle" fontSize="10" fontWeight="700" fill="#475569">{String(item.label || '').slice(0, 12)}</text>
                            <text x={x + barWidth / 2} y={height - 20} textAnchor="middle" fontSize="9" fill="#94a3b8">n={item.studentCount ?? item.mappedLoCount ?? '-'}</text>
                        </g>
                    );
                })}
            </svg>
        </div>
    );
}

function LineChart({ data, target, ariaLabel }) {
    if (!data?.length) return <EmptyChart message="Trend data needs at least one uploaded batch." />;
    const width = Math.max(620, data.length * 110);
    const height = 300;
    const margin = { top: 26, right: 28, bottom: 58, left: 48 };
    const innerWidth = width - margin.left - margin.right;
    const innerHeight = height - margin.top - margin.bottom;
    const points = data.map((item, index) => ({
        x: data.length === 1 ? margin.left + innerWidth / 2 : margin.left + (index / (data.length - 1)) * innerWidth,
        y: margin.top + innerHeight - (Math.max(0, Math.min(100, numberValue(item.average))) / 100) * innerHeight,
        item,
    }));
    const path = points.map(point => `${point.x},${point.y}`).join(' ');
    const targetY = margin.top + innerHeight - (numberValue(target) / 100) * innerHeight;

    return (
        <div className="overflow-x-auto pb-2">
            <svg viewBox={`0 0 ${width} ${height}`} style={{ minWidth: `${width}px` }} className="h-[300px] w-full" role="img" aria-label={ariaLabel}>
                <title>{ariaLabel}</title>
                {[0, 25, 50, 75, 100].map((tick) => {
                    const y = margin.top + innerHeight - (tick / 100) * innerHeight;
                    return <g key={tick}><line x1={margin.left} y1={y} x2={width - margin.right} y2={y} stroke="#e2e8f0" /><text x={margin.left - 9} y={y + 4} textAnchor="end" fontSize="10" fill="#64748b">{tick}%</text></g>;
                })}
                <line x1={margin.left} y1={targetY} x2={width - margin.right} y2={targetY} stroke="#dc2626" strokeWidth="2" strokeDasharray="6 5" />
                {points.length > 1 && <polyline points={path} fill="none" stroke="#4f46e5" strokeWidth="3" strokeLinejoin="round" strokeLinecap="round" />}
                {points.map(({ x, y, item }) => (
                    <g key={item.batch}>
                        <circle cx={x} cy={y} r="6" fill="#4f46e5" stroke="white" strokeWidth="3"><title>{`${item.year}: ${percent(item.average)}, ${item.studentCount} student results`}</title></circle>
                        <text x={x} y={Math.max(14, y - 13)} textAnchor="middle" fontSize="10" fontWeight="800" fill="#334155">{percent(item.average)}</text>
                        <text x={x} y={height - 31} textAnchor="middle" fontSize="10" fontWeight="700" fill="#475569">{item.year}</text>
                        <text x={x} y={height - 15} textAnchor="middle" fontSize="9" fill="#94a3b8">n={item.studentCount}</text>
                    </g>
                ))}
            </svg>
        </div>
    );
}

function DonutChart({ pass, fail, threshold }) {
    const total = pass + fail;
    const passRate = total ? (pass / total) * 100 : 0;
    const radius = 62;
    const circumference = 2 * Math.PI * radius;
    const passLength = (passRate / 100) * circumference;
    if (!total) return <EmptyChart />;
    return (
        <div className="flex flex-col items-center justify-center gap-5 md:flex-row md:gap-10">
            <svg width="190" height="190" viewBox="0 0 190 190" role="img" aria-label={`Pass rate ${percent(passRate)} at a ${percent(threshold)} threshold`}>
                <title>{`Pass ${pass}; Fail ${fail}; Pass rate ${percent(passRate)}`}</title>
                <circle cx="95" cy="95" r={radius} fill="none" stroke="#fee2e2" strokeWidth="22" />
                <circle cx="95" cy="95" r={radius} fill="none" stroke="#10b981" strokeWidth="22" strokeLinecap="round"
                    strokeDasharray={`${passLength} ${circumference}`} transform="rotate(-90 95 95)" />
                <text x="95" y="91" textAnchor="middle" fontSize="25" fontWeight="900" fill="#0f172a">{percent(passRate)}</text>
                <text x="95" y="112" textAnchor="middle" fontSize="10" fontWeight="700" fill="#64748b">PASS RATE</text>
            </svg>
            <div className="space-y-3 text-sm">
                <div className="flex min-w-40 items-center justify-between gap-8"><span className="flex items-center gap-2 text-slate-600"><span className="h-3 w-3 rounded-full bg-emerald-500" />Pass</span><strong>{pass}</strong></div>
                <div className="flex min-w-40 items-center justify-between gap-8"><span className="flex items-center gap-2 text-slate-600"><span className="h-3 w-3 rounded-full bg-red-200" />Fail</span><strong>{fail}</strong></div>
                <div className="border-t border-slate-100 pt-3 text-xs text-slate-500">Threshold: {percent(threshold)}</div>
            </div>
        </div>
    );
}

function DistributionChart({ distribution }) {
    const labels = distribution?.labels || EMPTY_DISTRIBUTION.labels;
    const values = distribution?.data || EMPTY_DISTRIBUTION.data;
    const maximum = Math.max(1, ...values.map(numberValue));
    if (!values.some(numberValue)) return <EmptyChart />;
    return (
        <div className="space-y-4" role="img" aria-label="Distribution of student results across six score bands">
            {labels.map((label, index) => {
                const value = numberValue(values[index]);
                return (
                    <div key={label}>
                        <div className="mb-1.5 flex justify-between text-xs font-bold text-slate-600"><span>{label}%</span><span>{value} results</span></div>
                        <div className="h-3 overflow-hidden rounded-full bg-slate-100"><div className="h-full rounded-full bg-gradient-to-r from-indigo-500 to-cyan-500" style={{ width: `${(value / maximum) * 100}%` }} /></div>
                    </div>
                );
            })}
        </div>
    );
}

function AccessibleTable({ caption, columns, rows }) {
    if (!rows?.length) return null;
    return (
        <details className="mt-5 border-t border-slate-100 pt-4">
            <summary className="cursor-pointer text-xs font-black uppercase tracking-wider text-indigo-600">View accessible data table</summary>
            <div className="mt-4 overflow-x-auto">
                <table className="min-w-full text-left text-sm">
                    <caption className="sr-only">{caption}</caption>
                    <thead><tr className="border-b border-slate-200">{columns.map(column => <th key={column.key} scope="col" className="px-3 py-2 text-xs font-black uppercase tracking-wide text-slate-500">{column.label}</th>)}</tr></thead>
                    <tbody>{rows.map((row, index) => <tr key={`${row.label || row.batch}-${index}`} className="border-b border-slate-100">{columns.map(column => <td key={column.key} className="px-3 py-2 text-slate-700">{column.format ? column.format(row[column.key]) : row[column.key]}</td>)}</tr>)}</tbody>
                </table>
            </div>
        </details>
    );
}

EmptyChart.propTypes = { message: PropTypes.string };
StatusBadge.propTypes = { status: PropTypes.string };
StatCard.propTypes = {
    label: PropTypes.string.isRequired,
    value: PropTypes.node.isRequired,
    detail: PropTypes.string.isRequired,
    tone: PropTypes.oneOf(['indigo', 'emerald', 'amber', 'slate']),
};
Panel.propTypes = {
    title: PropTypes.string.isRequired,
    subtitle: PropTypes.string.isRequired,
    children: PropTypes.node.isRequired,
    action: PropTypes.node,
};
BarChart.propTypes = {
    data: PropTypes.arrayOf(PropTypes.object).isRequired,
    valueKey: PropTypes.string,
    target: PropTypes.number,
    ariaLabel: PropTypes.string.isRequired,
};
LineChart.propTypes = {
    data: PropTypes.arrayOf(PropTypes.object).isRequired,
    target: PropTypes.number.isRequired,
    ariaLabel: PropTypes.string.isRequired,
};
DonutChart.propTypes = {
    pass: PropTypes.number.isRequired,
    fail: PropTypes.number.isRequired,
    threshold: PropTypes.number.isRequired,
};
DistributionChart.propTypes = {
    distribution: PropTypes.shape({
        labels: PropTypes.arrayOf(PropTypes.string),
        data: PropTypes.arrayOf(PropTypes.number),
    }).isRequired,
};
AccessibleTable.propTypes = {
    caption: PropTypes.string.isRequired,
    columns: PropTypes.arrayOf(PropTypes.shape({
        key: PropTypes.string.isRequired,
        label: PropTypes.string.isRequired,
        format: PropTypes.func,
    })).isRequired,
    rows: PropTypes.arrayOf(PropTypes.object).isRequired,
};

function createCsv(dashboard) {
    const rows = [['Section', 'Code/Batch', 'Name', 'Actual', 'Target', 'Gap', 'Count', 'Status']];
    (dashboard.focusTrend || []).forEach(item => rows.push(['Trend', item.batch, '', item.average, item.target, item.gap, item.studentCount, item.status]));
    (dashboard.loPerformance || []).forEach(item => rows.push(['LO', item.label, item.name, item.actual, item.target, item.gap, item.studentCount, item.status]));
    (dashboard.poPerformance || []).forEach(item => rows.push(['PO', item.label, item.name, item.actual, item.target, item.gap, item.mappedLoCount, item.status]));
    return rows.map(row => row.map(value => `"${String(value ?? '').replaceAll('"', '""')}"`).join(',')).join('\r\n');
}

export default function ComparisonPage() {
    const { loId } = useParams();
    const navigate = useNavigate();
    const [loInfo, setLoInfo] = useState(null);
    const [dashboard, setDashboard] = useState(null);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [error, setError] = useState('');
    const [filters, setFilters] = useState({ scope: 'lo', batch: '', markType: '', threshold: 50, target: 60 });
    const [appliedFilters, setAppliedFilters] = useState(filters);

    useEffect(() => {
        let active = true;
        async function loadLo() {
            setLoading(true);
            setError('');
            try {
                const response = await axios.get(`${API_BASE}/api/lospos/${encodeURIComponent(loId)}`);
                if (active) setLoInfo(response.data?.data || response.data);
            } catch (requestError) {
                if (active) {
                    setError(requestError.response?.data?.message || 'Could not load the selected learning outcome.');
                    setLoading(false);
                }
            }
        }
        loadLo();
        return () => { active = false; };
    }, [loId]);

    useEffect(() => {
        const moduleId = loInfo?.moduleId;
        if (!moduleId) return;
        let active = true;
        async function loadDashboard() {
            setRefreshing(Boolean(dashboard));
            setError('');
            try {
                const params = new URLSearchParams();
                if (appliedFilters.batch) params.set('batch', appliedFilters.batch);
                if (appliedFilters.markType) params.set('markType', appliedFilters.markType);
                if (appliedFilters.scope === 'lo') params.set('loId', loId);
                params.set('threshold', String(appliedFilters.threshold));
                params.set('target', String(appliedFilters.target));
                const response = await axios.get(`${API_BASE}/api/obe/graphs/dashboard/${encodeURIComponent(moduleId)}?${params}`);
                if (active) setDashboard(response.data || {});
            } catch (requestError) {
                if (active) setError(requestError.response?.data?.message || 'Could not generate the quality analysis.');
            } finally {
                if (active) { setLoading(false); setRefreshing(false); }
            }
        }
        loadDashboard();
        return () => { active = false; };
        // dashboard is deliberately excluded so a completed request does not trigger itself.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [loInfo?.moduleId, loId, appliedFilters]);

    const meta = dashboard?.meta || {};
    const summary = dashboard?.summary || {};
    const passFail = dashboard?.passFail || { data: [0, 0], threshold: appliedFilters.threshold };
    const warnings = dashboard?.dataQuality?.warnings || [];
    const trend = dashboard?.focusTrend || [];
    const target = meta?.filters?.target ?? appliedFilters.target;
    const available = meta.availableFilters || { batches: [], markTypes: [] };

    const weakAreas = useMemo(() => dashboard?.weakAreas || [], [dashboard]);

    const applyFilters = (event) => {
        event.preventDefault();
        const threshold = Math.max(0, Math.min(100, numberValue(filters.threshold)));
        const selectedTarget = Math.max(0, Math.min(100, numberValue(filters.target)));
        const next = { ...filters, threshold, target: selectedTarget };
        setFilters(next);
        setAppliedFilters(next);
    };

    const resetFilters = () => {
        const next = { scope: 'lo', batch: '', markType: '', threshold: 50, target: 60 };
        setFilters(next);
        setAppliedFilters(next);
    };

    const downloadCsv = () => {
        if (!dashboard) return;
        const blob = new Blob([`\ufeff${createCsv(dashboard)}`], { type: 'text/csv;charset=utf-8' });
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = `qa-graph-analysis-${meta.moduleId || 'module'}.csv`;
        anchor.click();
        URL.revokeObjectURL(url);
    };

    const trendColumns = [
        { key: 'year', label: 'Batch' },
        { key: 'average', label: 'Average', format: percent },
        { key: 'passRate', label: 'Pass rate', format: percent },
        { key: 'studentCount', label: 'Student results' },
        { key: 'delta', label: 'Change', format: value => value == null ? 'Baseline' : `${value > 0 ? '+' : ''}${numberValue(value).toFixed(1)} pp` },
        { key: 'status', label: 'Status' },
    ];
    const performanceColumns = [
        { key: 'label', label: 'Code' }, { key: 'name', label: 'Outcome' },
        { key: 'actual', label: 'Actual', format: percent }, { key: 'target', label: 'Target', format: percent },
        { key: 'gap', label: 'Gap', format: value => `${value > 0 ? '+' : ''}${numberValue(value).toFixed(1)} pp` }, { key: 'status', label: 'Status' },
    ];

    return (
        <div className="min-h-screen bg-slate-50 text-slate-900">
            <Header />
            <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
                <div className="mb-8 flex flex-wrap items-end justify-between gap-5">
                    <div>
                        <button onClick={() => navigate(-1)} className="mb-4 text-sm font-bold text-slate-500 hover:text-indigo-700">&larr; Back to learning outcome</button>
                        <p className="text-xs font-black uppercase tracking-[0.2em] text-indigo-600">Outcome-based quality assurance</p>
                        <h1 className="mt-2 text-3xl font-black tracking-tight text-slate-950 sm:text-4xl">Graph analysis</h1>
                        <p className="mt-2 max-w-3xl text-sm text-slate-600">
                            {loInfo?.id || loId} - {loInfo?.description || loInfo?.name || 'Learning outcome'}
                            {loInfo?.moduleId ? ` | Module ${loInfo.moduleId}` : ''}
                        </p>
                    </div>
                    <div className="flex gap-2 print:hidden">
                        <button type="button" onClick={downloadCsv} disabled={!dashboard} className="btn-secondary disabled:cursor-not-allowed disabled:opacity-50">Export CSV</button>
                        <button type="button" onClick={() => window.print()} disabled={!dashboard} className="btn-primary disabled:cursor-not-allowed disabled:opacity-50">Print report</button>
                    </div>
                </div>

                <form onSubmit={applyFilters} className="glass-card mb-8 grid gap-4 rounded-3xl border border-slate-100 p-5 md:grid-cols-6 print:hidden">
                    <label className="text-xs font-black uppercase tracking-wide text-slate-600">Analysis scope<select className="input-field mt-2 normal-case" value={filters.scope} onChange={e => setFilters({ ...filters, scope: e.target.value })}><option value="lo">Selected LO</option><option value="module">Whole module</option></select></label>
                    <label className="text-xs font-black uppercase tracking-wide text-slate-600">Batch<select className="input-field mt-2 normal-case" value={filters.batch} onChange={e => setFilters({ ...filters, batch: e.target.value })}><option value="">All batches</option>{(available.batches || []).map(value => <option key={value} value={value}>Batch {value}</option>)}</select></label>
                    <label className="text-xs font-black uppercase tracking-wide text-slate-600">Assessment<select className="input-field mt-2 normal-case" value={filters.markType} onChange={e => setFilters({ ...filters, markType: e.target.value })}><option value="">All types</option>{(available.markTypes || []).map(value => <option key={value} value={value}>{value.replaceAll('_', ' ')}</option>)}</select></label>
                    <label className="text-xs font-black uppercase tracking-wide text-slate-600">Pass threshold<input className="input-field mt-2 normal-case" type="number" min="0" max="100" step="0.5" value={filters.threshold} onChange={e => setFilters({ ...filters, threshold: e.target.value })} /></label>
                    <label className="text-xs font-black uppercase tracking-wide text-slate-600">QA target<input className="input-field mt-2 normal-case" type="number" min="0" max="100" step="0.5" value={filters.target} onChange={e => setFilters({ ...filters, target: e.target.value })} /></label>
                    <div className="flex items-end gap-2"><button className="btn-primary flex-1" type="submit">Apply</button><button className="btn-secondary" type="button" onClick={resetFilters}>Reset</button></div>
                </form>

                {loading ? (
                    <div className="glass-card rounded-[2rem] p-16 text-center"><div className="mx-auto h-12 w-12 animate-spin rounded-full border-4 border-indigo-100 border-t-indigo-600" /><p className="mt-5 text-sm font-bold text-slate-500">Generating quality analysis...</p></div>
                ) : error ? (
                    <div role="alert" className="rounded-[2rem] border border-red-200 bg-red-50 p-10 text-center"><h2 className="text-xl font-black text-red-800">Analysis unavailable</h2><p className="mt-2 text-sm text-red-700">{error}</p></div>
                ) : dashboard ? (
                    <div className={`space-y-8 transition-opacity ${refreshing ? 'opacity-60' : 'opacity-100'}`} aria-busy={refreshing}>
                        {warnings.length > 0 && <div role="status" className="rounded-2xl border border-amber-200 bg-amber-50 p-5"><h2 className="text-sm font-black text-amber-900">Data quality notice</h2><ul className="mt-2 list-disc space-y-1 pl-5 text-xs text-amber-800">{warnings.map(warning => <li key={warning}>{warning}</li>)}</ul></div>}

                        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
                            <StatCard label="Average attainment" value={percent(summary.average)} detail={`${summary.studentResults || 0} student/batch results`} />
                            <StatCard label="Pass rate" value={percent(summary.passRate)} detail={`Threshold ${percent(passFail.threshold)}`} tone="emerald" />
                            <StatCard label="Students" value={summary.totalStudents || 0} detail={`${summary.observationCount || 0} normalized observations`} tone="slate" />
                            <StatCard label="LO target coverage" value={`${summary.targetMet || 0}/${summary.loCount || 0}`} detail={`${summary.weakAreaCount || 0} outcomes require attention`} tone="amber" />
                        </div>

                        <Panel title="Attainment trend" subtitle={`${meta.scope === 'LEARNING_OUTCOME' ? 'Selected LO' : 'Module'} average by batch; change is shown in percentage points.`} action={<span className="rounded-full bg-red-50 px-3 py-1 text-[10px] font-black text-red-700">TARGET {percent(target)}</span>}>
                            <LineChart data={trend} target={target} ariaLabel="Average attainment trend by batch" />
                            <AccessibleTable caption="Attainment trend data" columns={trendColumns} rows={trend} />
                        </Panel>

                        <div className="grid gap-8 xl:grid-cols-2">
                            <Panel title="Pass and fail" subtitle="Each student is counted once per batch after their LO results are normalized."><DonutChart pass={numberValue(passFail.data?.[0])} fail={numberValue(passFail.data?.[1])} threshold={passFail.threshold} /></Panel>
                            <Panel title="Score distribution" subtitle="Distribution of distinct student/batch results, not raw mark rows."><DistributionChart distribution={dashboard.scoreBands || EMPTY_DISTRIBUTION} /></Panel>
                        </div>

                        <Panel title="Learning outcome performance" subtitle="Normalized LO averages compared with the selected QA target.">
                            <BarChart data={dashboard.loPerformance || []} target={target} ariaLabel="Learning outcome attainment compared with target" />
                            <AccessibleTable caption="Learning outcome performance data" columns={performanceColumns} rows={dashboard.loPerformance || []} />
                        </Panel>

                        <Panel title="Program outcome attainment" subtitle="Weighted from approved LO-to-PO mappings; this is not a relabelled LO average.">
                            <BarChart data={dashboard.poPerformance || []} target={target} ariaLabel="Program outcome attainment based on approved mappings" />
                            <AccessibleTable caption="Program outcome attainment data" columns={performanceColumns} rows={dashboard.poPerformance || []} />
                        </Panel>

                        <div className="grid gap-8 xl:grid-cols-2">
                            <Panel title="Outcomes requiring attention" subtitle={`LO averages below ${percent(target)}, weakest first.`}>
                                {weakAreas.length ? <div className="space-y-3">{weakAreas.map(item => <div key={item.loId} className="rounded-2xl border border-slate-200 bg-white p-4"><div className="flex flex-wrap items-center justify-between gap-3"><div><div className="font-black text-slate-900">{item.label} <span className="font-medium text-slate-500">{item.name}</span></div><div className="mt-1 text-xs text-slate-500">Gap {percent(Math.abs(item.gap))} below target | n={item.studentCount}</div></div><div className="flex items-center gap-3"><strong className="text-lg text-amber-700">{percent(item.actual)}</strong><StatusBadge status={item.status} /></div></div></div>)}</div> : <EmptyChart message="No weak learning outcomes were found for this scope." />}
                            </Panel>
                            <Panel title="Method and traceability" subtitle="The dashboard exposes its filters and calculation assumptions for review.">
                                <dl className="grid gap-4 text-sm sm:grid-cols-2">
                                    <div><dt className="text-xs font-black uppercase text-slate-400">Formula version</dt><dd className="mt-1 font-bold text-slate-800">{meta.formulaVersion}</dd></div>
                                    <div><dt className="text-xs font-black uppercase text-slate-400">Generated</dt><dd className="mt-1 font-bold text-slate-800">{meta.generatedAt ? new Date(meta.generatedAt).toLocaleString() : '-'}</dd></div>
                                    <div className="sm:col-span-2"><dt className="text-xs font-black uppercase text-slate-400">Normalization</dt><dd className="mt-1 leading-6 text-slate-700">{dashboard.dataQuality?.normalizationRule}</dd></div>
                                    <div><dt className="text-xs font-black uppercase text-slate-400">Assessment-normalized</dt><dd className="mt-1 font-bold text-slate-800">{dashboard.dataQuality?.normalizedObservationCount || 0} observations</dd></div>
                                    <div><dt className="text-xs font-black uppercase text-slate-400">Assumed percentage</dt><dd className="mt-1 font-bold text-slate-800">{dashboard.dataQuality?.assumedPercentageObservationCount || 0} observations</dd></div>
                                    <div><dt className="text-xs font-black uppercase text-slate-400">Source marks</dt><dd className="mt-1 font-bold text-slate-800">{dashboard.dataQuality?.sourceMarkCount || 0} records</dd></div>
                                </dl>
                            </Panel>
                        </div>
                    </div>
                ) : null}
            </main>
            <Footer />
        </div>
    );
}
