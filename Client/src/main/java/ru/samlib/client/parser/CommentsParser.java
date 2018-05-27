package ru.samlib.client.parser;

import net.vrallev.android.cat.Cat;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.kazantsev.template.domain.Valuable;
import ru.kazantsev.template.net.HTTPExecutor;
import ru.kazantsev.template.net.Header;
import ru.kazantsev.template.net.Response;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.Comment;
import ru.samlib.client.domain.entity.Link;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.lister.DefaultPageLister;
import ru.samlib.client.lister.RawRowSelector;
import ru.kazantsev.template.net.Request;
import ru.kazantsev.template.util.TextUtils;

import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Created by Dmitry on 29.10.2015.
 */
public class CommentsParser extends PageParser<Comment> {

    public static final String COMMENT_NEW_PREFIX = "/cgi-bin/comment";

    private Work work;
    private int archiveCount = -1;
    private int currentArchive = 0;

    public enum CommentParams implements Valuable {
        FILE(""), MSGID(""), OPERATION(""), NAME(""), EMAIL(""), URL(""), TEXT(""), add("Добавить!");

        private final String defaultValue;

        CommentParams(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        @Override
        public Object value() {
            return defaultValue;
        }
    }

    public enum Operation {
        store_new, store_edit, store_reply, edit, delete, reply;
    }

    public int getArchiveCount() {
        if (archiveCount < 0) {
            archiveCount = 0;
            try {
                lister.setPage(request, index);
                Document doc = getDocument(request);
                Elements arch = doc.select("b:contains(Архивы)");
                if (arch.size() > 0) {
                    String count = arch.text();
                    archiveCount = TextUtils.extractInt(count, 0);
                }
            } catch (Throwable ex) {
                Cat.e(ex);
            }
        }
        return archiveCount;
    }

    public void setArchive(int page) {
        if (page >= 0 && archiveCount >= page) {
            if(page == 0) {
                request.setSuffix("");
            } else {
                request.setSuffix("." + page);
            }
            pageCount = -1;
            currentArchive = page;
        }
    }

    public int getCurrentArchive() {
        return currentArchive;
    }

    public CommentsParser(Work work, boolean reverse) throws MalformedURLException {
        super(work.getCommentsLink().getLink(), new RawRowSelector() {

            @Override
            public String getRowStartDelimiter() {
                return "<small>\\d+\\.</small>";
            }

            @Override
            public String getRowEndDelimiter() {
                return "<hr noshade>";
            }

        }, new DefaultPageLister() {

            @Override
            public void setPage(Request request, int index) {
                request.addParam("PAGE", (index + 1));
            }

        });
        if (reverse) {
            request.addParam("ORDER", "reverse");
        }
        this.work = work;
    }

    @Override
    protected Comment parseRow(Element row, int position) {
        Comment comment = new Comment();
        comment.setWork(work);
        Elements smalls = row.select("small");
        comment.setNumber(TextUtils.extractInt(smalls.first().text()));
        String info = smalls.get(1).select("i").text();
        comment.setData(TextUtils.extractData(info, "/", ":"));
        if (info.contains("Удалено")) {
            comment.setDeleted(true);
            comment.setRawContent(info.substring(0, info.indexOf(".") + 1));
            Elements delete = row.select("a:contains(восстановить)");
            if (delete.size() > 0) {
                String link = delete.attr("href");
                comment.setMsgid(link.substring(link.indexOf(CommentParams.MSGID.name()) + CommentParams.MSGID.name().length() + 1));
                comment.setCanBeRestored(true);
            }
            return comment;
        }
        comment.setEmail(row.select("u").text());
        Element b = smalls.first().nextElementSibling();
        comment.setNickName(b.text());
        Elements a = b.select("a");
        if (a.size() != 0) comment.setLink(new Link(a.attr("href")));
        comment.setUserComment(b.select("font[color=red]").size() != 0);
        comment.setRawContent(TextUtils.Splitter.extractStrings(row.select("body").html(), true, "<br>", "<hr noshade>").get(0));
        Elements answer = row.select("a:contains(ответить)");
        if (answer.size() > 0) {
            String link = answer.attr("href");
            comment.setMsgid(link.substring(link.indexOf(CommentParams.MSGID.name()) + CommentParams.MSGID.name().length() + 1));
        }
        Elements change = row.select("a:contains(исправить)");
        if (change.size() > 0) {
            comment.setCanBeEdited(true);
        }
        Elements delete = row.select("a:contains(удалить)");
        if (delete.size() > 0) {
            comment.setCanBeDeleted(true);
        }
        return comment;
    }


