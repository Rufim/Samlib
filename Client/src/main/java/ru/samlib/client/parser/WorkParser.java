package ru.samlib.client.parser;

import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.net.HtmlClient;
import ru.samlib.client.util.ParserUtils;

import java.net.MalformedURLException;

/**
 * Created by Rufim on 04.07.2015.
 */
public class WorkParser extends Parser {

    private Work work;

    public WorkParser(Work work) throws MalformedURLException {
        setPath(work.getLink());
        this.work = work;
    }

    public WorkParser(String workLink) throws MalformedURLException {
        setPath(workLink);
        this.work = new Work(workLink);
    }

    public Work parse() {
        return ParserUtils.parseWork(HtmlClient.executeRequest(request));
    }
}
