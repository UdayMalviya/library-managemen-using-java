package librarymanagement;

import librarymanagement.service.LibraryService;
import librarymanagement.storage.FileStorage;
import librarymanagement.ui.LibraryConsoleApp;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            // Keep storage relative to the project so the data files are easy to inspect and back up.
            FileStorage storage = new FileStorage(Paths.get("data"));
            LibraryService service = new LibraryService(storage);
            LibraryConsoleApp app = new LibraryConsoleApp(service, scanner);
            app.run();
        } catch (IOException ex) {
            System.err.println("Failed to start the library management system: " + ex.getMessage());
        } finally {
            scanner.close();
        }
    }
}
