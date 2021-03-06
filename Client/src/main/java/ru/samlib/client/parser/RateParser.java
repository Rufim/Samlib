package ru.samlib.client.parser;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.samlib.client.domain.entity.Author;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.lister.DefaultJsoupSelector;
import ru.samlib.client.lister.DefaultPageLister;

import java.math.BigDecimal;
import java.net.MalformedURLException;

/**
 * Created by Rufim on 07.01.2015.
 */
public class RateParser extends PageListParser<Work> {

    public RateParser() throws MalformedURLException {
        super("/rating/expert/", 250, new DefaultJsoupSelector(), new DefaultPageLister());
    }

    @Override
    protected Work parseRow(Element row, int position) {
        Elements rowItems = row.select("li").first().children();
        Work work = new Work();
        for (int j = 0; j < rowItems.size(); j++) {
            String text = rowItems.get(j).text();
            Element item = rowItems.get(j);
            switch (j) {
                case 0:
                    String expertRate = item.select("a").text();
                    work.setExpertRate(new BigDecimal(expertRate.split("\\*")[0]));
                    work.setExpertVotes(Integer.parseInt(expertRate.split("\\*")[1]));
                    break;
                case 1:
                    Author author = new Author();
                    author.setSmartLink(item.select("a[href]").attr("href"));
                    author.setFullName(item.text());
                    work.setAuthor(author);
                    break;
                case 2:
                    work.setTitle(item.text());
                    work.setSmartLink(item.select("a[href]").attr("href"));
                    break;
                case 3:
                    Elements info = item.select("b");
                    work.setSize(Integer.parseInt(info.get(0).text().replace("k", "")));
                    if (info.size() > 1) {
                        String[] rate = info.get(1).text().split("\\*");
                        work.setRate(new BigDecimal(rate[0]));
                        work.setVotes(Integer.parseInt(rate[1]));
                    }
                    String ownText = item.ownText();
                    work.setGenresAsString(ownText.substring(ownText.lastIndexOf("\u00A0") + 1).trim());
                    break;
            }
        }
        return work;
    }
}
