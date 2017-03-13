
package ru.samlib.client.domain.google;

import com.google.gson.annotations.Expose;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Data
public class ResponseData {

    @Expose
    private List<Result> results = new ArrayList<Result>();
    @Expose
    private Cursor cursor;
}
