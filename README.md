# Private Cloud Storage App — Lab 08 Starter

A small **Spring Boot** backend where each user manages files and folders in
their **own** isolated cloud storage. New users get a **50 MB** quota, can manage
their profile, and can delete their own account.

This starter ships with a test suite that demonstrates **all ten testing methods**
from Lesson 08, driven by **JUnit 5 / AssertJ** and **Playwright (Java)**, and
reported with **Allure**.

> This is a *starter*. Read the TODOs, extend the features, and — most
> importantly — **write tests that exercise every testing method** before you
> submit.

---

## Tech stack

| Concern        | Choice                                   |
|----------------|------------------------------------------|
| Language       | Java 17                                  |
| Framework      | Spring Boot 3.2 (Web, Data JPA, Validation) |
| Database       | H2 (in-memory)                           |
| UI             | Thymeleaf (server-rendered)              |
| Unit/Integration tests | JUnit 5 + AssertJ + Hamcrest      |
| HTTP/API + browser tests | Playwright for Java            |
| Reporting      | Allure                                   |
| Build          | Maven                                    |

---

## Project layout

```
private-cloud-storage/
├── src/main/java/edu/itc/cloud/
│   ├── PrivateCloudStorageApplication.java
│   ├── model/        # User, Folder, FileEntity
│   ├── repository/   # Spring Data JPA repositories
│   ├── service/      # UserService, StorageService, exceptions
│   └── web/          # REST controllers, AuthService, DTOs, error handler
│       └── ui/       # WebUiController (Thymeleaf, session-based)
├── src/main/resources/
│   ├── application.properties
│   └── templates/    # login.html, dashboard.html (Thymeleaf UI)
├── src/test/java/edu/itc/cloud/
│   ├── TestingMethodsServiceTest.java   # methods 1–8 + isolation + deletion
│   ├── CloudApiPlaywrightTest.java      # schema/regex/contains over HTTP
│   ├── CloudUiPlaywrightTest.java       # browser UI test + screenshot snapshot
│   └── support/TestFiles.java
├── .github/workflows/ci.yml
├── pom.xml
└── README.md
```

---

## Run the app

```bash
mvn spring-boot:run
# Web UI  on http://localhost:8080/        (register, then manage files/folders)
# REST API on http://localhost:8080/api/...
# H2 console on http://localhost:8080/h2-console  (JDBC URL: jdbc:h2:mem:cloud)
```

Open <http://localhost:8080/> in a browser, click **Register** (you get 50 MB),
then create folders and upload files from the dashboard.

### Try it with curl

```bash
# Register (returns a token) — new user gets 50 MB
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"sok@itc.edu","password":"Secret123!"}'

TOKEN=...   # paste the token from the response

# Profile + quota usage
curl -s http://localhost:8080/api/me -H "Authorization: Bearer $TOKEN"

# Create a folder
curl -s -X POST http://localhost:8080/api/folders \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"Documents"}'

# Upload a file (multipart)
curl -s -X POST http://localhost:8080/api/files \
  -H "Authorization: Bearer $TOKEN" -F "file=@./photo.png"
```

---

## Run the tests

```bash
mvn test
```

> **Browser handling:** The UI test (`CloudUiPlaywrightTest`) is configured to use
> an installed browser on your system (Microsoft Edge by default, falls back to Chrome,
> then Chromium). This avoids relying on potentially unreliable CDN downloads for
> browser binaries. If you have neither Edge nor Chrome installed, the test will
> fall back to downloading Chromium on the first run (which needs internet access).

---

## View the Allure report

```bash
# Option A — Allure CLI (https://allurereport.org)
allure serve target/allure-results

# Option B — Maven plugin (no separate CLI needed)
mvn allure:serve
# or build static HTML into target/site/allure-maven-plugin
mvn allure:report
```

---

## Testing methods → where they are demonstrated

Fill in / extend this table as you add your own tests. The starter **already
covers every row at least once**.

| # | Testing method        | Demonstrated in | Test class |
|---|-----------------------|-----|---|
| 1 | Content equals        | `newUserQuotaEqualsFiftyMb` (quota = 50 MB exactly) · `profileContract` (HTTP 200) | `TestingMethodsServiceTest` · `CloudApiPlaywrightTest` |
| 2 | Contains              | `listingContainsUploadedName` (file list contains names) · `folderListingContainsNewFolder` (API response contains folder name) · `registerThroughUiAndSeeQuota` (heading text contains "My Cloud Storage") | `TestingMethodsServiceTest` · `CloudApiPlaywrightTest` · `CloudUiPlaywrightTest` |
| 3 | Regex matched         | `shareLinkAndEmailMatchPatterns` (email + share link format) · `profileContract` (email regex in JSON) · `registerThroughUiAndSeeQuota` (dashboard URL pattern) | `TestingMethodsServiceTest` · `CloudApiPlaywrightTest` · `CloudUiPlaywrightTest` |
| 4 | Formula matched       | `freeSpaceFollowsFormula` (free = quota - used)         | `TestingMethodsServiceTest` |
| 5 | Predicate / condition | `usageNeverExceedsQuota` (custom assertion on user object) | `TestingMethodsServiceTest` |
| 6 | Collection            | `collectionShapeIsExact` (list size, order, no duplicates) | `TestingMethodsServiceTest` |
| 7 | Exception             | `overQuotaThrows` (QuotaExceededException on oversized upload) · `unauthenticatedIsForbidden` (403 Forbidden on missing auth) | `TestingMethodsServiceTest` · `CloudApiPlaywrightTest` |
| 8 | Tolerance / range     | `toleranceAndRange` (isCloseTo 25 MB ±0.01) · `freeBytes` between 0 and quota | `TestingMethodsServiceTest` |
| 9 | Schema / JSON         | `profileContract` (response has email, displayName, quotaBytes, usedBytes, freeBytes; email matches regex; quota = 52428800) | `CloudApiPlaywrightTest` |
| 10| Visual / snapshot     | `registerThroughUiAndSeeQuota` (browser screenshot attached to Allure report) | `CloudUiPlaywrightTest` |

**Bonus coverage:** per-user **isolation** (`usersAreIsolated` — users cannot access each other's files) and **account deletion** (`deleteAccountWipesData` — deleting user removes all their data).

---

## What to do for the lab

1. Extend the app so all functional requirements (R1–R8) are solid.
2. Make sure **every testing method** is demonstrated and listed in the table above.
3. Generate the Allure report (commit `target/allure-report/` or screenshots).
4. Push to a **public** GitHub repository.
5. **Submit the GitHub link to Moodle.**

### Ideas to extend the starter
- Replace the toy password hash with **BCrypt** and wire **Spring Security**.
- Add file **move** between folders and folder **rename** tests.
- Add a thin **Thymeleaf** UI and a browser-driven Playwright test with a real
  screenshot snapshot.
- Add **JSON-schema** validation (e.g. `networknt/json-schema-validator`) for a
  stronger contract test.

---

## Notes & caveats (read before grading yourself)

- The in-memory bearer token store (`web/AuthService`) and the password hash in
  `UserService` are **deliberately minimal** for the starter — harden them.
- The database is in-memory and resets on restart.
- File bytes are stored in the DB as a BLOB to keep the project self-contained.
