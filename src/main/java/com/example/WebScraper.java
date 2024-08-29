package com.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import okhttp3.*;
import io.github.cdimascio.dotenv.Dotenv;

public class WebScraper {

    private static final Dotenv dotenv = Dotenv.configure().directory("../").load();
    private static final OkHttpClient client = new OkHttpClient();
    private static final String OPENAI_API_KEY = dotenv.get("OPENAI_API_KEY");

    public static void main(String[] args) {

        // Create map to categorize books by JLPT level
        Map<String, List<String>> booksByJLPT = new HashMap<>();
        booksByJLPT.put("N5", new ArrayList<>());
        booksByJLPT.put("N4", new ArrayList<>());
        booksByJLPT.put("N3", new ArrayList<>());
        booksByJLPT.put("N2", new ArrayList<>());
        booksByJLPT.put("N1", new ArrayList<>());

        // Base URL of the Honto.jp search results page
        String baseSearchResultsUrl = "https://honto.jp/ebook/search_0750_0229001000000_09-salesnum.html?slm=5&tbty=2&unt=0&cid=ip_eb_alpk_new_04";

        // Comment out the page iteration for testing
        /*
        // Go through all pages
        int totalPages = 4;
        for (int currentPage = 1; currentPage <= totalPages; currentPage++) {
            String searchResultsUrl = baseSearchResultsUrl + "&pgno=" + currentPage;
            System.out.println("Processing page: " + currentPage);
        */
        
        // Testing with a single page (uncomment this line for testing)
        String searchResultsUrl = baseSearchResultsUrl + "&pgno=1"; // Test only with page 1
        System.out.println("Processing page: 1");

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

                    // Extract the title from the detail page
                    String title = bookDoc.select("h1.stTitle").text();

                    // Debugging print statements
                    System.out.println("Fetched Book URL: " + bookUrl);
                    System.out.println("Fetched Title: " + title);

                    // Use OpenAI to determine JLPT level
                    String jlptLevel = determineJLPTLevelUsingOpenAI(title);

                    if (jlptLevel != null) {
                        booksByJLPT.get(jlptLevel).add(title);
                        // Save the result to file immediately after fetching
                        saveResultsToFile(booksByJLPT);
                    } else {
                        System.out.println("Failed to determine JLPT level for title: " + title);
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
    }

    // Function to get OpenAI to determine JLPT level
    private static String determineJLPTLevelUsingOpenAI(String title) {
        // Define the prompt based on the title
        String prompt = "You are a language model that specializes in translating Japanese manga titles to their JLPT difficulty level. determine its JLPT level. Only respond with one of the following levels: 'N5', 'N4', 'N3', 'N2', 'N1'. Ensure you ignore text that isn't related to the title such as 【電子書籍限定書き下ろしSS付き】Here is the title: " + title;
        
        try {
            String response = callOpenAIAPI(prompt);
            if (response != null && response.contains("N")) {
                return response.split(" ")[response.split(" ").length - 1].trim();
            }
        } catch (Exception e) {
            System.out.println("Error calling OpenAI API: " + e.getMessage());
        }
        return null;
    }

    // Function to call OpenAI API
    private static String callOpenAIAPI(String prompt) throws IOException {
        String url = "https://api.openai.com/v1/chat/completions";
        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "gpt-4o-mini");
        requestBody.put("messages", Collections.singletonList(new JSONObject().put("role", "user").put("content", prompt)));

        Request request = new Request.Builder()
            .url(url)
            .post(RequestBody.create(requestBody.toString(), JSON))
            .addHeader("Authorization", "Bearer " + OPENAI_API_KEY)
            .addHeader("Content-Type", "application/json") // Added Content-Type header
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                JSONObject jsonResponse = new JSONObject(responseBody);
                return jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
            } else {
                System.out.println("OpenAI API Error: " + response.message());
            }
        }
        return null;
    }

    // Function to save results to a file
    private static void saveResultsToFile(Map<String, List<String>> booksByJLPT) {
        JSONObject jsonObject = new JSONObject(booksByJLPT);

        try (FileWriter file = new FileWriter("books_by_jlpt.json", StandardCharsets.UTF_8, false)) {
            file.write(jsonObject.toString(4));
            System.out.println("Results saved to books_by_jlpt.json");
        } catch (IOException e) {
            System.out.println("Error writing to JSON file: " + e.getMessage());
        }
    }
}
