package ru.samlib.client.util;

import android.content.Context;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.widget.Toast;
import net.nightwhistler.htmlspanner.HtmlSpanner;
import ru.kazantsev.template.util.GuiUtils;
import ru.kazantsev.template.util.TextUtils;
import ru.samlib.client.domain.entity.Work;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

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
    private int offset = 0;
    private int phraseIndex = 0;
    private List<Phrase> phrases;
    private Work work;
    private final Context context;
    private TextToSpeech tts;
    private boolean playOnStart = false;
    private State state = State.UNAVAILABLE;
    private float speechRate = 1.3f;
    private float pitch = 1f;
    private String language = null;

    public static class Phrase {
        public String text;
        public int start;
        public int end;
    }

    private OnIndexSpeakFinished indexSpeakFinished;
    private OnNextPhraseListener nextPhraseListener;
    private OnTTSPlayerStateChanged stateChanged;

    public interface OnNextPhraseListener {
        void onNextPhrase(int indentIndex, int phraseIndex, List<Phrase> phrases);
    }

    public interface OnIndexSpeakFinished {
        void onNextPhrase(int indentIndex);
    }

    public interface OnTTSPlayerStateChanged {
        void onStateChanged(State state);
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

    public void setOnNextPhraseListener(OnNextPhraseListener nextPhraseListener) {
        this.nextPhraseListener = nextPhraseListener;
    }

    public void setOnIndexSpeakFinished(OnIndexSpeakFinished indexSpeakFinished) {
        this.indexSpeakFinished = indexSpeakFinished;
    }

    public void setOnTTSPlayerStateChanged(OnTTSPlayerStateChanged stateChanged) {
        this.stateChanged = stateChanged;
    }

    private List<Phrase> splitByPhrases(String indent, int offset) {
        String clearIndent = new HtmlSpanner().fromHtml(indent).toString();
        List<String> rawPhrases = TextUtils.Splitter.split(clearIndent.substring(offset), "[.!?]++",
                TextUtils.Splitter.DelimiterMode.TO_END);
        List<Phrase> phrases = new ArrayList<>(rawPhrases.size());
        for (int i = 0; i < rawPhrases.size(); i++) {
            String phrase = rawPhrases.get(i);
            if (phrase.length() >= maxPhraseSize) {
                List<String> newPhrases = new ArrayList<>();
                while (phrase.length() >= maxPhraseSize) {
                    newPhrases.add(phrase.substring(0, maxPhraseSize));
                    phrase = phrase.substring(maxPhraseSize);
                }
                rawPhrases.remove(i);
                newPhrases.add(phrase);
                rawPhrases.addAll(i, newPhrases);
            }
        }
        int start = offset;
        for (String rawPhrase : rawPhrases) {
            Phrase phrase = new Phrase();
            phrase.text = rawPhrase;
            phrase.start = start;
            start += rawPhrase.length();
            phrase.end = start;
            phrases.add(phrase);
        }
        return phrases;
    }

    public void startSpeak(int index) {
        startSpeak(index, 0, 0);
    }

    public void startSpeak(int index, int offset) {
        startSpeak(index, 0, offset);
    }

    public void startSpeak(int index, int phrase, int offset) {
        if (tts == null) return;
        if(work.getIndents().isEmpty()) return;
        if (index >= work.getIndents().size()) {
            tts.stop();
            changeState(State.END);
            return;
        }
        if (index < 0) {
            index = 0;
        }
        phrases = splitByPhrases(work.getIndents().get(index), offset);
        phraseIndex = phrase;
        this.indentIndex = index;
        nextPhrase();
        changeState(State.SPEAKING);
    }

    public boolean isSpeaking() {
        return tts.isSpeaking() || State.SPEAKING == state;
    }

    public void resume() {
        if (phrases != null) {
            nextPhrase();
        } else {
            startSpeak(indentIndex);
        }
        changeState(State.SPEAKING);
    }

    public void start() {
        startSpeak(0);
    }

    public void pause() {
        tts.stop();
        changeState(State.PAUSE);
    }

    public void stop() {
        indentIndex = 0;
        phraseIndex = 0;
        phrases = null;
        tts.stop();
        changeState(State.STOPPED);
    }

    public void next() {
        stop();
        startSpeak(indentIndex + 1);
    }

    public void pre() {
        stop();
        startSpeak(indentIndex - 1);
    }

    public State getState() {
        return state;
    }

    private void nextPhrase() {
        if (phraseIndex >= phrases.size()) {
            if (indexSpeakFinished != null) {
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
        Phrase phrase = phrases.get(phraseIndex);
        if (nextPhraseListener != null) {
            GuiUtils.runInUI(context, new GuiUtils.RunUIThread() {
                @Override
                public void run(Object... var) {
                    nextPhraseListener.onNextPhrase((int) var[0], (int) var[1], (List<Phrase>) var[2]);
                }
            }, indentIndex, phraseIndex, phrases);
        }
        tts.speak(phrase.text, TextToSpeech.QUEUE_FLUSH, params);
        phraseIndex++;
    }

    public float getSpeechRate() {
        return speechRate;
    }

    public void setSpeechRate(float speechRate) {
        this.speechRate = speechRate;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setOnUtteranceCompletedListener(utteranceId -> {
                if (state == State.SPEAKING) {
                    nextPhrase();
                }
            });
            tts.setSpeechRate(speechRate);
            tts.setPitch(pitch);
            if(language == null) {
                tts.setLanguage(Locale.getDefault());
            } else {
                tts.setLanguage(new Locale(language));
            }
            changeState(State.IDLE);
            if (playOnStart) {
                playOnStart = false;
                startSpeak(indentIndex, offset);
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

    public void playOnStart(Work work, int index, int offset) {
        playOnStart = true;
        this.work = work;
        indentIndex = index;
        this.offset = offset;
    }

    public void onStop() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        changeState(State.UNAVAILABLE);
    }

    private synchronized void changeState(State state) {
        this.state = state;
        if(stateChanged != null) {
            stateChanged.onStateChanged(state);
        }
    }
}
