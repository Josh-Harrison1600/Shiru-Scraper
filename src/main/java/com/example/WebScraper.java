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
import java.io.PrintWriter;
import java.util.regex.Pattern;


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
                    bookDoc.outputSettings().charset("UTF-8");

                    // Extract the title and description from the detail page
                    String title = bookDoc.select("h1.stTitle").text(); // Update with correct selector
                    String description = bookDoc.select("p.stText").text(); // Update with correct selector

                    //clean up the title in .json
                    title = cleanTitle(title);

                    // Debugging print statements
                    System.out.println("Fetched Book URL: " + bookUrl);
                    System.out.println("Fetched Title: " + title);
                    System.out.println("Fetched Description: " + description);                    

                    // Write the fetched data to an output file
                    //Comment this out if the kana is working in your terminal, this gives the same output as the terminal but in a .txt file
                    try (PrintWriter writer = new PrintWriter(new FileWriter("output.txt", StandardCharsets.UTF_8, true))) {
                        writer.println("Fetched Book URL: " + bookUrl);
                        writer.println("Fetched Title: " + title);
                        writer.println("Fetched Description: " + description);
                        writer.println(); //blank line for readability
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


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

    //function to clean up the title in .json
    private static String cleanTitle(String title) {
        //patterns to remove
        String[] patterns = {
            "新着", // "New Arrival"
            "【.*?】", // Removes text within brackets, such as "【電子書籍限定書き下ろしSS付き】"
            "\\(.*?\\)" // Removes text within parentheses
        };

        //remove each pattern
        for (String pattern : patterns) {
            title = title.replaceAll(pattern, "").trim();
        }

        return title;
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
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            vocabulary.addAll(lines);
        } catch (IOException e) {
            System.out.println("Error reading vocabulary file: " + fileName + " - " + e.getMessage());
        }
    }

    // Function to determine JLPT level based on description
    private static String determineJLPTLevel(String description) {

        //map of JLPT levels and their vocab sets
        Map<String, Set<String>> jlptLevels = new LinkedHashMap<>();
        jlptLevels.put("N1", N1_VOCAB);
        jlptLevels.put("N2", N2_VOCAB);
        jlptLevels.put("N3", N3_VOCAB);
        jlptLevels.put("N4", N4_VOCAB);
        jlptLevels.put("N5", N5_VOCAB);

        //iterate over the levels and return the first level that matches
        for(Map.Entry<String, Set<String>> entry : jlptLevels.entrySet()) {
            if(countMatchingWords(description, entry.getValue()) > 0) {
                return entry.getKey();
            }
        }

        //if no matches are found
        return null;
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
