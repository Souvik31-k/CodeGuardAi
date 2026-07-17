CREATE TABLE code_repository (
    repo_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    github_repo_id BIGINT NOT NULL UNIQUE,
    repo_name VARCHAR(255) NOT NULL,
    user_login VARCHAR(255) NOT NULL ,
    is_active BOOLEAN NOT NULL DEFAULT false,
    webhook_secret_encrypted VARCHAR(255) NOT NULL,
    minimum_severity VARCHAR(20) NOT NULL
        CHECK (minimum_severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE coding_standard_docs (
    docs_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    file_name TEXT NOT NULL,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT false,
    repo_id BIGINT NOT NULL,
    
    CONSTRAINT fk_coding_standard_docs_repository
        FOREIGN KEY (repo_id)
        REFERENCES code_repository(repo_id)
);

CREATE INDEX idx_coding_standard_docs_repo
ON coding_standard_docs(repo_id);

CREATE TABLE review_run (
    review_run_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pull_request_number BIGINT NOT NULL,
    repo_id BIGINT NOT NULL,
    docs_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMPTZ,
    status VARCHAR(30) NOT NULL
        CHECK (status IN ('PENDING','IN_PROGRESS', 'COMPLETED', 'PARTIALLY_COMPLETED', 'FAILED')),
    commit_sha TEXT NOT NULL,

    CONSTRAINT fk_review_repository
        FOREIGN KEY (repo_id)
        REFERENCES code_repository(repo_id),

    CONSTRAINT fk_review_docs
        FOREIGN KEY (docs_id)
        REFERENCES coding_standard_docs(docs_id),

    CONSTRAINT uq_review_run_repo_commit
    UNIQUE (repo_id, commit_sha)
);

CREATE INDEX idx_review_run_repo
ON review_run(repo_id);

CREATE INDEX idx_review_run_pr
ON review_run(pull_request_number);

CREATE INDEX idx_review_run_commit
ON review_run(commit_sha);

CREATE TABLE findings (
    finding_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    agent_type VARCHAR(20) NOT NULL,
        CHECK (agent_type IN ('SECURITY', 'QUALITY', 'TESTING', 'DOCUMENTATION')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    severity VARCHAR(20) NOT NULL,
        CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    title TEXT NOT NULL,
    file_path TEXT NOT NULL,
    line_number INT,
    details JSONB NOT NULL,
    review_run_id BIGINT NOT NULL,

    CONSTRAINT fk_findings_review
        FOREIGN KEY (review_run_id)
        REFERENCES review_run(review_run_id)
    
);

CREATE INDEX idx_findings_severity
ON findings(severity);

CREATE INDEX idx_findings_review_run
ON findings(review_run_id);

CREATE INDEX idx_findings_agent_type
ON findings(agent_type);


CREATE TABLE agent_execution_log (
    execution_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    agent_type VARCHAR(20) NOT NULL,
        CHECK (agent_type IN ('SECURITY', 'QUALITY', 'TESTING', 'DOCUMENTATION')),
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMPTZ,
    err_message TEXT,
    status VARCHAR(20) NOT NULL,
        CHECK (status IN ('RUNNING', 'SUCCESS', 'FAILED', 'TIMEOUT')),
    review_run_id BIGINT NOT NULL,
   
    CONSTRAINT fk_agent_log_review_run
        FOREIGN KEY (review_run_id)
        REFERENCES review_run (review_run_id)
);

CREATE INDEX idx_agent_log_agent_type
ON agent_execution_log(agent_type);

CREATE INDEX idx_agent_log_review_run
ON agent_execution_log(review_run_id);