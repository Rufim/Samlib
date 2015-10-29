package ru.samlib.client.parser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import ru.samlib.client.domain.entity.Comment;
import ru.samlib.client.lister.ParserPageLister;
import ru.samlib.client.net.Request;
import ru.samlib.client.util.TextUtils;

/**
 * Created by Dmitry on 29.10.2015.
 */
public class CommentsParser extends PageParser<Comment> {

    private boolean reverse;


    public CommentsParser(String path, int pageSize, boolean reverse) {
        super(!reverse ? path : path + "?ORDER=reverse", pageSize, new ParserPageLister() {
            @Override
            public void setPage(Request request, int index) {
                request.setSuffix("?PAGE=" + (index + 1));
            }

            @Override
            public String getRowSelector() {
                return "body > small:matches(\\d+\\.) ";
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
        comment.setNumber(TextUtils.extractInt(row.ownText()));

        return comment;
    }
}
