package com.explain.cblmessenger.ui.adapter;

import android.app.Activity;
import android.content.Context;
import android.widget.BaseAdapter;

import com.couchbase.lite.Document;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.QueryEnumerator;

/**
 * adapter takes live query and automatically updates its content when
 * query is changed
 */
public abstract class LiveQueryAdapter extends BaseAdapter {
    private LiveQuery mQuery;
    private QueryEnumerator mEnumerator;
    private Context mContext;

    public LiveQueryAdapter(Context context, LiveQuery query) {
        this.mContext = context;
        this.mQuery = query;

        query.addChangeListener(new LiveQuery.ChangeListener() {
            @Override
            public void changed(final LiveQuery.ChangeEvent event) {
                ((Activity) LiveQueryAdapter.this.mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mEnumerator = event.getRows();
                        notifyDataSetChanged();
                    }
                });
            }
        });

        query.start();
    }

    @Override
    public int getCount() {
        return mEnumerator != null ? mEnumerator.getCount() : 0;
    }

    @Override
    public Object getItem(int i) {
        return mEnumerator != null ? mEnumerator.getRow(i).getDocument() : null;
    }

    public Document getDocument(int position) {
        return (Document) getItem(position);
    }

    @Override
    public long getItemId(int i) {
        return mEnumerator.getRow(i).getSequenceNumber();
    }


    public void invalidate() {
        if (mQuery != null)
            mQuery.stop();
    }

    protected Context getContext() {
        return mContext;
    }
}
