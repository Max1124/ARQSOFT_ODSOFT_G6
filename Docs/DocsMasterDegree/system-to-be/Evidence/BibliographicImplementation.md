# Bibliographic implementation and verification

## What was implemented and why

- `BibliographicSource` is the extension contract. Google Books and Open Library adapters encapsulate their requests and use dedicated mappers, producing partial `BibliographicData` and Found/NotFound/Failure outcomes.
- `BibliographicService` receives an ordered source collection and `BibliographicMerge`. First available scalar values win, later sources fill blanks, the first non-empty author list is kept with divergence warnings, and genre suggestions are deduplicated by trimmed, case-insensitive comparison. Field provenance is returned.
- `SourceSelection` resolves installed registrations, preserves order and rejects empty/unknown/repeated identifiers and duplicate registrations. Adding another provider needs its adapter/mapper and registration, not coordinator conditionals. A fake third-source test exercises this extension.
- `BibliographicTransport` uses Java's HTTP client with connect/request timeouts (default 5000 ms, allowed 1..60000). Injected transport and mappers allow controlled tests. Requests are sequential; startup selection makes no network calls. Timeout is per provider, not a global lookup deadline.
- `CatalogueMatcher` only suggests authors based on names. A unique catalogue genre is matched after trimming outer whitespace and ignoring case. Ambiguous genres require a choice. No author or genre is created by retrieval.
- `AuthorCorrespondence` persists an explicit librarian confirmation for a source + ISBN + external author name, referencing an existing catalogue author. The key is SHA-256 of those values; name comparison is case-sensitive after trimming. This conservative book-specific scope avoids treating a name as a global identity. Confirming one ISBN does not automatically resolve the author of another edition. Missing/deleted author references require completion again.
- `BibliographicRegistrationService` separates read-only lookup from registration. Missing/invalid mandatory fields or unresolved associations return completion needs without saving a book. Explicit librarian overrides take priority. Valid registration delegates to the existing BookService and preserves catalogue validation. An absent description is passed as an empty string because the existing Book getter assumes a Description object; no existing domain class was weakened.

## REST contract

All new routes require a librarian. Existing book endpoints remain available.

- `GET /api/bibliographic/{isbn}`: 200 with `lookup` (status, partial data, per-source outcomes, warnings) and `catalogue` (resolved author IDs/genre, author suggestions and completion needs). NotFound/Failure are structured lookup results, not catalogue writes.
- `PUT /api/bibliographic/{isbn}/author-confirmations`: JSON `{ "source": "google-books", "externalName": "Robert Martin", "authorId": 1 }`. Returns 204. The name must occur in that enabled source's current ISBN result, and the catalogue author must exist. Repeat confirmation explicitly replaces the correspondence.
- `POST /api/bibliographic/{isbn}/registration`: optional JSON body; `{}` tries ISBN-only registration. Optional overrides: `title`, `description`, `genre` (existing catalogue name), `authorIds` (existing catalogue IDs). Returns 201 and Location for a saved book, or 422 with preview and `needs`, saving nothing. Existing ISBN: 409. Invalid ISBN: 400. Authentication/role failures: 401/403.

If at least one source succeeds, available data is retained. A failed source adds an incomplete-lookup warning. All NotFound means no match; no Found with any Failure means lookup could not be completed. A librarian may supply complete manual overrides even when external metadata is unavailable; domain and association validation still apply.

## Configuration and reproduction

### Google Books application identification

GoogleBooksAdapter accepts an external API key and sends it as the encoded `key` query parameter.
Set `GOOGLE_BOOKS_API_KEY` in the environment of the process launching the application, or supply
`bibliographic.google-books.api-key` through external configuration. Do not commit a real key.
The key is optional in code so controlled local-provider tests can run without credentials;
Google production requests should use the project's key. No live success with that key is claimed yet.
HTTP failure outcomes expose only the status code, not the provider body or credential.

In PowerShell, enter the key without displaying or saving it in command history, then start the application:

```powershell
$secret = Read-Host 'Chave Google Books' -AsSecureString
$env:GOOGLE_BOOKS_API_KEY = [System.Net.NetworkCredential]::new('', $secret).Password
Remove-Variable secret
.\mvnw.cmd spring-boot:run
```

Development defaults are in application.properties. Deployment overrides must be outside the JAR. Copy `config/bibliographic.properties.example` to a deployment-owned file and use standard Spring Boot additional configuration:

```powershell
java -jar target/psoft-g1-0.0.1-SNAPSHOT.jar --spring.config.additional-location=file:C:/deployment/bibliographic.properties
```

Example file:

```properties
bibliographic.sources=google-books,open-library
bibliographic.timeout-ms=10000
```

Switch to `open-library` or reverse the list and restart the same JAR. Spring command-line/environment settings can override file values; `BIBLIOGRAPHIC_SOURCES` is the environment form. Empty values are rejected, not interpreted as defaults. The configuration tests exercise startup with one/both sources, reversed order and invalid values. The third-source collaboration checks that a disabled source receives no calls. These tests are not a claim of deployment to Azure.

Provider endpoints are configurable (`bibliographic.google-books.url`, `bibliographic.open-library.url`) so tests use local HTTP servers. They are administrator configuration, never request parameters. Do not configure real endpoints in automated tests.

```powershell
.\mvnw.cmd -Pbibliographic test
.\mvnw.cmd -Pbibliographic test-compile org.pitest:pitest-maven:mutationCoverage
.\mvnw.cmd package
```

Use separate JaCoCo execution files or a clean run to avoid accumulated coverage. The bibliographic profile does not alter the existing lending Jenkins selection; enabling this profile in CI is a separate pipeline change.

