package com.ns.yc.lifehelper.adapter;


import android.app.Activity;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;


public class GuideViewPagerAdapter extends PagerAdapter {

    private final ArrayList<View> views;
    private final Activity content;

    public GuideViewPagerAdapter(ArrayList<View> picLists, Activity context) {
        this.views = picLists;
        this.content = context;
    }

    @Override
    public int getCount() {
        if(views.size()!=0){
            return views.size();
        }else {
            return 0;
        }
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view==object;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        //super.destroyItem(container, position, object);       //注意：一定不要这个，否则崩溃
        container.removeView(views.get(position));
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        container.addView(views.get(position), 0);
        return views.get(position);
    }

}
