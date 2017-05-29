package ru.samlib.client.service;

import com.annimon.stream.Stream;
import io.requery.Persistable;
import io.requery.query.Condition;
import io.requery.query.JoinAndOr;
import io.requery.query.Result;
import io.requery.sql.EntityDataStore;
import io.requery.sql.MissingKeyException;
import net.vrallev.android.cat.Cat;
import ru.samlib.client.App;
import ru.samlib.client.domain.entity.*;
import ru.samlib.client.fragments.WorkFragment;

import javax.inject.Inject;
import java.util.*;


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

    public DatabaseService(EntityDataStore<Persistable> dataStore) {
        this.dataStore = dataStore;
    }

    private JoinAndOr<Result<AuthorEntity>> getAuthorQuery() {
        return dataStore.select(AuthorEntity.class).distinct()
                .leftJoin(CategoryEntity.class).on((Condition) CategoryEntity.AUTHOR_ID.equal(AuthorEntity.LINK))
                .leftJoin(WorkEntity.class).on(WorkEntity.AUTHOR_ID.equal(AuthorEntity.LINK).or(WorkEntity.CATEGORY_ID.equal(CategoryEntity.ID)))
                .leftJoin(LinkEntity.class).on(LinkEntity.AUTHOR_ID.equal(AuthorEntity.LINK).or(LinkEntity.CATEGORY_ID.equal(CategoryEntity.ID)));
    }

    public synchronized AuthorEntity insertObservableAuthor(AuthorEntity entity) {
        entity.setObservable(true);
        return createOrUpdateAuthor(entity);
    }

    public synchronized AuthorEntity createOrUpdateAuthor(AuthorEntity entity) {
        AuthorEntity authorEntity = getAuthor(entity.getLink());
        if (authorEntity == null) {
            for (Category category : new ArrayList<>(entity.getCategories())) {
                category.setAuthor(entity);
                for (Work work : new ArrayList<>(addCategoryToAuthor(entity, category).getWorks())) {
                    work.setAuthor(entity);
                    resolveCategory(entity, work, category);
                }
            }
            return (AuthorEntity) doAction(Action.INSERT, entity);
        } else {
            if (entity != authorEntity) {
                updateAuthor(authorEntity, entity);
                List<Category> deleteCategories = new ArrayList<>(authorEntity.getCategories());
                for (Category category : new ArrayList<>(entity.getCategories())) {
                    CategoryEntity categoryEntity = addCategoryToAuthor(authorEntity, category);
                    deleteCategories.remove(categoryEntity);
                    if (categoryEntity != null) {
                        updateCategory(categoryEntity);
                    }
                }
                if (deleteCategories.size() > 0) {
                    authorEntity.getCategories().removeAll(deleteCategories);
                }
                Iterator<Work> itWork = authorEntity.getWorks().iterator();
                while (itWork.hasNext()) {
                    if (!entity.getWorks().contains(itWork.next())) {
                        itWork.remove();
                    }
                }
                Iterator<Link> itLink = authorEntity.getLinks().iterator();
                while (itLink.hasNext()) {
                    if (!entity.getLinks().contains(itLink.next())) {
                        itLink.remove();
                    }
                }
                for (Work work : new ArrayList<>(entity.getWorks())) {
                    addWorkToAuthor(work, authorEntity);
                }
                for (Link link : new ArrayList<>(entity.getLinks())) {
                    addLinkToAuthor(link, authorEntity);
                }
                authorEntity = (AuthorEntity) doAction(Action.UPDATE, authorEntity);
            } else {
                for (Work work : authorEntity.getWorks()) {
                    try {
                        doAction(Action.UPDATE, work);
                    } catch (Exception ex) {
                        Cat.e(ex, work.getCategory().toString());
                    }
                }
                for (Link link : authorEntity.getLinks()) {
                    try {
                        doAction(Action.UPDATE, link);
                    } catch (MissingKeyException ex) {
                    }
                }
                for (Category category : authorEntity.getCategories()) {
                    updateCategory(category);
                }
                authorEntity = (AuthorEntity) doAction(Action.UPDATE, authorEntity);
            }
            return authorEntity;
        }
    }

    private void updateCategory(Category category) {
        if (category.getIdNoDB() != null) {
            doAction(Action.UPDATE, category);
            for (Work work : category.getWorks()) {
                try {
                    doAction(Action.UPDATE, work);
                } catch (MissingKeyException ex) {
                }
            }
            for (Link link : category.getLinks()) {
                try {
                    doAction(Action.UPDATE, link);
                } catch (MissingKeyException ex) {
                }
            }
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

    public void deleteAuthors(Collection<AuthorEntity> authors) {
        try {
            for (AuthorEntity author : authors) {
                dataStore.delete(AuthorEntity.class).where(AuthorEntity.LINK.eq(author.getLink())).get().value();
            }
        } catch (Exception ex) {
          Cat.e(ex);
        }
    }

    public AuthorEntity getAuthor(String link) {
        return getAuthorQuery().where(AuthorEntity.LINK.eq(link)).get().firstOrNull();
    }

    public List<AuthorEntity> getObservableAuthors() {
        return getAuthorQuery().where(AuthorEntity.OBSERVABLE.eq(true)).orderBy(AuthorEntity.LAST_UPDATE_DATE.desc()).get().toList();
    }

    public List<AuthorEntity> getObservableAuthors(int skip, int size) {
        return getAuthorQuery().where(AuthorEntity.OBSERVABLE.eq(true)).orderBy(AuthorEntity.LAST_UPDATE_DATE.desc()).limit(size).offset(skip).get().toList();
    }

    public List<BookmarkEntity> getHistory(int skip, int size) {
        return dataStore.select(BookmarkEntity.class).where(BookmarkEntity.AUTHOR_URL.notNull()).orderBy(BookmarkEntity.SAVED_DATE.desc()).limit(size).offset(skip).get().toList();
    }

    public void deleteHistory() {
        dataStore.delete(BookmarkEntity.class).get().value();
    }

    public WorkEntity getWork(String link) {
        return dataStore.select(WorkEntity.class).where(WorkEntity.LINK.eq(link)).get().firstOrNull();
    }

    public synchronized BookmarkEntity insertOrUpdateBookmark(Bookmark bookmark) {
        BookmarkEntity bookmarkEntity = getBookmark(bookmark.getWorkUrl());
        if (bookmarkEntity == null) {
            bookmark.setSavedDate(new Date());
            bookmarkEntity = (BookmarkEntity) doAction(Action.INSERT, bookmark.createEntity());
        } else {
            bookmarkEntity.setSavedDate(new Date());
            updateField(bookmarkEntity::setIndent, bookmark.getIndent());
            updateField(bookmarkEntity::setAuthorShortName, bookmark.getAuthorShortName());
            updateField(bookmarkEntity::setPercent, bookmark.getPercent());
            updateField(bookmarkEntity::setTitle, bookmark.getTitle());
            updateField(bookmarkEntity::setWorkTitle, bookmark.getWorkTitle());
            updateField(bookmarkEntity::setIndentIndex, bookmark.getIndentIndex());
            updateField(bookmarkEntity::setAuthorUrl, bookmark.getAuthorUrl());
            updateField(bookmarkEntity::setGenres, bookmark.getGenres());
            bookmarkEntity = (BookmarkEntity) doAction(Action.UPDATE, bookmarkEntity);
        }
        return bookmarkEntity;
    }

    public synchronized BookmarkEntity getBookmark(String workUrl) {
        return dataStore.findByKey(BookmarkEntity.class, workUrl);
    }

    public CategoryEntity addCategoryToAuthor(AuthorEntity author, Category category) {
        Category result = Stream.of(author.getCategories()).filter(cat -> cat.equals(category)).findFirst().orElse(null);
        CategoryEntity categoryEntity = null;
        if (result == null) {
            categoryEntity = category.createEntity(author);
        } else {
            if (!result.isEntity()) {
                int index = author.getCategories().indexOf(result);
                result = result.createEntity(author);
                author.getCategories().set(index, result);
            }
            if (result != author.getCategories().get(author.getCategories().size() - 1)) {
                author.getCategories().remove(result);
                author.getCategories().add(result);
            }
            categoryEntity = (CategoryEntity) result;
            Iterator<Work> itWork = result.getWorks().iterator();
            while (itWork.hasNext()) {
                if (!category.getWorks().contains(itWork.next())) {
                    itWork.remove();
                }
            }
            Iterator<Link> itLink = result.getLinks().iterator();
            while (itLink.hasNext()) {
                if (!category.getLinks().contains(itLink.next())) {
                    itLink.remove();
                }
            }
            for (Work work : new ArrayList<>(category.getWorks())) {
                work.setAuthor(author);
                addWorkToCategory(author, work, categoryEntity);
            }
            for (Link link : new ArrayList<>(category.getLinks())) {
                link.setAuthor(author);
                addLinkToCategory(author, link, categoryEntity);
            }
        }
        return categoryEntity;
    }


    public CategoryEntity resolveCategory(AuthorEntity authorEntity, Work into, Category category) {
        Category result = Stream.of(into.getAuthor().getCategories()).filter(cat -> cat.equals(category)).findFirst().orElse(null);
        if (result == null) {
            into.setCategory(category.createEntity(authorEntity));
            addWorkToCategory(authorEntity, into, (CategoryEntity) into.getCategory());
            into.getAuthor().getCategories().add(into.getCategory());
        } else {
            if (!result.isEntity()) {
                int index = into.getAuthor().getCategories().indexOf(result);
                result = result.createEntity(authorEntity);
                into.getAuthor().getCategories().set(index, result);
            }
            addWorkToCategory(authorEntity, into, (CategoryEntity) result);
            updateField(result::setAnnotation, category.getAnnotation());
            updateField(result::setType, category.getType());
            updateField(result::setLink, category.getLink());
        }
        return (CategoryEntity) into.getCategory();
    }

    private void addWorkToCategory(AuthorEntity authorEntity, Work into, CategoryEntity category) {
        List<Work> works;
        if (category.getIdNoDB() == null) {
            if (category.getOriginalWorks() == null) {
                works = new ArrayList<>();
            } else {
                works = category.getOriginalWorks();
            }
        } else {
            works = category.getWorks();
        }
        into.setAuthor(authorEntity);
        into.setCategory(category);
        if (!into.isEntity()) {
            into.createEntity(authorEntity, category);
        } else {
            for (int i = 0; i < works.size(); i++) {
                if (works.get(i).equals(into)) {
                    works.remove(i);
                }
            }
            works.add(into);
        }
    }


    private void addLinkToCategory(AuthorEntity authorEntity, Link into, CategoryEntity category) {
        List<Link> links;
        if (category.getIdNoDB() == null) {
            if (category.getOriginalLinks() == null) {
                links = new ArrayList<>();
            } else {
                links = category.getOriginalLinks();
            }
        } else {
            links = category.getLinks();
        }
        into.setAuthor(authorEntity);
        into.setCategory(category);
        if (into.getClass() != LinkEntity.class) {
            into = into.createEntity(authorEntity, category);
        }
        for (int i = 0; i < links.size(); i++) {
            if (links.get(i).equals(into)) {
                links.remove(i);
            }
        }
        links.add(into);
    }

    private void addWorkToAuthor(Work into, Author author) {
        if (author != null && (into.isRootWork() || into.isRecommendation())) {
            if (author.getWorks() == null) {
                author.setWorks(Arrays.asList(into));
            } else {
                if (!Stream.of(author.getWorks()).filter(works -> works.getLink().equals(into.getLink())).findFirst().isPresent()) {
                    author.getWorks().add(into);
                }
            }
            if (into.getCategory() != null) {
                into.getCategory().setAuthor(author);
            }
            into.setAuthor(author);
        }
    }

    private void addLinkToAuthor(Link into, Author author) {
        if (author != null && into.isRootLink()) {
            if (author.getLinks() == null) {
                author.setLinks(Arrays.asList(into));
            } else {
                if (!Stream.of(author.getLinks()).filter(links -> links.equals(into)).findFirst().isPresent()) {
                    author.getLinks().add(into);
                }
            }
            if (into.getCategory() != null) {
                into.getCategory().setAuthor(author);
            }
            into.setAuthor(author);
        }
    }

    public void updateAuthor(Author into, Author from) {
        updateField(into::setFullName, from.getFullName());
        updateField(into::setShortName, from.getShortName());
        updateField(into::setLink, from.getLink());
        updateField(into::setRate, from.getRate());
        updateField(into::setKudoed, from.getKudoed());
        updateField(into::setAbout, from.getAbout());
        updateField(into::setDateBirth, from.getDateBirth());
        updateField(into::setAnnotation, from.getAnnotation());
        updateField(into::setAuthorSiteUrl, from.getAuthorSiteUrl());
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
        return dataStore.select(SavedHtml.class).orderBy(SavedHtml.UPDATED.asc()).get().toList();
    }

    public void saveHtml(SavedHtml savedHtml) {
        SavedHtml htmlDB = dataStore.findByKey(SavedHtml.class, savedHtml.getFilePath());
        if (htmlDB == null) {
            dataStore.insert(savedHtml);
        } else {
            dataStore.update(savedHtml);
        }
    }

    public ExternalWork getExternalWork(String filePath) {
        return dataStore.select(ExternalWork.class).where(ExternalWork.FILE_PATH.eq(filePath)).get().firstOrNull();
    }


    public List<ExternalWork> selectExternalWorks(int skip, int size) {
        return dataStore.select(ExternalWork.class).orderBy(ExternalWork.SAVED_DATE.asc()).limit(size).offset(skip).get().toList();
    }

    public ExternalWork saveExternalWork(Work work, String filePath) {
        ExternalWork externalWork = new ExternalWork();
        updateField(externalWork::setFilePath, filePath);
        updateField(externalWork::setWorkUrl, work.getFullLink());
        updateField(externalWork::setGenres, work.printGenres());
        updateField(externalWork::setWorkTitle, work.getTitle());
        updateField(externalWork::setAuthorShortName, work.getAuthor().getShortName());
        updateField(externalWork::setAuthorUrl, work.getAuthor().getFullLink());
        return insertOrUpdateExternalWork(externalWork);
    }

    public ExternalWork insertOrUpdateExternalWork(ExternalWork externalWork) {
        ExternalWork work = dataStore.findByKey(ExternalWork.class, externalWork.getFilePath());
        if (work == null) {
            externalWork.setSavedDate(new Date());
            work = (ExternalWork) doAction(Action.INSERT, externalWork);
        } else {
            work.setSavedDate(new Date());
            updateField(work::setAuthorShortName, externalWork.getAuthorShortName());
            updateField(work::setWorkUrl, externalWork.getWorkUrl());
            updateField(work::setWorkTitle, externalWork.getWorkTitle());
            updateField(work::setAuthorUrl, externalWork.getAuthorUrl());
            updateField(work::setGenres, externalWork.getGenres());
            work = (ExternalWork) doAction(Action.UPDATE, work);
        }
        return work;
    }

    public void deleteExternalWorks(List<ExternalWork> delete) {
        dataStore.delete(delete);
    }

    public void deleteCachedEntities(List<SavedHtml> delete) {
        dataStore.delete(delete);
    }

    interface UpdateAction<F> {
        void updateField(F f);
    }
}
