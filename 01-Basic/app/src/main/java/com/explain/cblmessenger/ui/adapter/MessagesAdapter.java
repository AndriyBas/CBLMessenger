package com.explain.cblmessenger.ui.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.couchbase.lite.Attachment;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.LiveQuery;
import com.explain.cblmessenger.R;
import com.explain.cblmessenger.model.Message;
import com.explain.cblmessenger.model.User;
import com.explain.cblmessenger.model.utils.CBHelper;

/**
 * adapter can be populated with 2 kinds of views :
 * User own messages - TYPE_RIGHT_MESSAGE
 * Messages created by other users - TYPE_LEFT_MESSAGE
 * <p/>
 * <sarcasm>No, it's style is not copy pasted from Bookface Messenger</sarcasm>
 * Created by bamboo on 23.07.14.
 */
public class MessagesAdapter extends LiveQueryAdapter {


    private static final int TOTAL_VIEW_TYPES = 2;
    private static final int TYPE_LEFT_MESSAGE = 0;
    private static final int TYPE_RIGHT_MESSAGE = TYPE_LEFT_MESSAGE + 1;

    private LruCache<String, Bitmap> mUserLogoThumbnailCache;

    public MessagesAdapter(Context context, LiveQuery query) {
        super(context, query);
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 10;
        mUserLogoThumbnailCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    private Bitmap addBitmapToMemoryCacheFromAttachment(String key, Attachment attachment) {
        Bitmap thumbnail = null;
        if (attachment != null) {
            try {
                Bitmap b = BitmapFactory.decodeStream(attachment.getContent());
                int requiredSize = getContext().getResources().getDimensionPixelSize(R.dimen.message_logo_size);
                thumbnail = ThumbnailUtils.extractThumbnail(b, requiredSize, requiredSize);
                mUserLogoThumbnailCache.put(key, thumbnail);
            } catch (CouchbaseLiteException e) {
                e.printStackTrace();
            }
        }

        return thumbnail;
    }


    private Bitmap getBitmapFromMemoryCache(String key) {
        return mUserLogoThumbnailCache.get(key);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Message message = new Message(getDocument(position));

        ViewHolder viewHolder = null;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            if (message.isCurrentUserOwner()) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.frag_cf_message_right, null);
                viewHolder.mMessageText = (TextView) convertView.findViewById(R.id.frag_cf_message_text_right);
            } else {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.frag_cf_message_left, null);
                viewHolder.mMessageText = (TextView) convertView.findViewById(R.id.frag_cf_message_text_left);
                viewHolder.mOwnerImage = (ImageView) convertView.findViewById(R.id.frag_cf_message_sender_image);
            }
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        TextView tv = viewHolder.mMessageText;

        tv.setText(message.getText());
        if (!message.isCurrentUserOwner()) {
            ImageView imageView = viewHolder.mOwnerImage;
            User sender = User.getUserById(CBHelper.INSTANCE.getDatabase(), message.getOwnerId());

            Bitmap thumbnail = getBitmapFromMemoryCache(sender.getUserId());
            if (thumbnail == null) {
                thumbnail = addBitmapToMemoryCacheFromAttachment(sender.getUserId(), sender.getAttachment("image"));
            }

            if (thumbnail != null) {
                imageView.setImageBitmap(thumbnail);
            } else {
                imageView.setImageResource(android.R.drawable.ic_menu_my_calendar);
            }
        }
        return convertView;
    }

    @Override
    public int getViewTypeCount() {
        // indicate TOTAL_VIEW_TYPES different types of views for ListView items
        return TOTAL_VIEW_TYPES;
    }

    @Override
    public int getItemViewType(int position) {
        return new Message(getDocument(position)).isCurrentUserOwner()
                ? TYPE_RIGHT_MESSAGE : TYPE_LEFT_MESSAGE;
    }

    private static class ViewHolder {
        TextView mMessageText;
        ImageView mOwnerImage;
    }
}
