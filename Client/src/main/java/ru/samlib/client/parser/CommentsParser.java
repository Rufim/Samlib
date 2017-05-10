package ru.samlib.client.parser;

import android.text.Editable;
import android.util.Log;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.kazantsev.template.domain.Valuable;
import ru.kazantsev.template.net.HTTPExecutor;
import ru.kazantsev.template.net.Response;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.domain.entity.Comment;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.lister.DefaultPageLister;
import ru.samlib.client.lister.RawRowSelector;
import ru.kazantsev.template.net.Request;
import ru.kazantsev.template.util.TextUtils;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;

/**
 * Created by Dmitry on 29.10.2015.
 */
public class CommentsParser extends PageParser<Comment> {

    public static final String COMMENT_NEW_PREFIX = "/cgi-bin/comment";

    public enum CommentParams implements Valuable {
        FILE(""), MSGID(""), OPERATION("store_new"), NAME(""), EMAIL(""), URL(""), TEXT(""), add("Добавить!");

        private final String defaultValue;

        CommentParams(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        @Override
        public Object value() {
            return defaultValue;
        }
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
        if(reverse) {
            request.addParam("ORDER", "reverse");
        }
    }

    @Override
    protected Comment parseRow(Element row, int position) {
        Comment comment = new Comment();
        Elements smalls = row.select("small");
        comment.setNumber(TextUtils.extractInt(smalls.first().text()));
        String info = smalls.get(1).select("i").text();
        comment.setData(TextUtils.extractData(info, "/", ":"));
        if(info.contains("Удалено")) {
            comment.setDeleted(true);
            comment.setRawContent(info.substring(0, info.indexOf(".") + 1));
            return comment;
        }
        comment.setEmail(row.select("u").text());
        Element b = smalls.first().nextElementSibling();
        comment.setNickName(b.text());
        Elements a = b.select("a");
        if(a.size() != 0) comment.setAuthor(new Author(a.attr("href")));
        comment.setUserComment(b.select("font[color=red]").size() != 0);
        comment.setRawContent(TextUtils.Splitter.extractStrings(row.select("body").html(), true, "<br>", "<hr noshade>").get(0));
        return comment;
    }

    public static String sendComment(Work work, CharSequence name, CharSequence email, CharSequence yourLink, CharSequence text) {
        try {
            Request request = new Request(Constants.Net.BASE_DOMAIN + COMMENT_NEW_PREFIX)
                    .setMethod(Request.Method.POST)
                    .addHeader("Accept", ACCEPT_VALUE)
                    .addHeader("User-Agent", USER_AGENT)
                    .addHeader("Content-Type","application/x-www-form-urlencoded")
                    .initParams(CommentParams.values())
                    .setEncoding("CP1251")
                    .addParam(CommentParams.TEXT, text)
                    .addParam(CommentParams.NAME, name)
                    .addParam(CommentParams.FILE, work.getLinkWithoutSuffix())
                    .addParam(CommentParams.EMAIL, email)
                    .addParam(CommentParams.URL, yourLink);
            HTTPExecutor executor = new HTTPExecutor(request);
            Response response = executor.execute();
            String resp = response.getRawContent("CP1251");
            return resp;
        } catch (Exception e) {
            return "";
        }
    }
}