    public static String requestCookie(Work work) {
        if (commentCookie != null) return commentCookie;
        try {
            Response response = new HTTPExecutor(new Request(Constants.Net.BASE_DOMAIN + COMMENT_NEW_PREFIX + "?COMMENT=" + work.getLinkWithoutSuffix())
                    .addHeader(Header.ACCEPT, ACCEPT_VALUE)
                    .addHeader(Header.USER_AGENT, USER_AGENT)).execute();
            String coockie = response.getHeaders().get(Header.SET_COOKIE).get(0);
            return commentCookie = HTTPExecutor.parseParamFromHeader(coockie, "COMMENT");
        } catch (Exception e) {
            return null;
        }
    }

    public static Response getAnswerForComment(Comment comment, Operation operation) {
        commentCookie = requestCookie(comment.getWork());
        if (commentCookie == null) {
            return null;
        }
        try {
            String link = Constants.Net.BASE_DOMAIN + comment.getReference();
            Request request = new Request(link)
                    .addHeader("Accept", ACCEPT_VALUE)
                    .addHeader("User-Agent", USER_AGENT)
                    .addHeader("Cookie", "COMMENT=" + commentCookie)
                    .addHeader("Host", Constants.Net.BASE_HOST)
                    .addHeader("Referer", link)
                    .addHeader("Upgrade-Insecure-Requests", "1")
                    .addParam(CommentParams.OPERATION, operation)
                    .addParam(CommentParams.MSGID, comment.getMsgid());
            if (comment.isDeleted() && operation.equals(Operation.delete)) {
                request.addParam("SUBOP", "rev");
            }
            return new HTTPExecutor(request).execute();
        } catch (Exception e) {
            return null;
        }
    }

    public static Response sendComment(Work work, CharSequence name, CharSequence email, CharSequence yourLink, CharSequence text, Operation operation, String msgid) {
        try {
            commentCookie = requestCookie(work);
            if (commentCookie == null) {
                return null;
            }
            String link = Constants.Net.BASE_DOMAIN + COMMENT_NEW_PREFIX;
            Request request = new Request(link)
                    .setMethod(Request.Method.POST)
                    .addHeader("Accept", ACCEPT_VALUE)
                    .addHeader("User-Agent", USER_AGENT)
                    .addHeader("Cookie", "COMMENT=" + commentCookie)
                    .addHeader("Host", Constants.Net.BASE_HOST)
                    .addHeader("Referer", link)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .addHeader("Upgrade-Insecure-Requests", "1")
                    .setEncoding("CP1251")
                    .addParam(CommentParams.FILE, work.getLinkWithoutSuffix())
                    .addParam(CommentParams.TEXT, text)
                    .addParam(CommentParams.NAME, name)
                    .addParam(CommentParams.EMAIL, email)
                    .addParam(CommentParams.URL, yourLink)
                    .addParam(CommentParams.MSGID, msgid)
                    .addParam(CommentParams.OPERATION, operation.name())
                    .addParam(CommentParams.add, "Добавить!");
            HTTPExecutor executor = new HTTPExecutor(request);
            return executor.execute();
        } catch (Exception e) {
            return null;
        }
    }
}
