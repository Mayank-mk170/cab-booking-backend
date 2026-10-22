RideGo Backend
RideGo is a cab-booking REST API built with Java and Spring Boot. The project uses JWT-based authentication, Spring Data JPA/Hibernate, PostgreSQL hosted on Supabase, and WebSocket ride events.
This README is based on code and configuration details shared during development. Verify routes and features against the current repository before publishing.

Technology stack
- Java (the project has previously run on Java 21)
- Spring Boot
- Spring Security and JWT
- Spring Data JPA / Hibernate
- PostgreSQL on Supabase
- WebSockets
- Maven
  Features
- Role-based authentication for riders, drivers, and admins
- Driver profile and vehicle management
- Driver verification and availability
- Ride requests and fare/distance calculation
- Driver ride acceptance and status transitions
- Rider/driver ride history
- Ride events through WebSockets
- Per-driver rejection of ride requests (requires the app.ride_rejections table and the updated backend files)
  Architecture
  React Frontend
  | HTTP/JSON + JWT; WebSocket ride events
  v
  Spring Boot REST API
  | JPA/Hibernate
  v
  PostgreSQL (Supabase)
  Prerequisites
- JDK version matching pom.xml
- Maven (or Maven Wrapper, if included)
- Supabase PostgreSQL project
  Configuration
  Set database connection, JWT, CORS, and any enabled email/payment settings in src/main/resources/application.yml or application.properties.

Do not commit database passwords, JWT signing secrets, SMTP credentials, or payment API keys. Use environment variables or your hosting provider's secret manager. Rotate any real secrets that were accidentally exposed.
Run locally
From the backend repository root:
mvn clean spring-boot:run
If Maven Wrapper exists, use ./mvnw spring-boot:run or .\mvnw.cmd spring-boot:run on Windows.

Previously used local base URL:
http://localhost:8080/api
Confirm the active port and context path in your configuration.
Driver ride API
Method	Endpoint	Purpose
GET	/api/driver/rides/available	List requests available to the current verified, online driver
GET	/api/driver/rides	List the current driver's rides
PUT	/api/driver/rides/{id}/accept	Accept a request
PUT	/api/driver/rides/{id}/reject	Reject a request for this driver only
PUT	/api/driver/rides/{id}/cancel	Cancel an assigned ride
PUT	/api/driver/rides/{id}/status	Advance a ride through supported status transitions


The reject route is part of the recent update. The frontend request method must be PUT to match the controller.
Ride lifecycle
REQUESTED -> ACCEPTED -> DRIVER_ARRIVING -> IN_PROGRESS -> COMPLETED
Cancellation is separate. Rejecting a request should record that driver's rejection without cancelling the rider's booking, so other eligible drivers can still accept it.
Rejection table migration
If Hibernate does not create the table automatically, run the supplied supabase_ride_rejections.sql in the Supabase SQL Editor. It creates app.ride_rejections, with references to app.rides and app.drivers and a unique constraint for each ride/driver pair.

Apply the migration before testing the reject endpoint.
Authorization
- Send JWTs as Authorization: Bearer <token>.
- Routes are protected by user roles.
- A driver must be verified and online to accept or reject an available request.
  Testing checklist
- Rider can log in and request a ride.
- Verified, online driver can see available requests.
- Accept assigns the ride to one driver.
- Reject hides the request from the rejecting driver but leaves it available to another eligible driver.
- Ride status transitions and cancellation work.
- Ride history and WebSocket updates work.
  Deployment
1. Configure Supabase and apply required migrations.
2. Set secrets in the hosting provider.
3. Configure CORS for the deployed frontend origin.
4. Deploy the Spring Boot app.
5. Set the frontend API base URL.
6. Test authentication and all rider/driver workflows.
   Typical project structure
   src/main/java/com/mayank/cabbooking/
   controller/
   dto/
   entity/
   repository/
   service/
   src/main/resources/
   pom.xml
   License
   Add the license applicable to this repository.