package com.explain.cblmessenger.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.couchbase.lite.LiveQuery;
import com.explain.cblmessenger.CBLMessenger;
import com.explain.cblmessenger.R;
import com.explain.cblmessenger.model.ChatRoom;

/**
 * Created by bamboo on 8/20/14.
 */
public class GroupsAdapter extends LiveQueryAdapter {

    private String mCurrentUserId;

    public GroupsAdapter(Context context, LiveQuery query) {
        super(context, query);
        mCurrentUserId = CBLMessenger.getInstance().getCurrentUserId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.frag_gf_list_item_group, null);
            viewHolder = new ViewHolder();
            viewHolder.mGroupName = (TextView) convertView.findViewById(R.id.frag_gf_group_name);
            viewHolder.mGroupPhoto = (ImageView) convertView.findViewById(R.id.frag_gf_group_image);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        ChatRoom chatRoom = new ChatRoom(getDocument(position));

        TextView text = viewHolder.mGroupName;
        text.setText(chatRoom.getName());

        /*ImageView imageView = viewHolder.mGroupPhoto;
        Attachment attachment = chatRoom.getAttachment("image");
        if (attachment != null) {
            Bitmap bitmap = null;
            Bitmap thumbnail = null;
            try {
                bitmap = BitmapFactory.decodeStream(attachment.getContent());
                int requiredSize = getContext().getResources().getDimensionPixelSize(R.dimen.friend_logo_size);
                thumbnail = ThumbnailUtils.extractThumbnail(bitmap, requiredSize, requiredSize);
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }
            if (thumbnail != null) {
                imageView.setImageBitmap(thumbnail);
            } else {
                imageView.setImageResource(android.R.drawable.ic_dialog_map);
            }
        }*/
        return convertView;
    }

    private static class ViewHolder {
        TextView mGroupName;
        ImageView mGroupPhoto;
    }
}
