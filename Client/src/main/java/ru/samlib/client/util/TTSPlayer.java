package ru.samlib.client.util;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;
import ru.samlib.client.domain.entity.Work;

import java.util.*;

/**
 * Created by 0shad on 01.08.2015.
 */
public class TTSPlayer implements TextToSpeech.OnInitListener {

    public enum STATE {
        SPEAKING, STOPPED, PAUSE;
    }

    private int speakIndex = 0;
    private int phraseIndex = 0;
    private List<String> phrases;
    private int maxPhraseSize = 200;
    private final Work work;
    private final Context context;
    private TextToSpeech tts;
    private STATE state = STATE.STOPPED;

    private OnIndexSpeakFinished indexSpeakFinished;
    private OnNextPhraseListener nextPhraseListener;

    public interface OnNextPhraseListener {
        void onNextPhrase(int speakIndex, int phraseIndex, String phrase);
    }

    public interface OnIndexSpeakFinished {
        void onNextPhrase(int speakIndex);
    }

    public TTSPlayer(Work work, Context context) {
        this.work = work;
        this.context = context;
    }

    public void setNextPhraseListener(OnNextPhraseListener nextPhraseListener) {
        this.nextPhraseListener = nextPhraseListener;
    }

    public void setIndexSpeakFinished(OnIndexSpeakFinished indexSpeakFinished) {
        this.indexSpeakFinished = indexSpeakFinished;
    }

    public void startSpeak(int speakIndex) {
        if (tts == null) return;
        if (tts.isSpeaking() || speakIndex >= work.getRootElements().size()) {
            tts.stop();
        }
        phrases = new LinkedList<>(Arrays.asList(work.getRootElements().get(speakIndex).text().split("[.!?]")));
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
        phraseIndex = 0;
        this.speakIndex = speakIndex;
        nextPhrase();
        state = STATE.SPEAKING;
    }

    public boolean isSpeaking() {
        return STATE.SPEAKING == state || tts.isSpeaking();
    }


    public void resume() {
        if(phrases != null) {
            nextPhrase();
        } else {
            startSpeak(speakIndex);
        }
        state = STATE.SPEAKING;
    }

    public void pause() {
        tts.stop();
        state = STATE.PAUSE;
    }

    public void stop() {
        speakIndex = 0;
        phraseIndex = 0;
        phrases = null;
        tts.stop();
        state = STATE.STOPPED;
    }

    public STATE getState() {
        return state;
    }

    private void nextPhrase() {
        if (phraseIndex >= phrases.size()) {
            if(indexSpeakFinished != null) {
                GuiUtils.runInUI(context, new GuiUtils.RunUIThread() {
                    @Override
                    public void run() {
                      indexSpeakFinished.onNextPhrase(speakIndex);
                    }
                });
            }
            startSpeak(speakIndex + 1);
            return;
        }
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "stringId");
        String phrase = phrases.get(phraseIndex);
        if (nextPhraseListener != null) {
            GuiUtils.runInUI(context, new GuiUtils.RunUIThread() {
                @Override
                public void run() {
                    nextPhraseListener.onNextPhrase(speakIndex, phraseIndex, phrase);
                }
            });
        }
        tts.speak(phrase, TextToSpeech.QUEUE_FLUSH, params);
        phraseIndex++;
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setOnUtteranceCompletedListener(utteranceId -> {
                if(state == STATE.SPEAKING) {
                    nextPhrase();
                }
            });
            tts.setLanguage(Locale.getDefault());
            tts.setSpeechRate(1.3f);
        } else {
            tts = null;
            Toast.makeText(context, "Failed to initialize TTS engine.", Toast.LENGTH_SHORT).show();
        }
    }

    public void onStart() {
        tts = new TextToSpeech(context, this);
    }

    public void onStop() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }


}
