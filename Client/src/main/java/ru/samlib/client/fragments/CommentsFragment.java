package ru.samlib.client.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.text.method.LinkMovementMethod;
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
import ru.samlib.client.domain.events.CommentsParsedEvent;
import ru.samlib.client.domain.events.ScrollToCommentEvent;
import ru.samlib.client.lister.DataSource;
import ru.samlib.client.parser.CommentsParser;
import ru.samlib.client.util.FragmentBuilder;
import ru.samlib.client.util.GuiUtils;
import ru.samlib.client.util.LinkHandler;
import ru.samlib.client.util.URLSpanNoUnderline;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by 0shad on 31.10.2015.
 */
public class CommentsFragment extends ListFragment<Comment> {

    private static final String TAG = CommentsFragment.class.getSimpleName();

    private static final int FIRST_PAGE = 10;
    private static final int PAGE = 40;

    private Work work;
    private DataSource<Comment> datasource;
    private CommentsParser parser;
    private int showPage = -1;
    private boolean isParseSend = false;
    HashMap<Integer, Integer> pagesSize = new HashMap<>();

    public static void show(FragmentBuilder builder, @IdRes int container, String link) {
        show(builder, container, CommentsFragment.class, Constants.ArgsName.LINK, link);
    }

    public static void show(BaseFragment fragment, String link) {
        show(fragment, CommentsFragment.class, Constants.ArgsName.LINK, link);
    }

    public static void show(BaseFragment fragment, Work work) {
        show(fragment, CommentsFragment.class, Constants.ArgsName.WORK, work);
    }


    @Override
    public boolean allowBackPress() {
        if (getFragmentManager().getBackStackEntryCount() == 0) {
            AuthorFragment.show(new FragmentBuilder(getFragmentManager()), getId(), work.getAuthor());
            return false;
        } else {
            return super.allowBackPress();
        }
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
            newWork = true;
        } else if (link != null) {
            if (work == null || !work.getLink().equals(link)) {
                work = new Work(link);
                newWork = true;
            }
        }
        pageSize = 10;
        if (newWork) {
            clearData();
            try {
                parser = new CommentsParser(work, false);
                setDataSource((skip, size) -> {
                    int pageIndex = pagesSize.size();
                    List<Comment> comments = new ArrayList<>();
                    while (showPage > pageIndex || getPagesSize() < skip + size) {
                        List<Comment> commentsAdd = parser.getPage(pageIndex);
                        pagesSize.put(pageIndex, commentsAdd.size());
                        comments.addAll(commentsAdd);
                        pageIndex++;
                    }
                    if (!isParseSend) {
                        postEvent(new CommentsParsedEvent(parser.getLastPage()));
                        isParseSend = true;
                    }
                    return comments;
                });
            } catch (MalformedURLException e) {
                Log.e(TAG, "Unknown exception", e);
                ErrorFragment.show(CommentsFragment.this, R.string.error);
            }
        } else {
            if (!isParseSend) {
                postEvent(new CommentsParsedEvent(parser.getLastPage()));
                isParseSend = true;
            }
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    public int getPagesSize() {
        return getPagesSize(pagesSize.size());
    }

    public int getPagesSize(int toPage) {
        int sizeCounter = 0;
        for (int i = 0; i < pagesSize.size() && i < toPage; i++) {
            sizeCounter += pagesSize.get(i);
        }
        return sizeCounter;
    }

    @Override
    protected void clearData() {
        super.clearData();
        pagesSize.clear();
        isParseSend = false;
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    public void onEvent(ScrollToCommentEvent event) {
        if (event.index > 0) {
            scrollToIndex(getItemIndex(event.index));
        } else {
            showPage = event.pageIndex;
            if(showPage <= pagesSize.size()) {
                showPage();
            } else {
                loadItems(0, true, new AsyncTask() {
                    @Override
                    protected Object doInBackground(Object[] params) {
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Object o) {
                        showPage();
                    }
                });
            }
        }
    }


    public void scrollToIndex(int index, int textOffset) {
        if (adapter.getItemCount() > index) {
            toIndex(index, textOffset);
        } else {
            moveToIndex = new MoveTask() {

                @Override
                protected void onPostExecute(Void empty) {
                    if (this == moveToIndex) {
                        toIndex(index - countLostComments(index), offsetLines);
                    }
                }
            };
            loadItems(index + pageSize, true, moveToIndex, index, textOffset);
        }
    }

    public void showPage() {
        scrollToIndex(getPagesSize(showPage));
        showPage = -1;
    }

    public int getItemIndex(int index) {
        int first = adapter.getItems().get(0).getNumber();
        return first - index;
    }

    public int countLostComments(int toIndex) {
        int lost = 0;
        for (int i = 0; i + 1 < adapter.getItems().size() && i + 1 < toIndex; i++) {
            int dif = adapter.getItems().get(i).getNumber() - adapter.getItems().get(i + 1).getNumber() - 1;
            if(dif > 0) {
                lost += dif;
            }
        }
        return lost;
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
            if (!comment.isDeleted()) {
                data.setText(comment.getFormattedData());
            } else {
                data.setText(comment.getRawContent() + " " + comment.getFormattedData());
                GuiUtils.setVisibility(View.GONE, author, content, email);
                return;
            }
            GuiUtils.setTextOrHide(email, comment.getEmail());
            HtmlSpanner spanner = new HtmlSpanner();
            spanner.registerHandler("a", new LinkHandler(content));
            GuiUtils.setTextOrHide(content, spanner.fromHtml(comment.getRawContent()));
            content.setMovementMethod(LinkMovementMethod.getInstance());
            if (comment.getAuthor() != null && comment.getAuthor().getLink() != null) {
                GuiUtils.setTextOrHide(author, GuiUtils.spannableText(comment.getNickName(),
                        new URLSpanNoUnderline(comment.getAuthor().getFullLink())));
                author.setMovementMethod(LinkMovementMethod.getInstance());
            } else if (comment.isUserComment()) {
                GuiUtils.setTextOrHide(author, GuiUtils.coloredText(getActivity(), comment.getNickName(), R.color.red_light));
            } else {
                GuiUtils.setTextOrHide(author, comment.getNickName());
            }
        }
    }
}
