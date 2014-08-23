package com.explain.cblmessenger.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import com.explain.cblmessenger.CBLMessenger;
import com.explain.cblmessenger.Const;
import com.facebook.FacebookException;
import com.facebook.model.GraphUser;
import com.facebook.widget.FriendPickerFragment;
import com.facebook.widget.PickerFragment;

import java.util.List;


/**
 * activity allows you to pick some of your friends who already has installed this app
 * on clicking OK, selected users will be saved to global CBLMessenger application using method setSelectedUsers(List<GraphUser> selectedUsers)
 * so that this list can be later retrieved from another activity
 */
public class FriendPickerActivity extends FragmentActivity {

    private static final String TAG = "FriendPickerActivity";
    private static final String EXTRA_SELECTION_IDS = "extra_selection_ids";

    private FriendPickerFragment mPickerFragment;

    /**
     * let you pass list of users you want to be already selected, usually previously selected
     *
     * @param i            Intent to be used to start FriendPickerActivity
     * @param selectionIds list of users you want to be already selected
     * @return populated Intent with extra
     */
    public static Intent populateIntentWithSelectionIds(Intent i, List<String> selectionIds) {
        i.putExtra(EXTRA_SELECTION_IDS, selectionIds.toArray(Const.EMPTY_STRING_ARRAY));
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentManager fm = getSupportFragmentManager();
        if (savedInstanceState == null) {

            Bundle bundle = new Bundle();
            bundle.putBoolean(FriendPickerFragment.MULTI_SELECT_BUNDLE_KEY, true);
            bundle.putBoolean(FriendPickerFragment.SHOW_TITLE_BAR_BUNDLE_KEY, true);
            // TODO : figure out why here is null ???
            bundle.putString(FriendPickerFragment.USER_ID_BUNDLE_KEY, null);

            mPickerFragment = new FriendPickerFragment(bundle);
            fm.beginTransaction()
                    .replace(android.R.id.content,
                            mPickerFragment)
                    .commit();
        } else {
            mPickerFragment = (FriendPickerFragment) fm.findFragmentById(android.R.id.content);
        }

        mPickerFragment.setOnErrorListener(new PickerFragment.OnErrorListener() {
            @Override
            public void onError(PickerFragment<?> fragment, FacebookException error) {
                Log.e(TAG, error.getMessage());
            }
        });
        // set empty selection
        mPickerFragment.setSelection();

        // select previously selected users if there are so
        String[] selectionIds = getIntent().getStringArrayExtra(EXTRA_SELECTION_IDS);
        if (selectionIds != null && selectionIds.length > 0) {
            mPickerFragment.setSelectionByIds(selectionIds);
        }

        mPickerFragment.setOnDoneButtonClickedListener(new PickerFragment.OnDoneButtonClickedListener() {
            @Override
            public void onDoneButtonClicked(PickerFragment<?> fragment) {
                // get selected users
                List<GraphUser> users = mPickerFragment.getSelection();
                // and save them to list
                CBLMessenger.getInstance().setSelectedUsers(users);
                // and finish
                setResult(RESULT_OK, null);
                finish();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mPickerFragment.loadData(false);
    }
}
