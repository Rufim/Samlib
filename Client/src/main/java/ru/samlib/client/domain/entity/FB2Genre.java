package ru.samlib.client.domain.entity;

import lombok.Getter;
import ru.kazantsev.template.util.TextUtils;

public enum FB2Genre {
    //Фантастика (Научная фантастика и Фэнтези),
    SF_HISTORY("sf_history","Альтернативная история"),
    SF_ACTION("sf_action","Боевая фантастика"),
    SF_EPIC("sf_epic","Эпическая фантастика"),
    SF_HEROIC("sf_heroic","Героическая фантастика"),
    SF_DETECTIV("sf_detective","Детективная фантастика"),
    SF_CYBERPUNK("sf_cyberpunk","Киберпанк"),
    SF_SPACE("sf_space","Космическая фантастика"),
    SF_SOCIAL("sf_social","Социально-психологическая фантастика"),
    SF_HORRO("sf_horror","Ужасы и Мистика"),
    SF_HUMOR("sf_humor","Юмористическая фантастика"),
    SF_FANTASY("sf_fantasy","Фэнтези"),
    SF("sf","Научная Фантастика"),
    //Детективы и Триллеры
    DET_CLASSIC ("det_classic","Классический детектив"),
    DET_POLICE ("det_police","Полицейский детектив"),
    DET_ACTION ("det_action","Боевик"),
    DET_IRONY ("det_irony","Иронический детектив"),
    DET_HISTORY("det_history","Исторический детектив"),
    DET_ESPIONAGE("det_espionage","Шпионский детектив"),
    DET_CRIME("det_crime","Криминальный детектив"),
    DET_POLITICAL ("det_political","Политический детектив"),
    DET_MANIAC ("det_maniac","Маньяки"),
    DET_HARD("det_hard","Крутой детектив"),
    THRILLER("thriller","Триллер"),
    DETECTIVE("detective","Детектив"),
    //Проза
    PROSE_CLASSIC("prose_classic","Классическая проза"),
    PROSE_HISTORY ("prose_history","Историческая проза"),
    PROSE_CONTEMPORARY("prose_contemporary","Современная проза"),
    PROSE_COUNTER("prose_counter","Контркультура"),
    PROSE_RUS_CLASSIC ("prose_rus_classic","Русская классическая проза"),
    PROSE_SU_CLASSICS("prose_su_classics","Советская классическая проза"),
    //Любовные романы
    LOVE_CONTEMPORARY("love_contemporary","Современные любовные романы"),
    LOVE_HISTORY("love_history","Исторические любовные романы"),
    LOVE_DETECTIVE ("love_detective","Остросюжетные любовные романы"),
    LOVE_SHORT ("love_short","Короткие любовные романы"),
    LOVE_EROTICA ("love_erotica","Эротика"),
    //Приключения
    ADV_WESTERN("adv_western","Вестерн"),
    ADV_HISTORY("adv_history","Исторические приключения"),
    ADV_INDIAN("adv_indian","Приключения про индейцев"),
    ADV_MARITIME("adv_maritime","Морские приключения"),
    ADV_GEO("adv_geo","Путешествия и география"),
    ADV_ANIMAL("adv_animal","Природа и животные"),
    ADVENTURE("adventure","Приключения"),
    //Детское"),
    CHILD_TALE("child_tale","Сказка"),
    CHILD_VERSE("child_verse","Детские стихи"),
    CHILD_PROSE("child_prose","Детскиая проза"),
    CHILD_SF("child_sf","Детская фантастика"),
    CHILD_DET("child_det","Детские остросюжетные"),
    CHILD_ADV("child_adv","Детские приключения"),
    CHILD_EDUCATION("child_education","Детская образовательная литература"),
    CHILDREN("children","Детская литература"),
    //Поэзия, Драматургия
    POETRY("poetry","Поэзия"),
    DRAMATURGY("dramaturgy","Драматургия"),
    //Старинное
    ANTIQUE_ANT("antique_ant","Античная литература"),
    ANTIQUE_EUROPEAN("antique_european","Европейская старинная литература"),
    ANTIQUE_RUSSIAN("antique_russian","Древнерусская литература"),
    ANTIQUE_EAST("antique_east","Древневосточная литература"),
    ANTIQUE_MYTHS("antique_myths","Мифы. Легенды. Эпос"),
    ANTIQUE("antique","Старинная литература"),
    //Наука, Образование
    SCI_HISTORY("sci_history","История"),
    SCI_PSYCHOLOGY("sci_psychology","Психология"),
    SCI_CULTURE("sci_culture","Культурология"),
    SCI_RELIGION("sci_religion","Религиоведение"),
    SCI_PHILOSOPHY("sci_philosophy","Философия"),
    SCI_POLITICS("sci_politics","Политика"),
    SCI_BUSINESS("sci_business","Деловая литература"),
    SCI_JURIS("sci_juris","Юриспруденция"),
    SCI_LINGUISTIC("sci_linguistic","Языкознание"),
    SCI_MEDICINE("sci_medicine","Медицина"),
    SCI_PHYS("sci_phys","Физика"),
    SCI_MATH("sci_math","Математика"),
    SCI_CHEM("sci_chem","Химия"),
    SCI_BIOLOGY("sci_biology","Биология"),
    SCI_TECH("sci_tech","Технические науки"),
    SCIENCE("science","Научная литература"),
    //Компьютеры и Интернет
    COMP_WWW("comp_www","Интернет"),
    COMP_PROGRAMMING("comp_programming","Программирование"),
    COMP_HARD("comp_hard","Компьютерное железо"),
    COMP_SOFT("comp_soft","Программы"),
    COMP_DB("comp_db","Базы данных"),
    COMP_OSNET("comp_osnet","ОС и Сети"),
    COMPUTERS("computers","Компьтерная литература"),
    //Справочная литература
    REF_ENCYC("ref_encyc","Энциклопедии"),
    REF_DICT("ref_dict","Словари"),
    REF_REF("ref_ref","Справочники"),
    REF_GUIDE("ref_guide","Руководства"),
    REFERENCE("reference","Cправочная литература"),
    //документальная литература
    NONF_BIOGRAPHY("nonf_biography","Биографии и Мемуары"),
    NONF_PUBLICISM("nonf_publicism","Публицистика"),
    NONF_CRITICISM("nonf_criticism","Критика"),
    DESIGN("design","Искусство и Дизайн"),
    NONFICTION("nonfiction","Документальная литература"),
    //религия и духовность
    RELIGION_REL("religion_rel","Религия"),
    RELIGION_ESOTERICS("religion_esoterics","Эзотерика"),
    RELIGION_SELF("religion_self","Самосовершенствование"),
    RELIGION("religion","Религия"),
    //юмор
    HUMOR_ANECDOTE("humor_anecdote","Анекдоты"),
    HUMOR_PROSE("humor_prose","Юмористическая проза"),
    HUMOR_VERSE("humor_verse","Юмористические стихи"),
    HUMOR("humor","Юмор"),
    //домоводство (Дом и семья)
    HOME_COOKING("home_cooking","Кулинария"),
    HOME_PETS("home_pets","Домашние животные"),
    HOME_CRAFTS("home_crafts","Хобби и ремесла"),
    HOME_ENTERTAIN("home_entertain","Развлечения"),
    HOME_HEALTH("home_health","Здоровье"),
    HOME_GARDEN("home_garden","Сад и огород"),
    HOME_DIY("home_diy","Сделай сам"),
    HOME_SPORT("home_sport","Спорт"),
    HOME_SEX("home_sex","Эротика, Секс"),
    HOME("home","Домоводство");

    private @Getter String value;
    private @Getter String name;

    FB2Genre(String value, String name) {
        this.value = value;
        this.name = name;
    }

    public static FB2Genre parseGenre(String genre) {
        if (genre != null && !genre.isEmpty()) {
            for (FB2Genre tryGenre : FB2Genre.values()) {
                if (tryGenre.getValue()
                        .toLowerCase()
                        .equals(TextUtils.trim(genre.toLowerCase())))
                    return tryGenre;
            }
        }
        return null;
    }
}
