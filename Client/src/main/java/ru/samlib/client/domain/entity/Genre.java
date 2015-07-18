package ru.samlib.client.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import ru.samlib.client.domain.Linkable;
import ru.samlib.client.domain.Validatable;
import ru.samlib.client.util.SystemUtils;

import java.util.Timer;

/**
 * Created by Rufim on 22.05.2014.
 */
@AllArgsConstructor
public enum Genre implements Linkable {
    PROSE("Проза", "/janr/index_janr_5-1.shtml"),
    POETRY("Поэзия", "/janr/index_janr_4-1.shtml>"),
    LYRICS("Лирика", "/janr/index_janr_3-1.shtml>"),
    MEMOIRS("Мемуары", "/janr/index_janr_19-1.shtml"),
    HISTORY("История", "/janr/index_janr_26-1.shtml"),
    NURSERY("Детская", "/janr/index_janr_29-1.shtml"),
    DETECTIVE("Детектив", "/janr/index_janr_2-1.shtml>"),
    ADVENTURE("Приключения", "/janr/index_janr_25-1.shtml"),
    FICTION("Фантастика", "/janr/index_janr_1-1.shtml>"),
    FANTASY("Фэнтези", "/janr/index_janr_24-1.shtml"),
    CYBERPUNK("Киберпанк", "/janr/index_janr_22-1.shtml"),
    PUBLICISM("Публицистика", "/janr/index_janr_11-1.shtml"),
    EVENTS("События", "/janr/index_janr_32-1.shtml"),
    LITREVIEW("Литобзор", "/janr/index_janr_23-1.shtml"),
    CRITICISM("Критика", "/janr/index_janr_9-1.shtml>"),
    PHILOSOPHY("Философия", "/janr/index_janr_15-1.shtml"),
    RELIGION("Религия", "/janr/index_janr_13-1.shtml"),
    ESOTERICS("Эзотерика", "/janr/index_janr_14-1.shtml"),
    OCCULTISM("Оккультизм", "/janr/index_janr_18-1.shtml"),
    MYSTIC("Мистика", "/janr/index_janr_17-1.shtml"),
    HORROR("Хоррор", "/janr/index_janr_30-1.shtml"),
    POLITICS("Политика", "/janr/index_janr_28-1.shtml"),
    LOVE_STORY("Любовный роман", "/janr/index_janr_12-1.shtml"),
    NATURAL_HISTORY("Естествознание", "/janr/index_janr_20-1.shtml"),
    INVENTION("Изобретательство", "/janr/index_janr_21-1.shtml"),
    HUMOR("Юмор", "/janr/index_janr_8-1.shtml>"),
    TALES("Байки", "/janr/index_janr_27-1.shtml"),
    PARODIES("Пародии", "/janr/index_janr_31-1.shtml"),
    TRANSLATIONS("Переводы", "/janr/index_janr_10-1.shtml"),
    FAIRY_TALES("Сказки", "/janr/index_janr_16-1.shtml"),
    DRAMATURGY("Драматургия", "/janr/index_janr_6-1.shtml>"),
    POSTMODERNISM("Постмодернизм", "/janr/index_janr_33-1.shtml"),
    FOREIGN_TRANSLAT("Foreign+Translat", "/janr/index_janr_34-1.shtml");

    private @Getter String title;
    private @Getter String link;

    public static Genre parseGenre(String genre) {
        if (genre != null && !genre.isEmpty()) {
            for (Genre tryGenre : Genre.values()) {
                if (tryGenre.getTitle()
                        .toLowerCase()
                        .equals(SystemUtils.trim(genre.toLowerCase())))
                    return tryGenre;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return title;
    }
}
