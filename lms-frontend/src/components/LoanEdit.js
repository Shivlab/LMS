import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import loanService from '../services/loanService';

const LoanEdit = () => {
    const { id } = useParams();
    const navigate = useNavigate();
    const [loan, setLoan] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [activeTab, setActiveTab] = useState('basic');
    
    // Form states
    const [editData, setEditData] = useState({
        annualRate: '',
        status: '',
        moratoriumMonths: '',
        changeReason: '',
        changeDescription: '',
        effectiveFrom: ''
    });
    
    const [newCharge, setNewCharge] = useState({
        chargeName: '',
        chargeAmount: '',
        chargeType: 'FIXED'
    });
    
    const [newDisbursement, setNewDisbursement] = useState({
        disbursementDate: '',
        amount: '',
        description: ''
    });
    
    const [newMoratorium, setNewMoratorium] = useState({
        startMonth: '',
        endMonth: '',
        type: 'FULL',
        partialPaymentEMI: ''
    });
    
    const [prepayment, setPrepayment] = useState({
        amount: '',
        paymentDate: '',
        description: ''
    });
    
    const [benchmarkRate, setBenchmarkRate] = useState({
        benchmarkName: '',
        newRate: ''
    });
    
    const [disbursements, setDisbursements] = useState([]);
    const [disbursementLoading, setDisbursementLoading] = useState(false);
    const [canEditDisbursements, setCanEditDisbursements] = useState(false);
    const [disbursementMessage, setDisbursementMessage] = useState('');

    useEffect(() => {
        fetchLoanDetails();
        if (activeTab === 'disbursements') {
            fetchDisbursements();
        }
    }, [id, activeTab]);

    const fetchLoanDetails = async () => {
        try {
            setLoading(true);
            const response = await loanService.getLoanById(id);
            setLoan(response.data);
            setEditData({
                annualRate: response.data.annualRate || '',
                status: response.data.status || '',
                moratoriumMonths: response.data.moratoriumMonths || '',
                changeReason: '',
                changeDescription: ''
            });
        } catch (err) {
            setError('Failed to fetch loan details');
        } finally {
            setLoading(false);
        }
    };
    
    const fetchDisbursements = async () => {
        try {
            setDisbursementLoading(true);
            const response = await loanService.getDisbursements(id);
            setDisbursements(response.data.disbursements || []);
            setCanEditDisbursements(response.data.canEdit || false);
            setDisbursementMessage(response.data.message || '');
        } catch (err) {
            setError('Failed to fetch disbursements');
            setDisbursements([]);
            setCanEditDisbursements(false);
        } finally {
            setDisbursementLoading(false);
        }
    };
    
    const handleDisbursementChange = (index, field, value) => {
        const updated = [...disbursements];
        updated[index] = { ...updated[index], [field]: value };
        setDisbursements(updated);
    };
    
    const addDisbursement = () => {
        const newDisbursement = {
            id: null,
            disbursementDate: '',
            amount: '',
            description: '',
            sequence: disbursements.length + 1,
            canEdit: true
        };
        setDisbursements([...disbursements, newDisbursement]);
    };
    
    const removeDisbursement = async (index) => {
        const disbursement = disbursements[index];
        
        if (disbursement.id) {
            // Delete from backend if it exists
            try {
                await loanService.deleteDisbursement(id, disbursement.id);
                setSuccess('Disbursement deleted successfully');
            } catch (err) {
                setError(err.response?.data?.error || 'Failed to delete disbursement');
                return;
            }
        }
        
        // Remove from local state
        const updated = disbursements.filter((_, i) => i !== index);
        setDisbursements(updated);
    };
    
    const saveDisbursements = async () => {
        try {
            setError('');
            setSuccess('');
            
            // Validate disbursements
            for (const disbursement of disbursements) {
                if (!disbursement.disbursementDate || !disbursement.amount) {
                    setError('All disbursements must have a date and amount');
                    return;
                }
                if (parseFloat(disbursement.amount) <= 0) {
                    setError('Disbursement amount must be greater than 0');
                    return;
                }
            }
            
            await loanService.updateDisbursements(id, disbursements);
            setSuccess('Disbursements updated successfully');
            fetchDisbursements(); // Refresh the list
        } catch (err) {
            setError(err.response?.data?.error || 'Failed to update disbursements');
        }
    };

    const handleBasicEdit = async (e) => {
        e.preventDefault();
        try {
            setError('');
            setSuccess('');
            
            // Map free-text reason to backend enum LoanVersionEntity.ChangeReason
            const allowedReasons = [
                'INITIAL_CREATION',
                'RATE_MODIFICATION',
                'SPREAD_MODIFICATION',
                'TERM_MODIFICATION',
                'BENCHMARK_CHANGE',
                'MANUAL_CORRECTION',
                'REGULATORY_CHANGE',
                'CUSTOMER_REQUEST'
            ];
            const normalizeReason = (reason) => {
                if (!reason || reason.trim() === '') return 'MANUAL_CORRECTION';
                const normalized = reason.trim().toUpperCase().replace(/\s+/g, '_');
                return allowedReasons.includes(normalized) ? normalized : 'MANUAL_CORRECTION';
            };

            // Create complete loan input with all required fields
            const updateData = {
                // Required fields from existing loan
                customerId: loan.customerId,
                productType: loan.productType,
                loanIssueDate: loan.loanIssueDate,
                startDate: loan.startDate,
                principal: loan.principal,
                months: loan.months,
                compoundingFrequency: loan.compoundingFrequency || 'MONTHLY',
                rateType: loan.rateType,
                benchmarkName: loan.benchmarkName,
                spread: loan.spread,
                
                // Updated fields
                annualRate: editData.annualRate !== '' ? parseFloat(editData.annualRate) : loan.annualRate,
                status: editData.status || loan.status,
                moratoriumMonths: editData.moratoriumMonths !== '' ? parseInt(editData.moratoriumMonths) : loan.moratoriumMonths,
                
                // Change tracking
                changeReason: normalizeReason(editData.changeReason),
                changeDescription: editData.changeDescription || 'Loan parameters updated',
                effectiveFrom: editData.effectiveFrom ? (editData.effectiveFrom + ' 00:00:00') : undefined
            };
            
            await loanService.editLoan(id, updateData);
            setSuccess('Loan updated successfully! New version created.');
            fetchLoanDetails();
        } catch (err) {
            setError(err.response?.data?.message || err.response?.data || 'Failed to update loan');
        }
    };

    const handleAddCharge = async (e) => {
        e.preventDefault();
        try {
            setError('');
            setSuccess('');
            
            await loanService.addLoanCharge(id, newCharge);
            setSuccess('Charge added successfully!');
            setNewCharge({ chargeName: '', chargeAmount: '', chargeType: 'FIXED' });
            fetchLoanDetails();
        } catch (err) {
            setError(err.response?.data || 'Failed to add charge');
        }
    };

    const handleAddDisbursement = async (e) => {
        e.preventDefault();
        try {
            setError('');
            setSuccess('');
            
            await loanService.addDisbursementPhase(id, newDisbursement);
            setSuccess('Disbursement phase added successfully!');
            setNewDisbursement({ disbursementDate: '', amount: '', description: '' });
            fetchLoanDetails();
        } catch (err) {
            setError(err.response?.data || 'Failed to add disbursement phase');
        }
    };

    const handleAddMoratorium = async (e) => {
        e.preventDefault();
        try {
            setError('');
            setSuccess('');
            
            await loanService.addMoratoriumPeriod(id, newMoratorium);
            setSuccess('Moratorium period added successfully!');
            setNewMoratorium({ startMonth: '', endMonth: '', type: 'FULL', partialPaymentEMI: '' });
            fetchLoanDetails();
        } catch (err) {
            setError(err.response?.data || 'Failed to add moratorium period');
        }
    };

    const handlePrepayment = async (e) => {
        e.preventDefault();
        try {
            setError('');
            setSuccess('');
            
            await loanService.recordPrepayment(id, prepayment);
            setSuccess('Prepayment recorded successfully! Schedule regenerated.');
            setPrepayment({ amount: '', paymentDate: '', description: '' });
            fetchLoanDetails();
        } catch (err) {
            setError(err.response?.data || 'Failed to record prepayment');
        }
    };

    const handleBenchmarkReset = async (e) => {
        e.preventDefault();
        try {
            setError('');
            setSuccess('');
            
            await loanService.addBenchmarkRate(benchmarkRate.benchmarkName, benchmarkRate.newRate);
            setSuccess('Benchmark rate updated! All floating loans affected.');
            setBenchmarkRate({ benchmarkName: '', newRate: '' });
            fetchLoanDetails();
        } catch (err) {
            setError(err.response?.data || 'Failed to update benchmark rate');
        }
    };

    if (loading) return <div className="flex justify-center items-center h-64">Loading...</div>;
    if (!loan) return <div className="text-red-600">Loan not found</div>;

    return (
        <div className="max-w-6xl mx-auto p-6">
            <div className="bg-white rounded-lg shadow-lg">
                <div className="border-b border-gray-200 px-6 py-4">
                    <div className="flex justify-between items-center">
                        <h1 className="text-2xl font-bold text-gray-900">
                            Edit Loan #{loan.loanId}
                        </h1>
                        <button
                            onClick={() => navigate('/loans')}
                            className="px-4 py-2 text-gray-600 hover:text-gray-800"
                        >
                            ← Back to Loans
                        </button>
                    </div>
                    
                    {/* Loan Summary */}
                    <div className="mt-4 grid grid-cols-1 md:grid-cols-4 gap-4 text-sm">
                        <div>
                            <span className="text-gray-500">Principal:</span>
                            <span className="ml-2 font-semibold">₹{loan.principal?.toLocaleString()}</span>
                        </div>
                        <div>
                            <span className="text-gray-500">Current Rate:</span>
                            <span className="ml-2 font-semibold">{loan.annualRate}%</span>
                        </div>
                        <div>
                            <span className="text-gray-500">Status:</span>
                            <span className={`ml-2 px-2 py-1 rounded text-xs ${
                                loan.status === 'ACTIVE' ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'
                            }`}>
                                {loan.status}
                            </span>
                        </div>
                        <div>
                            <span className="text-gray-500">Product:</span>
                            <span className="ml-2 font-semibold">{loan.productType}</span>
                        </div>
                    </div>
                </div>

                {/* Alert Messages */}
                {error && (
                    <div className="mx-6 mt-4 p-4 bg-red-50 border border-red-200 rounded-md">
                        <p className="text-red-800">{error}</p>
                    </div>
                )}
                {success && (
                    <div className="mx-6 mt-4 p-4 bg-green-50 border border-green-200 rounded-md">
                        <p className="text-green-800">{success}</p>
                    </div>
                )}

                {/* Tabs */}
                <div className="border-b border-gray-200">
                    <nav className="flex space-x-8 px-6">
                        {[
                            { id: 'basic', label: 'Basic Info' },
                            { id: 'charges', label: 'Charges' },
                            ...(loan.productType === 'HOME_LOAN' ? [{ id: 'disbursements', label: 'Disbursements' }] : []),
                            { id: 'moratorium', label: 'Moratorium' },
                            { id: 'prepayment', label: 'Prepayment' },
                            { id: 'benchmark', label: 'Rate Reset' }
                        ].map(tab => (
                            <button
                                key={tab.id}
                                onClick={() => setActiveTab(tab.id)}
                                className={`py-4 px-1 border-b-2 font-medium text-sm ${
                                    activeTab === tab.id
                                        ? 'border-blue-500 text-blue-600'
                                        : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                                }`}
                            >
                                {tab.label}
                            </button>
                        ))}
                    </nav>
                </div>

                <div className="p-6">
                    {/* Basic Info Tab */}
                    {activeTab === 'basic' && (
                        <form onSubmit={handleBasicEdit} className="space-y-6">
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-2">
                                        Annual Interest Rate (%)
                                    </label>
                                    <input
                                        type="number"
                                        step="0.01"
                                        value={editData.annualRate}
                                        onChange={(e) => setEditData({...editData, annualRate: e.target.value})}
                                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    />
                                </div>
                                
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-2">
                                        Loan Status
                                    </label>
                                    <select
                                        value={editData.status}
                                        onChange={(e) => setEditData({...editData, status: e.target.value})}
                                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    >
                                        <option value="ACTIVE">Active</option>
                                        <option value="CLOSED">Closed</option>
                                        <option value="SUSPENDED">Suspended</option>
                                    </select>
                                </div>
                                
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-2">
                                        Moratorium Months
                                    </label>
                                    <input
                                        type="number"
                                        value={editData.moratoriumMonths}
                                        onChange={(e) => setEditData({...editData, moratoriumMonths: e.target.value})}
                                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    />
                                </div>
                            </div>
                            
                            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-2">
                                        Change Reason
                                    </label>
                                    <input
                                        type="text"
                                        value={editData.changeReason}
                                        onChange={(e) => setEditData({...editData, changeReason: e.target.value})}
                                        placeholder="e.g., Rate revision, Status update"
                                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    />
                                </div>
                                
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-2">
                                        Change Description
                                    </label>
                                    <input
                                        type="text"
                                        value={editData.changeDescription}
                                        onChange={(e) => setEditData({...editData, changeDescription: e.target.value})}
                                        placeholder="Detailed description of changes"
                                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    />
                                </div>
                                
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-2">
                                        Effective From (Date)
                                    </label>
                                    <input
                                        type="date"
                                        value={editData.effectiveFrom}
                                        onChange={(e) => setEditData({...editData, effectiveFrom: e.target.value})}
                                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    />
                                </div>
                            </div>
                            
                            <div className="flex justify-end">
                                <button
                                    type="submit"
                                    className="px-6 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
                                >
                                    Update Loan
                                </button>
                            </div>
                        </form>
                    )}

                    {/* Charges Tab */}
                    {activeTab === 'charges' && (
                        <div className="space-y-6">
                            <h3 className="text-lg font-medium text-gray-900">Add New Charge</h3>
                            <form onSubmit={handleAddCharge} className="space-y-4">
                                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-2">
                                            Charge Name
                                        </label>
                                        <input
                                            type="text"
                                            value={newCharge.chargeName}
                                            onChange={(e) => setNewCharge({...newCharge, chargeName: e.target.value})}
                                            placeholder="e.g., Processing Fee"
                                            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                            required
                                        />
                                    </div>
                                    
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-2">
                                            Amount
                                        </label>
                                        <input
                                            type="number"
                                            step="0.01"
                                            value={newCharge.chargeAmount}
                                            onChange={(e) => setNewCharge({...newCharge, chargeAmount: e.target.value})}
                                            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                            required
                                        />
                                    </div>
                                    
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-2">
                                            Type
                                        </label>
                                        <select
                                            value={newCharge.chargeType}
                                            onChange={(e) => setNewCharge({...newCharge, chargeType: e.target.value})}
                                            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                        >
                                            <option value="FIXED">Fixed</option>
                                            <option value="PERCENTAGE">Percentage</option>
                                        </select>
                                    </div>
                                </div>
                                
                                <div className="flex justify-end">
                                    <button
                                        type="submit"
                                        className="px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700"
                                    >
                                        Add Charge
                                    </button>
                                </div>
                            </form>
                        </div>
                    )}

                    {/* Disbursements Tab */}
                    {activeTab === 'disbursements' && (
                        <div className="space-y-6">
                            <div className="flex justify-between items-center">
                                <h3 className="text-lg font-medium text-gray-900">Disbursement Phases</h3>
                                {canEditDisbursements && (
                                    <button
                                        onClick={addDisbursement}
                                        className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
                                    >
                                        Add Disbursement
                                    </button>
                                )}
                            </div>
                            
                            {disbursementMessage && (
                                <div className={`p-4 rounded-md ${canEditDisbursements ? 'bg-blue-50 border border-blue-200' : 'bg-yellow-50 border border-yellow-200'}`}>
                                    <p className={canEditDisbursements ? 'text-blue-800' : 'text-yellow-800'}>
                                        {disbursementMessage}
                                    </p>
                                </div>
                            )}
                            
                            {disbursementLoading ? (
                                <div className="flex justify-center items-center h-32">
                                    <div className="text-gray-500">Loading disbursements...</div>
                                </div>
                            ) : disbursements.length > 0 ? (
                                <div className="space-y-4">
                                    {disbursements.map((disbursement, index) => (
                                        <div key={index} className="border border-gray-200 rounded-lg p-4">
                                            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                                                <div>
                                                    <label className="block text-sm font-medium text-gray-700 mb-2">
                                                        Disbursement Date
                                                    </label>
                                                    <input
                                                        type="date"
                                                        value={disbursement.disbursementDate}
                                                        onChange={(e) => handleDisbursementChange(index, 'disbursementDate', e.target.value)}
                                                        className={`w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                                                            !disbursement.canEdit ? 'bg-gray-100 cursor-not-allowed' : ''
                                                        }`}
                                                        disabled={!disbursement.canEdit}
                                                        required
                                                    />
                                                </div>
                                                
                                                <div>
                                                    <label className="block text-sm font-medium text-gray-700 mb-2">
                                                        Amount (₹)
                                                    </label>
                                                    <input
                                                        type="number"
                                                        step="0.01"
                                                        value={disbursement.amount}
                                                        onChange={(e) => handleDisbursementChange(index, 'amount', e.target.value)}
                                                        className={`w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                                                            !disbursement.canEdit ? 'bg-gray-100 cursor-not-allowed' : ''
                                                        }`}
                                                        disabled={!disbursement.canEdit}
                                                        required
                                                    />
                                                </div>
                                                
                                                <div>
                                                    <label className="block text-sm font-medium text-gray-700 mb-2">
                                                        Description
                                                    </label>
                                                    <input
                                                        type="text"
                                                        value={disbursement.description || ''}
                                                        onChange={(e) => handleDisbursementChange(index, 'description', e.target.value)}
                                                        className={`w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                                                            !disbursement.canEdit ? 'bg-gray-100 cursor-not-allowed' : ''
                                                        }`}
                                                        disabled={!disbursement.canEdit}
                                                    />
                                                </div>
                                                
                                                <div className="flex items-end">
                                                    {disbursement.canEdit && (
                                                        <button
                                                            onClick={() => removeDisbursement(index)}
                                                            className="px-3 py-2 bg-red-600 text-white rounded-md hover:bg-red-700"
                                                        >
                                                            Remove
                                                        </button>
                                                    )}
                                                    {!disbursement.canEdit && (
                                                        <span className="text-sm text-gray-500 px-3 py-2">
                                                            Already disbursed
                                                        </span>
                                                    )}
                                                </div>
                                            </div>
                                        </div>
                                    ))}
                                    
                                    {canEditDisbursements && (
                                        <div className="flex justify-end">
                                            <button
                                                onClick={saveDisbursements}
                                                className="px-6 py-2 bg-green-600 text-white rounded-md hover:bg-green-700"
                                            >
                                                Save Changes
                                            </button>
                                        </div>
                                    )}
                                </div>
                            ) : (
                                <div className="text-center py-8 text-gray-500">
                                    No disbursement phases found for this loan.
                                </div>
                            )}
                        </div>
                    )}

                    {/* Moratorium Tab */}
                    {activeTab === 'moratorium' && (
                        <div className="space-y-6">
                            <h3 className="text-lg font-medium text-gray-900">Add Moratorium Period</h3>
                            <form onSubmit={handleAddMoratorium} className="space-y-4">
                                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-2">
                                            Start Month
                                        </label>
                                        <input
                                            type="number"
                                            value={newMoratorium.startMonth}
                                            onChange={(e) => setNewMoratorium({...newMoratorium, startMonth: e.target.value})}
                                            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                            required
                                        />
                                    </div>
                                    
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-2">
                                            End Month
                                        </label>
                                        <input
                                            type="number"
                                            value={newMoratorium.endMonth}
                                            onChange={(e) => setNewMoratorium({...newMoratorium, endMonth: e.target.value})}
                                            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                            required
                                        />
                                    </div>
                                    
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-2">
                                            Type
                                        </label>
                                        <select
                                            value={newMoratorium.type}
                                            onChange={(e) => setNewMoratorium({...newMoratorium, type: e.target.value})}
                                            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                        >
                                            <option value="FULL">Full Moratorium</option>
                                            <option value="INTEREST_ONLY">Interest Only</option>
                                            <option value="PARTIAL">Partial Payment</option>
                                        </select>
                                    </div>
                                    
                                    {newMoratorium.type === 'PARTIAL' && (
                                        <div>
                                            <label className="block text-sm font-medium text-gray-700 mb-2">
                                                Partial Payment EMI
                                            </label>
                                            <input
                                                type="number"
                                                step="0.01"
                                                value={newMoratorium.partialPaymentEMI}
                                                onChange={(e) => setNewMoratorium({...newMoratorium, partialPaymentEMI: e.target.value})}
                                                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                            />
                                        </div>
                                    )}
                                </div>
                                
                                <div className="flex justify-end">
                                    <button
                                        type="submit"
                                        className="px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700"
                                    >
                                        Add Moratorium
                                    </button>
                                </div>
                            </form>
                        </div>
                    )}

                    {/* Prepayment Tab */}
                    {activeTab === 'prepayment' && (
                        <div className="space-y-6">
                            <h3 className="text-lg font-medium text-gray-900">Record Prepayment</h3>
                            <form onSubmit={handlePrepayment} className="space-y-4">
                                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-2">
                                            Prepayment Amount
                                        </label>
                                        <input
                                            type="number"
                                            step="0.01"
                                            value={prepayment.amount}
                                            onChange={(e) => setPrepayment({...prepayment, amount: e.target.value})}
                                            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                            required
                                        />
                                    </div>
                                    
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-2">
                                            Payment Date
                                        </label>
                                        <input
                                            type="date"
                                            value={prepayment.paymentDate}
                                            onChange={(e) => setPrepayment({...prepayment, paymentDate: e.target.value})}
                                            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                            required
                                        />
                                    </div>
                                    
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-2">
                                            Description
                                        </label>
                                        <input
                                            type="text"
                                            value={prepayment.description}
                                            onChange={(e) => setPrepayment({...prepayment, description: e.target.value})}
                                            placeholder="Prepayment description"
                                            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                        />
                                    </div>
                                </div>
                                
                                <div className="flex justify-end">
                                    <button
                                        type="submit"
                                        className="px-4 py-2 bg-purple-600 text-white rounded-md hover:bg-purple-700"
                                    >
                                        Record Prepayment
                                    </button>
                                </div>
                            </form>
                        </div>
                    )}

                    {/* Benchmark Rate Reset Tab */}
                    {activeTab === 'benchmark' && (
                        <div className="space-y-6">
                            <h3 className="text-lg font-medium text-gray-900">Benchmark Rate Reset</h3>
                            <div className="bg-yellow-50 border border-yellow-200 rounded-md p-4">
                                <p className="text-yellow-800 text-sm">
                                    <strong>Note:</strong> This will update the benchmark rate and automatically apply it to all floating loans using this benchmark.
                                </p>
                            </div>
                            <form onSubmit={handleBenchmarkReset} className="space-y-4">
                                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-2">
                                            Benchmark Name
                                        </label>
                                        <input
                                            type="text"
                                            value={benchmarkRate.benchmarkName}
                                            onChange={(e) => setBenchmarkRate({...benchmarkRate, benchmarkName: e.target.value})}
                                            placeholder="e.g., REPO_RATE, MCLR"
                                            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                            required
                                        />
                                    </div>
                                    
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 mb-2">
                                            New Rate (%)
                                        </label>
                                        <input
                                            type="number"
                                            step="0.01"
                                            value={benchmarkRate.newRate}
                                            onChange={(e) => setBenchmarkRate({...benchmarkRate, newRate: e.target.value})}
                                            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                                            required
                                        />
                                    </div>
                                </div>
                                
                                <div className="flex justify-end">
                                    <button
                                        type="submit"
                                        className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700"
                                    >
                                        Update Benchmark Rate
                                    </button>
                                </div>
                            </form>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default LoanEdit;
