import React, { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { toast } from 'react-hot-toast';
import { Plus, TrendingUp, Calendar, History, RefreshCw } from 'lucide-react';
import { benchmarkService } from '../services/api';

const BenchmarkManagement = () => {
  const [benchmarks, setBenchmarks] = useState([]);
  const [selectedBenchmark, setSelectedBenchmark] = useState(null);
  const [benchmarkHistory, setBenchmarkHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showAddForm, setShowAddForm] = useState(false);

  const { register, handleSubmit, reset, formState: { errors } } = useForm();

  useEffect(() => {
    fetchBenchmarks();
  }, []);

  const fetchBenchmarks = async () => {
    try {
      const response = await benchmarkService.getAllBenchmarks();
      setBenchmarks(response.data);
    } catch (error) {
      toast.error('Failed to fetch benchmarks');
      console.error('Error fetching benchmarks:', error);
    } finally {
      setLoading(false);
    }
  };

  const fetchBenchmarkHistory = async (benchmarkName) => {
    try {
      const response = await benchmarkService.getBenchmarkHistory(benchmarkName);
      setBenchmarkHistory(response.data);
      setSelectedBenchmark(benchmarkName);
    } catch (error) {
      toast.error('Failed to fetch benchmark history');
      console.error('Error fetching history:', error);
    }
  };

  const onSubmit = async (data) => {
    try {
      const benchmarkData = {
        ...data,
        rate: parseFloat(data.rate),
        effectiveDate: data.effectiveDate || new Date().toISOString().split('T')[0]
      };

      await benchmarkService.addBenchmark(benchmarkData);
      toast.success('Benchmark added successfully!');
      reset();
      setShowAddForm(false);
      fetchBenchmarks();
    } catch (error) {
      toast.error('Failed to add benchmark: ' + (error.response?.data?.message || error.message));
    }
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  };

  const formatDateTime = (dateString) => {
    return new Date(dateString).toLocaleString('en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto">
      {/* Header */}
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-3xl font-bold text-secondary-900">Benchmark Management</h1>
          <p className="text-secondary-600 mt-2">Manage interest rate benchmarks and their historical rates</p>
        </div>
        <button
          onClick={() => setShowAddForm(true)}
          className="btn-primary flex items-center space-x-2"
        >
          <Plus size={16} />
          <span>Add Benchmark</span>
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Current Benchmarks */}
        <div className="card">
          <div className="card-header">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-2">
                <TrendingUp size={20} />
                <h2 className="text-lg font-semibold text-secondary-900">Current Benchmarks</h2>
              </div>
              <button
                onClick={fetchBenchmarks}
                className="btn-secondary p-2"
                title="Refresh"
              >
                <RefreshCw size={16} />
              </button>
            </div>
          </div>
          <div className="card-body">
            {benchmarks.length === 0 ? (
              <div className="text-center py-8">
                <TrendingUp className="h-12 w-12 text-secondary-400 mx-auto mb-4" />
                <p className="text-secondary-600">No benchmarks available</p>
              </div>
            ) : (
              <div className="space-y-4">
                {benchmarks.map((benchmark, index) => (
                  <div
                    key={index}
                    className={`p-4 border rounded-lg cursor-pointer transition-colors ${
                      selectedBenchmark === benchmark.benchmarkName
                        ? 'border-primary-500 bg-primary-50'
                        : 'border-secondary-200 hover:border-secondary-300'
                    }`}
                    onClick={() => fetchBenchmarkHistory(benchmark.benchmarkName)}
                  >
                    <div className="flex justify-between items-center">
                      <div>
                        <h3 className="font-semibold text-secondary-900">{benchmark.benchmarkName}</h3>
                        <p className="text-sm text-secondary-600">
                          Effective: {formatDate(benchmark.effectiveDate)}
                        </p>
                      </div>
                      <div className="text-right">
                        <div className="text-2xl font-bold text-primary-600">{benchmark.rate}%</div>
                        <div className="text-xs text-secondary-500">
                          Updated: {formatDateTime(benchmark.createdAt)}
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Benchmark History */}
        <div className="card">
          <div className="card-header">
            <div className="flex items-center space-x-2">
              <History size={20} />
              <h2 className="text-lg font-semibold text-secondary-900">
                {selectedBenchmark ? `${selectedBenchmark} History` : 'Benchmark History'}
              </h2>
            </div>
          </div>
          <div className="card-body">
            {!selectedBenchmark ? (
              <div className="text-center py-8">
                <History className="h-12 w-12 text-secondary-400 mx-auto mb-4" />
                <p className="text-secondary-600">Select a benchmark to view its history</p>
              </div>
            ) : benchmarkHistory.length === 0 ? (
              <div className="text-center py-8">
                <History className="h-12 w-12 text-secondary-400 mx-auto mb-4" />
                <p className="text-secondary-600">No history available for {selectedBenchmark}</p>
              </div>
            ) : (
              <div className="space-y-3 max-h-96 overflow-y-auto">
                {benchmarkHistory.map((entry, index) => (
                  <div
                    key={index}
                    className="flex justify-between items-center p-3 border border-secondary-200 rounded-lg"
                  >
                    <div>
                      <div className="font-medium text-secondary-900">{entry.rate}%</div>
                      <div className="text-sm text-secondary-600">
                        Effective: {formatDate(entry.effectiveDate)}
                      </div>
                    </div>
                    <div className="text-right">
                      <div className="text-sm text-secondary-600">
                        Added: {formatDateTime(entry.createdAt)}
                      </div>
                      {index === 0 && (
                        <span className="inline-flex px-2 py-1 text-xs font-semibold rounded-full bg-green-100 text-green-800">
                          Current
                        </span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Add Benchmark Modal */}
      {showAddForm && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-md mx-4">
            <div className="flex justify-between items-center mb-6">
              <h3 className="text-lg font-semibold text-secondary-900">Add New Benchmark</h3>
              <button
                onClick={() => {
                  setShowAddForm(false);
                  reset();
                }}
                className="text-secondary-400 hover:text-secondary-600"
              >
                ×
              </button>
            </div>

            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-secondary-700 mb-2">
                  Benchmark Name
                </label>
                <input
                  type="text"
                  {...register('benchmarkName', { 
                    required: 'Benchmark name is required',
                    pattern: {
                      value: /^[A-Z_]+$/,
                      message: 'Use uppercase letters and underscores only (e.g., MCLR, REPO_RATE)'
                    }
                  })}
                  className="input-field"
                  placeholder="MCLR"
                />
                {errors.benchmarkName && (
                  <p className="text-red-500 text-sm mt-1">{errors.benchmarkName.message}</p>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-secondary-700 mb-2">
                  Rate (%)
                </label>
                <input
                  type="number"
                  step="0.01"
                  {...register('rate', { 
                    required: 'Rate is required',
                    min: { value: 0, message: 'Rate must be positive' },
                    max: { value: 50, message: 'Rate cannot exceed 50%' }
                  })}
                  className="input-field"
                  placeholder="8.50"
                />
                {errors.rate && (
                  <p className="text-red-500 text-sm mt-1">{errors.rate.message}</p>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-secondary-700 mb-2">
                  Effective Date
                </label>
                <input
                  type="date"
                  {...register('effectiveDate')}
                  className="input-field"
                  defaultValue={new Date().toISOString().split('T')[0]}
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-secondary-700 mb-2">
                  Description (Optional)
                </label>
                <textarea
                  {...register('description')}
                  className="input-field"
                  rows="3"
                  placeholder="Optional description for this benchmark rate..."
                />
              </div>

              <div className="flex justify-end space-x-3 pt-4">
                <button
                  type="button"
                  onClick={() => {
                    setShowAddForm(false);
                    reset();
                  }}
                  className="btn-secondary"
                >
                  Cancel
                </button>
                <button type="submit" className="btn-primary">
                  Add Benchmark
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default BenchmarkManagement;
