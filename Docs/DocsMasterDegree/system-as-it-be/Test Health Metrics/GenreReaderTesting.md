# Genre and reader — current-behaviour testing

Scope: existing genre reports and reader registration, lookup and update behaviour. No new bibliographic sources or notification features are implemented by this work. These tests provide regression evidence; Project 1 section 1.3.3.4 still prioritizes the new functionality, which needs its own tests later.

## Test organization and SUT

All new suites follow Arrange / Act / Assert (AAA). For exceptions, the operation executes inside the exception assertion. Parameterized inputs provide the Arrange phase where appropriate.

- **Opaque-box, classes:** `GenreServiceImplTest`, `GenreControllerTest`, `ReaderServiceImplTest` and `ReaderControllerUnitTest` exercise a real service/controller with behavioural dependencies mocked. DTOs are input/output data, not mocked SUTs. Cases cover public contracts, rejection, propagation and meaningful interactions.
- **Transparent-box, domain classes:** `GenreBoundaryTest` and `ReaderValueObjectBoundaryTest` select cases from validation branches and boundaries (blank/length limits, phone prefixes and lengths, valid/invalid calendar dates). These complement existing Genre, PhoneNumber and BirthDate tests. Transparency describes how cases are selected, not starting Spring or mocking the SUT.
- **Small collaboration:** `GenreViewMapperCollaborationTest` and `ReaderDetailsPhoneCollaborationTest` exercise real collaborating application classes. ReaderDetails construction also executes ReaderNumber, BirthDate and PhoneNumber; this is explicitly wider than a single-class test. JPA version is supplied as fixture state without changing production constructors.
- **Opaque-box, controller + service + collaborators:** `GenreApiComponentTest` and `ReaderApiComponentTest` execute real controllers, services, generated mappers, domain and repositories with in-memory H2 and rollback. MockMvc simulates HTTP dispatch; these are component/collaboration tests. Reader quote lookup replaces only ApiNinjasService, avoiding live external calls.
- **Opaque-box, system:** `ReaderGenreSystemTest` starts the backend on a random port and sends real HTTP requests with a Java HTTP client. Login issues real JWTs. Only initial librarian/catalogue fixtures use repositories because the required creation endpoints do not exist. Reader workflow and genre reporting use HTTP. A separate disposable H2 database prevents changes to application data. No external-quote endpoint is called.
- **Mutation, classes:** the `reader-genre` Maven profile targets Genre, PhoneNumber, ReaderNumber and BirthDate using the model test suites. This is an explicit initial scope, not a mutation claim for all reader/genre classes. No acceptance threshold or exclusion of surviving mutants is invented.

The suites are not a claim of exhaustive coverage. Several existing domain tests execute real value objects and must not be relabelled isolated solely because their filename ends in Test. Annotations alone do not establish the SUT.

## Execution

From the repository root in PowerShell, with a supported JDK configured:

```powershell
# Functional tests: existing and new genre/reader tests, including the HTTP system test
.\mvnw.cmd -Preader-genre test

# Mutation analysis after functional tests pass
.\mvnw.cmd -Preader-genre test-compile org.pitest:pitest-maven:mutationCoverage
```

Run isolated classes first if evaluating the classroom unit-before-integration sequence:

```powershell
.\mvnw.cmd '-Dtest=GenreTest,GenreBoundaryTest,GenreServiceImplTest,GenreControllerTest,GenreLendingsDTOTest,PhoneNumberTest,BirthDateTest,ReaderValueObjectBoundaryTest,ReaderServiceImplTest,ReaderControllerUnitTest' test
.\mvnw.cmd '-Dtest=GenreViewMapperCollaborationTest,ReaderDetailsPhoneCollaborationTest,GenreApiComponentTest,ReaderApiComponentTest' test
.\mvnw.cmd '-Dtest=ReaderGenreSystemTest' test
```

The profile groups selection; it does not enforce execution ordering. Maven Surefire reports appear under `target/surefire-reports`; JaCoCo under `target/site/jacoco`; PIT under `target/pit-reports-reader-genre`. JaCoCo may accumulate prior execution data: use a clean run for a fresh measurement. Coverage is evidence of executed code, not proof of assertion quality.

## Current-design limitations

BirthDate and ReaderNumber still obtain the current date internally; relevant tests capture the date once but cannot eliminate a midnight boundary without a production change. BirthDate's field-injected minimum age is zero when directly constructed; tests do not pretend this enforces the configured age. No production refactor is included here.

A passing rejection test means the expected rejection occurred. Surviving mutants require review and are different from failing functional tests. No pipeline rule allowing failed integration tests has been introduced.

## Recorded verification — 2026-10-24

- `mvnw.cmd -Preader-genre test`: 105 cases, 0 failures, 0 errors, 0 skipped. Includes 84 new cases across 12 new test classes and 21 pre-existing cases.
- `mvnw.cmd -Preader-genre test-compile org.pitest:pitest-maven:mutationCoverage`: 19 mutants generated, 19 killed, none survived or lacked coverage. The mutation scope is only Genre, PhoneNumber, ReaderNumber and BirthDate. PIT reported 50/54 covered lines (93%) for these mutated classes, not for the entire application.
- Java 21.0.5, Maven wrapper; compilation targets Java 17. Initial sandbox execution failed to communicate with the Mockito instrumentation attach pipe; the verified runs completed outside that restriction.
- Production sources and existing tests were not changed. The complete project test suite was not run; the verified functional scope is the reader-genre profile.
