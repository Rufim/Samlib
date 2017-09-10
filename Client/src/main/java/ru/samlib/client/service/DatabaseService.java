package ru.samlib.client.service;

import com.annimon.stream.Stream;


import net.vrallev.android.cat.Cat;
import ru.samlib.client.App;
import ru.samlib.client.domain.*;
import ru.samlib.client.domain.entity.*;
import java.util.*;


/**
 * Created by 0shad on 26.06.2016.
 */

public class DatabaseService {

    public enum Action {INSERT, UPDATE, DELETE, UPSERT}


    public DatabaseService() {
        App.getInstance().getComponent().inject(this);
    }

    public synchronized Author insertObservableAuthor(Author author) {
        return author;
    }

    public synchronized Author createOrUpdateAuthor(Author author) {
        return author;
    }

    private void updateCategory(Category category) {

    }

    public Object doAction(Action action, Object value) {
        switch (action) {
            case INSERT:

                break;
            case UPDATE:

                break;
            case DELETE:

                break;
            case UPSERT:

                break;
        }
        return value;
    }

    public void deleteAuthor(Author author) {
        doAction(Action.DELETE, author);
    }

    public void deleteAuthor(String link) {
        doAction(Action.DELETE, getAuthor(link));
    }

    public void deleteAuthors(Collection<Author> authors) {

    }

    public Author getAuthor(String link) {
        return new Author();
    }

    public List<Author> getObservableAuthors() {
        return new ArrayList<>();
    }

    public List<Author> getObservableAuthors(int skip, int size) {
        return new ArrayList<>();
    }

    public List<Bookmark> getHistory(int skip, int size) {
        return new ArrayList<>();
    }

    public void deleteHistory() {

    }

    public Work getWork(String link) {
        return new Work();
    }

    public synchronized Bookmark insertOrUpdateBookmark(Bookmark bookmark) {
        return new Bookmark();
    }

    public synchronized Bookmark getBookmark(String workUrl) {
        return new Bookmark();
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
        return new ArrayList<>();
    }

    public void saveHtml(SavedHtml savedHtml) {

    }

    public ExternalWork getExternalWork(String filePath) {
        return new ExternalWork();
    }


    public List<ExternalWork> selectExternalWorks(int skip, int size) {
        return new ArrayList<>();
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
        return externalWork;
    }

    public void deleteExternalWorks(List<ExternalWork> delete) {

    }

    public void deleteCachedEntities(List<SavedHtml> delete) {

    }

    interface UpdateAction<F> {
        void updateField(F f);
    }
}
