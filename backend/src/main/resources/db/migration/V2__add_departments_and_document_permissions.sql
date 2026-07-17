CREATE TABLE IF NOT EXISTS departments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_by UUID NOT NULL,
    updated_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS department_managers (
    department_id UUID NOT NULL,
    user_id UUID NOT NULL,
    PRIMARY KEY (department_id, user_id),
    CONSTRAINT fk_department_managers_department FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE CASCADE,
    CONSTRAINT fk_department_managers_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS department_members (
    user_id UUID NOT NULL,
    department_id UUID NOT NULL,
    PRIMARY KEY (user_id, department_id),
    CONSTRAINT fk_department_members_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_department_members_department FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE CASCADE
);

ALTER TABLE documents
    ADD COLUMN IF NOT EXISTS department_id UUID;

ALTER TABLE documents
    ADD CONSTRAINT fk_documents_department FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_department_members_department ON department_members(department_id);
CREATE INDEX IF NOT EXISTS idx_department_managers_user ON department_managers(user_id);
CREATE INDEX IF NOT EXISTS idx_documents_department ON documents(department_id);
