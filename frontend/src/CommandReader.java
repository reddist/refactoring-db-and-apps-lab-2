import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  Класс, предназначенный для считывания и интерпретации команд.
 */
public class CommandReader {
    ObjectOutputStream streamToServer;

    public CommandReader(ObjectOutputStream streamToServer) {
        this.streamToServer = streamToServer;
    }

    /**
     * Метод, выполняющий считывание и интерпретацию команд, также выполняющий проверку на соответствие введённых команд Формату команд и Формату json-объектов.
     */
    public void readCommands(Scanner consoleIn) throws IOException {
        {
            String consoleCommand = consoleIn.nextLine();                                  // Считывание первой строчки
            while((charCounter(consoleCommand, '{') != charCounter(consoleCommand, '}'))) {     // Ввод построчно до конца объекта json, если в
                System.out.print("\t");                                                                        // первой строке была открывающая скобка '{'
                consoleCommand += consoleIn.nextLine();
            }
            // add{"author":"aa","name":"as","date":"29.08.1973","ISBN":"94-234-435345-2234","genres":"army;fantasy","annotation":"test book.","file":"book.txt"}
            String newCommand = consoleCommand.replaceAll("\\s+", "");      // Удаление всех пробелов, табуляций, знаков переносов строки
            if(newCommand.matches("[a-zIA_]+") ||    // Проверка соответствия введённой команды: - командам без объекта
//                    newCommand.matches("[a-z_]+\\{((\"[\\w]+\":)((\"[0-9\\wа-яА-Я+.\\-; ]+\",)|([0-9]{2}\\.[0-9]{2}\\.[0-9]{4},))){6}((\"[\\w]+\":)((\"[0-9\\wа-яА-Я+.\\-; ]+\")|([0-9]{2}\\.[0-9]{2}\\.[0-9]{4})))\\}") ||       // Проверка на команду add
                    newCommand.matches("add\\{[a-zA-Z0-9/_\\.\\-\\:\\\\]+\\}") ||       // Проверка на команду add
                    newCommand.matches("find\\{\"[\\w]+\":\"[0-9\\w+.\\-;]+\"\\}")) {               // - команда find
                StringBuilder command = new StringBuilder();
                int i = 0;
                for (char c; i < newCommand.length() && (newCommand.charAt(i) != '{'); i++) {
                    c = newCommand.charAt(i);
                    command.append(c);
                }
                if (newCommand.matches("[a-zIA_]+")) {
                    if ("add".equals(command.toString()) || "find".equals(command.toString())){
                        streamToServer.writeUTF("wrong");
                        streamToServer.flush();
                    } else {
                        streamToServer.writeUTF(command.toString());
                        streamToServer.flush();
                    }
                } else {
                    if ("add".equals(command.toString())) {
                        String returned = "";
                        String address = newCommand.substring(i + 1, newCommand.length() - 1);
                        boolean fileNotFound = false;
                        File importFrom = new File(address);
                        if(!importFrom.exists())
                            fileNotFound = true;
                        if (!fileNotFound) {
                            streamToServer.writeUTF(command.toString());
                            try {
                                InputStreamReader in = new InputStreamReader(new FileInputStream(address), StandardCharsets.UTF_8);
                                try {
                                    System.out.format("+ Extraction from %s\n", address);
                                    boolean file_ended = false;
                                    while (!file_ended) {
                                        int bufferSize = 1024;
                                        char[] buffer = new char[bufferSize];
                                        int readSize = in.read(buffer, 0, buffer.length);
                                        if (readSize < bufferSize) {
                                            buffer = Arrays.copyOf(buffer, readSize);    // копирование в buffer, если количество считанных символов
                                            file_ended = true;                           // меньше 1024, т.к. иначе лишние переносы строк
                                        }
                                        streamToServer.writeUTF(new String(buffer));
                                        streamToServer.flush();
                                    }
                                    streamToServer.writeUTF("");
                                    streamToServer.flush();
                                } catch (UnsupportedEncodingException e) {
                                    returned += "Кодировка не поддерживается.\n";
                                    streamToServer.writeUTF("");
                                    streamToServer.flush();
                                } catch (NegativeArraySizeException e) {
                                    returned += "Нет данных в файле.\n";
                                    streamToServer.writeUTF("");
                                    streamToServer.flush();
                                } catch (SecurityException e) {
                                    returned += "Файл недоступен для чтения.\n";
                                    streamToServer.writeUTF("");
                                    streamToServer.flush();
                                }
                            } catch (FileNotFoundException e) {
                                returned += "Файл не найден.\n";
                                streamToServer.writeUTF("");
                                streamToServer.flush();
                            }
                        } else {
                            returned += "Файла не существует.\n";
                            streamToServer.writeUTF("");
                            streamToServer.flush();
                        }
                        System.out.println(returned);
                    } else {

                    }
                }
            } else {
                System.out.println("Неправильный формат ввода!!!.");
                streamToServer.writeUTF("wrong");
                streamToServer.flush();
            }
        }
    }


    /**
     * Считает количество вхождений символа в строке. Работает на основе замены во всей строке символа методом String.replaceAll(String, String) и нахождения разности длины с начальной строкой.
     * @param string входная строка
     * @param character символ для подсчёта
     * @return Количество вхождений символа в строку
     * @see String#replaceAll(String, String)
     */
    public static int charCounter(String string, Character character){
        String regex = "\\" + character;
        regex = regex.replaceAll("\\s+", "");
        return string.length() - string.replaceAll(regex, "").length();
    }

}
