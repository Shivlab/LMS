import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080/api/v1';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor for adding auth tokens if needed
api.interceptors.request.use(
  (config) => {
    // Add auth token here if implementing authentication
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor for handling errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API Error:', error.response?.data || error.message);
    return Promise.reject(error);
  }
);

export const loanService = {
  createLoan: (loanData) => api.post('/loans', loanData),
  getAllLoans: () => api.get('/loans'),
  getLoanKFS: (loanId) => api.get(`/loans/${loanId}`),
  getKfsVersions: (loanId) => api.get(`/loans/${loanId}/kfs-versions`),
  getKfsVersion: (loanId, versionNumber) => api.get(`/loans/${loanId}/kfs-versions/${versionNumber}`),
  getRepaymentSchedule: (loanId, snapshotId = null) => {
    const params = snapshotId ? { snapshotId } : {};
    return api.get(`/loans/${loanId}/schedule`, { params });
  },
  addLoanCharge: (loanId, chargeData) => api.post(`/loans/${loanId}/charges`, chargeData),
  forceRateReset: (loanId, benchmarkName, newRate) => 
    api.post(`/loans/${loanId}/force-reset`, null, {
      params: { benchmarkName, newRate }
    }),
};

export const benchmarkService = {
  addBenchmark: (benchmarkData) => api.post('/benchmarks', benchmarkData),
  getAllBenchmarks: () => api.get('/benchmarks'),
  getBenchmarkHistory: (benchmarkName) => api.get(`/benchmarks/${benchmarkName}`),
  getLatestBenchmark: (benchmarkName) => api.get(`/benchmarks/${benchmarkName}/latest`),
};

export default api;
