ALTER TABLE department_managers
    DROP CONSTRAINT IF EXISTS fk_department_managers_department,
    DROP CONSTRAINT IF EXISTS fk_department_managers_user;

ALTER TABLE department_members
    DROP CONSTRAINT IF EXISTS fk_department_members_user,
    DROP CONSTRAINT IF EXISTS fk_department_members_department;

ALTER TABLE department_managers
    ADD CONSTRAINT fk_department_managers_department FOREIGN KEY (department_id)
        REFERENCES departments(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_department_managers_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE;

ALTER TABLE department_members
    ADD CONSTRAINT fk_department_members_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_department_members_department FOREIGN KEY (department_id)
        REFERENCES departments(id) ON DELETE CASCADE;
