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

//cmd to run
//mvn clean compile exec:java -D"exec.mainClass=com.example.AniOnline" -D"exec.args=-Dfile.encoding=UTF-8"


public class AniOnline {

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

        // Base URL of the Animate Online Shop search results page
        //String baseSearchResultsUrl = "https://www.animate-onlineshop.jp/products/index.php?spc=4&pageno=";

        // Go through all pages
        int totalPages = 4;
        for (int currentPage = 1; currentPage <= totalPages; currentPage++) {
            String searchResultsUrl = "https://www.animate-onlineshop.jp/products/index.php?spc=4&pageno=" + currentPage;
            System.out.println("Processing page: " + currentPage);

            try {
                // Fetch and parse the search results page
                Document doc = Jsoup.connect(searchResultsUrl).get();
                doc.outputSettings().charset("UTF-8");

                // Select the book item elements containing the titles
                Elements bookElements = doc.select("div.item_list ul li h3 a");

                for (Element linkElement : bookElements) {
                    // Extract URL and title directly from the linkElement
                    String bookUrl = linkElement.attr("href");
                    String title = linkElement.text();

                    // Check if the URL or title is not empty
                    if (bookUrl == null || bookUrl.isEmpty()) {
                        System.out.println("Book URL is empty. Skipping this entry.");
                        continue; // Skip this iteration if the book URL is empty
                    }

                    // Prepend the base URL if necessary
                    if (!bookUrl.startsWith("http")) {
                        bookUrl = "https://www.animate-onlineshop.jp" + bookUrl;
                    }

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

                }
            } catch (IOException e) {
                System.out.println("Error fetching the website: " + e.getMessage());
            } catch (InterruptedException e) {
                System.out.println("Error with thread sleep: " + e.getMessage());
                Thread.currentThread().interrupt(); // Restore interrupted status
            }
        }
    }

    // Function to get OpenAI to determine JLPT level
    private static String determineJLPTLevelUsingOpenAI(String title) {
        // Define the prompt based on the title
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
    private static void saveResultsToFile(Map<String, List<String>> booksByJLPT) {
        JSONObject jsonObject = new JSONObject(booksByJLPT);

        try (FileWriter file = new FileWriter("Ani_Online_Books.json", StandardCharsets.UTF_8, false)) {
            file.write(jsonObject.toString(4));
            System.out.println("Results saved to Ani_Online_Books.json");
        } catch (IOException e) {
            System.out.println("Error writing to JSON file: " + e.getMessage());
        }
    }
}
