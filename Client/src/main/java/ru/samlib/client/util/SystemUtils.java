package ru.samlib.client.util;

import android.util.Log;
import ru.samlib.client.R;

import java.io.*;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: Rufim
 * Date: 16.11.13
 * Time: 19:53
 * To change this template use File | Settings | File Templates.
 */
public class SystemUtils {

    private static final String TAG = SystemUtils.getClassName();

    private static final String[] FILE_SYSTEM_UNSAFE = {"/", "\\", "..", ":", "\"", "?", "*", "<", ">"};
    private static final String[] FILE_SYSTEM_UNSAFE_DIR = {"\\", "..", ":", "\"", "?", "*", "<", ">"};

    public static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException x) {
            Log.w(TAG, "Interrupted from sleep.", x);
        }
    }

    public static String getClassName() {
        try {
            String fullClassName = new Exception().getStackTrace()[1].getClassName();
            return fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
        } catch (Exception ex) {
            return "UnknownClass";
        }
    }

    public static Object round(Object value, int places) {
        // if (places < 0) throw new IllegalArgumentException();
        if (places < 0 || !(value instanceof Double)) {
            return value;
        }
        BigDecimal bd = new BigDecimal((Double) value);
        bd = bd.setScale(places, BigDecimal.ROUND_HALF_UP);
        return bd.doubleValue();
    }

    public static ByteBuffer toBuffer(InputStream inputStream) throws IOException {
        ByteBuffer resultBuffer = ByteBuffer.allocate(inputStream.available());
        int bufferLength = 28800;
        byte[] buffer = new byte[bufferLength];
        while (inputStream.read(buffer) != -1) {
            resultBuffer.put(buffer);
        }
        return resultBuffer;
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024 * 32];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    public static void copy(File src, File dst) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dst);
            copy(in, out);
        } catch (IOException ex) {
            throw ex;
        } finally {
            close(in);
            close(out);
        }
    }

    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        copy(input, output);
        return output.toByteArray();
    }

    public static <T> T[] concat(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    public static byte[] concat(byte[] first, byte[] second) {
        byte[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    public static <T> boolean contains(final T v, final T ... array) {
        if (v == null) {
            for (final T e : array)
                if (e == null)
                    return true;
        } else {
            for (final T e : array)
                if (e == v || v.equals(e))
                    return true;
        }

        return false;
    }

    public static void directoryMove(File sourceLocation, File targetLocation, Pattern pattern) throws IOException {
        if (sourceLocation == null || !sourceLocation.exists()) {
            return;
        }
        if (sourceLocation.isDirectory()) {
            String[] children = sourceLocation.list();
            for (int i = 0; i < children.length; i++) {
                File sourceChildren = new File(sourceLocation, children[i]);
                File targetChildren = new File(targetLocation, children[i]);
                if ((pattern == null || pattern.matcher(children[i]).matches()) && !isSubDirectory(sourceChildren, targetChildren)) {
                    directoryMove(new File(sourceLocation, children[i]), targetLocation, pattern);
                }
            }
            sourceLocation.delete();
        } else if ((pattern == null || pattern.matcher(sourceLocation.getName()).matches())) {
            sourceLocation.delete();
        }
    }

    public static void directoryDelete(File sourceLocation, Pattern pattern) throws IOException {
        if (sourceLocation == null || !sourceLocation.exists()) {
            return;
        }
        if (sourceLocation.isDirectory()) {
            String[] children = sourceLocation.list();
            for (int i = 0; i < children.length; i++) {
                if ((pattern == null || pattern.matcher(children[i]).matches())) {
                    directoryDelete(new File(sourceLocation, children[i]), pattern);
                }
            }
            sourceLocation.delete();
        } else if ((pattern == null || pattern.matcher(sourceLocation.getName()).matches())) {
            sourceLocation.delete();
        }
    }

    public static long directorySize(File sourceLocation, Pattern pattern) throws IOException {
        long size = 0;
        if (sourceLocation == null || !sourceLocation.exists()) {
            return size;
        }
        if (sourceLocation.isDirectory()) {
            String[] children = sourceLocation.list();
            for (int i = 0; i < children.length; i++) {
                size += directorySize(new File(sourceLocation, children[i]), pattern);
            }
            return size;
        } else if ((pattern == null || pattern.matcher(sourceLocation.getName()).matches())) {
            return sourceLocation.length();
        } else {
            return 0;
        }
    }

    public static boolean isSubDirectory(File sourceLocation, File targetLocation) {
        return Pattern.matches(sourceLocation.getAbsolutePath() + ".*", targetLocation.getAbsolutePath());
    }

    /**
     * Makes a given filename safe by replacing special characters like slashes ("/" and "\")
     * with dashes ("-").
     *
     * @param filename The filename in question.
     * @return The filename with special characters replaced by hyphens.
     */
    private static String fileSystemSafe(String filename) {
        if (filename == null || filename.trim().length() == 0) {
            return "unnamed";
        }

        for (String s : FILE_SYSTEM_UNSAFE) {
            filename = filename.replace(s, "-");
        }
        return filename;
    }

    /**
     * Makes a given filename safe by replacing special characters like colons (":")
     * with dashes ("-").
     *
     * @param path The path of the directory in question.
     * @return The the directory name with special characters replaced by hyphens.
     */
    private static String fileSystemSafeDir(String path) {
        if (path == null || path.trim().length() == 0) {
            return "";
        }

        for (String s : FILE_SYSTEM_UNSAFE_DIR) {
            path = path.replace(s, "-");
        }
        return path;
    }

    /**
     * Similar to {@link java.io.File#listFiles()}, but returns a sorted set.
     * Never returns {@code null}, instead a warning is logged, and an empty set is returned.
     */
    public static SortedSet<File> listFiles(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            Log.w(TAG, "Failed to list children for " + dir.getPath());
            return new TreeSet<File>();
        }

        return new TreeSet<File>(Arrays.asList(files));
    }


    /**
     * Returns the base name (the substring before the last dot) of the given file. The dot
     * is not included in the returned basename.
     *
     * @param name The filename in question.
     * @return The base name, or an empty string if no basename is found.
     */
    public static String getBaseName(String name) {
        int index = name.lastIndexOf('.');
        return index == -1 ? name : name.substring(0, index);
    }


    public static void close(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Exception x) {
            // Ignored
        }
    }

    public static void interrupt(Thread thread) {
        try {
            if (thread != null) {
                thread.interrupt();
            }
        } catch (Exception x) {
            // Ignored
        }
    }

    public static boolean delete(File file) {
        if (file != null && file.exists()) {
            if (!file.delete()) {
                Log.w(TAG, "Failed to delete file " + file);
                return false;
            }
            Log.i(TAG, "Deleted file " + file);
        }
        return true;
    }

    public static String getSize(long size) {
        String hrSize = "";
        double m = size / (1024.0 * 1024.0);
        DecimalFormat dec = new DecimalFormat("0.00");

        if (m > 1) {
            hrSize = dec.format(m).concat(" Мб");
        } else {
            hrSize = dec.format(size).concat(" Кб");
        }
        return hrSize;
    }

    public static void execute(String command) {
        try {
            // Preform su to get root privledges
            Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            Log.i(TAG,e.getLocalizedMessage());
        }
    }

    public interface IfNotNull<R,T> {
        R function(T ... obj);
    }

    public static <T, R> R nn(IfNotNull<R, T> exec, T ... obj) {
        for (T t : obj) {
            if(t == null) {
                return null;
            }
        }
        return exec.function(obj);
    }

    public interface IfNotNullNoArgs {
        void function();
    }

    public static <T, R> void nn(IfNotNullNoArgs exec, T... obj) {
        for (T t : obj) {
            if (t == null) {
                return;
            }
        }
        exec.function();
    }

    public static int parseInt(String intValue) {
        if(intValue == null) return -1;
        try {
            return Integer.parseInt(intValue);
        } catch (NumberFormatException ex) {
            return -1;
        }

    }

    public static <T extends Enum<T>> T parseEnum(String value, Class<T> enumType) {
        if (value != null && !value.isEmpty() && enumType != null) {
            try {
                return Enum.valueOf(enumType, value);
            } catch (IllegalArgumentException ex) {
                return null;
            }
        } else {
            return null;
        }
    }

}
