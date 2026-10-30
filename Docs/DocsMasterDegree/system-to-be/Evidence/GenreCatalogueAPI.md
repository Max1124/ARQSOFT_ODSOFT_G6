# Genre catalogue operations

These operations support explicit librarian selection/creation during bibliographic registration.
They do not automatically import external genre suggestions. All requests require a librarian bearer token.

- `GET /api/genres`: list all genres, including those without books, as `{"items":[{"genre":"Computers"}]}`.
- `POST /api/genres`: create a genre using JSON `{"genre":"Computers"}`; returns 201.
- `PUT /api/genres/{name}`: rename an existing genre using JSON `{"genre":"Computing"}`; returns 200.
- `DELETE /api/genres/{name}`: delete an unused genre; returns 204.

Use the exact current catalogue name in the URL, URL-encoding spaces and accented characters.
Names are trimmed on create/update, nonblank, and at most 100 characters. Duplicate checks ignore case
and surrounding whitespace. A duplicate returns 409; a missing update/delete target returns 404.
Renaming preserves the existing entity and its book/reader associations. Deletion returns 409 when
books or reader interests reference the genre; those associations must be resolved first.

After creating `Computers`, bibliographic registration can use:

```json
{"authorIds":[1552],"genre":"Computers"}
```

The author ID must exist in the running catalogue. Creating the genre does not create a book.

Verification: `GenreCrudIntegrationTest` exercises real REST controllers, services, domain and H2
persistence with controlled authentication. It covers CRUD, unused-genre listing, invalid/duplicate
names, missing targets, preservation of book associations, book/reader deletion conflicts and access control.
Run with `./mvnw -Dtest=Genre*Test test` (PowerShell: `.\mvnw.cmd '-Dtest=Genre*Test' test`).
