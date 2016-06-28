package ru.samlib.client.service;

import dagger.Module;
import io.requery.Persistable;
import io.requery.query.JoinAndOr;
import io.requery.query.Result;
import io.requery.sql.EntityDataStore;
import ru.samlib.client.App;
import ru.samlib.client.domain.entity.*;

import javax.inject.Inject;
import java.util.List;

/**
 * Created by 0shad on 26.06.2016.
 */

public class ObservableService {


    @Inject
    public EntityDataStore<Persistable> dataStore;

    private JoinAndOr<Result<AuthorEntity>> joinAndOr;

    public ObservableService() {
        joinAndOr = dataStore.select(AuthorEntity.class).distinct()
                .leftJoin(CategoryEntity.class).on(CategoryEntity.AUTHOR_ID.equal(AuthorEntity.ID))
                .leftJoin(WorkEntity.class).on(WorkEntity.AUTHOR_ID.equal(AuthorEntity.ID).or(WorkEntity.CATEGORY_ID.equal(CategoryEntity.ID)))
                .leftJoin(LinkEntity.class).on(LinkEntity.AUTHOR_ID.equal(AuthorEntity.ID).or(LinkEntity.CATEGORY_ID.equal(CategoryEntity.ID)));
    }

    public AuthorEntity insertAuthor(AuthorEntity entity) {
        AuthorEntity createdEntity = dataStore.insert(entity);
        for (Category category : entity.getCategories()) {
            dataStore.insert((CategoryEntity)category);
        }
        return createdEntity;
    }

    public AuthorEntity upsertAuthor(AuthorEntity entity) {
        AuthorEntity updatedEntity = dataStore.upsert(entity);
        for (Category category : entity.getCategories()) {
            dataStore.upsert((CategoryEntity)category);
        }
        return updatedEntity;
    }

    public void deleteAuthor(AuthorEntity entity) {
        dataStore.delete(entity);
    }

    public AuthorEntity getAuthorById(Integer id) {
        return joinAndOr.where(AuthorEntity.ID.eq(id)).get().first();
    }

    public AuthorEntity getAuthorByLink(String link) {
        return joinAndOr.where(AuthorEntity.LINK.eq(link)).get().firstOrNull();
    }

    public List<AuthorEntity> getObservableAuthors() {
        return joinAndOr.get().toList();
    }

}
