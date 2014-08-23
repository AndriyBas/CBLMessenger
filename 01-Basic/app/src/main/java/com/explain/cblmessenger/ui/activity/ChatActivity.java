package com.explain.cblmessenger.ui.activity;

import android.app.Fragment;
import android.content.Intent;

import com.explain.cblmessenger.ui.fragment.ChatFragment;
import com.explain.cblmessenger.utils.SingleFragmentActivity;

/**
 * Created by bamboo on 7/21/14.
 */
public class ChatActivity extends SingleFragmentActivity
        implements ChatFragment.OnFragmentInteractionListener {

    private static final String CHAT_ROOM_ID_EXTRA = "ChatActivity.extra_chat_room_id";

    public static Intent populateIntent(Intent i, String chatRoomId) {
        if (i == null)
            return null;
        i.putExtra(CHAT_ROOM_ID_EXTRA, chatRoomId);
        return i;
    }

    @Override
    public Fragment createFragment() {
        return ChatFragment.newInstance(getIntent().getStringExtra(CHAT_ROOM_ID_EXTRA));
    }

    @Override
    public void onFragmentInteraction(String param) {

    }
}
