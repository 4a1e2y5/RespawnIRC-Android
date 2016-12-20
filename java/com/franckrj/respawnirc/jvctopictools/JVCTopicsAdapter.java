package com.franckrj.respawnirc.jvctopictools;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.franckrj.respawnirc.R;
import com.franckrj.respawnirc.utils.JVCParser;
import com.franckrj.respawnirc.utils.Undeprecator;
import com.franckrj.respawnirc.utils.Utils;

import java.util.ArrayList;

public class JVCTopicsAdapter extends BaseAdapter {
    private ArrayList<JVCParser.TopicInfos> listOfTopics = new ArrayList<>();
    private ArrayList<ContentHolder> listOfContentForTopics = new ArrayList<>();
    private LayoutInflater serviceInflater;
    private Activity parentActivity = null;
    private boolean alternateBackgroundColor = false;
    private Drawable iconMarqueOn = null;
    private Drawable iconMarqueOff = null;
    private Drawable iconDossier2 = null;
    private Drawable iconLock = null;
    private Drawable iconResolu = null;
    private Drawable iconDossier1 = null;

    public JVCTopicsAdapter(Activity newParentActivity) {
        parentActivity = newParentActivity;
        serviceInflater = (LayoutInflater) parentActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        iconMarqueOn = Undeprecator.resourcesGetDrawable(parentActivity.getResources(), R.drawable.icon_topic_marque_on);
        iconMarqueOff = Undeprecator.resourcesGetDrawable(parentActivity.getResources(), R.drawable.icon_topic_marque_off);
        iconDossier2 = Undeprecator.resourcesGetDrawable(parentActivity.getResources(), R.drawable.icon_topic_dossier2);
        iconLock = Undeprecator.resourcesGetDrawable(parentActivity.getResources(), R.drawable.icon_topic_lock);
        iconResolu = Undeprecator.resourcesGetDrawable(parentActivity.getResources(), R.drawable.icon_topic_resolu);
        iconDossier1 = Undeprecator.resourcesGetDrawable(parentActivity.getResources(), R.drawable.icon_topic_dossier1);
    }

    public ArrayList<JVCParser.TopicInfos> getAllItems() {
        return listOfTopics;
    }

    public void setAlternateBackgroundColor(boolean newVal) {
        alternateBackgroundColor = newVal;
    }

    public void removeAllItems() {
        listOfTopics.clear();
        listOfContentForTopics.clear();
    }

    public void addItem(JVCParser.TopicInfos item) {
        String textForAuthor;
        ContentHolder thisHolder = new ContentHolder();
        thisHolder.firstLineContent = Undeprecator.htmlFromHtml("<b><font color=\"" + Utils.resColorToString(R.color.linkColor, parentActivity) +
                                        "\">" + item.name + "</font> (" + item.messages + ")</b>");
        switch (item.authorType) {
            case "modo":
                textForAuthor = "<small><font color=\"" + Utils.resColorToString(R.color.colorPseudoModo, parentActivity) + "\">" + item.author + "</font></small>";
                break;
            case "admin":
            case "staff":
                textForAuthor = "<small><font color=\"" + Utils.resColorToString(R.color.colorPseudoAdmin, parentActivity) + "\">" + item.author + "</font></small>";
                break;
            default:
                textForAuthor = "<small>" + item.author + "</small>";
                break;
        }
        thisHolder.secondLineContent = Undeprecator.htmlFromHtml(textForAuthor);
        thisHolder.thirdLineContent = Undeprecator.htmlFromHtml("<small>" + item.wholeDate + "</small>");
        listOfTopics.add(item);
        listOfContentForTopics.add(thisHolder);
    }

    public void updateAllItems() {
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return listOfTopics.size();
    }

    @Override
    public JVCParser.TopicInfos getItem(int position) {
        return listOfTopics.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        ContentHolder currentTopicContent = listOfContentForTopics.get(position);
        JVCParser.TopicInfos currentTopicInfos = listOfTopics.get(position);

        if (convertView == null) {
            holder = new ViewHolder();

            convertView = serviceInflater.inflate(R.layout.jvctopics_row, parent, false);
            holder.firstLine = (TextView) convertView.findViewById(R.id.item_one_jvctopics_text_row);
            holder.secondLine = (TextView) convertView.findViewById(R.id.item_two_jvctopics_text_row);
            holder.thirdLine = (TextView) convertView.findViewById(R.id.item_three_jvctopics_text_row);
            holder.topicIcon = (ImageView) convertView.findViewById(R.id.item_topic_icon_jvctopics_image_row);
            holder.background = convertView.getBackground();

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.firstLine.setText(currentTopicContent.firstLineContent);
        holder.secondLine.setText(currentTopicContent.secondLineContent);
        holder.thirdLine.setText(currentTopicContent.thirdLineContent);

        switch (currentTopicInfos.type) {
            case "marque-on":
                holder.topicIcon.setImageDrawable(iconMarqueOn);
                break;
            case "marque-off":
                holder.topicIcon.setImageDrawable(iconMarqueOff);
                break;
            case "dossier2":
                holder.topicIcon.setImageDrawable(iconDossier2);
                break;
            case "lock":
                holder.topicIcon.setImageDrawable(iconLock);
                break;
            case "resolu":
                holder.topicIcon.setImageDrawable(iconResolu);
                break;
            case "dossier1":
            default:
                holder.topicIcon.setImageDrawable(iconDossier1);
                break;
        }

        if (position % 2 == 0 || !alternateBackgroundColor) {
            Undeprecator.viewSetBackgroundDrawable(convertView, holder.background);
        } else {
            convertView.setBackgroundColor(Undeprecator.resourcesGetColor(parentActivity.getResources(), R.color.altBackgroundMessageColor));
        }

        return convertView;
    }

    private class ViewHolder {
        private TextView firstLine;
        private TextView secondLine;
        private TextView thirdLine;
        private Drawable background;
        private ImageView topicIcon;
    }

    private class ContentHolder {
        private Spanned firstLineContent;
        private Spanned secondLineContent;
        private Spanned thirdLineContent;
    }
}
