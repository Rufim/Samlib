package ru.samlib.client.service;

import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;
import com.annimon.stream.Stream;


import com.raizlabs.android.dbflow.config.DatabaseDefinition;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.Operator;
import com.raizlabs.android.dbflow.sql.language.OrderBy;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.structure.BaseModel;
import com.raizlabs.android.dbflow.structure.Model;
import com.raizlabs.android.dbflow.structure.database.DatabaseWrapper;
import com.raizlabs.android.dbflow.structure.database.transaction.ProcessModelTransaction;
import com.raizlabs.android.dbflow.structure.database.transaction.Transaction;
import net.vrallev.android.cat.Cat;
import ru.samlib.client.App;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.*;
import ru.samlib.client.util.DBFlowUtils;

import java.time.Duration;
import java.time.Period;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static ru.samlib.client.util.DBFlowUtils.*;


/**
 * Created by 0shad on 26.06.2016.
 */

public class DatabaseService {


    public enum Action {INSERT, UPDATE, DELETE, UPSERT}


    public void insertOrUpdateSavedHtml(SavedHtml savedHtml) {
        doAction(Action.UPSERT, savedHtml);
    }

    public SavedHtml getSavedHtml(String absolutePath) {
        return dbFlowFindFirst(SavedHtml.class, SavedHtml_Table.filePath.eq(absolutePath));
    }

    public DatabaseService() {
        App.getInstance().getComponent().inject(this);
    }

