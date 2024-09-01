package com.example;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class BookController {

    @GetMapping("/api/books")
        public ResponseEntity<String> getBooks() {
            try{
                //point to file location
                String jsonPath =  "C:/VSCProjects/ReactProjects/shiruscraper/shiruscraper/books_by_jlpt.json";
                String content = new String(Files.readAllBytes(Paths.get(jsonPath)), StandardCharsets.UTF_8);
                return ResponseEntity.ok(content);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error reading the JSON file: " + e.getMessage());
        }
    }
}