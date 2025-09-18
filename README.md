# Loan Management System (LMS) - Technical Overview

## What This System Does

The Loan Management System (LMS) is a comprehensive financial application that manages the complete lifecycle of loans from creation to repayment tracking. It's designed for banks and financial institutions to handle various loan products with sophisticated calculation engines and regulatory compliance features.

## Core Functionality

### 1. Loan Creation & Management
- **Multi-Product Support**: Handles different loan types (Home Loans, Personal Loans, etc.)
- **Flexible Loan Parameters**: 
  - Principal amount, interest rates, tenure
  - Fixed vs Floating rate loans
  - Moratorium periods with different types (Full, Interest-only, Partial)
  - Benchmark-based floating rates (MCLR, etc.)

### 2. Advanced Loan Features
- **Phased Disbursements**: Particularly for Home Loans where funds are released in phases
- **Rate Reset Mechanism**: Automatic and manual rate adjustments for floating rate loans
- **Loan Charges Management**: Processing fees, insurance, and other charges
- **Moratorium Handling**: Grace periods with various payment options

### 3. Key Facts Statement (KFS) Generation
- **Regulatory Compliance**: Generates standardized loan disclosure documents
- **Version Control**: Maintains historical versions of KFS when loan terms change
- **Audit Trail**: Tracks all modifications with timestamps and user information

### 4. Repayment Schedule Management
- **EMI Calculations**: Sophisticated algorithms for Equal Monthly Installments
- **Schedule Versioning**: Maintains historical repayment schedules
- **Hybrid Schedules**: Handles schedule changes mid-loan lifecycle
- **APR Calculations**: Annual Percentage Rate including all charges

## Technical Architecture

### Backend (Spring Boot)
```
lms-backend/
├── calculator/          # Loan calculation engines
├── controller/          # REST API endpoints
├── model/
│   ├── entity/         # JPA entities for database
│   └── dto/            # Data Transfer Objects
├── repository/         # Data access layer
├── service/            # Business logic
└── scheduler/          # Automated rate reset jobs
```

**Key Components:**
- **LoanService**: Core business logic for loan operations
- **HomeLoan Calculator**: Advanced calculation engine for complex loan products
- **RepaymentSnapshotService**: Manages versioned repayment schedules
- **BenchmarkService**: Handles interest rate benchmark management
- **KfsVersionService**: Manages Key Facts Statement versioning

### Frontend (React)
```
lms-frontend/src/
├── components/
│   ├── Dashboard.js           # Main dashboard with loan overview
│   ├── LoanCreateForm.js      # Loan creation interface
│   ├── LoanEdit.js           # Loan modification interface
│   ├── LoanKFSView.js        # KFS document viewer
│   ├── RepaymentSchedule.js   # Schedule display component
│   └── BenchmarkManagement.js # Rate benchmark admin
└── services/
    └── api.js                # API integration layer
```

**Key Features:**
- **Responsive Design**: Built with Tailwind CSS for modern UI
- **Real-time Updates**: Live loan calculations and schedule updates
- **Document Viewer**: KFS and schedule visualization
- **Admin Interface**: Benchmark and rate management

### Database (PostgreSQL)
**Core Tables:**
- `loans`: Main loan entity with all loan parameters
- `disbursement_phases`: Phased disbursement tracking
- `repayment_snapshots`: Versioned repayment schedules
- `loan_versions`: Audit trail of loan modifications
- `kfs_versions`: Historical KFS documents
- `benchmark_history`: Interest rate benchmark tracking

## Business Workflows

### 1. Loan Creation Process
1. **Input Validation**: Validate loan parameters and business rules
2. **Schedule Calculation**: Generate initial repayment schedule using loan calculator
3. **APR Calculation**: Calculate Annual Percentage Rate including all charges
4. **KFS Generation**: Create initial Key Facts Statement
5. **Database Persistence**: Save loan entity and related data
6. **Snapshot Creation**: Create initial repayment schedule snapshot

### 2. Loan Modification Process
1. **Change Detection**: Compare new parameters with existing loan
2. **Audit Logging**: Record all changes with user and timestamp
3. **Schedule Recalculation**: Generate new repayment schedule
4. **Version Management**: Create new versions of schedules and KFS
5. **Hybrid Schedule**: Merge paid installments with new calculations

### 3. Rate Reset Process (Floating Loans)
1. **Benchmark Update**: New benchmark rates are added to system
2. **Loan Identification**: Find all loans linked to updated benchmark
3. **Rate Calculation**: Apply spread to new benchmark rate
4. **Schedule Regeneration**: Recalculate remaining installments
5. **Notification**: Update loan status and create audit entries

## Key Business Rules

### Loan Calculations
- **EMI Formula**: Uses compound interest calculations with monthly compounding
- **Moratorium Handling**: Adjusts schedules based on moratorium type
- **Floating Rate Logic**: Implements EMI-constant or tenure-constant strategies
- **APR Compliance**: Includes all charges in APR calculation per regulatory requirements

### Data Integrity
- **Version Control**: All schedule and KFS changes are versioned
- **Audit Trail**: Complete history of loan modifications
- **Referential Integrity**: Maintains relationships between loans, schedules, and versions
- **Concurrent Access**: Handles multiple users modifying loans safely

## Integration Points

### External Systems
- **Benchmark Feeds**: Integration points for external rate feeds (MCLR, etc.)
- **Document Generation**: KFS and schedule export capabilities
- **Audit Systems**: Comprehensive logging for regulatory compliance
- **Notification Services**: Rate reset and schedule change notifications

### API Endpoints
- **Loan Management**: CRUD operations for loans
- **Schedule Access**: Retrieve current and historical schedules
- **KFS Generation**: Generate and retrieve KFS documents
- **Benchmark Management**: Admin functions for rate management
- **Reporting**: Various loan portfolio reports

## Deployment & Operations

### Containerized Architecture
- **Docker Compose**: Orchestrates all services
- **Health Checks**: Monitors service availability
- **Volume Management**: Persistent data storage
- **Network Isolation**: Secure inter-service communication

### Database Management
- **Flyway Migrations**: Version-controlled schema changes
- **Connection Pooling**: Efficient database connections
- **Backup Strategy**: Automated data backup capabilities
- **Performance Monitoring**: Query optimization and indexing

This LMS system provides a complete solution for financial institutions to manage their loan portfolio with regulatory compliance, advanced calculations, and comprehensive audit trails.
