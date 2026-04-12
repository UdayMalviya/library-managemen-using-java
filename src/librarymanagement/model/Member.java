package librarymanagement.model;

import java.time.LocalDate;

public class Member {
    private final String id;
    private final String name;
    private final String email;
    private final LocalDate registeredOn;

    public Member(String id, String name, String email, LocalDate registeredOn) {
        this.id = requireText(id, "Member id");
        this.name = requireText(name, "Member name");
        this.email = requireText(email, "Email");

        if (registeredOn == null) {
            throw new IllegalArgumentException("Registration date is required.");
        }

        this.registeredOn = registeredOn;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public LocalDate getRegisteredOn() {
        return registeredOn;
    }

    public boolean matchesKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }

        // Member lookup in the CLI accepts id, name, or email fragments.
        String normalized = keyword.trim().toLowerCase();
        return id.toLowerCase().contains(normalized)
                || name.toLowerCase().contains(normalized)
                || email.toLowerCase().contains(normalized);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }
}
