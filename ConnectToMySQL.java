package com.simbirsoft;


import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.List;
import java.util.Map;

public class ConnectToMySQL {
    private static final Logger logger = Logger.getLogger(ConsoleIntarface.class);
    //  Запрос на создание таблицы если она не создана
    private static final String CREATE_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS result_parser (" +
            " id INT(11) NOT NULL AUTO_INCREMENT," +
            " url TEXT NOT NULL," +
            " tag VARCHAR(30) NOT NULL," +
            " word VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL," +
            " appearance INT(11) NOT NULL," +
            " time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            " user_info VARCHAR(100)," +
            " PRIMARY KEY (id)" +
            ")";

    //  Запрос на запись данных в бд
    private static final String INSERT_QUERY = "INSERT INTO result_parser (url, tag, word, appearance, time, user_info) VALUES (?, ?, ?, ?, ?, ?)";

    //  Размер для batch-оперций
    private static final int BATCH_SIZE = 1000;

    //  Обявление Connection
    private static Connection connection;

    // Метод возвращает имя бд если есть подключение и "None" если подключения нет
    public static String nameConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                return connection.getCatalog();
            } else {
                return "None";
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }


    //  Подключаемся к бд
    public static void connect_to_database(String hostname, String port, String databaseName, String username, String password) throws SQLException {
        String dbURI = "jdbc:mysql://" + hostname + ":" + port + "/" + databaseName + "?rewriteBatchedStatements=true";

        try {
            connection = DriverManager.getConnection(dbURI, username, password);
        } catch (Exception ex) {
            System.out.println("Ошибка подключения");
        }
    }


    //  Получение информации о пользователе,
    private static String getUserInfo() {
        try {
            InetAddress address = InetAddress.getLocalHost();
            String currentComputerName = address.getHostName();
            String currentComputerIP = address.getHostAddress();
            return currentComputerIP + " - " + currentComputerName;
        } catch (UnknownHostException ex) {
            System.err.println("Не удалось получить имя и IP текущего компьютера: " + ex.getMessage());
            return "";
        }
    }

    //  Делаем запрос в бд на создание таблицы
    private static void createTableIfNotExist() {
        try (Statement statement = connection.createStatement()) {
            statement.execute(CREATE_TABLE_QUERY);
        }
        catch (Exception ex) {
            logger.error(ex);
        }
    }


    //  Разбираем статистику и вставляем в выражение, чтобы отправить пакет запросов в бд
    public static void insertWordsMap(Map<String, List<Map.Entry<String, Integer>>> fullStatistic, String Url) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(INSERT_QUERY)) {
            String userInfo = getUserInfo();
            createTableIfNotExist();

            int count = 0;
            for (Map.Entry<String, List<Map.Entry<String, Integer>>> entry : fullStatistic.entrySet()) {
                String tag = entry.getKey();
                List<Map.Entry<String, Integer>> wordList = entry.getValue();

                for (Map.Entry<String, Integer> wordEntry : wordList) {
                    String currentWord = wordEntry.getKey();
                    Integer currentAppearance = wordEntry.getValue();

                    preparedStatement.setString(1, Url);              // url
                    preparedStatement.setString(2, tag);              // tag
                    preparedStatement.setString(3, currentWord);      // word
                    preparedStatement.setInt(4, currentAppearance);   // appearance
                    preparedStatement.setTimestamp(5, new Timestamp(System.currentTimeMillis())); // time
                    preparedStatement.setString(6, userInfo);         // user_info
                    preparedStatement.addBatch();

                    if (++count % BATCH_SIZE == 0) {
                        executeBatch(preparedStatement);
                    }
                }
            }
            executeBatch(preparedStatement);
        }
        catch (Exception ex) {
            String errorMessage = "Error occurred while executing query: " + ex.getMessage();
            logger.error(errorMessage, ex);
        }
    }


    //  Отправляем пакет запросов
    private static void executeBatch(PreparedStatement preparedStatement) throws SQLException {
        int[] result = preparedStatement.executeBatch();
        for (int i : result) {
            if (i == Statement.EXECUTE_FAILED) {
                throw new SQLException("Failed to execute batch insert statement.");
            }
        }
    }


    //  Закрываем соединение
    public static void close_connection() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

}

