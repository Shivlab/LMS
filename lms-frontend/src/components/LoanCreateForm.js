import React, { useState } from 'react';
import { useForm, useFieldArray } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import { toast } from 'react-hot-toast';
import { Plus, Trash2, Calendar, DollarSign } from 'lucide-react';
import { loanService } from '../services/api';

const LoanCreateForm = () => {
  const navigate = useNavigate();
  const [isSubmitting, setIsSubmitting] = useState(false);
  
  const { register, control, handleSubmit, watch, formState: { errors } } = useForm({
    defaultValues: {
      productType: 'HOME_LOAN',
      rateType: 'FIXED',
      compoundingFrequency: 'DAILY',
      floatingStrategy: 'EMI_CONSTANT',
      moratoriumType: 'FULL',
      moratoriumMonths: 0,
      disbursementPhases: [],
      charges: []
    }
  });

  const { fields: disbursementFields, append: appendDisbursement, remove: removeDisbursement } = useFieldArray({
    control,
    name: 'disbursementPhases'
  });

  const { fields: chargeFields, append: appendCharge, remove: removeCharge } = useFieldArray({
    control,
    name: 'charges'
  });

  const watchRateType = watch('rateType');
  const watchProductType = watch('productType');

  const onSubmit = async (data) => {
    setIsSubmitting(true);
    try {
      // Format dates and numbers for API
      const formattedData = {
        ...data,
        principal: parseFloat(data.principal),
        annualRate: parseFloat(data.annualRate),
        months: parseInt(data.months),
        moratoriumMonths: parseInt(data.moratoriumMonths || 0),
        loanIssueDate: data.loanIssueDate || new Date().toISOString().split('T')[0],
        startDate: data.startDate || new Date().toISOString().split('T')[0],
        disbursementPhases: data.disbursementPhases?.map(phase => ({
          ...phase,
          amount: parseFloat(phase.amount),
          sequence: parseInt(phase.sequence || 1)
        })) || [],
        charges: data.charges?.map(charge => ({
          ...charge,
          amount: parseFloat(charge.amount),
          isRecurring: charge.isRecurring === 'true'
        })) || [],
        customerId: 'CUST' + Math.floor(Math.random() * 10000) // Add a random customer ID if not provided
      };

      if (watchRateType === 'FLOATING') {
        formattedData.spread = parseFloat(data.spread || 0);
        formattedData.resetPeriodicityMonths = parseInt(data.resetPeriodicityMonths || 12);
      }

      console.log('Submitting loan data:', formattedData);
      const response = await loanService.createLoan(formattedData);
      console.log('=== LOAN CREATION RESPONSE ===');
      console.log('Full response:', response);
      console.log('Response data:', response.data);
      console.log('Loan ID:', response.data.loanId);
      console.log('Initial EMI:', response.data.initialEmi);
      console.log('Repayment Schedule:', response.data.repaymentSchedule);
      console.log('==============================');
      
      if (response.data && response.data.loanId) {
        toast.success('Loan created successfully!');
        navigate(`/loans/${response.data.loanId}/kfs`);
      } else {
        throw new Error('Invalid response format from server');
      }
    } catch (error) {
      console.error('Error creating loan:', error);
      const errorMessage = error.response?.data?.message || 
                         error.response?.data || 
                         error.message || 
                         'Failed to create loan';
      toast.error(errorMessage);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-secondary-900">Create New Loan</h1>
        <p className="text-secondary-600 mt-2">Enter loan details to generate KFS and repayment schedule</p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-8">
        {/* Basic Loan Information */}
        <div className="card">
          <div className="card-header">
            <h2 className="text-xl font-semibold text-secondary-900">Basic Information</h2>
          </div>
          <div className="card-body">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-secondary-700 mb-2">
                  Product Type
                </label>
                <select {...register('productType', { required: 'Product type is required' })} className="input-field">
                  <option value="HOME_LOAN">Home Loan</option>
                  <option value="PERSONAL_LOAN">Personal Loan</option>
                  <option value="CAR_LOAN">Car Loan</option>
                  <option value="BUSINESS_LOAN">Business Loan</option>
                </select>
                {errors.productType && <p className="text-red-500 text-sm mt-1">{errors.productType.message}</p>}
              </div>

              <div>
                <label className="block text-sm font-medium text-secondary-700 mb-2">
                  Principal Amount (₹)
                </label>
                <input
                  type="number"
                  step="0.01"
                  {...register('principal', { 
                    required: 'Principal amount is required',
                    min: { value: 1, message: 'Amount must be greater than 0' }
                  })}
                  className="input-field"
                  placeholder="5000000"
                />
                {errors.principal && <p className="text-red-500 text-sm mt-1">{errors.principal.message}</p>}
              </div>

              <div>
                <label className="block text-sm font-medium text-secondary-700 mb-2">
                  Annual Interest Rate (%)
                </label>
                <input
                  type="number"
                  step="0.01"
                  {...register('annualRate', { 
                    required: 'Interest rate is required',
                    min: { value: 0.01, message: 'Rate must be greater than 0' },
                    max: { value: 50, message: 'Rate must be less than 50%' }
                  })}
                  className="input-field"
                  placeholder="8.5"
                />
                {errors.annualRate && <p className="text-red-500 text-sm mt-1">{errors.annualRate.message}</p>}
              </div>

              <div>
                <label className="block text-sm font-medium text-secondary-700 mb-2">
                  Loan Tenure (Months)
                </label>
                <input
                  type="number"
                  {...register('months', { 
                    required: 'Loan tenure is required',
                    min: { value: 1, message: 'Tenure must be at least 1 month' },
                    max: { value: 600, message: 'Tenure cannot exceed 600 months' }
                  })}
                  className="input-field"
                  placeholder="240"
                />
                {errors.months && <p className="text-red-500 text-sm mt-1">{errors.months.message}</p>}
              </div>

              <div>
                <label className="block text-sm font-medium text-secondary-700 mb-2">
                  Loan Issue Date
                </label>
                <input
                  type="date"
                  {...register('loanIssueDate')}
                  className="input-field"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-secondary-700 mb-2">
                  EMI Start Date
                </label>
                <input
                  type="date"
                  {...register('startDate')}
                  className="input-field"
                />
              </div>
            </div>
          </div>
        </div>

        {/* Rate Configuration */}
        <div className="card">
          <div className="card-header">
            <h2 className="text-xl font-semibold text-secondary-900">Rate Configuration</h2>
          </div>
          <div className="card-body">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <label className="block text-sm font-medium text-secondary-700 mb-2">
                  Rate Type
                </label>
                <select {...register('rateType')} className="input-field">
                  <option value="FIXED">Fixed Rate</option>
                  <option value="FLOATING">Floating Rate</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-secondary-700 mb-2">
                  Compounding Frequency
                </label>
                <select {...register('compoundingFrequency')} className="input-field">
                  <option value="DAILY">Daily</option>
                  <option value="MONTHLY">Monthly</option>
                </select>
              </div>

              {watchRateType === 'FLOATING' && (
                <>
                  <div>
                    <label className="block text-sm font-medium text-secondary-700 mb-2">
                      Floating Strategy
                    </label>
                    <select {...register('floatingStrategy')} className="input-field">
                      <option value="EMI_CONSTANT">EMI Constant</option>
                      <option value="TENURE_CONSTANT">Tenure Constant</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-secondary-700 mb-2">
                      Benchmark Name
                    </label>
                    <input
                      type="text"
                      {...register('benchmarkName')}
                      className="input-field"
                      placeholder="MCLR"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-secondary-700 mb-2">
                      Spread (%)
                    </label>
                    <input
                      type="number"
                      step="0.01"
                      {...register('spread')}
                      className="input-field"
                      placeholder="0.25"
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-secondary-700 mb-2">
                      Reset Periodicity (Months)
                    </label>
                    <input
                      type="number"
                      {...register('resetPeriodicityMonths')}
                      className="input-field"
                      placeholder="12"
                    />
                  </div>
                </>
              )}
            </div>
          </div>
        </div>

        {/* Moratorium Configuration */}
        <div className="card">
          <div className="card-header">
            <h2 className="text-xl font-semibold text-secondary-900">Moratorium Configuration</h2>
          </div>
          <div className="card-body">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              <div>
                <label className="block text-sm font-medium text-secondary-700 mb-2">
                  Moratorium Months
                </label>
                <input
                  type="number"
                  {...register('moratoriumMonths')}
                  className="input-field"
                  placeholder="0"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-secondary-700 mb-2">
                  Moratorium Type
                </label>
                <select {...register('moratoriumType')} className="input-field">
                  <option value="FULL">Full Moratorium</option>
                  <option value="INTEREST_ONLY">Interest Only</option>
                  <option value="PARTIAL">Partial Payment</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-secondary-700 mb-2">
                  Partial Payment EMI (₹)
                </label>
                <input
                  type="number"
                  step="0.01"
                  {...register('partialPaymentEmi')}
                  className="input-field"
                  placeholder="10000"
                />
              </div>
            </div>
          </div>
        </div>

        {/* Disbursement Phases (for Home Loans) */}
        {watchProductType === 'HOME_LOAN' && (
          <div className="card">
            <div className="card-header">
              <div className="flex justify-between items-center">
                <h2 className="text-xl font-semibold text-secondary-900">Disbursement Phases</h2>
                <button
                  type="button"
                  onClick={() => appendDisbursement({ disbursementDate: '', amount: '', description: '', sequence: disbursementFields.length + 1 })}
                  className="btn-secondary flex items-center space-x-2"
                >
                  <Plus size={16} />
                  <span>Add Phase</span>
                </button>
              </div>
            </div>
            <div className="card-body">
              {disbursementFields.map((field, index) => (
                <div key={field.id} className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-4 p-4 border border-secondary-200 rounded-lg">
                  <div>
                    <label className="block text-sm font-medium text-secondary-700 mb-2">
                      Date
                    </label>
                    <input
                      type="date"
                      {...register(`disbursementPhases.${index}.disbursementDate`)}
                      className="input-field"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-secondary-700 mb-2">
                      Amount (₹)
                    </label>
                    <input
                      type="number"
                      step="0.01"
                      {...register(`disbursementPhases.${index}.amount`)}
                      className="input-field"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-secondary-700 mb-2">
                      Description
                    </label>
                    <input
                      type="text"
                      {...register(`disbursementPhases.${index}.description`)}
                      className="input-field"
                      placeholder="Land Purchase"
                    />
                  </div>
                  <div className="flex items-end">
                    <button
                      type="button"
                      onClick={() => removeDisbursement(index)}
                      className="btn-secondary text-red-600 hover:bg-red-50"
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Charges */}
        <div className="card">
          <div className="card-header">
            <div className="flex justify-between items-center">
              <h2 className="text-xl font-semibold text-secondary-900">Charges & Fees</h2>
              <button
                type="button"
                onClick={() => appendCharge({ chargeType: '', payableTo: '', isRecurring: false, amount: '' })}
                className="btn-secondary flex items-center space-x-2"
              >
                <Plus size={16} />
                <span>Add Charge</span>
              </button>
            </div>
          </div>
          <div className="card-body">
            {chargeFields.map((field, index) => (
              <div key={field.id} className="grid grid-cols-1 md:grid-cols-5 gap-4 mb-4 p-4 border border-secondary-200 rounded-lg">
                <div>
                  <label className="block text-sm font-medium text-secondary-700 mb-2">
                    Charge Type
                  </label>
                  <input
                    type="text"
                    {...register(`charges.${index}.chargeType`)}
                    className="input-field"
                    placeholder="Processing Fee"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-secondary-700 mb-2">
                    Payable To
                  </label>
                  <input
                    type="text"
                    {...register(`charges.${index}.payableTo`)}
                    className="input-field"
                    placeholder="Bank"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-secondary-700 mb-2">
                    Amount (₹)
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    {...register(`charges.${index}.amount`)}
                    className="input-field"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-secondary-700 mb-2">
                    Recurring
                  </label>
                  <select {...register(`charges.${index}.isRecurring`)} className="input-field">
                    <option value="false">No</option>
                    <option value="true">Yes</option>
                  </select>
                </div>
                <div className="flex items-end">
                  <button
                    type="button"
                    onClick={() => removeCharge(index)}
                    className="btn-secondary text-red-600 hover:bg-red-50"
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Submit Button */}
        <div className="flex justify-end space-x-4">
          <button
            type="button"
            onClick={() => navigate('/dashboard')}
            className="btn-secondary"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={isSubmitting}
            className="btn-primary disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isSubmitting ? 'Creating...' : 'Create Loan'}
          </button>
        </div>
      </form>
    </div>
  );
};

export default LoanCreateForm;
