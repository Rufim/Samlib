package ru.samlib.client.domain.entity;

import lombok.Data;
import ru.samlib.client.domain.Validatable;

import java.util.Date;

/**
 * Created by Dmitry on 29.10.2015.
 */

@Data
public class Comment implements Validatable {
    private Integer number;
    private String rawContent;
    private Author author;
    private String nickName;
    private Date data;

    @Override
    public boolean validate() {
        return number != null && nickName != null && rawContent != null;
    }
}
