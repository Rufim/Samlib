package ru.samlib.client.domain.model

enum class Type(
    val title: String,
    val link: String,
) {
    NOVEL("Роман", "/type/index_type_1-1.shtml"),
    TALE("Повесть", "/type/index_type_2-1.shtml"),
    HEAD("Глава", "/type/index_type_16-1.shtml"),
    STORYBOOK("Сборник рассказов", "/type/index_type_10-1.shtml"),
    STORY("Рассказ", "/type/index_type_3-1.shtml"),
    POETRY("Поэма", "/type/index_type_11-1.shtml"),
    POETRY_COLLECTION("Сборник стихов", "/type/index_type_7-1.shtml"),
    POEM("Стихотворение", "/type/index_type_6-1.shtml"),
    ESSAY("Эссе", "/type/index_type_13-1.shtml"),
    OUTLINE("Очерк", "/type/index_type_4-1.shtml"),
    ARTICLE("Статья", "/type/index_type_5-1.shtml"),
    MONOGRAPH("Монография", "/type/index_type_17-1.shtml"),
    DIRECTORY("Справочник", "/type/index_type_18-1.shtml"),
    SONG("Песня", "/type/index_type_12-1.shtml"),
    SHORT_STORY("Новелла", "/type/index_type_15-1.shtml"),
    PLAY_SCREENPLAY("Пьеса; сценарий", "/type/index_type_9-1.shtml"),
    MINIATURE("Миниатюра", "/type/index_type_8-1.shtml"),
    INTERVIEW("Интервью", "/type/index_type_14-1.shtml"),
    OTHER("", ""),
    ;

    companion object {
        fun parseType(type: String?): Type {
            if (!type.isNullOrEmpty()) {
                val t = type.trim().lowercase()
                return entries.firstOrNull { it.title.lowercase() == t } ?: OTHER
            }
            return OTHER
        }
    }
}
