import React from 'react';

const ModuleModal = ({ module, onClose }) => {
    if (!module) return null;

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white rounded-xl p-6 w-full max-w-2xl max-h-[80vh] overflow-y-auto">
                <div className="flex justify-between items-center mb-6">
                    <h2 className="text-2xl font-bold text-gray-800">
                        {module.moduleId} - {module.moduleName}
                    </h2>
                    <button
                        onClick={onClose}
                        className="text-gray-500 hover:text-gray-700"
                    >
                        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                        </svg>
                    </button>
                </div>

                <div className="space-y-6">
                    {/* Learning Outcomes Section */}
                    <div>
                        <h3 className="text-lg font-semibold text-gray-700 mb-3">Learning Outcomes (LOs)</h3>
                        {module.los && module.los.length > 0 ? (
                            <ul className="list-disc list-inside space-y-2 text-gray-600">
                                {module.los.map((lo, index) => (
                                    <li key={index} className="pl-2">
                                        <span className="font-medium">{lo.code}:</span> {lo.description}
                                    </li>
                                ))}
                            </ul>
                        ) : (
                            <p className="text-gray-500 italic">No learning outcomes defined.</p>
                        )}
                    </div>

                    {/* Program Outcomes Section */}
                    <div>
                        <h3 className="text-lg font-semibold text-gray-700 mb-3">Program Outcomes (POs)</h3>
                        {module.pos && module.pos.length > 0 ? (
                            <ul className="list-disc list-inside space-y-2 text-gray-600">
                                {module.pos.map((po, index) => (
                                    <li key={index} className="pl-2">
                                        <span className="font-medium">{po.code}:</span> {po.description}
                                    </li>
                                ))}
                            </ul>
                        ) : (
                            <p className="text-gray-500 italic">No program outcomes defined.</p>
                        )}
                    </div>
                </div>

                <div className="mt-8 flex justify-end">
                    <button
                        onClick={onClose}
                        className="px-6 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition-colors"
                    >
                        Close
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ModuleModal;
