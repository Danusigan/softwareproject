import React, { useState, useMemo } from 'react';

// Generic type-ahead multi-select: typing filters options by label, clicking adds a
// chip, already-selected options are excluded from suggestions. Used for assigning
// lecturers to a module and, in reverse, modules to a lecturer.
export default function MultiSelectAutocomplete({ options, selectedValues, onChange, placeholder, emptyHint }) {
    const [query, setQuery] = useState('');
    const [open, setOpen] = useState(false);

    const selectedOptions = useMemo(
        () => selectedValues.map(value => options.find(o => o.value === value) || { value, label: value }),
        [selectedValues, options]
    );

    const suggestions = useMemo(() => {
        const q = query.trim().toLowerCase();
        return options
            .filter(o => !selectedValues.includes(o.value))
            .filter(o => !q
                || o.label.toLowerCase().includes(q)
                || (o.sublabel && o.sublabel.toLowerCase().includes(q)))
            .slice(0, 8);
    }, [options, selectedValues, query]);

    const addValue = (value) => {
        onChange([...selectedValues, value]);
        setQuery('');
        setOpen(false);
    };

    const removeValue = (value) => {
        onChange(selectedValues.filter(v => v !== value));
    };

    return (
        <div className="relative">
            {selectedOptions.length > 0 && (
                <div className="flex flex-wrap gap-2 mb-2">
                    {selectedOptions.map(opt => (
                        <span key={opt.value} className="inline-flex items-center gap-1.5 px-3 py-1 bg-blue-100 text-blue-800 rounded-full text-sm">
                            {opt.label}
                            <button
                                type="button"
                                onClick={() => removeValue(opt.value)}
                                className="text-blue-600 hover:text-blue-900"
                                aria-label={`Remove ${opt.label}`}
                            >
                                &times;
                            </button>
                        </span>
                    ))}
                </div>
            )}
            <input
                type="text"
                value={query}
                onChange={(e) => { setQuery(e.target.value); setOpen(true); }}
                onFocus={() => setOpen(true)}
                onBlur={() => setTimeout(() => setOpen(false), 150)}
                placeholder={placeholder}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
            {open && suggestions.length > 0 && (
                <ul className="absolute z-10 mt-1 w-full bg-white border border-gray-200 rounded-lg shadow-lg max-h-48 overflow-y-auto">
                    {suggestions.map(opt => (
                        <li key={opt.value}>
                            <button
                                type="button"
                                onMouseDown={(e) => { e.preventDefault(); addValue(opt.value); }}
                                className="w-full text-left px-4 py-2 hover:bg-blue-50 text-sm"
                            >
                                <span className="font-medium">{opt.label}</span>
                                {opt.sublabel && <span className="text-gray-500 ml-2">{opt.sublabel}</span>}
                            </button>
                        </li>
                    ))}
                </ul>
            )}
            {selectedOptions.length === 0 && emptyHint && (
                <p className="text-xs text-gray-500 mt-1">{emptyHint}</p>
            )}
        </div>
    );
}
