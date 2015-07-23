package ru.samlib.client.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jsoup.nodes.Document;

/**
 * Created by 0shad on 23.07.2015.
 */
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
public  class Chapter {
    private String title;
    private float percent;
    private String content;
    private Document parsedContent;
}
