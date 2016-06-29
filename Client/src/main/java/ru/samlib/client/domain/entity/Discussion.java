package ru.samlib.client.domain.entity;

import lombok.Data;
import ru.samlib.client.adapter.ItemListAdapter;
import ru.samlib.client.domain.Findable;
import ru.samlib.client.domain.Validatable;

import java.util.Date;

/**
 * Created by Dmitry on 20.11.2015.
 */
@Data
public class Discussion implements Validatable, Findable {
    private Work work;
    private Author author;
    private Integer countOfDay;
    private Integer count;
    private Date lastOne;

    @Override
    public boolean validate() {
        return work!= null && author != null ;
    }


    @Override
    public boolean find(ItemListAdapter.FilterEvent query) {
        return work.find(query);
    }
}
