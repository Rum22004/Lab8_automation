# Private Cloud Storage App — Lab 08

## Description

This project is a small Spring Boot private cloud storage application for Lab 08.
Each user can register an account, receive a default storage quota of 50 MB, create folders, upload files, view their storage usage, and delete their own account.

The main purpose of this lab is to apply the testing methods from Lesson 08. The project uses JUnit 5, AssertJ, Hamcrest, Playwright for Java, and Allure to test backend logic, REST API responses, and the web UI.

---

## Problem Identification

Before fixing and testing the project, these problems and requirements needed to be checked:

1. A new user must receive exactly 50 MB quota.
2. The application must calculate used space and free space correctly.
3. A user must be able to create folders.
4. A user must be able to upload files.
5. Uploaded files and created folders must appear in the user’s own listing.
6. A user must not be able to upload files beyond their quota.
7. Each user must only see their own files and folders.
8. Deleting an account must also delete that user’s folders and files.
9. API endpoints must return the correct HTTP status codes and JSON response structure.
10. The Thymeleaf UI must allow a user to register, view the dashboard, and see the 50 MB quota.
11. Playwright API and UI tests must run correctly.
12. Allure results must be generated from the test run.

---

## Solution / Fix Summary

The project was checked and fixed to make sure the main Lab 08 requirements work correctly.

The completed solution includes:

* User registration with email and password.
* Automatic 50 MB quota for new users.
* Profile and storage usage display.
* Folder creation.
* File upload.
* File and folder listing by owner.
* Quota validation before upload.
* Correct error handling when upload exceeds quota.
* User isolation so one user cannot see another user’s data.
* Account deletion with cleanup of the user’s files and folders.
* REST API testing using Playwright.
* UI testing using Playwright.
* Screenshot attachment for Allure visual testing.
* All 10 testing methods from Lesson 08 demonstrated in the test suite.

---

## Tech Stack

| Concern                    | Technology            |
| -------------------------- | --------------------- |
| Language                   | Java 17               |
| Framework                  | Spring Boot 3.2       |
| Build Tool                 | Maven                 |
| Database                   | H2 in-memory database |
| UI                         | Thymeleaf             |
| Unit / Integration Testing | JUnit 5               |
| Assertions                 | AssertJ, Hamcrest     |
| API / UI Testing           | Playwright for Java   |
| Report                     | Allure                |

---

## Project Structure

```text
private-cloud-storage/
├── src/main/java/edu/itc/cloud/
│   ├── PrivateCloudStorageApplication.java
│   ├── model/
│   ├── repository/
│   ├── service/
│   └── web/
│       └── ui/
├── src/main/resources/
│   ├── application.properties
│   └── templates/
├── src/test/java/edu/itc/cloud/
│   ├── TestingMethodsServiceTest.java
│   ├── CloudApiPlaywrightTest.java
│   ├── CloudUiPlaywrightTest.java
│   └── support/
├── pom.xml
└── README.md
```

---

## How to Run the Application

Run this command from the project root folder:

```bash
mvn spring-boot:run
```

Then open the web UI:

```text
http://localhost:8080/
```

Other available URLs:

```text
REST API: http://localhost:8080/api/...
H2 Console: http://localhost:8080/h2-console
```

H2 JDBC URL:

```text
jdbc:h2:mem:cloud
```

---

## How to Run Tests

Run:

```bash
mvn clean test
```

Test result after fixing:

```text
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## How to Generate Allure Report

To generate the Allure report:

```bash
mvn allure:report
```

To open/serve the report:

```bash
mvn allure:serve
```

The Allure result files are generated from the test run, and the UI test attaches a screenshot as visual evidence.

---

## Testing Methods Coverage

| #  | Testing Method         | Problem Checked                                                            | Test Method                                                                                                      |
| -- | ---------------------- | -------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------- |
| 1  | Content equals         | New user quota must be exactly 50 MB.                                      | `TestingMethodsServiceTest.newUserQuotaEqualsFiftyMb`                                                            |
| 2  | Contains               | Listing must contain the uploaded file or created folder name.             | `TestingMethodsServiceTest.listingContainsUploadedName`, `CloudApiPlaywrightTest.folderListingContainsNewFolder` |
| 3  | Regex matched          | Email, token, share link, or URL format must be valid.                     | `TestingMethodsServiceTest.shareLinkAndEmailMatchPatterns`, `CloudApiPlaywrightTest.profileContract`             |
| 4  | Formula matched        | Free space must equal quota minus used space.                              | `TestingMethodsServiceTest.freeSpaceFollowsFormula`                                                              |
| 5  | Predicate / condition  | Used storage must never exceed quota.                                      | `TestingMethodsServiceTest.usageNeverExceedsQuota`                                                               |
| 6  | Collection matching    | Folder/file list must have correct items, correct size, and no duplicates. | `TestingMethodsServiceTest.collectionShapeIsExact`                                                               |
| 7  | Exception matching     | Upload beyond quota must fail correctly.                                   | `TestingMethodsServiceTest.overQuotaThrows`, `CloudApiPlaywrightTest.unauthenticatedIsForbidden`                 |
| 8  | Tolerance / range      | Used MB and free bytes must be within a valid range.                       | `TestingMethodsServiceTest.toleranceAndRange`                                                                    |
| 9  | Schema / JSON matching | API profile response must contain the correct JSON fields and values.      | `CloudApiPlaywrightTest.profileContract`                                                                         |
| 10 | Visual / snapshot      | UI dashboard must show the user quota correctly and attach a screenshot.   | `CloudUiPlaywrightTest.registerThroughUiAndSeeQuota`                                                             |

---

## Bonus Test Coverage

The project also includes extra tests for important cloud storage behavior:

| Feature                  | Test Method                                        |
| ------------------------ | -------------------------------------------------- |
| User data isolation      | `TestingMethodsServiceTest.usersAreIsolated`       |
| Account deletion cleanup | `TestingMethodsServiceTest.deleteAccountWipesData` |

---

## Final Result

After fixing and testing the project:

```text
Tests run: 14
Failures: 0
Errors: 0
Skipped: 0
Build: SUCCESS
```

This confirms that the Private Cloud Storage App meets the Lab 08 requirements and demonstrates all 10 testing methods from Lesson 08.

---

## Submission

For submission:

1. Push the completed project to a public GitHub repository.
2. Include the updated README.md.
3. Include the Allure report or screenshots if required by the teacher.
4. Submit the GitHub repository link to Moodle.
