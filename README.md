# Bitespeed Identity Reconciliation Service

## Overview

This project implements the **Identity Reconciliation API** as specified in the Bitespeed backend assignment.

The service resolves and unifies customer identities based on **email** and **phone number**, ensuring that multiple records belonging to the same real-world user are linked correctly using **primary** and **secondary** contacts.

The API is **idempotent**, **deterministic**, and designed to handle partial overlaps (same email, same phone, or both).

---

## Problem Statement (In Short)

Customers may reach the system via different identifiers over time:

* Same email, different phone numbers
* Same phone number, different emails
* Or both

The system must:

* Maintain **one primary contact** per logical user
* Link all other related records as **secondary contacts**
* Always keep the **oldest contact as primary**

---

## Tech Stack

* Java 17
* Spring Boot 3.x
* Spring Data JPA
* H2 (in-memory DB, configurable)
* Lombok
* Maven
* JUnit 5 + Spring Boot Test

---

## Project Structure

```
.
├── Dockerfile
├── pom.xml
└── src
    ├── main
    │   ├── java/com/bitespeed/identity
    │   │   ├── Contact.java
    │   │   ├── ContactRepository.java
    │   │   ├── IdentifyRequest.java
    │   │   ├── IdentifyResponse.java
    │   │   ├── IdentityApplication.java
    │   │   ├── IdentityController.java
    │   │   └── IdentityService.java
    │   └── resources
    │       └── application.properties
    └── test
        ├── java/com/bitespeed/identity
        │   ├── IdentityApplicationTests.java
        │   ├── IdentityControllerTest.java
        │   └── IdentityServiceTest.java
        └── resources
            └── application-test.properties
```

---

## Data Model

### Contact Entity

Each row represents one contact entry.

Key fields:

* `id`
* `email` (nullable)
* `phoneNumber` (nullable)
* `linkedId` → points to primary contact if secondary
* `linkPrecedence` → `PRIMARY` or `SECONDARY`
* `createdAt`

Rules:

* A **PRIMARY** contact has `linkedId = null`
* A **SECONDARY** contact has `linkedId = primary.id`

---

## API Contract

### Endpoint

```
POST /identify
```

### Request Body

```json
{
  "email": "test@bitespeed.com",
  "phoneNumber": "9999999999"
}
```

Either field may be `null`, but **at least one must be present**.

---

### Response Body

```json
{
  "contact": {
    "primaryContactId": 1,
    "emails": [
      "test@bitespeed.com"
    ],
    "phoneNumbers": [
      "9999999999",
      "8888888888"
    ],
    "secondaryContactIds": [
      2
    ]
  }
}
```

Response guarantees:

* `primaryContactId` is always the **oldest** primary
* Emails and phone numbers are **unique**
* Secondary IDs exclude the primary ID

---

## Identity Resolution Logic

High-level flow:

1. Search for existing contacts by **email OR phone number**
2. If none exist → create a new PRIMARY contact
3. If matches exist:

   * Determine the **root primary** (oldest PRIMARY)
   * Re-link any other primaries as SECONDARY
4. If request contains new info (email/phone not yet present):

   * Create a new SECONDARY contact linked to root primary
5. Aggregate all related contacts to build the response

This ensures:

* No duplicate primaries
* Historical consistency
* Deterministic output

---

## Running the Application

### Prerequisites

* Java 17+
* Maven 3.8+

### Start Locally

```bash
mvn clean spring-boot:run
```

App starts on:

```
http://localhost:8080
```

---

## Running Tests

```bash
mvn clean test
```

Includes:

* Context load test
* Controller integration test
* Service-level logic tests

---

## Docker Support

Build image:

```bash
docker build -t bitespeed-identity .
```

Run container:

```bash
docker run -p 8080:8080 bitespeed-identity
```

---

## Design Considerations

* **Idempotent API**: same input → same logical identity
* **Oldest-wins rule** for primary selection
* **Transactional service layer** to avoid partial updates
* **Clean separation** of controller, service, and persistence
* Easily extensible for PostgreSQL / MySQL

---

## Assumptions

* Email and phone number uniqueness is enforced logically, not via DB constraints
* Database is small enough for simple joins (as per assignment scope)
* Soft deletes not required

---

## Possible Enhancements

* Add DB-level indexes on email & phoneNumber
* Pagination for very large identity graphs
* Audit logging
* Production-grade DB (PostgreSQL)

---

## Author Notes

This solution prioritizes **correctness, clarity, and maintainability** over premature optimization. The logic closely follows real-world identity reconciliation patterns used in CRM systems.

---

✅ **Assignment Complete & Production-Ready**
