package textAnalyzer;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.ru.RussianLightStemFilter;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.ClassicFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

/**
 * Keywords extractor functionality handler
 */
public class KeywordsExtractor {

    /**
     * Get list of keywords with stem form, frequency rank, and terms dictionary
     *
     * @param fullText
     * @return List<CardKeyword>, which contains keywords cards
     * @throws IOException
     */
    public static List<CardKeyword> getKeywordsList(String fullText) throws IOException {

        TokenStream tokenStream = null;

        try {
            // treat the dashed words, don't let separate them during the processing
            fullText = fullText.replaceAll("-+", "-0");

            // replace any punctuation char but apostrophes and dashes with a space
            fullText = fullText.replaceAll("[\\p{Punct}&&[^'-]]+", " ");

            // replace most common English contractions
            fullText = fullText.replaceAll("(?:'(?:[tdsm]|[vr]e|ll))+\\b", "");

            WhitespaceTokenizer stdToken = new WhitespaceTokenizer();
            stdToken.setReader(new StringReader(fullText));

            tokenStream = new StopFilter(new ASCIIFoldingFilter(new ClassicFilter(new LowerCaseFilter(stdToken))), RussianAnalyzer.getDefaultStopSet());
            tokenStream.reset();

            List<CardKeyword> cardKeywords = new LinkedList<>();

            CharTermAttribute token = tokenStream.getAttribute(CharTermAttribute.class);

            while (tokenStream.incrementToken()) {
                String term = token.toString();
                String stem = getStemForm(term);

                if (stem != null && !stem.equals("-")) {
                    CardKeyword cardKeyword = find(cardKeywords, new CardKeyword(stem.replaceAll("-0", "-")));
                    // treat the dashed words back, let look them pretty
                    cardKeyword.add(term.replaceAll("-0", "-"));
                }
            }

            // reverse sort by frequency
            Collections.sort(cardKeywords);

            return cardKeywords;
        } finally {
            if (tokenStream != null) {
                try {
                    tokenStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Get stem form of the term
     *
     * @param term
     * @return String, which contains the stemmed form of the term
     * @throws IOException
     */
    private static String getStemForm(String term) throws IOException {

        TokenStream tokenStream = null;

        try {
            WhitespaceTokenizer stdToken = new WhitespaceTokenizer();
            stdToken.setReader(new StringReader(term));

            tokenStream = new RussianLightStemFilter(stdToken);
            tokenStream.reset();

            // eliminate duplicate tokens by adding them to a set
            Set<String> stems = new HashSet<>();

            CharTermAttribute token = tokenStream.getAttribute(CharTermAttribute.class);

            while (tokenStream.incrementToken()) {
                stems.add(token.toString());
            }

            // if stem form was not found or more than 2 stems have been found, return null
            if (stems.size() != 1) {
                return null;
            }

            String stem = stems.iterator().next();

            // if the stem form has non-alphanumerical chars, return null
            if (!stem.matches("[а-яА-Я0-9-]+")) {
                return null;
            }

            return stem;
        } finally {
            if (tokenStream != null) {
                try {
                    tokenStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Find sample in collection
     *
     * @param collection
     * @param sample
     * @param <T>
     * @return <T> T, which contains the found object within collection if exists, otherwise the initially searched object
     */
    private static <T> T find(Collection<T> collection, T sample) {

        for (T element : collection) {
            if (element.equals(sample)) {
                return element;
            }
        }

        collection.add(sample);

        return sample;
    }
}
