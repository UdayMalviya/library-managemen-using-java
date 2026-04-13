package librarymanagement.ui;

import librarymanagement.model.Book;
import librarymanagement.model.LoanRecord;
import librarymanagement.model.Member;
import librarymanagement.service.LibraryService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

public class LibraryConsoleApp {
    private final LibraryService service;
    private final Scanner scanner;

    public LibraryConsoleApp(LibraryService service, Scanner scanner) {
        if (service == null) {
            throw new IllegalArgumentException("Library service is required.");
        }
        if (scanner == null) {
            throw new IllegalArgumentException("Scanner is required.");
        }

        this.service = service;
        this.scanner = scanner;
    }

    public void run() {
        boolean running = true;

        while (running) {
            printMenu();
            int choice = readInt("Choose an option: ");

            try {
                // Keep the full menu routing in one place so each command stays easy to trace.
                switch (choice) {
                    case 1 -> addBook();
                    case 2 -> listBooks(service.getAllBooks());
                    case 3 -> searchBooks();
                    case 4 -> removeBook();
                    case 5 -> registerMember();
                    case 6 -> listMembers(service.getAllMembers());
                    case 7 -> searchMembers();
                    case 8 -> removeMember();
                    case 9 -> issueBook();
                    case 10 -> returnBook();
                    case 11 -> listLoans(service.getActiveLoans(), "Active Loans");
                    case 12 -> listLoans(service.getOverdueLoans(), "Overdue Loans");
                    case 13 -> listLoans(service.getLoansWithOutstandingFines(), "Outstanding Fines");
                    case 14 -> payFine();
                    case 15 -> {
                        running = false;
                        System.out.println("Library system closed.");
                    }
                    default -> System.out.println("Please choose a menu option between 1 and 15.");
                }
            } catch (IllegalArgumentException ex) {
                System.out.println("Input error: " + ex.getMessage());
            } catch (IllegalStateException ex) {
                System.out.println("Action failed: " + ex.getMessage());
            } catch (IOException ex) {
                System.out.println("Storage error: " + ex.getMessage());
            }

            System.out.println();
        }
    }

    private void printMenu() {
        System.out.println("============================================================");
        System.out.println("                 Library Management System");
        System.out.println("============================================================");
        System.out.println("Books: " + service.getBookCount()
                + " | Members: " + service.getMemberCount()
                + " | Active Loans: " + service.getActiveLoanCount()
                + " | Fine Cases: " + service.getOutstandingFineCount());
        System.out.println();
        System.out.println("1.  Add book");
        System.out.println("2.  List books");
        System.out.println("3.  Search books");
        System.out.println("4.  Remove book");
        System.out.println("5.  Register member");
        System.out.println("6.  List members");
        System.out.println("7.  Search members");
        System.out.println("8.  Remove member");
        System.out.println("9.  Issue book");
        System.out.println("10. Return book");
        System.out.println("11. View active loans");
        System.out.println("12. View overdue loans");
        System.out.println("13. View outstanding fines");
        System.out.println("14. Pay fine");
        System.out.println("15. Exit");
        System.out.println("------------------------------------------------------------");
    }

    private void addBook() throws IOException {
        System.out.println("Add Book");
        String title = readRequiredText("Title: ");
        String author = readRequiredText("Author: ");
        String category = readRequiredText("Category: ");
        int totalCopies = readInt("Total copies: ");

        Book book = service.addBook(title, author, category, totalCopies);
        System.out.println("Book added successfully with ID " + book.getId() + ".");
    }

    private void searchBooks() {
        String keyword = readRequiredText("Search keyword: ");
        listBooks(service.searchBooks(keyword));
    }

    private void removeBook() throws IOException {
        String bookId = readRequiredText("Book ID to remove: ");
        service.removeBook(bookId);
        System.out.println("Book removed successfully.");
    }

    private void registerMember() throws IOException {
        System.out.println("Register Member");
        String name = readRequiredText("Name: ");
        String email = readRequiredText("Email: ");

        Member member = service.registerMember(name, email);
        System.out.println("Member registered successfully with ID " + member.getId() + ".");
    }

    private void searchMembers() {
        String keyword = readRequiredText("Search keyword: ");
        listMembers(service.searchMembers(keyword));
    }

    private void removeMember() throws IOException {
        String memberId = readRequiredText("Member ID to remove: ");
        service.removeMember(memberId);
        System.out.println("Member removed successfully.");
    }

