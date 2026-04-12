package librarymanagement.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class LoanRecord {
    private final String id;
    private final String bookId;
    private final String memberId;
    private final LocalDate issueDate;
    private final LocalDate dueDate;
    private final double dailyFineRate;
    private LocalDate returnDate;
    private double finePaid;

    public LoanRecord(
            String id,
            String bookId,
            String memberId,
            LocalDate issueDate,
            LocalDate dueDate,
            LocalDate returnDate,
            double dailyFineRate,
            double finePaid
    ) {
        this.id = requireText(id, "Loan id");
        this.bookId = requireText(bookId, "Book id");
        this.memberId = requireText(memberId, "Member id");

        if (issueDate == null) {
            throw new IllegalArgumentException("Issue date is required.");
        }
        if (dueDate == null) {
            throw new IllegalArgumentException("Due date is required.");
        }
        if (dueDate.isBefore(issueDate)) {
            throw new IllegalArgumentException("Due date cannot be before issue date.");
        }
        if (returnDate != null && returnDate.isBefore(issueDate)) {
            throw new IllegalArgumentException("Return date cannot be before issue date.");
        }
        if (dailyFineRate < 0) {
            throw new IllegalArgumentException("Daily fine rate cannot be negative.");
        }
        if (finePaid < 0) {
            throw new IllegalArgumentException("Fine paid cannot be negative.");
        }

        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.dailyFineRate = roundCurrency(dailyFineRate);
        this.finePaid = roundCurrency(finePaid);
    }

    public String getId() {
        return id;
    }

    public String getBookId() {
        return bookId;
    }

    public String getMemberId() {
        return memberId;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public double getDailyFineRate() {
        return dailyFineRate;
    }

    public double getFinePaid() {
        return finePaid;
    }

    public boolean isReturned() {
        return returnDate != null;
    }

    public boolean isActive() {
        return !isReturned();
    }

    public boolean isOverdue(LocalDate asOfDate) {
        return getOverdueDays(asOfDate) > 0;
    }

    public long getOverdueDays(LocalDate asOfDate) {
        // Once a book is returned, overdue days stop increasing on the return date.
        LocalDate effectiveDate = getFineEvaluationDate(asOfDate);
        if (!effectiveDate.isAfter(dueDate)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(dueDate, effectiveDate);
    }

    public double getAccruedFine(LocalDate asOfDate) {
        return roundCurrency(getOverdueDays(asOfDate) * dailyFineRate);
    }

    public double getOutstandingFine(LocalDate asOfDate) {
        double outstanding = getAccruedFine(asOfDate) - finePaid;
        return roundCurrency(Math.max(0.0, outstanding));
    }

    public boolean hasOutstandingFine(LocalDate asOfDate) {
        return getOutstandingFine(asOfDate) > 0.0;
    }

    public void markReturned(LocalDate returnedOn) {
        if (returnedOn == null) {
            throw new IllegalArgumentException("Return date is required.");
        }
        if (returnedOn.isBefore(issueDate)) {
            throw new IllegalArgumentException("Return date cannot be before issue date.");
        }
        if (isReturned()) {
            throw new IllegalStateException("This loan has already been returned.");
        }
        this.returnDate = returnedOn;
    }

    public void payFine(double amount, LocalDate asOfDate) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Fine payment must be greater than zero.");
        }

        double outstanding = getOutstandingFine(asOfDate);
        if (amount > outstanding) {
            throw new IllegalArgumentException(String.format(
                    "Payment exceeds outstanding fine. Current outstanding fine is %.2f.",
                    outstanding
            ));
        }

        finePaid = roundCurrency(finePaid + amount);
    }

    private LocalDate getFineEvaluationDate(LocalDate asOfDate) {
        if (asOfDate == null) {
            throw new IllegalArgumentException("Evaluation date is required.");
        }
        if (returnDate != null && returnDate.isBefore(asOfDate)) {
            return returnDate;
        }
        return asOfDate;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }

    private static double roundCurrency(double value) {
        // Persist rounded money values so displayed totals match saved totals.
        return Math.round(value * 100.0) / 100.0;
    }
}
