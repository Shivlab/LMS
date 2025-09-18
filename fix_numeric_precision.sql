-- Fix numeric field overflow by increasing precision for rate fields
-- This addresses the PostgreSQL error: "numeric field overflow - A field with precision 8, scale 4 must round to an absolute value less than 10^4"

-- Update loan entity annual_rate and spread fields
ALTER TABLE loans ALTER COLUMN annual_rate TYPE NUMERIC(8,4);
ALTER TABLE loans ALTER COLUMN spread TYPE NUMERIC(8,4);

-- Update repayment_snapshot apr field  
ALTER TABLE repayment_snapshot ALTER COLUMN apr TYPE NUMERIC(10,4);

-- Update rate_reset_audit fields
ALTER TABLE rate_reset_audit ALTER COLUMN previous_rate TYPE NUMERIC(8,4);
ALTER TABLE rate_reset_audit ALTER COLUMN new_rate TYPE NUMERIC(8,4);

-- Update benchmark_history rate field
ALTER TABLE benchmark_history ALTER COLUMN benchmark_rate TYPE NUMERIC(8,4);

-- Verify the changes
\d loan;
\d repayment_snapshot;
\d rate_reset_audit;
\d benchmark_history;
