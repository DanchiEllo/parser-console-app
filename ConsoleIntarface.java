package com.simbirsoft;

import org.jsoup.nodes.Document;

import org.apache.log4j.Logger;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Scanner;

public class ConsoleIntarface {

    private static final Logger logger = Logger.getLogger(ConsoleIntarface.class);

    static {
        File logFile = new File(System.getProperty("user.dir") + "\\logfile.log");
        try {
            if (logFile.createNewFile()) {
                System.out.println("Файл logfile.log успешно создан.");
            }
        } catch (IOException e) {
            System.out.println("Ошибка при создании файла logfile.log: " + e.getMessage());
        }
    }


    //  Общий scanner дабы не объявлять его снова и снова
    private static final Scanner scanner = new Scanner(System.in);


    //  Общий input для выбора действий пользователя
    private static int input;


    //  Основной метод главного меню
    public static void startMenu() {

        while (true) {
            System.out.println("\n\n\nПодключено к бд: " + ConnectToMySQL.nameConnection());
            System.out.println("Выбранные теги: " + Parser.getUserTags());
            System.out.println("1 - Парсить");

            if (ConnectToMySQL.nameConnection().equals("None")) {
                System.out.println("2 - Подключиться к бд (MySQL)");
            } else {
                System.out.println("2 - Закрыть соединение с бд");
            }

            System.out.println("3 - Управление тегами (По умолчанию <body>)");
            System.out.println("0 - Выход");
            System.out.println("Выберите действие: ");

            String inputStr = scanner.nextLine();

            if (inputStr.matches("[0-4]")) {
                input = Integer.parseInt(inputStr);

                switch (input) {
                    case 1:

                        String URL;

                        do {
                            System.out.print("Введите URL веб страницы: ");
                            URL = scanner.nextLine().trim();
                        } while (URL.isEmpty());

                        Document doc = Parser.parsing(URL);

                        if (doc == null) {
                            System.out.println("\nНеверный URL\n");
                        } else if (!Parser.fullStatistic.isEmpty() && !Objects.equals(ConnectToMySQL.nameConnection(), "None")) {
                            System.out.println("Желаете сохранить статистику в базу данных?");

                            while (true) {
                                System.out.println("(Y/n)?");
                                String answer = scanner.nextLine().toLowerCase().trim();

                                if (answer.equals("y")) {
                                    ConnectToMySQL.insertWordsMap(Parser.fullStatistic, URL);
                                    break;
                                } else if (answer.equals("n")) {
                                    break;
                                } else {
                                    System.out.println("Некорректный ввод!");
                                }
                            }
                        }
                        break;
                    case 2:
                        connectToDatabaseMenu();
                        break;
                    case 3:
                        managementTagsMenu();
                        break;
                    case 0:
                        try {
                            ConnectToMySQL.close_connection();
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }

                        System.exit(0);
                        break;
                }
            } else {
                System.out.println("Некорректный ввод!");
            }
        }
    }


    //  Меню управления тегами
    public static void managementTagsMenu() {

        while (true) {
        System.out.println("\n\n\nВыбранные теги: " + Parser.getUserTags());
        System.out.println("1 - Добавить теги");
        System.out.println("2 - Удалить теги");
        System.out.println("3 - Установить по умолчанию (<body>)");
        System.out.println("4 - Назад");
        System.out.println("Выберите действие: ");

            String inputStr = scanner.nextLine();

            if (inputStr.matches("[1-4]")) {
                input = Integer.parseInt(inputStr);
                String tags;

                switch (input) {
                    case 1:
                        System.out.println("Напишите желаемые теги через пробел");
                        tags = scanner.nextLine().trim();
                        Parser.addTags(tags);

                        break;
                    case 2:
                        System.out.println("Напишите теги которые хотите удалить через пробел");
                        tags = scanner.nextLine().trim();
                        Parser.deleteTags(tags);

                        break;
                    case 3:
                        Parser.setDefaultTag();
                        break;
                    case 4:
                        return;
                }
            } else {
                System.out.println("Некорректный ввод!");
            }
        }

    }


    //  Меню подключения к бд
    public static void connectToDatabaseMenu() {

        if (ConnectToMySQL.nameConnection().equals("None")) {
            String[] inputs = new String[5]; // Массив для хранения всех введенных значений
            String[] prompts = {"Введите хост/ip: ", "Введите порт: ", "Введите имя базы данных: ", "Введите имя пользователя: ", "Введите пароль: "};

            for (int i = 0; i < inputs.length; i++) {
                String input = "";
                while (input.isEmpty()) {
                    System.out.print(prompts[i]);
                    input = scanner.nextLine();
                }
                inputs[i] = input;
            }

            String hostName = inputs[0];
            String port = inputs[1];
            String databaseName = inputs[2];
            String userName = inputs[3];
            String userPassword = inputs[4];

            try {
                ConnectToMySQL.connect_to_database(hostName, port, databaseName, userName, userPassword);
            } catch (SQLException e) {
                System.out.println("\nОшибка подключения\n");
            }
        } else {

            try {
                ConnectToMySQL.close_connection();
            } catch (SQLException e) {
                logger.error("Не удалось закрыть соединение", e);
            }

        }

    }


}

