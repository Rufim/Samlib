
package ru.samlib.client.domain.google;

import com.google.gson.annotations.Expose;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Data
public class Cursor {

    @Expose
    private String resultCount;
    @Expose
    private List<Page> pages = new ArrayList<Page>();
    @Expose
    private String estimatedResultCount;
    @Expose
    private Integer currentPageIndex;
    @Expose
    private String moreResultsUrl;
    @Expose
    private String searchResultTime;

}
