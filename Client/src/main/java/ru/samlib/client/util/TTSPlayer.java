package ru.samlib.client.util;

import android.content.Context;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;
import ru.samlib.client.domain.entity.Work;

import java.util.*;

/**
 * Created by 0shad on 01.08.2015.
 */
public class TTSPlayer implements TextToSpeech.OnInitListener {

    public enum State {
        SPEAKING, STOPPED, PAUSE, END, IDLE, UNAVAILABLE;
    }

    public static Handler TTS_HANDLER;
    public static int maxPhraseSize = 800;

    private int indentIndex = 0;
    private int phraseIndex = 0;
    private List<String> phrases;
    private Work work;
    private final Context context;
    private TextToSpeech tts;
    private boolean playOnStart = false;
    private State state = State.UNAVAILABLE;

    private OnIndexSpeakFinished indexSpeakFinished;
    private OnNextPhraseListener nextPhraseListener;

    public interface OnNextPhraseListener {
        void onNextPhrase(int indentIndex, int phraseIndex, List<String> phrases);
    }

    public interface OnIndexSpeakFinished {
        void onNextPhrase(int indentIndex);
    }

    public TTSPlayer(Work work, Context context) {
        this.work = work;
        this.context = context;
    }

    public TTSPlayer(Context context) {
        this.context = context;
    }

    public void setWork(Work work) {
        this.work = work;
    }

    public Work getWork() {
        return work;
    }

    public boolean isOver() {
        return state.equals(State.END);
    }

    public void setNextPhraseListener(OnNextPhraseListener nextPhraseListener) {
        this.nextPhraseListener = nextPhraseListener;
    }

    public void setIndexSpeakFinished(OnIndexSpeakFinished indexSpeakFinished) {
        this.indexSpeakFinished = indexSpeakFinished;
    }

    private List<String> splitByPhrases(String indent) {
        List<String> phrases = Splitter.split(ParserUtils.cleanHtml(indent), "[.!?]++",
                Splitter.DelimiterMode.TO_END);
        for (int i = 0; i < phrases.size(); i++) {
            String phrase = phrases.get(i);
            if (phrase.length() >= maxPhraseSize) {
                List<String> newPhrases = new ArrayList<>();
                while (phrase.length() >= maxPhraseSize) {
                    newPhrases.add(phrase.substring(0, maxPhraseSize));
                    phrase = phrase.substring(maxPhraseSize);
                }
                phrases.remove(i);
                newPhrases.add(phrase);
                phrases.addAll(i, newPhrases);
            }
        }
        return phrases;
    }

    public void startSpeak(int indentIndex) {
        if (tts == null) return;
        if (tts.isSpeaking() || indentIndex >= work.getIndents().size()) {
            tts.stop();
            state = State.END;
        }
        if(indentIndex < 0) {
            indentIndex = 0;
        }
        phrases = splitByPhrases(work.getIndents().get(indentIndex));
        phraseIndex = 0;
        this.indentIndex = indentIndex;
        nextPhrase();
        state = State.SPEAKING;
    }

    public boolean isSpeaking() {
        return tts.isSpeaking() || State.SPEAKING == state;
    }


    public void resume() {
        if(phrases != null) {
            nextPhrase();
        } else {
            startSpeak(indentIndex);
        }
        state = State.SPEAKING;
    }

    public void start() {
        startSpeak(0);
    }

    public void pause() {
        tts.stop();
        state = State.PAUSE;
    }

    public void stop() {
        indentIndex = 0;
        phraseIndex = 0;
        phrases = null;
        tts.stop();
        state = State.STOPPED;
    }

    public void next() {
        tts.stop();
        startSpeak(indentIndex + 1);
    }

    public void pre() {
        tts.stop();
        startSpeak(indentIndex - 1);
    }

    public State getState() {
        return state;
    }

    private void nextPhrase() {
        if (phraseIndex >= phrases.size()) {
            if(indexSpeakFinished != null) {
                GuiUtils.runInUI(context, new GuiUtils.RunUIThread() {
                    @Override
                    public void run(Object... var) {
                      indexSpeakFinished.onNextPhrase((int) var[0]);
                    }
                }, indentIndex);
            }
            startSpeak(indentIndex + 1);
            return;
        }
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "stringId");
        String phrase = phrases.get(phraseIndex);
        if (nextPhraseListener != null) {
            GuiUtils.runInUI(context, new GuiUtils.RunUIThread() {
                @Override
                public void run(Object ... var) {
                    nextPhraseListener.onNextPhrase((int)var[0],(int) var[1], (List<String>) var[2]);
                }
            }, indentIndex, phraseIndex, phrases);
        }
        tts.speak(phrase, TextToSpeech.QUEUE_FLUSH, params);
        phraseIndex++;
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setOnUtteranceCompletedListener(utteranceId -> {
                if (state == State.SPEAKING) {
                    nextPhrase();
                }
            });
            tts.setLanguage(Locale.getDefault());
            tts.setSpeechRate(1.3f);
            state = State.IDLE;
            if(playOnStart) {
                playOnStart = false;
                startSpeak(indentIndex);
            }
        } else {
            tts = null;
            Toast.makeText(context, "Failed to initialize TTS engine.", Toast.LENGTH_SHORT).show();
        }
    }

    public void reset() {
        onStop();
        onStart();
    }

    public void onStart() {
        tts = new TextToSpeech(context, this);
    }

    public void playOnStart(Work work, int index) {
        playOnStart = true;
        this.work = work;
        indentIndex = index;
    }

    public void onStop() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        state = State.UNAVAILABLE;
    }

    public static class Phrase {
        public int indentIndex;
        public int start;
        public int end;
        public String phrase;
    }
}
