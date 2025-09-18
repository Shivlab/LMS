import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import Navbar from './components/Navbar';
import Dashboard from './components/Dashboard';
import LoanCreateForm from './components/LoanCreateForm';
import LoanEdit from './components/LoanEdit';
import LoanKFSView from './components/LoanKFSView';
import RepaymentSchedule from './components/RepaymentSchedule';
import BenchmarkManagement from './components/BenchmarkManagement';
import './index.css';

function App() {
  return (
    <Router>
      <div className="min-h-screen bg-secondary-50">
        <Navbar />
        <main className="container mx-auto px-4 py-8">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/loans/create" element={<LoanCreateForm />} />
            <Route path="/loans/:id/edit" element={<LoanEdit />} />
            <Route path="/loans/:id/kfs" element={<LoanKFSView />} />
            <Route path="/loans/:id/schedule" element={<RepaymentSchedule />} />
            <Route path="/benchmarks" element={<BenchmarkManagement />} />
          </Routes>
        </main>
        <Toaster
          position="top-right"
          toastOptions={{
            duration: 4000,
            style: {
              background: '#363636',
              color: '#fff',
            },
          }}
        />
      </div>
    </Router>
  );
}

export default App;
