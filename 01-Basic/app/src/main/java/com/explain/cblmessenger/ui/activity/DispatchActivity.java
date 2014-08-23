package com.explain.cblmessenger.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.explain.cblmessenger.CBLMessenger;
import com.facebook.Session;

/**
 * it is launch activity
 */
public class DispatchActivity extends Activity {

    private static final String TAG = "DispatchActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //  here we check if user was previously logged in (user_id is stored in SharedPreferences)
        String userId = CBLMessenger.getInstance().getCurrentUserId();

        Log.d(TAG, userId == null ? "null" : userId);
        // if userId not null, check if it's session is still valid
        if (userId != null && isOpenFacebookSession()) {
            Intent i = new Intent(this, MainActivity.class);
            // make no history on back
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        } else {
            // need to log in
            Intent i = new Intent(this, LogInActivity.class);
            // make no history on back
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        }
    }

    /**
     * check is Facebook still keeps an open session of last logged in user
     *
     * @return true - if session is open and still valid, other - false
     */
    private boolean isOpenFacebookSession() {
        Session session = Session.getActiveSession();

        Log.d(TAG, session == null ? "null" : session.toString());
        // dark magic with Facebook SDK
        if (session != null) {
            // session can be open, check for valid Token
            if (!session.isClosed()) {
                if (!session.getAccessToken().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }
}
