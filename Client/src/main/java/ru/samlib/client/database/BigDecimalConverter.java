package ru.samlib.client.database;

import io.requery.Converter;

import java.math.BigDecimal;

/**
 * Created by 0shad on 18.06.2016.
 */
public class BigDecimalConverter implements Converter<BigDecimal, String>{


    @Override
    public Class<BigDecimal> mappedType() {
        return BigDecimal.class;
    }

    @Override
    public Class<String> persistedType() {
        return String.class;
    }

    @Override
    public Integer persistedSize() {
        return null;
    }

    @Override
    public String convertToPersisted(BigDecimal value) {
        return value == null ? null : value.toString();
    }

    @Override
    public BigDecimal convertToMapped(Class<? extends BigDecimal> type, String value) {
        return value == null ? null : new BigDecimal(value);
    }
}
