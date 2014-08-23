package com.explain.cblmessenger.model;

import com.couchbase.lite.Attachment;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.explain.cblmessenger.common.logger.Log;
import com.explain.cblmessenger.model.utils.CBHelper;
import com.explain.cblmessenger.model.utils.ImageLoaderAsyncTask;
import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User {

    private static final String VIEW_NAME_DEFAULT = "users";
    private static final String VIEW_BY_ID = "users_by_id";
    private static final String DOC_TYPE = "user";
    private static final String TAG = "User";

//    @JsonProperty("user_id")
//    private String mUserId;
//
//    @JsonProperty("user_name")
//    private String mName;
//
//    @JsonProperty("user_email")
//    private String mEmail;
//
//    @JsonProperty("facebook_id")
//    private String mFacebookId;

    private final Document mDocument;

    // map for caching already retrieved users
    private static final Map<String, User> mUserById = new HashMap<>();

    public User(Document document) {
        Preconditions.checkNotNull(document, "doc is null");
        this.mDocument = document;
    }

    public static Query getQuery(Database db) {
        // good practice is to name views like "database_name/view_name"
        com.couchbase.lite.View cbView = db.getView(String.format("%s/%s", db.getName(), VIEW_NAME_DEFAULT));
        if (cbView.getMap() == null) {
            Mapper mapper = new Mapper() {
                @Override
                public void map(Map<String, Object> properties, Emitter emitter) {
                    if (DOC_TYPE.equals(properties.get("type"))) {
                        emitter.emit(properties.get("user_name"), properties);
                    }
                }
            };
            cbView.setMap(mapper, CBHelper.VIEW_VERSION);
        }
        return cbView.createQuery();
    }

    public static Query getQueryById(Database db, final String userId) {
        // good practice is to name views like "database_name/view_name"
        com.couchbase.lite.View cbView = db.getView(String.format("%s/%s", db.getName(), VIEW_BY_ID));
        if (cbView.getMap() == null) {
            cbView.setMap(new Mapper() {
                @Override
                public void map(Map<String, Object> properties, Emitter emitter) {
                    if (DOC_TYPE.equals(properties.get("type"))) {
                        emitter.emit(properties.get("user_id"), properties);
                        emitter.emit(properties.get("facebook_id"), properties);
                    }
                }
            }, CBHelper.VIEW_VERSION);
        }

        Query query = cbView.createQuery();
        List<Object> keys = new ArrayList<>();
        keys.add(userId);
        query.setKeys(keys);
        return query;
    }

    public static User getUserById(Database db, final String userId) {
        // check for cached users
        if (mUserById.get(userId) != null) {
            return mUserById.get(userId);
        }
        QueryEnumerator enumerator = null;
        try {
            enumerator = getQueryById(db, userId).run();
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, "error getting user with id : " + userId, e);
        }
        if (enumerator != null && enumerator.getCount() > 0) {
            User user = new User(enumerator.getRow(0).getDocument());
            mUserById.put(userId, user);
            return user;
        }
        return null;
    }

    /**
     * Sync Gateway does the all magic with authorization through user email in facebook
     * it seems to be its name, and the property you pass to access("user@email aka name", channels) requireUser("user@email")
     */
    public static User createUser(Database db, String facebookId, String userName, String userEmail)
            throws CouchbaseLiteException {
        Map<String, Object> properties = new HashMap<>();

        properties.put("type", DOC_TYPE);
        properties.put("user_email", userEmail);
        properties.put("user_name", userName);
        properties.put("user_id", userEmail);
        properties.put("facebook_id", facebookId);

        Document doc = db.getDocument("user:" + userEmail);
        doc.putProperties(properties);

        // load user photo asynchronously
        new ImageLoaderAsyncTask().execute("https://graph.facebook.com/" + facebookId + "/picture?type=large", doc);

        return new User(doc);
    }

    public String getUserId() {
        return (String) mDocument.getProperties().get("user_id");
    }

    public String getName() {
        return (String) mDocument.getProperties().get("user_name");
    }

    public String getEmail() {
        return (String) mDocument.getProperties().get("user_email");
    }

    public String getFacebookId() {
        return (String) mDocument.getProperties().get("facebook_id");
    }

    public Attachment getAttachment(String name) {
        return mDocument.getCurrentRevision().getAttachment(name);
    }
}
