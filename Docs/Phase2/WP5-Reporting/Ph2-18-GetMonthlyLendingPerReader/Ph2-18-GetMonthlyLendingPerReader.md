# WP#5.18 Get Monthly Lending Per Reader
## 1. Requirements Engineering
### 1.1. User Story Description

As Librarian I want to know the Monthly lending per reader of a certain period

### 1.2. Customer Specifications and Clarifications

>[View WP5](../WP5-Lendings.md/#12-customer-specifications-and-clarifications)

>[Q: Pretende-se uma lista de médias, ou a média de um utilizador em específico? ](https://moodle.isep.ipp.pt/mod/forum/discuss.php?d=29859#p37874)
>
>A:  Pretende-se a média de empréstimos. Ou seja, quantos livros foram emprestados num dado mês tendo em conta a totalidade de leitores registados na biblioteca

>[Q: A conta é **média( total de empréstimos numa data / utilizadores registados a essa data)**, entre o início e o fim de um mês?
](https://moodle.isep.ipp.pt/mod/forum/discuss.php?d=29859#p38059)
>
>A: sim

>[Q: Quais são os critérios de aceitação destas USs?](https://moodle.isep.ipp.pt/mod/forum/discuss.php?d=30099#p38213)
>
>A: lista com o número de empréstimos de um dado leitor para um dado período


### 1.3. Acceptance Criteria
- Interpreted AC: 
  - The API will return, per reader:
    - The lending count per month, divided by the number of months the reader has been registered 
    - Ordered by date of reader registration
### 1.4. Found out Dependencies
### 1.5 Input and Output Data

**Input Data:**

* Typed data:
  * (...)

**Output Data:**

* (In)success of the operation


### 1.6. System Sequence Diagram (SSD)
### 1.7. Functionality
### 1.8 Other Relevant Remarks
## 2. OO Analysis
### 2.1. Relevant Domain Model Excerpt
### 2.2. Other Remarks
## 3. Design
### 3.1. Sequence Diagram (SD)

[//]: # (<img src="Ph1-15-LendBook-SD-WP_4A_15_Lend_Book.svg" alt="WP_4A_15_Lend_Book">)

### 3.2. Class Diagram (CD)
## 4. Tests
## 5. Observations
