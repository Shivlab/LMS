import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { Calendar, Download, Filter, TrendingUp, DollarSign } from 'lucide-react';
import { loanService } from '../services/api';
import { toast } from 'react-hot-toast';

const RepaymentSchedule = () => {
  const { id } = useParams();
  const [schedule, setSchedule] = useState(null);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState({
    fromMonth: '',
    toMonth: '',
    showPaidOnly: false
  });

  useEffect(() => {
    fetchRepaymentSchedule();
  }, [id]);

  const fetchRepaymentSchedule = async () => {
    try {
      const response = await loanService.getRepaymentSchedule(id);
      setSchedule(response.data);
    } catch (error) {
      toast.error('Failed to fetch repayment schedule');
      console.error('Error fetching schedule:', error);
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
      month: 'short',
      day: 'numeric'
    });
  };

  const getFilteredRows = () => {
    if (!schedule?.repaymentRows) return [];
    
    return schedule.repaymentRows.filter(row => {
      if (filters.fromMonth && row.monthNumber < parseInt(filters.fromMonth)) return false;
      if (filters.toMonth && row.monthNumber > parseInt(filters.toMonth)) return false;
      if (filters.showPaidOnly && row.status !== 'PAID') return false;
      return true;
    });
  };

  const calculateTotals = (rows) => {
    return rows.reduce((totals, row) => ({
      totalEmi: totals.totalEmi + row.emi,
      totalPrincipal: totals.totalPrincipal + row.principalPaid,
      totalInterest: totals.totalInterest + row.interestPaid
    }), { totalEmi: 0, totalPrincipal: 0, totalInterest: 0 });
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (!schedule) {
    return (
      <div className="text-center py-12">
        <Calendar className="h-16 w-16 text-secondary-400 mx-auto mb-4" />
        <h2 className="text-xl font-semibold text-secondary-900 mb-2">Schedule Not Available</h2>
        <p className="text-secondary-600">The repayment schedule could not be loaded.</p>
      </div>
    );
  }

  const filteredRows = getFilteredRows();
  const totals = calculateTotals(filteredRows);

  return (
    <div className="max-w-7xl mx-auto">
      {/* Header */}
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-3xl font-bold text-secondary-900">Repayment Schedule</h1>
          <p className="text-secondary-600 mt-2">
            Loan ID: {id} | Snapshot Date: {formatDate(schedule.snapshotDate)}
          </p>
        </div>
        <div className="flex space-x-3">
          <button className="btn-secondary flex items-center space-x-2">
            <Download size={16} />
            <span>Export CSV</span>
          </button>
          <button className="btn-primary flex items-center space-x-2">
            <Download size={16} />
            <span>Download PDF</span>
          </button>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
        <div className="card">
          <div className="card-body">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-secondary-600">Principal Balance</p>
                <p className="text-2xl font-bold text-secondary-900">{formatCurrency(schedule.principalBalance)}</p>
              </div>
              <DollarSign className="h-8 w-8 text-primary-600" />
            </div>
          </div>
        </div>

        <div className="card">
          <div className="card-body">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-secondary-600">Current Rate</p>
                <p className="text-2xl font-bold text-secondary-900">{schedule.currentRate}%</p>
              </div>
              <TrendingUp className="h-8 w-8 text-green-600" />
            </div>
          </div>
        </div>

        <div className="card">
          <div className="card-body">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-secondary-600">Months Remaining</p>
                <p className="text-2xl font-bold text-secondary-900">{schedule.monthsRemaining}</p>
              </div>
              <Calendar className="h-8 w-8 text-blue-600" />
            </div>
          </div>
        </div>

        <div className="card">
          <div className="card-body">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-secondary-600">APR</p>
                <p className="text-2xl font-bold text-secondary-900">{schedule.apr}%</p>
              </div>
              <TrendingUp className="h-8 w-8 text-purple-600" />
            </div>
          </div>
        </div>
      </div>

      {/* Filters */}
      <div className="card mb-6">
        <div className="card-header">
          <div className="flex items-center space-x-2">
            <Filter size={20} />
            <h2 className="text-lg font-semibold text-secondary-900">Filters</h2>
          </div>
        </div>
        <div className="card-body">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div>
              <label className="block text-sm font-medium text-secondary-700 mb-2">
                From Month
              </label>
              <input
                type="number"
                min="1"
                max={schedule.repaymentRows?.length || 1}
                value={filters.fromMonth}
                onChange={(e) => setFilters(prev => ({ ...prev, fromMonth: e.target.value }))}
                className="input-field"
                placeholder="1"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-secondary-700 mb-2">
                To Month
              </label>
              <input
                type="number"
                min="1"
                max={schedule.repaymentRows?.length || 1}
                value={filters.toMonth}
                onChange={(e) => setFilters(prev => ({ ...prev, toMonth: e.target.value }))}
                className="input-field"
                placeholder={schedule.repaymentRows?.length || ''}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-secondary-700 mb-2">
                Show Paid Only
              </label>
              <select
                value={filters.showPaidOnly}
                onChange={(e) => setFilters(prev => ({ ...prev, showPaidOnly: e.target.value === 'true' }))}
                className="input-field"
              >
                <option value="false">All Payments</option>
                <option value="true">Paid Only</option>
              </select>
            </div>
            <div className="flex items-end">
              <button
                onClick={() => setFilters({ fromMonth: '', toMonth: '', showPaidOnly: false })}
                className="btn-secondary w-full"
              >
                Clear Filters
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Schedule Table */}
      <div className="card">
        <div className="card-header">
          <div className="flex justify-between items-center">
            <h2 className="text-lg font-semibold text-secondary-900">Payment Schedule</h2>
            <div className="text-sm text-secondary-600">
              Showing {filteredRows.length} of {schedule.repaymentRows?.length || 0} payments
            </div>
          </div>
        </div>
        <div className="card-body p-0">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-secondary-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-secondary-500 uppercase tracking-wider">
                    Month
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-secondary-500 uppercase tracking-wider">
                    Due Date
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider">
                    EMI Amount
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider">
                    Principal
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider">
                    Interest
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-secondary-500 uppercase tracking-wider">
                    Balance
                  </th>
                  <th className="px-6 py-3 text-center text-xs font-medium text-secondary-500 uppercase tracking-wider">
                    Rate %
                  </th>
                  <th className="px-6 py-3 text-center text-xs font-medium text-secondary-500 uppercase tracking-wider">
                    Status
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-secondary-200">
                {filteredRows.map((row, index) => (
                  <tr key={index} className={index % 2 === 0 ? 'bg-white' : 'bg-secondary-50'}>
                    <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-secondary-900">
                      {row.monthNumber}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-secondary-900">
                      {formatDate(row.paymentDate)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-secondary-900 text-right font-medium">
                      {formatCurrency(row.emi)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-secondary-900 text-right">
                      {formatCurrency(row.principalPaid)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-secondary-900 text-right">
                      {formatCurrency(row.interestPaid)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-secondary-900 text-right">
                      {formatCurrency(row.remainingBalance)}
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-sm text-secondary-900 text-center">
                      {row.currentRate}%
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-center">
                      <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${
                        row.status === 'PAID' 
                          ? 'bg-green-100 text-green-800'
                          : row.status === 'DUE'
                          ? 'bg-yellow-100 text-yellow-800'
                          : 'bg-secondary-100 text-secondary-800'
                      }`}>
                        {row.status || 'PENDING'}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
              {filteredRows.length > 0 && (
                <tfoot className="bg-secondary-100">
                  <tr>
                    <td colSpan="2" className="px-6 py-4 text-sm font-semibold text-secondary-900">
                      Totals (Filtered)
                    </td>
                    <td className="px-6 py-4 text-sm font-semibold text-secondary-900 text-right">
                      {formatCurrency(totals.totalEmi)}
                    </td>
                    <td className="px-6 py-4 text-sm font-semibold text-secondary-900 text-right">
                      {formatCurrency(totals.totalPrincipal)}
                    </td>
                    <td className="px-6 py-4 text-sm font-semibold text-secondary-900 text-right">
                      {formatCurrency(totals.totalInterest)}
                    </td>
                    <td colSpan="3"></td>
                  </tr>
                </tfoot>
              )}
            </table>
          </div>
        </div>
      </div>

      {filteredRows.length === 0 && (
        <div className="text-center py-12">
          <Calendar className="h-16 w-16 text-secondary-400 mx-auto mb-4" />
          <h3 className="text-lg font-semibold text-secondary-900 mb-2">No Payments Found</h3>
          <p className="text-secondary-600">No payments match the current filter criteria.</p>
        </div>
      )}
    </div>
  );
};

export default RepaymentSchedule;
