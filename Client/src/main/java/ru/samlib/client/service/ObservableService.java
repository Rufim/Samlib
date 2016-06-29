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

    enum Action {INSERT,UPDATE,DELETE,UPSERT}

    @Inject
    EntityDataStore<Persistable> dataStore;

    private JoinAndOr<Result<AuthorEntity>> joinAndOr;

    public ObservableService() {
        App.getInstance().getComponent().inject(this);
        joinAndOr = dataStore.select(AuthorEntity.class).distinct()
                .leftJoin(CategoryEntity.class).on(CategoryEntity.AUTHOR_ID.equal(AuthorEntity.ID))
                .leftJoin(WorkEntity.class).on(WorkEntity.AUTHOR_ID.equal(AuthorEntity.ID).or(WorkEntity.CATEGORY_ID.equal(CategoryEntity.ID)))
                .leftJoin(LinkEntity.class).on(LinkEntity.AUTHOR_ID.equal(AuthorEntity.ID).or(LinkEntity.CATEGORY_ID.equal(CategoryEntity.ID)));
    }

    public AuthorEntity insertAuthor(AuthorEntity entity) {
        return doActionAuthor(Action.INSERT, entity);
    }

    public AuthorEntity upsertAuthor(AuthorEntity entity) {
        return doActionAuthor(Action.UPDATE, entity);
    }

    public AuthorEntity doActionAuthor(Action action, AuthorEntity authorEntity) {
        AuthorEntity result = (AuthorEntity) doAction(action, authorEntity);
        for (Category category : authorEntity.getCategories()) {
            category.setAuthor(result);
            doAction(action, category);
            for (Work work : category.getWorks()) {
                work.setCategory(category);
                doAction(action, work);
            }
            for (Link link : category.getLinks()) {
                link.setCategory(category);
                doAction(action, link);
            }
        }
        for (Work work : authorEntity.getRootWorks()) {
            work.setAuthor(result);
            doAction(action, work);
        }
        for (Link link : authorEntity.getRootLinks()) {
            link.setAuthor(result);
            doAction(action, link);
        }
        if(result != null) {
            return result;
        } else {
            return authorEntity;
        }
    }

    private Persistable doAction(Action action, Object value) {
        Persistable result = null;
        if(value instanceof Persistable) {
            Persistable entity = (Persistable) value;
            switch (action) {
                case INSERT:
                    result = dataStore.insert(entity);
                    break;
                case UPDATE:
                    result = dataStore.update(entity);
                    break;
                case DELETE:
                    dataStore.delete(entity);
                    break;
            }
        }
        return result;
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
