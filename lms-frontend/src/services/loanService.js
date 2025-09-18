import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api/v1';

const loanService = {
    // Existing loan operations
    createLoan: (loanData) => {
        return axios.post(`${API_BASE_URL}/loans`, loanData);
    },

    getAllLoans: () => {
        return axios.get(`${API_BASE_URL}/loans`);
    },

    getLoanById: (id) => {
        return axios.get(`${API_BASE_URL}/loans/${id}`);
    },

    getLoanKFS: (id) => {
        return axios.get(`${API_BASE_URL}/loans/${id}/kfs`);
    },

    // New edit operations
    editLoan: (id, editData) => {
        return axios.put(`${API_BASE_URL}/loans/${id}`, editData);
    },

    // Version history operations
    getLoanVersions: (id) => {
        return axios.get(`${API_BASE_URL}/loans/${id}/versions`);
    },

    getKfsVersions: (id) => {
        return axios.get(`${API_BASE_URL}/loans/${id}/kfs-versions`);
    },

    getKfsVersion: (id, versionNumber) => {
        return axios.get(`${API_BASE_URL}/loans/${id}/kfs-versions/${versionNumber}`);
    },

    // Loan charges management
    addLoanCharge: (id, chargeData) => {
        return axios.post(`${API_BASE_URL}/loans/${id}/charges/new`, chargeData);
    },

    updateLoanCharge: (id, chargeId, chargeData) => {
        return axios.put(`${API_BASE_URL}/loans/${id}/charges/${chargeId}`, chargeData);
    },

    deleteLoanCharge: (id, chargeId) => {
        return axios.delete(`${API_BASE_URL}/loans/${id}/charges/${chargeId}`);
    },

    // Disbursement phases management
    getDisbursements: (id) => {
        return axios.get(`${API_BASE_URL}/loans/${id}/disbursements`);
    },

    updateDisbursements: (id, disbursements) => {
        return axios.put(`${API_BASE_URL}/loans/${id}/disbursements`, disbursements);
    },

    deleteDisbursement: (id, disbursementId) => {
        return axios.delete(`${API_BASE_URL}/loans/${id}/disbursements/${disbursementId}`);
    },

    // Moratorium management
    addMoratoriumPeriod: (id, moratoriumData) => {
        return axios.post(`${API_BASE_URL}/loans/${id}/moratorium`, moratoriumData);
    },

    updateMoratoriumPeriod: (id, moratoriumId, moratoriumData) => {
        return axios.put(`${API_BASE_URL}/loans/${id}/moratorium/${moratoriumId}`, moratoriumData);
    },

    deleteMoratoriumPeriod: (id, moratoriumId) => {
        return axios.delete(`${API_BASE_URL}/loans/${id}/moratorium/${moratoriumId}`);
    },

    // Prepayment operations
    recordPrepayment: (id, prepaymentData) => {
        return axios.post(`${API_BASE_URL}/loans/${id}/prepayments`, prepaymentData);
    },

    getPrepaymentHistory: (id) => {
        return axios.get(`${API_BASE_URL}/loans/${id}/prepayments`);
    },

    // Benchmark rate operations
    addBenchmarkRate: (benchmarkName, rate) => {
        return axios.post(`${API_BASE_URL}/benchmarks/${benchmarkName}/rates?rate=${rate}`);
    },

    getBenchmarkHistory: (benchmarkName) => {
        return axios.get(`${API_BASE_URL}/benchmarks/${benchmarkName}/history`);
    },

    getCurrentBenchmarkRate: (benchmarkName) => {
        return axios.get(`${API_BASE_URL}/benchmarks/${benchmarkName}/current`);
    },

    getAllBenchmarkNames: () => {
        return axios.get(`${API_BASE_URL}/benchmarks`);
    },

    // Loan status operations
    updateLoanStatus: (id, status, reason) => {
        return axios.patch(`${API_BASE_URL}/loans/${id}/status`, {
            status: status,
            changeReason: reason || 'Status update',
            changeDescription: `Loan status changed to ${status}`
        });
    },

    // Rate reset operations
    applyRateReset: (id, newRate, reason) => {
        return axios.patch(`${API_BASE_URL}/loans/${id}/rate-reset`, {
            annualRate: newRate,
            changeReason: reason || 'Rate reset',
            changeDescription: `Interest rate updated to ${newRate}%`
        });
    }
};

export default loanService;
