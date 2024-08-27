package com.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.nio.charset.StandardCharsets;

public class WebScraper {

    // Sample JLPT vocab datasets for every level
    private static final Set<String> N5_VOCAB = new HashSet<>();
    private static final Set<String> N4_VOCAB = new HashSet<>();
    private static final Set<String> N3_VOCAB = new HashSet<>();
    private static final Set<String> N2_VOCAB = new HashSet<>();
    private static final Set<String> N1_VOCAB = new HashSet<>();

    public static void main(String[] args) {
        // Load vocab from .txt files
        loadVocabulary("src/main/java/com/example/N5.txt", N5_VOCAB);
        loadVocabulary("src/main/java/com/example/N4.txt", N4_VOCAB);
        loadVocabulary("src/main/java/com/example/N3.txt", N3_VOCAB);
        loadVocabulary("src/main/java/com/example/N2.txt", N2_VOCAB);
        loadVocabulary("src/main/java/com/example/N1.txt", N1_VOCAB);

        // Create map to categorize books by JLPT level
        Map<String, List<String>> booksByJLPT = new HashMap<>();
        booksByJLPT.put("N5", new ArrayList<>());
        booksByJLPT.put("N4", new ArrayList<>());
        booksByJLPT.put("N3", new ArrayList<>());
        booksByJLPT.put("N2", new ArrayList<>());
        booksByJLPT.put("N1", new ArrayList<>());

        // URL of the Honto.jp search results page
        String searchResultsUrl = "https://honto.jp/ebook/search_0750_0229001000000_09-salesnum.html?slm=5&tbty=2&unt=0&cid=ip_eb_alpk_new_04";

        // Ensure URL is not empty
        if (searchResultsUrl == null || searchResultsUrl.isEmpty()) {
            System.out.println("The 'url' parameter must not be empty.");
            return; // Exit if the URL is invalid
        }

        try {
            // Fetch and parse the search results page
            Document doc = Jsoup.connect(searchResultsUrl).get();
            doc.outputSettings().charset("UTF-8");

            // Select the book item elements
            Elements bookElements = doc.select(".stProduct02");

            for (Element bookElement : bookElements) {
                // Extract URL of the book's details page
                String bookUrl = bookElement.select("a.dyTitle").attr("href");

                // Check if the URL is not empty
                if (bookUrl == null || bookUrl.isEmpty()) {
                    System.out.println("Book URL is empty. Skipping this entry.");
                    continue; // Skip this iteration if the book URL is empty
                }

                // Prepend the base URL if necessary
                if (!bookUrl.startsWith("http")) {
                    bookUrl = "https://honto.jp" + bookUrl;
                }

                try {
                    // Navigate to the book's detail page
                    Document bookDoc = Jsoup.connect(bookUrl).get();

                    // Extract the title and description from the detail page
                    String title = bookDoc.select("h1.stTitle").text(); // Update with correct selector
                    String description = bookDoc.select("p.stText").text(); // Update with correct selector

                    // Debugging print statements
                    System.out.println("Fetched Book URL: " + bookUrl);
                    System.out.println("Fetched Title: " + title);
                    System.out.println("Fetched Description: " + description);

                    // Determine the JLPT level based on the description
                    String jlptLevel = determineJLPTLevel(description);

                    if (jlptLevel != null) {
                        booksByJLPT.get(jlptLevel).add(title);
                    }

                    // Add delay between requests
                    Thread.sleep(2000);

                } catch (IOException e) {
                    System.out.println("Error fetching the book detail page: " + e.getMessage());
                } catch (InterruptedException e) {
                    System.out.println("Error with thread sleep: " + e.getMessage());
                    Thread.currentThread().interrupt(); // Restore interrupted status
                }
            }
        } catch (IOException e) {
            System.out.println("Error fetching the website: " + e.getMessage());
        }

        // Create a JSON object from the map
        JSONObject jsonObject = new JSONObject(booksByJLPT);

        try (FileWriter file = new FileWriter("books_by_jlpt.json", StandardCharsets.UTF_8)) {
            file.write(jsonObject.toString(4));
            System.out.println("Successfully written to books_by_jlpt.json");
        } catch (IOException e) {
            System.out.println("Error writing to JSON file: " + e.getMessage());
        }
    }

    // Load vocabulary from file into the provided set
    private static void loadVocabulary(String fileName, Set<String> vocabulary) {
        try {
            // Use absolute path if the files are in a specific directory
            Path filePath = Paths.get(fileName); // Update if necessary
            if (!Files.exists(filePath)) {
                System.out.println("File not found: " + fileName);
                return;
            }

            // Read all lines from the file
            List<String> lines = Files.readAllLines(filePath);
            vocabulary.addAll(lines);
        } catch (IOException e) {
            System.out.println("Error reading vocabulary file: " + fileName + " - " + e.getMessage());
        }
    }

    // Function to determine JLPT level based on description
    private static String determineJLPTLevel(String description) {
        int n5score = countMatchingWords(description, N5_VOCAB);
        int n4score = countMatchingWords(description, N4_VOCAB);
        int n3score = countMatchingWords(description, N3_VOCAB);
        int n2score = countMatchingWords(description, N2_VOCAB);
        int n1score = countMatchingWords(description, N1_VOCAB);

        //HashMap initialization to determine score
        Map<String, Integer> scores = new HashMap<>();
        scores.put("N5", n5score);
        scores.put("N4", n4score);
        scores.put("N3", n3score);
        scores.put("N2", n2score);
        scores.put("N1", n1score);

        return scores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .filter(entry -> entry.getValue() > 0) // Ensure there's at least one match
            .map(Map.Entry::getKey)
            .orElse(null); // Return the highest scoring level or null if no matches
    }

    // Helper function to count matching words from a set
    private static int countMatchingWords(String text, Set<String> vocabulary) {
        int count = 0;
        for (String word : vocabulary) {
            if (text.contains(word)) {
                count++;
            }
        }
        return count;
    }
}
