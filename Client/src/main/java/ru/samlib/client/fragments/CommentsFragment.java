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
import net.nightwhistler.htmlspanner.HtmlSpanner;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import ru.kazantsev.template.fragments.BaseFragment;
import ru.kazantsev.template.fragments.ListFragment;
import ru.kazantsev.template.lister.DataSource;
import ru.kazantsev.template.net.Response;
import ru.samlib.client.R;
import ru.kazantsev.template.adapter.ItemListAdapter;
import ru.samlib.client.dialog.NewCommentDialog;
import ru.samlib.client.domain.Constants;
import ru.samlib.client.domain.entity.Comment;
import ru.samlib.client.domain.entity.Work;
import ru.kazantsev.template.util.FragmentBuilder;
import ru.kazantsev.template.util.GuiUtils;
import ru.samlib.client.parser.CommentsParser;
import ru.samlib.client.util.LinkHandler;
import ru.kazantsev.template.util.URLSpanNoUnderline;

import java.io.IOException;
import java.util.List;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Created by 0shad on 31.10.2015.
 */
public class CommentsFragment extends ListFragment<Comment> {

    private static final String TAG = CommentsFragment.class.getSimpleName();

    private Work work;
    private int showPage = -1;
    private Document document;

    public static CommentsFragment show(FragmentBuilder builder, @IdRes int container, String link) {
        return show(builder.putArg(Constants.ArgsName.LINK, link), container, CommentsFragment.class);
    }

    public static CommentsFragment show(BaseFragment fragment, String link) {
        return show(fragment, CommentsFragment.class, Constants.ArgsName.LINK, link);
    }

    public CommentsFragment show(BaseFragment fragment, Work work) {
        return show(fragment, CommentsFragment.class, Constants.ArgsName.WORK, work);
    }

    public CommentsFragment() {
        retainInstance = false;
    }

