package com.franckrj.respawnirc.jvctopic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.franckrj.respawnirc.MainActivity;
import com.franckrj.respawnirc.R;
import com.franckrj.respawnirc.dialogs.ChoosePageNumberDialogFragment;
import com.franckrj.respawnirc.dialogs.LinkContextMenuDialogFragment;
import com.franckrj.respawnirc.dialogs.SelectStickerDialogFragment;
import com.franckrj.respawnirc.dialogs.ShowImageDialogFragment;
import com.franckrj.respawnirc.jvctopic.jvctopicgetters.AbsJVCTopicGetter;
import com.franckrj.respawnirc.jvctopic.jvctopicgetters.JVCTopicModeForumGetter;
import com.franckrj.respawnirc.jvctopic.jvctopicviewers.AbsShowTopicFragment;
import com.franckrj.respawnirc.jvctopic.jvctopicviewers.JVCTopicAdapter;
import com.franckrj.respawnirc.jvctopic.jvctopicviewers.ShowTopicModeForumFragment;
import com.franckrj.respawnirc.jvctopic.jvctopicviewers.ShowTopicModeIRCFragment;
import com.franckrj.respawnirc.AbsShowSomethingFragment;
import com.franckrj.respawnirc.PageNavigationUtil;
import com.franckrj.respawnirc.jvcforum.ShowForumActivity;
import com.franckrj.respawnirc.utils.AddOrRemoveThingToFavs;
import com.franckrj.respawnirc.utils.JVCParser;
import com.franckrj.respawnirc.utils.Undeprecator;
import com.franckrj.respawnirc.utils.Utils;

