
package ru.samlib.client.domain.google;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.annotations.Expose;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class ResponseData {

    @Expose
    private List<Result> results = new ArrayList<Result>();
    @Expose
    private Cursor cursor;
}
