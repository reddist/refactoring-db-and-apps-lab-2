package controller;

import util.CryptoHash;

import java.io.*;
import java.math.BigDecimal;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Client {
    private SocketChannel socketChannel;
    public Thread thread;
    private Server server;
    private static int clients_shared_id_variable = 0;
    private int id;
    private Socket clientSocket;
    private ObjectInputStream reader;
    private ObjectOutputStream writer;
    private CommandExecutor executor;
    private String login;
    private boolean isLogged;

    public Client(SocketChannel socketChannel, Server server) {
        this.id = ++clients_shared_id_variable;
        this.server = server;
        this.socketChannel = socketChannel;
        this.clientSocket = socketChannel.socket();
        this.executor = new CommandExecutor();
        this.isLogged = false;
        try {
            InputStream inputClientStream = clientSocket.getInputStream();
            OutputStream outClientStream = clientSocket.getOutputStream();

            writer = new ObjectOutputStream(outClientStream);
            reader = new ObjectInputStream(inputClientStream);
            System.out.println("Клиент номер " + id + " подключился к серверу");
        } catch (IOException e) {
            System.out.println("Поток ввода не получен");
            System.exit(0);
        }
    }

    private void serveClient() {
        while (!clientSocket.isClosed()) {
            try {
                if(this.isLogged) {
                    sendMessage("> ");
                    String command = getMessage();

                    switch (command) {
                        case "add": {
                            String book_text = getMessage();
                            if (book_text.equals("")) break;
                            StringBuilder builder = new StringBuilder(book_text);
                            while (!book_text.equals("")){
                                book_text = getMessage();
                                if (!book_text.equals("")) builder.append(book_text);
                            }
                            sendMessage(executor.add(builder.toString()));
                            break;
                        }

                        case "exit": {
                            sendMessage("Goodbye!\n");
                            close();
                            break;
                        }

                        case "change_password":{
                            change_password();
                            break;
                        }
                        case "help": {
                            sendMessage(executor.help());
                            break;
                        }
                        /*case "short": {
                            if(executor.isShort) {
                                sendMessage("Description is already shown in a short form.\n");
                            } else {
                                sendMessage("Now description would be shown in a short form.\n");
                            }
                            executor.isShort = true;
                            break;
                        }
                        case "full": {
                            if(executor.isShort) {
                                sendMessage("Now description would be shown in a full form.\n");
                            } else {
                                sendMessage("Description is already shown in a full form.\n");
                            }
                            executor.isShort = false;
                            break;
                        }*/
                        case "wrong": {
                            sendMessage("Wrong command.\n");
                            break;
                        }
                        default: {
                            sendMessage(command + " - wrong command.\n");
                            break;
                        }
                    }
                } else {
                    boolean hasLogin = false;
                    while(!hasLogin && (!clientSocket.isClosed())) {
                        sendMessage("for login");
                        sendMessage("Do you have an account? [yes / no]\n> ");
                        boolean notLogged = true;
                        while (notLogged && (!clientSocket.isClosed())) {
                            String accountResponse = getMessage();
                            if (accountResponse == null) {
                                close();
                            }
                            switch (accountResponse) {
                                case "yes": {
                                    hasLogin = true;
                                    notLogged = false;
                                    tryLogin();
                                    break;
                                }
                                case "no": {
                                    sendMessage("for login");
                                    sendMessage("Do you want to register? [yes / no]\n> ");
                                    boolean wantRegister = false;
                                    while (!wantRegister && (!clientSocket.isClosed())) {
                                        String registerResponse = getMessage();
                                        if (registerResponse == null) {
                                            close();
                                        }
                                        switch (registerResponse) {
                                            case "yes": {
                                                wantRegister = true;
                                                register();
                                                notLogged = false;
                                                break;
                                            }
                                            case "no": {
                                                sendMessage("Goodbye!\n");
                                                close();
                                                break;
                                            }
                                            default: {
                                                sendMessage("for login");
                                                sendMessage("Please, type 'yes' or 'no'.\n> ");
                                                break;
                                            }
                                        }
                                    }
                                    break;
                                }
                                default: {
                                    sendMessage("for login");
                                    sendMessage("Please, type 'yes' or 'no'.\n> ");
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (NullPointerException e) {//| IOException | ClassNotFoundException e) {
                e.printStackTrace();
                close();
                break;
            } catch (SQLException e){
                e.printStackTrace();
                System.out.println(e.getMessage());
                close();
                break;
            }
        }
    }

    private void tryLogin() throws SQLException, NullPointerException {
        while (!this.isLogged && (!clientSocket.isClosed())) {
            sendMessage("for login");
            sendMessage("login: ");
            String gotLogin = getMessage();

            Connection connection = Server.connection;
            if (connection == null) {
                System.out.println("Bitch!!");
            }
            PreparedStatement findLogin = connection.prepareStatement("SELECT count(*) FROM users WHERE user_login=?");
            findLogin.setString(1, gotLogin);
            System.out.println("login attempt: \'" + gotLogin + "\'");
            ResultSet counted = findLogin.executeQuery();
            counted.next();
            BigDecimal count = counted.getBigDecimal("count");
            if (count.intValue() == 1) {
                System.out.println("valid.");
                sendMessage("for login");
                sendMessage("password: ");
                String gotPassword = getMessage();
                boolean truePassword = false;  // проверяем пароль в БД.
                connection = Server.connection;
                PreparedStatement findPassword = connection.prepareStatement("SELECT user_password FROM users WHERE user_login = ?");
                findPassword.setString(1, gotLogin);
                ResultSet passwordFromTable = findPassword.executeQuery();
                passwordFromTable.next();
                String password = passwordFromTable.getString("user_password");
                try {
                    gotPassword = CryptoHash.getHash(gotPassword);
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                if (password.equals(gotPassword))
                    truePassword = true;
                if (truePassword) {
                    this.isLogged = true;
                    this.login = gotLogin;
                    System.out.println("Correct password.\n");
                    return;
                } else {
                    sendMessage("for login");
                    sendMessage("Wrong password.\n");
                    System.out.println("Wrong password.\n");
                }
            } else {
                System.out.println("invalid.");
                sendMessage("for login");
                sendMessage("Wrong login.\n");
            }

            sendMessage("for login");
            sendMessage("Try again? [yes / no]\n> ");
            boolean wantContinue = false;
            while (!wantContinue && (!clientSocket.isClosed())) {
                String continueResponse = getMessage();
                if (continueResponse == null) {
                    close();
                }
                switch (continueResponse) {
                    case "yes": {
                        wantContinue = true;
                        break;
                    }
                    case "no": {
                        sendMessage("Goodbye!\n");
                        close();
                        break;
                    }
                    default: {
                        sendMessage("for login");
                        sendMessage("Please, type 'yes' or 'no'.\n> ");
                        break;
                    }
                }
            }
        }
    }

    private void register() throws SQLException {
        boolean allowedLogin = false;
        String sendedLogin = "";
        while (!allowedLogin && (!clientSocket.isClosed())) {
            sendMessage("for login");
            sendMessage("Please, type the login you prefer.\n> ");
            String preferedLogin = getMessage();
            if (preferedLogin == null) {
                close();
            }
            Connection connection = Server.connection;
            PreparedStatement findLogin = connection.prepareStatement("SELECT count(*) FROM users WHERE user_login=?");
            findLogin.setString(1, preferedLogin);
            ResultSet countLogin = findLogin.executeQuery();
            countLogin.next();
            BigDecimal count = countLogin.getBigDecimal("count");
            if (count.intValue() == 0) {
                sendedLogin = preferedLogin;
                allowedLogin = true;
            } else {
                sendMessage("for login");
                sendMessage("The login is already used. Try again? [yes / no]\n> ");
                boolean wantContinue = false;
                while (!wantContinue && (!clientSocket.isClosed())) {
                    String continueResponse = getMessage();
                    if (continueResponse == null) {
                        close();
                    }
                    switch (continueResponse) {
                        case "yes": {
                            wantContinue = true;
                            break;
                        }
                        case "no": {
                            sendMessage("Goodbye!\n");
                            close();
                            break;
                        }
                        default: {
                            sendMessage("for login");
                            sendMessage("Please, type 'yes' or 'no'.\n> ");
                            break;
                        }
                    }
                }
            }
        }
        boolean hasTypedPassword = false;
        String gotPassword = "";
        String gotRepeatOfPassword = "";
        while (!hasTypedPassword && (!clientSocket.isClosed())) {
            sendMessage("for login");
            sendMessage("Please, type your password.\n> ");
            gotPassword = getMessage();
            if (gotPassword == null) {
                close();
            }
            sendMessage("for login");
            sendMessage("Please, repeat your password.\n> ");
            gotRepeatOfPassword = getMessage();
            if (gotRepeatOfPassword == null) {
                close();
            }
            if (gotPassword.compareTo(gotRepeatOfPassword) != 0) {
                sendMessage("for login");
                sendMessage("Passwords are different. Try again? [yes / no]\n> ");
                boolean wantContinue = false;
                while (!wantContinue && (!clientSocket.isClosed())) {
                    String continueResponse = getMessage();
                    if (continueResponse == null) {
                        close();
                    }
                    switch (continueResponse) {
                        case "yes": {
                            wantContinue = true;
                            break;
                        }
                        case "no": {
                            sendMessage("Goodbye!\n");
                            close();
                            break;
                        }
                        default: {
                            sendMessage("for login");
                            sendMessage("Please, type 'yes' or 'no'.\n> ");
                            break;
                        }
                    }
                }
            } else {
                hasTypedPassword = true;
            }
        }
        if (hasTypedPassword) {
            try {
                PreparedStatement insertUser = Server.connection.prepareStatement("INSERT INTO users (user_login, user_password) VALUES (?, ?);");
                insertUser.setString(1, sendedLogin);
                try {
                    insertUser.setString(2, CryptoHash.getHash(gotPassword));
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                insertUser.execute();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void change_password() throws SQLException{
        boolean truePassword = false;
        while (!truePassword && (!clientSocket.isClosed())) {
            sendMessage("for login");
            sendMessage("Please, type your current password.\n> ");
            String oldPassword = getMessage();
            if (oldPassword == null) {
                close();
            }
            Connection connection = Server.connection;
            PreparedStatement findPassword = connection.prepareStatement("SELECT user_password FROM users WHERE user_login = ?");
            findPassword.setString(1, login);
            ResultSet passwordFromTable = findPassword.executeQuery();
            passwordFromTable.next();
            String password = passwordFromTable.getString("user_password");
            try {
                oldPassword = CryptoHash.getHash(oldPassword);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            if (password.equals(oldPassword)) {
                truePassword = true;
                sendMessage("for login");
                sendMessage("Please, type new password.\n> ");
                String newPassword = getMessage();
                if (newPassword == null) {
                    close();
                }
                try {
                    PreparedStatement updateUser = Server.connection.prepareStatement("UPDATE users SET user_password = ? WHERE user_login = ?;");
                    updateUser.setString(2, login);
                    try {
                        updateUser.setString(1, CryptoHash.getHash(newPassword));
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                    updateUser.executeUpdate();
                    sendMessage("for login");
                    sendMessage("Password successfully changed.\n> ");
                } catch (SQLException e) {
                    System.out.println(e.getMessage());
                }
            } else {
                sendMessage("for login");
                sendMessage("Wrong password. Try again? [yes / no]\n> ");
                boolean wantContinue = false;
                while (!wantContinue && (!clientSocket.isClosed())) {
                    String continueResponse = getMessage();
                    if (continueResponse == null) {
                        close();
                    }
                    if (continueResponse.equals("yes")) {
                        wantContinue = true;
                    } else {
                        if (!continueResponse.equals("no")) {
                            sendMessage("for login");
                            sendMessage("Please, type 'yes' or 'no'.\n> ");
                        }
                        break;
                    }
                }
                if (!wantContinue) {
                    break;
                }
            }
        }
    }

    private void close() {
        try {
            clientSocket.close();
            writer.close();
            reader.close();
            thread.interrupt();
            socketChannel.close();
            server.getClients().remove(this);
            System.out.println("Обслуживание клиента " + id + " завершено.");
//            --clients_shared_id_variable;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("не удалось прервать обслуживание клиента.");
        }
    }

    public void sendMessage(String s) {
        try {
            writer.writeUTF(s);
            writer.flush();
        } catch (IOException e) {
            System.out.println("Отправка не удалась.");
        }
    }

    private String getMessage() {
        try {
            return reader.readUTF();
        } catch (Exception e) {
            System.out.println("Команда не была передана.");
            return null;
        }

    }

    void go() {
        thread = new Thread(this::serveClient);
        thread.start();
    }

    public int getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }
}
