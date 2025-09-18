-- Loan Master
CREATE TABLE loans (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  customer_id VARCHAR(255) NULL,
  product_type VARCHAR(50) NOT NULL, -- e.g., HOME_LOAN, PERSONAL
  loan_issue_date DATE,
  start_date DATE,
  principal NUMERIC(18,2) NOT NULL,
  annual_rate NUMERIC(6,4) NOT NULL,
  rate_type VARCHAR(20) NOT NULL, -- FIXED / FLOATING
  floating_strategy VARCHAR(20), -- EMI_CONSTANT / TENURE_CONSTANT
  months INTEGER,
  moratorium_months INTEGER DEFAULT 0,
  moratorium_type VARCHAR(20),
  partial_payment_emi NUMERIC(18,2),
  compounding_frequency VARCHAR(10), -- DAILY / MONTHLY
  reset_periodicity_months INTEGER,
  benchmark_name VARCHAR(50),
  spread NUMERIC(6,4),
  status VARCHAR(20) DEFAULT 'ACTIVE',
  created_at TIMESTAMP DEFAULT now(),
  updated_at TIMESTAMP DEFAULT now()
);

-- Disbursement Phases
CREATE TABLE disbursement_phases (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  loan_id UUID REFERENCES loans(id) ON DELETE CASCADE,
  disbursement_date DATE NOT NULL,
  amount NUMERIC(18,2) NOT NULL,
  description TEXT,
  sequence INTEGER
);

-- Fees / Charges
CREATE TABLE loan_charges (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  loan_id UUID REFERENCES loans(id) ON DELETE CASCADE,
  charge_type VARCHAR(100),
  payable_to VARCHAR(100),
  is_recurring BOOLEAN,
  amount NUMERIC(18,2),
  created_at TIMESTAMP DEFAULT now()
);

-- Repayment Snapshots
CREATE TABLE repayment_snapshot (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  loan_id UUID REFERENCES loans(id) ON DELETE CASCADE,
  snapshot_date DATE NOT NULL,
  principal_balance NUMERIC(18,2) NOT NULL,
  months_remaining INTEGER NOT NULL,
  annual_rate NUMERIC(6,4) NOT NULL,
  rate_type VARCHAR(20),
  apr NUMERIC(6,4),
  memo TEXT,
  created_at TIMESTAMP DEFAULT now()
);

-- Repayment Rows
CREATE TABLE repayment_row (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  snapshot_id UUID REFERENCES repayment_snapshot(id) ON DELETE CASCADE,
  month_number INTEGER NOT NULL,
  payment_date DATE,
  emi NUMERIC(18,2),
  principal_paid NUMERIC(18,2),
  interest_paid NUMERIC(18,2),
  remaining_balance NUMERIC(18,2),
  payment_type VARCHAR(50),
  created_at TIMESTAMP DEFAULT now()
);

-- Benchmark History
CREATE TABLE benchmark_history (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  benchmark_name VARCHAR(100),
  benchmark_date DATE,
  benchmark_rate NUMERIC(6,4),
  created_at TIMESTAMP DEFAULT now()
);

-- Rate Reset Audit
CREATE TABLE rate_reset_audit (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  loan_id UUID REFERENCES loans(id),
  previous_rate NUMERIC(6,4),
  new_rate NUMERIC(6,4),
  reset_date DATE,
  snapshot_id UUID REFERENCES repayment_snapshot(id),
  created_at TIMESTAMP DEFAULT now()
);

-- Create indexes for better performance
CREATE INDEX idx_loans_status ON loans(status);
CREATE INDEX idx_loans_rate_type ON loans(rate_type);
CREATE INDEX idx_loans_benchmark_name ON loans(benchmark_name);
CREATE INDEX idx_disbursement_phases_loan_id ON disbursement_phases(loan_id);
CREATE INDEX idx_loan_charges_loan_id ON loan_charges(loan_id);
CREATE INDEX idx_repayment_snapshot_loan_id ON repayment_snapshot(loan_id);
CREATE INDEX idx_repayment_row_snapshot_id ON repayment_row(snapshot_id);
CREATE INDEX idx_benchmark_history_name_date ON benchmark_history(benchmark_name, benchmark_date);
CREATE INDEX idx_rate_reset_audit_loan_id ON rate_reset_audit(loan_id);