    private void issueBook() throws IOException {
        System.out.println("Issue Book");
        String bookId = readRequiredText("Book ID: ");
        String memberId = readRequiredText("Member ID: ");
        int loanDays = readOptionalInt(
                "Loan period in days (default " + LibraryService.DEFAULT_LOAN_DAYS + "): ",
                LibraryService.DEFAULT_LOAN_DAYS
        );
        double fineRate = readOptionalDouble(
                "Daily fine rate (default " + formatMoney(LibraryService.DEFAULT_DAILY_FINE_RATE) + "): ",
                LibraryService.DEFAULT_DAILY_FINE_RATE
        );

        LoanRecord loan = service.issueBook(bookId, memberId, loanDays, fineRate);
        System.out.println("Book issued successfully with loan ID " + loan.getId() + ".");
        System.out.println("Due date: " + loan.getDueDate() + " | Daily fine rate: " + formatMoney(loan.getDailyFineRate()));
    }

    private void returnBook() throws IOException {
        String loanId = readRequiredText("Loan ID to return: ");
        LoanRecord loan = service.returnBook(loanId);

        System.out.println("Book returned successfully.");
        if (loan.hasOutstandingFine(LocalDate.now())) {
            System.out.println("Outstanding fine: " + formatMoney(loan.getOutstandingFine(LocalDate.now())));
        } else {
            System.out.println("No fine is outstanding for this loan.");
        }
    }

    private void payFine() throws IOException {
        String loanId = readRequiredText("Loan ID for fine payment: ");
        double amount = readDouble("Payment amount: ");
        LoanRecord loan = service.payFine(loanId, amount);
        System.out.println("Fine payment recorded. Remaining outstanding fine: "
                + formatMoney(loan.getOutstandingFine(LocalDate.now())));
    }

    private void listBooks(List<Book> books) {
        System.out.println("Books");
        if (books.isEmpty()) {
            System.out.println("No books found.");
            return;
        }

        for (Book book : books) {
            System.out.println(String.format(
                    "%s | %s | %s | %s | Available %d/%d",
                    book.getId(),
                    book.getTitle(),
                    book.getAuthor(),
                    book.getCategory(),
                    book.getAvailableCopies(),
                    book.getTotalCopies()
            ));
        }
    }

    private void listMembers(List<Member> members) {
        System.out.println("Members");
        if (members.isEmpty()) {
            System.out.println("No members found.");
            return;
        }

        for (Member member : members) {
            System.out.println(String.format(
                    "%s | %s | %s | Registered %s",
                    member.getId(),
                    member.getName(),
                    member.getEmail(),
                    member.getRegisteredOn()
            ));
        }
    }

    private void listLoans(List<LoanRecord> loans, String title) {
        System.out.println(title);
        if (loans.isEmpty()) {
            System.out.println("No records found.");
            return;
        }

        LocalDate today = LocalDate.now();
        for (LoanRecord loan : loans) {
            // Loans store ids only, so the display resolves readable labels when printing reports.
            Book book = service.findBookById(loan.getBookId());
            Member member = service.findMemberById(loan.getMemberId());

            String bookLabel = book == null ? loan.getBookId() : book.getTitle();
            String memberLabel = member == null ? loan.getMemberId() : member.getName();
            String status = loan.isReturned() ? "Returned on " + loan.getReturnDate() : "Active";

            System.out.println(String.format(
                    "%s | Book: %s | Member: %s | Due: %s | Status: %s | Fine Due: %s | Paid: %s | Outstanding: %s",
                    loan.getId(),
                    bookLabel,
                    memberLabel,
                    loan.getDueDate(),
                    status,
                    formatMoney(loan.getAccruedFine(today)),
                    formatMoney(loan.getFinePaid()),
                    formatMoney(loan.getOutstandingFine(today))
            ));
        }
    }

    private String readRequiredText(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine();
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
            System.out.println("A value is required.");
        }
    }

    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String raw = scanner.nextLine();
            try {
                return Integer.parseInt(raw.trim());
            } catch (NumberFormatException ex) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    private int readOptionalInt(String prompt, int defaultValue) {
        while (true) {
            System.out.print(prompt);
            String raw = scanner.nextLine();
            if (raw == null || raw.trim().isEmpty()) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(raw.trim());
            } catch (NumberFormatException ex) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    private double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String raw = scanner.nextLine();
            try {
                return Double.parseDouble(raw.trim());
            } catch (NumberFormatException ex) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private double readOptionalDouble(String prompt, double defaultValue) {
        while (true) {
            System.out.print(prompt);
            String raw = scanner.nextLine();
            if (raw == null || raw.trim().isEmpty()) {
                // Blank input lets the operator accept the suggested default quickly.
                return defaultValue;
            }
            try {
                return Double.parseDouble(raw.trim());
            } catch (NumberFormatException ex) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private String formatMoney(double amount) {
        return String.format("%.2f", amount);
    }
}
