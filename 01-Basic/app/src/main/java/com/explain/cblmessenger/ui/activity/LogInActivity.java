package com.explain.cblmessenger.ui.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.explain.cblmessenger.CBLMessenger;
import com.explain.cblmessenger.Const;
import com.explain.cblmessenger.R;
import com.explain.cblmessenger.model.User;
import com.explain.cblmessenger.model.utils.CBHelper;
import com.explain.cblmessenger.utils.Utils;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class LogInActivity extends Activity {

    private static final String TAG = "LogInActivity";
    private Button mLogInButton;
    private Session.StatusCallback mStatusCallbacks = new FacebookSessionStatusCallbacks();

    /**
     * method prints unique hash key of the app
     * use that generated key to register your app in Facebook API
     */
    private void printKeyHash() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    "com.explain.cblmessenger",
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("KeyHash", e.toString());
        } catch (NoSuchAlgorithmException e) {
            Log.d("KeyHash", e.toString());
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        printKeyHash();
        setContentView(R.layout.act_lia_layout);

        mLogInButton = (Button) findViewById(R.id.act_lia_log_in_btn);

        mLogInButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        logInWithFacebookAndStartSync();
                    }
                }
        );
    }

    /**
     * open Facebook session for current user
     */
    private void logInWithFacebookAndStartSync() {
        Session session = Session.getActiveSession();

        if (session == null) {
            session = new Session(this);
            Session.setActiveSession(session);
        }

        if (!session.isOpened() && !session.isClosed()) {
            session.openForRead(getFacebookOpenRequest());
        } else {
            Session.openActiveSession(this, true, Arrays.asList(Const.FACEBOOK_BASE_PERMISSIONS), mStatusCallbacks);
        }
    }

    private Session.OpenRequest getFacebookOpenRequest() {
        Session.OpenRequest request = new Session.OpenRequest(this)
                .setPermissions(Arrays.asList(Const.FACEBOOK_BASE_PERMISSIONS))
                .setCallback(mStatusCallbacks);
        return request;
    }

    /**
     * check if user confirmed all suggested permissions
     * if not, ask user again to confirm them
     *
     * @param session current session
     * @return
     */
    private boolean sessionHasNecessaryPermissions(Session session) {
        if (session != null && session.getPermissions() != null) {
            for (String perm : Const.FACEBOOK_BASE_PERMISSIONS) {
                if (!session.getPermissions().contains(perm)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }


    /**
     * finad all permissions that user was offered to confirm but rejected
     *
     * @param session current session
     * @return list of missing permissions
     */
    private List<String> getMissingPermissions(Session session) {
        List<String> missingPermissions = new ArrayList<String>(Arrays.asList(Const.FACEBOOK_BASE_PERMISSIONS));
        if (session != null && session.getPermissions() != null) {
            for (String requestPerm : Const.FACEBOOK_BASE_PERMISSIONS) {
                if (session.getPermissions().contains(requestPerm)) {
                    missingPermissions.remove(requestPerm);
                }
            }
        }
        return missingPermissions;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Session session = Session.getActiveSession();
        if (session != null) {
            session.onActivityResult(this, requestCode, resultCode, data);
        }
    }


    private class FacebookSessionStatusCallbacks implements Session.StatusCallback {

        @Override
        public void call(final Session session, SessionState state, Exception exception) {
            if (session == null) {
                return;
            }

            if (state.isOpened() && !sessionHasNecessaryPermissions(session)) {
                // user not confirmed some permissions, ask again
                AlertDialog.Builder builder = new AlertDialog.Builder(LogInActivity.this);
                builder.setTitle("Confirm the permissions");
                builder.setMessage("We need your permission\nPlease, confirm them on the following screen");
                builder.setPositiveButton(
                        "OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                session.requestNewReadPermissions(
                                        new Session.NewPermissionsRequest(
                                                LogInActivity.this,
                                                getMissingPermissions(session))
                                );
                            }
                        }
                );
                builder.setNegativeButton(
                        "Quit",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        }
                );
                builder.show();
            } else {

                Request.newMeRequest(session, new Request.GraphUserCallback() {
                    @Override
                    public void onCompleted(GraphUser user, Response response) {
                        if (user == null) {
                            return;
                        }
                        // user logged in and gave all permissions
                        // retrieve all needed info

                        String userEmail = (String) user.getProperty("email");
                        String userName = user.getName();

                        CBLMessenger.getInstance().setCurrentUserId(userEmail);
                        try {
                            User u = User.getUserById(CBHelper.INSTANCE.getDatabase(), userEmail);
                            if (u == null) {
                                User.createUser(CBHelper.INSTANCE.getDatabase(), user.getId(), userName, userEmail);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(LogInActivity.this, "error creating user, try again later", Toast.LENGTH_LONG)
                                    .show();
                            return;
                        }

                        // here we are finally logged in and created user properly

                        CBHelper.INSTANCE.startReplicationWithFacebookAccessToken(session.getAccessToken());

                        Log.d(TAG, "user:" + userEmail);
                        Utils.toast("user : " + userEmail);
                        Intent i = new Intent(LogInActivity.this, MainActivity.class);
                        // clear task in order not to return to it when user press back button
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                    }
                }).executeAsync();
            }
        }
    }

}
