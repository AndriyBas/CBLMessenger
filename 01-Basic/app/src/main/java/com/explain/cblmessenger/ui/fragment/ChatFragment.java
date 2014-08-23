package com.explain.cblmessenger.ui.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.couchbase.lite.CouchbaseLiteException;
import com.explain.cblmessenger.CBLMessenger;
import com.explain.cblmessenger.R;
import com.explain.cblmessenger.common.logger.Log;
import com.explain.cblmessenger.model.ChatRoom;
import com.explain.cblmessenger.model.Message;
import com.explain.cblmessenger.model.User;
import com.explain.cblmessenger.model.utils.CBHelper;
import com.explain.cblmessenger.ui.activity.FriendPickerActivity;
import com.explain.cblmessenger.ui.adapter.MessagesAdapter;
import com.explain.cblmessenger.utils.Utils;
import com.facebook.model.GraphUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple {@link android.app.Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ChatFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ChatFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChatFragment extends Fragment {

    private static final String ARG_CHAT_ROOM_ID = "ChatFragment.arg_chat_room_id";

    private static final int MANAGE_FRIENDS_REQUEST_CODE = 2190;
    private static final String TAG = "ChatFragment";

    private String mChatRoomId;
    private String mCurrentUserId;

    private ChatRoom mChatRoom;

    private Map<String, User> mMembersById;

    private String mDisplayName;

    private ListView mChatListView;
    private EditText mChatEditText;
    private Button mChatButton;

    private OnFragmentInteractionListener mListener;

    public static ChatFragment newInstance(String param1) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CHAT_ROOM_ID, param1);
        fragment.setArguments(args);
        return fragment;
    }

    public ChatFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        if (getArguments() != null) {
            mChatRoomId = getArguments().getString(ARG_CHAT_ROOM_ID);
        }

        if (mChatRoomId == null) {
            throw new IllegalStateException("WTF, chat room id is null");
        }

        mCurrentUserId = CBLMessenger.getInstance().getCurrentUserId();

        mChatRoom = ChatRoom.getChatRoomById(CBHelper.INSTANCE.getDatabase(), mChatRoomId);
        mMembersById = new HashMap<>();
        ArrayList<String> members = mChatRoom.getMembers();
        for (String userId : members) {
            mMembersById.put(userId, User.getUserById(CBHelper.INSTANCE.getDatabase(), userId));
        }

        mDisplayName = mChatRoom.getName();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().onBackPressed();
                return true;
            case R.id.act_ca_action_manage_members:
                manageMembersAction();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void manageMembersAction() {
        Intent i = new Intent(getActivity(), FriendPickerActivity.class);
        if (mChatRoom != null) {
            List<String> membersFacebookIds = new ArrayList<>();
            for (String s : mChatRoom.getMembers()) {
                User u = User.getUserById(CBHelper.INSTANCE.getDatabase(), s);
                if (u != null) {
                    membersFacebookIds.add(u.getFacebookId());
                }
            }
            i = FriendPickerActivity.populateIntentWithSelectionIds(i, membersFacebookIds);
        }
        startActivityForResult(i, MANAGE_FRIENDS_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == MANAGE_FRIENDS_REQUEST_CODE) {
                List<GraphUser> selectedUsers = CBLMessenger.getInstance().getSelectedUsers();
                if (selectedUsers != null) {
                    List<String> memberIds = new ArrayList<>();
                    memberIds.add(mCurrentUserId);
                    for (GraphUser graphUser : selectedUsers) {
                        // get user by facebook id
                        User user = User.getUserById(CBHelper.INSTANCE.getDatabase(), graphUser.getId());
                        memberIds.add(user.getUserId());
                    }

                    mChatRoom.setNewMembers(memberIds);
                }
            }
        }

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mChatRoom != null && mChatRoom.getOwnerId().equals(mCurrentUserId)) {
            inflater.inflate(R.menu.chat_room_menu, menu);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_cf_layout, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mChatButton = (Button) view.findViewById(R.id.btn_chat_send);

        mChatEditText = (EditText) view.findViewById(R.id.chat_edit_text);

        mChatListView = (ListView) view.findViewById(R.id.chat_list_view);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mChatListView.setAdapter(new MessagesAdapter(getActivity(),
                Message.getQuery(CBHelper.INSTANCE.getDatabase(), mChatRoomId).toLiveQuery()));
        mChatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });


        getActivity().getActionBar().setTitle(mDisplayName);
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
        getActivity().getActionBar().setIcon(new ColorDrawable(android.R.color.transparent));
    }

    private void sendMessage() {
        String text = mChatEditText.getText().toString().trim();
        if (text.trim().length() == 0) {
            return;
        }
        mChatEditText.setText("");

        try {
            Message.createMessage(
                    CBHelper.INSTANCE.getDatabase(),
                    mChatRoomId,
                    text
            );
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, "error creating message", e);
            Utils.toast("error sending message");
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        public void onFragmentInteraction(String param);
    }
}