## Test boundaries and evidence

- Class contracts: source selection, merge, adapters (mock transport/mapper) and registration (mock behavioural collaborators). The coordinator also executes the existing Isbn validator; catalogue matching also uses the real correspondence-key function. Those paths are small collaborations under the classroom convention, not strict single-class isolation.
- Domain transparent-box: correspondence identity/validation, including scoped keys and positive ID boundaries. Existing Book/Author invariants are retained, not invented solely to fill a test category.
- Mapper tests: mapper plus real Jackson parsing and shared ProviderJson helper; these are small collaboration tests, not strict single-production-class isolation.
- Component: real API, service, adapters, HTTP transport, mappers, domain and H2 repositories, with local provider doubles.
- System: actual random-port HTTP server, real login/JWT, confirmation, incomplete registration, successful ISBN-only registration and duplicate rejection. Reference catalogue is seeded explicitly. No live providers are used by automated tests.
- Mutation: explicitly scoped to BibliographicMerge, SourceSelection and AuthorCorrespondence; not a claim for the whole application.

### Recorded results — 2026-10-28

- Full regression and packaging: **570 tests, 0 failures, 0 errors, 0 skipped**, BUILD SUCCESS. This execution included the first 41 bibliographic cases; the subsequent 11 boundary/error cases were verified in the final bibliographic run. Production code was unchanged after the full regression.
- Final bibliographic run: **52 cases, 0 failures, 0 errors, 0 skipped**, BUILD SUCCESS.
- Isolated JaCoCo scope: packages containing `bibliographicmanagement`, **345/359 executable lines (96.10%)**, **191/238 branches (80.25%)**. Generated/existing classes outside that package are not included. This exceeds the bibliographic 80% branch proposal; it does not establish 85% coverage for the entire project.
- PIT: **38 generated, 38 killed, 0 survived, 0 without coverage**, limited to BibliographicMerge, SourceSelection and AuthorCorrespondence. Initially two survived; a blank-field fallback test and ID=1 boundary test closed those specific assertion gaps. No mutants were excluded to improve the score.
- Java 21.0.5 / Maven wrapper; compilation targets Java 17. No new dependency was required for HTTP or local provider doubles. `git diff --check` passed.

Machine-readable summary: [BibliographicValidation.json](Evidence/BibliographicValidation.json).

Local generated evidence (not committed): `target/bibliographic-tests-final.log`, `target/bibliographic-regression.log`, `target/bibliographic-pit-final.log`, `target/bibliographic-coverage-final.csv`, `target/site/jacoco/index.html`, `target/pit-reports-bibliographic/index.html`. Reports under target can be overwritten by later Maven runs. The test classes in `src/test/java/pt/psoft/g1/psoftg1/bibliographicmanagement` provide reproducible assertions.

Compilation emitted existing MapStruct unmapped-property and deprecated Spring Security warnings. PIT emitted an optional Spring plugin advisory; the selected pure-class mutation run completed successfully.

### Limits of the evidence

No Azure deployment or Jenkins change was performed. Existing lending CI does not automatically run the bibliographic profile. A live Google success remains unverified because of HTTP 429. Open Library's live response was checked, but the demonstrated full registration uses controlled HTTP providers. The same-artifact configuration workflow is documented and startup binding is tested; a separate two-deployment Azure experiment was not performed. No manual title search or cover download was added.

## Provider documentation and live check

Requests follow [Google volumes.list](https://developers.google.com/books/docs/v1/reference/volumes/list) and the [Open Library Books API](https://openlibrary.org/dev/docs/api/books). Open Library marks `/api/books` as legacy; the adapter isolates that dependency for future replacement. Its `jscmd=data` response may omit description, which remains optional. Google results are filtered to an exact returned ISBN identifier to avoid importing another edition.

On 2026-10-28 a public HTTP check for ISBN 9780132350884 returned Open Library 200 with the matching ISBN object, and Google 429 (Too Many Requests). This verifies observed endpoint availability only, not an end-to-end live registration. Local tests verify failure handling, but cannot guarantee third-party uptime or quotas. Google live success remains unverified in this environment.

## Implementation changes
## Name validation and possible duplicate authors

Name validation: Updated the shared Name value object to accept valid names such as Robert C. Martin, including accented characters, apostrophes and hyphens. Invalid characters and names exceeding 150 characters are rejected.

Author matching: Improved CatalogueMatcher to ignore periods and repeated whitespace when suggesting existing authors. Suggestions do not automatically resolve identities or merge duplicate authors.

Duplicate handling: Existing authors can be reused through catalogue suggestions and explicit librarian confirmation. Automatic duplicate detection and merging remain outside the current implementation.

## Additional verification

QAS1 — Extensibility: Verified that a third bibliographic source can be registered, selected and merged through the existing coordinator without modifying its logic.

QAS4 — Configurability: Tested different source configurations, execution order, disabled sources, invalid configurations and Google API key propagation using controlled HTTP transport.

QAS9 — Testability: Expanded automated tests covering domain validation, provider adapters, mapping, catalogue matching, registration and real HTTP/JWT workflows with local provider doubles.

## Updated test results

Full regression: 593 tests passed, with zero failures, errors or skipped tests.

Mutation testing (PIT): 124 mutants generated, 108 killed (87.10%), 9 survived and 7 without coverage.

Mutation scope: Expanded from 3 to 9 production classes. The previous result of 38/38 killed remains valid for its smaller scope.

Regression validation: Confirmed that Robert C. Martin can be stored and retrieved through the author API.
