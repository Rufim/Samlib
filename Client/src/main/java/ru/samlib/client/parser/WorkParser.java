package ru.samlib.client.parser;

import android.util.Log;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.net.CachedResponse;
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
        CachedResponse rawContent = null;
        if (work.getRawContent() == null) {
            rawContent = HtmlClient.executeRequest(request, MIN_BODY_SIZE);
        } else {
            rawContent = HtmlClient.executeRequest(request);
        }
        if(rawContent == null) {
            return work;
        }
        work = ParserUtils.parseWork(rawContent, work);
        if (rawContent.isDownloadOver) {
            work.setParsed(true);
        }
        Log.e(TAG, "Work parsed using url " + request.getBaseUrl());
        return work;
    }
}
