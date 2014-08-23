package com.explain.cblmessenger.model.utils;

import android.os.AsyncTask;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.couchbase.lite.UnsavedRevision;
import com.explain.cblmessenger.common.logger.Log;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * AsyncTask accepts two params : URL String and com.couchbase.lite.Document
 * it loads image from URL and saves it as attachment "image"
 * in the Document
 */
public class ImageLoaderAsyncTask extends AsyncTask<Object, Void, Void> {

    private static final String TAG = "ImageLoaderAsyncTask";

    @Override
    protected Void doInBackground(Object... params) {
        String stringUrl = (String) params[0];
        URL url = null;
        try {
            url = new URL(stringUrl);
        } catch (MalformedURLException e) {
            Log.e(TAG, "bad url : [" + url + "]", e);
        }
        Document doc = (Document) params[1];

        boolean done = false;
        do {
            UnsavedRevision revision = doc.createRevision();
            revision.setAttachment("image", "image/jpg", url);
            try {
                revision.save();
                done = true;
            } catch (CouchbaseLiteException e) {
                if (e.getCBLStatus().getCode() == com.couchbase.lite.Status.CONFLICT) {
                    // race conflict, keep trying
                } else {
                    Log.e(TAG, "oops, smt bad happened", e);
                    done = true;
                }
            }
        } while (!done);

        return null;
    }
}