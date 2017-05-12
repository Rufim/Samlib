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
    private Link link;
    private Work work;
    private String nickName;
    private String email;
    private Date data;
    private String msgid;
    private boolean canBeDeleted = false;
    private boolean canBeRestored = false;
    private boolean canBeEdited = false;
    private boolean userComment = false;
    private boolean deleted = false;

    public String getFormattedData() {
        return new SimpleDateFormat(Constants.Pattern.DATA_TIME_PATTERN).format(data);
    }


    @Override
    public boolean validate() {
        return number != null && rawContent != null;
    }

    public String getReference() {
        return "/comment" + work.getLinkWithoutSuffix();
    }

}
