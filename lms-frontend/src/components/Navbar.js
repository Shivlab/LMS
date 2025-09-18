import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Home, PlusCircle, Settings, BarChart3 } from 'lucide-react';

const Navbar = () => {
  const location = useLocation();

  const navItems = [
    { path: '/dashboard', label: 'Dashboard', icon: Home },
    { path: '/loans/create', label: 'Create Loan', icon: PlusCircle },
    { path: '/admin/benchmarks', label: 'Benchmarks', icon: BarChart3 },
  ];

  return (
    <nav className="bg-white shadow-lg border-b border-secondary-200">
      <div className="container mx-auto px-4">
        <div className="flex justify-between items-center h-16">
          <div className="flex items-center space-x-8">
            <Link to="/dashboard" className="text-xl font-bold text-primary-600">
              LMS
            </Link>
            <div className="hidden md:flex space-x-6">
              {navItems.map((item) => {
                const Icon = item.icon;
                const isActive = location.pathname === item.path;
                return (
                  <Link
                    key={item.path}
                    to={item.path}
                    className={`flex items-center space-x-2 px-3 py-2 rounded-lg transition-colors duration-200 ${
                      isActive
                        ? 'bg-primary-100 text-primary-700'
                        : 'text-secondary-600 hover:text-primary-600 hover:bg-secondary-100'
                    }`}
                  >
                    <Icon size={18} />
                    <span>{item.label}</span>
                  </Link>
                );
              })}
            </div>
          </div>
          <div className="flex items-center space-x-4">
            <span className="text-sm text-secondary-600">Mini Loan Management System</span>
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
