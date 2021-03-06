package com.franckrj.respawnirc;

import android.os.Bundle;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.franckrj.respawnirc.base.AbsHomeIsBackActivity;
import com.franckrj.respawnirc.base.AbsWebRequestAsyncTask;
import com.franckrj.respawnirc.utils.AccountManager;
import com.franckrj.respawnirc.utils.JVCParser;
import com.franckrj.respawnirc.utils.Utils;
import com.franckrj.respawnirc.utils.WebManager;

public class ConnectAsModoActivity extends AbsHomeIsBackActivity {
    private static final String SAVE_LIST_OF_INPUT = "saveListOfInput";

    private ConnectAsModoTask currentTaskConnectAsModo = null;
    private SwipeRefreshLayout swipeRefresh = null;
    private EditText modoPasswordText = null;
    private Button validateButton = null;
    private String latestListOfInputInAString = "";
    private String currentCookieList = "";

    private final View.OnClickListener validateButtonClickedListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            performConnect();
        }
    };

    private final TextView.OnEditorActionListener actionInPasswordEditTextListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                performConnect();
                return true;
            }
            return false;
        }
    };

    private final AbsWebRequestAsyncTask.RequestIsStarted connectAsModoIsStartedListener = new AbsWebRequestAsyncTask.RequestIsStarted() {
        @Override
        public void onRequestIsStarted() {
            swipeRefresh.setRefreshing(true);
        }
    };

    private final AbsWebRequestAsyncTask.RequestIsFinished<String> connectAsModoIsFinishedListener = new AbsWebRequestAsyncTask.RequestIsFinished<String>() {
        @Override
        public void onRequestIsFinished(String reqResult) {
            boolean isNotARealConnection = currentTaskConnectAsModo.getIsNotARealConnection();

            swipeRefresh.setRefreshing(false);
            currentTaskConnectAsModo = null;

            if (reqResult != null) {
                if (isNotARealConnection) {
                    latestListOfInputInAString = JVCParser.getListOfInputInAStringInModoConnectFormForThisPage(reqResult);
                    if (!latestListOfInputInAString.isEmpty()) {
                        modoPasswordText.setVisibility(View.VISIBLE);
                        validateButton.setVisibility(View.VISIBLE);
                        modoPasswordText.requestFocus();
                        Utils.showSoftKeyboard(ConnectAsModoActivity.this);

                        return;
                    } else if (JVCParser.getErrorMessageWhenModoConnect(reqResult).isEmpty()) {
                        AccountManager.setCurrentAccountIsModo(true);
                        Toast.makeText(ConnectAsModoActivity.this, R.string.youAreAlreadyConnectedAsModo, Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                } else {
                    String errorWhenConnecting = JVCParser.getErrorMessageWhenModoConnect(reqResult);
                    latestListOfInputInAString = JVCParser.getListOfInputInAStringInModoConnectFormForThisPage(reqResult);

                    if (errorWhenConnecting.isEmpty()) {
                        AccountManager.setCurrentAccountIsModo(true);
                        Toast.makeText(ConnectAsModoActivity.this, R.string.connectionSuccessful, Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(ConnectAsModoActivity.this, errorWhenConnecting, Toast.LENGTH_SHORT).show();
                    }

                    return;
                }
            }

            Toast.makeText(ConnectAsModoActivity.this, R.string.errorDownloadFailed, Toast.LENGTH_SHORT).show();
        }
    };

    private void performConnect() {
        Utils.hideSoftKeyboard(this);

        if (currentTaskConnectAsModo == null) {
            currentTaskConnectAsModo = new ConnectAsModoTask(modoPasswordText.getText().toString(), latestListOfInputInAString);
            currentTaskConnectAsModo.setRequestIsStartedListener(connectAsModoIsStartedListener);
            currentTaskConnectAsModo.setRequestIsFinishedListener(connectAsModoIsFinishedListener);
            currentTaskConnectAsModo.execute(currentCookieList);
        } else {
            Toast.makeText(ConnectAsModoActivity.this, R.string.errorActionAlreadyRunning, Toast.LENGTH_SHORT).show();
        }
    }

    private void stopAllCurrentTasks() {
        if (currentTaskConnectAsModo != null) {
            currentTaskConnectAsModo.clearListenersAndCancel();
            currentTaskConnectAsModo = null;
        }
        swipeRefresh.setRefreshing(false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modoconnect);
        initToolbar(R.id.toolbar_modoconnect);

        swipeRefresh = findViewById(R.id.swiperefresh_modoconnect);
        modoPasswordText = findViewById(R.id.password_text_modoconnect);
        validateButton = findViewById(R.id.validate_button_modoconnect);

        swipeRefresh.setEnabled(false);
        swipeRefresh.setColorSchemeResources(R.color.colorControlHighlightThemeLight);
        modoPasswordText.setOnEditorActionListener(actionInPasswordEditTextListener);
        validateButton.setOnClickListener(validateButtonClickedListener);

        currentCookieList = AccountManager.getCurrentAccount().cookie;

        if (savedInstanceState != null) {
            String tmpListOfInputInAString = savedInstanceState.getString(SAVE_LIST_OF_INPUT, null);

            if (!Utils.stringIsEmptyOrNull(tmpListOfInputInAString)) {
                latestListOfInputInAString = tmpListOfInputInAString;
                return;
            }
        }

        modoPasswordText.setVisibility(View.GONE);
        validateButton.setVisibility(View.GONE);
        currentTaskConnectAsModo = new ConnectAsModoTask(null);
        currentTaskConnectAsModo.setRequestIsStartedListener(connectAsModoIsStartedListener);
        currentTaskConnectAsModo.setRequestIsFinishedListener(connectAsModoIsFinishedListener);
        currentTaskConnectAsModo.execute(currentCookieList);
    }

    @Override
    public void onPause() {
        stopAllCurrentTasks();
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SAVE_LIST_OF_INPUT, latestListOfInputInAString);
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, getString(R.string.warningNotConnected), Toast.LENGTH_LONG).show();
        super.onBackPressed();
    }

    private static class ConnectAsModoTask extends AbsWebRequestAsyncTask<String, Void, String> {
        private String passwordToUse;
        private String listOfInputInStringToUse = "";

        public ConnectAsModoTask(String newPasswordToUse) {
            passwordToUse = newPasswordToUse;
        }

        public ConnectAsModoTask(String newPasswordToUse, String newListOfInputInStringToUse) {
            passwordToUse = newPasswordToUse;
            listOfInputInStringToUse = newListOfInputInStringToUse;
        }

        public boolean getIsNotARealConnection() {
            return passwordToUse == null;
        }

        @Override
        protected String doInBackground(String... params) {
            if (params.length > 0) {
                WebManager.WebInfos currentWebInfos = initWebInfos(params[0], false);

                if (passwordToUse == null) {
                    return WebManager.sendRequest("https://www.jeuxvideo.com/sso/auth.php", "GET", "", currentWebInfos);
                } else {
                    return WebManager.sendRequest("https://www.jeuxvideo.com/sso/auth.php", "POST", "password=" +
                            Utils.encodeStringToUrlString(passwordToUse) + listOfInputInStringToUse, currentWebInfos);
                }
            }
            return null;
        }
    }
}
