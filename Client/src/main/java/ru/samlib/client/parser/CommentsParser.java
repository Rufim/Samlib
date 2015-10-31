package ru.samlib.client.parser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.domain.entity.Comment;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.lister.PageLister;
import ru.samlib.client.lister.RawRowSelector;
import ru.samlib.client.net.Request;
import ru.samlib.client.util.TextUtils;

import java.net.MalformedURLException;

/**
 * Created by Dmitry on 29.10.2015.
 */
public class CommentsParser extends PageParser<Comment> {

    private boolean reverse;


    public CommentsParser(Work work, boolean reverse) throws MalformedURLException {
        super(!reverse ? work.getCommentsLink() : work.getCommentsLink() + "?ORDER=reverse", -1, new RawRowSelector() {

            @Override
            public String getRowStartDelimiter() {
                return "<small>\\d+\\.</small>";
            }

            @Override
            public String getRowEndDelimiter() {
                return "<hr noshade>";
            }

        }, new PageLister() {

            @Override
            public void setPage(Request request, int index) {
                request.setSuffix("?PAGE=" + (index + 1));
            }


            @Override
            public int getLastPage(Document document) {
                return TextUtils.extractInt(document.select("center > b:contains(Страниц)").text());
            }
        });
    }

    @Override
    protected Comment parseRow(Element row) {
        Comment comment = new Comment();
        Elements smalls = row.select("small");
        comment.setNumber(TextUtils.extractInt(smalls.first().text()));
        comment.setData(TextUtils.extractData(smalls.get(1).select("i").text(), "/", ":"));
        comment.setEmail(row.select("u").text());
        Element b = smalls.first().nextElementSibling();
        comment.setNickName(b.text());
        Elements a = b.select("a");
        if(a.size() != 0) comment.setAuthor(new Author(a.attr("href")));
        comment.setUserComment(b.select("font[color=red]").size() != 0);
        comment.setRawContent("<br>" + TextUtils.Splitter.extractStrings(row.html(), true, "<br>", "<hr noshade>").get(0));
        return comment;
    }
}
