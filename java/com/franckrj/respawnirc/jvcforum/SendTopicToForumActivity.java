package com.franckrj.respawnirc.jvcforum;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.franckrj.respawnirc.R;
import com.franckrj.respawnirc.dialogs.InsertStuffDialogFragment;
import com.franckrj.respawnirc.utils.JVCParser;
import com.franckrj.respawnirc.utils.PrefsManager;
import com.franckrj.respawnirc.utils.Utils;
import com.franckrj.respawnirc.utils.WebManager;

public class SendTopicToForumActivity extends AppCompatActivity implements InsertStuffDialogFragment.StuffInserted {
    public static final String EXTRA_FORUM_NAME = "com.franckrj.respawnirc.sendtopicactivity.EXTRA_FORUM_NAME";
    public static final String EXTRA_FORUM_LINK = "com.franckrj.respawnirc.sendtopicactivity.EXTRA_FORUM_LINK";
    public static final String EXTRA_INPUT_LIST = "com.franckrj.respawnirc.sendtopicactivity.EXTRA_INPUT_LIST";

    private SendTopicToJVC currentAsyncTaskForSendTopic = null;
    private String linkToSend = "";
    private String listOfInputsInAstring = "";
    private EditText topicTitleEdit = null;
    private EditText topicContentEdit = null;
    private String lastTopicTitleSended = "";
    private String lastTopicContentSended = "";

    private void sendNewTopic() {
        if (currentAsyncTaskForSendTopic == null) {
            lastTopicTitleSended = topicTitleEdit.getText().toString();
            lastTopicContentSended = topicContentEdit.getText().toString();

            currentAsyncTaskForSendTopic = new SendTopicToJVC();
            currentAsyncTaskForSendTopic.execute(linkToSend, Utils.convertStringToUrlString(lastTopicTitleSended), Utils.convertStringToUrlString(lastTopicContentSended), listOfInputsInAstring, PrefsManager.getString(PrefsManager.StringPref.Names.COOKIES_LIST));

            PrefsManager.putString(PrefsManager.StringPref.Names.LAST_TOPIC_TITLE_SENDED, lastTopicTitleSended);
            PrefsManager.putString(PrefsManager.StringPref.Names.LAST_TOPIC_CONTENT_SENDED, lastTopicContentSended);
            PrefsManager.applyChanges();
        }
    }

    private void stopAllCurrentTasks() {
        if (currentAsyncTaskForSendTopic != null) {
            currentAsyncTaskForSendTopic.cancel(true);
            currentAsyncTaskForSendTopic = null;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sendtopic);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar_sendtopic);
        setSupportActionBar(myToolbar);

        ActionBar myActionBar = getSupportActionBar();
        if (myActionBar != null) {
            myActionBar.setHomeButtonEnabled(true);
            myActionBar.setDisplayHomeAsUpEnabled(true);
        }

        topicTitleEdit = (EditText) findViewById(R.id.topic_title_edit_sendtopic);
        topicContentEdit = (EditText) findViewById(R.id.topic_content_edit_sendtopic);

        if (getIntent() != null) {
            if (getIntent().getStringExtra(EXTRA_FORUM_NAME) != null && myActionBar != null) {
                myActionBar.setSubtitle(getIntent().getStringExtra(EXTRA_FORUM_NAME));
            }
            if (getIntent().getStringExtra(EXTRA_FORUM_LINK) != null) {
                linkToSend = getIntent().getStringExtra(EXTRA_FORUM_LINK);
            }
            if (getIntent().getStringExtra(EXTRA_INPUT_LIST) != null) {
                listOfInputsInAstring = getIntent().getStringExtra(EXTRA_INPUT_LIST);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        lastTopicTitleSended = PrefsManager.getString(PrefsManager.StringPref.Names.LAST_TOPIC_TITLE_SENDED);
        lastTopicContentSended = PrefsManager.getString(PrefsManager.StringPref.Names.LAST_TOPIC_CONTENT_SENDED);
    }

    @Override
    public void onPause() {
        stopAllCurrentTasks();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_sendtopic, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.action_past_last_topic_sended_sendtopic).setEnabled(!lastTopicTitleSended.isEmpty() || !lastTopicContentSended.isEmpty());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_send_topic_sendtopic:
                sendNewTopic();
                return true;
            case R.id.action_past_last_topic_sended_sendtopic:
                topicTitleEdit.setText(lastTopicTitleSended);
                topicContentEdit.setText(lastTopicContentSended);
                return true;
            case R.id.action_select_sticker_sendtopic:
                InsertStuffDialogFragment insertStuffDialogFragment = new InsertStuffDialogFragment();
                insertStuffDialogFragment.show(getFragmentManager(), "InsertStuffDialogFragment");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void getStringInserted(String newStringToAdd, int posOfCenterFromEnd) {
        int currentCursorPos = topicContentEdit.getSelectionStart();
        if (currentCursorPos == -1) {
            currentCursorPos = 0;
            topicContentEdit.append(newStringToAdd);
        } else {
            topicContentEdit.getText().insert(currentCursorPos, newStringToAdd);
        }
        topicContentEdit.setSelection(currentCursorPos + newStringToAdd.length() - posOfCenterFromEnd);
    }

    private class SendTopicToJVC extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            if (params.length > 4) {
                WebManager.WebInfos currentWebInfos = new WebManager.WebInfos();
                int numberOfTrys = 0;
                String pageContent;
                currentWebInfos.followRedirects = false;

                do {
                    ++numberOfTrys;
                    pageContent = WebManager.sendRequest(params[0], "POST", "titre_topic=" + params[1] + "&message_topic=" + params[2] + params[3], params[4], currentWebInfos);
                } while (params[0].equals(currentWebInfos.currentUrl) && numberOfTrys < 2);

                if (params[0].equals(currentWebInfos.currentUrl)) {
                    pageContent = "respawnirc:resendneeded";
                }

                return pageContent;
            } else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(String pageResult) {
            super.onPostExecute(pageResult);

            currentAsyncTaskForSendTopic = null;

            if (!Utils.stringIsEmptyOrNull(pageResult)) {
                if (pageResult.equals("respawnirc:resendneeded")) {
                    Toast.makeText(SendTopicToForumActivity.this, R.string.unknownErrorPleaseResend, Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    Toast.makeText(SendTopicToForumActivity.this, JVCParser.getErrorMessage(pageResult), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(SendTopicToForumActivity.this, R.string.topicIsSended, Toast.LENGTH_SHORT).show();
            }

            onBackPressed();
        }
    }
}
