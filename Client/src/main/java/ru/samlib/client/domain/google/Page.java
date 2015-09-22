
package ru.samlib.client.domain.google;

import com.google.gson.annotations.Expose;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class Page {

    @Expose
    private String start;
    @Expose
    private Integer label;

}
