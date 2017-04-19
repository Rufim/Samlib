package ru.samlib.client.service;

import io.requery.Persistable;
import io.requery.query.Condition;
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

    enum Action {INSERT, UPDATE, DELETE, UPSERT}

    @Inject
    EntityDataStore<Persistable> dataStore;

    public ObservableService() {
        App.getInstance().getComponent().inject(this);
    }

    private JoinAndOr<Result<AuthorEntity>> getAuthorQuery() {
        return dataStore.select(AuthorEntity.class).distinct()
                .leftJoin(Category.class).on((Condition) CategoryEntity.AUTHOR_ID.equal(AuthorEntity.ID))
                .leftJoin(Work.class).on(WorkEntity.ROOT_AUTHOR_ID.equal(AuthorEntity.ID).or(WorkEntity.CATEGORY_ID.equal(CategoryEntity.ID)))
                .leftJoin(Link.class).on(LinkEntity.ROOT_AUTHOR_ID.equal(AuthorEntity.ID).or(LinkEntity.CATEGORY_ID.equal(CategoryEntity.ID)));
    }

    public AuthorEntity insertAuthor(AuthorEntity entity) {
        return doActionAuthor(Action.INSERT, entity);
    }

    public AuthorEntity updateAuthor(AuthorEntity entity) {
        return doActionAuthor(Action.UPDATE, entity);
    }

    public AuthorEntity doActionAuthor(Action action, AuthorEntity Author) {
        AuthorEntity result =  (AuthorEntity) doAction(action, Author);
        if(action.equals(Action.UPDATE)) {
            for (Category category : Author.getCategories()) {
                doAction(action, category);
            }
        }
        return result;
    }

    private Persistable doAction(Action action, Object value) {
        Persistable result = null;
        if (value instanceof Persistable) {
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
                case UPSERT:
                    result = dataStore.upsert(entity);
                    break;
            }
        }
        return result;
    }

    public void deleteAuthor(AuthorEntity entity) {
        doAction(Action.DELETE, entity);
    }

    public void deleteAuthor(Author author) {
        doAction(Action.DELETE, getAuthorByLink(author.getLink()));
    }

    public AuthorEntity getAuthorById(Integer id) {
        return getAuthorQuery().where(AuthorEntity.ID.eq(id)).get().first();
    }

    public AuthorEntity getAuthorByLink(String link) {
        return getAuthorQuery().where(AuthorEntity.LINK.eq(link)).get().firstOrNull();
    }

    public List<AuthorEntity> getObservableAuthors() {
        return getAuthorQuery().get().toList();
    }

}
