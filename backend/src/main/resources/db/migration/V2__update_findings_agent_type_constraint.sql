ALTER TABLE findings
DROP CONSTRAINT findings_agent_type_check;

ALTER TABLE findings
ADD CONSTRAINT findings_agent_type_check
CHECK (
    agent_type IN (
        'SECURITY',
        'QUALITY',
        'TEST',
        'DOCUMENTATION'
    )
);