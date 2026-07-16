package ru.samlib.client.domain.model

enum class Gender {
    MALE,
    FEMALE,
    UNDEFINED,
    ;

    companion object {
        private const val MALE_REGEX =
            "(ло)|(ый)|(чук)|(ко)(ик)|(ов)|(ев)|(ин)|(ын)|(ой)|(цкий)|(ский)|(цкой)|(ской)"
        private const val FEMALE_REGEX =
            "(ова)|(ева)|(ина)|(ая)|(ия)|(яя)|(екая)|(цкая)|(ская)"
        private val malePattern =
            Regex(".*($MALE_REGEX)", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        private val femalePattern =
            Regex(".*($FEMALE_REGEX)", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))

        fun parseLastName(lastName: String?): Gender {
            if (lastName == null) return UNDEFINED
            val name = lastName.replace("ё", "е")
            return when {
                malePattern.matches(name) -> MALE
                femalePattern.matches(name) -> FEMALE
                else -> UNDEFINED
            }
        }
    }
}
