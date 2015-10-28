package ru.samlib.client.util;

import android.annotation.SuppressLint;
import android.util.Log;
import org.intellij.lang.annotations.RegExp;
import ru.samlib.client.net.CachedResponse;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Dmitry on 19.10.2015.
 */
public class TextUtils {

    // utl fll template: ((https?|ftp)\:\/\/)?([a-z0-9+!*(),;?&=\$_.-]+(\:[a-z0-9+!*(),;?&=\$_.-]+)?@)?([a-z0-9-.]*)\.([a-z]{2,5})(\:[0-9]{2,5})?(\/([a-z0-9+\$_-]\.?)+)*\/?(\?[a-z+&\$_.-][a-z0-9;:@&%=+\/\$_.-]*)?(#[a-z_.-][a-z0-9+\$_.-]*)?

    public static final String URL_REGEX
            = "((https?|ftp)\\:\\/\\/)?" // SCHEME
            + "([a-z0-9+!*(),;?&=\\$_.-]+(\\:[a-z0-9+!*(),;?&=\\$_.-]+)?@)?" // User and Pass
            + "([a-z0-9-.]*)\\.([a-z]{2,5})" // Host or IP
            + "(\\:[0-9]{2,5})?" // Port
            + "(\\/([a-z0-9+\\$_-]\\.?)+)*\\/?" // Path
            + "(\\?[a-z+&\\$_.-][a-z0-9;:@&%=+\\/\\$_.-]*)?" // GET Query
            + "(#[a-z_.-][a-z0-9+\\$_.-]*)?"; // Anchor
    public static final String SUSPICIOUS_BY_LINK = "\\w+\\.\\w+";
    public static final String OUTSIDE_TAGS = "(?![^<\"]*(>|\")|[^<>]*(<|\")\\/)";
    public static final Pattern suspiciousPattern = Pattern.compile(SUSPICIOUS_BY_LINK,
            Pattern.DOTALL | Pattern.UNIX_LINES | Pattern.CASE_INSENSITIVE);
    public static final Pattern urlPattern = Pattern.compile(URL_REGEX,
            Pattern.DOTALL | Pattern.UNIX_LINES | Pattern.CASE_INSENSITIVE);

    public static String trim(String string) {
        return string.replaceAll("^\\s+|\\s+$", "");
    }

    public static boolean isLink(String text, String scheme, String userAndPass, String hostOrIp, String path, String query, String anchor) {
        return isLink(text, scheme, userAndPass, hostOrIp, path, query, anchor, null);
    }

    public static boolean isLink(String text, String hostOrIp, String path) {
        return isLink(text, null, null, hostOrIp, path, null, null, null);
    }

    public static boolean isLink(String ... conditions) {
        String text = conditions[0];
        if(text == null) {
            return false;
        }
        Matcher matcher = urlPattern.matcher(text);
        boolean result = matcher.find();
        if(!result) return false;
        for (int i = 1; i < conditions.length; i++) {
            if(conditions[i] == null) {
                continue;
            }
            String group = matcher.group(i);
            if(group == null && !group.equals(conditions[i])) {
                result = false;
                break;
            }
        }
        return result;
    }

    public static boolean isEmpty(String string) {
        return string == null || string.length() == 0;
    }

    public static String linkify(String text) {
        if (!suspiciousPattern.matcher(text).find()) {
            return text;
        }
        Matcher matcher = urlPattern.matcher(text);
        StringBuffer s = new StringBuffer();
        while (matcher.find()) {
            String scheme = matcher.group(2);
            if (scheme != null) {
                matcher.appendReplacement(s, "<a href=\"$0\">$0</a>");
            } else {
                matcher.appendReplacement(s, "<a href=\"http://$0\">$0</a>");
            }
        }
        matcher.appendTail(s);
        return s.toString();
    }

    public static String linkifyHtml(String text) {
        if (!suspiciousPattern.matcher(text).find()) {
            return text;
        }
        Pattern pattern = Pattern.compile("(" + URL_REGEX + ")" + OUTSIDE_TAGS);
        Matcher matcher = pattern.matcher(text);
        StringBuffer s = new StringBuffer();
        while (matcher.find()) {
            String scheme = matcher.group(2);
            if (scheme != null) {
                matcher.appendReplacement(s, "<a href=\"$0\">$0</a>");
            } else {
                matcher.appendReplacement(s, "<a href=\"http://$0\">$0</a>");
            }
        }
        matcher.appendTail(s);
        return s.toString();
    }

