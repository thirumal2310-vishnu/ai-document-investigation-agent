# AI Document Investigation Agent

## Overview

AI Document Investigation Agent is a Java-based Spring Boot REST application that investigates questions across user-provided text documents.

The application extracts relevant evidence, produces a conclusion based on the available document contents, identifies conflicting or missing information, and recommends a next action. Each investigation is persisted so users can review previous results.

**Current implementation:** The investigation engine uses deterministic, rule-based logic. It does not call an external LLM. This makes the current version reproducible and suitable for demonstrating the investigation workflow without external AI credentials.

## Features

- Upload text documents through a REST API.
- Validate file type, size, content, and user identifier.
- Investigate questions using selected documents.
- Return conclusions with supporting document excerpts.
- Report missing information and potential conflicts.
- Reject credential-seeking investigation questions.
- Enforce document ownership checks.
- Persist investigation results and provide history endpoints.
- Use an H2 database with Spring Data JPA.
- Include automated tests for core investigation behavior.

## Technology Stack

- Java 17
- Spring Boot 4
- Spring MVC REST APIs
- Spring Data JPA and Hibernate
- H2 database
- Maven
- JUnit and Mockito

## Prerequisites

- JDK 17 or compatible version
- Maven 3.9 or compatible version
- PowerShell, terminal, or command prompt

## Run the Application

From the directory containing `pom.xml`, run:

```bash
mvn clean test
mvn spring-boot:run
```

The application starts at:

`http://localhost:8080`

The H2 console is available at:

`http://localhost:8080/h2-console`

The configured JDBC URL is:

`jdbc:h2:file:./data/investigationdb`

The H2 console is intended for local development only.

## API Endpoints

### Upload a document

`POST /api/documents`

Required header:

`X-User-Id: user1`

Example using PowerShell:

```powershell
curl.exe -i -X POST "http://localhost:8080/api/documents" `
  -H "X-User-Id: user1" `
  -F "file=@$env:USERPROFILE\Downloads\invoice.txt"
```

The request returns document metadata, including its generated ID.

### List documents

`GET /api/documents`

Required header: `X-User-Id`

Returns documents associated with the supplied user identifier.

### Retrieve a document

`GET /api/documents/{id}`

Required header: `X-User-Id`

A document that does not exist or is not owned by the supplied user returns HTTP 404.

### Investigate documents

`POST /api/agent/investigate`

Required header: `X-User-Id`

Example request:

```json
{
  "question": "Has invoice INV-1001 been paid?",
  "documentIds": [3]
}
```

The response contains:

- `conclusion`: Findings supported by document contents.
- `evidence`: Relevant excerpts with document references.
- `conflicts`: Detected conflicting information.
- `missingInformation`: Information that cannot be established from the available evidence.
- `nextAction`: Recommended follow-up.

The agent distinguishes what a document reports from independently verified facts.

### List investigation history

`GET /api/agent/history`

Required header: `X-User-Id`

Returns investigations associated with the supplied user identifier, newest first.

### Retrieve an investigation

`GET /api/agent/history/{id}`

Required header: `X-User-Id`

Only investigations associated with the supplied user identifier can be retrieved.

## Design Decisions

- **Layered architecture:** Controllers handle HTTP requests, services implement business logic, repositories access persistence, and entities represent stored data.
- **Evidence-first responses:** Conclusions are accompanied by document excerpts so users can inspect the supporting information.
- **Conservative conclusions:** A document's statement is not treated as independent confirmation of the real-world fact.
- **Persistent history:** Investigation results are stored in H2 using JPA.
- **Ownership checks:** Document and history queries are scoped to a user identifier.
- **Deterministic investigation:** Rule-based logic makes the current implementation predictable and avoids dependence on external AI services.

## Validation and Error Handling

The application validates questions and document identifiers, checks document ownership, and rejects unsupported or invalid uploads.

Expected HTTP responses include:

- `201 Created` — Document uploaded successfully.
- `400 Bad Request` — Invalid input or rejected investigation request.
- `404 Not Found` — Document or investigation is missing or inaccessible.
- `500 Internal Server Error` — Unexpected server-side failure.

## Security Considerations

The `X-User-Id` header is currently supplied by the caller. It is useful for demonstrating ownership isolation but is **not authentication** because a caller can choose another identifier.

A production deployment should integrate authenticated identity, authorize requests using the authenticated principal, configure appropriate database and file-access controls, and avoid exposing the H2 console publicly.

Credential-seeking requests are rejected by basic rule-based checks. These checks are not a complete security filter and should not be treated as a replacement for a dedicated security review.

## Testing

Run automated tests with:

```bash
mvn clean test
```

The current verified test run reported five tests, zero failures, and zero errors.

Manual API checks also verified document upload, evidence-based investigation, history persistence, blank-question rejection, credential-seeking request rejection, and ownership isolation.

## Limitations and Future Improvements

- The current investigation engine is rule-based, not an LLM-powered agent.
- Document support is currently limited to text files.
- Conflict detection and evidence relevance can be improved.
- Authentication and authorization should be integrated with a proper identity provider.
- Investigation processing should include stronger audit logging and more comprehensive error handling.
- A future version could integrate an LLM with structured outputs, evidence-grounding requirements, and safeguards against prompt injection in documents.
- Additional automated controller and integration tests should cover upload validation, ownership isolation, conflicts, and error responses.

## AI Tool Usage

AI assistance was used during development to help with implementation guidance, debugging, test design, and documentation. The current investigation engine itself uses deterministic Java logic and does not make external LLM calls.
