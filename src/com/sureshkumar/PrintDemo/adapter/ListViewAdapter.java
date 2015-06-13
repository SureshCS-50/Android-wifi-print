package com.sureshkumar.PrintDemo.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.sureshkumar.PrintDemo.R;

import java.util.ArrayList;

/**
 * Created by Sureshkumar on 09-06-2015.
 */
public class ListViewAdapter extends BaseAdapter {

    private Activity mActivity;

    private class ListItem {
        public String name;
        public boolean selected = false;

        public ListItem(String name,boolean selected){
            this.name = name;
            this.selected = selected;
        }
    }

    private ArrayList<ListItem> itemArray = new ArrayList<ListItem>();

    public ListViewAdapter(Activity mActivity, String[] mItems){
        this.mActivity = mActivity;
        for(String name:mItems){
            this.itemArray.add(new ListItem(name,false));
        }
    }

    @Override
    public int getCount() {
        return itemArray.size();
    }

    @Override
    public Object getItem(int i) {
        return itemArray.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {
        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View mView = inflater.inflate(R.layout.list_item, null);
        TextView mText = (TextView) mView.findViewById(R.id.text1);
        ImageView mImage = (ImageView) mView.findViewById(R.id.img1);

        mText.setText(itemArray.get(i).name);

        if(itemArray.get(i).selected){
            mText.setTextColor(mActivity.getResources().getColor(R.color.blue));
            mImage.setColorFilter(mActivity.getResources().getColor(R.color.blue), android.graphics.PorterDuff.Mode.MULTIPLY);
            mView.setBackgroundColor(mActivity.getResources().getColor(R.color.list_item_selected));
        } else{
            mText.setTextColor(mActivity.getResources().getColor(android.R.color.black));
            mImage.setColorFilter(mActivity.getResources().getColor(R.color.list_item_selected), android.graphics.PorterDuff.Mode.MULTIPLY);
            mView.setBackgroundColor(mActivity.getResources().getColor(android.R.color.white));
        }

        return mView;
    }

    public void setSelected(int index){
        final String name = itemArray.get(index).name;
        for(int i = 0; i< itemArray.size(); i++) {
            if(i == index) {
                itemArray.set(i, new ListItem(name, true));
            } else {
                itemArray.set(i, new ListItem(itemArray.get(i).name, false));
            }
        }
    }
}
