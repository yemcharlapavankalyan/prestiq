# Prestiq Discovery Server (`discovery-server`)

The **Discovery Server** provides centralized service registration and discovery for the Prestiq microservice ecosystem using **Spring Cloud Netflix Eureka Server**.

---

## 1. Specifications & Technology Stack

* **Port:** `8761` (default, configurable via `$PORT`)
* **Java Version:** Java 21 LTS
* **Spring Boot Version:** `3.3.5`
* **Spring Cloud Version:** `2023.0.4` (Release Train: *Leyton*)
* **Dependency:** `spring-cloud-starter-netflix-eureka-server`, `spring-boot-starter-actuator`
* **Security & Database:** Unnecessary security and database dependencies are disabled for this standalone discovery component.

---

## 2. Local Development & JVM Settings

This service is optimized for development on an 8 GB MacBook Air M2:
* Configured in `.mvn/jvm.config` with `-Xms64m -Xmx256m -XX:+TieredCompilation -XX:TieredStopAtLevel=1`.
* Standalone mode: Does not register itself as a client or attempt to sync with external peers when running locally.

---

## 3. Startup, Verification, and Shutdown

### Startup Command
From the service directory:
```bash
# Ensure Java 21 is selected
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# Run with Maven Wrapper
./mvnw spring-boot:run
```

### Verification Endpoints
Once started, verify the service in a separate terminal:

1. **Spring Boot Actuator Health Check:**
   ```bash
   curl -s http://localhost:8761/actuator/health
   # Expected response: {"status":"UP","components":{...}}
   ```

2. **Eureka Web Dashboard:**
   Open in your browser:
   `http://localhost:8761`

3. **Eureka Registered Applications (JSON):**
   ```bash
   curl -s -H "Accept: application/json" http://localhost:8761/eureka/apps
   ```

### Shutdown Command
* Press `Ctrl+C` in the terminal where the service is running.
* Alternatively, to stop a background process listening on port 8761:
  ```bash
  kill $(lsof -t -i:8761)
  ```

---

## 4. Cloud Deployment (Railway / Render) Compatibility

* **Port Binding:** `server.port: ${PORT:8761}` binds dynamically to the cloud platform's `$PORT`.
* **Hostname:** `eureka.instance.hostname: ${EUREKA_HOSTNAME:localhost}` can be overridden with the service's private mesh domain (e.g. `discovery-server.railway.internal`).
