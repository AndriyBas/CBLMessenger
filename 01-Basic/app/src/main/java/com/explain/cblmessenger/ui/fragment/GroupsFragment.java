package com.explain.cblmessenger.ui.fragment;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Query;
import com.explain.cblmessenger.CBLMessenger;
import com.explain.cblmessenger.R;
import com.explain.cblmessenger.common.logger.Log;
import com.explain.cblmessenger.model.ChatRoom;
import com.explain.cblmessenger.model.utils.CBHelper;
import com.explain.cblmessenger.ui.activity.ChatActivity;
import com.explain.cblmessenger.ui.adapter.GroupsAdapter;
import com.explain.cblmessenger.utils.Utils;

public class GroupsFragment extends ListFragment {
    private static final String TAG = "GroupFragment";


    private GroupsAdapter mGroupsAdapter;

    public GroupsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (mGroupsAdapter == null) {
            Query query = ChatRoom.getQuery(CBHelper.INSTANCE.getDatabase());
            mGroupsAdapter = new GroupsAdapter(getActivity(), query.toLiveQuery());
        }
        getListView().setAdapter(mGroupsAdapter);

        // add context menu to delete Group
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position,
                                           long id) {
                PopupMenu popup = new PopupMenu(getActivity(), view);
                popup.getMenu().add("Remove group");
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (item.getTitle().equals("Remove group")) {
                            ChatRoom room = new ChatRoom(mGroupsAdapter.getDocument(position));
                            if (!room.getOwnerId().equals(CBLMessenger.getInstance().getCurrentUserId())) {
                                Utils.toast("You must be crazy, only owner can delete the group ...");
                                return true;
                            }
                            room.delete();
                        }
                        return true;
                    }
                });
                popup.show();
                return true;
            }
        });
        setListShown(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.groups_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.act_hat_action_add_group:
                addGroupAction();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void addGroupAction() {

        final EditText editText = new EditText(getActivity());
        new AlertDialog.Builder(getActivity())
                .setTitle("Enter group name")
                .setView(editText)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String groupName = editText.getText().toString().trim();
                        if (groupName.length() == 0) {
                            Utils.toast("Are you mad ? Enter not empty group name, dude ...");
                            dialog.dismiss();
                        }
                        try {
                            ChatRoom.createChatRoom(
                                    CBHelper.INSTANCE.getDatabase(),
                                    groupName,
                                    CBLMessenger.getInstance().getCurrentUserId()
                            );
                        } catch (CouchbaseLiteException e) {
                            Log.e(TAG, "error creating chat room", e);
                            Utils.toast("error creating chat room");
                        }
                    }
                })
                .show();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent intent = new Intent(getActivity(), ChatActivity.class);
        ChatRoom room = new ChatRoom(mGroupsAdapter.getDocument(position));
        startActivity(ChatActivity.populateIntent(intent, room.getId()));
    }
}
