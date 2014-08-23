package com.explain.cblmessenger.utils;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;

/**
 * simple activity that host one single fragment
 * its descendants will implement abstract method createFragment()
 */
public abstract class SingleFragmentActivity extends Activity {

    public abstract Fragment createFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fm = getFragmentManager();
        if (fm.findFragmentById(android.R.id.content) == null) {
            fm.beginTransaction()
                    .replace(android.R.id.content, createFragment())
                    .commit();
        }
    }
}