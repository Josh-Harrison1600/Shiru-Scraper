package com.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import okhttp3.*;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.concurrent.TimeUnit;


//cmd to run scraper
//mvn clean compile exec:java -D"exec.mainClass=com.example.WebScraper" -D"exec.args=-Dfile.encoding=UTF-8"

public class WebScraper {

    private static final Dotenv dotenv = Dotenv.configure().directory("../").load();

    // Increase OkHttp timeouts to avoid timeout issues with long API responses
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)  // 60 seconds to establish a connection
            .readTimeout(60, TimeUnit.SECONDS)     // 60 seconds to wait for the response
            .writeTimeout(60, TimeUnit.SECONDS)    // 60 seconds to send data to the server
            .build();
    
    private static final String OPENAI_API_KEY = dotenv.get("OPENAI_API_KEY");

    public static void main(String[] args) {

        // Create map to categorize books by JLPT level
        Map<String, List<Map<String, String>>> booksByJLPT = new HashMap<>();
        booksByJLPT.put("N5", new ArrayList<>());
        booksByJLPT.put("N4", new ArrayList<>());
        booksByJLPT.put("N3", new ArrayList<>());
        booksByJLPT.put("N2", new ArrayList<>());
        booksByJLPT.put("N1", new ArrayList<>());
        booksByJLPT.put("N/A", new ArrayList<>());

        // Base URL of the Honto.jp search results page
        String baseSearchResultsUrl = "https://honto.jp/ebook/search_0750_0229001000000_09-salesnum.html?slm=5&tbty=2&unt=0&cid=ip_eb_alpk_new_04";

        // List to collect book titles for batch processing
        List<String> bookTitles = new ArrayList<>();
        Map<String, Map<String, String>> bookDataMap = new HashMap<>(); // Maps titles to book data (title, image, and URL)

        // Go through all pages
        int totalPages = 7;
        for (int currentPage = 1; currentPage <= totalPages; currentPage++) {
            String searchResultsUrl = baseSearchResultsUrl + "&pgno=" + currentPage;
            System.out.println("Processing page: " + currentPage);

            if (searchResultsUrl == null || searchResultsUrl.isEmpty()) {
                System.out.println("The 'url' parameter must not be empty.");
                return;
            }

            try {
                Document doc = Jsoup.connect(searchResultsUrl).get();
                doc.outputSettings().charset("UTF-8");

                Elements bookElements = doc.select(".stProduct02");

                for (Element bookElement : bookElements) {
                    String bookUrl = bookElement.select("a.dyTitle").attr("href");

                    // Add base URL if needed
                    if (bookUrl == null || bookUrl.isEmpty()) {
                        System.out.println("Book URL is empty. Skipping this entry.");
                        continue;
                    }

                    if (!bookUrl.startsWith("http")) {
                        bookUrl = "https://honto.jp" + bookUrl;
                    }

                    try {
                        Document bookDoc = Jsoup.connect(bookUrl).get();
                        bookDoc.outputSettings().charset("UTF-8");

                        // Extract the title from the detail page
                        String title = bookDoc.select("h1.stTitle").text();

                        // Extract the image URL from the detail page
                        String imageUrl = bookElement.select("img.dyImage").attr("data-src");
                        if (imageUrl == null || imageUrl.isEmpty()) {
                            imageUrl = bookElement.select("img.dyImage").attr("srcset");
                        }

                        // If the image is not found, skip
                        if (imageUrl == null || imageUrl.isEmpty()) {
                            System.out.println("Image URL is empty. Skipping this entry.");
                            continue;
                        }

                        // Print debugging info
                        System.out.println("Fetched Book URL: " + bookUrl);
                        System.out.println("Fetched Title: " + title);
                        System.out.println("Fetched Image URL: " + imageUrl);

                        // Add title to the list for batch processing
                        bookTitles.add(title);

                        // Store the title, image URL, and book URL for later use
                        Map<String, String> bookData = new HashMap<>();
                        bookData.put("title", title);
                        bookData.put("imageUrl", imageUrl);
                        bookData.put("bookUrl", bookUrl);  // Store the book URL
                        bookDataMap.put(title, bookData); // Store the data by title

                        // Temporarily store the book under "N/A" category
                        booksByJLPT.get("N/A").add(bookData);

                    } catch (IOException e) {
                        System.out.println("Error fetching the book detail page: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                System.out.println("Error fetching the website: " + e.getMessage());
            }
        }

        // Process the batch of titles if not empty
        if (!bookTitles.isEmpty()) {
            // Process titles in batches to avoid large API requests and include delay
            Map<String, String> titleToJLPTMap = batchProcessJLPTClassification(bookTitles);
            System.out.println("JLPT Levels: " + titleToJLPTMap);

           // Map each book to its correct JLPT level
            for (Map.Entry<String, String> entry : titleToJLPTMap.entrySet()) {
                String openAITitle = entry.getKey(); // Title from OpenAI response
                String jlptLevel = entry.getValue();

                // Ensure the JLPT level exists in the booksByJLPT map
                if (!booksByJLPT.containsKey(jlptLevel)) {
                    System.out.println("Unknown JLPT level: " + jlptLevel + ". Defaulting to N/A.");
                    jlptLevel = "N/A"; // Default to "N/A" if the level is not recognized
                }

                boolean matched = false;
                String closestMatch = null;
                int minDistance = Integer.MAX_VALUE;

                // Iterate through the fetched book titles and attempt to match them
                for (String fetchedTitle : bookDataMap.keySet()) {
                    String normalizedFetchedTitle = normalizeTitle(fetchedTitle);

                    // Calculate Levenshtein distance between OpenAI title and fetched title
                    int distance = levenshteinDistance(openAITitle, normalizedFetchedTitle);
                    int threshold = calculateAdaptiveThreshold(normalizedFetchedTitle); // Adaptive threshold

                    if (distance < threshold) {
                        // If below the threshold, we found a good match
                        matched = true;
                        Map<String, String> bookData = bookDataMap.get(fetchedTitle);

                        // Move the book to the correct JLPT level
                        booksByJLPT.get(jlptLevel).add(bookData);
                        System.out.println("Matched and moved book: " + fetchedTitle + " to JLPT level " + jlptLevel);

                        // Remove the book from "N/A"
                        booksByJLPT.get("N/A").remove(bookData);
                        break;
                    }

                    // Record the closest match if nothing below the threshold is found
                    if (distance < minDistance) {
                        minDistance = distance;
                        closestMatch = fetchedTitle;
                    }
                }

                // If no match was found, use the closest match
                if (!matched && closestMatch != null) {
                    System.out.println("No perfect match found for title: " + openAITitle + ". Using closest match: " + closestMatch);

                    Map<String, String> bookData = bookDataMap.get(closestMatch);
                    booksByJLPT.get(jlptLevel).add(bookData);
                    booksByJLPT.get("N/A").remove(bookData);
                }
            }


            // Save results after JLPT level determination
            saveResultsToFile(booksByJLPT);
        }
    }

    // Function to batch process titles for OpenAI classification with a delay between batches
    private static Map<String, String> batchProcessJLPTClassification(List<String> bookTitles) {
        Map<String, String> titleToJLPTMap = new HashMap<>();
        int batchSize = 20; // Limit each batch to 20 titles to avoid token limits or large responses
        int totalTitles = bookTitles.size();

        for (int i = 0; i < totalTitles; i += batchSize) {
            List<String> batch = bookTitles.subList(i, Math.min(i + batchSize, totalTitles));
            Map<String, String> batchResults = determineJLPTLevelUsingOpenAI(batch);
            titleToJLPTMap.putAll(batchResults); // Merge batch results into the overall map

            // Introduce a 20-second delay between batches to respect OpenAI API rate limits
            try {
                System.out.println("Waiting for 20 seconds before next batch...");
                Thread.sleep(20000);  // 20 seconds delay between batches
            } catch (InterruptedException e) {
                System.out.println("Interrupted during delay: " + e.getMessage());
            }
        }

        return titleToJLPTMap;
    }

    // Normalize titles by removing special characters and trimming whitespace
    private static String normalizeTitle(String title) {
        title = Normalizer.normalize(title, Normalizer.Form.NFKC);
        return title.replaceAll("[^\\p{L}\\p{N}]+", "").toLowerCase().trim();
    }

    // Function to calculate Levenshtein distance
    public static int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1),
                            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1));
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }

    // Adaptive threshold based on title length for Levenshtein distance
    private static int calculateAdaptiveThreshold(String title) {
        int length = title.length();
        if (length < 10) return 2; // Short titles can have a small threshold
        if (length < 20) return 3; // Medium titles a bit larger
        return 5; // Longer titles can have a higher tolerance
    }

    // Function to get OpenAI to determine JLPT level for a batch of titles
    private static Map<String, String> determineJLPTLevelUsingOpenAI(List<String> titles) {
        Map<String, String> titleToJLPTMap = new HashMap<>();

        // Define the prompt based on the batch of titles
        String prompt = "You are a language model that specializes in translating Japanese manga titles to their JLPT difficulty level. Please determine the JLPT levels for the following titles. Respond with one of the following levels: 'N5', 'N4', 'N3', 'N2', 'N1' for each title. Ensure you ignore text that isn't related to the title such as 【電子書籍限定書き下ろしSS付き】\n\n" + String.join("\n", titles);
        try {
            String response = callOpenAIAPI(prompt);
            if (response != null && response.contains("N")) {
                // Split the response into lines and parse each line
                String[] lines = response.split("\n");
                for (String line : lines) {
                    if (line.contains("-")) {
                        String[] parts = line.split(" - ");
                        if (parts.length == 2) {
                            String title = parts[0].trim().replaceFirst("\\d+\\. ", ""); // Removing the leading number
                            String jlptLevel = parts[1].trim(); // Extracting the JLPT level (e.g., N2)
                            titleToJLPTMap.put(normalizeTitle(title), jlptLevel); // Store normalized title
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error calling OpenAI API: " + e.getMessage());
        }
        return titleToJLPTMap;
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
                System.out.println("OpenAI API Error: " + response.code() + " - " + response.message());
                System.out.println("Response body: " + response.body().string());
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
                bookJson.put("bookUrl", book.get("bookUrl"));  // Add the book URL to the JSON output
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
