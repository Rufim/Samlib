package ru.samlib.client.util;

import android.content.Context;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.widget.Toast;
import net.nightwhistler.htmlspanner.HtmlSpanner;
import ru.kazantsev.template.util.GuiUtils;
import ru.kazantsev.template.util.SystemUtils;
import ru.kazantsev.template.util.TextUtils;
import ru.samlib.client.domain.entity.Work;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private static AtomicBoolean ready = new AtomicBoolean(false);

    private static Map<String, String> available;

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
        if(clearIndent.length() <= offset) {
            offset = 0;
        }
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

    public synchronized void startSpeak(int index, int phrase, int offset) {
        if (tts == null || state.equals(State.UNAVAILABLE) || state.equals(State.END)) return;
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
        changeState(State.SPEAKING);
        nextPhrase();
    }

    public boolean isSpeaking() {
        return tts.isSpeaking() || State.SPEAKING == state;
    }

    public  void resume() {
        changeState(State.IDLE);
        if (phrases != null) {
            nextPhrase();
        } else {
            startSpeak(indentIndex);
        }
    }

    public void start() {
        startSpeak(0);
    }

    public synchronized void pause() {
        tts.stop();
        changeState(State.PAUSE);
    }

    public synchronized void stop() {
        tts.stop();
        indentIndex = 0;
        phraseIndex = 0;
        phrases = null;
        changeState(State.STOPPED);
    }

    public synchronized  void next() {
        int current = indentIndex;
        changeState(State.STOPPED);
        tts.stop();
        while (tts.isSpeaking()){
            SystemUtils.sleepQuietly(100);
        }
        startSpeak(current + 1);
    }

    public synchronized  void pre() {
        int current = indentIndex;
        changeState(State.STOPPED);
        tts.stop();
        while (tts.isSpeaking()) {
            SystemUtils.sleepQuietly(100);
        }
        startSpeak(current - 1);
    }

    public State getState() {
        return state;
    }

    public boolean notReady() {
     return tts == null || state.equals(State.UNAVAILABLE) || state.equals(State.END) || state.equals(State.STOPPED) || state.equals(State.PAUSE);
    }

    private synchronized void nextPhrase() {
        if (notReady()) return;
        if (phraseIndex >= phrases.size()) {
            if (indexSpeakFinished != null) {
                GuiUtils.runInUI(context, new GuiUtils.RunUIThread() {
                    @Override
                    public void run(Object... var) {
                        if (notReady()) return;
                        if (indexSpeakFinished != null) {
                            indexSpeakFinished.onNextPhrase((int) var[0]);
                        }
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
                    if (notReady()) return;
                    if(nextPhraseListener != null) {
                        nextPhraseListener.onNextPhrase((int) var[0], (int) var[1], (List<Phrase>) var[2]);
                    }
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
    public synchronized  void onInit(int status) {
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
                tts.setLanguage(language.length() == 2 ? new Locale(language, language) : new Locale(language));
            }
            changeState(State.IDLE);
            if (playOnStart) {
                playOnStart = false;
                startSpeak(indentIndex, offset);
            }
            ready.set(true);
        } else {
            tts = null;
            Toast.makeText(context, "Failed to initialize TTS engine.", Toast.LENGTH_SHORT).show();
        }
    }

    public synchronized  void reset() {
        onStop();
        onStart();
    }

    public void onStart() {
        tts = new TextToSpeech(context, this);
    }

    public synchronized  void play(Work work, int index, int offset) {
        if(tts != null) {
            tts.stop();
            tts.shutdown();
        }
        playOnStart = true;
        this.work = work;
        indentIndex = index;
        this.offset = offset;
        onStart();
    }

    public synchronized  void onStop() {
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



    public static Map<String, String> getAvailableLanguages(Context context) {
        if (available != null) return available;
        TextToSpeech myTTS = new TextToSpeech(context, status -> {
            ready.set(true);
        });
        while (!ready.get()) {
            SystemUtils.sleepQuietly(100);
        }
        available = new LinkedHashMap<>();
        for (Locale each : Locale.getAvailableLocales()) {
            if (TextToSpeech.LANG_AVAILABLE == myTTS.isLanguageAvailable(each)) {
                available.put(getLanguageName(each), each.toString());
            }
        }
        new Thread(() -> myTTS.shutdown());
        return available;
    }

    public static void dropAvailableLanguages() {
        available = null;
    }

    public static String getLanguageName(Locale locale) {
        String name = resolveLanguage(locale).getDisplayName();
        if (TextUtils.isEmpty(name) && countryCodeMap.containsKey(locale.toString())) {
            name = countryCodeMap.get(locale.toString());
        }
        return name;
    }

    private static Locale resolveLanguage(Locale locale) {
        return locale.toString().length() == 2 ? new Locale(locale.toString(), locale.toString()) : locale;
    }

    private static Map<String, String> countryCodeMap = new LinkedHashMap<>();

    static {
        countryCodeMap.put("af", "Afghanistan");
        countryCodeMap.put("ax", "Åland Islands");
        countryCodeMap.put("al", "Albania");
        countryCodeMap.put("dz", "Algeria");
        countryCodeMap.put("as", "American Samoa");
        countryCodeMap.put("ad", "Andorra");
        countryCodeMap.put("ao", "Angola");
        countryCodeMap.put("ai", "Anguilla");
        countryCodeMap.put("aq", "Antarctica");
        countryCodeMap.put("ag", "Antigua and Barbuda");
        countryCodeMap.put("ar", "Argentina");
        countryCodeMap.put("am", "Armenia");
        countryCodeMap.put("aw", "Aruba");
        countryCodeMap.put("au", "Australia");
        countryCodeMap.put("at", "Austria");
        countryCodeMap.put("az", "Azerbaijan");
        countryCodeMap.put("bs", "Bahamas");
        countryCodeMap.put("bh", "Bahrain");
        countryCodeMap.put("bd", "Bangladesh");
        countryCodeMap.put("bb", "Barbados");
        countryCodeMap.put("by", "Belarus");
        countryCodeMap.put("be", "Belgium");
        countryCodeMap.put("bz", "Belize");
        countryCodeMap.put("bj", "Benin");
        countryCodeMap.put("bm", "Bermuda");
        countryCodeMap.put("bt", "Bhutan");
        countryCodeMap.put("bo", "Bolivia");
        countryCodeMap.put("bq", "Bonaire, Sint Eustatius and Saba");
        countryCodeMap.put("ba", "Bosnia and Herzegovina");
        countryCodeMap.put("bw", "Botswana");
        countryCodeMap.put("bv", "Bouvet Island");
        countryCodeMap.put("br", "Brazil");
        countryCodeMap.put("io", "British Indian Ocean Territory");
        countryCodeMap.put("vg", "British Virgin Islands");
        countryCodeMap.put("bn", "Brunei");
        countryCodeMap.put("bg", "Bulgaria");
        countryCodeMap.put("bf", "Burkina Faso");
        countryCodeMap.put("bi", "Burundi");
        countryCodeMap.put("kh", "Cambodia");
        countryCodeMap.put("cm", "Cameroon");
        countryCodeMap.put("ca", "Canada");
        countryCodeMap.put("cv", "Cape Verde");
        countryCodeMap.put("ky", "Cayman Islands");
        countryCodeMap.put("cf", "Central African Republic");
        countryCodeMap.put("td", "Chad");
        countryCodeMap.put("cl", "Chile");
        countryCodeMap.put("cn", "China");
        countryCodeMap.put("cx", "Christmas Island");
        countryCodeMap.put("cc", "Cocos Islands");
        countryCodeMap.put("co", "Colombia");
        countryCodeMap.put("km", "Comoros");
        countryCodeMap.put("cg", "Congo");
        countryCodeMap.put("ck", "Cook Islands");
        countryCodeMap.put("cr", "Costa Rica");
        countryCodeMap.put("ci", "Côte d’Ivoire");
        countryCodeMap.put("hr", "Croatia");
        countryCodeMap.put("cu", "Cuba");
        countryCodeMap.put("cw", "Curaçao");
        countryCodeMap.put("cy", "Cyprus");
        countryCodeMap.put("cz", "Czech Republic");
        countryCodeMap.put("dk", "Denmark");
        countryCodeMap.put("dj", "Djibouti");
        countryCodeMap.put("dm", "Dominica");
        countryCodeMap.put("do", "Dominican Republic");
        countryCodeMap.put("ec", "Ecuador");
        countryCodeMap.put("eg", "Egypt");
        countryCodeMap.put("sv", "El Salvador");
        countryCodeMap.put("gq", "Equatorial Guinea");
        countryCodeMap.put("er", "Eritrea");
        countryCodeMap.put("ee", "Estonia");
        countryCodeMap.put("et", "Ethiopia");
        countryCodeMap.put("fk", "Falkland Islands");
        countryCodeMap.put("fo", "Faroe Islands");
        countryCodeMap.put("fj", "Fiji");
        countryCodeMap.put("fi", "Finland");
        countryCodeMap.put("fr", "France");
        countryCodeMap.put("gf", "French Guiana");
        countryCodeMap.put("pf", "French Polynesia");
        countryCodeMap.put("tf", "French Southern Territories");
        countryCodeMap.put("ga", "Gabon");
        countryCodeMap.put("gm", "Gambia");
        countryCodeMap.put("ge", "Georgia");
        countryCodeMap.put("de", "Germany");
        countryCodeMap.put("gh", "Ghana");
        countryCodeMap.put("gi", "Gibraltar");
        countryCodeMap.put("gr", "Greece");
        countryCodeMap.put("gl", "Greenland");
        countryCodeMap.put("gd", "Grenada");
        countryCodeMap.put("gp", "Guadeloupe");
        countryCodeMap.put("gu", "Guam");
        countryCodeMap.put("gt", "Guatemala");
        countryCodeMap.put("gg", "Guernsey");
        countryCodeMap.put("gn", "Guinea");
        countryCodeMap.put("gw", "Guinea - Bissau");
        countryCodeMap.put("gy", "Guyana");
        countryCodeMap.put("ht", "Haiti");
        countryCodeMap.put("hm", "Heard Island And McDonald Islands");
        countryCodeMap.put("hn", "Honduras");
        countryCodeMap.put("hk", "Hong Kong");
        countryCodeMap.put("hu", "Hungary");
        countryCodeMap.put("is", "Iceland");
        countryCodeMap.put("in", "India");
        countryCodeMap.put("id", "Indonesia");
        countryCodeMap.put("ir", "Iran");
        countryCodeMap.put("iq", "Iraq");
        countryCodeMap.put("ie", "Ireland");
        countryCodeMap.put("im", "Isle Of Man");
        countryCodeMap.put("il", "Israel");
        countryCodeMap.put("it", "Italy");
        countryCodeMap.put("jm", "Jamaica");
        countryCodeMap.put("jp", "Japan");
        countryCodeMap.put("je", "Jersey");
        countryCodeMap.put("jo", "Jordan");
        countryCodeMap.put("kz", "Kazakhstan");
        countryCodeMap.put("ke", "Kenya");
        countryCodeMap.put("ki", "Kiribati");
        countryCodeMap.put("kw", "Kuwait");
        countryCodeMap.put("kg", "Kyrgyzstan");
        countryCodeMap.put("la", "Laos");
        countryCodeMap.put("lv", "Latvia");
        countryCodeMap.put("lb", "Lebanon");
        countryCodeMap.put("ls", "Lesotho");
        countryCodeMap.put("lr", "Liberia");
        countryCodeMap.put("ly", "Libya");
        countryCodeMap.put("li", "Liechtenstein");
        countryCodeMap.put("lt", "Lithuania");
        countryCodeMap.put("lu", "Luxembourg");
        countryCodeMap.put("mo", "Macao");
        countryCodeMap.put("mk", "Macedonia");
        countryCodeMap.put("mg", "Madagascar");
        countryCodeMap.put("mw", "Malawi");
        countryCodeMap.put("my", "Malaysia");
        countryCodeMap.put("mv", "Maldives");
        countryCodeMap.put("ml", "Mali");
        countryCodeMap.put("mt", "Malta");
        countryCodeMap.put("mh", "Marshall Islands");
        countryCodeMap.put("mq", "Martinique");
        countryCodeMap.put("mr", "Mauritania");
        countryCodeMap.put("mu", "Mauritius");
        countryCodeMap.put("yt", "Mayotte");
        countryCodeMap.put("mx", "Mexico");
        countryCodeMap.put("fm", "Micronesia");
        countryCodeMap.put("md", "Moldova");
        countryCodeMap.put("mc", "Monaco");
        countryCodeMap.put("mn", "Mongolia");
        countryCodeMap.put("me", "Montenegro");
        countryCodeMap.put("ms", "Montserrat");
        countryCodeMap.put("ma", "Morocco");
        countryCodeMap.put("mz", "Mozambique");
        countryCodeMap.put("mm", "Myanmar");
        countryCodeMap.put("na", "Namibia");
        countryCodeMap.put("nr", "Nauru");
        countryCodeMap.put("np", "Nepal");
        countryCodeMap.put("nl", "Netherlands");
        countryCodeMap.put("an", "Netherlands Antilles");
        countryCodeMap.put("nc", "New Caledonia");
        countryCodeMap.put("nz", "New Zealand");
        countryCodeMap.put("ni", "Nicaragua");
        countryCodeMap.put("ne", "Niger");
        countryCodeMap.put("ng", "Nigeria");
        countryCodeMap.put("nu", "Niue");
        countryCodeMap.put("nf", "Norfolk Island");
        countryCodeMap.put("mp", "Northern Mariana Islands");
        countryCodeMap.put("kp", "North Korea");
        countryCodeMap.put("no", "Norway");
        countryCodeMap.put("om", "Oman");
        countryCodeMap.put("pk", "Pakistan");
        countryCodeMap.put("pw", "Palau");
        countryCodeMap.put("ps", "Palestine");
        countryCodeMap.put("pa", "Panama");
        countryCodeMap.put("pg", "Papua New Guinea");
        countryCodeMap.put("py", "Paraguay");
        countryCodeMap.put("pe", "Peru");
        countryCodeMap.put("ph", "Philippines");
        countryCodeMap.put("pn", "Pitcairn");
        countryCodeMap.put("pl", "Poland");
        countryCodeMap.put("pt", "Portugal");
        countryCodeMap.put("pr", "Puerto Rico");
        countryCodeMap.put("qa", "Qatar");
        countryCodeMap.put("re", "Reunion");
        countryCodeMap.put("ro", "Romania");
        countryCodeMap.put("ru", "Russia");
        countryCodeMap.put("rw", "Rwanda");
        countryCodeMap.put("bl", "Saint Barthélemy");
        countryCodeMap.put("sh", "Saint Helena");
        countryCodeMap.put("kn", "Saint Kitts And Nevis");
        countryCodeMap.put("lc", "Saint Lucia");
        countryCodeMap.put("mf", "Saint Martin");
        countryCodeMap.put("pm", "Saint Pierre And Miquelon");
        countryCodeMap.put("vc", "Saint Vincent And The Grenadines");
        countryCodeMap.put("ws", "Samoa");
        countryCodeMap.put("sm", "San Marino");
        countryCodeMap.put("st", "Sao Tome And Principe");
        countryCodeMap.put("sa", "Saudi Arabia");
        countryCodeMap.put("sn", "Senegal");
        countryCodeMap.put("rs", "Serbia");
        countryCodeMap.put("sc", "Seychelles");
        countryCodeMap.put("sl", "Sierra Leone");
        countryCodeMap.put("sg", "Singapore");
        countryCodeMap.put("sx", "Sint Maarten");
        countryCodeMap.put("sk", "Slovakia");
        countryCodeMap.put("si", "Slovenia");
        countryCodeMap.put("sb", "Solomon Islands");
        countryCodeMap.put("so", "Somalia");
        countryCodeMap.put("za", "South Africa");
        countryCodeMap.put("gs", "South Georgia And The South Sandwich Islands");
        countryCodeMap.put("kr", "South Korea");
        countryCodeMap.put("ss", "South Sudan");
        countryCodeMap.put("es", "Spain");
        countryCodeMap.put("lk", "Sri Lanka");
        countryCodeMap.put("sd", "Sudan");
        countryCodeMap.put("sr", "Suriname");
        countryCodeMap.put("sj", "Svalbard And Jan Mayen");
        countryCodeMap.put("sz", "Swaziland");
        countryCodeMap.put("se", "Sweden");
        countryCodeMap.put("ch", "Switzerland");
        countryCodeMap.put("sy", "Syria");
        countryCodeMap.put("tw", "Taiwan");
        countryCodeMap.put("tj", "Tajikistan");
        countryCodeMap.put("tz", "Tanzania");
        countryCodeMap.put("th", "Thailand");
        countryCodeMap.put("cd", "The Democratic Republic Of Congo");
        countryCodeMap.put("tl", "Timor - Leste");
        countryCodeMap.put("tg", "Togo");
        countryCodeMap.put("tk", "Tokelau");
        countryCodeMap.put("to", "Tonga");
        countryCodeMap.put("tt", "Trinidad and Tobago");
        countryCodeMap.put("tn", "Tunisia");
        countryCodeMap.put("tr", "Turkey");
        countryCodeMap.put("tm", "Turkmenistan");
        countryCodeMap.put("tc", "Turks And Caicos Islands");
        countryCodeMap.put("tv", "Tuvalu");
        countryCodeMap.put("vi", "U.S.Virgin Islands");
        countryCodeMap.put("ug", "Uganda");
        countryCodeMap.put("ua", "Ukraine");
        countryCodeMap.put("ae", "United Arab Emirates");
        countryCodeMap.put("gb", "United Kingdom");
        countryCodeMap.put("us", "United States ");
        countryCodeMap.put("um", "United States Minor Outlying Islands");
        countryCodeMap.put("uy", "Uruguay");
        countryCodeMap.put("uz", "Uzbekistan");
        countryCodeMap.put("vu", "Vanuatu");
        countryCodeMap.put("va", "Vatican");
        countryCodeMap.put("ve", "Venezuela");
        countryCodeMap.put("vn", "Vietnam");
        countryCodeMap.put("wf", "Wallis And Futuna");
        countryCodeMap.put("eh", "Western Sahara");
        countryCodeMap.put("ye", "Yemen");
        countryCodeMap.put("zm", "Zambia");
        countryCodeMap.put("zw", "Zimbabwe");
    }

}
