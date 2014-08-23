package com.explain.cblmessenger;

import android.app.Application;
import android.content.Context;
import android.preference.PreferenceManager;

import com.explain.cblmessenger.common.logger.Log;
import com.explain.cblmessenger.common.logger.LogWrapper;
import com.explain.cblmessenger.model.utils.CBHelper;
import com.facebook.model.GraphUser;

import java.util.List;

/**
 * Created by bamboo on 22.08.14.
 */
public class CBLMessenger extends Application {

    private static CBLMessenger sInstance;

    // list of users picked from Facebook's FriendPickerFragment
    private List<GraphUser> selectedUsers;

    private String mCurrentUserId;

    @Override
    public void onCreate() {
        super.onCreate();

        sInstance = this;
        // IMPORTANT : initialize logger, since use own
        Log.setLogNode(new LogWrapper());
        // call anything from CBHelper to run its constructor
        CBHelper.INSTANCE.getManager();
    }

    public static Context getAppContext() {
        return sInstance;
    }

    public String getCurrentUserId() {
        if (mCurrentUserId == null) {
            mCurrentUserId = PreferenceManager.getDefaultSharedPreferences(this)
                    .getString("user_id", null);
        }
        return mCurrentUserId;
    }

    public static CBLMessenger getInstance() {
        return sInstance;
    }

    public void setCurrentUserId(String userId) {
        mCurrentUserId = userId;
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString("user_id", userId)
                .apply();
    }


    public List<GraphUser> getSelectedUsers() {
        return selectedUsers;
    }

    public void setSelectedUsers(List<GraphUser> selectedUsers) {
        this.selectedUsers = selectedUsers;
    }
}
