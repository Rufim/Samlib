
package ru.samlib.client.domain.google;

import com.google.gson.annotations.Expose;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class Example {
    @Expose
    private ResponseData responseData;
    @Expose
    private Object responseDetails;
    @Expose
    private Integer responseStatus;

}
