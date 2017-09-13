package ru.samlib.client.database;

import com.raizlabs.android.dbflow.converter.TypeConverter;
import ru.samlib.client.domain.entity.Genre;

import java.util.List;

public class ListGenreConverter extends TypeConverter<String, List<Genre>> implements ListConverter {


    @Override
    public String getDBValue(List<Genre> model) {
        return getStringDBValue(model);
    }

    @Override
    public List<Genre> getModelValue(String data) {
        return getListModelValue(data);
    }
}
