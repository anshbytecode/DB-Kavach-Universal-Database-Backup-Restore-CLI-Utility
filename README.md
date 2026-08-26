# DB Kavach - Universal Database Backup & Restore Utility

A production-grade, extensible Database Security & Backup Platform built with **Java 17**, **Spring Boot 3**, **Picocli**, **Tailwind CSS**, and **Vite**.

## Features

- **Multi-DBMS Support**: Full backup and restore capabilities for MySQL, PostgreSQL, MongoDB, and SQLite. Dual-engine design utilizes native CLI tools (`mysqldump`, `pg_dump`, `mongodump`) when available, and automatically falls back to pure Java JDBC/MongoDB Driver engines when native tools are missing.
- **Database Security Suite**:
  - **AES-256-GCM Backup Encryption**: Authenticated file encryption with PBKDF2 key derivation.
  - **PII Data Sanitization & Masking**: Automatically masks emails, credit card numbers, SSNs, phone numbers, and secret tokens in database dumps.
  - **Security Audit Compliance Scanner**: Scans database instances for default/weak passwords, SSL/TLS transport enforcement, exposed default ports, and excessive admin privileges.
  - **Encrypted Credential Vault**: Stores database connection profiles in an encrypted vault (`vault.enc`) to keep passwords out of plain text scripts and terminal history.
- **Backup Types**: Full, Incremental, and Differential backup strategies with selective table/collection filtering (`--tables`).
- **Compression**: Supports GZIP (`.gz`), ZIP (`.zip`), TAR.GZ (`.tar.gz`), and uncompressed formats.
- **Storage Options**: Store backups locally or upload directly to cloud storage (AWS S3, Google Cloud Storage, Azure Blob Storage).
- **Activity Logging & History**: Audit logs persisted to an embedded H2 database. View past backup/restore runs via the `history` CLI command.
- **Slack Notifications**: Optional Slack Webhook alerts on backup completion or failure.
- **Automated Scheduling**: Schedule background cron jobs using the `schedule` command.
