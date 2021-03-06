package ru.samlib.client.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ru.samlib.client.domain.Linkable;

import java.io.Serializable;

/**
 * Created by Dmitry on 27.10.2015.
 */
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
public class Image implements Serializable, Linkable {
    private Integer number;
    private String link;
    private String annotation;
    private String title;
    private Integer size;
    private int width;
    private int height;

    @Override
    public String toString() {
        return title != null ? title : number.toString();
    }
}



