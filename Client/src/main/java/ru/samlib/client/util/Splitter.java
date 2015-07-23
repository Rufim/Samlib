package ru.samlib.client.util;

import android.annotation.SuppressLint;
import android.util.Log;
import org.intellij.lang.annotations.Language;
import ru.samlib.client.net.CachedResponse;

import java.io.*;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Rufim on 04.07.2015.
 */
@SuppressLint("NewApi")
public class Splitter {
    public Queue<String> start = new ArrayDeque<>();
    public Queue<String> end = new ArrayDeque<>();
    public Integer skipStart = 0;
    public Integer skipEnd = 0;
    public int flag = 0;

    public Splitter() {
    }

    public Splitter(@Language("RegExp") String start, @Language("RegExp") String end) {
        this.start.add(start);
        this.end.add(end);
    }

    public Splitter(@Language("RegExp") String start, @Language("RegExp") String end, Integer skipStart, Integer skipEnd) {
        this.start.add(start);
        this.end.add(end);
        this.skipStart = skipStart;
        this.skipEnd = skipEnd;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public Splitter addStart(@Language("RegExp") String start) {
        this.start.add(start);
        return this;
    }

    public Splitter addEnd(@Language("RegExp") String end) {
        this.end.add(end);
        return this;
    }

    public Splitter setSkipStart(Integer skip_start) {
        this.skipStart = skip_start;
        return this;
    }

    public Splitter setSkipEnd(Integer skip_end) {
        this.skipEnd = skip_end;
        return this;
    }

    public Pattern nextStart() {
        if (!start.isEmpty()) {
            return Pattern.compile(start.remove(), flag);
        }
        return null;
    }


    public Pattern nextEnd() {
        if (!end.isEmpty()) {
            return Pattern.compile(end.remove(), flag);
        }
        return null;
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
                while (matcher.find()) {
                    if (splitter.skipStart-- > 0) continue;
                    if (notInclude) index = matcher.end();
                    else index = matcher.start();
                    line = line.substring(index);
                    break;
                }
            }
            String nextLine = line;
            while ((end = splitter.nextEnd()) != null) {
                matcher = end.matcher(line);
                while (matcher.find()) {
                    if (splitter.skipEnd-- > 0) continue;
                    if (notInclude) index = matcher.start();
                    else index = matcher.end();
                    if (splitter.end.isEmpty()) {
                        nextLine = line.substring(0, index);
                        line = line.substring(index);
                    }
                    break;
                }
            }
            parts[i++] = nextLine;
        }
        return parts;
    }

    public static String[] extractLines(CachedResponse source, boolean notInclude, Splitter... splitters) {
        try {
            if (source != null)
                return extractLines(new FileInputStream(source), source.getEncoding(), notInclude, splitters);
        } catch (FileNotFoundException e) {
            Log.e(ParserUtils.TAG, "File not found");
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
                            if (splitter.skipStart-- > 0) {
                                matchedStart = false;
                            } else {
                                start = splitter.nextStart();
                                if (notInclude) continue;
                            }
                        }
                    }
                    if (matchedStart) {
                        matchedEnd = end != null && end.matcher(line).find();
                        if (matchedEnd) {
                            end = splitter.nextEnd();
                            if (splitter.skipEnd-- > 0) matchedEnd = false;

                        }
                        if (matchedEnd) {
                            if (!notInclude) builder.append(line + "\n");
                            break;
                        } else {
                            builder.append(line + "\n");
                        }
                    }
                }
                parts[i++] = builder.toString();
            }
        } catch (Exception e) {
            Log.e(ParserUtils.TAG, "File not parsable");
            Log.w(ParserUtils.TAG, e);
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
            Log.e(ParserUtils.TAG, "File not parsable");
            Log.w(ParserUtils.TAG, e);
        }
        return builder.toString();
    }
}

