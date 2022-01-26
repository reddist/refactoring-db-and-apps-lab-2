package controller;

import textAnalyzer.KeywordsExtractor;

import java.io.IOException;
import java.sql.Connection;
import java.time.ZonedDateTime;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Main {
    public static void main (String[] args) throws IOException {
        System.out.println(ZonedDateTime.now());
        try {
            Server server = new Server();
            Thread serverThread = new Thread(server);
            serverThread.start();
            boolean exit = false;
            Scanner scanner = new Scanner(System.in);
            String s;
            try {
                while (!exit) {
                    s = scanner.next();
                    exit = s.equals("exit");
                    if (s.equals("show")) {
                        server.getClients().forEach(client ->
                            System.out.println(
                                client.getId() + ": " + client.getLogin()
                            )
                        );
                    }
                }
                serverThread.interrupt();
            } catch (NoSuchElementException e) {
                serverThread.interrupt();
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    serverThread.interrupt();
                    Server.statement.close();
                    Server.connection.close();
                    finish(server);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println(">>> Данные не сохранены.");
                }
            }));
        } catch (Exception e) {
            e.printStackTrace();
        }

//        KeywordsExtractor.getKeywordsList(text);
//        KeywordsExtractor.getKeywordsList(text).forEach((cardKeyword -> System.out.println(cardKeyword.getTerms().toString() + "\t-\t" + cardKeyword.getStem() + " (" + cardKeyword.getFrequency() + ")")));
    }

    private static void finish(Server server) throws IOException{
//        MyReader writer = new MyReader(server.getPath(), server.getObjects());
//        System.out.println(writer.write());
    }
}
