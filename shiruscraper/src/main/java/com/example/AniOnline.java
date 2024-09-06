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

public class AniOnline {

    private static final Dotenv dotenv = Dotenv.configure().directory("../").load();
    private static final OkHttpClient client = new OkHttpClient();
    private static final String OPENAI_API_KEY = dotenv.get("OPENAI_API_KEY");

    public static void main(String[] args) {

        // Create map to store book details (title and image URL)
        Map<String, List<Map<String, String>>> booksByJLPT = new HashMap<>();
        booksByJLPT.put("N5", new ArrayList<>());
        booksByJLPT.put("N4", new ArrayList<>());
        booksByJLPT.put("N3", new ArrayList<>());
        booksByJLPT.put("N2", new ArrayList<>());
        booksByJLPT.put("N1", new ArrayList<>());

        // Total pages to process
        int totalPages = 4; //there is 19 pages total using 4 for testing currently
        for (int currentPage = 1; currentPage <= totalPages; currentPage++) {
            String searchResultsUrl = "https://www.animate-onlineshop.jp/products/index.php?spc=4&pageno=" + currentPage;
            System.out.println("Processing page: " + currentPage);

            boolean success = false;
            int attempts = 0;
            int maxAttempts = 5;
            int timeout = 50000; // 60 seconds timeout
            int backoffTime = 5000; //Initial 5 seconds backoff

            while (!success && attempts < maxAttempts) {
                attempts++;
                try {
                    // Fetch and parse the search results page
                    Document doc = Jsoup.connect(searchResultsUrl)
                            .timeout(timeout)
                            .get();
                    doc.outputSettings().charset("UTF-8");

                    // Select the book item elements containing the titles and images
                    Elements bookElements = doc.select("div.item_list ul li");

                    for (Element bookElement : bookElements) {
                        // Extract title
                        String title = bookElement.select("h3 a").text();

                        // Extract image URL
                        String imageUrl = bookElement.select("div.item_list_thumb img").attr("src");

                        // Skip if no title or image URL is found
                        if (title.isEmpty() || imageUrl.isEmpty()) {
                            System.out.println("Book title or image URL is empty. Skipping this entry.");
                            continue;
                        }

                        // Prepend the base URL if necessary
                        if (!imageUrl.startsWith("http")) {
                            imageUrl = "https://www.animate-onlineshop.jp" + imageUrl;
                        }

                        // Debugging print statements
                        System.out.println("Fetched Title: " + title);
                        System.out.println("Fetched Image URL: " + imageUrl);

                        // Use OpenAI to determine JLPT level (as before)
                        String jlptLevel = determineJLPTLevelUsingOpenAI(title);

                        if (jlptLevel != null) {
                            // Create a map to hold the book's title and image URL
                            Map<String, String> bookData = new HashMap<>();
                            bookData.put("title", title);
                            bookData.put("imageUrl", imageUrl);

                            // Add bookData to the corresponding JLPT level list
                            booksByJLPT.get(jlptLevel).add(bookData);

                            // Save the result to file after fetching each entry
                            saveResultsToFile(booksByJLPT);
                        } else {
                            System.out.println("Failed to determine JLPT level for title: " + title);
                        }

                        // Add delay between requests
                        Thread.sleep(3000);
                    }

                    success = true; // Successfully processed the page
                } catch (IOException e) {
                    System.out.println("Error fetching the website (attempt: " + attempts + "): " + e.getMessage());
                    if (attempts >= maxAttempts) {
                        System.out.println("Max attempts reached. Skipping page: " + currentPage);
                    } else {
                        System.out.println("Retrying in " + backoffTime / 1000 + " seconds...");
                        try {
                            Thread.sleep(backoffTime);
                            backoffTime *= 2; // Exponential backoff
                        } catch (InterruptedException ie) {
                            System.out.println("Error with thread sleep: " + ie.getMessage());
                            Thread.currentThread().interrupt(); // Restore interrupted status
                        }
                    }
                } catch (InterruptedException e) {
                    System.out.println("Error with thread sleep: " + e.getMessage());
                    Thread.currentThread().interrupt(); // Restore interrupted status
                }
            }
        }
    }

    // Function to get OpenAI to determine JLPT level
    private static String determineJLPTLevelUsingOpenAI(String title) {
        String prompt = "You are a language model that specializes in translating Japanese manga titles to their JLPT difficulty level. Determine its JLPT level. Only respond with one of the following levels: 'N5', 'N4', 'N3', 'N2', 'N1'. Ignore text that isn't related to the title. Here is the title: " + title;

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
                .addHeader("Content-Type", "application/json")
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
    private static void saveResultsToFile(Map<String, List<Map<String, String>>> booksByJLPT) {
        JSONObject jsonObject = new JSONObject(booksByJLPT);

        try (FileWriter file = new FileWriter("Ani_Online_Books.json", StandardCharsets.UTF_8, false)) {
            file.write(jsonObject.toString(4));
            System.out.println("Results saved to Ani_Online_Books.json");
        } catch (IOException e) {
            System.out.println("Error writing to JSON file: " + e.getMessage());
        }
    }
}
