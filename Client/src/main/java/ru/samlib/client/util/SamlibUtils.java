package ru.samlib.client.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.support.annotation.ColorRes;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import net.vrallev.android.cat.Cat;
import ru.kazantsev.template.util.AndroidSystemUtils;
import ru.kazantsev.template.util.GuiUtils;
import ru.samlib.client.R;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by 0shad on 23.04.2017.
 */
public class SamlibUtils {

    private static final Map<Character, String> latinFromKirilMap = new HashMap<>();

    static {
        latinFromKirilMap.put(' ', "_");
        latinFromKirilMap.put('А', "A");
        latinFromKirilMap.put('а', "a");
        latinFromKirilMap.put('Б', "B");
        latinFromKirilMap.put('б', "b");
        latinFromKirilMap.put('В', "W");
        latinFromKirilMap.put('в', "w");
        latinFromKirilMap.put('Г', "G");
        latinFromKirilMap.put('г', "g");
        latinFromKirilMap.put('Ґ', "G");
        latinFromKirilMap.put('ґ', "g");
        latinFromKirilMap.put('Д', "D");
        latinFromKirilMap.put('д', "d");
        latinFromKirilMap.put('Е', "E");
        latinFromKirilMap.put('е', "e");
        latinFromKirilMap.put('Ё', "E");
        latinFromKirilMap.put('ё', "e");
        latinFromKirilMap.put('Є', "E");
        latinFromKirilMap.put('є', "e");
        latinFromKirilMap.put('Ж', "Zh");
        latinFromKirilMap.put('ж', "zh");
        latinFromKirilMap.put('З', "Z");
        latinFromKirilMap.put('з', "z");
        latinFromKirilMap.put('И', "I");
        latinFromKirilMap.put('и', "i");
        latinFromKirilMap.put('І', "I");
        latinFromKirilMap.put('і', "i");
        latinFromKirilMap.put('Ї', "Y");
        latinFromKirilMap.put('ї', "i");
        latinFromKirilMap.put('Й', "J");
        latinFromKirilMap.put('й', "j");
        latinFromKirilMap.put('К', "K");
        latinFromKirilMap.put('к', "k");
        latinFromKirilMap.put('Л', "L");
        latinFromKirilMap.put('л', "l");
        latinFromKirilMap.put('М', "M");
        latinFromKirilMap.put('м', "m");
        latinFromKirilMap.put('Н', "N");
        latinFromKirilMap.put('н', "n");
        latinFromKirilMap.put('О', "O");
        latinFromKirilMap.put('о', "o");
        latinFromKirilMap.put('П', "P");
        latinFromKirilMap.put('п', "p");
        latinFromKirilMap.put('Р', "R");
        latinFromKirilMap.put('р', "r");
        latinFromKirilMap.put('С', "S");
        latinFromKirilMap.put('с', "s");
        latinFromKirilMap.put('Т', "T");
        latinFromKirilMap.put('т', "t");
        latinFromKirilMap.put('У', "U");
        latinFromKirilMap.put('у', "u");
        latinFromKirilMap.put('Ф', "F");
        latinFromKirilMap.put('ф', "f");
        latinFromKirilMap.put('Х', "H");
        latinFromKirilMap.put('х', "h");
        latinFromKirilMap.put('Ц', "C");
        latinFromKirilMap.put('ц', "c");
        latinFromKirilMap.put('Ч', "Ch");
        latinFromKirilMap.put('ч', "ch");
        latinFromKirilMap.put('Ш', "Sh");
        latinFromKirilMap.put('ш', "sh");
        latinFromKirilMap.put('Щ', "Sh");
        latinFromKirilMap.put('щ', "sh");
        latinFromKirilMap.put('Ы', "Y");
        latinFromKirilMap.put('ы', "y");
        latinFromKirilMap.put('Э', "E");
        latinFromKirilMap.put('э', "e");
        latinFromKirilMap.put('Ю', "Ju");
        latinFromKirilMap.put('ю', "ju");
        latinFromKirilMap.put('Я', "Ja");
        latinFromKirilMap.put('я', "ja");
        latinFromKirilMap.put('Ь', "");
        latinFromKirilMap.put('ь', "");
        latinFromKirilMap.put('Ъ', "");
        latinFromKirilMap.put('ъ', "");
        latinFromKirilMap.put('\\', "");
        latinFromKirilMap.put('.', "");
    }


    public static String getLinkFromAuthorName(String name) {
        name = name.toLowerCase();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            Character nextChar = name.charAt(i);
            if (latinFromKirilMap.containsKey(nextChar)) {
                builder.append(latinFromKirilMap.get(nextChar));
            } else {
                builder.append(nextChar);
            }
        }
        String[] linkParts = builder.toString().split("_");
        String link = linkParts[0];
        if (linkParts.length > 1) {
            for (int i = 1; i < linkParts.length; i++) {
                link += "_" + linkParts[i].charAt(0);
            }
        }
        return "/" + link.charAt(0) + "/" + link + "/";
    }

    public static CharSequence generateText(Context context, String title, String addition, @ColorRes int colorRes, float prop) {
        return generateText(title, addition, context.getResources().getColor(colorRes), prop);
    }

    public static CharSequence generateText(String title, String addition, int color, float prop) {
        if (TextUtils.isEmpty(addition)) return title;
        String result = title + " " + addition;
        int from = title.length() + 1;
        int to = result.length();
        SpannableStringBuilder spannable = GuiUtils.spannableText(result, new ForegroundColorSpan(color), from, to);
        spannable.setSpan(new RelativeSizeSpan(prop), from, to, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannable;
    }

    public static class Font {

        private static Map<String, String> mapFonts(AssetManager manager) {
            Map<String, String> ttf = new HashMap<>();
            try {
                ttf = filterAssetDir(manager, "fonts", ".ttf");
            } catch (IOException e) {
                Cat.e("Unknown exception", e);
            }
            return ttf;
        }

        private static Map<String, String> filterAssetDir(AssetManager manager, String path, String type) throws IOException {
            Map<String, String> files = new HashMap<>();
            String[] list = manager.list(path);
            for (String file : list) {
                if (!file.contains(".")) {
                    files.putAll(filterAssetDir(manager, path + "/" + file, type));
                } else if (file.endsWith(type)) {
                    files.put(file.substring(0, file.lastIndexOf(type)), path + "/" + file);
                }
            }
            return files;
        }

        public static List<String> listFontNames(AssetManager manager) {
            return Stream.of(mapFonts(manager)).map(Map.Entry::getKey).collect(Collectors.toList());
        }

        public static String findPathForName(AssetManager manager, String name) {
            Map.Entry<String,String> result = Stream.of(mapFonts(manager)).filter(entry -> entry.getKey().equals(name)).findFirst().orElse(null);
            if(result != null) {
                return result.getValue();
            }
            return null;
        }

    }

}
