-- SureHealth baseline schema (PostgreSQL)

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    role VARCHAR(50),
    status VARCHAR(50),
    created_at TIMESTAMP,
    failed_login_attempts INTEGER DEFAULT 0,
    lock_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS doctors (
    user_id BIGINT PRIMARY KEY,
    license_number VARCHAR(255) UNIQUE,
    speciality VARCHAR(50),
    experience_years INTEGER,
    verified BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_doctors_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS cases (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT,
    doctor_id BIGINT,
    title VARCHAR(150),
    description VARCHAR(2000),
    speciality VARCHAR(50),
    urgency VARCHAR(50),
    status VARCHAR(50),
    created_at TIMESTAMP,
    CONSTRAINT fk_cases_patient
        FOREIGN KEY (patient_id) REFERENCES users(id),
    CONSTRAINT fk_cases_doctor
        FOREIGN KEY (doctor_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS medical_documents (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255),
    file_type VARCHAR(255),
    file_path VARCHAR(255),
    case_id BIGINT,
    uploaded_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_documents_case
        FOREIGN KEY (case_id) REFERENCES cases(id)
);

CREATE TABLE IF NOT EXISTS opinions (
    id BIGSERIAL PRIMARY KEY,
    case_id BIGINT,
    doctor_id BIGINT,
    comment VARCHAR(5000),
    created_at TIMESTAMP,
    CONSTRAINT fk_opinions_case
        FOREIGN KEY (case_id) REFERENCES cases(id),
    CONSTRAINT fk_opinions_doctor
        FOREIGN KEY (doctor_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    message VARCHAR(255),
    event_type VARCHAR(50),
    read_status BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP,
    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_cases_patient_id ON cases(patient_id);
CREATE INDEX IF NOT EXISTS idx_cases_doctor_id ON cases(doctor_id);
CREATE INDEX IF NOT EXISTS idx_cases_created_at ON cases(created_at);

CREATE INDEX IF NOT EXISTS idx_documents_case_id ON medical_documents(case_id);
CREATE INDEX IF NOT EXISTS idx_opinions_case_id ON opinions(case_id);
CREATE INDEX IF NOT EXISTS idx_opinions_doctor_id ON opinions(doctor_id);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_read_status ON notifications(read_status);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at);
