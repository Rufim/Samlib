package ru.samlib.client.domain;

import ru.samlib.client.adapter.ItemListAdapter;

/**
 * Created by Dmitry on 29.07.2015.
 */
public interface Findable {
    boolean find(ItemListAdapter.FilterEvent query);
}
