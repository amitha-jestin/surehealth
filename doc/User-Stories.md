# SureHealth User Stories

## Overview

SureHealth is a healthcare case management platform that allows users to
submit medical cases, specialists to review and provide opinions, and
administrators to manage platform security and operations.

------------------------------------------------------------------------
## SureHealth User Stories & Technical Stories

# 📌 Section 1: Product User Stories (Business / Functional)

# Epic 1: User Management & Authentication

## US-1: User Registration

**As a user**, I want to register an account so that I can securely
submit my medical cases.

### Acceptance Criteria:

-   User can register using email and password
-   User receives a confirmation message after successful registration

------------------------------------------------------------------------

## US-2: User Login

**As a user**, I want to log in so that I can securely access my
submitted cases.

### Acceptance Criteria:

-   User can log in using email and password
-   JWT token is generated upon successful login
-   Invalid credentials return an error message

------------------------------------------------------------------------

# Epic 2: Medical Case Management

## US-3: Submit Medical Case

**As a user**, I want to submit a medical case so that a
specialist can review it.

### Acceptance Criteria:

-   User can upload case details.
-   User can assign the case to required doctor.
-   Case is stored securely in the database
-   User receives confirmation after submission


------------------------------------------------------------------------

## US-4: View Submitted Cases

**As a user or a specialist **, I want to view my submitted cases/ cases assigned to me so that I can track
their status.

### Acceptance Criteria:

-   User can view a list of submitted cases
-   Each case shows status (Pending, Reviewed, Closed)

------------------------------------------------------------------------

# Epic 3: Specialist Review

## US-5: Specialist Accept or Reject Cases

**As a specialist**, I want to accept or reject assigned cases so that I can manage my workload efficiently.

### Acceptance Criteria:

-   Doctor can accept or reject case

-   Case status updates automatically

## US-6: Submit Specialist Opinion

**As a specialist**, I want to submit a written opinion so that the user
receives professional guidance.

### Acceptance Criteria:

-   Specialist can view assigned cases
-   Specialist can submit comments and recommendations

------------------------------------------------------------------------


# Epic 4: Administration & Security

## US-7: Manage User Roles

**As an admin**, I want to manage user roles so that the platform
remains secure and organized.

### Acceptance Criteria:

-   Admin can assign roles (USER, SPECIALIST, ADMIN)
-   Role-based access control is enforced in APIs

## US-8: Account Lockout

**As an admin**, I want to block users after multiple failed login
attempts so that the system is protected from brute-force attacks.

### Acceptance Criteria:

-   System tracks failed login attempts
-   User account is locked after N failed attempts
-   Admin can manually unblock the account
------------------------------------------------------------------------

# Future Backlog (Planned Features)

## US-9: Medical Document Upload
**As a user**, I want to upload medical documents (PDF, images) so that specialists can review them.

### Acceptance Criteria:

-   User can upload multiple files per case
-   Files are stored securely and linked to the case
-   Specialists can view the uploaded documents
- 
## US-10: Case Notifications

**As a user and doctor**, I want to receive notifications for case assignment, acceptance, rejection, and review so that I stay informed.

### Acceptance Criteria:

-   Notifications triggered for assignment, accept, reject, and review

-   Notifications can be email or in-app

-   Notification history is stored

## US-11: Account logout
**As a user**, I want to log out of my account so that I can ensure my session is secure.
### Acceptance Criteria:
-   User can log out successfully
-   JWT token is invalidated upon logout
- User receives confirmation after logout



------------------------------------------------------------------------
📌 Section 2: Technical / Engineering User Stories (non-functional)
# Epic 5: Observability & Documentation
## EN-1: Logging & Monitoring

**As a developer**, I want to implement centralized logging so that system issues can be traced and debugged easily.

### Acceptance Criteria:

    Log requests, responses, and errors

    Use structured logging (SLF4J / Logback)

    Logs forwarded to Splunk/ELK via agent or HEC

## EN-2: API Documentation (Swagger)

**As a developer**, I want to document APIs using Swagger/OpenAPI so that frontend and QA teams can easily test APIs.

###Acceptance Criteria:

    Swagger UI available at /swagger-ui

    All REST endpoints documented

    JWT security documented in Swagger

# Epic 6: Testing
## EN-3: Unit Testing with JUnit

**As a developer**, I want to write JUnit test cases so that core business logic is validated automatically.

### Acceptance Criteria:

    Service layer unit tests with Mockito

    Minimum 70–80% code coverage

    Tests executed in CI pipeline

#Epic 7: DevOps & Deployment
##EN-4: CI/CD Pipeline using Jenkins

**As a DevOps engineer**, I want to create a Jenkins pipeline so that code is automatically built, tested, and packaged.

### Acceptance Criteria:

    Pipeline stages: Build → Test → Package → Docker Build

    Pipeline triggered on Git push

    Artifacts stored in repository (Nexus/Registry)

## EN-5: Containerization with Docker

**As a developer**, I want to containerize the application so that it can run consistently across environments.

### Acceptance Criteria:

    Dockerfile created for Spring Boot application

    Application runs in Docker container

    Environment variables configured for DB and secrets

## EN-6: Deployment Strategy

***As a DevOps engineer**, I want to deploy the application to a server/cloud so that users can access the system.

### Acceptance Criteria:

    Deployment to AWS EC2 / Kubernetes / VM

    Separate environments: Dev, QA, Prod

    Health check endpoint configured

## Notes

Product User Stories represent business features.

Technical Stories represent engineering and DevOps tasks

