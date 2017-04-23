package ru.samlib.client.service;

import com.annimon.stream.Stream;
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

public class DatabaseService {

    public enum Action {INSERT, UPDATE, DELETE, UPSERT}

    @Inject
    EntityDataStore<Persistable> dataStore;

    public DatabaseService() {
        App.getInstance().getComponent().inject(this);
    }

    private JoinAndOr<Result<AuthorEntity>> getAuthorQuery() {
        return dataStore.select(AuthorEntity.class).distinct()
                .leftJoin(Category.class).on((Condition) CategoryEntity.AUTHOR_ID.equal(AuthorEntity.LINK))
                .leftJoin(Work.class).on(WorkEntity.ROOT_AUTHOR_ID.equal(AuthorEntity.LINK).or(WorkEntity.CATEGORY_ID.equal(CategoryEntity.ID)))
                .leftJoin(Link.class).on(LinkEntity.ROOT_AUTHOR_ID.equal(AuthorEntity.LINK).or(LinkEntity.CATEGORY_ID.equal(CategoryEntity.ID)));
    }

    public AuthorEntity insertObservableAuthor(AuthorEntity entity) {
        entity.setObservable(true);
        Author author;
        if((author = getAuthor(entity.getLink())) != null) {
           return updateAuthor((AuthorEntity) author);
        }
        return doActionAuthor(Action.INSERT, entity);
    }

    public AuthorEntity updateAuthor(AuthorEntity entity) {
        return doActionAuthor(Action.UPDATE, entity);
    }

    public AuthorEntity doActionAuthor(Action action, AuthorEntity Author) {
        AuthorEntity result = (AuthorEntity) doAction(action, Author);
        if (action.equals(Action.UPDATE)) {
            for (Category category : Author.getCategories()) {
                doAction(action, category);
            }
        }
        return result;
    }

    public Persistable doAction(Action action, Object value) {
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
        doAction(Action.DELETE, getAuthor(author.getLink()));
    }

    public AuthorEntity getAuthor(String link) {
        return getAuthorQuery().where(AuthorEntity.LINK.eq(link)).get().firstOrNull();
    }

    public List<AuthorEntity> getObservableAuthors() {
        return getAuthorQuery().where(AuthorEntity.OBSERVABLE.eq(true)).get().toList();
    }

    public List<AuthorEntity> getObservableAuthors(int skip, int size) {
        return getAuthorQuery().where(AuthorEntity.OBSERVABLE.eq(true)).limit(size).offset(skip).get().toList();
    }

    public List<WorkEntity> getHistory(int skip, int size) {
        return dataStore.select(WorkEntity.class).distinct().where(WorkEntity.BOOKMARK_ID.notNull()).orderBy(WorkEntity.CACHED_DATE.desc()).limit(size).offset(skip).get().toList();
    }

    public WorkEntity getWork(String link) {
        return dataStore.select(WorkEntity.class).distinct().leftJoin(BookmarkEntity.class).on(WorkEntity.BOOKMARK_ID.eq(BookmarkEntity.ID)).where(WorkEntity.LINK.eq(link)).get().firstOrNull();
    }

    public WorkEntity insertOrUpdateWork(Work work) {
        WorkEntity workEntity = null;
        if (!(work instanceof WorkEntity)) {
           workEntity = getWork(work.getLink());
            if (workEntity == null) {
                return (WorkEntity) doAction(Action.INSERT, work.createEntity());
            } else {
                updateWork(workEntity, work);
                return (WorkEntity) doAction(Action.UPDATE, workEntity);
            }
        } else {
            return (WorkEntity) doAction(Action.UPDATE, work);
        }
    }


    public AuthorEntity resolveAuthor(Author author) {
          AuthorEntity authorEntity = getAuthor(author.getLink());
          if(authorEntity != null) {
              return authorEntity;
          }
          return author.createEntry();
    }

    public CategoryEntity resolveCategory(Work into, Category category) {
        if(into.getAuthor().isEntity()) {
            CategoryEntity result = (CategoryEntity) Stream.of(into.getAuthor().getCategories()).filter(cat -> cat.equals(category)).findFirst().orElse(null);
            if (result == null) {
                into.setCategory(category.createEntity());
                into.getAuthor().getCategories().add(into.getCategory());
                result = (CategoryEntity) into.getCategory();
            } else {
                result.addLink(into);
            }
            return result;
        } else {
            into.setCategory(into.getCategory().createEntity());
            return (CategoryEntity) into.getCategory();
        }
    }

    public void updateWork(Work into, Work from) {
        into.setTitle(from.getTitle());
        into.setLink(from.getLink());
        into.setRate(from.getRate());
        into.setKudoed(from.getKudoed());
        into.setGenres(from.getGenres());
        into.setType(from.getType());
        into.setAnnotationBlocks(from.getAnnotationBlocks());
        if (into.getCategory() != null && from.getCategory() != null && !into.getCategory().equals(from.getCategory())) {
            resolveCategory(into, from.getCategory());
        }
        into.setState(from.getState());
        into.setHasIllustration(from.isHasIllustration());
        into.setHasComments(from.isHasComments());
        into.setBookmark(from.getBookmark().createEntry());
        into.setChanged(from.isChanged());
    }
}
