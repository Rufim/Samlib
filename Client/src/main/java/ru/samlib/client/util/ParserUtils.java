package ru.samlib.client.util;

import android.util.Log;
import org.jsoup.Jsoup;
import ru.samlib.client.domain.Splitter;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.net.CachedResponse;

import java.io.*;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Rufim on 04.07.2015.
 */
public class ParserUtils {

    protected static final String TAG = ParserUtils.class.getSimpleName();

    public static String trim(String string) {
        return string.replaceAll("^\\s+|\\s+$", "");
    }

    public static Date parseData(String text) {
        Calendar calendar = Calendar.getInstance();
        if (text.contains(":")) {
            String[] time = text.split(":");
            int hours = Integer.parseInt(time[0]);
            calendar.set(Calendar.MINUTE, Integer.parseInt(time[1]));
            calendar.set(Calendar.HOUR_OF_DAY, hours);
            return calendar.getTime();
        } else if (text.contains("/")) {
            String[] date = text.split("/");
            if (date.length == 3) {
                calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(date[0]));
                calendar.set(Calendar.MONTH, Integer.parseInt(date[1]) - 1);
                calendar.set(Calendar.YEAR, Integer.parseInt(date[2]));
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                return calendar.getTime();
            } else if (date.length == 2) {
                calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(date[0]));
                calendar.set(Calendar.MONTH, Integer.parseInt(date[1]) - 1);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                return calendar.getTime();
            }
        }
        return null;
    }

    public static Work parseWork(CachedResponse file) {
        Work work = new Work();
        String[] parts = extractLines(file, true,
                new Splitter().addEnd("Первый блок ссылок"),
                new Splitter("Блок описания произведения", "Кнопка вызова Лингвоанализатора"),
                new Splitter().addStart("Блочек голосования").addStart("<!-------.*").addEnd("Собственно произведение"),
                new Splitter().addEnd("<!-------.*"));
        work.setTitle(Jsoup.parseBodyFragment(parts[0]).select("center > h2").text());
        String description = Jsoup.parseBodyFragment(parts[1]).select("ul li").get(2).text();
        String [] data = extractString(description, true,
                new Splitter(" ", ","),
                new Splitter(" ", "\\."),
                new Splitter(" ", "k"));
        work.setCreateDate(parseData(data[0]));
        work.setUpdateDate(parseData(data[1]));
        work.setSize(Integer.parseInt(data[2]));
        work.setDescription(Jsoup.parseBodyFragment(parts[2]).select("center > h2").text());
        work.setRawContent(parts[3]);
        return work;
    }

    public static String[] extractString(final String source, boolean notInclude, Splitter... splitters) {
        String[] parts = new String[splitters.length];
        int i = 0;
        String line = new String(source);
        for (Splitter splitter : splitters) {
            Pattern start;
            Pattern end;
            int index;
            Matcher matcher;
            while ((start = splitter.nextStart()) != null) {
                matcher = start.matcher(line);
                if(matcher.find()) {
                    if (notInclude) index = matcher.end();
                    else index = matcher.start();
                    line = line.substring(index);
                }
            }
            String nextLine = line;
            while ((end = splitter.nextEnd()) != null) {
                matcher = end.matcher(line);
                if (matcher.find()) {
                    if (notInclude) index = matcher.start();
                    else index = matcher.end();
                    if (splitter.end.isEmpty()) {
                        nextLine = line.substring(0, index);
                        line = line.substring(index);
                    }
                }
            }
            parts[i++] = nextLine;
        }
        return parts;
    }

    public static String[] extractLines(CachedResponse source, boolean notInclude, Splitter... splitters) {
        try {
            if(source != null)
            return extractLines(new FileInputStream(source), source.getEncoding(), notInclude, splitters);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found");
            Log.w(FileNotFoundException.class.getSimpleName(), e);
        }
        return new String[0];
    }

    public static String[] extractLines(InputStream source, String encoding, boolean notInclude, Splitter... splitters) {
        String[] parts = new String[splitters.length];
        try (final InputStream is = source;
             final InputStreamReader isr = new InputStreamReader(is, encoding);
             final BufferedReader reader = new BufferedReader(isr)) {
            int i = 0;
            for (Splitter splitter : splitters) {
                Pattern start = splitter.nextStart();
                Pattern end = splitter.nextEnd();
                StringBuilder builder = new StringBuilder();
                boolean matchedStart = start == null;
                boolean matchedEnd;
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!matchedStart) {
                        matchedStart = start.matcher(line).find();
                        if (matchedStart) {
                            start = splitter.nextStart();
                            if (splitter.skip_start-- > 0) matchedStart = false;
                            if (notInclude) continue;
                        }
                    }
                    if (matchedStart) {
                        matchedEnd = end != null && end.matcher(line).find();
                        if (matchedEnd) {
                            end = splitter.nextEnd();
                            if (splitter.skip_end-- > 0) matchedEnd = false;

                        }
                        if (matchedEnd ) {
                            if(!notInclude) builder.append(line + "\n");
                            break;
                        } else {
                            builder.append(line + "\n");
                        }
                    }
                }
                parts[i++] = builder.toString();
            }
        } catch (Exception e) {
            Log.e(TAG, "File not parsable");
            Log.w(TAG, e);
        }
        return parts;
    }

    public static String extractLines(CachedResponse file, boolean notInclude, String startReg, String endReg) {
        Pattern start = Pattern.compile(startReg, Pattern.CASE_INSENSITIVE);
        Pattern end = Pattern.compile(endReg, Pattern.CASE_INSENSITIVE);
        final StringBuilder builder = new StringBuilder();
        boolean matchedStart = false;
        boolean matchedEnd;
        try (final InputStream is = new FileInputStream(file);
             final InputStreamReader isr = new InputStreamReader(is, file.getEncoding());
             final BufferedReader reader = new BufferedReader(isr)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!matchedStart) {
                    matchedStart = start.matcher(line).find();
                    if (notInclude) continue;
                }
                if (matchedStart) {
                    matchedEnd = end.matcher(line).find();
                    if (notInclude && matchedEnd) break;
                    builder.append(line);
                    if (matchedEnd) break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "File not parsable");
            Log.w(TAG, e);
        }
        return builder.toString();
    }

}