    public static String cleanupSlashes(String link) {
        return link.replaceAll("/+", "/");
    }


    public static String replaceOutsideTags(String text, String template, String replacement) {
        return text.replaceAll("(" + template + ")" + OUTSIDE_TAGS, replacement);
    }

    public static boolean contains(String str, boolean in, String... strs) {
        for (String s : strs) {
            if (in) {
                if (s.contains(str)) return true;
            } else {
                if (str.contains(s)) return true;
            }
        }
        return false;
    }

    public static String putInString(String source, String placement, int gap) {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        while (i + gap < source.length()) {
            builder.append(source.substring(i, i + gap));
            builder.append(placement);
            i += gap;
        }
        builder.append(source.substring(i));
        return builder.toString();
    }

    public static List<Piece> searchAll(String source, @RegExp String template) {
        List<Piece> pieces = new ArrayList<>();
        Pattern pattern = Pattern.compile(template);
        Matcher matcher = pattern.matcher(source);
        while (matcher.find()) {
            pieces.add(new Piece(source, matcher));
        }
        return pieces;
    }

    public static class Piece {
        public String text;
        public int start;
        public int end;

        public Piece(){}

        public Piece(String source, Matcher matcher) {
            this.start = matcher.start();
            this.end = matcher.end();
            this.text = source.substring(start, end);
        }
        
        public Piece(String text, int start, int end) {
            this.text = text;
            this.start = start;
            this.end = end;
        }

        public Piece(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    /**
     * Created by Rufim on 04.07.2015.
     */
    @SuppressLint("NewApi")
    public static class Splitter {

        public Queue<String> start = new ArrayDeque<>();
        public Queue<String> end = new ArrayDeque<>();
        public Integer skipStart = 0;
        public Integer skipEnd = 0;
        public int flags = 0;

        public Splitter() {
        }

        public Splitter(@RegExp String start, @RegExp String end) {
            this.start.add(start);
            this.end.add(end);
        }

        public Splitter(@RegExp String start, @RegExp String end, Integer skipStart, Integer skipEnd) {
            this.start.add(start);
            this.end.add(end);
            this.skipStart = skipStart;
            this.skipEnd = skipEnd;
        }

        public void setFlags(int flags) {
            this.flags = flags;
        }

        public Splitter addStart(@RegExp String start) {
            this.start.add(start);
            return this;
        }

        public Splitter addEnd(@RegExp String end) {
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
                return Pattern.compile(start.remove(), flags);
            }
            return null;
        }


        public Pattern nextEnd() {
            if (!end.isEmpty()) {
                return Pattern.compile(end.remove(), flags);
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

        public static String extractLine(CachedResponse file, boolean notInclude, @RegExp String startReg, @RegExp String endReg) {
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

        public enum DelimiterMode {NONE, TO_END, FROM_START, SEPARATE}

        public static ArrayList<String> split(String str, @RegExp String delimiter, DelimiterMode mode) {
            Matcher matcher = Pattern.compile(delimiter).matcher(str);
            ArrayList<String> split = new ArrayList<>();
            int lastFound = 0;
            String previous = "";
            while (true) {
                if (matcher.find()) {
                    switch (mode) {
                        case NONE:
                            split.add(str.substring(lastFound, matcher.start()));
                            break;
                        case TO_END:
                            split.add(str.substring(lastFound, matcher.end()));
                            break;
                        case FROM_START:
                            split.add(previous + str.substring(lastFound, matcher.start()));
                            previous = str.substring(matcher.start(), matcher.end());
                            break;
                        case SEPARATE:
                            split.add(str.substring(lastFound, matcher.start()));
                            split.add(str.substring(matcher.start(), matcher.end()));
                            break;
                    }
                    lastFound = matcher.end();
                } else {
                    if (lastFound < str.length() - 1) {
                        split.add(str.substring(lastFound));
                    }
                    break;
                }
            }
            return split;
        }
    }
}
