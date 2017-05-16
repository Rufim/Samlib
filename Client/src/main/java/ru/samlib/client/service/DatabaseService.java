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
import java.util.ArrayList;
import java.util.Arrays;
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

    public synchronized AuthorEntity insertObservableAuthor(AuthorEntity entity) {
        entity.setObservable(true);
        return createOrUpdateAuthor(entity);
    }

    public synchronized AuthorEntity createOrUpdateAuthor(AuthorEntity entity) {
        AuthorEntity authorEntity = getAuthor(entity.getLink());
        if(authorEntity == null) {
            return (AuthorEntity) doAction(Action.INSERT, entity);
        } else {
            List<Category> deleteCategories = new ArrayList<>(authorEntity.getCategories());
            for (Category category : entity.getCategories()) {
                CategoryEntity categoryEntity = resolveCategory(authorEntity, category);
                deleteCategories.remove(categoryEntity);
                if(categoryEntity != null) {
                    if(categoryEntity.getIdNoDB() == null) {
                        doAction(Action.INSERT, categoryEntity);
                    } else {
                        doAction(Action.UPDATE, categoryEntity);
                    }
                }
            }
            if(deleteCategories.size() > 0) {
                for (Category deleteCategory : deleteCategories) {
                    doAction(Action.DELETE, deleteCategories);
                }
            }
            return (AuthorEntity) doAction(Action.UPDATE, authorEntity);
        }
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

    public synchronized  WorkEntity insertOrUpdateWork(Work work) {
        return mergeWorkEntity(work);
    }


    public AuthorEntity resolveAuthor(Author author) {
        AuthorEntity authorEntity = getAuthor(author.getLink());
        if (authorEntity != null) {
            return authorEntity;
        }
        return author.createEntry();
    }


    private synchronized WorkEntity mergeWorkEntity(Work work) {
        Author author = work.getAuthor();
        Category category = work.getCategory();
        AuthorEntity authorEntity = getAuthor(author.getLink());
        WorkEntity workEntity = getWork(work.getLink());
        boolean insert = false;
        if (workEntity != null) {
            updateWork(workEntity, work);
        } else {
            workEntity = work.createEntity();
        }
        if (authorEntity == null) {
            insert = true;
            authorEntity = (AuthorEntity) workEntity.getAuthor();
        } else {
            workEntity.setAuthor(authorEntity);
        }
        createOrUpdateAuthor(authorEntity, author);
        addWorkToAuthor(workEntity, workEntity.getAuthor());
        if (category != null) {
            CategoryEntity categoryEntity = resolveCategory(workEntity, category);
            if(categoryEntity != null && !insert) {
                if(categoryEntity.getIdNoDB() == null) {
                    doAction(Action.INSERT, categoryEntity);
                } else {
                    doAction(Action.UPDATE, categoryEntity);
                }
            }
        }
        if(insert) {
            doAction(Action.INSERT, authorEntity);
        } else {
            doAction(Action.UPDATE, authorEntity);
        }
        if(workEntity.getBookmark() != null) {
            workEntity.getBookmark().setWork(workEntity);
            if(workEntity.getBookmark().getIdNoDB() == null) {
                doAction(Action.INSERT, workEntity.getBookmark());
            } else {
                doAction(Action.UPDATE, workEntity.getBookmark());
            }
        }
        return workEntity;
    }

    public CategoryEntity resolveCategory(Author author, Category category) {
        Category result = Stream.of(author.getCategories()).filter(cat -> cat.equals(category)).findFirst().orElse(null);
        CategoryEntity categoryEntity = null;
         if(result == null) {
             category.setAuthor(author);
             categoryEntity = category.createEntity();
             author.getCategories().add(categoryEntity);
         } else {
             if(!result.isEntity()) {
                 int index = author.getCategories().indexOf(result);
                 result.setAuthor(author);
                 result = result.createEntity();
                 author.getCategories().set(index, result);
             }
             for (Work work : category.getWorks()) {
                  addWorkToCategory(work, result);
             }
             categoryEntity = (CategoryEntity) result;
         }
        return categoryEntity;
    }


    public CategoryEntity resolveCategory(Work into, Category category) {
        Category result = Stream.of(into.getAuthor().getCategories()).filter(cat -> cat.equals(category)).findFirst().orElse(null);
        if (result == null) {
            addWorkToCategory(into, category);
            category.setAuthor(into.getAuthor());
            into.setCategory(category.createEntity());
            into.getAuthor().getCategories().add(into.getCategory());
        } else {
            if (!result.isEntity()) {
                int index = into.getAuthor().getCategories().indexOf(result);
                result.setAuthor(into.getAuthor());
                result = result.createEntity();
                into.getAuthor().getCategories().set(index, result);
            }
            addWorkToCategory(into, result);
            updateField(result::setAnnotation, category.getAnnotation());
            updateField(result::setType, category.getType());
            updateField(result::setLink, category.getLink());
        }
        return (CategoryEntity) into.getCategory();
    }

    private void addWorkToCategory(Work into, Category category) {
        List<Work> works;
        if (category instanceof CategoryEntity && category.getId() == null) {
            works = new ArrayList<>();
            works.add(into);
            category.setWorks(works);
        } else {
            boolean replaced = false;
            for (int i = 0; i < category.getWorks().size(); i++) {
                if (category.getWorks().get(i).getLink().equals(into.getLink())) {
                    category.getWorks().set(i, into);
                    replaced = true;
                }
            }
            if(!replaced) {
                category.getWorks().add(into);
            }
        }
        into.setCategory(category);
    }

    private void addWorkToAuthor(Work into, Author author) {
        if(author != null) {
            if(author.getWorks() == null) {
                author.setWorks(Arrays.asList(into));
            } else {
                if(!Stream.of(author.getWorks()).filter(works -> works.getLink().equals(into.getLink())).findFirst().isPresent()) {
                    author.getWorks().add(into);
                }
            }
            into.setAuthor(author);
        }
    }

    public void updateWork(Work into, Work from) {
        updateField(into::setTitle, from.getTitle());
        updateField(into::setLink, from.getLink());
        updateField(into::setRate, from.getRate());
        updateField(into::setKudoed, from.getKudoed());
        updateField(into::setGenres, from.getGenres());
        updateField(into::setType, from.getType());
        updateField(into::setAnnotationBlocks, from.getAnnotationBlocks());
        updateField(into::setState, from.getState());
        updateField(into::setHasIllustration, from.isHasIllustration());
        updateField(into::setHasComments, from.isHasComments());
        updateField(into::setBookmark, from.getBookmark().createEntry());
        updateField(into::setChanged, from.isChanged());
        updateField(into::setCachedDate, from.getCachedDate());
    }

    public void createOrUpdateAuthor(Author into, Author from) {
        updateField(into::setFullName, from.getFullName());
        updateField(into::setShortName, from.getShortName());
        updateField(into::setLink, from.getLink());
        updateField(into::setRate, from.getRate());
        updateField(into::setKudoed, from.getKudoed());
        updateField(into::setAbout, from.getAbout());
        updateField(into::setDateBirth, from.getDateBirth());
        updateField(into::setAnnotation, from.getAnnotation());
        updateField(into::setSite, from.getSite());
        updateField(into::setViews, from.getViews());
        updateField(into::setWorkCount, from.getWorkCount());
        updateField(into::setHasUpdates, from.isHasUpdates());
        updateField(into::setHasAbout, from.isHasAbout());
        updateField(into::setNewest, from.isNewest());
        updateField(into::setNotNotified, from.isNotNotified());
        updateField(into::setObservable, from.isObservable());
        updateField(into::setHasAvatar, from.isHasAvatar());
    }

    public <F> void updateField(UpdateAction<F> update, F field) {
        if (field != null) {
            update.updateField(field);
        }
    }


    public List<SavedHtml> selectCachedEntities() {
        return dataStore.select(SavedHtml.class).distinct().orderBy(SavedHtml.UPDATED.asc()).get().toList();
    }

    public void deleteCachedEntities(List<SavedHtml> delete) {
        dataStore.delete(delete);
    }

    interface UpdateAction<F> {
        void updateField(F f);
    }
}
