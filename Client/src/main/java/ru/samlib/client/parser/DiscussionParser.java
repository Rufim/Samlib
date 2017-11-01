package ru.samlib.client.parser;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.samlib.client.domain.entity.Discussion;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.lister.DefaultPageLister;
import ru.samlib.client.lister.JsoupRowSelector;
import ru.kazantsev.template.util.TextUtils;

import java.net.MalformedURLException;

/**
 * Created by Dmitry on 20.11.2015.
 */
public class DiscussionParser extends PageListParser<Discussion>{



    public DiscussionParser() throws MalformedURLException {
        super("/rating/comment/", 201,
                new JsoupRowSelector() {
                    @Override
                    public String getRowSelector() {
                        return "table[width=100%] tr";
                    }
                },
                new DefaultPageLister());
        setLazyLoad(true);
    }

    @Override
    protected Discussion parseRow(Element row, int position) {
        if(position == 0 || row.children().size() == 0) return null;
        Elements tds = row.select("td");
        Discussion discussion = new Discussion();
        Element aw = tds.get(0).select("a").first();
        Work work = new Work(aw.attr("href"));
        discussion.setWork(work);
        discussion.getWork().setTitle(aw.text());
        Element info = tds.get(0).select("small").first();
        String workSize = info.select("b").first().ownText();
        work.setSize(Integer.parseInt(workSize.substring(0, workSize.lastIndexOf("k"))));
        work.setGenresAsString(info.ownText());
        work.getAuthor().setFullName(tds.get(1).select("a").first().text());
        discussion.setAuthor(work.getAuthor());
        String counts[]  = tds.get(2).select("center").text().split("/");
        discussion.setCount(Integer.parseInt(counts[0]));
        if(counts.length > 1) {
            discussion.setCountOfDay(Integer.parseInt(counts[1]));
        }
        discussion.setLastOne(TextUtils.extractData(tds.get(3).select("center").text(), "/", ":", ", "));
        return discussion;
    }
}