    @Override
    protected ItemListAdapter<Comment> newAdapter() {
        return new CommentsAdapter(R.layout.item_comment);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        boolean newWork = false;
        Integer page = getArguments().getInt(Constants.ArgsName.COMMENTS_PAGE);
        Work incomingWork = (Work) getArguments().getSerializable(Constants.ArgsName.WORK);
        if (incomingWork != null && !incomingWork.equals(work)) {
            work = incomingWork;
            newWork = true;
        }
        pageSize = 10;
        if (newWork && showPage != page) {
            showPage = page;
            clearData();
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected DataSource<Comment> newDataSource() throws Exception {
        return (skip, size) -> {
            isEnd = true;
            if (document != null) {
                List<Comment> comments = ((CommentsPagerFragment) getParentFragment()).parser.parseDocument(document);
                document = null;
                return comments;
            } else {
                return ((CommentsPagerFragment) getParentFragment()).parser.getPage(showPage);
            }
        };
    }

    public void refreshWithDocument(Document document) {
        this.document = document;
        refreshData(false);
    }

    protected class CommentsAdapter extends ItemListAdapter<Comment> {

        public CommentsAdapter(int layoutId) {
            super(layoutId);
            bindClicks = false;
        }


        @Override
        public void onClick(View view, int position) {
            final CommentsParser.Operation operation;
            switch (view.getId()) {
                case R.id.comment_reply:
                    operation = CommentsParser.Operation.reply;
                    break;
                case R.id.comment_edit:
                    operation = CommentsParser.Operation.edit;
                    break;
                case R.id.comment_restore:
                case R.id.comment_delete:
                    operation = CommentsParser.Operation.delete;
                    break;
                default: operation = null;
            }
            final Comment comment = getItems().get(position);
            if(operation != null) {
                ((CommentsPagerFragment) getParentFragment()).startLoading(true);
                new AsyncTask<Void, Void, String[]>() {

                    @Override
                    protected String[] doInBackground(Void... params) {
                        String[] answer = new String[2];
                        Response response = CommentsParser.getAnswerForComment(comment, operation);
                        if (response != null && response.getCode() == 200) {
                            answer[0] = "OK";
                            try {
                                answer[1] = response.getRawContent();
                            } catch (IOException e) {
                                answer[0] = "ERROR";
                                answer[1] = getString(R.string.error);
                                Log.e(TAG, "Unknown exception", e);
                            }
                        } else {
                            answer[0] = "ERROR";
                            answer[1] = getString(R.string.error);
                        }
                        return answer;
                    }

                    @Override
                    protected void onPostExecute(String[] answer) {
                        if ("OK".equals(answer[0])) {
                            if (operation.equals(CommentsParser.Operation.delete)) {
                                refreshWithDocument(Jsoup.parse(answer[1]));
                            } else {
                                NewCommentDialog dialog = (NewCommentDialog) getFragmentManager().findFragmentByTag(NewCommentDialog.class.getSimpleName());
                                if (dialog == null) {
                                    dialog = new NewCommentDialog();
                                }
                                dialog.setIndexPage(showPage);
                                dialog.setExisting(comment);
                                if(operation.equals(CommentsParser.Operation.reply))
                                dialog.setOperation(CommentsParser.Operation.store_reply);
                                else if(operation.equals(CommentsParser.Operation.edit))
                                dialog.setOperation(CommentsParser.Operation.store_edit);
                                dialog.setText(getTextareaText(answer[1]));
                                dialog.show(getFragmentManager(), NewCommentDialog.class.getSimpleName());
                            }
                        } else {
                            GuiUtils.toast(getContext(), answer[0]);
                        }
                        ((CommentsPagerFragment) getParentFragment()).stopLoading();
                    }
                }.execute();
            }
        }

        private String getTextareaText(String body) {
            return Jsoup.parse(body).select("textarea[name=TEXT]").text();
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            TextView number = holder.getView(R.id.comment_number);
            TextView author = holder.getView(R.id.comment_author);
            TextView email = holder.getView(R.id.comment_email);
            TextView content = holder.getView(R.id.comment_content);
            TextView data = holder.getView(R.id.comment_data);
            TextView reply = holder.getView(R.id.comment_reply);
            TextView edit = holder.getView(R.id.comment_edit);
            TextView delete = holder.getView(R.id.comment_delete);
            TextView restore = holder.getView(R.id.comment_restore);
            reply.setOnClickListener(this);
            reply.setTag(holder);
            edit.setOnClickListener(this);
            edit.setTag(holder);
            delete.setOnClickListener(this);
            delete.setTag(holder);
            restore.setOnClickListener(this);
            delete.setTag(holder);
            Comment comment = items.get(position);
            number.setText(comment.getNumber().toString());
            if (!comment.isDeleted()) {
                data.setText(comment.getFormattedData());
                restore.setVisibility(GONE);
            } else {
                data.setText(comment.getRawContent() + " " + comment.getFormattedData());
                if(comment.isCanBeRestored()) {
                    restore.setVisibility(VISIBLE);
                }
                GuiUtils.setVisibility(GONE, author, content, email, reply, edit, delete);
                return;
            }
            GuiUtils.setTextOrHide(email, comment.getEmail());
            HtmlSpanner spanner = new HtmlSpanner();
            spanner.registerHandler("a", new LinkHandler(content));
            GuiUtils.setTextOrHide(content, spanner.fromHtml(comment.getRawContent()));
            content.setMovementMethod(LinkMovementMethod.getInstance());
            if (comment.getLink() != null) {
                GuiUtils.setTextOrHide(author, GuiUtils.spannableText(comment.getNickName(),
                        new URLSpanNoUnderline(comment.getLink().isAuthor() ? comment.getLink().getFullLink() : comment.getLink().getLink())));
                author.setMovementMethod(LinkMovementMethod.getInstance());
            } else if (comment.isUserComment()) {
                GuiUtils.setTextOrHide(author, GuiUtils.coloredText(getActivity(), comment.getNickName(), R.color.red_light));
            } else {
                GuiUtils.setTextOrHide(author, comment.getNickName());
            }
            if (comment.getMsgid() == null) {
                GuiUtils.setVisibility(GONE, reply, edit, delete);
            } else {
                reply.setVisibility(VISIBLE);
                if (!comment.isCanBeEdited()) {
                    edit.setVisibility(GONE);
                } else {
                    edit.setVisibility(VISIBLE);
                }
                if (!comment.isCanBeDeleted()) {
                    delete.setVisibility(GONE);
                } else {
                    edit.setVisibility(VISIBLE);
                }
            }
        }
    }
}
