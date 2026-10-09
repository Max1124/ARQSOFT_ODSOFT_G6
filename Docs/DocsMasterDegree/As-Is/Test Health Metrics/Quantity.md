# Number of Tests by Type of Quantity Table

| Types of Quantity    | Number of Tests |
|:--------------------:|:---------------:|
| Test Classes Created |       21        |
| Created Tests        |       109       |
| Implemented Tests    |       106       |
| Active Tests         |       102       |
| Inactive Tests       |        7        |
| Unit Tests           |       89        |
| Integration Tests    |       17        |

A test is **created** when a `@Test` method exists, including methods that are commented out. It is **implemented** when that method has a body. It is **active** when Maven Surefire executes it, and **inactive** when the method is commented out and is not executed.

A test is an **integration** test when its class loads a Spring context (`@SpringBootTest`, `@DataJpaTest` or `@WebMvcTest`). The other 89 implemented tests are **unit** tests. Of the 17 integration tests, 13 are active and 4 are inactive (`TestAuthApi`).

Maven Surefire reported `Tests run: 102, Failures: 0, Errors: 0, Skipped: 0`.

# Number of Tests by Type of Quantity and Test Class

## TestAuthApi Table

| Types of Quantity | Number of Tests |
|:-----------------:|:---------------:|
| Implemented Tests |        4        |
| Active Tests      |        0        |
| Inactive Tests    |        4        |

The whole class is inside a block comment, so the four methods (`testLoginSuccess`, `testLoginFail`, `testRegisterSuccess`, `testRegisterFail`) are not compiled or executed.

## AuthorControllerIntegrationTest Table

| Types of Quantity | Number of Tests |
|:-----------------:|:---------------:|
| Implemented Tests |        0        |
| Active Tests      |        0        |
| Inactive Tests    |        0        |

The class loads `AuthorController` with `@WebMvcTest` and has a `@BeforeEach`, but it declares no `@Test` method.

## AuthorTest Table

| Types of Quantity | Number of Tests |
|:-----------------:|:---------------:|
| Implemented Tests |        9        |
| Active Tests      |        9        |
| Inactive Tests    |        0        |

## BioTest Table

| Types of Quantity | Number of Tests |
|:-----------------:|:---------------:|
| Implemented Tests |        5        |
| Active Tests      |        5        |
| Inactive Tests    |        0        |

## AuthorRepositoryIntegrationTest Table

| Types of Quantity | Number of Tests |
|:-----------------:|:---------------:|
| Implemented Tests |        1        |
| Active Tests      |        1        |
| Inactive Tests    |        0        |

## AuthorServiceImplIntegrationTest Table

| Types of Quantity | Number of Tests |
|:-----------------:|:---------------:|
| Implemented Tests |        1        |
| Active Tests      |        1        |
| Inactive Tests    |        0        |

## BookTest Table

| Types of Quantity | Number of Tests |
|:-----------------:|:---------------:|
| Implemented Tests |        6        |
| Active Tests      |        6        |
| Inactive Tests    |        0        |

## DescriptionTest Table

| Types of Quantity | Number of Tests |
|:-----------------:|:---------------:|
| Implemented Tests |        4        |
| Active Tests      |        4        |
| Inactive Tests    |        0        |

## IsbnTest Table

| Types of Quantity | Number of Tests |
|:-----------------:|:---------------:|
| Implemented Tests |        7        |
| Active Tests      |        7        |
| Inactive Tests    |        0        |

## TitleTest Table

| Types of Quantity | Number of Tests |
|:-----------------:|:---------------:|
| Implemented Tests |        7        |
| Active Tests      |        7        |
| Inactive Tests    |        0        |

## GenreTest Table

| Types of Quantity | Number of Tests |
|:-----------------:|:---------------:|
| Implemented Tests |        4        |
| Active Tests      |        4        |
| Inactive Tests    |        0        |

## LendingNumberTest Table

| Types of Quantity | Number of Tests |
|:-----------------:|:---------------:|
| Implemented Tests |        8        |
| Active Tests      |        8        |
| Inactive Tests    |        0        |

## LendingTest Table

| Types of Quantity | Number of Tests |
|:-----------------:|:---------------:|
| Implemented Tests |       14        |
| Active Tests      |       14        |
| Inactive Tests    |        0        |

## LendingRepositoryIntegrationTest Table

| Types of Quantity | Number of Tests |
|:-----------------:|:---------------:|
| Implemented Tests |        7        |
| Active Tests      |        7        |
| Inactive Tests    |        0        |

## LendingServiceImplTest Table

| Types of Quantity | Number of Tests |
|:-----------------:|:---------------:|
| Implemented Tests |        3        |
| Active Tests      |        3        |
| Inactive Tests    |        3        |

`testFindByLendingNumber`, `testCreate` and `testSetReturned` are active. `testListByReaderNumberAndIsbn`, `testGetAverageDuration` and `testGetOverdue` are commented out and have empty bodies, so they count as inactive and not implemented.

## BirthDateTest Table

| Types of Quantity | Number of Tests |
|:-----------------:|:---------------:|
| Implemented Tests |        4        |
| Active Tests      |        4        |
| Inactive Tests    |        0        |

## PhoneNumberTest Table

| Types of Quantity | Number of Tests |
|:-----------------:|:---------------:|
| Implemented Tests |        4        |
| Active Tests      |        4        |
| Inactive Tests    |        0        |

## ReaderTest Table

| Types of Quantity | Number of Tests |
|:-----------------:|:---------------:|
| Implemented Tests |        9        |
| Active Tests      |        9        |
| Inactive Tests    |        0        |

## NameTest Table

| Types of Quantity | Number of Tests |
|:-----------------:|:---------------:|
| Implemented Tests |        6        |
| Active Tests      |        6        |
| Inactive Tests    |        0        |

## PhotoTest Table

| Types of Quantity | Number of Tests |
|:-----------------:|:---------------:|
| Implemented Tests |        2        |
| Active Tests      |        2        |
| Inactive Tests    |        0        |

## PsoftG1ApplicationTests Table

| Types of Quantity | Number of Tests |
|:-----------------:|:---------------:|
| Implemented Tests |        1        |
| Active Tests      |        1        |
| Inactive Tests    |        0        |