    public synchronized Author insertObservableAuthor(Author author) {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY,0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        author.setObservable(true);
        if(author.getLastUpdateDate() == null) {
            author.setLastUpdateDate(new Date());
        } else if(TimeUnit.MILLISECONDS.toDays(today.getTime().getTime()) == TimeUnit.MILLISECONDS.toDays(author.getLastUpdateDate().getTime())) {
            Calendar calendar = Calendar.getInstance();
            Calendar now = Calendar.getInstance();
            calendar.setTime(author.getLastUpdateDate());
            calendar.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY));
            calendar.set(Calendar.MINUTE, now.get(Calendar.MINUTE));
            calendar.set(Calendar.SECOND, now.get(Calendar.SECOND));
            calendar.set(Calendar.MILLISECOND, now.get(Calendar.MILLISECOND));
            author.setLastUpdateDate(calendar.getTime());
        }
        author.setLastCheckedDate(new Date());
        doAction(Action.INSERT, author);
        doAction(Action.UPSERT, author.getCategories());
        return getAuthor(author.getLink());
    }

    public synchronized Author createOrUpdateAuthor(Author author) {
        doAction(Action.UPSERT, author);
        doAction(Action.UPSERT, author.getCategories());
        author = getAuthor(author.getLink());
        return author;
    }

    private void updateCategory(Category category) {
        doAction(Action.UPDATE, category);
    }

    public <C extends BaseModel> C doAction(Action action, C value, boolean async) {
        boolean result = false;
        Model model = value;
        if(async) {
            model = value.async();
        }
        switch (action) {
            case INSERT:
                result = model.insert() > 0;
                break;
            case UPDATE:
                result = model.update();
                break;
            case DELETE:
                result = model.delete();
                break;
            case UPSERT:
                result = model.save();
                break;
        }
        if (!result) {
            Cat.e("Error in DB operation:" + action + ". See log for more info. Class:" + value.getClass());
        }
        return value;
    }

    public <C extends BaseModel> C doAction(Action action, C value) {
        return doAction(action, value, false);
    }

    public <C extends BaseModel> void doAction(Action action, Collection<C> list) {
        doAction(action, list, false);
    }

    public <C extends BaseModel> void doAction(Action action, Collection<C> list, boolean async) {
        ProcessModelTransaction<C> processModelTransaction =
                new ProcessModelTransaction.Builder<>(new ProcessModelTransaction.ProcessModel<C>() {
                    @Override
                    public void processModel(C model, DatabaseWrapper wrapper) {
                        doAction(action, model);
                    }
                }).addAll(list).build();
        DatabaseDefinition database = FlowManager.getDatabase(Constants.App.DATABASE_NAME);
        Transaction transaction = database.beginTransactionAsync(processModelTransaction).error(new Transaction.Error() {
            @Override
            public void onError(@NonNull Transaction transaction, @NonNull Throwable error) {
                Cat.e("Error in DB operation:" + action + ".", error);
            }
        }).build();
        if (async) {
            transaction.execute();
        } else {
            transaction.executeSync();
        }
    }

    public void deleteAuthor(Author author) {
        doAction(Action.DELETE, author);
    }

    public void deleteAuthor(String link) {
        dbFlowDelete(Author.class, Author_Table.link.eq(link));
    }

    public void deleteAuthors(Collection<Author> authors) {
        doAction(Action.DELETE, authors);
    }

    public Author getAuthor(String link) {
        return dbFlowFindFirst(Author.class, Author_Table.link.eq(link));
    }

    public List<Author> getObservableAuthors() {
        return SQLite.select().from(Author.class).orderBy(Author_Table.hasUpdates, false).orderBy(Author_Table.lastUpdateDate, false).queryList();
    }

    public List<Author> getObservableAuthors(int skip, int size) {
        return SQLite.select().from(Author.class).offset(skip).limit(size).orderBy(Author_Table.hasUpdates, false).orderBy(Author_Table.lastUpdateDate, false).queryList();
    }

    public List<Bookmark> getHistory(int skip, int size) {
        return SQLite.select().from(Bookmark.class).offset(skip).limit(size).orderBy(Bookmark_Table.savedDate, false).queryList();
    }

    public List<Bookmark> getHistory(int skip, int size, boolean locked) {
        return SQLite.select().from(Bookmark.class).where(Bookmark_Table.userBookmark.eq(locked)).offset(skip).limit(size).orderBy(Bookmark_Table.savedDate, false).queryList();
    }

    public void deleteHistory() {
        SQLite.delete().from(Bookmark.class).where(Bookmark_Table.userBookmark.eq(false)).or(Bookmark_Table.userBookmark.isNull()).execute();
    }

    public Work getWork(String link) {
        return dbFlowFindFirst(Work.class, Work_Table.link.eq(link));
    }

    public synchronized Bookmark insertOrUpdateBookmark(Bookmark bookmark) {
        bookmark.setSavedDate(new Date());
        return doAction(Action.UPSERT, bookmark);
    }

    public synchronized Bookmark getBookmark(String workUrl) {
        return dbFlowFindFirst(Bookmark.class, Bookmark_Table.workUrl.eq(workUrl));
    }


    public List<SavedHtml> selectCachedEntities() {
        return dbFlowQueryList(SavedHtml.class, null);
    }

    public void saveHtml(SavedHtml savedHtml) {
        doAction(Action.INSERT, savedHtml);
    }

    public ExternalWork getExternalWork(String filePath) {
        return dbFlowFindFirst(ExternalWork.class, ExternalWork_Table.filePath.eq(filePath));
    }

    public ExternalWork insertOrUpdateExternalWork(ExternalWork externalWork) {
        externalWork.setSavedDate(new Date());
        return doAction(Action.UPSERT, externalWork);
    }

    public void deleteExternalWorks(List<ExternalWork> delete) {
        doAction(Action.DELETE, delete);
    }

    public void deleteCachedEntities(List<SavedHtml> delete) {
        doAction(Action.DELETE, delete);
    }

    public List<ExternalWork> selectExternalWorks(int skip, int size) {
        return dbFlowQueryList(ExternalWork.class, skip, size, OrderBy.fromProperty(ExternalWork_Table.savedDate).descending(), ExternalWork_Table.filePath.notLike("/data/data/%"));
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
        updateField(into::setVotes, from.getVotes());
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

    interface UpdateAction<F> {
        void updateField(F f);
    }
}
