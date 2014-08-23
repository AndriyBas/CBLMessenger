package com.explain.cblmessenger.model;

import com.couchbase.lite.Attachment;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.Status;
import com.explain.cblmessenger.CBLMessenger;
import com.explain.cblmessenger.common.logger.Log;
import com.explain.cblmessenger.model.utils.CBHelper;
import com.explain.cblmessenger.utils.Utils;
import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatRoom {

    public static final String DOC_TYPE = "chat_room";

    private static final String VIEW_NAME_DEFAULT = "chat_room";
    private static final String TAG = "ChatRoom";

//    @JsonProperty("name")
//    private String mName;
//
//    @JsonProperty("owner_id")
//    private String mOwnerId;
//
//    @JsonProperty("members")
//    @JsonDeserialize(as = ArrayList.class)
//    private List<String> mMembers;
//
//    @JsonProperty("created_at")
//    private String mCreatedAt;

    private final Document mDocument;

    public void delete() {
        try {
            mDocument.delete();
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, "Error deleting doc", e);
        }
    }

    public Attachment getAttachment(String attachmentName) {
        return mDocument.getCurrentRevision().getAttachment(attachmentName);
    }

    public ChatRoom(Document document) {
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
                        emitter.emit(properties.get("_id"), properties);
                    }
                }
            };
            cbView.setMap(mapper, CBHelper.VIEW_VERSION);
        }
        return cbView.createQuery();
    }

    public static Query getQueryById(Database db, String chatRoomId) {
        Query query = getQuery(db);
        List<Object> keys = new ArrayList<>();
        keys.add(chatRoomId);
        query.setKeys(keys);
        return query;
    }


    public static ChatRoom getChatRoomById(Database db, String chatRoomId) {
        QueryEnumerator enumerator = null;

        try {
            enumerator = getQueryById(db, chatRoomId).run();
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, "error getting chat_room (" + chatRoomId + ") from database", e);
            Utils.toast("error getting chat_room (" + chatRoomId + ") from database");
        }

        if (enumerator != null && enumerator.getCount() > 0) {
            return new ChatRoom(enumerator.getRow(0).getDocument());
        }

        return null;
    }

    public static ChatRoom createChatRoom(Database db, String name, String ownerId, String... members)
            throws CouchbaseLiteException {
        Map<String, Object> properties = new HashMap<>();

        properties.put("type", DOC_TYPE);
        properties.put("name", name);
        properties.put("owner_id", ownerId);
        properties.put("created_at", Utils.getGMTDateTimeAsString());
        ArrayList<String> newMembers = new ArrayList<>();
        if (members != null)
            for (String m : members) {
                newMembers.add(m);
            }
        if (!newMembers.contains(ownerId)) {
            newMembers.add(ownerId);
        }
        properties.put("members", newMembers);

        Document doc = db.createDocument();
        doc.putProperties(properties);
        return new ChatRoom(doc);
    }

    /**
     * LOOK AT THIS FUNCTION, IT'S UGLY
     *
     * @param memberIds
     */
    public void setNewMembers(List<String> memberIds) {
        if (memberIds == null) {
            return;
        }
        List<String> membersCopy = new ArrayList<>(memberIds.size());
        membersCopy.addAll(memberIds);

        if (!membersCopy.contains(CBLMessenger.getInstance().getCurrentUserId())) {
            membersCopy.add(CBLMessenger.getInstance().getCurrentUserId());
        }

        // LOOK AT IT ====>>>
        // IT'S HORRIBLE
        boolean done = false;
        do {
            Map<String, Object> properties = new HashMap<>();
            properties.putAll(mDocument.getProperties());
            properties.put("members", membersCopy);
            try {
                mDocument.putProperties(properties);
                done = true;
            } catch (CouchbaseLiteException e) {
                if (e.getCBLStatus().getCode() == Status.CONFLICT) {
                    // race conflict, keep trying
                } else {
                    Log.e(TAG, "oops, smt bad happened", e);
                    done = true;
                }
            }
        } while (!done);
    }

    public String getName() {
        return (String) mDocument.getProperties().get("name");
    }

    public String getOwnerId() {
        return (String) mDocument.getProperties().get("owner_id");
    }

    /**
     * return list of member ids
     * NOTE : method is protected and returns copy of internal structure, so changing the list will
     * not change the internals of the class
     *
     * @return list of member ids
     */
    public ArrayList<String> getMembers() {
        // copy member list so that users cannot change members
        ArrayList<String> members = (ArrayList<String>) mDocument.getProperties().get("members");
        return new ArrayList<>(members);
    }

    public String getCreatedAt() {
        return (String) mDocument.getProperties().get("created_at");
    }

    public String getId() {
        return mDocument.getId();
    }
}
