package com.franckrj.respawnirc;

import android.app.Activity;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.franckrj.respawnirc.utils.Undeprecator;

import java.util.ArrayList;

public class NavigationMenuAdapter extends BaseAdapter {
    private ArrayList<MenuItemInfo> listOfMenuItem = new ArrayList<>();
    private Activity parentActivity = null;
    private LayoutInflater serviceInflater = null;
    private int rowSelected = -1;
    private @ColorInt int selectedItemColor;
    private @ColorInt int unselectedItemColor;
    private @ColorInt int selectedBackgroundColor;
    private @ColorInt int unselectedBackgroundColor;
    private @ColorInt int normalTextColor;
    private @ColorInt int headerTextColor;

    NavigationMenuAdapter(Activity newParentActivity) {
        parentActivity = newParentActivity;
        serviceInflater = (LayoutInflater) parentActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public int getItemIDOfRow(int position) {
        if (position < listOfMenuItem.size()) {
            return listOfMenuItem.get(position).itemID;
        }
        return -1;
    }

    public int getGroupIDOfRow(int position) {
        if (position < listOfMenuItem.size()) {
            return listOfMenuItem.get(position).groupID;
        }
        return -1;
    }

    public int getPositionDependingOfID(int itemID, int groupID) {
        if (itemID != -1) {
            for (int i = 0; i < listOfMenuItem.size(); ++i) {
                MenuItemInfo currentItemInfo = listOfMenuItem.get(i);
                if (currentItemInfo.itemID == itemID && (groupID == -1 || currentItemInfo.groupID == groupID)) {
                    return i;
                }
            }
        }

        return -1;
    }

    public void removeAllItemsFromGroup(int groupID) {
        for (int i = 0; i < listOfMenuItem.size(); ) {
            if (listOfMenuItem.get(i).groupID == groupID) {
                listOfMenuItem.remove(i);
            } else {
                ++i;
            }
        }
    }

    public void setListOfMenuItem(ArrayList<MenuItemInfo> newList) {
        listOfMenuItem = newList;
    }

    public void setBackgroundColors(@ColorInt int newSelectedItemColor, @ColorInt int newUnselectedItemColor,
                                    @ColorInt int newSelectedBackgroundColor, @ColorInt int newUnselectedBackgroundColor) {
        selectedItemColor = newSelectedItemColor;
        unselectedItemColor = newUnselectedItemColor;
        selectedBackgroundColor = newSelectedBackgroundColor;
        unselectedBackgroundColor = newUnselectedBackgroundColor;
    }

    public void setFontColors(@ColorInt int newNormalTextColor, @ColorInt int newHeaderTextColor) {
        normalTextColor = newNormalTextColor;
        headerTextColor = newHeaderTextColor;
    }

    public void setRowSelected(int newVal) {
        rowSelected = newVal;
    }

    public void setRowEnabled(int position, boolean newVal) {
        if (position < listOfMenuItem.size()) {
            listOfMenuItem.get(position).isEnabled = newVal;
        }
    }

    public void setRowText(int position, String newText) {
        if (position < listOfMenuItem.size()) {
            listOfMenuItem.get(position).textContent = newText;
        }
    }

    public void updateList() {
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return listOfMenuItem.size();
    }

    @Override
    public Object getItem(int position) {
        return listOfMenuItem.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        CustomViewHolder holder;
        MenuItemInfo currentMenuItemInfo = listOfMenuItem.get(position);

        if (convertView == null) {
            holder = new CustomViewHolder();

            convertView = serviceInflater.inflate(R.layout.navigationmenu_row, parent, false);
            holder.contentTextView = convertView.findViewById(R.id.content_text_navigationmenu);
            holder.upperLineView = convertView.findViewById(R.id.upper_line_navigationmenu);

            holder.contentTextView.setCompoundDrawablePadding(parentActivity.getResources().getDimensionPixelSize(R.dimen.paddingForCompoundDrawableNavigationMenu));
            convertView.setTag(holder);
        } else {
            holder = (CustomViewHolder) convertView.getTag();
        }

        holder.contentTextView.setText(currentMenuItemInfo.textContent);
        holder.contentTextView.setAlpha(currentMenuItemInfo.isEnabled ? 1f : 0.33f);

        if (currentMenuItemInfo.isHeader) {
            if (position > 0) {
                holder.upperLineView.setVisibility(View.VISIBLE);
            } else {
                holder.upperLineView.setVisibility(View.INVISIBLE);
            }
            holder.contentTextView.setTextColor(headerTextColor);
        } else {
            holder.upperLineView.setVisibility(View.GONE);
            holder.contentTextView.setTextColor(normalTextColor);
        }

        if (currentMenuItemInfo.drawableResID != 0) {
            Drawable compoundDrawable = Undeprecator.resourcesGetDrawable(parentActivity.getResources(), currentMenuItemInfo.drawableResID).mutate();

            if (rowSelected == position && currentMenuItemInfo.isEnabled) {
                compoundDrawable.setColorFilter(selectedItemColor, PorterDuff.Mode.SRC_ATOP);
            } else {
                compoundDrawable.setColorFilter(unselectedItemColor, PorterDuff.Mode.SRC_ATOP);
            }

            holder.contentTextView.setCompoundDrawablesWithIntrinsicBounds(compoundDrawable, null, null, null);
        } else {
            holder.contentTextView.setCompoundDrawables(null, null, null, null);
        }

        if (rowSelected == position && currentMenuItemInfo.isEnabled) {
            convertView.setBackgroundColor(selectedBackgroundColor);
        } else {
            convertView.setBackgroundColor(unselectedBackgroundColor);
        }

        return convertView;
    }

    @Override
    public boolean isEnabled(int position) {
        MenuItemInfo currentItemInfo = listOfMenuItem.get(position);
        return !currentItemInfo.isHeader && currentItemInfo.isEnabled;
    }

    private class CustomViewHolder {
        public TextView contentTextView = null;
        public View upperLineView = null;
    }

    public static class MenuItemInfo {
        public String textContent = "";
        public @DrawableRes int drawableResID = 0;
        public boolean isHeader = false;
        public boolean isEnabled = true;
        public int itemID = -1;
        public int groupID = -1;
    }
}
