package com.shiruscraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class WebScraper {

    //sample JLPT vocab datasets for every level
    private static final Set<String> N5_VOCAB = Set.of("これ", "それ", "あれ"); 
    private static final Set<String> N4_VOCAB = Set.of("友達", "便利", "練習"); 
    private static final Set<String> N3_VOCAB = Set.of("場合", "必要", "関係"); 
    private static final Set<String> N2_VOCAB = Set.of("経済", "状況", "報告");
    private static final Set<String> N1_VOCAB = Set.of("哲学", "機能", "理論");

    public static void main(String[] args) {
        Map<String, List<String> booksByJLPT = new HashMap<>();
        booksByJLPT.put("N5", new ArrayList<>());
        booksByJLPT.put("N4", new ArrayList<>());
        booksByJLPT.put("N3", new ArrayList<>());
        booksByJLPT.put("N2", new ArrayList<>());
        booksByJLPT.put("N1", new ArrayList<>());
    

        List<String> websites = List.of("MAKETHISANACTUALWEBSITE")

        for (String website : websites) {
            try {
                Document doc = Jsoup.connect(website).get();
                Elements bookElements = doc.select(".book-item");

                for (Element book : bookElements) {
                    String title = book.select(".book-title").text();
                    String description = book.select(".book-description").text();

                    String jlptLevel = determineJLPTLevel(description);

                    if(jlptLevel != null) {
                        booksByJLPT.get(jlptLevel).add(title);
                    }
                }
            } catch (IOException e) {
                System.out.println("Error fetching the website: " + e.getMessage());
            }
        }

        JSONObject jsonObject = new JSONObject(booksByJLPT);

        try (FileWriter file = new FileWriter("books_by_jlpt.json")){
            file.write(jsonObject.toString(4));
            System.out.println("Successfully written to books_by_jlpt.json");
        } catch (IOException e) {
            System.out.println("Error writing to JSON file: " + e.getMessage());
        }
    }

    //function to determine JLPT level based on description
    private static String determineJLPTLevel(String description) {
        int n5score = countMatchingWords(description, N5_VOCAB);
        int n4score = countMatchingWords(description, N4_VOCAB);
        int n3score = countMatchingWords(description, N3_VOCAB);
        int n2score = countMatchingWords(description, N2_VOCAB);
        int n1score = countMatchingWords(description, N1_VOCAB);

        Map<String, Integer> scores= Map.of(
            "N5" , n5score,
            "N4" , n4score,
            "N3" , n3score,
            "N2" , n2score,
            "N1" , n1score,
        );

        return scores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .filter(entry -> entry.getValue() > 0)//ensures theres atleast 1 match
            .map(Map.Entry::getKey)
            .orElse(null);//return the highest scoring level or null if no matches
    }

    //helper function to count matching words from a set
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