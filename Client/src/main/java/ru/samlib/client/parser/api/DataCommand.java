package ru.samlib.client.parser.api;


import lombok.Data;
import ru.samlib.client.domain.entity.Genre;
import ru.samlib.client.domain.entity.Type;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class DataCommand {
    String link;
    Command command;
    Date commandDate;
    String title;
    String authorName;
    Type type;
    Genre genre;
    String annotation;
    Date createDate;
    Integer imageCount;
    Long unixtime;
    Integer size;
    BigDecimal rate;
    Integer votes;
}
