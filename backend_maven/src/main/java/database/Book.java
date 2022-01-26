package database;

import lombok.Data;
import textAnalyzer.CardKeyword;
import textAnalyzer.KeywordsExtractor;

import java.io.IOException;
import java.util.List;

@Data
public class Book {
    private String ISBN;
    private String book_name;
    private String author_name;
    private String publication_date;
    private String[] genres;
    private List<CardKeyword> keywords_annotation;
    private List<CardKeyword> keywords_book;
    private byte[] annotation;
    private byte[] file;

    public static Book convertFromStringToBook(String text) {
        String[] splitted = text.split("\r\n|\r|\n", 8);
        Book book = new Book();
        String annotation = "";
        for (int i = 0; i < 6; i++) {
            String attribute_name = splitted[i].split(": ", 2)[0];
            String attribute_value = splitted[i].split(": ", 2)[1];
            if ("Author".equals(attribute_name)) book.setAuthor_name(attribute_value);
            if ("Date".equals(attribute_name)) book.setPublication_date(attribute_value);
            if ("Name".equals(attribute_name)) book.setBook_name(attribute_value);
            if ("ISBN".equals(attribute_name)) book.setISBN(attribute_value);
            if ("Genres".equals(attribute_name)) book.setGenres(attribute_value.split(","));
            if ("Annotation".equals(attribute_name)) {
                annotation = attribute_value;
                book.setAnnotation(attribute_value.getBytes());
            }
        }
        book.setFile(text.getBytes());
        try {
            book.setKeywords_book(KeywordsExtractor.getKeywordsList(splitted[7]).subList(0, 4));
            book.setKeywords_annotation(KeywordsExtractor.getKeywordsList(annotation).subList(0, 4));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return book;
    }

    public static boolean checkTextFormat (String text) {
        String splitted[] = text.split("\r\n|\r|\n", 8);
        boolean checks[] = new boolean[6];
        for (int i = 0; i < 6; i++) {
            checks[i] = false;
        }
        for (int i = 0; i < 6; i++) {
            String attribute_name = splitted[i].split(": ", 2)[0];
            if ("Author".equals(attribute_name)) checks[0] = true;
            if ("Date".equals(attribute_name)) checks[1] = true;
            if ("Name".equals(attribute_name)) checks[2] = true;
            if ("ISBN".equals(attribute_name)) checks[3] = true;
            if ("Genres".equals(attribute_name)) checks[4] = true;
            if ("Annotation".equals(attribute_name)) checks[5] = true;
        }
        boolean totalCheck = true;
        for (int i = 0; i < 6; i++) {
            totalCheck = totalCheck && checks[i];
        }
        return totalCheck;
    }
}
