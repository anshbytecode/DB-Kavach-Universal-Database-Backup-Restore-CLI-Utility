# 🛡️ DB-Kavach Banking Platform
> **Secure Banking. Intelligent Management. Complete Protection.**

[![Java 17](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Picocli](https://img.shields.io/badge/Picocli-4.7.5-blue.svg)](https://picocli.info/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

**DB-Kavach Banking** is a full-fledged, enterprise-grade **Digital Banking Management & Disaster Recovery Platform**. It extends the core **DB-Kavach Database Backup & Restore CLI Utility** into an integrated solution combining digital customer banking, role-based bank administration, real-time AI/rule-based fraud detection, immutable compliance auditing, and multi-cloud database disaster recovery.

---

## 🌟 Key Highlights

- 🏦 **Digital Customer Banking**: Savings & Current Accounts, Internal & External Money Transfers, Beneficiary Management with 30-min Cooling Protection, Utility Bill Payments, Fixed Deposits (FD), Loan Applications with EMI Calculator, Debit & Credit Card Controls, and Simulated UPI Pay.
- 🛡️ **Cybersecurity & Fraud Detection Center**: Real-time 0–100 risk scoring engine analyzing transfer amounts, rapid succession frequency, late-night timing, and suspicious account state with automated alert generation and account freezing.
- 🔐 **Spring Security 6 & RBAC**: Strict Role-Based Access Control enforcing 5 distinct role experiences (`SUPER_ADMIN`, `BANK_ADMIN`, `BANK_EMPLOYEE`, `CUSTOMER`, `AUDITOR`) with BCrypt password hashing and account lockout policies (5 failed attempts = 15 min lock).
- 💰 **Financial Math & Transaction Safety**: Strict `BigDecimal` arithmetic (zero floating-point inaccuracy) coupled with `@Transactional` ACID isolation and idempotency keys to prevent duplicate transfers, negative balances, and double payments.
- 🗄️ **Integrated DB-Kavach Disaster Recovery Engine**: Multi-DBMS support (MySQL, PostgreSQL, MongoDB, SQLite) with GZIP compression, AES-256-GCM encryption, PII masking, SHA-256 checksum validation, and RPO (< 5 mins) / RTO (< 15 mins) tracking.
- 💻 **Dual Web & Picocli CLI Interfaces**: Fully usable via a modern dark-mode Web SPA UI and a functional command-line interface (`db-backup`).

---

## 🏗️ System Architecture

```
+-----------------------------------------------------------------------------------+
|                                DB-Kavach Banking                                  |
|         Tagline: Secure Banking. Intelligent Management. Complete Protection.      |
+-----------------------------------------------------------------------------------+
|                                  LANDING PAGE                                     |
|           Hero | Services | Security | Features | About | Contact | FAQ           |
+-----------------------------------------------------------------------------------+
|                               AUTHENTICATION LAYER                                |
|        Spring Security 6 | BCrypt | Session/JWT | MFA Simulation | RBAC Guard    |
+---------------------+-------------------+--------------------+--------------------+
|  CUSTOMER PORTAL    | ADMIN PORTAL      | STAFF PORTAL       | AUDITOR PORTAL     |
| - Dashboard & Stats | - Core Analytics  | - Assigned Customers| - Compliance Audit |
| - Accounts (Savings,| - Customer Mgmt   | - Pending KYC      | - Security Events  |
|   Current, FD, RD)  | - Account Mgmt    | - Ticket Queue     | - Fraud Logs       |
| - Transfers & Benef | - KYC Approvals   | - Transaction Logs | - Disaster Recovery|
| - Loans & EMI Calc  | - Loan Approval   | - Account Requests |   Metrics (RPO/RTO)|
| - Card Mgmt & Limits| - Branch & Product|                    |                    |
| - Bill Payments     | - Fraud Detection |                    |                    |
| - Support Tickets   | - Security Center |                    |                    |
| - Security Settings | - Backup & DR     |                    |                    |
+---------------------+-------------------+--------------------+--------------------+
|                               Financial Engine                                    |
|   `BigDecimal` Precision | `@Transactional` | Idempotency Key | Double-Entry Logs  |
+-----------------------------------------------------------------------------------+
|                         DB-Kavach Backup & Security Engine                        |
|  AES-256-GCM | PII Masking | Multi-DBMS | Storage Drivers | Disaster Recovery    |
+-----------------------------------------------------------------------------------+
```

---

## 👥 Role-Based Access Control (RBAC) & Demo Accounts

| Role | Username | Password | Access Level & Core Features |
| :--- | :--- | :--- | :--- |
| **Super Admin** | `superadmin` | `Admin123!` | Full system control, admin user creation, branch & product config |
| **Bank Admin** | `admin` | `Admin123!` | Customer mgmt, account freezing, KYC review, loan approvals, fraud center, reports, DR scan |
| **Bank Staff** | `employee1` | `Staff123!` | Assigned customers, pending KYC queue, service tickets, transaction logs |
| **Customer** | `anshul` | `Customer123!` | Digital banking, accounts, transfers, loans, FDs, cards, bill payments, support tickets |
| **Auditor** | `auditor1` | `Auditor123!` | Read-only compliance controls, immutable audit logs, security events, DR metrics |

---

## 💻 CLI Commands (Picocli Engine)

Run CLI commands via terminal:
```bash
java -jar target/db-backup-cli-1.0.0.jar <command> [options]
```

### Available Commands:
- `status`: Check overall system, database, and disaster recovery health status.
- `database`: Manage and inspect connected database inventory.
- `audit`: View administrative security & banking audit trail.
- `verify-backup`: Verify integrity and SHA-256 checksum of backup archives.
- `backup`: Execute database backup with GZIP compression, AES-256 encryption, and PII masking.
- `restore`: Restore database from local or cloud backup archives.
- `security`: Run database security compliance scanner & vault management.
- `test-connection`: Test connection reachability for target database.
- `history`: Display past backup and restore audit history.
- `schedule`: Schedule automated cron backup tasks.

---


## 🧪 Testing

Run automated JUnit tests covering Auth, RBAC, Financial Math, Transfers, Money Safety, Fraud Detection, and Backups:
```bash
mvn test
```

---

## 📜 License & Compliance Notice

This software contains compliance-oriented controls (KYC masking, audit logging, RBAC, AES-256 encryption). It is intended for project demonstration, academic presentation, and technical evaluation.
