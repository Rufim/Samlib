package ru.samlib.client.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.*;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Created with IntelliJ IDEA.
 * User: Dmitry
 * Date: 26.02.13
 * Time: 9:44
 * To change this template use File | Settings | File Templates.
 */
public class DirectoryChooserDialog extends AlertDialog {

    private static final String TAG = DirectoryChooserDialog.class.getSimpleName();

    public OnChooseFileListener onChooseFileListener;
    public OnDirectoryChangedListener onDirectoryChangedListener;

    private String[] fileTypes;
    private String[] mFileList;
    private File currentDir = Environment.getExternalStorageDirectory().getAbsoluteFile();
    private ListView filesListView;
    private EditText pathTextView;
    private LinearLayout contentView;
    private ArrayList<File> createdNewDirectories = new ArrayList<File>();
    private boolean allowRootDir = false;

    public interface OnChooseFileListener {
        void onChosenFile(File chosenFile);
    }

    public interface OnDirectoryChangedListener {
        void onDirectoryChanged(File chosenFile);
    }

    public void setFileTypes(String[] fileTypes) {
        this.fileTypes = fileTypes;
    }

    public void setPositiveButton(CharSequence positiveButtonText, OnClickListener listener) {
        setButton(AlertDialog.BUTTON_POSITIVE, positiveButtonText, listener);
    }

    public void setNegativeButton(CharSequence negativeButtonText, OnClickListener listener) {
        setButton(AlertDialog.BUTTON_NEGATIVE, negativeButtonText, listener);
    }

    public void setNeutralButton(CharSequence neutralButtonText, OnClickListener listener) {
        setButton(AlertDialog.BUTTON_NEUTRAL, neutralButtonText, listener);
    }


    public DirectoryChooserDialog(Context context, String sourceDirectory, boolean withPathTextView) {
        this(context, withPathTextView);
        setSourceDirectory(sourceDirectory);
    }

