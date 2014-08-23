package com.explain.cblmessenger.ui.activity;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.explain.cblmessenger.CBLMessenger;
import com.explain.cblmessenger.R;
import com.explain.cblmessenger.model.ChatRoom;
import com.explain.cblmessenger.model.User;
import com.explain.cblmessenger.model.utils.CBHelper;
import com.explain.cblmessenger.ui.fragment.GroupsFragment;
import com.explain.cblmessenger.utils.SingleFragmentActivity;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;


public class MainActivity extends SingleFragmentActivity {


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        User user = User.getUserById(CBHelper.INSTANCE.getDatabase(), CBLMessenger.getInstance().getCurrentUserId());
        getActionBar().setTitle(user.getName());
        // TODO : put user photo as ActionBar icon
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.act_hat_action_log_out:
                logOutUser();
                return true;

            case R.id.act_hat_action_revoke_access:
                revokeUser();
                return true;

            case R.id.act_hat_action_chat_room_docs:
                showChatRoomDocs();
                return true;

            case R.id.act_hat_action_user_docs:
                showUserDocs();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showDialogWithInfo(String title, String info) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        builder.setMessage(info);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private String queryToString(Query query) {
        QueryEnumerator enumerator = null;

        try {
            enumerator = query.run();
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }

        StringBuilder sb = new StringBuilder();

        if (enumerator != null) {
            for (int i = 0; i < enumerator.getCount(); i++) {
                Document doc = enumerator.getRow(i).getDocument();
                sb.append(doc.getProperties().toString());
                sb.append("\n\n");
            }
        }
        return sb.toString();
    }

    private void showUserDocs() {
        showDialogWithInfo("User docs", queryToString(User.getQuery(CBHelper.INSTANCE.getDatabase())));
    }

    private void showChatRoomDocs() {
        showDialogWithInfo("Chat room docs", queryToString(ChatRoom.getQuery(CBHelper.INSTANCE.getDatabase())));
    }

    /**
     * revoke any access permissions for user
     * (delete all data about the session)
     * will require login screen later
     */
    private void revokeUser() {
        Session session = Session.getActiveSession();
        if (session == null) {
            session = new Session(this);
            Session.setActiveSession(session);
        }

        new Request(
                session,
                "/me/permissions",
                null,
                HttpMethod.DELETE,
                new Request.Callback() {
                    @Override
                    public void onCompleted(Response response) {
                        clearUserPrefs();
                        Intent i = new Intent(MainActivity.this, LogInActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                    }
                }
        ).executeAsync();
    }

    private void logOutUser() {
        Session session = Session.getActiveSession();
        if (session != null) {
            if (!session.isClosed()) {
                session.closeAndClearTokenInformation();
                clearUserPrefs();
            }
        } else {
            session = new Session(this);
            Session.setActiveSession(session);
            session.closeAndClearTokenInformation();
            clearUserPrefs();
        }

        Intent i = new Intent(this, LogInActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
    }

    /*
     * clears any user prefs
     */
    private void clearUserPrefs() {
        CBLMessenger.getInstance().setCurrentUserId(null);
    }

    @Override
    public Fragment createFragment() {
        return new GroupsFragment();
    }
}
