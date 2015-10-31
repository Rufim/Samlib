package ru.samlib.client.fragments;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v4.app.FragmentManager;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import de.greenrobot.event.EventBus;
import net.nightwhistler.htmlspanner.HtmlSpanner;
import ru.samlib.client.R;
import ru.samlib.client.adapter.ItemListAdapter;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.Comment;
import ru.samlib.client.domain.entity.Work;
import ru.samlib.client.domain.events.WorkParsedEvent;
import ru.samlib.client.parser.CommentsParser;
import ru.samlib.client.parser.IllustrationsParser;
import ru.samlib.client.util.GuiUtils;
import ru.samlib.client.util.LinkHandler;
import ru.samlib.client.util.URLSpanNoUnderline;

import java.net.MalformedURLException;

/**
 * Created by 0shad on 31.10.2015.
 */
public class CommentsFragment extends ListFragment<Comment> {

    private static final String TAG = CommentsFragment.class.getSimpleName();

    private Work work;

    public static void show(FragmentManager manager, @IdRes int container, String link) {
        show(manager, container, CommentsFragment.class, Constants.ArgsName.LINK, link);
    }

    public static void show(BaseFragment fragment, String link) {
        show(fragment, CommentsFragment.class, Constants.ArgsName.LINK, link);
    }

    public static void show(BaseFragment fragment, Work work) {
        show(fragment, CommentsFragment.class, Constants.ArgsName.WORK, work);
    }

    @Override
    protected ItemListAdapter<Comment> getAdapter() {
        return new CommentsAdapter(R.layout.item_comment);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        boolean newWork = false;
        String link = getArguments().getString(Constants.ArgsName.LINK);
        Work incomingWork = (Work) getArguments().getSerializable(Constants.ArgsName.WORK);
        if (incomingWork != null && !incomingWork.equals(work)) {
            work = incomingWork;
            clearData();
            newWork = true;
        } else if (link != null) {
            if (work == null || !work.getLink().equals(link)) {
                work = new Work(link);
                clearData();
                newWork = true;
            }
        }
        if (newWork) {
            try {
                setDataSource(new CommentsParser(work, false));
            } catch (MalformedURLException e) {
                Log.e(TAG, "Unknown exception", e);
                ErrorFragment.show(CommentsFragment.this, R.string.error);
            }
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    protected class CommentsAdapter extends ItemListAdapter<Comment> {

        public CommentsAdapter(int layoutId) {
            super(layoutId);
            bindClicks = false;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            TextView number = holder.getView(R.id.comment_number);
            TextView author = holder.getView(R.id.comment_author);
            TextView email = holder.getView(R.id.comment_email);
            TextView content = holder.getView(R.id.comment_content);
            TextView data = holder.getView(R.id.comment_data);
            Comment comment = items.get(position);
            number.setText(comment.getNumber().toString());
            if(!comment.isDeleted()) {
                data.setText(comment.getFormattedData());
            } else {
                data.setText(comment.getRawContent() + " " + comment.getFormattedData());
                GuiUtils.setVisibility(View.GONE, author, content, email);
                return;
            }
            GuiUtils.setTextOrHide(email, comment.getEmail());
            HtmlSpanner spanner = new HtmlSpanner();
            spanner.registerHandler("a", new LinkHandler(content));
            content.setText(spanner.fromHtml(comment.getRawContent()));
            content.setMovementMethod(LinkMovementMethod.getInstance());
            if(comment.getAuthor() != null && comment.getAuthor().getLink() != null) {
                author.setText(GuiUtils.spannableText(comment.getNickName(), new URLSpanNoUnderline(comment.getAuthor().getFullLink())));
                author.setMovementMethod(LinkMovementMethod.getInstance());
            } else if(comment.isUserComment()){
                author.setText(GuiUtils.coloredText(getActivity(), comment.getNickName(), R.color.red_light));
            } else {
                GuiUtils.setTextOrHide(author, comment.getNickName());
            }
        }
    }
}
