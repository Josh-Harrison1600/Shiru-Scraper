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

//cmd to run scraper
//mvn clean compile exec:java -D"exec.mainClass=com.example.WebScraper" -D"exec.args=-Dfile.encoding=UTF-8"

public class WebScraper {

    private static final Dotenv dotenv = Dotenv.configure().directory("../").load();
    private static final OkHttpClient client = new OkHttpClient();
    private static final String OPENAI_API_KEY = dotenv.get("OPENAI_API_KEY");

    public static void main(String[] args) {

        // Create map to categorize books by JLPT level
        Map<String, List<Map<String, String>>> booksByJLPT = new HashMap<>();
        booksByJLPT.put("N5", new ArrayList<>());
        booksByJLPT.put("N4", new ArrayList<>());
        booksByJLPT.put("N3", new ArrayList<>());
        booksByJLPT.put("N2", new ArrayList<>());
        booksByJLPT.put("N1", new ArrayList<>());

        // Base URL of the Honto.jp search results page
        String baseSearchResultsUrl = "https://honto.jp/ebook/search_0750_0229001000000_09-salesnum.html?slm=5&tbty=2&unt=0&cid=ip_eb_alpk_new_04";

        // Go through all pages
        int totalPages = 4;
        for (int currentPage = 1; currentPage <= totalPages; currentPage++) {
            String searchResultsUrl = baseSearchResultsUrl + "&pgno=" + currentPage;
            System.out.println("Processing page: " + currentPage);

            if (searchResultsUrl == null || searchResultsUrl.isEmpty()) {
                System.out.println("The 'url' parameter must not be empty.");
                return;
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

                    if (bookUrl == null || bookUrl.isEmpty()) {
                        System.out.println("Book URL is empty. Skipping this entry.");
                        continue;
                    }

                    if (!bookUrl.startsWith("http")) {
                        bookUrl = "https://honto.jp" + bookUrl;
                    }

                    try {
                        // Navigate to the book's detail page
                        Document bookDoc = Jsoup.connect(bookUrl).get();
                        bookDoc.outputSettings().charset("UTF-8");

                        // Extract the title from the detail page
                        String title = bookDoc.select("h1.stTitle").text();

                        // Extract the image URL from the detail page
                        String imageUrl = bookElement.select("img.dyImage").attr("data-src");
                        if(imageUrl == null || imageUrl.isEmpty()) {
                            imageUrl = bookElement.select("img.dyImage").attr("srcset");
                        }

                        //if image is not found skip
                        if (imageUrl == null || imageUrl.isEmpty()) {
                            System.out.println("Image URL is empty. Skipping this entry.");
                            continue;
                        }

                        // Print debugging info
                        System.out.println("Fetched Book URL: " + bookUrl);
                        System.out.println("Fetched Title: " + title);
                        System.out.println("Fetched Image URL: " + imageUrl);

                        // Use OpenAI to determine JLPT level
                        String jlptLevel = determineJLPTLevelUsingOpenAI(title);

                        if (jlptLevel != null) {
                            // Store the title and image URL
                            Map<String, String> bookData = new HashMap<>();
                            bookData.put("title", title);
                            bookData.put("imageUrl", imageUrl);

                            // Add the book data to the corresponding JLPT level list
                            booksByJLPT.get(jlptLevel).add(bookData);

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
    // The outer JSONObject holds the JLPT levels as keys and lists of books as values
    JSONObject jsonObject = new JSONObject();

    for (Map.Entry<String, List<Map<String, String>>> entry : booksByJLPT.entrySet()) {
        String jlptLevel = entry.getKey();
        List<Map<String, String>> books = entry.getValue();

        // Create a JSON array for the books
        org.json.JSONArray jsonArray = new org.json.JSONArray();

        for (Map<String, String> book : books) {
            JSONObject bookJson = new JSONObject();
            bookJson.put("title", book.get("title"));
            bookJson.put("imageUrl", book.get("imageUrl"));
            jsonArray.put(bookJson);
        }

        // Put the JSON array under the corresponding JLPT level
        jsonObject.put(jlptLevel, jsonArray);
    }

    try (FileWriter file = new FileWriter("books_by_jlpt.json", StandardCharsets.UTF_8, false)) {
        file.write(jsonObject.toString(4)); // Pretty print with indentation
        System.out.println("Results saved to books_by_jlpt.json");
    } catch (IOException e) {
        System.out.println("Error writing to JSON file: " + e.getMessage());
    }
}
}