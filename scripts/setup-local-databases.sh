#!/usr/bin/env bash
# ==============================================================================
# Prestiq — Local PostgreSQL Database Preparation Script
#
# PURPOSE:
# Idempotently initializes the 9 independent logical databases required by
# Prestiq microservices on macOS (Homebrew PostgreSQL).
#
# SAFETY GUARANTEES:
# - NEVER drops, resets, or overwrites existing databases.
# - Safe for repeated execution (idempotent).
# - Validates prerequisites (Homebrew, psql, server connectivity).
# - Separates database creation from pgvector extension installation.
# - Will NOT fail if pgvector is not yet installed.
# ==============================================================================

set -euo pipefail

# ANSI Color Codes for readable output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

info()    { echo -e "${BLUE}[INFO]${NC} $*"; }
success() { echo -e "${GREEN}[SUCCESS]${NC} $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC} $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*"; }

echo -e "${CYAN}====================================================================${NC}"
echo -e "${CYAN} Prestiq — Microservices Database Setup (9 Logical Databases)${NC}"
echo -e "${CYAN}====================================================================${NC}"
echo ""

# ------------------------------------------------------------------------------
# 1. Connection Parameters & Privileges Notice
# ------------------------------------------------------------------------------
PGHOST="${PGHOST:-localhost}"
PGPORT="${PGPORT:-5432}"
# On macOS Homebrew, the default superuser is usually the logged-in user or 'postgres'
PGUSER="${PGUSER:-$(whoami)}"

cat <<EOF
PRIVILEGE & CONNECTION NOTICE:
- Target Host:     ${PGHOST}:${PGPORT}
- Connecting User: ${PGUSER}
- Database Count:  9 dedicated microservice databases

The connecting PostgreSQL user must have CREATEDB privileges.
If you need to specify a different user or password, set:
  export PGUSER="your_user"
  export PGPASSWORD="your_password"
before running this script.

EOF

# ------------------------------------------------------------------------------
# 2. Prerequisite Checks: Homebrew, psql, and Server Connectivity
# ------------------------------------------------------------------------------
info "Checking environment prerequisites..."

# Check Homebrew availability
if command -v brew >/dev/null 2>&1; then
    BREW_VERSION=$(brew --version | head -n 1)
    success "Homebrew detected: ${BREW_VERSION}"
else
    warn "Homebrew not found in PATH. Continuing with system PostgreSQL tools if present."
fi

# Check psql CLI client
if ! command -v psql >/dev/null 2>&1; then
    error "PostgreSQL client 'psql' is not installed or not in PATH."
    echo ""
    echo "To install PostgreSQL on macOS using Homebrew:"
    echo "  brew install postgresql@16"
    echo "  brew services start postgresql@16"
    exit 1
fi
PSQL_VERSION=$(psql --version)
success "PostgreSQL client detected: ${PSQL_VERSION}"

# Check if PostgreSQL server is accepting connections
info "Testing connectivity to PostgreSQL at ${PGHOST}:${PGPORT}..."
if command -v pg_isready >/dev/null 2>&1; then
    if ! pg_isready -h "${PGHOST}" -p "${PGPORT}" >/dev/null 2>&1; then
        error "PostgreSQL server is NOT accepting connections at ${PGHOST}:${PGPORT}."
        echo ""
        echo "If installed via Homebrew, start the service using:"
        echo "  brew services start postgresql@16"
        echo "  # or for the default package: brew services start postgresql"
        exit 1
    fi
    success "PostgreSQL server is running and accepting connections."
else
    warn "'pg_isready' not found, falling back to direct connection test..."
fi

# Validate user connection to the default maintenance database
CONNECT_TEST_CMD=(psql -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d postgres -c "SELECT 1;" -t -A)
if ! "${CONNECT_TEST_CMD[@]}" >/dev/null 2>&1; then
    # Try fallback to 'postgres' user if current whoami user fails
    if psql -h "${PGHOST}" -p "${PGPORT}" -U postgres -d postgres -c "SELECT 1;" -t -A >/dev/null 2>&1; then
        PGUSER="postgres"
        info "Connected using default 'postgres' user."
    else
        error "Failed to connect to PostgreSQL maintenance database 'postgres'."
        echo "Please verify that your database user has access. You can set:"
        echo "  export PGUSER=postgres"
        echo "  export PGPASSWORD=your_password"
        exit 1
    fi
fi
success "Connected successfully as user '${PGUSER}'."
echo ""

