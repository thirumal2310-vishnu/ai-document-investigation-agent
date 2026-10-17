# AI Document Investigation Agent

## Overview

AI Document Investigation Agent is a Java-based Spring Boot REST application that investigates questions across user-provided text documents.

The application extracts relevant evidence, produces conclusions based on available document contents, identifies conflicting or missing information, and recommends next actions. Investigation results are persisted so users can review previous investigations.

**Current implementation:** The investigation engine uses deterministic, rule-based logic. It does not call an external LLM. This makes the current version reproducible and demonstrates the investigation workflow without requiring external AI credentials.

## Features

- Upload text documents through a REST API.
- Validate file type, size, content, and user identifier.
- Investigate questions using selected documents.
- Return conclusions with supporting document excerpts.
- Identify missing information and potential conflicts.
- Reject credential-seeking investigation questions.
- Enforce document ownership checks.
- Persist investigation results and provide history endpoints.
- Use H2 with Spring Data JPA for persistence.
- Include automated tests for core investigation behavior.

## Technology Stack

- Java 17
- Spring Boot 4
- Spring MVC REST APIs
- Spring Data JPA and Hibernate
- H2 Database
- Maven
- JUnit and Mockito

## Prerequisites

- JDK 17 or compatible version
- Maven 3.9 or compatible version, or the included Maven Wrapper
- PowerShell, terminal, or command prompt

## Run the Application

From the directory containing `pom.xml`, run the automated tests:

```bash
mvn clean test
```

Start the application:

```bash
mvn spring-boot:run
```

On Windows, you can use the included Maven Wrapper instead:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

The application starts at:

`http://localhost:8080`

### H2 Console

The H2 console is available at:

`http://localhost:8080/h2-console`

Configured JDBC URL:

`jdbc:h2:file:./data/investigationdb`

Username: `sa`

Password: empty

The H2 console is intended for local development only and should not be exposed publicly.

## Sample Data

A sample invoice is provided at:

`sample-data/invoice.txt`

The file contains fictional test data:

```text
Invoice Number: INV-1001
Vendor: ABC Supplies
Invoice Amount: 5000 INR
Invoice Date: 2026-10-10
Payment Status: Pending
```

### 1. Upload the Sample Invoice

Start the application and open PowerShell in the project directory.

Run:

```powershell
curl.exe -i -X POST "http://localhost:8080/api/documents" `
  -H "X-User-Id: user1" `
  -F "file=@sample-data/invoice.txt"
```

The request should return HTTP `201 Created` with document metadata, including a generated document ID.

Note the returned ID and use it in the investigation request.

### 2. Investigate the Invoice

The following example uses document ID `1`. Replace it with the actual ID returned by your upload request.

```powershell
$body = @{
    question = "Has invoice INV-1001 been paid?"
    documentIds = @(1)
} | ConvertTo-Json -Compress

Invoke-RestMethod `
    -Uri "http://localhost:8080/api/agent/investigate" `
    -Method Post `
    -Headers @{ "X-User-Id" = "user1" } `
    -ContentType "application/json" `
    -Body $body |
    ConvertTo-Json -Depth 10
```

### Expected Result

The investigation should report that the document lists the payment status as `Pending`.

The response should include relevant evidence, missing information, and a recommended next action to verify payment against an authoritative source.

The agent distinguishes between what a document reports and what has been independently verified. The sample result is not independent confirmation of the real-world payment status.

## API Endpoints

All endpoints run locally at `http://localhost:8080`.

### Upload a Document

`POST /api/documents`

Required header:

`X-User-Id: user1`

Content type: `multipart/form-data`

Form field: `file`

Example:

```powershell
curl.exe -i -X POST "http://localhost:8080/api/documents" `
  -H "X-User-Id: user1" `
  -F "file=@sample-data/invoice.txt"
```

Returns document metadata and the generated document ID.

### List Documents

`GET /api/documents`

Required header: `X-User-Id`

Returns documents associated with the supplied user identifier.

### Retrieve a Document

`GET /api/documents/{id}`

Required header: `X-User-Id`

A document that does not exist or is not owned by the supplied user returns HTTP `404 Not Found`.

### Investigate Documents

`POST /api/agent/investigate`

Required header: `X-User-Id`

Example request:

```json
{
  "question": "Has invoice INV-1001 been paid?",
  "documentIds": [1]
}
```

The response contains:

- `conclusion`: Findings supported by document contents.
- `evidence`: Relevant excerpts with document references.
- `conflicts`: Detected conflicting information.
- `missingInformation`: Information that cannot be established from the available evidence.
- `nextAction`: Recommended follow-up.

### List Investigation History

`GET /api/agent/history`

Required header: `X-User-Id`

Returns investigations associated with the supplied user identifier, newest first.

### Retrieve an Investigation

`GET /api/agent/history/{id}`

Required header: `X-User-Id`

Only investigations associated with the supplied user identifier can be retrieved.

## Validation and Error Handling

The application validates questions and document identifiers, checks document ownership, and rejects unsupported or invalid uploads.

Expected HTTP responses include:

- `201 Created` — Document uploaded successfully.
- `400 Bad Request` — Invalid input or rejected investigation request.
- `404 Not Found` — Document or investigation is missing or inaccessible.
- `500 Internal Server Error` — Unexpected server-side failure.

## Design Decisions

- **Layered architecture:** Controllers handle HTTP requests, services implement business logic, repositories access persistence, and entities represent stored data.
- **Evidence-first responses:** Conclusions include document excerpts so users can inspect the supporting information.
- **Conservative conclusions:** A document's statement is not treated as independent confirmation of a real-world fact.
- **Persistent history:** Investigation results are stored in H2 using JPA.
- **Ownership checks:** Document and history queries are scoped to a user identifier.
- **Deterministic investigation:** Rule-based logic makes the current implementation predictable and avoids dependence on external AI services.

## Security Considerations

The `X-User-Id` header is supplied by the caller. It demonstrates ownership isolation but is not authentication because a caller can choose another identifier.

A production deployment should integrate authenticated identity, authorize requests using the authenticated principal, configure appropriate database and file-access controls, and avoid exposing the H2 console publicly.

Credential-seeking requests are rejected by basic rule-based checks. These checks are not a complete security filter and should not be treated as a replacement for a dedicated security review.

## Testing

Run automated tests using:

```bash
mvn clean test
```

The last verified test run reported five tests, zero failures, and zero errors.

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
