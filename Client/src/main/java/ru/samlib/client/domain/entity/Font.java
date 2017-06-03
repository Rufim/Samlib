package ru.samlib.client.domain.entity;

import android.content.Context;
import android.content.res.AssetManager;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import lombok.Data;
import net.vrallev.android.cat.Cat;
import ru.kazantsev.template.util.AndroidSystemUtils;
import ru.samlib.client.R;
import ru.samlib.client.domain.Constants;

import java.io.IOException;
import java.util.*;

/**
 * Created by 0shad on 03.06.2017.
 */
@Data
public class Font {

    private static Map<String, Font> fontMap;
    private static Font defFont = new Font("Roboto-Regular").addType(Font.Type.PLAIN, Constants.Assets.ROBOTO_FONT_PATH);

    private String name;
    private float size;
    private int color;
    private Map<Type, String> types = new LinkedHashMap<>();

    public Font(String name) {
        this.name = name;
    }

    public enum Type {
        BOLD, ITALIC, BOLD_ITALIC, PLAIN;

        public String toString() {
            switch (this) {
                case BOLD:
                    return "Bold";
                case ITALIC:
                    return "Italic";
                case BOLD_ITALIC:
                    return "Bold/Italic";
                case PLAIN:
                    return "Plain";
            }
            return super.toString();
        }
    }

    public Font addType(Type type, String path) {
        types.put(type, path);
        return this;
    }

    public String toString() {
        return name;
    }

    public boolean equals(Object obj) {
        if(obj != null && obj instanceof Font) return ((Font) obj).getName().equals(getName());
        if(obj != null && obj instanceof CharSequence) return obj.equals(toString());
        return false;
    }

    public static Font getDefFont() {
        return defFont;
    }

    public static String getFontPath(Context context, String name, Type type) {
        if(name == null) {
            name = AndroidSystemUtils.getStringResPreference(context, R.string.preferenceFontReader, defFont.getName());
        }
        if(type == null) {
            type = Type.valueOf(AndroidSystemUtils.getStringResPreference(context, R.string.preferenceFontStyleReader,  Type.PLAIN.name()));
        }
        Font font =  mapFonts(context.getAssets()).get(name);
        if(font != null) {
            if(font.getTypes().containsKey(type)) {
                return font.getTypes().get(type);
            } else {
                return font.getTypes().entrySet().iterator().next().getValue();
            }
        }
        return null;
    }

    public static Map<Font.Type, Font.Type> getFontTypes(Context context, String name) {
        if (name == null) {
            name = AndroidSystemUtils.getStringResPreference(context, R.string.preferenceFontReader, defFont.getName());
        }
        Font font = Font.mapFonts(context.getAssets()).get(name);
        Map<Font.Type, Font.Type> keyType = new LinkedHashMap<>();
        if (font != null) {
            for (Map.Entry<Font.Type, String> entry : font.getTypes().entrySet()) {
                keyType.put(entry.getKey(), entry.getKey());
            }
        }
        return keyType;
    }

    public static Map<String, Font> mapFonts(AssetManager manager) {
        if(fontMap != null) return fontMap;
        Map<String, Font> ttf = new HashMap<>();
        try {
            ttf = filterAssetDir(manager, "fonts", ".ttf", new LinkedHashMap<>());
        } catch (IOException e) {
            Cat.e("Unknown exception", e);
        }
        return fontMap = ttf;
    }

    private static Map<String, Font> filterAssetDir(AssetManager manager, String path, String type, Map<String, Font> fontMap) throws IOException {
        String[] list = manager.list(path);
        for (String file : list) {
            String filePath = path + "/" + file;
            if (!file.contains(".")) {
                filterAssetDir(manager, filePath, type, fontMap);
            } else if (file.endsWith(type)) {
                String name = file.substring(0, file.lastIndexOf(type));
                List<Type> fontTypes = new ArrayList<>();
                if (name.toLowerCase().contains("italic")) {
                    name = name.replaceAll("(?i)italic", "");
                    fontTypes.add(Type.ITALIC);
                }
                if (name.toLowerCase().contains("bold")) {
                    name = name.replaceAll("(?i)bold", "");
                    fontTypes.add(Type.BOLD);
                }
                name = name.trim();
                while (name.length() > 1 && (name.endsWith("-") || name.endsWith("_"))) {
                    name = name.substring(0, name.length() - 1);
                }
                if (fontTypes.size() == 2) {
                    fontTypes.clear();
                    fontTypes.add(Type.BOLD_ITALIC);
                }
                if(fontTypes.isEmpty()) {
                    fontTypes.add(Type.PLAIN);
                }
                Font font;
                if (!fontMap.containsKey(name)) {
                    font = new Font(name);
                } else {
                    font = fontMap.get(name);
                }
                font.addType(fontTypes.get(0), filePath);
                fontMap.put(name, font);
            }
        }
        return fontMap;
    }

    public static List<String> listFontNames(AssetManager manager) {
        return Stream.of(mapFonts(manager)).map(Map.Entry::getKey).collect(Collectors.toList());
    }

    public static String findPathForName(AssetManager manager, String name) {
        Map.Entry<String, Font> result = Stream.of(mapFonts(manager)).filter(entry -> entry.getKey().equals(name)).findFirst().orElse(null);
        if (result != null) {
            return result.getValue().getTypes().get(Type.PLAIN);
        }
        return null;
    }

    public static String  getNameForPath(String filePath) {
        int start = 0;
        if(filePath.contains("/")) {
            start = filePath.lastIndexOf("/");
        }
        String name = filePath.substring(start);
        List<Type> fontTypes = new ArrayList<>();
        if (name.toLowerCase().contains("italic")) {
            name = name.replaceAll("(?i)italic", "");
        }
        if (name.toLowerCase().contains("bold")) {
            name = name.replaceAll("(?i)blod", "");
        }
        name = name.trim();
        while (name.length() > 1 && (name.endsWith("-") || name.endsWith("_"))) {
            name = name.substring(0, name.length() - 1);
        }
        return name;
    }

}
