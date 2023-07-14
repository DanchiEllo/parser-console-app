package com.simbirsoft;

import org.apache.log4j.*;

import java.io.FileWriter;
import java.io.IOException;


public class Log4jConfig {

    public static void configure() {
        // Создание конфигурации
        Logger rootLogger = Logger.getRootLogger();
        rootLogger.setLevel(Level.DEBUG);

        // Создание аппендера файлового типа
        RollingFileAppender fileAppender = new RollingFileAppender();
        fileAppender.setName("LOGFILE");

        // Установка пути к файлу лога
        String logFilePath = System.getProperty("user.dir") + "\\logfile.log";
        try {
            fileAppender.setWriter(new FileWriter(logFilePath, true));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Добавление основного макета
        PatternLayout mainLayout = new PatternLayout();
        mainLayout.setConversionPattern("%p %d{yyyy-MM-dd HH:mm:ss.SSS} [%t] %l - %m%n");
        fileAppender.setLayout(mainLayout);

        // Добавление аппендера
        rootLogger.addAppender(fileAppender);
    }
}
