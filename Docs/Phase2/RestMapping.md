# Rest Mapping

| UC      | Method | URI Template                                                                    | Equivalent RPC operation   | Access                  |                                       
|---------|:-------|:--------------------------------------------------------------------------------|----------------------------|-------------------------|
| A3/B3   | POST   | api/authors                                                                     | createAuthor               | Librarian               |
| A4      | PATCH  | api/authors/{authorNumber}                                                      | updateAuthor               | Librarian               |
| A5      | GET    | api/authors/{authorNumber}                                                      | getAuthor                  | Librarian, Reader       |
| A6      | GET    | api/authors?name={name}                                                         | searchAuthorsByName        | Librarian, Reader       |
| A7/B7   | PUT    | api/books/{isbn}                                                                | createBook                 | Librarian               |
| A8      | PATCH  | api/books/{isbn}                                                                | updateBook                 | Librarian               |
| A9      | GET    | api/books/{isbn}                                                                | getBook                    | Librarian, Reader       |
| A10     | GET    | api/books?genre={genre}                                                         | getBooksByGenre            | Librarian, Reader       |
| A11/B12 | POST   | api/readers                                                                     | createReader               | *Anon*                  |
| A12     | PATCH  | api/readers/{year}/{seq}                                                        | updateReader               | Reader(Self)            |
| A13     | GET    | api/readers/{year}/{seq}                                                        | getReader                  | Librarian, Reader(Self) |
| A14     | GET    | api/readers?name={name}                                                         | searchReadersByName        | Librarian               |
| A15     | POST   | api/lendings                                                                    | createLending              | Librarian               |
| A16     | GET    | api/readers/{year}/{seq}/lendings?isbn={isbn}                                   | getReaderLendingsByIsbn    | Reader(Self)            |
| A16     | PATCH  | api/lendings/{year}/{seq}                                                       | updateLending              | Reader(Self)            |
| A17     | GET    | api/lendings/{year}/{seq}                                                       | getLending                 | Librarian, Reader(Self) |
|         |        | PHASE 2                                                                         |                            |                         |
| B4      | GET    | api/authors/{authorNumber}/books                                                | getBookByAuthor            | Reader                  |
| B5      | GET    | api/authors/{authorNumber}/coauthors                                            | getAuthorCoauthors         | Reader                  |
| B6      | GET    | api/authors/top5                                                                | getTop5Authors             | Reader                  |
| B8      | GET    | api/books?title={title}                                                         | searchBookByTitle          | Reader                  |
| B9      | GET    | api/books/top5                                                                  | getTop5Books               | Librarian               |
| B10     | GET    | api/genres/top5                                                                 | getTop5Genres              | Librarian               |
| B11     | GET    | api/readers/top5                                                                | getTop5Readers             | Librarian               |
| B13     | GET    | api/books/suggestions                                                           | getBookSuggestions         | Reader(Self)            |
| B23     | GET    | api/lendings/overdue                                                            | getOverdueLendings         | Librarian               |
| B14     | GET    | api/genres/avgLendings?period={day}&start={date}&end{date}                      | getAverageLendings         | Librarian               |
| B15     | GET    | api/lendings/avgDuration                                                        | getAverageDuration         | Librarian               |
| B16     | GET    | api/lendings?period={month}&count={12}                                          | getLendingCount            | Librarian               |
| B17     | GET    | api/genres/top5readers                                                          | getTop5Readers             | Librarian               |
| B18     | GET    | api/lendings/averageMonthlyPerReader                                            | getAverageMonthlyPerReader | Librarian               |
| B19     | GET    | api/lendings/avgDuration?period={month}&groupBy={genre}&start={date}&end={date} | getAverageDuration         | Librarian               |

[//]: # (<img src="RestMapping-PSOFT_G1_Rest_Mapping.svg" alt="Rest Mapping Diagram">)
