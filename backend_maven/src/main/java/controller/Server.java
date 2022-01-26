package controller;

import database.DatabaseProcedures;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class Server implements Runnable {
    public static Connection connection;

    public static Statement statement;

    private ServerSocketChannel serverSocket;

    private ArrayList<Client> clients = new ArrayList<>();

    private int port = 1488;
    private CommandExecutor executor;

    public Server() {
        executor = new CommandExecutor();
    }

    @Override
    public void run() {
        connection = createConnectionWithDataBase();
        DatabaseProcedures.createTables();
        SocketAddress socketAddress = new InetSocketAddress(port);
        System.out.println("Попытка запустить сервер...");
        try {
            serverSocket = ServerSocketChannel.open();
            serverSocket.bind(socketAddress);
            System.out.println("Порт: " + serverSocket.socket().getLocalPort());
        } catch (IOException e) {
            System.out.println("Что-то пошло не так.");
            e.printStackTrace();
            System.exit(-1);
        }
        while (serverSocket.isOpen()) {
            SocketChannel clientChannel = waitConnection();
            if (clientChannel == null) break;
            Client client = new Client(clientChannel, this);
            clients.add(client);
            client.go();
        }
    }

    private Connection createConnectionWithDataBase() {
        try {
            Class.forName("org.postgresql.Driver");
            String url = "jdbc:postgresql://localhost:5432/rdbapps";
            Connection connection = DriverManager.getConnection(url, "aleksey", "12345");
            statement = connection.createStatement();
//            statement.executeQuery("INSERT INTO users (user_id, user_login, user_mail, user_password) VALUES (2, 'reddist', 'erddist@gmail.com', 'qwerty');");
            return connection;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        return null;
    }

    private SocketChannel waitConnection() {
        try {
            SocketChannel client;
            System.out.println("Ждём подключения");
            client = serverSocket.accept();
            System.out.println("Произошло подключение.");
            return client;
        } catch (ClosedByInterruptException e) {
            System.out.println("Сервер выключается.");
            try {
                serverSocket.close();
                clients.forEach(c -> c.thread.interrupt());
            } catch (IOException ex) {
                System.out.println("Не удалось закрыть сетевой канал.");
            }
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Что-то не так.");
            return null;
        }
    }

    public ArrayList<Client> getClients() {
        return clients;
    }
}
