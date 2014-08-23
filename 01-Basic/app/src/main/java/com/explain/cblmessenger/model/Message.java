package com.explain.cblmessenger.model;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.explain.cblmessenger.CBLMessenger;
import com.explain.cblmessenger.model.utils.CBHelper;
import com.explain.cblmessenger.utils.Utils;
import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Message {

    public static final String DOC_TYPE = "message";

    private static final String VIEW_NAME_DEFAULT = "messages";

//    @JsonProperty("chat_room_id")
//    private String mChatRoomId;
//
//    @JsonProperty("owner_id")
//    private String mOwnerId;
//
//    @JsonProperty("text")
//    private String mText;
//
//    @JsonProperty("created_at")
//    private String mCreatedAt;

    private final Document mDocument;

    public Message(Document document) {
        Preconditions.checkNotNull(document, "doc is null");
        this.mDocument = document;
    }

    public static Query getQuery(Database db, String chatRoomId) {
        // good practice is to name views like "database_name/view_name"
        com.couchbase.lite.View cbView = db.getView(String.format("%s/%s", db.getName(), VIEW_NAME_DEFAULT));

        // instead of creating separate view for each ChatRoom, it is better to create one that emits
        // docs with keys of particular ChatRoom, and
        if (cbView.getMap() == null) {
            Mapper mapper = new Mapper() {
                @Override
                public void map(Map<String, Object> properties, Emitter emitter) {
                    String type = (String) properties.get("type");
                    if (DOC_TYPE.equals(type)) {
                        // use compound keys to sort messages first by ChatRoomId and then by time created at
                        List<Object> keys = new ArrayList<>();
                        keys.add(properties.get("chat_room_id"));
                        keys.add(properties.get("created_at"));
                        emitter.emit(keys, properties);
                    }
                }
            };
            cbView.setMap(mapper, CBHelper.VIEW_VERSION);
        }

        Query query = cbView.createQuery();
        // set descending false so that newer messages (with greater date) are first (list view is displayed from the button)
        // we get messages only of the current room, so chat_room_id will be the same, hence, docs will be sorted by key created at
        query.setDescending(false);

        // kind of hack with adding HashMap to the keys, maps are treated as {}, and are last in the sort order
        // so if you want all docs in ascending order with keys starting from "abc" and anything that goes after that,
        // put start list of keys like ("abc") and end list of keys like : ("abc", {})
        // in descending switch start and end keys
        List<Object> endKeys = new ArrayList<>();
        endKeys.add(chatRoomId);
        endKeys.add(new HashMap<String, Object>());
        query.setEndKey(endKeys);

        List<Object> startKeys = new ArrayList<>();
        startKeys.add(chatRoomId);
        query.setStartKey(startKeys);

        return query;
    }

    public static Message createMessage(Database db, String chatRoomId, String text) throws CouchbaseLiteException {
        Map<String, Object> properties = new HashMap<>();

        properties.put("type", DOC_TYPE);
        properties.put("created_at", Utils.getGMTDateTimeAsString());
        properties.put("text", text);
        properties.put("chat_room_id", chatRoomId);
        properties.put("owner_id", CBLMessenger.getInstance().getCurrentUserId());

        Document doc = db.createDocument();
        doc.putProperties(properties);

        return new Message(doc);
    }

    public String getText() {
        return (String) mDocument.getProperties().get("text");
    }

    public String getCreatedAt() {
        return (String) mDocument.getProperties().get("created_at");
    }

    public String getChatRoomId() {
        return (String) mDocument.getProperties().get("chat_room_id");
    }

    public String getOwnerId() {
        return (String) mDocument.getProperties().get("owner_id");
    }

    public boolean isCurrentUserOwner() {
        return getOwnerId().equals(CBLMessenger.getInstance().getCurrentUserId());
    }

}
