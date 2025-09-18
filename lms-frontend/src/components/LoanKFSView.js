import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { FileText, Calendar, DollarSign, TrendingUp, Download, Eye } from 'lucide-react';
import { loanService } from '../services/api';
import { toast } from 'react-hot-toast';

const LoanKFSView = () => {
  const { id } = useParams();
  const [loan, setLoan] = useState(null);
  const [schedule, setSchedule] = useState(null);
  const [loading, setLoading] = useState(true);
  const [versions, setVersions] = useState([]);
  const [selectedVersion, setSelectedVersion] = useState(null);

  useEffect(() => {
    const fetchData = async () => {
      await fetchLoanDetails();
      await fetchKfsVersions();
    };
    fetchData();
  }, [id]);
  const fetchKfsVersions = async () => {
    try {
      const response = await loanService.getKfsVersions(id);
      const sorted = (response.data || []).sort((a,b) => (b.versionNumber - a.versionNumber));
      setVersions(sorted);
      // Set latest version as default if not already set
      if (!selectedVersion && sorted.length > 0) {
        setSelectedVersion('latest');
      }
    } catch (error) {
      console.error('Error fetching KFS versions:', error);
    }
  };

  const handleVersionChange = async (e) => {
    const value = e.target.value;
    setSelectedVersion(value);
    if (!value || value === 'latest') {
      await fetchLoanDetails();
      return;
    }
    try {
      const resp = await loanService.getKfsVersion(id, value);
      setLoan(resp.data);
      // Always fetch the latest repayment schedule for the selected version
      if (resp.data?.repaymentSchedule) {
        setSchedule(resp.data.repaymentSchedule);
      } else {
        setSchedule(null);
        await fetchRepaymentSchedule();
      }
    } catch (error) {
      console.error('Error fetching KFS version:', error);
      toast.error('Failed to load selected KFS version');
    }
  };


  const fetchLoanDetails = async () => {
    try {
      const response = await loanService.getLoanKFS(id);
      console.log('=== LOAN KFS RESPONSE ===');
      console.log('Full response:', response);
      console.log('Response data:', response.data);
      console.log('Initial EMI:', response.data.initialEmi);
      console.log('Latest snapshot:', response.data.latestSnapshot);
      console.log('Repayment schedule:', response.data.repaymentSchedule);
      console.log('========================');
      
      setLoan(response.data);
      setSelectedVersion('latest');
      
      // Always set the schedule if repayment schedule exists
      if (response.data.repaymentSchedule) {
        console.log('Setting repayment schedule from loan data:', response.data.repaymentSchedule);
        console.log('Schedule has repaymentRows:', response.data.repaymentSchedule.repaymentRows?.length);
        console.log('Schedule has installments:', response.data.repaymentSchedule.installments?.length);
        setSchedule(response.data.repaymentSchedule);
      }
      
      // Always try to fetch schedule separately as fallback
      if (!response.data.repaymentSchedule) {
        console.log('No repayment schedule in loan data, fetching separately...');
        await fetchRepaymentSchedule();
      }
      setLoading(false);
    } catch (error) {
      console.error('Error fetching loan:', error);
      toast.error('Failed to fetch loan details: ' + (error.response?.data?.message || error.message));
      setLoading(false);
    }
  };

  const fetchRepaymentSchedule = async () => {
    try {
      // Only fetch schedule if not already available in loan data
      if (!schedule) {
        const response = await loanService.getRepaymentSchedule(id);
        console.log('Repayment schedule:', response.data);
        setSchedule(response.data);
      }
    } catch (error) {
      console.error('Error fetching repayment schedule:', error);
      toast.error('Failed to fetch repayment schedule: ' + (error.response?.data?.message || error.message));
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(amount);
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-IN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (!loan) {
    return (
      <div className="text-center py-12">
        <FileText className="h-16 w-16 text-secondary-400 mx-auto mb-4" />
        <h2 className="text-xl font-semibold text-secondary-900 mb-2">Loan Not Found</h2>
        <p className="text-secondary-600">The requested loan could not be found.</p>
        <Link to="/dashboard" className="btn-primary mt-4 inline-flex items-center">
          Back to Dashboard
        </Link>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto">
      {/* Header */}
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-3xl font-bold text-secondary-900">Key Facts Statement</h1>
          <p className="text-secondary-600 mt-2">Loan ID: {loan.loanId}</p>
        </div>
        <div className="flex space-x-3 items-center">
          <div>
            <label className="block text-xs text-secondary-600 mb-1">KFS Version</label>
            <select
              value={selectedVersion || 'latest'}
              onChange={handleVersionChange}
              className="input-field"
            >
              <option value="latest">Latest</option>
              {versions.map(v => (
                <option key={v.versionNumber} value={v.versionNumber}>
                  v{v.versionNumber} — {new Date(v.createdAt).toLocaleString('en-IN')}
                </option>
              ))}
            </select>
          </div>
          <Link
            to={`/loans/${id}/schedule`}
            className="btn-secondary flex items-center space-x-2"
          >
            <Eye size={16} />
            <span>View Schedule</span>
          </Link>
          <button className="btn-primary flex items-center space-x-2">
            <Download size={16} />
            <span>Download PDF</span>
          </button>
        </div>
      </div>

      {/* KFS Document */}
      <div className="bg-white shadow-lg border-2 border-black p-8 mb-8" style={{ fontFamily: 'Times, serif', fontSize: '14px', lineHeight: '1.4' }}>
        {/* Bank Header */}
        <div className="text-center mb-6 pb-4 border-b-2 border-black">
          <h1 className="text-2xl font-bold text-black mb-2">MyBank Limited</h1>
          <h2 className="text-lg font-bold text-black">KEY FACTS STATEMENT</h2>
          <p className="text-sm text-black mt-1">As per RBI Guidelines</p>
        </div>

        {/* Main KFS Table */}
        <table className="w-full border-collapse border-2 border-black mb-6" style={{ fontSize: '13px' }}>
          <tbody>
            {/* Row 1: Loan Proposal Number */}
            <tr>
              <td className="border border-black px-3 py-2 font-semibold bg-gray-50 w-8 text-center">1.</td>
              <td className="border border-black px-3 py-2 font-semibold w-1/3">Loan Proposal Number</td>
              <td className="border border-black px-3 py-2">{loan.loanId || 'N/A'}</td>
            </tr>
            
            {/* Row 2: Borrower Name */}
            <tr>
              <td className="border border-black px-3 py-2 font-semibold bg-gray-50 w-8 text-center">2.</td>
              <td className="border border-black px-3 py-2 font-semibold">Name of the Borrower</td>
              <td className="border border-black px-3 py-2">{loan.borrowerName || 'Customer Name'}</td>
            </tr>
            
            {/* Row 3: Loan Amount */}
            <tr>
              <td className="border border-black px-3 py-2 font-semibold bg-gray-50 w-8 text-center">3.</td>
              <td className="border border-black px-3 py-2 font-semibold">Sanctioned Amount / Limit</td>
              <td className="border border-black px-3 py-2 ">{formatCurrency(loan.principal)}</td>
            </tr>
            
            {/* Row 4: Disbursement Schedule */}
            <tr>
              <td className="border border-black px-3 py-2 font-semibold bg-gray-50 w-8 text-center">4.</td>
              <td className="border border-black px-3 py-2 font-semibold">Disbursal Schedule</td>
              <td className="border border-black px-3 py-2">
                {loan.disbursementPhases && loan.disbursementPhases.length > 0 ? (
                  <div className="space-y-1">
                    {loan.disbursementPhases.map((phase, index) => (
                      <div key={index} className="text-sm">
                        Phase {phase.sequence || index + 1}: {formatCurrency(phase.amount)} on {formatDate(phase.disbursementDate)}
                      </div>
                    ))}
                  </div>
                ) : (
                  `Full amount on ${formatDate(loan.loanIssueDate)}`
                )}
              </td>
            </tr>
            
            {/* Row 5: Loan Term */}
            <tr>
              <td className="border border-black px-3 py-2 font-semibold bg-gray-50 w-8 text-center">5.</td>
              <td className="border border-black px-3 py-2 font-semibold">Loan Term</td>
              <td className="border border-black px-3 py-2">{loan.months} months ({Math.floor(loan.months / 12)} years {loan.months % 12} months)</td>
            </tr>
            
            {/* Row 6: Installment Details */}
            <tr>
              <td className="border border-black px-3 py-2 font-semibold bg-gray-50 w-8 text-center">6.</td>
              <td className="border border-black px-3 py-2 font-semibold">Installment Details</td>
              <td className="border border-black px-3 py-2">
                <div className="space-y-1">
                  <div><strong>EMI Amount:</strong> {(() => {
                    // Find the first regular EMI (not pre-EMI) from repayment schedule
                    const regularEmi = schedule?.repaymentRows?.find(row => 
                      row.paymentType !== 'PRE_EMI' && row.emi && row.emi > 0
                    );
                    if (regularEmi) return formatCurrency(regularEmi.emi);
                    
                    // Fallback to existing logic
                    if (loan.initialEmi) return formatCurrency(loan.initialEmi);
                    if (loan.latestSnapshot?.monthlyEmi) return formatCurrency(loan.latestSnapshot.monthlyEmi);
                    if (schedule?.installments?.[0]?.emi) return formatCurrency(schedule.installments[0].emi);
                    return formatCurrency(0);
                  })()}</div>
                  <div><strong>First EMI Date:</strong> {(() => {
                    // Find the first regular EMI date (not pre-EMI) from repayment schedule
                    const regularEmi = schedule?.repaymentRows?.find(row => 
                      row.paymentType !== 'PRE_EMI' && row.emi && row.emi > 0
                    );
                    if (regularEmi && regularEmi.paymentDate) return formatDate(regularEmi.paymentDate);
                    
                    // Fallback: add 1 month to start date
                    const startDate = new Date(loan.startDate);
                    startDate.setMonth(startDate.getMonth() + 1);
                    return formatDate(startDate.toISOString().split('T')[0]);
                  })()}</div>
                  <div><strong>Frequency:</strong> Monthly</div>
                </div>
              </td>
            </tr>
            
            {/* Row 7: Interest Rate */}
            <tr>
              <td className="border border-black px-3 py-2 font-semibold bg-gray-50 w-8 text-center">7.</td>
              <td className="border border-black px-3 py-2 font-semibold">Interest Rate</td>
              <td className="border border-black px-3 py-2">
                <div className="space-y-1">
                  <div><strong>Rate:</strong> {loan.annualRate}% per annum ({loan.rateType})</div>
                  {loan.benchmarkName && (
                    <>
                      <div><strong>Benchmark:</strong> {loan.benchmarkName}</div>
                      <div><strong>Spread:</strong> {loan.spread}%</div>
                    </>
                  )}
                </div>
              </td>
            </tr>
            
            {/* Row 8: APR */}
            <tr>
              <td className="border border-black px-3 py-2 font-semibold bg-gray-50 w-8 text-center">8.</td>
              <td className="border border-black px-3 py-2 font-semibold">Annual Percentage Rate (APR)</td>
              <td className="border border-black px-3 py-2">{loan.apr}% per annum</td>
            </tr>
            
            {/* Row 9: Fees and Charges */}
            <tr>
              <td className="border border-black px-3 py-2 font-semibold bg-gray-50 w-8 text-center">9.</td>
              <td className="border border-black px-3 py-2 font-semibold">Fees/Charges payable by borrower</td>
              <td className="border border-black px-3 py-2">
                {loan.charges && loan.charges.length > 0 ? (
                  <div className="space-y-1">
                    {loan.charges.map((charge, index) => (
                      <div key={index} className="text-sm">
                        <strong>{charge.chargeType}:</strong> {formatCurrency(charge.amount)} 
                        {charge.isRecurring && ' (Recurring)'}
                      </div>
                    ))}
                  </div>
                ) : (
                  'As per bank\'s schedule of charges'
                )}
              </td>
            </tr>
            
            {/* Row 10: Total Cost */}
            <tr>
              <td className="border border-black px-3 py-2 font-semibold bg-gray-50 w-8 text-center">10.</td>
              <td className="border border-black px-3 py-2 font-semibold">Total amount payable by borrower</td>
              <td className="border border-black px-3 py-2">
                {formatCurrency(loan.totalAmountPayable || 0)}
              </td>
            </tr>
            
            {/* Row 11: Security */}
            <tr>
              <td className="border border-black px-3 py-2 font-semibold bg-gray-50 w-8 text-center">11.</td>
              <td className="border border-black px-3 py-2 font-semibold">Security, if any</td>
              <td className="border border-black px-3 py-2">As per sanction letter</td>
            </tr>
            
            {/* Row 12: Guarantor */}
            <tr>
              <td className="border border-black px-3 py-2 font-semibold bg-gray-50 w-8 text-center">12.</td>
              <td className="border border-black px-3 py-2 font-semibold">Guarantor, if any</td>
              <td className="border border-black px-3 py-2">As per sanction letter</td>
            </tr>
          </tbody>
        </table>

        {/* Contingent Charges Section */}
        <div className="mb-6">
          <h3 className="text-lg font-bold text-black mb-4 pb-2 border-b-2 border-black">Details of Contingent Charges</h3>
          
          <table className="w-full border-collapse border-2 border-black mb-4" style={{ fontSize: '12px' }}>
            <tbody>
              <tr>
                <td className="border border-black px-3 py-2 font-semibold bg-gray-50 w-8 text-center">(i)</td>
                <td className="border border-black px-3 py-2 font-semibold w-1/3">Penal charges, if any, in case of delayed payment</td>
                <td className="border border-black px-3 py-2">18% per annum (+) Applicable Taxes on overdue installment amount</td>
              </tr>
              
              <tr>
                <td className="border border-black px-3 py-2 font-semibold bg-gray-50 w-8 text-center">(ii)</td>
                <td className="border border-black px-3 py-2 font-semibold">Other penal charges, if any</td>
                <td className="border border-black px-3 py-2">As applicable as per bank's policy</td>
              </tr>
              
              <tr>
                <td className="border border-black px-3 py-2 font-semibold bg-gray-50 w-8 text-center">(iii)</td>
                <td className="border border-black px-3 py-2 font-semibold">Foreclosure charges, if applicable</td>
                <td className="border border-black px-3 py-2">
                  <div className="space-y-1 text-xs">
                    <div><strong>Pre-Payment/Part-Payment:</strong></div>
                    <div>• No charges for part prepayment once per financial year if &le; 25% of principal outstanding</div>
                    <div>• <strong>2.5% (+) Applicable Taxes</strong> if amount &gt; 25% of principal outstanding</div>
                    <div><strong>Premature Closure:</strong></div>
                    <div>• <strong>2.5% (+) Applicable Taxes</strong> of principal outstanding</div>
                    <div>• <strong>NIL</strong> charges post 60 months from last disbursement</div>
                    <div>• <strong>NIL</strong> charges for floating rate individual/MSE loans</div>
                  </div>
                </td>
              </tr>
              
              <tr>
                <td className="border border-black px-3 py-2 font-semibold bg-gray-50 w-8 text-center">(iv)</td>
                <td className="border border-black px-3 py-2 font-semibold">Charges for switching loans from floating to fixed rate and vice versa</td>
                <td className="border border-black px-3 py-2">NIL</td>
              </tr>
              
              <tr>
                <td className="border border-black px-3 py-2 font-semibold bg-gray-50 w-8 text-center">(v)</td>
                <td className="border border-black px-3 py-2 font-semibold">Any other charges</td>
                <td className="border border-black px-3 py-2">
                  <div className="space-y-1 text-xs">
                    <div>• <strong>Repayment Mode Swapping:</strong> Rs. 500/- (+) Taxes per incident</div>
                    <div>• <strong>Payment Return:</strong> Rs. 450/- (+) Taxes per instance</div>
                    <div>• <strong>Duplicate Schedule:</strong> Rs. 50/- (+) Taxes per request</div>
                    <div>• <strong>Property Swapping:</strong> 0.1% of loan amount (Min Rs. 10,000/-, Max Rs. 25,000/-)</div>
                    <div>• <strong>Document Retrieval:</strong> Rs. 75/- (+) Taxes per document set</div>
                    <div>• <strong>Legal/Repossession:</strong> At actuals</div>
                    <div>• <strong>Custody Charges:</strong> Rs. 1,000/- (+) Taxes per month (post 60 days)</div>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        {/* Important Notes Section */}
        <div className="mb-6">
          <h3 className="text-lg font-bold text-black mb-4 pb-2 border-b-2 border-black">Important Notes</h3>
          <div className="text-sm text-black space-y-2 border-2 border-black p-4">
            <p><strong>1.</strong> The APR (Annual Percentage Rate) includes the effect of fees and charges on the cost of borrowing.</p>
            <p><strong>2.</strong> Interest is calculated on daily reducing balance basis.</p>
            <p><strong>3.</strong> EMI amount may vary in case of floating rate loans based on benchmark rate changes.</p>
            <p><strong>4.</strong> Prepayment charges may apply as per bank's policy as detailed above.</p>
            <p><strong>5.</strong> This statement is generated as per RBI guidelines and forms part of the loan agreement.</p>
            <p><strong>6.</strong> For detailed list of charges please visit our website <strong>"www.mybank.com"</strong></p>
            <p><strong>7.</strong> All service charges/fees/commissions mentioned are exclusive of applicable taxes.</p>
          </div>
        </div>

        {/* Full Repayment Schedule - At the end of KFS */}
        {((schedule && schedule.installments && schedule.installments.length > 0) || 
          (schedule && schedule.repaymentRows && schedule.repaymentRows.length > 0)) && (
          <div className="mt-8 border-t-2 border-black pt-6">
            <h3 className="text-lg font-bold text-black mb-4 pb-2 border-b-2 border-black">
              Complete Repayment Schedule
            </h3>
            <div className="overflow-x-auto">
              <table className="w-full border-collapse border-2 border-black text-sm">
                <thead>
                  <tr className="bg-gray-100">
                    <th className="border border-black px-3 py-2 text-center font-bold">Installment No.</th>
                    <th className="border border-black px-3 py-2 text-center font-bold">Due Date</th>
                    <th className="border border-black px-3 py-2 text-center font-bold">EMI Amount (₹)</th>
                    <th className="border border-black px-3 py-2 text-center font-bold">Principal (₹)</th>
                    <th className="border border-black px-3 py-2 text-center font-bold">Interest (₹)</th>
                    <th className="border border-black px-3 py-2 text-center font-bold">Outstanding Balance (₹)</th>
                  </tr>
                </thead>
                <tbody>
                  {(schedule.installments || schedule.repaymentRows || []).map((installment, index) => (
                    <tr key={index} className={index % 2 === 0 ? 'bg-white' : 'bg-gray-50'}>
                      <td className="border border-black px-3 py-2 text-center">
                        {installment.installmentNumber || installment.monthNumber || index + 1}
                      </td>
                      <td className="border border-black px-3 py-2 text-center">
                        {formatDate(installment.dueDate || installment.paymentDate)}
                      </td>
                      <td className="border border-black px-3 py-2 text-right font-medium">
                        {formatCurrency(installment.emi || installment.installmentAmount || 0)}
                      </td>
                      <td className="border border-black px-3 py-2 text-right">
                        {formatCurrency(installment.principal || installment.principalComponent || installment.principalPaid || 0)}
                      </td>
                      <td className="border border-black px-3 py-2 text-right">
                        {formatCurrency(installment.interest || installment.interestComponent || installment.interestPaid || 0)}
                      </td>
                      <td className="border border-black px-3 py-2 text-right font-medium">
                        {formatCurrency(installment.remainingBalance || installment.principalOutstanding || 0)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="mt-4 text-xs text-black border-2 border-black p-3">
              <p><strong>Note:</strong> This schedule shows all {(schedule.installments || schedule.repaymentRows || []).length} installments for the complete loan tenure.</p>
              <p><strong>Disclaimer:</strong> Amounts are calculated based on the loan terms at the time of loan creation and may vary for floating rate loans.</p>
            </div>
          </div>
        )}

        {/* Footer */}
        <div className="text-center mt-8 pt-4 border-t-2 border-black">
          <div className="border-2 border-black p-3">
            <p className="text-sm font-bold text-black mb-2">
              MyBank Limited
            </p>
            <p className="text-xs text-black">
              Generated on {new Date().toLocaleDateString('en-IN', { 
                year: 'numeric', 
                month: 'long', 
                day: 'numeric' 
              })} | CIN: U65100MH1994PLC080618
            </p>
            <p className="text-xs text-black mt-2">
              <strong>This Key Facts Statement is issued in compliance with RBI Guidelines</strong>
            </p>
          </div>
        </div>
      </div>

      {/* Action Buttons */}
      <div className="flex justify-center space-x-4">
        <Link to="/dashboard" className="btn-secondary">
          Back to Dashboard
        </Link>
        <Link to={`/loans/${id}/schedule`} className="btn-primary">
          View Repayment Schedule
        </Link>
      </div>
    </div>
  );
};

export default LoanKFSView;
