package ru.samlib.client.fragments;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import net.nightwhistler.htmlspanner.HtmlSpanner;
import ru.kazantsev.template.fragments.BaseFragment;
import ru.samlib.client.R;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.util.LinkHandler;

/**
 * Created by 0shad on 04.06.2017.
 */
public class HelpFragment extends BaseFragment {


    @BindView(R.id.work_text_indent)
    TextView textView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getBaseActivity().setTitle(R.string.drawer_help);
        getBaseActivity().getNavigationView().setCheckedItem(R.id.drawer_help);
        View root = inflater.inflate(R.layout.item_indent, container, false);
        bind(root);
        HtmlSpanner spanner =  new HtmlSpanner(Constants.Net.BASE_DOMAIN, R.drawable.ic_image_crop_original);
        spanner.setTextView(textView);
        spanner.registerHandler("a", new LinkHandler());
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setText(spanner.fromHtml(getString(R.string.help)));
        return root;
    }


}
