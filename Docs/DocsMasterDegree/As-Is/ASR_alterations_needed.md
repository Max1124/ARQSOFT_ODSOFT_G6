# Architecturally Significant Requirements

The Library Management Service currently does not adequately support:
1. Extensibility
2. Configurability
3. Testability

Below are occurrences of ASR noncompliance in the system as-is:
## 1. Extensibility
- Business rules sit inside service methods in `LendingServiceImpl.create`:
    - A reader with overdue lendings is refused. (line 65)
    - A reader with 3 outstanding lendings is refused. The limit 3 is a literal. (line 70)
- `LendingServiceImpl.searchLendings` hard-codes its default query:
    - The default start date is today minus 10 days. (lines 130-134)
- `BookServiceImpl.getBooksSuggestionsForReader` has the suggestion algorithm written inline:
    - It loops over the reader's genres and takes the first N books of each. (lines 179-195)
- The fine calculation is fixed inside `Lending`:
    - `getFineValueInCents` multiplies days delayed by a fixed rate. (lines 215-222)
- `ReaderController` depends on a concrete external provider:
    - It injects `ApiNinjasService` directly. (line 63)
    - It calls the service in `findByReaderNumber`. (line 105)
- `ApiNinjasConfig` is tied to one provider:
    - The base URL and the `v1/` version are literals. (lines 18, 29)
    - The `X-API-KEY` header is a literal. (line 20)
- `ReaderController` contains authorisation logic:
    - It checks `instanceof Librarian`. (lines 73, 158, 310)
    - It compares reader numbers. (lines 163, 315)
- `ReaderController` builds the reader number as `year + "/" + seq`. (lines 96, 163, 169, 302)
    - `ReaderNumber` already builds this format. (line 20)
- The photo consistency block is duplicated:
    - `BookServiceImpl.create`. (lines 65-68)
    - `BookServiceImpl.update`. (lines 100-103)
    - `ReaderServiceImpl.create`. (lines 68-70)
    - `ReaderServiceImpl.update`. (lines 110-112)
- The year-sequence count is duplicated:
    - `LendingServiceImpl.create`. (line 79)
    - `ReaderServiceImpl.create`. (line 72)
- `FileStorageService` is a concrete class with no interface. (line 53)
    - It builds local file-system paths with `Paths.get`. (lines 61, 93, 103)

## 2. Configurability
- Secrets are committed in `application.properties`:
    - The API Ninjas key. (main line 74; test line 49)
    - The datasource username and password. (lines 31-32)
    - The RSA key files are in `src/main/resources`. (referenced at lines 18-19)
- `application.properties` holds one fixed environment:
    - `spring.profiles.active=bootstrap` is fixed. (line 6)
    - The datasource URL is a local H2 file. (line 26)
    - `spring.jpa.hibernate.ddl-auto=update`. (line 44)
    - `spring.h2.console.enabled=true`. (line 51)
- `@PropertySource("classpath:config/library.properties")` is repeated in:
    - `LendingServiceImpl`. (line 25)
    - `BookServiceImpl`. (line 31)
    - `Bootstrapper`. (line 34)
    - `BirthDate`. (line 16)
    - `Name`. (line 12; `Name` reads no property)
- `@Value` is scattered across classes:
    - `LendingServiceImpl`. (lines 32, 34)
    - `BookServiceImpl`. (line 40)
    - `Bootstrapper`. (lines 37, 39)
    - `SecurityConfig`. (lines 68-77)
    - `ApiNinjasConfig`. (line 22)
- `BirthDate` does not receive its configured value:
    - `@Value("${minimumReaderAge}")` is on a field of a JPA `@Embeddable`, which is not a Spring bean. (line 27)
    - `minimumAge` stays 0, so the minimum age is never applied. (line 49)
- Hard-coded values that have no property:
    - The limit of 3 outstanding lendings. (`LendingServiceImpl`, line 70)
    - Top 5 books and the one-year window. (`BookServiceImpl`, lines 128-129)
    - Top 5 genre readers. (`ReaderServiceImpl`, line 85)
    - Top 5 readers. (`ReaderController`, line 329)
    - Default page `Page(1, 10)`. (`LendingServiceImpl` line 110, `BookServiceImpl` line 203, `ReaderServiceImpl` line 190)
    - Default lending search window of 10 days. (`LendingServiceImpl`, line 133)

## 3. Testability
- The current date is read directly from `LocalDate.now()`:
    - `Lending`. (lines 140, 141, 169, 183, 188)
    - `LendingNumber`. (lines 38, 77)
    - `ReaderNumber`. (line 20)
    - `BirthDate`. (line 49)
    - `LendingServiceImpl`. (line 133)
    - `BookServiceImpl`. (line 128)
    - `SpringDataLendingRepository`. (lines 104, 170)
    - `SpringDataGenreRepository`. (line 73)
    - `UserBootstrapper`. (lines 59, 97, 121, 145, 169, 193, 217, 241)
- Randomness and a network call are inside `ApiNinjasService`:
    - `Math.random()`. (line 28)
    - `.block()` on the web call. (line 23)
    - An empty response list would make `get(randomIndex)` fail. (lines 27-29)
- Tests depend on the real clock:
    - `LendingTest`. (lines 71, 101, 119, 125)
    - `LendingServiceImplTest`. (lines 91-94, 113-114, 125)
    - `LendingRepositoryIntegrationTest`. (lines 90-93, 139-141, 220)
    - `LendingNumberTest`. (lines 35, 51)
- Tests read production configuration through Spring:
    - `LendingTest` uses `@PropertySource` and `@Value`. (lines 19, 24, 26)
- Tests start the full application context:
    - `LendingServiceImplTest`. (line 32)
    - `LendingRepositoryIntegrationTest`. (line 31)
    - `TestAuthApi`. (line 30)
    - `PsoftG1ApplicationTests`. (line 8)
    - `AuthorServiceImplIntegrationTest`. (line 25)
- The default profile seeds data in every context:
    - `spring.profiles.active=bootstrap` activates `Bootstrapper` and `UserBootstrapper`. (`application.properties` line 6; `@Profile("bootstrap")` at `Bootstrapper` line 33 and `UserBootstrapper` line 29)
- Test coverage is missing:
    - There are no tests for `BookServiceImpl`, `ReaderServiceImpl` or `GenreServiceImpl`.
    - There are no tests for any controller.
    - There are no tests for `SecurityConfig` or `ApiNinjasService`.
- Business rules cannot be tested alone:
    - The lending rules are inside `LendingServiceImpl.create`, with repository calls and exception handling in the same method. (lines 59-83)
