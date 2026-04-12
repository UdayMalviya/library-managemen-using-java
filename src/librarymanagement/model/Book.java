package librarymanagement.model;

public class Book {
    private final String id;
    private final String title;
    private final String author;
    private final String category;
    private final int totalCopies;
    // Available copies change as books are issued and returned, while totalCopies stays fixed.
    private int availableCopies;

    public Book(String id, String title, String author, String category, int totalCopies, int availableCopies) {
        this.id = requireText(id, "Book id");
        this.title = requireText(title, "Title");
        this.author = requireText(author, "Author");
        this.category = requireText(category, "Category");

        if (totalCopies <= 0) {
            throw new IllegalArgumentException("Total copies must be greater than zero.");
        }
        if (availableCopies < 0 || availableCopies > totalCopies) {
            throw new IllegalArgumentException("Available copies must stay between 0 and total copies.");
        }

        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getCategory() {
        return category;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    public boolean isAvailable() {
        return availableCopies > 0;
    }

    public void issueCopy() {
        if (!isAvailable()) {
            throw new IllegalStateException("No copies are currently available for this book.");
        }
        availableCopies--;
    }

    public void returnCopy() {
        if (availableCopies >= totalCopies) {
            throw new IllegalStateException("All copies are already marked as available.");
        }
        availableCopies++;
    }

    public boolean matchesKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }

        String normalized = keyword.trim().toLowerCase();
        return id.toLowerCase().contains(normalized)
                || title.toLowerCase().contains(normalized)
                || author.toLowerCase().contains(normalized)
                || category.toLowerCase().contains(normalized);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }
}
