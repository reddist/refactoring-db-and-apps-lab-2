import java.io.*;
import java.net.Socket;
import java.util.*;

/**
 * Главный класс программы, содержащий исполняемый метод.
 * @see Client#go(String[])
 */
public class Client {

    private int port = 1488;

    public void go(String[] args){
//        MyReader reader = new MyReader(path);

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("Введите \"connect\", чтобы подключиться к серверу, или \"exit\", чтобы выйти.");
            try {
                String s = scanner.next();
                if (s.equals("exit")) System.exit(0);
                if (!s.equals("connect")) continue;
            } catch (NoSuchElementException e) {
                System.out.println("Ой!!!");
                break;
            }
            Socket socket;
            try {
                socket = connect(Integer.parseInt(args[0]));
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                socket = connect(port);
            }
            if (socket == null) {
                continue;
            }

            OutputStream outputStream = null;
            InputStream inputStream = null;
            try {
                outputStream = socket.getOutputStream();
                inputStream = socket.getInputStream();
            } catch (IOException e) {
                System.out.println("Невозможно получить поток вывода!");
                System.exit(-1);
            }
            ObjectInputStream objectInputStream = null;
            ObjectOutputStream objectOutputStream = null;

            try {
                objectInputStream = new ObjectInputStream(inputStream);
                objectOutputStream = new ObjectOutputStream(outputStream);

            } catch (NullPointerException | IOException e) {
                System.out.println("Coeдинение не установлено.");
                continue;
            }

            try {
                System.out.println("Соединение установлено.");
                CommandReader commandReader = new CommandReader(objectOutputStream);
                while (true) {
                    String sss = objectInputStream.readUTF();
					//System.out.print(sss);
                    if (sss.equals("Goodbye!\n")) {
                        System.out.println(sss);
                        inputStream.close();
                        outputStream.close();
                        socket.close();
                        break;
                    }
                    if (sss.equals("Антон Валерьевич")) {
                        System.out.print(objectInputStream.readUTF());
                        Sound.playSound("fanfares.wav").join();
                    } else {
                        if (!sss.equals("for login")) {
                            System.out.print(sss);
                            if (sss.equals("> ")) {
                                try {
                                    commandReader.readCommands(new Scanner(System.in));
                                } catch (NoSuchElementException e) {
                                    System.out.println("Typing Ctrl+C/Ctrl+D is unnecessary.");
                                }
                            }
                        } else {
                            sss = objectInputStream.readUTF();
                            if (!sss.equals("for login"))
                                System.out.print(sss);
                            if (!(sss.equals("Wrong password.\n") || sss.equals("Wrong login.\n"))) {
                                if(sss.equals("password: ") || sss.equals("Please, type your current password.\n> ")){
                                    try {
                                        String password = new String(System.console().readPassword());
                                        objectOutputStream.writeUTF(password);
                                        objectOutputStream.flush();
                                    } catch (NoSuchElementException e) {
                                        System.out.println("Typing Ctrl+C/Ctrl+D is unnecessary.");
                                    }
                                } else {
                                    try {
                                        Scanner forLogIn = new Scanner(System.in);
                                        objectOutputStream.writeUTF(forLogIn.nextLine());
                                        objectOutputStream.flush();
                                    } catch (NoSuchElementException e) {
                                        System.out.println("Typing Ctrl+С/Ctrl+D is unnecessary.");
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Сервер недоступен для отправки");

            } catch (NullPointerException e) {
                e.printStackTrace();
                System.out.println("упссссс");

            } catch (ArrayIndexOutOfBoundsException e) {
                //e.printStackTrace();
                System.out.println("Попробуйте еще раз");
            }

        }
    }
    private static Socket connect(int port) {
        try {
            return new Socket("localhost", port);
        } catch (Exception e) {
            System.out.println("не удалось установить соединение");
        }
        return null;
    }
}
