package ru.samlib.client.domain.entity;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by 0shad on 25.10.2015.
 */
public enum Gender implements Serializable {

    MALE, FEMALE, UNDEFINED;

    public static final String MALE_REGEX = "(ло)|(ый)|(чук)|(ко)(ик)|(ов)|(ев)|(ин)|(ын)|(ой)|(цкий)|(ский)|(цкой)|(ской)";
    public static final String FEMALE_REGEX = "(ова)|(ева)|(ина)|(ая)|(ия)|(яя)|(екая)|(цкая)|(ская)";
    public static final Pattern male_pattern = Pattern.compile(".*(" + MALE_REGEX + ")",
            Pattern.DOTALL | Pattern.UNIX_LINES | Pattern.CASE_INSENSITIVE);
    public static final Pattern female_pattern = Pattern.compile(".*(" + FEMALE_REGEX + ")",
            Pattern.DOTALL | Pattern.UNIX_LINES | Pattern.CASE_INSENSITIVE);

    public static Gender parseLastName(String lastName) {
        if (lastName == null) return null;
        lastName = lastName.replace("ё", "е");
        Matcher matcher = male_pattern.matcher(lastName);
        if (matcher.matches()) {
            return Gender.MALE;
        }
        matcher = female_pattern.matcher(lastName);
        if (matcher.matches()) {
            return Gender.FEMALE;
        }
        return Gender.UNDEFINED;
    }

}
