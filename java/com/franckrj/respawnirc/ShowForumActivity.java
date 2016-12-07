package com.franckrj.respawnirc;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.franckrj.respawnirc.dialogs.ChooseTopicOrForumLinkDialogFragment;
import com.franckrj.respawnirc.dialogs.HelpFirstLaunchDialogFragment;
import com.franckrj.respawnirc.jvctopictools.JVCTopicGetter;
import com.franckrj.respawnirc.jvctopictools.ShowForumFragment;
import com.franckrj.respawnirc.utils.JVCParser;

public class ShowForumActivity extends AppCompatActivity implements ChooseTopicOrForumLinkDialogFragment.NewTopicOrForumSelected,
                                                    ShowForumFragment.NewTopicWantRead, JVCTopicGetter.NewForumNameAvailable {
    private static final int LIST_DRAWER_POS_HOME = 0;
    private static final int LIST_DRAWER_POS_CONNECT = 1;
    private static final int LIST_DRAWER_POS_SELECT_TOPIC_OR_FORUM = 2;
    private static final int LIST_DRAWER_POS_SETTING = 3;

    private DrawerLayout layoutForDrawer = null;
    private ListView listForDrawer = null;
    private ActionBarDrawerToggle toggleForDrawer = null;
    private int lastNewActivitySelected = -1;
    private SharedPreferences sharedPref = null;
    private String currentTitle = "";

    private ListView.OnItemClickListener itemInDrawerClickedListener = new ListView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            lastNewActivitySelected = position;
            listForDrawer.setItemChecked(position, true);
            layoutForDrawer.closeDrawer(listForDrawer);
        }
    };

    private void createNewFragmentForForumRead(String possibleForumLink) {
        ShowForumFragment currentFragment = new ShowForumFragment();

        if (possibleForumLink != null) {
            Bundle argForFrag = new Bundle();
            argForFrag.putString(ShowForumFragment.ARG_FORUM_LINK, possibleForumLink);
            currentFragment.setArguments(argForFrag);
        }

        getFragmentManager().beginTransaction().replace(R.id.content_frame_showforum, currentFragment).commit();
    }

    private void setTopicOrForum(String link, boolean updateForumFragIfNeeded, String topicName) {
        if (link != null) {
            ShowForumFragment currentFragment = (ShowForumFragment) getFragmentManager().findFragmentById(R.id.content_frame_showforum);
            if (JVCParser.checkIfItsForumLink(link)) {
                currentFragment.goToThisNewPage(link);
            } else {
                Intent newShowTopicIntent = new Intent(this, ShowTopicActivity.class);

                if (updateForumFragIfNeeded) {
                    currentFragment.setForumByTopicLink(link);
                }

                if (topicName != null) {
                    newShowTopicIntent.putExtra(ShowTopicActivity.EXTRA_TOPIC_NAME, topicName);
                }
                if (!currentTitle.equals(getString(R.string.app_name))) {
                    newShowTopicIntent.putExtra(ShowTopicActivity.EXTRA_FORUM_NAME, currentTitle);
                }

                newShowTopicIntent.putExtra(ShowTopicActivity.EXTRA_TOPIC_LINK, link);
                startActivity(newShowTopicIntent);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_showforum);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar_showforum);
        setSupportActionBar(myToolbar);

        ActionBar myActionBar = getSupportActionBar();
        if (myActionBar != null) {
            myActionBar.setHomeButtonEnabled(true);
            myActionBar.setDisplayHomeAsUpEnabled(true);
        }

        sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        layoutForDrawer = (DrawerLayout) findViewById(R.id.layout_drawer_showforum);
        listForDrawer = (ListView) findViewById(R.id.view_left_drawer_showforum);

        toggleForDrawer = new ActionBarDrawerToggle(this, layoutForDrawer, R.string.openDrawerContentDescRes, R.string.closeDrawerContentDescRes) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                super.onDrawerSlide(drawerView, 0);
                lastNewActivitySelected = -1;
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                switch (lastNewActivitySelected) {
                    case LIST_DRAWER_POS_HOME:
                        break;
                    case LIST_DRAWER_POS_CONNECT:
                        startActivity(new Intent(ShowForumActivity.this, ConnectActivity.class));
                        break;
                    case LIST_DRAWER_POS_SELECT_TOPIC_OR_FORUM:
                        ChooseTopicOrForumLinkDialogFragment chooseLinkDialogFragment = new ChooseTopicOrForumLinkDialogFragment();
                        chooseLinkDialogFragment.show(getFragmentManager(), "ChooseTopicOrForumLinkDialogFragment");
                        listForDrawer.setItemChecked(0, true);
                        break;
                    case LIST_DRAWER_POS_SETTING:
                        startActivity(new Intent(ShowForumActivity.this, SettingsActivity.class));
                        break;
                }
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, 0);
            }
        };

        listForDrawer.setAdapter(new ArrayAdapter<>(this, R.layout.draweritem_row, getResources().getStringArray(R.array.itemChoiceDrawerList)));
        listForDrawer.setOnItemClickListener(itemInDrawerClickedListener);
        layoutForDrawer.addDrawerListener(toggleForDrawer);
        layoutForDrawer.setDrawerShadow(R.drawable.shadow_drawer, GravityCompat.START);

        if (savedInstanceState == null) {
            currentTitle = getString(R.string.app_name);
            createNewFragmentForForumRead(null);
        } else {
            currentTitle = savedInstanceState.getString(getString(R.string.saveCurrentForumTitle), getString(R.string.app_name));
        }
        setTitle(currentTitle);

        if (sharedPref.getBoolean(getString(R.string.prefIsFirstLaunch), true)) {
            SharedPreferences.Editor sharedPrefEdit = sharedPref.edit();
            HelpFirstLaunchDialogFragment firstLaunchDialogFragment = new HelpFirstLaunchDialogFragment();
            firstLaunchDialogFragment.show(getFragmentManager(), "HelpFirstLaunchDialogFragment");
            sharedPrefEdit.putBoolean(getString(R.string.prefIsFirstLaunch), false);
            sharedPrefEdit.apply();
        }
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        toggleForDrawer.syncState();
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences.Editor sharedPrefEdit = sharedPref.edit();
        listForDrawer.setItemChecked(0, true);
        sharedPrefEdit.putInt(getString(R.string.prefLastActivityViewed), MainActivity.ACTIVITY_SHOW_FORUM);
        sharedPrefEdit.apply();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(getString(R.string.saveCurrentForumTitle), currentTitle);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        toggleForDrawer.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (toggleForDrawer.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void newTopicOrForumAvailable(String newTopicOrForumLink) {
        setTopicOrForum(newTopicOrForumLink, true, null);
    }

    @Override
    public void setReadNewTopic(String newTopicLink, String newTopicName) {
        setTopicOrForum(newTopicLink, false, newTopicName);
    }

    @Override
    public void getNewForumName(String newForumName) {
        if (!newForumName.isEmpty()) {
            currentTitle = newForumName;
        } else {
            currentTitle = getString(R.string.app_name);
        }
        setTitle(currentTitle);
    }
}