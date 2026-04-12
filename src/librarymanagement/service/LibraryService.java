package librarymanagement.service;

import librarymanagement.model.Book;
import librarymanagement.model.LoanRecord;
import librarymanagement.model.Member;
import librarymanagement.storage.FileStorage;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LibraryService {
    public static final int DEFAULT_LOAN_DAYS = 14;
    public static final double DEFAULT_DAILY_FINE_RATE = 2.00;

    private final FileStorage storage;
    // LinkedHashMap keeps listing order predictable in the CLI and in saved output.
    private final Map<String, Book> books = new LinkedHashMap<String, Book>();
    private final Map<String, Member> members = new LinkedHashMap<String, Member>();
    private final Map<String, LoanRecord> loans = new LinkedHashMap<String, LoanRecord>();

    private int nextBookNumber;
    private int nextMemberNumber;
    private int nextLoanNumber;

    public LibraryService(FileStorage storage) throws IOException {
        if (storage == null) {
            throw new IllegalArgumentException("Storage is required.");
        }

        this.storage = storage;
        loadData();
    }

    public Book addBook(String title, String author, String category, int totalCopies) throws IOException {
        Book book = new Book(nextId("BOOK", nextBookNumber++), title, author, category, totalCopies, totalCopies);
        books.put(book.getId(), book);
        saveChanges();
        return book;
    }

    public List<Book> getAllBooks() {
        return new ArrayList<Book>(books.values());
    }

    public List<Book> searchBooks(String keyword) {
        List<Book> matches = new ArrayList<Book>();
        for (Book book : books.values()) {
            if (book.matchesKeyword(keyword)) {
                matches.add(book);
            }
        }
        return matches;
    }

    public void removeBook(String bookId) throws IOException {
        String normalizedId = normalizeId(bookId);
        Book book = getRequiredBook(normalizedId);

        if (book.getAvailableCopies() != book.getTotalCopies()) {
            throw new IllegalStateException("This book cannot be removed because copies are currently issued.");
        }

        for (LoanRecord loan : loans.values()) {
            if (loan.getBookId().equals(normalizedId) && loan.isActive()) {
                throw new IllegalStateException("This book has an active loan and cannot be removed.");
            }
        }

        books.remove(normalizedId);
        saveChanges();
    }

    public Member registerMember(String name, String email) throws IOException {
        String normalizedEmail = normalizeEmail(email);
        ensureUniqueEmail(normalizedEmail, null);

        Member member = new Member(nextId("MEM", nextMemberNumber++), name, normalizedEmail, LocalDate.now());
        members.put(member.getId(), member);
        saveChanges();
        return member;
    }

    public List<Member> getAllMembers() {
        return new ArrayList<Member>(members.values());
    }

    public List<Member> searchMembers(String keyword) {
        List<Member> matches = new ArrayList<Member>();
        for (Member member : members.values()) {
            if (member.matchesKeyword(keyword)) {
                matches.add(member);
            }
        }
        return matches;
    }

    public void removeMember(String memberId) throws IOException {
        String normalizedId = normalizeId(memberId);
        Member member = getRequiredMember(normalizedId);

        for (LoanRecord loan : loans.values()) {
            if (loan.getMemberId().equals(normalizedId) && loan.isActive()) {
                throw new IllegalStateException("This member cannot be removed because they still have active loans.");
            }
            if (loan.getMemberId().equals(normalizedId) && loan.hasOutstandingFine(LocalDate.now())) {
                throw new IllegalStateException("This member cannot be removed because they have unpaid fines.");
            }
        }

        members.remove(member.getId());
        saveChanges();
    }

    public LoanRecord issueBook(String bookId, String memberId, int loanDays, double dailyFineRate) throws IOException {
        if (loanDays <= 0) {
            throw new IllegalArgumentException("Loan days must be greater than zero.");
        }
        if (dailyFineRate < 0) {
            throw new IllegalArgumentException("Daily fine rate cannot be negative.");
        }

        Book book = getRequiredBook(normalizeId(bookId));
        Member member = getRequiredMember(normalizeId(memberId));

        if (!book.isAvailable()) {
            throw new IllegalStateException("No copies are available for this book.");
        }

        for (LoanRecord loan : loans.values()) {
            if (loan.isActive()
                    && loan.getBookId().equals(book.getId())
                    && loan.getMemberId().equals(member.getId())) {
                throw new IllegalStateException("This member already has an active loan for the selected book.");
            }
        }

        LocalDate issueDate = LocalDate.now();
        // Fine configuration is copied into the loan so future rule changes do not rewrite old loans.
        LoanRecord loan = new LoanRecord(
                nextId("LOAN", nextLoanNumber++),
                book.getId(),
                member.getId(),
                issueDate,
                issueDate.plusDays(loanDays),
                null,
                dailyFineRate,
                0.0
        );

        book.issueCopy();
        loans.put(loan.getId(), loan);
        saveChanges();
        return loan;
    }

    public LoanRecord returnBook(String loanId) throws IOException {
        LoanRecord loan = getRequiredLoan(normalizeId(loanId));
        if (loan.isReturned()) {
            throw new IllegalStateException("This loan has already been returned.");
        }

        Book book = getRequiredBook(loan.getBookId());
        loan.markReturned(LocalDate.now());
        book.returnCopy();
        saveChanges();
        return loan;
    }

    public LoanRecord payFine(String loanId, double amount) throws IOException {
        LoanRecord loan = getRequiredLoan(normalizeId(loanId));
        loan.payFine(amount, LocalDate.now());
        saveChanges();
        return loan;
    }

    public List<LoanRecord> getActiveLoans() {
        List<LoanRecord> activeLoans = new ArrayList<LoanRecord>();
        for (LoanRecord loan : loans.values()) {
            if (loan.isActive()) {
                activeLoans.add(loan);
            }
        }
        sortLoansByDueDate(activeLoans);
        return activeLoans;
    }

    public List<LoanRecord> getOverdueLoans() {
        LocalDate today = LocalDate.now();
        List<LoanRecord> overdueLoans = new ArrayList<LoanRecord>();
        for (LoanRecord loan : loans.values()) {
            if (loan.isActive() && loan.isOverdue(today)) {
                overdueLoans.add(loan);
            }
        }
        sortLoansByDueDate(overdueLoans);
        return overdueLoans;
    }

    public List<LoanRecord> getLoansWithOutstandingFines() {
        LocalDate today = LocalDate.now();
        List<LoanRecord> fineLoans = new ArrayList<LoanRecord>();
        for (LoanRecord loan : loans.values()) {
            if (loan.hasOutstandingFine(today)) {
                fineLoans.add(loan);
            }
        }
        sortLoansByDueDate(fineLoans);
        return fineLoans;
    }

    public Book findBookById(String bookId) {
        return books.get(normalizeId(bookId));
    }

    public Member findMemberById(String memberId) {
        return members.get(normalizeId(memberId));
    }

    public LoanRecord findLoanById(String loanId) {
        return loans.get(normalizeId(loanId));
    }

    public int getBookCount() {
        return books.size();
    }

    public int getMemberCount() {
        return members.size();
    }

    public int getActiveLoanCount() {
        return getActiveLoans().size();
    }

    public int getOutstandingFineCount() {
        return getLoansWithOutstandingFines().size();
    }

    private void loadData() throws IOException {
        FileStorage.LoadedData loadedData = storage.load();

        loadBooks(loadedData.getBooks());
        loadMembers(loadedData.getMembers());
        loadLoans(loadedData.getLoans());

        nextBookNumber = findNextNumber(books.keySet(), "BOOK-");
        nextMemberNumber = findNextNumber(members.keySet(), "MEM-");
        nextLoanNumber = findNextNumber(loans.keySet(), "LOAN-");
    }

    private void loadBooks(List<Book> loadedBooks) {
        for (Book book : loadedBooks) {
            if (books.containsKey(book.getId())) {
                throw new IllegalStateException("Duplicate book id found in data: " + book.getId());
            }
            books.put(book.getId(), book);
        }
    }

    private void loadMembers(List<Member> loadedMembers) {
        for (Member member : loadedMembers) {
            if (members.containsKey(member.getId())) {
                throw new IllegalStateException("Duplicate member id found in data: " + member.getId());
            }
            ensureUniqueEmail(normalizeEmail(member.getEmail()), member.getId());
            members.put(member.getId(), member);
        }
    }

    private void loadLoans(List<LoanRecord> loadedLoans) {
        for (LoanRecord loan : loadedLoans) {
            if (loans.containsKey(loan.getId())) {
                throw new IllegalStateException("Duplicate loan id found in data: " + loan.getId());
            }
            loans.put(loan.getId(), loan);
        }
    }

    private void ensureUniqueEmail(String email, String ignoredMemberId) {
        for (Member member : members.values()) {
            boolean sameRecord = ignoredMemberId != null && member.getId().equals(ignoredMemberId);
            if (!sameRecord && normalizeEmail(member.getEmail()).equals(email)) {
                throw new IllegalArgumentException("A member with this email already exists.");
            }
        }
    }

    private void saveChanges() throws IOException {
        storage.save(books.values(), members.values(), loans.values());
    }

    private Book getRequiredBook(String bookId) {
        Book book = books.get(bookId);
        if (book == null) {
            throw new IllegalArgumentException("Book not found: " + bookId);
        }
        return book;
    }

    private Member getRequiredMember(String memberId) {
        Member member = members.get(memberId);
        if (member == null) {
            throw new IllegalArgumentException("Member not found: " + memberId);
        }
        return member;
    }

    private LoanRecord getRequiredLoan(String loanId) {
        LoanRecord loan = loans.get(loanId);
        if (loan == null) {
            throw new IllegalArgumentException("Loan not found: " + loanId);
        }
        return loan;
    }

    private int findNextNumber(Collection<String> ids, String prefix) {
        int max = 0;
        for (String id : ids) {
            if (id != null && id.startsWith(prefix)) {
                try {
                    int value = Integer.parseInt(id.substring(prefix.length()));
                    max = Math.max(max, value);
                } catch (NumberFormatException ignored) {
                    // Ignore malformed ids and continue with the highest valid value found.
                }
            }
        }
        return max + 1;
    }

    private void sortLoansByDueDate(List<LoanRecord> loanRecords) {
        Collections.sort(loanRecords, new Comparator<LoanRecord>() {
            @Override
            public int compare(LoanRecord first, LoanRecord second) {
                return first.getDueDate().compareTo(second.getDueDate());
            }
        });
    }

    private String nextId(String prefix, int nextNumber) {
        return prefix + "-" + String.format("%04d", nextNumber);
    }

    private String normalizeId(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Id is required.");
        }
        String normalized = id.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Id is required.");
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
