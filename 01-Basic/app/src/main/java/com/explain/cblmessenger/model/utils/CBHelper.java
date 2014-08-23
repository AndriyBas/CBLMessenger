package com.explain.cblmessenger.model.utils;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.auth.Authenticator;
import com.couchbase.lite.auth.AuthenticatorFactory;
import com.couchbase.lite.replicator.Replication;
import com.explain.cblmessenger.CBLMessenger;
import com.explain.cblmessenger.common.logger.Log;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Class designed as best practices of implementing singleton
 * without any Dependency Injection
 * Created by bamboo on 08.20.14.
 */
public enum CBHelper {

    // single instance of this enum-singleton
    INSTANCE;

    private static final String TAG = "CBHelper";

    private Manager manager;

    // name of our database
    private final String CBL_MESSENGER_DB_NAME = "db_cbl_messenger";

    private Database mDatabase = null;

    // URL where our Sync Gateway runs
    // if you run it on emulator and Sync Gateway is on the same computer,
    // put "http://10.0.2.2:4985" to connect to localhost
    private final String BASE_URL = "YOUR_SYNC_GATEWAY_IP";

    private final String CBL_MESSENGER_SYNC_URL = BASE_URL + CBL_MESSENGER_DB_NAME;

    public static String VIEW_VERSION = "0.1";

    private CBHelper() {
        try {

            // initialize Manager
            manager = new Manager(

                    new AndroidContext(CBLMessenger.getAppContext()),

                    Manager.DEFAULT_OPTIONS);

            // and database
            mDatabase = manager.getDatabase(CBL_MESSENGER_DB_NAME);
        } catch (IOException e) {
            Log.e(TAG, "Error initializing manager");
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, "Error opening " + CBL_MESSENGER_DB_NAME + " database");
        }
    }

    /**
     * method log in user with given accessToken
     * and connects to SyncGateway, located at CBL_MESSENGER_SYNC_URL
     * starts both Pull and Push Replications
     *
     * @param accessToken
     */
    public void startReplicationWithFacebookAccessToken(String accessToken) {

        Authenticator authenticator = AuthenticatorFactory.createFacebookAuthenticator(accessToken);

        Replication[] messageReplication = createReplication(getDatabase(), CBL_MESSENGER_SYNC_URL);
        startReplication(messageReplication[0], authenticator);
        startReplication(messageReplication[1], authenticator);
    }

    private void startReplication(Replication replication, Authenticator authenticator) {
        replication.setAuthenticator(authenticator);
        replication.start();
    }

    /**
     * connect to Sync Gateway at urlString
     * and create list of Pull[0] and Push[1] Replication
     *
     * @param db
     * @param urlString location of Sync Gateway
     * @return list [pullReplication, pushReplication]
     */
    private Replication[] createReplication(Database db, String urlString) {
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            Log.e(TAG, "error in creating url : " + urlString);
            throw new RuntimeException("invalid URL");
        }

        Replication pullReplication = db.createPullReplication(url);
        pullReplication.setContinuous(true);

        Replication pushReplication = db.createPushReplication(url);
        pushReplication.setContinuous(true);
        return new Replication[]
                {pullReplication, pushReplication};
    }

    public Database getDatabase() {
        return mDatabase;
    }

    public Manager getManager() {
        return manager;
    }
}