    public DirectoryChooserDialog(final Context context, boolean withPathTextView) {
        super(context);
        setTitle("Выберите папку");
        setButton(AlertDialog.BUTTON_POSITIVE, context.getResources().getString(android.R.string.ok), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (onChooseFileListener != null) {
                    onChooseFileListener.onChosenFile(currentDir);
                }
            }
        });
        contentView = new LinearLayout(getContext());

        contentView.setOrientation(LinearLayout.VERTICAL);
        contentView.setPadding(0, 10, 0, 0);
        filesListView = new ListView(getContext());
        filesListView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0.1f));
        if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD) {
            filesListView.setBackgroundColor(Color.WHITE);
            contentView.setBackgroundColor(Color.WHITE);
            filesListView.setCacheColorHint(Color.WHITE);
        }
        if(withPathTextView) {
            View divider = new View(getContext());
            divider.setBackgroundColor(Color.DKGRAY);
            divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 1));
            pathTextView = new EditText(context) ;
            pathTextView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            pathTextView.setBackgroundDrawable(null);
            pathTextView.setPadding(5, 5, 5, 7);
            pathTextView.setGravity(Gravity.LEFT);
            pathTextView.setSingleLine();
            pathTextView.setImeOptions(EditorInfo.IME_ACTION_DONE);
            onDirectoryChangedListener = new OnDirectoryChangedListener() {
                @Override
                public void onDirectoryChanged(File chosenFile) {
                    String newPath = chosenFile.getAbsolutePath();
                    pathTextView.setText(newPath);
                    pathTextView.setSelection(newPath.length());

                }
            };
            pathTextView.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    checkPath(s.toString());
                }
            });
            contentView.addView(pathTextView);
            contentView.addView(divider);
        }
        contentView.addView(filesListView);
        setNeutralButton("Создать папку", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
    }

    public void setOnChooseFileListener(OnChooseFileListener onChooseFileListener) {
        this.onChooseFileListener = onChooseFileListener;
    }

    public void setOnDirectoryChangedListener(OnDirectoryChangedListener onDirectoryChangedListener) {
        this.onDirectoryChangedListener = onDirectoryChangedListener;
    }

    public void setSourceDirectory(String sourceDirectory) {
        setSourceDirectory(new File(sourceDirectory));
    }

    public void setSourceDirectory(File sourceDirectory) {
        if (sourceDirectory != null && sourceDirectory.exists() && sourceDirectory.isDirectory()) {
            try {
                currentDir = sourceDirectory.getCanonicalFile();
            } catch (IOException e) {
                Log.e(TAG, "Error when getting canonical file", e);
                currentDir = sourceDirectory;
            }
        }
    }

    public void setAllowRootDir(boolean allowRootDir) {
        this.allowRootDir = allowRootDir;
    }

    @Override
    public ListView getListView() {
        return filesListView;
    }

    public EditText getPathTextView() {
        return pathTextView;
    }

    public String getPath() {
        return pathTextView.getText().toString();
    }

    public File getCurrentDir() {
        return currentDir;
    }

    @Override
    public void show() {
        filesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (i == 0 && currentDir.getParent() != null) {
                    setSourceDirectory(new File(currentDir.getParentFile().getAbsolutePath()));
                } else {
                    setSourceDirectory(new File(currentDir.getAbsolutePath(), mFileList[i]));
                }
                changePath();
                checkPath(currentDir.getAbsolutePath());
            }
        });
        changePath();
        super.setView(contentView, 0, 0, 0, 0);
        super.show();
        checkPath(currentDir.getAbsolutePath());
        Button neutralButton = getButton(BUTTON_NEUTRAL);
        if(pathTextView != null) {
            pathTextView.setText(currentDir.getAbsolutePath());
        }
        if(neutralButton != null) {
            neutralButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Context context = DirectoryChooserDialog.this.getContext();
                    Builder createNewDirectory = new Builder(context);
                    createNewDirectory.setTitle("Создать папку");
                    final EditText editText = new EditText(context);
                    editText.setSingleLine();
                    createNewDirectory.setView(editText);
                    createNewDirectory.setPositiveButton(context.getResources().getString(android.R.string.ok), new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            File newDir = new File(currentDir.getAbsolutePath(), editText.getText().toString());
                            if (newDir.mkdir()) {
                                Log.i(TAG, "Created directory " + newDir);
                            } else {
                                Log.w(TAG, "Failed to create directory " + newDir);
                            }
                            createdNewDirectories.add(newDir);
                            changePath();
                        }
                    });
                    createNewDirectory.setNegativeButton(context.getResources().getString(android.R.string.cancel), new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    createNewDirectory.show();
                }
            });
        }
    }

    public ArrayList<File> getCreatedNewDirectories() {
        return createdNewDirectories;
    }

    public void setCreatedNewDirectories(ArrayList<File> createdNewDirectories) {
        this.createdNewDirectories = createdNewDirectories;
    }

    private void checkPath(String path) {
        path = path.replaceAll("//+", "/");
        if(getButton(AlertDialog.BUTTON_POSITIVE) != null && !allowRootDir) {
            if (path.contains(Environment.getExternalStorageDirectory().getAbsolutePath() + "/") &&
                    !path.equals(Environment.getExternalStorageDirectory().getAbsolutePath() + "/")) {
                getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            } else {
                getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            }
        }
    }

    private void changePath() {
        loadFileList();
        ListAdapter listAdapter = new ArrayAdapter<CharSequence>(getContext(), android.R.layout.simple_list_item_1, android.R.id.text1, mFileList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView text;
                if (convertView == null) {
                    text = (TextView) ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(android.R.layout.simple_list_item_1, parent, false);
                } else {
                    text = (TextView) convertView;
                }
                text.setText(getItem(position));
                return text;
            }
        };
        filesListView.setAdapter(listAdapter);
        filesListView.invalidateViews();
        if (onDirectoryChangedListener != null) {
            onDirectoryChangedListener.onDirectoryChanged(currentDir);
        }
    }

    @Deprecated
    @Override
    public void setView(View view) {
    }

    @Deprecated
    @Override
    public void setView(View view, int viewSpacingLeft, int viewSpacingTop, int viewSpacingRight, int viewSpacingBottom) {
    }

    private void loadFileList() {
        try {
            currentDir.mkdirs();
        } catch (SecurityException e) {
            Log.e(TAG, "unable to write on the sd card " + e.toString());
        }
        if (currentDir.exists()) {
            FilenameFilter filter = new FilenameFilter() {
                public boolean accept(File dir, String filename) {
                    File sel = new File(dir, filename);
                    boolean isNotHidden = !sel.isHidden();
                    if (!sel.isDirectory()) {
                        if (fileTypes != null) {
                            if (fileTypes.length == 0) {
                                return isNotHidden;
                            }
                            int index = filename.lastIndexOf(".");
                            if (index != -1) {
                                return isNotHidden && Arrays.asList(fileTypes).contains(filename.substring(index + 1));
                            }
                        }
                        return false;
                    }
                    return isNotHidden;
                }
            };
            String[] fileList = currentDir.list(filter);
            if(fileList == null) {
                fileList = new String[0];
            }
            Arrays.sort(fileList, new Comparator<String>() {
                @Override
                public int compare(String lf, String rf) {
                    lf = lf.toLowerCase();
                    rf = rf.toLowerCase();
                    if (lf.contains(".")) {
                        if (rf.contains(".")) {
                            return lf.compareTo(rf);
                        } else {
                            return -1;
                        }
                    } else if (rf.contains(".")) {
                        return 1;
                    }
                    return lf.compareTo(rf);
                }
            });

            if (currentDir.getParent() != null) {
                mFileList = new String[fileList.length + 1];
                mFileList[0] = "\u21A9";
                System.arraycopy(fileList, 0, mFileList, 1, fileList.length);
            } else {
                mFileList = fileList;
            }
        } else {
            mFileList = new String[0];
        }
    }
}
