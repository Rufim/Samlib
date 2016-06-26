package ru.samlib.client.service;

import io.requery.Persistable;
import io.requery.query.JoinAndOr;
import io.requery.query.Result;
import io.requery.sql.EntityDataStore;
import ru.samlib.client.App;
import ru.samlib.client.domain.entity.*;

import java.util.List;

/**
 * Created by 0shad on 26.06.2016.
 */
public class ObservableService {

    private static ObservableService service;
    private JoinAndOr<Result<AuthorEntity>> joinAndOr;

    public ObservableService(EntityDataStore<Persistable> dataStore) {
        this.dataStore = dataStore;
        joinAndOr = dataStore.select(AuthorEntity.class).distinct()
                .leftJoin(CategoryEntity.class).on(CategoryEntity.AUTHOR_ID.equal(AuthorEntity.ID))
                .leftJoin(WorkEntity.class).on(WorkEntity.AUTHOR_ID.equal(AuthorEntity.ID).or(WorkEntity.CATEGORY_ID.equal(CategoryEntity.ID)))
                .leftJoin(LinkEntity.class).on(LinkEntity.AUTHOR_ID.equal(AuthorEntity.ID).or(LinkEntity.CATEGORY_ID.equal(CategoryEntity.ID)));
    }

    public static synchronized ObservableService getInstance() {
        if(service == null) {
            if (App.getInstance() == null || App.getInstance().getDataStore() == null)
                throw new RuntimeException("database not available");
            return service = new ObservableService(App.getInstance().getDataStore());
        }
        return service;
    }


    private final EntityDataStore<Persistable> dataStore;


    public AuthorEntity getAuthorById(Integer id) {
        return joinAndOr.where(AuthorEntity.ID.eq(id)).get().first();
    }

    public AuthorEntity getAuthorByLink(String link) {
        return joinAndOr.where(AuthorEntity.LINK.eq(link)).get().firstOrNull();
    }

    public List<AuthorEntity> getObservableAuthors() {
        return joinAndOr.get().toList();
    }

    public AuthorEntity updateAuthor(AuthorEntity entity) {
        AuthorEntity updatedEntity = dataStore.update(entity);
        for (Category category : entity.getCategories()) {
            dataStore.update((CategoryEntity)category);
        }
        return updatedEntity;
    }
}
