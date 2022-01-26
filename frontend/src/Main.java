/**
 * Класс, в котором создаётся коллекция и запускается работа с ней.
 * @see Client
 */

public class Main {
    public static void main(String[] args) {
        Client client = new Client();
        client.go(args);
    }
}
