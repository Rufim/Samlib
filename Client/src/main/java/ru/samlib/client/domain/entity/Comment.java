package ru.samlib.client.domain.entity;

import lombok.Data;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.Validatable;

import java.text.SimpleDateFormat;
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
    private String email;
    private Date data;
    private boolean userComment = false;

    public String getFormattedData() {
        return new SimpleDateFormat(Constants.Pattern.DATA_TIME_PATTERN).format(data);
    }


    @Override
    public boolean validate() {
        return number != null && nickName != null && rawContent != null;
    }
}
