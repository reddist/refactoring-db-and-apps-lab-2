package database;

import controller.Server;
import textAnalyzer.CardKeyword;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseProcedures {

    public static void createTables(){
        String createUsersTable = "CREATE TABLE IF NOT EXISTS Users(" +
                "user_id SERIAL PRIMARY KEY, " +
                "user_login text unique, " +
                "user_password text);";

        String createBooksTable = "CREATE TABLE IF NOT EXISTS Books(" +
                "book_id serial PRIMARY KEY," +
                "ISBN text unique," +
                "book_name text, author_name text, " +
                "publication_date text, annotation bytea," +
                "file bytea);";

        String createGenresTable = "CREATE TABLE IF NOT EXISTS Genres(" +
                "genre_id serial PRIMARY KEY," +
                "genre_name text unique);";

        String createBooksToGenresTable = "CREATE TABLE IF NOT EXISTS BooksToGenres(" +
                "book_id int," +
                "genre_id int," +
                "FOREIGN KEY (book_id) REFERENCES Books (book_id)," +
                "FOREIGN KEY (genre_id) REFERENCES Genres (genre_id)," +
                "PRIMARY KEY (book_id, genre_id));";

        String createKeywordsTable = "CREATE TABLE IF NOT EXISTS Keywords(" +
                "keyword_id serial PRIMARY KEY," +
                "keyword text unique);";

        String createBooksToKeywordsTable = "CREATE TABLE IF NOT EXISTS BooksToKeywords(" +
                "book_id int," +
                "keyword_id int," +
                "frequency int," +
                "from_annotation bool," +
                "FOREIGN KEY (book_id) REFERENCES Books (book_id)," +
                "FOREIGN KEY (keyword_id) REFERENCES Keywords (keyword_id)," +
                "PRIMARY KEY (book_id, keyword_id));";

        try {
            Server.statement.execute(createUsersTable);
            Server.statement.execute(createBooksTable);
            Server.statement.execute(createGenresTable);
            Server.statement.execute(createBooksToGenresTable);
            Server.statement.execute(createKeywordsTable);
            Server.statement.execute(createBooksToKeywordsTable);
            System.out.println("База данных готова");
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

    public static void addBook(Book book) {
        try {
            PreparedStatement addBookToDatabasePreparedStatement = Server.connection.prepareStatement(
                    "INSERT INTO Books (isbn, book_name, author_name, publication_date, annotation, file) values (?, ?, ?, ?, ?, ?)"
            );
            addBookToDatabasePreparedStatement.setString(1, book.getISBN());
            addBookToDatabasePreparedStatement.setString(2, book.getBook_name());
            addBookToDatabasePreparedStatement.setString(3, book.getAuthor_name());
            addBookToDatabasePreparedStatement.setString(4, book.getPublication_date());
            addBookToDatabasePreparedStatement.setBytes(5, book.getAnnotation());
            addBookToDatabasePreparedStatement.setBytes(6, book.getFile());
            addBookToDatabasePreparedStatement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Error adding Book to database.");
        }

        for (String genre : book.getGenres()) {
            try {
                PreparedStatement addGenreToDatabasePreparedStatement = Server.connection.prepareStatement(
                        "INSERT INTO Genres (genre_name) values (?);"
                );
                addGenreToDatabasePreparedStatement.setString(1, genre);
                addGenreToDatabasePreparedStatement.execute();
            } catch (SQLException e) {
                e.printStackTrace();
                System.out.println("Error adding genre \"" + genre + "\" to database.");
            }
        }
        try {
            PreparedStatement getBookId = Server.connection.prepareStatement("SELECT book_id FROM Books WHERE isbn like ?;");
            getBookId.setString(1, book.getISBN());
            ResultSet bookIdResultSet = getBookId.executeQuery();
            bookIdResultSet.next();
            int book_id = bookIdResultSet.getInt("book_id");
            for (String genre : book.getGenres()) {
                PreparedStatement getGenreId = Server.connection.prepareStatement("SELECT genre_id FROM Genres WHERE genre_name like ?;");
                getGenreId.setString(1, genre);
                ResultSet genreIdResultSet = getGenreId.executeQuery();
                genreIdResultSet.next();
                int genre_id = genreIdResultSet.getInt("genre_id");
                PreparedStatement setGenreToBook = Server.connection.prepareStatement("INSERT INTO bookstogenres (book_id, genre_id) VALUES (?, ?);");
                setGenreToBook.setInt(1, book_id);
                setGenreToBook.setInt(2, genre_id);
                setGenreToBook.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Error connecting Book to Genres in the database.");
        }

        for (CardKeyword keyword : book.getKeywords_annotation()) {
            try {
                PreparedStatement addKeywordToDatabasePreparedStatement = Server.connection.prepareStatement(
                        "INSERT INTO keywords (keyword) values (?);"
                );
                addKeywordToDatabasePreparedStatement.setString(1, keyword.getStem());
                addKeywordToDatabasePreparedStatement.execute();
            } catch (SQLException e) {
                e.printStackTrace();
                System.out.println("Error adding keyword \"" + keyword.getStem() + "\" to database.");
            }
        }
        try {
            PreparedStatement getBookId = Server.connection.prepareStatement("SELECT book_id FROM Books WHERE isbn like ?;");
            getBookId.setString(1, book.getISBN());
            ResultSet bookIdResultSet = getBookId.executeQuery();
            bookIdResultSet.next();
            int book_id = bookIdResultSet.getInt("book_id");
            for (CardKeyword keyword : book.getKeywords_annotation()) {
                PreparedStatement getKeywordId = Server.connection.prepareStatement("SELECT keyword_id FROM keywords WHERE keyword like ?;");
                getKeywordId.setString(1, keyword.getStem());
                ResultSet keywordIdResultSet = getKeywordId.executeQuery();
                keywordIdResultSet.next();
                int keyword_id = keywordIdResultSet.getInt("keyword_id");
                PreparedStatement setAnnotationKeywordToBook = Server.connection.prepareStatement("INSERT INTO bookstokeywords (book_id, keyword_id, frequency, from_annotation) VALUES (?, ?, ?, true);");
                setAnnotationKeywordToBook.setInt(1, book_id);
                setAnnotationKeywordToBook.setInt(2, keyword_id);
                setAnnotationKeywordToBook.setInt(3, keyword.getFrequency());
                setAnnotationKeywordToBook.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Error connecting Book to Annotation Keywords in the database.");
        }
        for (CardKeyword keyword : book.getKeywords_book()) {
            try {
                PreparedStatement addKeywordToDatabasePreparedStatement = Server.connection.prepareStatement(
                        "INSERT INTO keywords (keyword) values (?);"
                );
                addKeywordToDatabasePreparedStatement.setString(1, keyword.getStem());
                addKeywordToDatabasePreparedStatement.execute();
            } catch (SQLException e) {
                e.printStackTrace();
                System.out.println("Error adding keyword \"" + keyword.getStem() + "\" to database.");
            }
        }
        try {
            PreparedStatement getBookId = Server.connection.prepareStatement("SELECT book_id FROM Books WHERE isbn like ?;");
            getBookId.setString(1, book.getISBN());
            ResultSet bookIdResultSet = getBookId.executeQuery();
            bookIdResultSet.next();
            int book_id = bookIdResultSet.getInt("book_id");
            for (CardKeyword keyword : book.getKeywords_book()) {
                PreparedStatement getKeywordId = Server.connection.prepareStatement("SELECT keyword_id FROM keywords WHERE keyword like ?;");
                getKeywordId.setString(1, keyword.getStem());
                ResultSet keywordIdResultSet = getKeywordId.executeQuery();
                keywordIdResultSet.next();
                int keyword_id = keywordIdResultSet.getInt("keyword_id");
                PreparedStatement setAnnotationKeywordToBook = Server.connection.prepareStatement("INSERT INTO bookstokeywords (book_id, keyword_id, frequency, from_annotation) VALUES (?, ?, ?, false);");
                setAnnotationKeywordToBook.setInt(1, book_id);
                setAnnotationKeywordToBook.setInt(2, keyword_id);
                setAnnotationKeywordToBook.setInt(3, keyword.getFrequency());
                setAnnotationKeywordToBook.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Error connecting Book to Keywords in the database.");
        }
    }
}
