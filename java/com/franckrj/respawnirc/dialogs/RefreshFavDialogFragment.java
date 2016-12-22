package com.franckrj.respawnirc.dialogs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.franckrj.respawnirc.R;
import com.franckrj.respawnirc.utils.JVCParser;
import com.franckrj.respawnirc.utils.WebManager;

import java.util.ArrayList;

public class RefreshFavDialogFragment extends DialogFragment {
    public static final String ARG_PSEUDO = "com.franckrj.respawnirc.refreshfavdialogfragment.pseudo";
    public static final String ARG_FAV_TYPE = "com.franckrj.respawnirc.refreshfavdialogfragment.fav_type";
    public static final int FAV_FORUM = 0;
    public static final int FAV_TOPIC = 1;

    private GetFavsOfPseudo currentTaskGetFavs = null;

    private void stopAllCurrentTask() {
        if (currentTaskGetFavs != null) {
            currentTaskGetFavs.cancel(true);
            currentTaskGetFavs = null;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle currentArgs = getArguments();
        String pseudoToFetch = "";
        int typeOfFav = FAV_FORUM;

        if (currentArgs != null) {
            pseudoToFetch = currentArgs.getString(ARG_PSEUDO, "");
            typeOfFav = currentArgs.getInt(ARG_FAV_TYPE, FAV_FORUM);
        }

        if (pseudoToFetch.isEmpty()) {
            dismiss();
        } else {
            currentTaskGetFavs = new GetFavsOfPseudo(typeOfFav);
            currentTaskGetFavs.execute(pseudoToFetch);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View mainView = inflater.inflate(R.layout.dialog_loading, container, false);
        TextView textView = (TextView) mainView.findViewById(R.id.textview_loading_dialog);
        textView.setText(R.string.loadingFavs);

        return mainView;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        if (dialog.getWindow() != null) {
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }

        return dialog;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        stopAllCurrentTask();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        stopAllCurrentTask();
    }

    private class GetFavsOfPseudo extends AsyncTask<String, Void, ArrayList<JVCParser.NameAndLink>> {
        final int typeOfFav;

        GetFavsOfPseudo(int newTypeOfFav) {
            typeOfFav = newTypeOfFav;
        }

        @Override
        protected ArrayList<JVCParser.NameAndLink> doInBackground(String... params) {
            if (params.length > 0) {
                WebManager.WebInfos currentWebInfos = new WebManager.WebInfos();
                String pageContent;
                currentWebInfos.followRedirects = false;
                pageContent = WebManager.sendRequest("http://www.jeuxvideo.com/profil/" + params[0].toLowerCase(), "GET", "mode=favoris", "", currentWebInfos);

                if (pageContent != null) {
                    if (typeOfFav == FAV_FORUM) {
                        return JVCParser.getListOfForumsFavs(pageContent);
                    } else {
                        return JVCParser.getListOfTopicsFavs(pageContent);
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<JVCParser.NameAndLink> listOfForumsFavs) {
            super.onPostExecute(listOfForumsFavs);

            if (getActivity() instanceof NewFavsAvailable) {
                ((NewFavsAvailable) getActivity()).getNewForumsFavs(listOfForumsFavs, typeOfFav);
            }

            currentTaskGetFavs = null;
            dismiss();
        }
    }

    public interface NewFavsAvailable {
        void getNewForumsFavs(ArrayList<JVCParser.NameAndLink> listOfFavs, int typeOfFav);
    }
}
