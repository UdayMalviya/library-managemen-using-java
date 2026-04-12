# Library Management System

A command-line library management system written in Java with local file-based persistence.

This project is designed for small-scale usage and learning purposes. It lets you manage books, members, book issue and return operations, and overdue fine payments without using a database or any external libraries.

## Overview

The application runs as a text-based menu in the terminal. All records are stored in plain files under the `data/` folder, so information remains available after the program closes.

The system currently supports:

- book management
- member management
- loan tracking
- overdue fine calculation
- fine payment recording
- file persistence

## Features

### Book Management

- Add a new book
- List all books
- Search books by id, title, author, or category
- Remove a book if no copies are currently issued

### Member Management

- Register a new member
- List all members
- Search members by id, name, or email
- Remove a member if they have no active loans and no unpaid fine

### Loan Management

- Issue a book to a member
- Return an issued book
- View all active loans
- View overdue loans

### Fine Management

- Calculate overdue fines automatically based on due date and daily fine rate
- View all loans with outstanding fines
- Record fine payments
- Stop fine growth once a book is returned

### Persistence

- Saves all records in flat files inside `data/`
- Loads previously saved data automatically on startup
- Uses a simple text format that is easy to inspect manually

## Technology

- Language: Java
- Java compatibility target: Java 8 style code
- Build approach: direct `javac` compilation, no Maven or Gradle required
- Interface: terminal / CLI
- Storage: plain text files

## Requirements

To run the project from source, you need:

- a JDK installed
- `javac` available on your system `PATH`
- Java runtime available through `java`
- PowerShell if you want to use `run.ps1`

Important:

- A JRE alone is not enough to compile the project.
- The provided `run.ps1` script checks for `javac` before running.

## Project Structure

```text
library-management/
|-- data/
|   |-- books.db
|   |-- members.db
|   `-- loans.db
|-- out/
|   `-- compiled .class files
|-- src/
|   `-- librarymanagement/
|       |-- Main.java
|       |-- model/
|       |   |-- Book.java
|       |   |-- LoanRecord.java
|       |   `-- Member.java
|       |-- service/
|       |   `-- LibraryService.java
|       |-- storage/
|       |   `-- FileStorage.java
|       `-- ui/
|           `-- LibraryConsoleApp.java
|-- run.ps1
`-- README.md
```

## Main Components

### `Main.java`

Application entry point. It:

- creates the file storage handler
- loads the library service
- starts the CLI

### `model/`

Contains the domain classes:

- `Book`: stores book information and available copy count
- `Member`: stores member information
- `LoanRecord`: stores issue date, due date, return date, fine rate, and fine payment data

### `service/LibraryService.java`

Contains the main business logic:

- adding and removing books
- registering and removing members
- issuing and returning books
- generating ids
- validating operations
- calculating fine-related views
- saving changes after each successful operation

### `storage/FileStorage.java`

Handles reading and writing records to:

- `data/books.db`
- `data/members.db`
- `data/loans.db`

### `ui/LibraryConsoleApp.java`

Contains the menu-driven CLI and all user prompts.

## How to Run

### Option 1: Run with PowerShell Script

From the project root:

```powershell
.\run.ps1
```

What this script does:

1. checks whether `javac` is available
2. creates the `out/` directory if needed
3. compiles all `.java` files into `out/`
4. starts the application

### Option 2: Compile and Run Manually

From the project root:

```powershell
New-Item -ItemType Directory -Force -Path .\out | Out-Null
$sources = Get-ChildItem -Path .\src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -d .\out $sources
java -cp .\out librarymanagement.Main
```

## Menu Options

When the application starts, it displays this menu:

1. Add book
2. List books
3. Search books
4. Remove book
5. Register member
6. List members
7. Search members
8. Remove member
9. Issue book
10. Return book
11. View active loans
12. View overdue loans
13. View outstanding fines
14. Pay fine
15. Exit

## Default Values

The application uses the following defaults when issuing a book:

- Default loan period: `14` days
- Default daily fine rate: `2.00`

When prompted during issue:

- pressing Enter for loan days uses `14`
- pressing Enter for daily fine rate uses `2.00`

## ID Format

The system automatically creates ids in the following format:

- Books: `BOOK-0001`
- Members: `MEM-0001`
- Loans: `LOAN-0001`

The next number is derived from the highest existing saved id.

## Business Rules

The system enforces these rules:

- a book must have at least 1 total copy
- available copies cannot be negative or greater than total copies
- a member email must be unique
- a book cannot be issued if no copies are available
- the same member cannot have another active loan for the same book
- a book cannot be removed while any copy is issued
- a member cannot be removed if they have an active loan
- a member cannot be removed if they have any unpaid fine
- a due date cannot be before the issue date
- a return date cannot be before the issue date
- a fine payment cannot be greater than the outstanding fine
- overdue fine stops increasing after a return is recorded

## Fine Calculation

Fine calculation is based on:

- due date
- current date or return date
- daily fine rate
- amount already paid

The formula is:

```text
overdue days = days between due date and evaluation date
accrued fine = overdue days * daily fine rate
outstanding fine = accrued fine - fine paid
```

Evaluation date means:

- today, if the book has not yet been returned
- return date, if the book has already been returned

## Data Storage

All persistent data is stored under the `data/` directory.

### `books.db`

Each line format:

```text
bookId|title|author|category|totalCopies|availableCopies
```

Example:

```text
BOOK-0001|dsa+with+cpp|uday|dsa|3|3
```

### `members.db`

Each line format:

```text
memberId|name|email|registeredOn
```

Example:

```text
MEM-0001|John+Doe|john%40mail.com|2026-04-12
```

### `loans.db`

Each line format:

```text
loanId|bookId|memberId|issueDate|dueDate|returnDate|dailyFineRate|finePaid
```

Example:

```text
LOAN-0001|BOOK-0001|MEM-0001|2026-04-12|2026-04-26||2.0|0.0
```

Notes:

- text values are URL-encoded before saving
- this prevents the `|` separator from breaking the file structure
- blank `returnDate` means the book has not been returned yet

## Sample Workflow

Example usage flow:

1. Start the application.
2. Add one or more books.
3. Register one or more members.
4. Issue a book using the book id and member id.
5. View active loans.
6. Return the book later.
7. If overdue, view outstanding fines.
8. Record a fine payment.
9. Exit the program.
10. Start it again and confirm the data is still available.

## Error Handling

The CLI catches and prints readable messages for common failures, including:

- invalid number input
- missing required values
- missing book, member, or loan ids
- duplicate member email
- issuing unavailable books
- overpaying fines
- invalid or corrupted storage records

## Limitations

Current limitations of this version:

- no update/edit operation for books or members
- no multi-user support
- no authentication or roles
- no database backend
- no export or report generation
- no automated tests included in the project
- file writes replace the full file content rather than updating individual rows

## Troubleshooting

### `javac was not found`

Install a JDK and ensure `javac` is on your `PATH`.

### Data does not persist

Check that:

- the application is being run from the project root
- the `data/` folder is writable
- no storage file is malformed

### Application fails on startup with storage error

One of the `.db` files may contain invalid data. Verify that each line matches the expected format described above.

## Future Improvement Ideas

Possible next steps for this project:

- add update operations for books and members
- support multiple copies across branch libraries
- add search filters and sorting options
- add issue history per member
- add fine reports by member
- add unit tests
- switch storage to a database such as SQLite or MySQL

## Authoring Notes

This project is intentionally kept simple:

- no external dependencies
- easy-to-read Java classes
- easy-to-inspect storage files
- suitable for learning object-oriented design, file handling, and CLI application structure
