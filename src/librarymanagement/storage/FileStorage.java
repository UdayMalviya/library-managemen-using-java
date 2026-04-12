package librarymanagement.storage;

import librarymanagement.model.Book;
import librarymanagement.model.LoanRecord;
import librarymanagement.model.Member;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FileStorage {
    private final Path dataDirectory;
    private final Path booksFile;
    private final Path membersFile;
    private final Path loansFile;

    public FileStorage(Path dataDirectory) {
        if (dataDirectory == null) {
            throw new IllegalArgumentException("Data directory is required.");
        }

        this.dataDirectory = dataDirectory;
        this.booksFile = dataDirectory.resolve("books.db");
        this.membersFile = dataDirectory.resolve("members.db");
        this.loansFile = dataDirectory.resolve("loans.db");
    }

    public LoadedData load() throws IOException {
        ensureDirectoryExists();
        return new LoadedData(readBooks(), readMembers(), readLoans());
    }

    public void save(Collection<Book> books, Collection<Member> members, Collection<LoanRecord> loans) throws IOException {
        ensureDirectoryExists();
        writeBooks(books);
        writeMembers(members);
        writeLoans(loans);
    }

    private void ensureDirectoryExists() throws IOException {
        if (!Files.exists(dataDirectory)) {
            Files.createDirectories(dataDirectory);
        }
    }

    private List<Book> readBooks() throws IOException {
        List<Book> books = new ArrayList<Book>();
        if (!Files.exists(booksFile)) {
            return books;
        }

        List<String> lines = Files.readAllLines(booksFile, StandardCharsets.UTF_8);
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }

            String[] parts = line.split("\\|", -1);
            if (parts.length != 6) {
                throw new IOException("Invalid book record: " + line);
            }

            books.add(new Book(
                    parts[0],
                    decode(parts[1]),
                    decode(parts[2]),
                    decode(parts[3]),
                    parseInt(parts[4], "total copies"),
                    parseInt(parts[5], "available copies")
            ));
        }
        return books;
    }

    private List<Member> readMembers() throws IOException {
        List<Member> members = new ArrayList<Member>();
        if (!Files.exists(membersFile)) {
            return members;
        }

        List<String> lines = Files.readAllLines(membersFile, StandardCharsets.UTF_8);
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }

            String[] parts = line.split("\\|", -1);
            if (parts.length != 4) {
                throw new IOException("Invalid member record: " + line);
            }

            members.add(new Member(
                    parts[0],
                    decode(parts[1]),
                    decode(parts[2]),
                    parseDate(parts[3], "registered date")
            ));
        }
        return members;
    }

    private List<LoanRecord> readLoans() throws IOException {
        List<LoanRecord> loans = new ArrayList<LoanRecord>();
        if (!Files.exists(loansFile)) {
            return loans;
        }

        List<String> lines = Files.readAllLines(loansFile, StandardCharsets.UTF_8);
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }

            String[] parts = line.split("\\|", -1);
            if (parts.length != 8) {
                throw new IOException("Invalid loan record: " + line);
            }

            loans.add(new LoanRecord(
                    parts[0],
                    parts[1],
                    parts[2],
                    parseDate(parts[3], "issue date"),
                    parseDate(parts[4], "due date"),
                    parseOptionalDate(parts[5]),
                    parseDouble(parts[6], "daily fine rate"),
                    parseDouble(parts[7], "fine paid")
            ));
        }
        return loans;
    }

    private void writeBooks(Collection<Book> books) throws IOException {
        List<String> lines = new ArrayList<String>();
        for (Book book : books) {
            lines.add(String.join("|",
                    book.getId(),
                    encode(book.getTitle()),
                    encode(book.getAuthor()),
                    encode(book.getCategory()),
                    Integer.toString(book.getTotalCopies()),
                    Integer.toString(book.getAvailableCopies())
            ));
        }
        writeLines(booksFile, lines);
    }

    private void writeMembers(Collection<Member> members) throws IOException {
        List<String> lines = new ArrayList<String>();
        for (Member member : members) {
            lines.add(String.join("|",
                    member.getId(),
                    encode(member.getName()),
                    encode(member.getEmail()),
                    member.getRegisteredOn().toString()
            ));
        }
        writeLines(membersFile, lines);
    }

    private void writeLoans(Collection<LoanRecord> loans) throws IOException {
        List<String> lines = new ArrayList<String>();
        for (LoanRecord loan : loans) {
            lines.add(String.join("|",
                    loan.getId(),
                    loan.getBookId(),
                    loan.getMemberId(),
                    loan.getIssueDate().toString(),
                    loan.getDueDate().toString(),
                    loan.getReturnDate() == null ? "" : loan.getReturnDate().toString(),
                    Double.toString(loan.getDailyFineRate()),
                    Double.toString(loan.getFinePaid())
            ));
        }
        writeLines(loansFile, lines);
    }

    private void writeLines(Path path, List<String> lines) throws IOException {
        // Rewrite the whole file each time to keep the storage format simple and consistent.
        Files.write(path, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private int parseInt(String value, String fieldName) throws IOException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IOException("Invalid " + fieldName + ": " + value, ex);
        }
    }

    private double parseDouble(String value, String fieldName) throws IOException {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            throw new IOException("Invalid " + fieldName + ": " + value, ex);
        }
    }

    private LocalDate parseDate(String value, String fieldName) throws IOException {
        if (value == null || value.trim().isEmpty()) {
            throw new IOException("Missing " + fieldName + ".");
        }

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new IOException("Invalid " + fieldName + ": " + value, ex);
        }
    }

    private LocalDate parseOptionalDate(String value) throws IOException {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return parseDate(value, "return date");
    }

    private String encode(String value) {
        try {
            // Encode text fields so separators like "|" do not corrupt the flat-file format.
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException("UTF-8 encoding is not available.", ex);
        }
    }

    private String decode(String value) {
        try {
            return URLDecoder.decode(value == null ? "" : value, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException("UTF-8 encoding is not available.", ex);
        }
    }

    public static class LoadedData {
        private final List<Book> books;
        private final List<Member> members;
        private final List<LoanRecord> loans;

        public LoadedData(List<Book> books, List<Member> members, List<LoanRecord> loans) {
            this.books = books;
            this.members = members;
            this.loans = loans;
        }

        public List<Book> getBooks() {
            return books;
        }

        public List<Member> getMembers() {
            return members;
        }

        public List<LoanRecord> getLoans() {
            return loans;
        }
    }
}
