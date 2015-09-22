
package ru.samlib.client.domain.google;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.annotations.Expose;
import lombok.Data;
import lombok.NoArgsConstructor;

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
