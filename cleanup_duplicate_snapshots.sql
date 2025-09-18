-- Clean up duplicate snapshots created on view
-- Keep only the earliest snapshot per loan (original creation)
-- Delete all later snapshots that were created during view operations

-- First, let's see what we're dealing with
SELECT
    loan_id,
    COUNT(*) as snapshot_count,
    STRING_AGG(version::text, ', ' ORDER BY created_at) as versions,
    STRING_AGG(memo, '; ' ORDER BY created_at) as memos
FROM repayment_snapshot
GROUP BY loan_id
HAVING COUNT(*) > 1
ORDER BY loan_id;

-- Clean up by deleting duplicate snapshots (keep only version 1 per loan)
DELETE FROM repayment_row
WHERE snapshot_id IN (
    SELECT rs.id
    FROM repayment_snapshot rs
    INNER JOIN (
        SELECT loan_id, MIN(created_at) as keep_time
        FROM repayment_snapshot
        GROUP BY loan_id
    ) keep ON rs.loan_id = keep.loan_id
    WHERE rs.created_at > keep.keep_time
);

DELETE FROM repayment_snapshot rs
USING (
    SELECT loan_id, MIN(created_at) as keep_time
    FROM repayment_snapshot
    GROUP BY loan_id
) keep
WHERE rs.loan_id = keep.loan_id
AND rs.created_at > keep.keep_time;

-- Update the remaining snapshot to have version 1
UPDATE repayment_snapshot SET version = 1 WHERE version IS NULL OR version = 0;

-- Verify the cleanup
SELECT
    loan_id,
    COUNT(*) as remaining_snapshots,
    STRING_AGG(version::text, ', ' ORDER BY created_at) as versions,
    STRING_AGG(memo, '; ' ORDER BY created_at) as memos
FROM repayment_snapshot
GROUP BY loan_id
ORDER BY loan_id;
