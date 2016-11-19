package com.franckrj.respawnirc;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;

import java.net.URLEncoder;

class JVCMessageSender {
    private Activity parentActivity = null;
    private String ajaxListInfos = null;
    private boolean isInEdit = false;
    private String lastInfosForEdit = null;
    private NewMessageWantEditListener listenerForNewMessageWantEdit = null;
    private NewMessagePostedListener listenerForNewMessagePosted = null;

    JVCMessageSender(Activity newParentActivity) {
        parentActivity = newParentActivity;
    }

    boolean getIsInEdit() {
        return isInEdit;
    }

    void setListenerForNewMessageWantEdit(NewMessageWantEditListener newListener) {
        listenerForNewMessageWantEdit = newListener;
    }
    
    void setListenerForNewMessagePosted(NewMessagePostedListener newListener) {
        listenerForNewMessagePosted = newListener;
    }

    void loadFromBundle(Bundle savedInstanceState) {
        ajaxListInfos = savedInstanceState.getString(parentActivity.getString(R.string.saveOldAjaxListInfos), null);
        isInEdit = savedInstanceState.getBoolean(parentActivity.getString(R.string.saveIsInEdit), false);
        lastInfosForEdit = savedInstanceState.getString(parentActivity.getString(R.string.saveLastInfosForEdit), null);
    }

    void saveToBundle(Bundle savedInstanceState) {
        savedInstanceState.putString(parentActivity.getString(R.string.saveOldAjaxListInfos), ajaxListInfos);
        savedInstanceState.putBoolean(parentActivity.getString(R.string.saveIsInEdit), isInEdit);
        savedInstanceState.putString(parentActivity.getString(R.string.saveLastInfosForEdit), lastInfosForEdit);
    }

    void sendEditMessage(String messageEditedToSend, String cookieListInAString) {
        sendThisMessage(messageEditedToSend, "http://www.jeuxvideo.com/forums/ajax_edit_message.php", lastInfosForEdit, cookieListInAString);
    }

    void sendThisMessage(String messageToSend, String urlToSend, String latestListOfInput, String cookieListInAString) {
        try {
            messageToSend = URLEncoder.encode(messageToSend, "UTF-8");
        } catch (Exception e) {
            messageToSend = "";
            e.printStackTrace();
        }

        new PostJVCMessage().execute(urlToSend, messageToSend, latestListOfInput + "&form_alias_rang=1", cookieListInAString);
    }

    void getInfosForEditMessage(String idOfMessage, String oldAjaxListInfos, String cookieListInAString) {
        isInEdit = true;
        ajaxListInfos = oldAjaxListInfos;
        lastInfosForEdit = "&id_message=" + idOfMessage + "&";

        new GetEditJVCMessageInfos().execute(idOfMessage, oldAjaxListInfos, cookieListInAString);
    }

    private class GetEditJVCMessageInfos extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            if (params.length > 2) {
                return WebManager.sendRequest("http://www.jeuxvideo.com/forums/ajax_edit_message.php", "GET", "id_message=" + params[0] + "&" + params[1] + "&action=get", params[2]);
            } else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(String pageResult) {
            super.onPostExecute(pageResult);
            String newMessageEdit = "";

            if (pageResult != null) {
                if (!pageResult.isEmpty()) {
                    lastInfosForEdit += ajaxListInfos + "&action=post";
                    pageResult = JVCParser.parsingAjaxMessages(pageResult);
                    lastInfosForEdit += JVCParser.getListOfInputInAString(pageResult);
                    newMessageEdit = JVCParser.getMessageEdit(pageResult);
                }
            }

            if (newMessageEdit.isEmpty()) {
                isInEdit = false;
                lastInfosForEdit = null;
                ajaxListInfos = null;
            }

            if (listenerForNewMessageWantEdit != null) {
                listenerForNewMessageWantEdit.initializeEditMode(newMessageEdit);
            }
        }
    }

    private class PostJVCMessage extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            if (params.length > 3) {
                return WebManager.sendRequest(params[0], "POST", "message_topic=" + params[1] + params[2], params[3]);
            } else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(String pageResult) {
            super.onPostExecute(pageResult);
            String errorWhenSending = null;

            if (pageResult != null) {
                if (!pageResult.isEmpty()) {
                    if (!isInEdit) {
                        if (listenerForNewMessagePosted != null) {
                            errorWhenSending = JVCParser.getErrorMessage(pageResult);
                        }
                    }
                }
            }

            if (isInEdit) {
                isInEdit = false;
                lastInfosForEdit = null;
                ajaxListInfos = null;
            }

            if (listenerForNewMessagePosted != null) {
                listenerForNewMessagePosted.lastMessageIsSended(errorWhenSending);
            }
        }
    }

    interface NewMessageWantEditListener {
        void initializeEditMode(String newMessageToEdit);
    }

    interface NewMessagePostedListener {
        void lastMessageIsSended(String withThisError);
    }
}