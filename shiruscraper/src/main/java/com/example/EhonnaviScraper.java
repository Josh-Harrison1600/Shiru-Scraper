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

public class EhonnaviScraper {

    public static void main(String[] args) {

        // URL pattern for the pages, with %d for page number
        String baseSearchResultsUrlPattern = "https://www.ehonnavi.net/whatsnew.asp?st=1&pg=%d";

        // Create a list to hold book information
        List<Map<String, String>> n5Books = new ArrayList<>();

        try {
            // Loop through pages 1 to 40
            for (int pageNumber = 1; pageNumber <= 20; pageNumber++) {
                String pageUrl = String.format(baseSearchResultsUrlPattern, pageNumber);
                System.out.println("Fetching data from: " + pageUrl);

                // Connect and parse the page using Jsoup
                Document doc = Jsoup.connect(pageUrl).get();
                doc.outputSettings().charset("UTF-8");

                // Select all book entries
                Elements bookElements = doc.select("div.m_booklist");

                for (Element bookElement : bookElements) {
                    // Extract title
                    Element titleElement = bookElement.selectFirst("h3.m_bold.c_mb10 a");
                    String title = titleElement != null ? titleElement.text() : "";

                    // Extract image URL (inside <img> tag with class 'm_booklist-img')
                    Element imgElement = bookElement.selectFirst("div.m_booklist_img img");
                    String imageUrl = imgElement != null ? imgElement.attr("src") : "";

                    // Prepend the base URL if the image URL is relative
                    if (!imageUrl.startsWith("http") && !imageUrl.isEmpty()) {
                        imageUrl = "https://www.ehonnavi.net/" + imageUrl;
                    }

                    // Skip if the title or image URL is empty
                    if (title.isEmpty() || imageUrl.isEmpty()) {
                        System.out.println("Missing title or image URL. Skipping.");
                        continue;
                    }

                    // Debugging prints
                    System.out.println("Fetched Title: " + title);
                    System.out.println("Fetched Image URL: " + imageUrl);

                    // Store the title and image URL in a map
                    Map<String, String> bookData = new HashMap<>();
                    bookData.put("title", title);
                    bookData.put("imageUrl", imageUrl);

                    // Add the book data to the N5 list
                    n5Books.add(bookData);
                }

                // Wait briefly between requests to avoid overloading the server
                Thread.sleep(1000); // 1 second delay between pages
            }

            // Save the results to a JSON file after scraping all pages
            saveResultsToFile(n5Books);

        } catch (IOException e) {
            System.out.println("Error fetching the website: " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("Thread interrupted: " + e.getMessage());
        }
    }

    // Function to save the results to a file
    private static void saveResultsToFile(List<Map<String, String>> n5Books) {
        // Convert the list of books to a JSON object
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("N5", n5Books);

        try (FileWriter file = new FileWriter("N5Books.json", StandardCharsets.UTF_8, false)) {
            file.write(jsonObject.toString(4)); // Pretty print with indentation
            System.out.println("Results saved to N5Books.json");
        } catch (IOException e) {
            System.out.println("Error writing to JSON file: " + e.getMessage());
        }
    }
}
