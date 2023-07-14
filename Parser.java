package com.simbirsoft;


import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;


import java.util.*;

public class Parser {

    private static final Logger logger = Logger.getLogger(ConsoleIntarface.class);
    //  Некоторые юзерагенты которые будем пробовать при соединении (Например на LordFilm работают не все)
    private static final String[] userAgents = {
            "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:60.0) Gecko/20100101 Firefox/60.0",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.120 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.97 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36",
            "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:54.0) Gecko/20100101 Firefox/54.0"
    };


    //  Полная статистика со всеми тегами пользователя
    public static Map<String, List<Map.Entry<String, Integer>>> fullStatistic = new TreeMap<>();


    //  Массив тегов пользователя, ставим по умолчанию <body>
    private static ArrayList<String> userTags = new ArrayList<>();
    static { userTags.add("body"); }

    //  Возвращает теги пользователя
    public static ArrayList<String> getUserTags() {
        return userTags;
    }

    //  Массив тегов, служит для сопоставления с теми тегами, которые введёт пользователь
    //(если введёт неправильно то он не добавится в userTags)
    private static final String[] tags = {"a", "abbr", "title", "address", "area", "b", "base", "bdo", "blockquote"
            , "button", "caption", "cite", "code", "dd", "del", "dfn", "div", "dl", "dt", "em", "span", "form"
            , "head", "i", "ins", "kbd", "label", "legend", "li", "link", "meta", "ol", "p", "q"
            , "s", "script", "small", "var"};



    //  Метод, который осуществляет парсинг HTML страницы по указанному URL и выводит статистику слов
    public static Document parsing(String URL) {

        //  Создаём подключение, проходимся циклом по юзерагентам. Если подключение есть, то останавливаем цикл,
        //если же прошлись по всем юзерагентам, но подключения нет, то выводим соответствующую информацию
        Document doc = null;
        for (String userAgent : userAgents) {
            try {
                doc = Jsoup.connect(URL)
                        .userAgent(userAgent)
                        .referrer("https://www.yandex.ru/")
                        .timeout(10000)
                        .get();
                break;
            } catch (Exception e) {
                /*Next userAgent*/
            }
        }


        //  Если возникла ошибка, а именно: doc = null
        //тогда выводим соответствующую информацию, если же нет, то
        //передаём наш doc и тег/теги в getStatistic
        if (doc != null) {
            if (!fullStatistic.isEmpty()) {
                fullStatistic.clear();
            }
            for (String tag : userTags) {

                List<Map.Entry<String, Integer>> statisticTagOnly = getStatisticTagOnly(doc, tag);
                if (statisticTagOnly.isEmpty()){
                    System.out.println("\nВ теге <" + tag + "> ничего нет");
                }
                else {
                    System.out.println("\n<" + tag + ">\n");
                    printStatistic(statisticTagOnly);
                    fullStatistic.put(tag, statisticTagOnly);
                    System.out.println("\n<" + tag + ">\n");
                }

            }
            return doc;
        }

        return null;
    }

    //  Метод принимающий статистику слов и выводящий её на консоль
    public static void printStatistic(List<Map.Entry<String, Integer>> statistic) {
        for (Map.Entry<String, Integer> word : statistic) {
            System.out.println(word.getKey() + " - " + word.getValue());
        }
    }


    //  Удаляем дубликаты в тегах
    public static void removeDuplicatesTags() {
        HashSet<String> uniqueWords = new HashSet<>(userTags);
        userTags.clear();
        userTags.addAll(uniqueWords);
    }


    // Удаляем теги пользователя
    public static void deleteTags(String inputTags) {

        String[] splitInputTags =  inputTags.toLowerCase()
                .replaceAll("\\d+", "")
                .replaceAll("[^a-zA-Z]+", " ")
                .trim()
                .split("\\s+");

        for (String tag : splitInputTags) {
            if (userTags.contains(tag)) {
                userTags.remove(tag);
            }
        }

        if (userTags.isEmpty()) {
            setDefaultTag();
        }
        removeDuplicatesTags();
    }


    //  Добавляем теги пользователя
    public static void addTags(String inputTags) {

        String[] splitInputTags =  inputTags.toLowerCase()
                .replaceAll("\\d+", "")
                .replaceAll("[^a-zA-Z]+", " ")
                .trim()
                .split("\\s+");

        for (String tag : splitInputTags) {
            if (Arrays.asList(tags).contains(tag)) {
                userTags.add(tag);
            }
        }

        removeDuplicatesTags();
    }


    // Устанавливаем тег по умолчанию
    public static void setDefaultTag() {
        if (!userTags.isEmpty()) {
            userTags.clear();
        }

        userTags.add("body");

    }


    //  Метод, который извлекает текст из определённых тегов, разбивает его на слова,
    //присваивает каждому слову количество его вхождений в тексте
    //и сортирует их в порядке возрастания. Возвращает статистику.
    public static List<Map.Entry<String, Integer>> getStatisticTagOnly(Document doc, String tag) {

        //  Разбиваем теги и проверяем их на правильность написания

        Elements elements = doc.select(tag);
        String[] words = elements
                .text()
                .toUpperCase()
                .replaceAll("\\d+", "") //Удаляем цифры
                .replaceAll("[^а-яА-Яa-zA-Z]+", " ") //Заменяем всё кроме кириллицы и латиницы на пробел
                .trim() //Удаляем лишние пробелы вначале и конце строки
                .split("\\s+"); //Разбиваем строку на массив слов использовав пробел как разделитель

        Map<String, Integer> wordCounts = new TreeMap<>();
        for (String word : words) {
            if (!word.isEmpty()) {
                wordCounts.put(word, wordCounts.getOrDefault(word, 0) + 1);
            }
        }

        List<Map.Entry<String, Integer>> sortedWords = new ArrayList<>(wordCounts.entrySet());

        Comparator<Map.Entry<String, Integer>> valueComparator = Map.Entry.comparingByValue();

        sortedWords.sort(valueComparator);

        return sortedWords;
    }

}
