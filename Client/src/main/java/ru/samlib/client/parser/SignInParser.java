package ru.samlib.client.parser;

import android.content.Context;

import net.vrallev.android.cat.Cat;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

import ru.kazantsev.template.domain.Valuable;
import ru.kazantsev.template.net.HTTPExecutor;
import ru.kazantsev.template.net.Request;
import ru.kazantsev.template.net.Response;
import ru.kazantsev.template.util.PreferenceMaster;
import ru.kazantsev.template.util.TextUtils;
import ru.samlib.client.R;
import ru.samlib.client.domain.Constants;

import static ru.samlib.client.domain.Constants.Net.BASE_DOMAIN;


public class SignInParser extends Parser {
    public static final String LOGIN_PATH = BASE_DOMAIN + "/cgi-bin/login";

    private String localLoginCookie;

    public enum LoginParams implements Valuable {
        OPERATION("login"), BACK("http://samlib.ru/cgi-bin/login"), DATA0(""), DATA1("");

        private final String defaultValue;

        LoginParams(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        @Override
        public Object value() {
            return defaultValue;
        }
    }

    public SignInParser() {
    }

    public SignInParser(String localLoginCookie) {
        this.localLoginCookie = localLoginCookie;
    }


    public static String login(String login, String password) {
        try {
            Request request = new Request(LOGIN_PATH)
                    .setMethod(Request.Method.POST)
                    .addHeader("Accept", ACCEPT_VALUE)
                    .addHeader("User-Agent", USER_AGENT)
                    .addHeader("Host", Constants.Net.BASE_HOST)
                    .addHeader("Referer", LOGIN_PATH)
                    .addHeader("Upgrade-Insecure-Requests", "1")
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .initParams(LoginParams.values())
                    .addParam(LoginParams.DATA0, login)
                    .addParam(LoginParams.DATA1, password);
            Response response = new HTTPExecutor(request).execute();
            if (response.getCode() == 200) {
                Map<String, List<String>> headers = response.getHeaders();
                List<String> setCookies = headers.get("Set-Cookie");
                StringBuilder cookie = new StringBuilder();
                if (setCookies != null && setCookies.size() > 0) {
                    for (String set : setCookies) {
                        String val = set.substring(0, set.indexOf(";"));
                        if(TextUtils.notEmpty(val)) {
                            cookie.append(val);
                            cookie.append("; ");
                        }
                    }
                    return cookie.substring(0, cookie.length() - 2);
                }
            }
        } catch (Exception e) {
            Cat.e(e);
        }
        return "";
    }

    public void enterLogin(Context context, String cookie) {
        if (TextUtils.notEmpty(cookie)) {
            loginCookie = cookie;
            localLoginCookie = cookie;
            PreferenceMaster master = new PreferenceMaster(context);
            master.putValue(R.string.preferenceLoginCookie, loginCookie);
        }
    }

    public void eraseLogin(Context context) {
        loginCookie = null;
        localLoginCookie = null;
        PreferenceMaster master = new PreferenceMaster(context);
        master.putValue(R.string.preferenceLoginCookie, "");
    }

    private String getLoginCookieValue(String name) {
        String value = "";
        if (TextUtils.notEmpty(localLoginCookie)) {
            value = HTTPExecutor.parseParamFromHeader(localLoginCookie, name);
        }
        if (TextUtils.notEmpty(getLoginCookie())) {
            value = HTTPExecutor.parseParamFromHeader(getLoginCookie(), name);
        }
        if (TextUtils.notEmpty(value)) {
            try {
                value = URLDecoder.decode(value, "CP1251");
            } catch (Exception e) {
                Cat.e(e);
            }
        }
        return value;
    }

    public String getUsername() {
        String zui = getLoginCookieValue("ZUI");
        if (TextUtils.notEmpty(zui) && zui.indexOf("&") > 0) {
            return zui.substring(0, zui.indexOf("&"));
        }
        return "";
    }

    public String getEmail() {
        String zui = getLoginCookieValue("ZUI");
        String[] vals = zui.split("&");
        if (TextUtils.notEmpty(zui) && vals.length > 1) {
            return vals[1];
        }
        return "";
    }

    public String getSectionPath() {
        String home = getLoginCookieValue("HOME");
        if (TextUtils.notEmpty(home)) {
            return BASE_DOMAIN + "/" + home;
        } else {
            return "";
        }
    }

    public String getLogin() {
        String login = getLoginCookieValue("NAME");
        if (TextUtils.notEmpty(login)) {
            return login;
        } else {
            return "";
        }
    }
}