public class ShowTopicActivity extends AppCompatActivity implements AbsShowTopicFragment.NewModeNeededListener, AbsJVCTopicGetter.NewForumAndTopicNameAvailable,
                                                                    PopupMenu.OnMenuItemClickListener, JVCTopicModeForumGetter.NewNumbersOfPagesListener,
                                                                    ChoosePageNumberDialogFragment.NewPageNumberSelected, JVCTopicAdapter.URLClicked,
                                                                    AbsJVCTopicGetter.NewReasonForTopicLock, SelectStickerDialogFragment.StickerSelected,
        PageNavigationUtil.PageNavigationFunctions, AddOrRemoveThingToFavs.ActionToFavsEnded,
                                                                    AbsShowTopicFragment.NewSurveyNeedToBeShown {
    public static final String EXTRA_TOPIC_LINK = "com.franckrj.respawnirc.EXTRA_TOPIC_LINK";
    public static final String EXTRA_TOPIC_NAME = "com.franckrj.respawnirc.EXTRA_TOPIC_NAME";
    public static final String EXTRA_FORUM_NAME = "com.franckrj.respawnirc.EXTRA_FORUM_NAME";
    public static final String EXTRA_GO_TO_BOTTOM = "com.franckrj.respawnirc.EXTRA_GO_TO_BOTTOM";

    private static final String SAVE_CURRENT_FORUM_TITLE_FOR_TOPIC = "saveCurrentForumTitleForTopic";
    private static final String SAVE_CURRENT_TOPIC_TITLE_FOR_TOPIC = "saveCurrentTopicTitleForTopic";
    private static final String SAVE_LAST_PAGE = "saveLastPage";
    private static final String SAVE_REASON_OF_LOCK = "saveReasonOfLock";

    private SharedPreferences sharedPref = null;
    private JVCParser.ForumAndTopicName currentTitles = new JVCParser.ForumAndTopicName();
    private JVCMessageToTopicSender senderForMessages = null;
    private JVCMessageInTopicAction actionsForMessages = null;
    private ImageButton messageSendButton = null;
    private EditText messageSendEdit = null;
    private View messageSendLayout = null;
    private String pseudoOfUser = "";
    private String cookieListInAString = "";
    private String lastMessageSended = "";
    private AddOrRemoveThingToFavs currentTaskForFavs = null;
    private String reasonOfLock = null;
    private ImageButton selectStickerButton = null;
    private PageNavigationUtil pageNavigation = null;
    private boolean useInternalNavigatorForDefaultOpening = false;
    private boolean convertNoelshackLinkToDirectLink = false;
    private boolean showOverviewOnImageClick = false;
    private boolean goToBottomAtPageLoading = false;

    private final JVCMessageToTopicSender.NewMessageWantEditListener listenerForNewMessageWantEdit = new JVCMessageToTopicSender.NewMessageWantEditListener() {
        @Override
        public void initializeEditMode(String newMessageToEdit) {
            if (reasonOfLock == null) {
                messageSendButton.setEnabled(true);

                if (newMessageToEdit.isEmpty()) {
                    messageSendButton.setImageResource(R.drawable.ic_action_content_send);
                    Toast.makeText(ShowTopicActivity.this, R.string.errorCantGetEditInfos, Toast.LENGTH_SHORT).show();
                } else {
                    messageSendEdit.setText(newMessageToEdit);
                    messageSendEdit.setSelection(newMessageToEdit.length());
                }
            }
        }
    };

    private final JVCMessageToTopicSender.NewMessagePostedListener listenerForNewMessagePosted = new JVCMessageToTopicSender.NewMessagePostedListener() {
        @Override
        public void lastMessageIsSended(String withThisError) {
            if (reasonOfLock == null) {
                messageSendButton.setEnabled(true);
                messageSendButton.setImageResource(R.drawable.ic_action_content_send);

                if (withThisError != null) {
                    Toast.makeText(ShowTopicActivity.this, withThisError, Toast.LENGTH_LONG).show();
                } else {
                    messageSendEdit.setText("");
                }

                getCurrentFragment().reloadTopic();
            }
        }
    };

    private final Button.OnClickListener sendMessageToTopicListener = new View.OnClickListener() {
        @Override
        public void onClick(View buttonView) {
            if (messageSendButton.isEnabled() && reasonOfLock == null) {
                String tmpLastMessageSended = "";

                if (!pseudoOfUser.isEmpty()) {
                    if (!senderForMessages.getIsInEdit()) {
                        boolean messageIsSended = false;
                        if (getCurrentFragment().getLatestListOfInputInAString() != null) {
                            messageSendButton.setEnabled(false);
                            tmpLastMessageSended = messageSendEdit.getText().toString();
                            messageIsSended = senderForMessages.sendThisMessage(tmpLastMessageSended, getCurrentFragment().getCurrentUrlOfTopic(), getCurrentFragment().getLatestListOfInputInAString(), cookieListInAString, false);
                        }

                        if (!messageIsSended) {
                            Toast.makeText(ShowTopicActivity.this, R.string.errorInfosMissings, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        messageSendButton.setEnabled(false);
                        tmpLastMessageSended = messageSendEdit.getText().toString();
                        senderForMessages.sendEditMessage(tmpLastMessageSended, cookieListInAString);
                    }
                } else {
                    Toast.makeText(ShowTopicActivity.this, R.string.errorConnectedNeededBeforePost, Toast.LENGTH_LONG).show();
                }

                Utils.hideSoftKeyboard(ShowTopicActivity.this);
                messageSendLayout.requestFocus();

                if (!tmpLastMessageSended.isEmpty()) {
                    SharedPreferences.Editor sharedPrefEdit = sharedPref.edit();
                    lastMessageSended = tmpLastMessageSended;
                    sharedPrefEdit.putString(getString(R.string.prefLastMessageSended), lastMessageSended);
                    sharedPrefEdit.apply();
                }
            } else {
                Toast.makeText(ShowTopicActivity.this, R.string.errorMessageAlreadySending, Toast.LENGTH_SHORT).show();
            }
        }
    };

    private final Button.OnLongClickListener refreshFromSendButton = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            getCurrentFragment().reloadTopic();
            return true;
        }
    };

    private final Button.OnClickListener selectStickerClickedListener = new View.OnClickListener() {
        @Override
        public void onClick(View buttonView) {
            SelectStickerDialogFragment selectStickerDialogFragment = new SelectStickerDialogFragment();
            selectStickerDialogFragment.show(getFragmentManager(), "SelectStickerDialogFragment");
        }
    };

    private final JVCMessageInTopicAction.NewMessageIsQuoted messageIsQuotedListener = new JVCMessageInTopicAction.NewMessageIsQuoted() {
        @Override
        public void getNewMessageQuoted(String messageQuoted) {
            if (reasonOfLock == null) {
                String currentMessage = messageSendEdit.getText().toString();

                if (!currentMessage.isEmpty() && !currentMessage.endsWith("\n\n")) {
                    if (!currentMessage.endsWith("\n")) {
                        currentMessage += "\n";
                    }
                    currentMessage += "\n";
                }
                currentMessage += messageQuoted;

                messageSendEdit.setText(currentMessage);
                messageSendEdit.setSelection(currentMessage.length());
            }
        }
    };

    public ShowTopicActivity() {
        pageNavigation = new PageNavigationUtil(this);
    }

    private void stopAllCurrentTask() {
        if (currentTaskForFavs != null) {
            currentTaskForFavs.cancel(true);
            currentTaskForFavs = null;
        }
        actionsForMessages.stopAllCurrentTasks();
        senderForMessages.stopAllCurrentTask();
    }

    private void reloadSettings() {
        pseudoOfUser = sharedPref.getString(getString(R.string.prefPseudoUser), "");
        cookieListInAString = sharedPref.getString(getString(R.string.prefCookiesList), "");
        lastMessageSended = sharedPref.getString(getString(R.string.prefLastMessageSended), "");
    }

    private void updateShowNavigationButtons() {
        int currentTopicMode = sharedPref.getInt(getString(R.string.prefCurrentTopicMode), AbsShowTopicFragment.MODE_FORUM);

        if (currentTopicMode == AbsShowTopicFragment.MODE_FORUM) {
            pageNavigation.setShowNavigationButtons(ShowTopicModeForumFragment.getShowNavigationButtons());
        } else {
            pageNavigation.setShowNavigationButtons(ShowTopicModeIRCFragment.getShowNavigationButtons());
        }
    }

    private void updateLastPageAndCurrentItemAndButtonsToCurrentLink() {
        if (!pageNavigation.getCurrentLink().isEmpty()) {
            pageNavigation.setLastPageNumber(getShowablePageNumberForThisLink(pageNavigation.getCurrentLink()));
            pageNavigation.notifyDataSetChanged();
            pageNavigation.updateCurrentItemAndButtonsToCurrentLink();
        }
    }

    private AbsShowTopicFragment getCurrentFragment() {
        return (AbsShowTopicFragment) pageNavigation.getCurrentFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_showtopic);

        Drawable arrowDrawable = Undeprecator.resourcesGetDrawable(getResources(), R.drawable.ic_action_navigation_arrow_drop_down);
        arrowDrawable.setBounds(0, 0, arrowDrawable.getIntrinsicWidth() / 2, arrowDrawable.getIntrinsicHeight() / 2);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar_showtopic);
        setSupportActionBar(myToolbar);

        ActionBar myActionBar = getSupportActionBar();
        if (myActionBar != null) {
            myActionBar.setHomeButtonEnabled(true);
            myActionBar.setDisplayHomeAsUpEnabled(true);
        }

        sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        messageSendLayout = findViewById(R.id.sendmessage_layout_showtopic);
        messageSendEdit = (EditText) findViewById(R.id.sendmessage_text_showtopic);
        messageSendButton = (ImageButton) findViewById(R.id.sendmessage_button_showtopic);
        selectStickerButton = (ImageButton) findViewById(R.id.selectsticker_button_showtopic);

        pageNavigation.initializeLayoutForAllNavigationButtons(findViewById(R.id.header_layout_showtopic), findViewById(R.id.shadow_header_showtopic));
        pageNavigation.initializePagerView((ViewPager) findViewById(R.id.pager_showtopic));
        pageNavigation.initializeNavigationButtons((Button) findViewById(R.id.firstpage_button_showtopic), (Button) findViewById(R.id.previouspage_button_showtopic),
                (Button) findViewById(R.id.currentpage_button_showtopic), (Button) findViewById(R.id.nextpage_button_showtopic), (Button) findViewById(R.id.lastpage_button_showtopic));

        pageNavigation.setDrawableForCurrentPageButton(arrowDrawable);

        pageNavigation.updateAdapterForPagerView();
        actionsForMessages = new JVCMessageInTopicAction(this);
        actionsForMessages.setNewMessageIsQuotedListener(messageIsQuotedListener);
        senderForMessages = new JVCMessageToTopicSender(this);
        senderForMessages.setListenerForNewMessageWantEdit(listenerForNewMessageWantEdit);
        senderForMessages.setListenerForNewMessagePosted(listenerForNewMessagePosted);
        messageSendButton.setOnClickListener(sendMessageToTopicListener);
        messageSendButton.setOnLongClickListener(refreshFromSendButton);
        selectStickerButton.setOnClickListener(selectStickerClickedListener);

        pageNavigation.setCurrentLink(sharedPref.getString(getString(R.string.prefTopicUrlToFetch), ""));
        updateShowNavigationButtons();
        if (savedInstanceState == null) {
            if (getIntent() != null) {
                goToBottomAtPageLoading = getIntent().getBooleanExtra(EXTRA_GO_TO_BOTTOM, false);
                currentTitles.topic = getIntent().getStringExtra(EXTRA_TOPIC_NAME);
                currentTitles.forum = getIntent().getStringExtra(EXTRA_FORUM_NAME);

                if (currentTitles.topic == null) {
                    currentTitles.topic = "";
                }
                if (Utils.stringIsEmptyOrNull(currentTitles.forum)) {
                    currentTitles.forum = getString(R.string.app_name);
                }

                if (getIntent().getStringExtra(EXTRA_TOPIC_LINK) != null) {
                    pageNavigation.setCurrentLink(getIntent().getStringExtra(EXTRA_TOPIC_LINK));
                }
            } else {
                currentTitles.topic = "";
                currentTitles.forum = getString(R.string.app_name);
            }

            updateLastPageAndCurrentItemAndButtonsToCurrentLink();
        } else {
            currentTitles.forum = savedInstanceState.getString(SAVE_CURRENT_FORUM_TITLE_FOR_TOPIC, getString(R.string.app_name));
            currentTitles.topic = savedInstanceState.getString(SAVE_CURRENT_TOPIC_TITLE_FOR_TOPIC, "");
            pageNavigation.setLastPageNumber(savedInstanceState.getInt(SAVE_LAST_PAGE, pageNavigation.getCurrentItemIndex() + 1));
            getNewLockReason(savedInstanceState.getString(SAVE_REASON_OF_LOCK, null));
            pageNavigation.notifyDataSetChanged();

            senderForMessages.loadFromBundle(savedInstanceState);

            if (senderForMessages.getIsInEdit()) {
                messageSendButton.setImageResource(R.drawable.ic_action_content_edit);
            }

            pageNavigation.updateNavigationButtons();
        }
        reloadSettings();

        if (myActionBar != null) {
            myActionBar.setTitle(currentTitles.forum);
            myActionBar.setSubtitle(currentTitles.topic);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences.Editor sharedPrefEdit = sharedPref.edit();
        sharedPrefEdit.putInt(getString(R.string.prefLastActivityViewed), MainActivity.ACTIVITY_SHOW_TOPIC);
        sharedPrefEdit.apply();

        useInternalNavigatorForDefaultOpening = sharedPref.getBoolean(getString(R.string.settingsUseInternalNavigator), Boolean.valueOf(getString(R.string.useInternalNavigatorDefault)));
        convertNoelshackLinkToDirectLink = sharedPref.getBoolean(getString(R.string.settingsUseDirectNoelshackLink), Boolean.valueOf(getString(R.string.useDirectNoelshackLinkDefault)));
        showOverviewOnImageClick = sharedPref.getBoolean(getString(R.string.settingsShowOverviewOnImageClick), Boolean.valueOf(getString(R.string.showOverviewOnImageClickDefault)));
    }

    @Override
    public void onPause() {
        stopAllCurrentTask();
        if (!pageNavigation.getCurrentLink().isEmpty()) {
            SharedPreferences.Editor sharedPrefEdit = sharedPref.edit();
            sharedPrefEdit.putString(getString(R.string.prefTopicUrlToFetch), setShowedPageNumberForThisLink(pageNavigation.getCurrentLink(), pageNavigation.getCurrentItemIndex() + 1));
            sharedPrefEdit.apply();
        }
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SAVE_CURRENT_FORUM_TITLE_FOR_TOPIC, currentTitles.forum);
        outState.putString(SAVE_CURRENT_TOPIC_TITLE_FOR_TOPIC, currentTitles.topic);
        outState.putInt(SAVE_LAST_PAGE, pageNavigation.getLastPage());
        outState.putString(SAVE_REASON_OF_LOCK, reasonOfLock);
        senderForMessages.saveToBundle(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_showtopic, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.action_past_last_message_sended_showtopic).setEnabled(!lastMessageSended.isEmpty());

        if (!pseudoOfUser.isEmpty() && getCurrentFragment() != null) {
            if (getCurrentFragment().getIsInFavs() != null) {
                menu.findItem(R.id.action_change_topic_fav_value_showtopic).setEnabled(true);
                if (getCurrentFragment().getIsInFavs()) {
                    menu.findItem(R.id.action_change_topic_fav_value_showtopic).setTitle(R.string.removeOfFavs);
                } else {
                    menu.findItem(R.id.action_change_topic_fav_value_showtopic).setTitle(R.string.addToFavs);
                }
                return true;
            }
        }
        menu.findItem(R.id.action_change_topic_fav_value_showtopic).setEnabled(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_change_topic_fav_value_showtopic:
                if (currentTaskForFavs == null) {
                    currentTaskForFavs = new AddOrRemoveThingToFavs(!getCurrentFragment().getIsInFavs(), this);
                    currentTaskForFavs.execute(JVCParser.getForumIDOfThisTopic(pageNavigation.getCurrentLink()), getCurrentFragment().getTopicID(), getCurrentFragment().getLatestAjaxInfos().pref, cookieListInAString);
                } else {
                    Toast.makeText(ShowTopicActivity.this, R.string.errorActionAlreadyRunning, Toast.LENGTH_SHORT).show();
                }

                return true;
            case R.id.action_open_in_browser_showtopic:
                if (!useInternalNavigatorForDefaultOpening) {
                    Utils.openLinkInExternalNavigator(pageNavigation.getCurrentLink(), this);
                } else {
                    Utils.openLinkInInternalNavigator(pageNavigation.getCurrentLink(), this);
                }
                return true;
            case R.id.action_past_last_message_sended_showtopic:
                if (reasonOfLock == null) {
                    messageSendEdit.setText(lastMessageSended);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void newModeRequested(int newMode) {
        if (newMode == AbsShowTopicFragment.MODE_IRC || newMode == AbsShowTopicFragment.MODE_FORUM) {
            SharedPreferences.Editor sharedPrefEdit = sharedPref.edit();

            sharedPrefEdit.putInt(getString(R.string.prefCurrentTopicMode), newMode);
            sharedPrefEdit.apply();
            updateShowNavigationButtons();
            pageNavigation.updateAdapterForPagerView();

            if (newMode == AbsShowTopicFragment.MODE_FORUM) {
                updateLastPageAndCurrentItemAndButtonsToCurrentLink();
                if (pageNavigation.getCurrentItemIndex() > 0) {
                    pageNavigation.clearPageForThisFragment(0);
                }
            }
        }
    }

    @Override
    public void getNewForumAndTopicName(JVCParser.ForumAndTopicName newNames) {
        ActionBar myActionBar = getSupportActionBar();

        if (!newNames.topic.isEmpty()) {
            currentTitles.topic = newNames.topic;
        } else {
            currentTitles.topic = "";
        }

        if (!newNames.forum.isEmpty()) {
            currentTitles.forum = newNames.forum;
        } else {
            currentTitles.forum = getString(R.string.app_name);
        }

        if (myActionBar != null) {
            myActionBar.setTitle(currentTitles.forum);
            myActionBar.setSubtitle(currentTitles.topic);
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_quote_message:
                if (pseudoOfUser.isEmpty()) {
                    Toast.makeText(this, R.string.errorConnectNeeded, Toast.LENGTH_SHORT).show();
                } else if (reasonOfLock != null) {
                    Toast.makeText(this, R.string.errorTopicIsLocked, Toast.LENGTH_SHORT).show();
                } else {
                    actionsForMessages.startQuoteThisMessage(getCurrentFragment().getLatestAjaxInfos(), getCurrentFragment().getCurrentItemSelected(), cookieListInAString);
                }
                return true;
            case R.id.menu_edit_message:
                if (reasonOfLock == null) {
                    if (senderForMessages.getIsInEdit()) {
                        senderForMessages.cancelEdit();
                        messageSendButton.setEnabled(true);
                        messageSendButton.setImageResource(R.drawable.ic_action_content_send);
                        messageSendEdit.setText("");
                    } else {
                        boolean infoForEditAreGetted = false;
                        if (messageSendButton.isEnabled() && getCurrentFragment().getLatestAjaxInfos().list != null) {
                            String idOfMessage = Long.toString(getCurrentFragment().getCurrentItemSelected().id);
                            messageSendButton.setEnabled(false);
                            messageSendButton.setImageResource(R.drawable.ic_action_content_edit);
                            infoForEditAreGetted = senderForMessages.getInfosForEditMessage(idOfMessage, getCurrentFragment().getLatestAjaxInfos().list, cookieListInAString);
                        }

                        if (!infoForEditAreGetted) {
                            if (!messageSendButton.isEnabled()) {
                                Toast.makeText(this, R.string.errorMessageAlreadySending, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, R.string.errorInfosMissings, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, R.string.errorTopicIsLocked, Toast.LENGTH_SHORT).show();
                }

                return true;
            case R.id.menu_delete_message:
                actionsForMessages.startDeleteThisMessage(getCurrentFragment().getLatestAjaxInfos(), getCurrentFragment().getCurrentItemSelected(), cookieListInAString);
                return true;
            default:
                return getCurrentFragment().onMenuItemClick(item);
        }
    }

    @Override
    public void getNewLastPageNumber(String newNumber) {
        if (!newNumber.isEmpty()) {
            pageNavigation.setLastPageNumber(Integer.parseInt(newNumber));
        } else {
            pageNavigation.setLastPageNumber(pageNavigation.getCurrentItemIndex() + 1);
        }
        pageNavigation.notifyDataSetChanged();
        pageNavigation.updateNavigationButtons();
    }

    @Override
    public void newPageNumberChoosen(int newPageNumber) {
        if (!pageNavigation.getCurrentLink().isEmpty()) {
            if (newPageNumber > pageNavigation.getLastPage() || newPageNumber < 0) {
                newPageNumber = pageNavigation.getLastPage();
            } else if (newPageNumber < 1) {
                newPageNumber = 1;
            }

            pageNavigation.setCurrentItemIndex(newPageNumber - 1);
        }
    }

    @Override
    public void extendPageSelection(View buttonView) {
        if (pageNavigation.getIdOfThisButton(buttonView) == PageNavigationUtil.ID_BUTTON_CURRENT) {
            ChoosePageNumberDialogFragment choosePageDialogFragment = new ChoosePageNumberDialogFragment();
            choosePageDialogFragment.show(getFragmentManager(), "ChoosePageNumberDialogFragment");
        }
    }

    @Override
    public AbsShowSomethingFragment createNewFragmentForRead(String possibleTopicLink) {
        int currentTopicMode = sharedPref.getInt(getString(R.string.prefCurrentTopicMode), AbsShowTopicFragment.MODE_FORUM);
        AbsShowTopicFragment newFragment;

        if (currentTopicMode == AbsShowTopicFragment.MODE_FORUM) {
            newFragment = new ShowTopicModeForumFragment();
        } else {
            newFragment = new ShowTopicModeIRCFragment();
        }

        if (possibleTopicLink != null) {
            Bundle argForFrag = new Bundle();
            argForFrag.putString(AbsShowTopicFragment.ARG_TOPIC_LINK, possibleTopicLink);
            argForFrag.putBoolean(AbsShowTopicFragment.ARG_GO_TO_BOTTOM, goToBottomAtPageLoading);
            newFragment.setArguments(argForFrag);
            goToBottomAtPageLoading = false;
        }

        return newFragment;
    }

    @Override
    public int getShowablePageNumberForThisLink(String link) {
        return Integer.parseInt(JVCParser.getPageNumberForThisTopicLink(link));
    }

    @Override
    public String setShowedPageNumberForThisLink(String link, int newPageNumber) {
        return JVCParser.setPageNumberForThisTopicLink(link, newPageNumber);
    }

    @Override
    public void getClickedURL(String link, boolean itsLongClick) {
        if (convertNoelshackLinkToDirectLink) {
            if (JVCParser.checkIfItsNoelshackLink(link)) {
                link = JVCParser.noelshackToDirectLink(link);
            }
        }

        if (!itsLongClick) {
            String possibleNewLink = JVCParser.formatThisUrl(link);

            if (JVCParser.checkIfTopicAreSame(pageNavigation.getCurrentLink(), possibleNewLink)) {
                pageNavigation.setCurrentItemIndex(getShowablePageNumberForThisLink(possibleNewLink) - 1);
            } else if (JVCParser.checkIfItsJVCLink(possibleNewLink)) {
                Intent newShowForumIntent = new Intent(this, ShowForumActivity.class);
                newShowForumIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                newShowForumIntent.putExtra(ShowForumActivity.EXTRA_NEW_LINK, possibleNewLink);
                startActivity(newShowForumIntent);
                finish();
            } else if (showOverviewOnImageClick && JVCParser.checkIfItsNoelshackLink(link)) {
                Bundle argForFrag = new Bundle();
                ShowImageDialogFragment showImageDialogFragment = new ShowImageDialogFragment();
                argForFrag.putString(ShowImageDialogFragment.ARG_IMAGE_LINK, JVCParser.noelshackToDirectLink(link));
                showImageDialogFragment.setArguments(argForFrag);
                showImageDialogFragment.show(getFragmentManager(), "ShowImageDialogFragment");
            } else {
                if (!useInternalNavigatorForDefaultOpening) {
                    Utils.openLinkInExternalNavigator(link, this);
                } else {
                    Utils.openLinkInInternalNavigator(link, this);
                }
            }
        } else {
            Bundle argForFrag = new Bundle();
            LinkContextMenuDialogFragment linkMenuDialogFragment = new LinkContextMenuDialogFragment();
            argForFrag.putString(LinkContextMenuDialogFragment.ARG_URL, link);
            linkMenuDialogFragment.setArguments(argForFrag);
            linkMenuDialogFragment.show(getFragmentManager(), "LinkContextMenuDialogFragment");
        }
    }

    @Override
    public void getNewLockReason(String newReason) {
        if (!Utils.compareStrings(reasonOfLock, newReason)) {
            reasonOfLock = newReason;
            if (reasonOfLock == null) {
                selectStickerButton.setVisibility(View.VISIBLE);
                messageSendButton.setVisibility(View.VISIBLE);
                messageSendButton.setEnabled(true);
                messageSendEdit.setEnabled(true);
                messageSendEdit.setText("");
            } else {
                selectStickerButton.setVisibility(View.GONE);
                messageSendButton.setVisibility(View.GONE);
                messageSendButton.setEnabled(false);
                messageSendEdit.setEnabled(false);
                messageSendEdit.setText(getString(R.string.topicLockedForReason, Utils.truncateString(reasonOfLock, 80, getString(R.string.waitingText))));
            }
        }
    }

    @Override
    public void getSelectedSticker(String newStickerToAdd) {
        if (reasonOfLock == null) {
            messageSendEdit.append(newStickerToAdd);
        }
    }

    @Override
    public void getActionToFavsResult(String resultInString, boolean itsAnError) {
        if (itsAnError) {
            if (resultInString.isEmpty()) {
                resultInString = getString(R.string.errorInfosMissings);
            }
            Toast.makeText(this, resultInString, Toast.LENGTH_SHORT).show();
        } else {
            if (currentTaskForFavs.getAddToFavs()) {
                resultInString = getString(R.string.favAdded);
            } else {
                resultInString = getString(R.string.favRemoved);
            }
            Toast.makeText(this, resultInString, Toast.LENGTH_SHORT).show();
            getCurrentFragment().setIsInFavs(currentTaskForFavs.getAddToFavs());
        }
        currentTaskForFavs = null;
    }

    @Override
    public void getNewSurveyInfos(String surveyTitle, String topicID, String ajaxInfos) {
        Intent newShowSurveyIntent = new Intent(ShowTopicActivity.this, ShowSurveyActivity.class);
        newShowSurveyIntent.putExtra(ShowSurveyActivity.EXTRA_SURVEY_TITLE, surveyTitle);
        newShowSurveyIntent.putExtra(ShowSurveyActivity.EXTRA_TOPIC_ID, topicID);
        newShowSurveyIntent.putExtra(ShowSurveyActivity.EXTRA_AJAX_INFOS, ajaxInfos);
        newShowSurveyIntent.putExtra(ShowSurveyActivity.EXTRA_COOKIES, cookieListInAString);
        startActivity(newShowSurveyIntent);
    }
}