package ru.samlib.client.data.parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import ru.samlib.client.domain.repository.SelectOption

/** Разбор формы редактора раздела (/cgi-bin/zhurnal): имя поля → значение. */
class AuthorEditParser {
    /**
     * Контролы первой формы. Через FormElement.elements(): когда форма разорвана
     * таблицей (new_book), поля не являются потомками <form> и обычный select их не видит.
     */
    private fun formControls(html: String): List<Element> {
        val form =
            try {
                Jsoup.parse(html).forms().firstOrNull()
            } catch (e: Exception) {
                throw ParseException("Не удалось разобрать форму редактора", e)
            } ?: return emptyList()
        return form.elements().filter { it.hasAttr("name") }
    }

    /**
     * Возвращает поля первой формы: input (кроме submit/button/file) по value и
     * textarea по содержимому. Сохраняем все скрытые поля, чтобы вернуть их при POST.
     */
    fun parseForm(html: String): Map<String, String> {
        val map = LinkedHashMap<String, String>()
        formControls(html).forEach { control ->
            val name = control.attr("name")
            when (control.tagName()) {
                "input" -> {
                    val type = control.attr("type").lowercase()
                    if (type == "submit" || type == "button" || type == "file") return@forEach
                    // чекбоксы/радио отправляются только когда отмечены
                    if ((type == "checkbox" || type == "radio") && !control.hasAttr("checked")) return@forEach
                    map[name] = control.attr("value").trim()
                }

                "select" -> {
                    val option =
                        control.selectFirst("option[selected]") ?: control.selectFirst("option")
                    if (option != null) {
                        map[name] =
                            if (option.hasAttr("value")) {
                                option.attr("value")
                            } else {
                                option
                                    .text()
                                    .trim()
                            }
                    }
                }
                // HTML игнорирует один перевод строки сразу после <textarea>.
                "textarea" -> map[name] = control.wholeText().removePrefix("\n")
            }
        }
        return map
    }

    /** Все варианты каждого select первой формы (для выпадающих списков UI). */
    fun parseSelectOptions(html: String): Map<String, List<SelectOption>> {
        val map = LinkedHashMap<String, List<SelectOption>>()
        formControls(html).filter { it.tagName() == "select" }.forEach { select ->
            map[select.attr("name")] =
                select.select("option").map { option ->
                    SelectOption(option.attr("value"), cleanLabel(option.text()))
                }
        }
        return map
    }

    /** Подписи вида «Роман (roman)» → «Роман»: транслит в скобках без кириллицы отбрасываем. */
    private fun cleanLabel(text: String): String =
        text
            .trim()
            .replace(Regex("\\s*\\([^()]*\\)$")) { m ->
                if (m.value.any { it in 'а'..'я' || it in 'А'..'Я' || it == 'ё' || it == 'Ё' }) m.value else ""
            }.trim()
}
