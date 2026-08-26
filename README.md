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

## Prerequisites

- Java 17 or higher
- Apache Maven 3.8+

## Build Instructions

```bash
mvn clean package -DskipTests
```

This generates the executable JAR at `target/db-backup-cli-1.0.0.jar`.

## Run Unit & Integration Tests

```bash
mvn clean test
```

## CLI Command Usage

### 1. View Help Manual
```bash
java -jar target/db-backup-cli-1.0.0.jar --help
```

### 2. Test Connection
```bash
java -jar target/db-backup-cli-1.0.0.jar test-connection --dbms=mysql -h localhost -P 3306 -d my_database -u root -p
```

### 3. Database Security Suite (`security`)

#### A. Run Security Compliance Audit
```bash
java -jar target/db-backup-cli-1.0.0.jar security audit --dbms=mysql -h localhost -P 3306 -d prod_db -u root -p root
```

#### B. Encrypt & Decrypt Backup Files (AES-256-GCM)
```bash
# Encrypt file
java -jar target/db-backup-cli-1.0.0.jar security encrypt -f backup.sql.gz -p "SecretKey123!" -o backup.sql.gz.enc

# Decrypt file
java -jar target/db-backup-cli-1.0.0.jar security decrypt -f backup.sql.gz.enc -p "SecretKey123!" -o backup.sql.gz
```

#### C. Mask PII Sensitive Data in Dumps
```bash
java -jar target/db-backup-cli-1.0.0.jar security mask -f raw_dump.sql -o sanitized_dump.sql
```

#### D. Encrypted Credential Vault Management
```bash
# Save profile to vault
java -jar target/db-backup-cli-1.0.0.jar security vault save --master-password="VaultMasterSecret!" --name="prod-db" --dbms=mysql -h localhost -P 3306 -d prod_db -u db_user -p db_pass

# List profiles
java -jar target/db-backup-cli-1.0.0.jar security vault list --master-password="VaultMasterSecret!"

# Backup using vault profile
java -jar target/db-backup-cli-1.0.0.jar backup --profile="prod-db" --vault-password="VaultMasterSecret!" --encrypt --passphrase="BackupSecretKey!"
```

### 4. Create Encrypted & Sanitized Database Backup
```bash
# SQLite Local Backup with AES-256 Encryption & PII Data Masking
java -jar target/db-backup-cli-1.0.0.jar backup --dbms=sqlite --database=my_app.db --encrypt --passphrase="SecretPassphrase!" --mask-pii --storage=LOCAL

# MySQL Backup to AWS S3 with ZIP compression & AES-256 Encryption
java -jar target/db-backup-cli-1.0.0.jar backup --dbms=mysql -h localhost -d my_db -u root -p secret --encrypt --passphrase="SecretPassphrase!" --storage=S3 --compression=ZIP
```

### 5. Restore Encrypted Database Backup
```bash
java -jar target/db-backup-cli-1.0.0.jar restore --dbms=sqlite --database=restored_app.db -f ./backups/backup_sqlite_my_app.db.gz.enc --passphrase="SecretPassphrase!"
```

### 6. View Audit History
```bash
java -jar target/db-backup-cli-1.0.0.jar history
```

### 7. Schedule Automated Backups
```bash
java -jar target/db-backup-cli-1.0.0.jar schedule --cron="0 0 2 * * ?" --dbms=mysql -d prod_db --storage=S3
```
