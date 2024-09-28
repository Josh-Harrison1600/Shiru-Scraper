package com.example;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.net.URISyntaxException;
import java.nio.file.Path;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.io.ClassPathResource;

@RestController
@CrossOrigin(origins = "*")  // Allow all origins temporarily for testing purposes
public class BookController {

    // Utility function to load JSON from resources
    private String loadJSONFromResource(String resourcePath) throws IOException, URISyntaxException {
        Path path = Paths.get(new ClassPathResource(resourcePath).getURI());
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    @GetMapping("/api/books")
    public ResponseEntity<String> getBooks() {
        try {
            // Use the classpath to access the JSON file inside resources directory
            String content = loadJSONFromResource("books_by_jlpt.json");
            return ResponseEntity.ok(content);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error reading the JSON file: " + e.getMessage());
        }
    }

    @GetMapping("/api/ani-books")
    public ResponseEntity<String> getAniBooks() {
        try {
            // Use the classpath to access the JSON file inside resources directory
            String content = loadJSONFromResource("Ani_Online_Books.json");
            return ResponseEntity.ok(content);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error reading the JSON file: " + e.getMessage());
        }
    }

    @GetMapping("/api/ehonnavi-books")
    public ResponseEntity<String> getEhonnaviBooks() {
        try {
            // Use the classpath to access the JSON file inside resources directory
            String content = loadJSONFromResource("N5Books.json");
            return ResponseEntity.ok(content);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error reading the JSON file: " + e.getMessage());
        }
    }
}
