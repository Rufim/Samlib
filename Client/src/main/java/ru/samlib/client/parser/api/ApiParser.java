package ru.samlib.client.parser.api;

import android.annotation.SuppressLint;
import net.vrallev.android.cat.Cat;
import ru.kazantsev.template.util.TextUtils;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.Genre;
import ru.samlib.client.domain.entity.Type;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ApiParser {
    private static final String TAG = "Parser";

    private final static SimpleDateFormat dateTimeFormat = new SimpleDateFormat(Constants.Pattern.DATA_ISO_8601_24H);
    private final static SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.Pattern.DATA_PATTERN_LOG);
    private final static SimpleDateFormat dateFormatDiff = new SimpleDateFormat(Constants.Pattern.DATA_PATTERN_DIFF);

    private final static Pattern title = Pattern.compile("\\s*<title>Журнал &quot;Самиздат&quot;: Статистика: (.+), (.+)</title>\\s*");
    private final static Pattern linkAndName = Pattern.compile("<a href=(.+)>(.+)</a></td>");
    private final static Pattern views = Pattern.compile("<td>(\\d+)</td><td>(\\d+)</td>");


    public interface ParseDelegate {
        DataCommand parseLine(String line, ApiParser parser);
    }

    public static AReaderDelegate getAReaderDelegateInstance() {
        return new AReaderDelegate();
    }

    public static LogDelegate getLogDelegateInstance() {
        return new LogDelegate();
    }

    @SuppressLint("NewApi")
    public List<DataCommand> parseInput(InputStream inputStream, ParseDelegate delegate) {
        if (delegate == null || inputStream == null) throw new IllegalArgumentException("Nulls not accepted");
        ArrayList<DataCommand> dataCommands = new ArrayList<>();
        try (final InputStream is = inputStream;
             final InputStreamReader isr = new InputStreamReader(is, "CP1251");
             final BufferedReader reader = new BufferedReader(isr)) {
            String line = "";
            StringBuilder builder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                if (delegate.getClass() == LogDelegate.class) {
                    if (line.charAt(0) == '/') {
                        addLine(builder.toString(), dataCommands, delegate);
                        builder = new StringBuilder();
                    }
                    builder.append(line);
                } else {
                    addLine(line, dataCommands, delegate);
                }
            }
            if (delegate.getClass() == LogDelegate.class) addLine(builder.toString(), dataCommands, delegate);
        } catch (Exception e) {
            Cat.e(e, "Error while read log input");
        }
        return dataCommands;
    }

    @SuppressLint("NewApi")
    public static Map<String, String> parseStat(InputStream inputStream) throws IOException {
        if (inputStream == null) throw new IllegalArgumentException("Nulls not accepted");
        Map<String, String> stat = new LinkedHashMap<>();
        boolean titleParsed = false;
        try (final InputStream is = inputStream;
             final InputStreamReader isr = new InputStreamReader(is, "CP1251");
             final BufferedReader reader = new BufferedReader(isr)) {
            String line = "";
            while ((line = reader.readLine()) != null) {
                Matcher matcher = linkAndName.matcher(line);
                if (matcher.matches()) {
                    String link = matcher.group(1);
                    if ((line = reader.readLine()) != null && line.isEmpty()) {
                        line = reader.readLine();
                        if (line != null && (matcher = views.matcher(line)).matches()) {
                            stat.put(link, Integer.parseInt(matcher.group(1)) + "");
                        }
                    }
                } else if (titleParsed && (matcher = title.matcher(line)).matches()) {
                    stat.put("title", matcher.group(1));
                    stat.put("about", matcher.group(2));
                    titleParsed = true;
                }
            }
        }
        return stat;
    }

    private void addLine(String fullLine, List<DataCommand> dataCommands, ParseDelegate delegate) {
        if (TextUtils.notEmpty(fullLine)) {
            fullLine = TextUtils.trim(fullLine.replace('\0', '0'));
            if (fullLine.endsWith("k") || fullLine.endsWith("|")) {
                DataCommand command = delegate.parseLine(fullLine, this);
                if (command != null) dataCommands.add(command);
            } else {
                Cat.e("Invalid line " + fullLine);
            }
        }
    }


    public static class AReaderDelegate implements ParseDelegate {

        private AReaderDelegate() {
        }

        ;

        //      0       1      2     3      4        5        6    7     8
        //    линк  | author|title|type|size kb|create date|rate|votes|annot|
        // s/saharow_w_i/inter-6|Сахаров Василий Иванович|Интервью с Владимиром Чихиревым.|Интервью|4|19/12/2015|3.20|31|Короткое интервью с автором СИ Владимиром Чихаревым.|
        public DataCommand parseLine(String line, ApiParser parser) {
            if (TextUtils.notEmpty(line)) {
                try {

                    String[] fields = line.split("\\|", 9);
                    if (fields.length != 9) {
                        Cat.e("Invalid areader line " + line);
                        return null;
                    }
                    if (Type.parseType(fields[3]) == Type.OTHER) {
                        ArrayList<String> fieldsArray = new ArrayList<>();
                        String lines[];
                        fieldsArray.add((lines = line.split("\\|", 2))[0]);
                        fieldsArray.add((lines = lines[1].split("\\|", 2))[0]);
                        StringBuilder builder = new StringBuilder();
                        lines = lines[1].split("\\|", 2);
                        builder.append(lines[0]);
                        lines = lines[1].split("\\|", 2);
                        while (Type.parseType(lines[0]) == Type.OTHER) {
                            builder.append("|" + lines[0]);
                            lines = lines[1].split("\\|", 2);
                            if (lines.length < 2) {
                                throw new Exception("invalid format exception!");
                            }
                        }
                        fieldsArray.add(builder.toString());
                        fieldsArray.add(lines[0]);
                        fieldsArray.addAll(Arrays.asList(lines[1].split("\\|", 5)));
                        fields = fieldsArray.toArray(new String[9]);
                    }
                    DataCommand dataCommand = new DataCommand();
                    dataCommand.setLink("/" + fields[0]);
                    dataCommand.setCommand(Command.ARD);
                    dataCommand.setAuthorName(fields[1]);
                    dataCommand.setTitle(fields[2]);
                    dataCommand.setCommandDate(new Date());
                    dataCommand.setType(Type.parseType(fields[3]));
                    dataCommand.setGenre(Genre.EMPTY);
                    if (TextUtils.notEmpty(fields[4].trim())) dataCommand.setSize(Integer.parseInt(fields[4]));
                    try {
                        if (TextUtils.notEmpty(fields[5].trim()))
                            dataCommand.setCreateDate(fields[5].contains("/") ? dateFormat.parse(fields[5]) : dateFormatDiff.parse(fields[5]));
                    } catch (ParseException ex) {
                        dataCommand.setCreateDate(null);
                    }
                    if (TextUtils.notEmpty(fields[6].trim())) dataCommand.setRate(new BigDecimal(fields[6]));
                    if (TextUtils.notEmpty(fields[7].trim())) dataCommand.setVotes(Integer.parseInt(fields[7]));
                    dataCommand.setAnnotation(fields[8]);
                    return dataCommand;
                } catch (Exception ex) {
                    Cat.e(ex, line);
                }
            }
            return null;
        }

    }


    private static class LogDelegate implements ParseDelegate {

        private LogDelegate() {
        }

        ;

        //     0         1            2             3     4      5    6    7        8         9       10             11
        //    линк  |тег oперации|таймштамп-MySQL|title|author|type|janr|annot|create date|img_cnt|update-unixtime|size kb
        // /m/maksimowa_alina/dymchatyjsiluettebja|EDT|2015-06-30 20:16:07|Дымчатый силуэт тебя|Максимова Алина|Роман|Фантастика|       Новенький баскетболист в университете произвёл на Софию особое впечатление. Да и Дашке он приглянулся. Что делать, |02/06/2015|1|1433273100|695k
        public DataCommand parseLine(String line, ApiParser parser) {
            if (TextUtils.notEmpty(line)) {
                try {
                    String[] fields = line.split("\\|", -1);
                    if (fields.length != 12) {
                        Cat.e("Invalid log line" + line);
                        return null;
                    }
                    DataCommand dataCommand = new DataCommand();
                    dataCommand.setLink(fields[0]);
                    String command = fields[1];
                    String title = fields[3];
                    if (TextUtils.notEmpty(fields[1])) {
                        try {
                            if (command.length() > 3) {
                                dataCommand.setCommand(Command.valueOf(fields[1].substring(0, 3)));
                                title = fields[1].substring(4, fields[1].length() - 1);
                            } else {
                                dataCommand.setCommand(Command.valueOf(fields[1]));
                            }
                        } catch (IllegalArgumentException ex) {
                            Cat.w("Invalid command - " + fields[1] + " " + line);
                        }
                    } else {
                        Cat.w("Empty command " + line);
                    }
                    dataCommand.setTitle(title);
                    if (TextUtils.notEmpty(fields[2].trim()))
                        dataCommand.setCommandDate(dateTimeFormat.parse(fields[2]));
                    dataCommand.setAuthorName(fields[4]);
                    dataCommand.setType(Type.parseType(fields[5]));
                    dataCommand.setGenre(Genre.parseGenre(fields[6]));
                    dataCommand.setAnnotation(fields[7]);
                    try {
                        if (TextUtils.notEmpty(fields[8].trim()))
                            dataCommand.setCreateDate(fields[8].contains("/") ? dateFormat.parse(fields[8]) : dateFormatDiff.parse(fields[8]));
                    } catch (ParseException ex) {
                        dataCommand.setCreateDate(null);
                    }
                    if (TextUtils.notEmpty(fields[9].trim())) dataCommand.setImageCount(Integer.parseInt(fields[9]));
                    if (TextUtils.notEmpty(fields[10].trim())) dataCommand.setUnixtime(Long.parseLong(fields[10]));
                    if (TextUtils.notEmpty(fields[11].trim()))
                        dataCommand.setSize(Integer.parseInt(fields[11].substring(0, fields[11].length() - 1)));
                    return dataCommand;
                } catch (Exception ex) {
                    Cat.e(ex, line);
                }
            }
            return null;
        }
    }
}
