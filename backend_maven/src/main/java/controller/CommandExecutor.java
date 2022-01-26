package controller;

import database.Book;
import database.DatabaseProcedures;

public class CommandExecutor {

    public String help(){
        return ("Допустимые команды: \n" +
                "\thelp - Выводит информацию о доступных командах и их формате.\n" +
                "\tchange_password - Сменить пароль.\n" +
                "\texit - Выполняет выход из программы.\n" +
                "\tadd {<file name>}: - Добавляет новую книгу в каталог.\n" +
                "\tfind {\"<field>\":\"<value>\"} - поиск по каталогу.\n");
    }

    public String add(String text) {

        if (Book.checkTextFormat(text)) {
            Book book = Book.convertFromStringToBook(text);
            if (book != null) {
                DatabaseProcedures.addBook(book);
                return "Successfully added.\n";
            } else {
                return "Cannot parse keywords.\n";
            }
        } else {
            return "Incorrect book format.\n";
        }

    }
}
