package ru.samlib.client.parser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import ru.samlib.client.domain.entity.Comment;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.lister.PageLister;
import ru.samlib.client.lister.RawPageLister;
import ru.samlib.client.net.Request;
import ru.samlib.client.util.TextUtils;

/**
 * Created by Dmitry on 29.10.2015.
 */
public class CommentsParser extends PageParser<Comment> {

    private boolean reverse;


    public CommentsParser(Work work, int pageSize, boolean reverse) {
        super(!reverse ? work.getCommentsLink() : work.getCommentsLink() + "?ORDER=reverse", pageSize, new RawPageLister() {


            @Override
            public String getRowStartDelimiter() {
                return "<small>\\d+\\.</small>";
            }

            @Override
            public String getRowEndDelimiter() {
                return "<hr noshade>";
            }

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

        return comment;
    }
}
