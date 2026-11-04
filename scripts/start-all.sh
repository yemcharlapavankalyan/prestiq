#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$DIR"

mkdir -p logs .pids

echo "=========================================="
echo " Starting PrestIQ Local Demonstration Stack"
echo "=========================================="

# Export common variables
export DB_HOST=localhost
export DB_PORT=5432
export DB_USER=postgres
export DB_PASSWORD=postgres
export POSTGRES_USER=postgres
export POSTGRES_PASSWORD=postgres
export STUDENT_DB_USERNAME=postgres
export STUDENT_DB_PASSWORD=postgres
export SCHOLARSHIP_DB_USERNAME=postgres
export SCHOLARSHIP_DB_PASSWORD=postgres
export APPLICATION_DB_USERNAME=postgres
export APPLICATION_DB_PASSWORD=postgres
export EUREKA_URI=http://localhost:8761/eureka/
export EUREKA_SERVER_URL=http://localhost:8761/eureka/
export JWT_SECRET=d8f99e8cfb24d648849df63ba900221a8b3c9d0e1f2a3b4c5d6e7f8a9b0c1d2
export INTERNAL_SERVICE_KEY=prestiq-internal-dev-secret-key-2026

# Direct service routing fallbacks for Gateway
export AUTH_SERVICE_URL=http://localhost:8081
export STUDENT_SERVICE_URL=http://localhost:8082
export SCHOLARSHIP_SERVICE_URL=http://localhost:8083
export APPLICATION_SERVICE_URL=http://localhost:8086

start_service() {
    local name="$1"
    local jar="$2"
    local port="$3"

    echo "-> Starting $name (port $port)..."
    nohup java -Xms128m -Xmx256m -jar "$jar" > "logs/${name}.log" 2>&1 &
    local pid=$!
    echo $pid > ".pids/${name}.pid"
    echo "   $name started with PID $pid"
}

# 1. Discovery Server
start_service "discovery-server" "services/discovery-server/target/discovery-server-0.0.1-SNAPSHOT.jar" 8761
sleep 4

# 2. Auth Service
start_service "auth-service" "services/auth-service/target/auth-service-0.0.1-SNAPSHOT.jar" 8081

# 3. Student Service
start_service "student-service" "services/student-service/target/student-service-0.0.1-SNAPSHOT.jar" 8082

# 4. Scholarship Service
start_service "scholarship-service" "services/scholarship-service/target/scholarship-service-0.0.1-SNAPSHOT.jar" 8083

# 5. Application Service
start_service "application-service" "services/application-service/target/application-service-0.0.1-SNAPSHOT.jar" 8086

# 6. API Gateway
start_service "api-gateway" "services/api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar" 8080

# 7. Frontend Next.js
echo "-> Starting Next.js frontend (port 3000)..."
(cd frontend/prestiq-landing-page && nohup pnpm dev -p 3000 > "$DIR/logs/frontend.log" 2>&1 & echo $! > "$DIR/.pids/frontend.pid")
echo "   frontend started with PID $(cat .pids/frontend.pid)"

echo "=========================================="
echo " All services launched."
echo " Checking gateway and services health..."
echo "=========================================="

