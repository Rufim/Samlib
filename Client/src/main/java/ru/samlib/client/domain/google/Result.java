
package ru.samlib.client.domain.google;

import com.google.gson.annotations.Expose;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class Result {

    @Expose
    private String GsearchResultClass;
    @Expose
    private String unescapedUrl;
    @Expose
    private String url;
    @Expose
    private String visibleUrl;
    @Expose
    private String cacheUrl;
    @Expose
    private String title;
    @Expose
    private String titleNoFormatting;
    @Expose
    private String content;
}
