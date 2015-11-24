package ru.samlib.client.domain.entity;

import android.widget.TextView;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.Validatable;
import ru.samlib.client.util.SystemUtils;
import ru.samlib.client.util.TextUtils;

import java.util.Timer;

/**
 * Created by Rufim on 22.05.2014.
 */
@AllArgsConstructor
public enum Genre implements Linkable {
    PROSE("Проза", "/janr/index_janr_5"),
    POETRY("Поэзия", "/janr/index_janr_4>"),
    LYRICS("Лирика", "/janr/index_janr_3>"),
    MEMOIRS("Мемуары", "/janr/index_janr_19"),
    HISTORY("История", "/janr/index_janr_26"),
    NURSERY("Детская", "/janr/index_janr_29"),
    DETECTIVE("Детектив", "/janr/index_janr_2>"),
    ADVENTURE("Приключения", "/janr/index_janr_25"),
    FICTION("Фантастика", "/janr/index_janr_1>"),
    FANTASY("Фэнтези", "/janr/index_janr_24"),
    CYBERPUNK("Киберпанк", "/janr/index_janr_22"),
    PUBLICISM("Публицистика", "/janr/index_janr_11"),
    EVENTS("События", "/janr/index_janr_32"),
    LITREVIEW("Литобзор", "/janr/index_janr_23"),
    CRITICISM("Критика", "/janr/index_janr_9>"),
    PHILOSOPHY("Философия", "/janr/index_janr_15"),
    RELIGION("Религия", "/janr/index_janr_13"),
    ESOTERICS("Эзотерика", "/janr/index_janr_14"),
    OCCULTISM("Оккультизм", "/janr/index_janr_18"),
    MYSTIC("Мистика", "/janr/index_janr_17"),
    HORROR("Хоррор", "/janr/index_janr_30"),
    POLITICS("Политика", "/janr/index_janr_28"),
    LOVE_STORY("Любовный роман", "/janr/index_janr_12"),
    NATURAL_HISTORY("Естествознание", "/janr/index_janr_20"),
    INVENTION("Изобретательство", "/janr/index_janr_21"),
    HUMOR("Юмор", "/janr/index_janr_8>"),
    TALES("Байки", "/janr/index_janr_27"),
    PARODIES("Пародии", "/janr/index_janr_31"),
    TRANSLATIONS("Переводы", "/janr/index_janr_10"),
    FAIRY_TALES("Сказки", "/janr/index_janr_16"),
    DRAMATURGY("Драматургия", "/janr/index_janr_6>"),
    POSTMODERNISM("Постмодернизм", "/janr/index_janr_33"),
    FOREIGN_TRANSLAT("Foreign+Translat", "/janr/index_janr_34"),
    EMPTY("Без жанра","");

    private @Getter String title;
    private @Getter String link;

    public static Genre parseGenre(String genre) {
        if (genre != null && !genre.isEmpty()) {
            for (Genre tryGenre : Genre.values()) {
                if (tryGenre.getTitle()
                        .toLowerCase()
                        .equals(TextUtils.trim(genre.toLowerCase())))
                    return tryGenre;
            }
        }
        return null;
    }

    public String getAnnotation() {
        return "";
    }

    @Override
    public String toString() {
        return title;
    }
}
