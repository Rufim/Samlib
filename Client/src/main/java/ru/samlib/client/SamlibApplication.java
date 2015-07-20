package ru.samlib.client;

import android.app.Application;
import android.content.res.Configuration;
import com.facebook.drawee.backends.pipeline.Fresco;

import com.nd.android.sdp.im.common.widget.htmlview.view.HtmlView;
import ru.samlib.client.domain.entity.Link;
import ru.samlib.client.domain.Constants;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

/**
 * Created by Rufim on 03.07.2015.
 */
public class SamlibApplication extends Application {

    private static SamlibApplication singleton;

    public static SamlibApplication getInstance() {
        return singleton;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;
        Link.setBaseDomain(Constants.Net.BASE_DOMAIN);
        Fresco.initialize(this);
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath(Constants.Assets.ROBOTO_FONT_PATH)
                .setFontAttrId(R.attr.fontPath)
                .build());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

}
