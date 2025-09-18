-- Add version column to repayment_snapshot
ALTER TABLE repayment_snapshot ADD COLUMN version INTEGER DEFAULT 1;

-- Add change_marker column to repayment_row
ALTER TABLE repayment_row ADD COLUMN change_marker BOOLEAN DEFAULT FALSE;

-- Create loan_audit table
CREATE TABLE loan_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id UUID NOT NULL REFERENCES loans(id),
    field_name VARCHAR(50) NOT NULL,
    old_value VARCHAR(255),
    new_value VARCHAR(255),
    change_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    changed_by VARCHAR(100),
    CONSTRAINT fk_loan_audit_loan FOREIGN KEY (loan_id) REFERENCES loans(id) ON DELETE CASCADE
);

-- Create index for better performance
CREATE INDEX idx_loan_audit_loan_id ON loan_audit(loan_id);
CREATE INDEX idx_loan_audit_change_date ON loan_audit(change_date);

-- Update existing snapshots to have version 1
UPDATE repayment_snapshot SET version = 1 WHERE version IS NULL;

-- Make version column NOT NULL after setting default values
ALTER TABLE repayment_snapshot ALTER COLUMN version SET NOT NULL;