# ------------------------------------------------------------------------------
# 3. The 9 Microservices Databases
# ------------------------------------------------------------------------------
# 1. auth-service                 -> prestiq_auth_db
# 2. student-service              -> prestiq_student_db
# 3. scholarship-service          -> prestiq_scholarship_db
# 4. eligibility-service          -> prestiq_eligibility_db
# 5. recommendation-service       -> prestiq_recommendation_db
# 6. application-service          -> prestiq_application_db
# 7. notification-service         -> prestiq_notification_db
# 8. ai-service                   -> prestiq_ai_db
# 9. scholarship-ingestion-service -> prestiq_ingestion_db

DATABASES=(
    "prestiq_auth_db"
    "prestiq_student_db"
    "prestiq_scholarship_db"
    "prestiq_eligibility_db"
    "prestiq_recommendation_db"
    "prestiq_application_db"
    "prestiq_notification_db"
    "prestiq_ai_db"
    "prestiq_ingestion_db"
)

info "Creating databases (Safe & Idempotent: existing databases will NEVER be modified or dropped)..."
echo ""

CREATED_COUNT=0
EXISTING_COUNT=0
FAILED_COUNT=0

for DB_NAME in "${DATABASES[@]}"; do
    # Check if database already exists
    EXISTS_QUERY="SELECT 1 FROM pg_database WHERE datname = '${DB_NAME}';"
    EXISTS=$(psql -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d postgres -t -A -c "${EXISTS_QUERY}" 2>/dev/null || echo "")

    if [ "${EXISTS}" = "1" ]; then
        echo -e "  ${YELLOW}[EXISTS]${NC}  ${DB_NAME} (preserved unchanged)"
        EXISTING_COUNT=$((EXISTING_COUNT + 1))
    else
        CREATE_QUERY="CREATE DATABASE \"${DB_NAME}\";"
        if psql -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d postgres -c "${CREATE_QUERY}" >/dev/null 2>&1; then
            echo -e "  ${GREEN}[CREATED]${NC} ${DB_NAME}"
            CREATED_COUNT=$((CREATED_COUNT + 1))
        else
            echo -e "  ${RED}[FAILED]${NC}  ${DB_NAME}"
            FAILED_COUNT=$((FAILED_COUNT + 1))
        fi
    fi
done

echo ""
info "Database creation summary: ${CREATED_COUNT} created, ${EXISTING_COUNT} already existed, ${FAILED_COUNT} failed."
echo ""

# ------------------------------------------------------------------------------
# 4. pgvector Extension Check (Specifically for prestiq_ai_db)
# ------------------------------------------------------------------------------
info "Checking pgvector extension for 'prestiq_ai_db'..."

VECTOR_AVAILABLE_QUERY="SELECT 1 FROM pg_available_extensions WHERE name = 'vector';"
VECTOR_AVAILABLE=$(psql -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d prestiq_ai_db -t -A -c "${VECTOR_AVAILABLE_QUERY}" 2>/dev/null || echo "")

if [ "${VECTOR_AVAILABLE}" = "1" ]; then
    info "'vector' extension is available in PostgreSQL. Enabling in 'prestiq_ai_db'..."
    if psql -h "${PGHOST}" -p "${PGPORT}" -U "${PGUSER}" -d prestiq_ai_db -c "CREATE EXTENSION IF NOT EXISTS vector;" >/dev/null 2>&1; then
        success "pgvector extension enabled in 'prestiq_ai_db'."
    else
        warn "Could not create 'vector' extension in 'prestiq_ai_db'. Check user superuser permissions."
    fi
else
    warn "'vector' extension is NOT currently available in your PostgreSQL installation."
    cat <<EOF

NOTE ON PGVECTOR:
- The 9 databases were successfully created and are ready for relational use.
- The 'ai-service' will require pgvector in Phase 10.
- To install pgvector on macOS:
    brew install pgvector
- Once installed, the extension can be enabled at any time by running:
    psql -h ${PGHOST} -p ${PGPORT} -U ${PGUSER} -d prestiq_ai_db -c "CREATE EXTENSION IF NOT EXISTS vector;"

EOF
fi

# ------------------------------------------------------------------------------
# 5. Final Report & Next Steps
# ------------------------------------------------------------------------------
if [ "${FAILED_COUNT}" -eq 0 ]; then
    echo -e "${GREEN}====================================================================${NC}"
    echo -e "${GREEN} All 9 Prestiq microservice databases are ready!${NC}"
    echo -e "${GREEN}====================================================================${NC}"
    echo ""
    echo "Configured databases:"
    for DB_NAME in "${DATABASES[@]}"; do
        echo "  - ${DB_NAME}"
    done
    echo ""
    echo "Next: Configure your local .env file by copying .env.example:"
    echo "  cp .env.example .env"
    exit 0
else
    echo -e "${RED}====================================================================${NC}"
    echo -e "${RED} One or more databases failed to create. Please review errors above.${NC}"
    echo -e "${RED}====================================================================${NC}"
    exit 1
fi
