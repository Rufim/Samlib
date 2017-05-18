package ru.samlib.client.service;

import com.annimon.stream.Stream;
import io.requery.Persistable;
import io.requery.query.Condition;
import io.requery.query.JoinAndOr;
import io.requery.query.Result;
import io.requery.sql.EntityDataStore;
import io.requery.sql.MissingKeyException;
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
                authorEntity = (AuthorEntity) doAction(Action.UPDATE, authorEntity);
                for (Category category : authorEntity.getCategories()) {
                    updateCategory(category);
                }
            }
            for (Work work : authorEntity.getWorks()) {
                try {
                    doAction(Action.UPDATE, work);
                } catch (MissingKeyException ex) {
                }
            }
            for (Work work : authorEntity.getRootWorks()) {
                try {
                    doAction(Action.UPDATE, work);
                } catch (MissingKeyException ex) {
                }
            }
            return authorEntity;
        }
    }

    public void updateCategory(Category category) {
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
        return dataStore.select(WorkEntity.class).distinct().leftJoin(BookmarkEntity.class).on(BookmarkEntity.WORK_ID.equal(WorkEntity.LINK)).where(BookmarkEntity.WORK_ID.notNull()).orderBy(WorkEntity.CACHED_DATE.desc()).limit(size).offset(skip).get().toList();
    }

    public WorkEntity getWork(String link) {
        return dataStore.select(WorkEntity.class).distinct().leftJoin(BookmarkEntity.class).on(BookmarkEntity.WORK_ID.equal(WorkEntity.LINK)).where(WorkEntity.LINK.eq(link)).get().firstOrNull();
    }

    public synchronized WorkEntity insertOrUpdateWork(Work work) {
        return mergeWorkEntity(work);
    }


    public AuthorEntity resolveAuthor(Author author) {
        AuthorEntity authorEntity = getAuthor(author.getLink());
        if (authorEntity != null) {
            return authorEntity;
        }
        return author.createEntity();
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
        updateAuthor(authorEntity, author);
        addWorkToAuthor(workEntity, authorEntity);
        if (category != null) {
            CategoryEntity categoryEntity = resolveCategory(authorEntity, workEntity, category);
            if (categoryEntity != null && !insert) {
                if (categoryEntity.getIdNoDB() != null) {
                    doAction(Action.UPDATE, categoryEntity);
                    for (Work workEn : categoryEntity.getWorks()) {
                        try {
                            doAction(Action.UPDATE, workEn);
                        } catch (Exception ex) {
                        }
                    }
                }
            }
        }
        if (insert) {
            doAction(Action.INSERT, authorEntity);
        } else {
            doAction(Action.UPDATE, authorEntity);
        }
        if (workEntity.getBookmark() != null) {
            if (workEntity.getBookmark().getIdNoDB() == null) {
                doAction(Action.INSERT, workEntity.getBookmark());
            } else {
                doAction(Action.UPDATE, workEntity.getBookmark());
            }
            doAction(Action.UPDATE, workEntity);
        }
        return workEntity;
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
            if(into.getCategory() != null) {
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
        updateField(into::setBookmark, from.getBookmark().createEntity());
        updateField(into::setChanged, from.isChanged());
        updateField(into::setCachedDate, from.getCachedDate());
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


    public void saveExternalWork(ExternalWork externalWork) {
        ExternalWork work = dataStore.findByKey(ExternalWork.class, externalWork.getFilePath());
        if (work == null) {
            dataStore.insert(externalWork);
        } else {
            dataStore.update(externalWork);
        }
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
