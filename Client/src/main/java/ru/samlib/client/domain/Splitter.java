package ru.samlib.client.domain;

import org.intellij.lang.annotations.Language;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.regex.Pattern;

/**
 * Created by Rufim on 04.07.2015.
 */
public class Splitter {
    public Queue<String> start = new ArrayDeque<>();
    public Queue<String> end = new ArrayDeque<>();
    public Integer skip_start = 0;
    public Integer skip_end = 0;
    public int flag = 0;

    public Splitter() {
    }

    public Splitter(@Language("RegExp") String start, @Language("RegExp") String end) {
        this.start.add(start);
        this.end.add(end);
    }

    public Splitter(@Language("RegExp") String start, @Language("RegExp") String end, Integer skip_start, Integer skip_end) {
        this.start.add(start);
        this.end.add(end);
   //     this.skip_start = skip_start;
   //     this.skip_end = skip_end;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public Splitter addStart(@Language("RegExp") String start) {
        this.start.add(start);
        return this;
    }

    public Splitter addEnd(@Language("RegExp") String end) {
        this.end.add(end);
        return this;
    }

    public Splitter setSkip_start(Integer skip_start) {
  //      this.skip_start = skip_start;
        return this;
    }

    public Splitter setSkip_end(Integer skip_end) {
   //     this.skip_end = skip_end;
        return this;
    }

    public Pattern nextStart() {
        if (!start.isEmpty()) {
            return Pattern.compile(start.remove(), flag);
        }
        return null;
    }


    public Pattern nextEnd() {
        if (!end.isEmpty()) {
            return Pattern.compile(end.remove(), flag);
        }
        return null;
    }
}

