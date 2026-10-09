# Architecturally Significant Requirements

The Library Management Service currently does not adequately support:
1. Extensibility
2. Configurability
3. Testability

Below are occurrences of ASR noncompliance in the system as-is:
## 1. Extensibility

- In `LendingServiceImpl`:
  - In `create`, a reader with overdue lendings is refused. (line 65)
  - In `create`, a reader with 3 outstanding lendings is refused. The limit 3 is a literal. (line 70)
  - In `create`, the lending sequence is obtained from the year's count. This is duplicated in `ReaderServiceImpl.create` in line 72. If the criteria for lending sequence changes then it will have to be changed in both places  (line 79)
  - `searchLendings` hard-codes its default query, so the default start date is today minus 10 days. (lines 130-134)

- In `BookServiceImpl`:
  - In `create` (lines 65-68) and in `update` (lines 100-103), the photo consistency block is duplicated.

- In `ReaderServiceImpl`:
  - In `create` (lines 68-70) and in `update`(lines 110-112), the photo consistency block is duplicated. 
  - In `create`, the reader sequence is obtained from the year's count. This is duplicated in `LendingServiceImpl.create`. (line 72)

- In `ReaderController`:
  - It calls `ApiNinjasService`, `ConcurrencyService` and `FileStorageService` directly instead of using DI. (line 63)
  - In `findByReaderNumber`, it builds the reader number as `year + "/" + seq` by itself instead of using `ReaderNumber`. (line 96)
  - In `getSpecificReaderPhoto`, it checks `instanceof Librarian`. (line 158)
  - In `getSpecificReaderPhoto`, it compares reader numbers and builds them as `year + "/" + seq` instead of using `ReaderNumber`. (lines 163, 169)
  - In `getReaderLendings`, it builds the reader number as `year + "/" + seq` instead of using `ReaderNumber`. (line 302)
  - In `getReaderLendings`, it checks `instanceof Librarian`. (line 310)
  - In `getReaderLendings`, it compares reader numbers. (line 315)
  - In `getData`, it checks `instanceof Librarian`. (line 73)

- `ApiNinjasConfig` is tied to one provider:
  - The base URL and the `v1/` version are literals. (lines 18, 29)
  - The `X-API-KEY` header is a literal. (line 20)

- `FileStorageService` is a concrete class with no interface. (line 53)
  - It builds local file-system paths with `Paths.get`. (lines 61, 93, 103)

- `ConcurrencyService` is a concrete class with no interface. (line 8)

## 2. Configurability

- In `application.properties`:
  - The API Ninjas key. (line 74)
  - The datasource username and password. (lines 31-32)
  - The RSA key files are in `src/main/resources`. (referenced at lines 18-19)
  - `spring.profiles.active=bootstrap` is fixed. (line 6)
  - The datasource URL is a local H2 file. (line 26)
  - `spring.jpa.hibernate.ddl-auto=update`. (line 44)
  - `spring.h2.console.enabled=true`. (line 51)

- The test `application.properties` commits a secret:
  - The API Ninjas key. (line 49)

- `LendingServiceImpl` loads and scatters configuration:
  - In `create`, the limit of 3 outstanding lendings has no property. (line 70)
  - In `getOverdue`, the default page `Page(1, 10)` has no property. (line 110)
  - In `searchLendings`, the default search window of 10 days has no property. (line 133)

- `BookServiceImpl` loads and scatters configuration:
  - In `findTop5BooksLent`, the one-year window and the top 5 have no property. (lines 128-129)
  - In `searchBooks`, the default page `Page(1, 10)` has no property. (line 203)

- In `ReaderServiceImpl`:
  - In `findTopByGenre`, the top 5 has no property. (line 85)
  - In `searchReaders`, the default page `Page(1, 10)` has no property. (line 190)

- In `ReaderController`:
  - In `getTop`, the top 5 has no property. (line 329)

- `BirthDate` does not receive its configured value:
  - `@Value("${minimumReaderAge}")` is on a field of a JPA `@Embeddable`, which is not a Spring bean. (line 27)
  - `minimumAge` stays 0, so the minimum age is never applied. (line 49)

- `Name` declares configuration it does not use:
  - `@PropertySource("classpath:config/library.properties")` is declared, but `Name` reads no property. (line 12)

- `Bootstrapper` loads and scatters configuration:
  - `@PropertySource("classpath:config/library.properties")` is repeated here. (line 34)

## 3. Testability

- `Lending` reads the current date directly from `LocalDate.now()`:
  - Lines 140, 141, 169, 183, 188.

- `LendingNumber` reads the current date directly from `LocalDate.now()`:
  - Lines 38, 77.

- `ReaderNumber` reads the current date directly from `LocalDate.now()`:
  - Line 20.

- `BirthDate` reads the current date directly from `LocalDate.now()`:
  - Line 49.

- `LendingServiceImpl` reads the current date directly, and its rules cannot be tested alone:
  - In `searchLendings`, `LocalDate.now()` is called. (line 133)
  - In `create`, the lending rules, repository calls and exception handling are in the same method. (lines 59-83)

- `BookServiceImpl` reads the current date directly and has no tests:
  - In `findTop5BooksLent`, `LocalDate.now()` is called. (line 128)
  - There are no tests for this class.

- `ReaderServiceImpl` has no tests:
  - There are no tests for this class.

- `GenreServiceImpl` has no tests:
  - There are no tests for this class.

- `SpringDataLendingRepository` reads the current date directly:
  - Lines 104, 170.

- `SpringDataGenreRepository` reads the current date directly:
  - Line 73.

- `UserBootstrapper` reads the current date directly and runs under the default profile:
  - `LocalDate.now()` is called at lines 59, 97, 121, 145, 169, 193, 217, 241.
  - `@Profile("bootstrap")` is set. (line 29)

- `Bootstrapper` runs under the default profile:
  - `@Profile("bootstrap")` is set. (line 33)
  - `spring.profiles.active=bootstrap` in `application.properties` activates it, and `UserBootstrapper`, in every context. (line 6)

- `ApiNinjasService` has randomness, a blocking network call and no tests:
  - `Math.random()` selects the event. (line 28)
  - `.block()` is called on the web call. (line 23)
  - An empty response list would make `get(randomIndex)` fail. (lines 27-29)
  - There are no tests for this class.

- Controllers and `SecurityConfig` have no tests:
  - There are no tests for any controller.
  - There are no tests for `SecurityConfig`.

- `LendingTest` depends on the real clock and on Spring configuration:
  - `LocalDate.now()` is used in assertions. (lines 71, 101, 119, 125)
  - `@PropertySource` and `@Value` read production configuration. (lines 19, 24, 26)

- `LendingServiceImplTest` depends on the real clock and starts the full application context:
  - `LocalDate.now()` is used. (lines 91-94, 113-114, 125)
  - `@SpringBootTest` is used. (line 32)

- `LendingRepositoryIntegrationTest` depends on the real clock and starts the full application context:
  - `LocalDate.now()` is used. (lines 90-93, 139-141, 220)
  - `@SpringBootTest` is used. (line 31)

- `LendingNumberTest` depends on the real clock:
  - `LocalDate.now()` is used. (lines 35, 51)

- `TestAuthApi` starts the full application context:
  - `@SpringBootTest` is used. (line 30)

- `PsoftG1ApplicationTests` starts the full application context:
  - `@SpringBootTest` is used. (line 8)

- `AuthorServiceImplIntegrationTest` starts the full application context:
  - `@SpringBootTest` is used. (line 25)
