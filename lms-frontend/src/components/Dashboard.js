import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { PlusCircle, FileText, TrendingUp, Search, Eye } from 'lucide-react';
import { loanService } from '../services/api';

const Dashboard = () => {
  const [loans, setLoans] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchRecentLoans();
  }, []);

  const fetchRecentLoans = async () => {
    setLoading(true);
    try {
      const response = await loanService.getAllLoans();
      setLoans(response.data.slice(0, 5)); // Show only recent 5 loans
    } catch (error) {
      console.error('Error fetching loans:', error);
      // Set mock data for demo
      setLoans([
        {
          loanId: '123e4567-e89b-12d3-a456-426614174000',
          productType: 'HOME_LOAN',
          principal: 5000000,
          annualRate: 8.5,
          status: 'ACTIVE',
          loanIssueDate: '2024-01-15',
          months: 240
        },
        {
          loanId: '987fcdeb-51a2-43d7-8f9e-123456789abc',
          productType: 'PERSONAL_LOAN',
          principal: 1000000,
          annualRate: 12.0,
          status: 'ACTIVE',
          loanIssueDate: '2024-02-01',
          months: 60
        }
      ]);
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


  return (
    <div className="space-y-8">
      <div className="flex justify-between items-center">
        <h1 className="text-3xl font-bold text-secondary-900">Dashboard</h1>
        <Link to="/loans/create" className="btn-primary flex items-center space-x-2">
          <PlusCircle size={20} />
          <span>Create New Loan</span>
        </Link>
      </div>


      {/* Quick Actions */}
      <div className="card">
        <div className="card-header">
          <h2 className="text-xl font-semibold text-secondary-900">Quick Actions</h2>
        </div>
        <div className="card-body">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <Link
              to="/loans/create"
              className="p-6 border-2 border-dashed border-secondary-300 rounded-lg hover:border-primary-400 hover:bg-primary-50 transition-colors duration-200 text-center"
            >
              <PlusCircle className="h-8 w-8 mx-auto mb-2 text-secondary-400" />
              <h3 className="font-medium text-secondary-900">Create New Loan</h3>
              <p className="text-sm text-secondary-600 mt-1">Start a new loan application</p>
            </Link>
            
            <Link
              to="/admin/benchmarks"
              className="p-6 border-2 border-dashed border-secondary-300 rounded-lg hover:border-primary-400 hover:bg-primary-50 transition-colors duration-200 text-center"
            >
              <TrendingUp className="h-8 w-8 mx-auto mb-2 text-secondary-400" />
              <h3 className="font-medium text-secondary-900">Manage Benchmarks</h3>
              <p className="text-sm text-secondary-600 mt-1">Update interest rate benchmarks</p>
            </Link>
            
            <div className="p-6 border-2 border-dashed border-secondary-300 rounded-lg hover:border-primary-400 hover:bg-primary-50 transition-colors duration-200 text-center cursor-pointer">
              <FileText className="h-8 w-8 mx-auto mb-2 text-secondary-400" />
              <h3 className="font-medium text-secondary-900">View Reports</h3>
              <p className="text-sm text-secondary-600 mt-1">Generate loan reports</p>
            </div>
          </div>
        </div>
      </div>

      {/* Existing Loans */}
      <div className="card">
        <div className="card-header">
          <div className="flex justify-between items-center">
            <h2 className="text-xl font-semibold text-secondary-900">Recent Loans</h2>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-secondary-400 h-4 w-4" />
              <input
                type="text"
                placeholder="Search by Loan ID..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-10 pr-4 py-2 border border-secondary-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              />
            </div>
          </div>
        </div>
        <div className="card-body">
          {loading ? (
            <div className="text-center py-8">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600 mx-auto"></div>
              <p className="text-secondary-600 mt-2">Loading loans...</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-secondary-200">
                <thead className="bg-secondary-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-secondary-500 uppercase tracking-wider">
                      Loan ID
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-secondary-500 uppercase tracking-wider">
                      Product Type
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-secondary-500 uppercase tracking-wider">
                      Principal
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-secondary-500 uppercase tracking-wider">
                      Rate
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-secondary-500 uppercase tracking-wider">
                      Status
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-secondary-500 uppercase tracking-wider">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-secondary-200">
                  {loans
                    .filter(loan => 
                      !searchTerm || 
                      loan.loanId.toLowerCase().includes(searchTerm.toLowerCase()) ||
                      loan.productType.toLowerCase().includes(searchTerm.toLowerCase())
                    )
                    .map((loan) => (
                    <tr key={loan.loanId} className="hover:bg-secondary-50">
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-secondary-900">
                        {loan.loanId.substring(0, 8)}...
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-secondary-600">
                        {loan.productType.replace('_', ' ')}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-secondary-600">
                        {formatCurrency(loan.principal)}
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-secondary-600">
                        {loan.annualRate}%
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${
                          loan.status === 'ACTIVE' 
                            ? 'bg-green-100 text-green-800' 
                            : 'bg-gray-100 text-gray-800'
                        }`}>
                          {loan.status}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium">
                        <div className="flex space-x-2">
                          <Link
                            to={`/loans/${loan.loanId}/edit`}
                            className="inline-flex items-center px-3 py-1 border border-transparent text-sm leading-4 font-medium rounded-md text-blue-700 bg-blue-100 hover:bg-blue-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                          >
                            <svg className="h-4 w-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                            </svg>
                            Edit
                          </Link>
                          <Link
                            to={`/loans/${loan.loanId}/kfs`}
                            className="inline-flex items-center px-3 py-1 border border-transparent text-sm leading-4 font-medium rounded-md text-primary-700 bg-primary-100 hover:bg-primary-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary-500"
                          >
                            <Eye className="h-4 w-4 mr-1" />
                            View KFS
                          </Link>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {loans.length === 0 && (
                <div className="text-center py-8">
                  <FileText className="h-12 w-12 text-secondary-400 mx-auto mb-4" />
                  <p className="text-secondary-600">No loans found</p>
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Recent Activity */}
      <div className="card">
        <div className="card-header">
          <h2 className="text-xl font-semibold text-secondary-900">Recent Activity</h2>
        </div>
        <div className="card-body">
          <div className="space-y-4">
            <div className="flex items-center space-x-4 p-4 bg-secondary-50 rounded-lg">
              <div className="w-2 h-2 bg-green-500 rounded-full"></div>
              <div className="flex-1">
                <p className="text-sm font-medium text-secondary-900">New loan created</p>
                <p className="text-xs text-secondary-600">Home Loan - ₹50,00,000 - 2 hours ago</p>
              </div>
            </div>
            <div className="flex items-center space-x-4 p-4 bg-secondary-50 rounded-lg">
              <div className="w-2 h-2 bg-blue-500 rounded-full"></div>
              <div className="flex-1">
                <p className="text-sm font-medium text-secondary-900">Rate reset applied</p>
                <p className="text-xs text-secondary-600">MCLR benchmark updated - 5 hours ago</p>
              </div>
            </div>
            <div className="flex items-center space-x-4 p-4 bg-secondary-50 rounded-lg">
              <div className="w-2 h-2 bg-yellow-500 rounded-full"></div>
              <div className="flex-1">
                <p className="text-sm font-medium text-secondary-900">Benchmark added</p>
                <p className="text-xs text-secondary-600">New MCLR rate: 8.25% - 1 day ago</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
