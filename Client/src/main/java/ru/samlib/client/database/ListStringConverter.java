package ru.samlib.client.database;


import com.google.gson.Gson;
import com.raizlabs.android.dbflow.converter.TypeConverter;
import ru.samlib.client.domain.entity.Genre;

import java.util.List;

public class ListStringConverter extends TypeConverter<String, List<String>> implements ListConverter{

    @Override
    public String getDBValue(List<String> model) {
        return getStringDBValue(model);
    }

    @Override
    public List<String> getModelValue(String data) {
        return getListModelValue(data);
    }

}
