package com.explain.cblmessenger.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import com.explain.cblmessenger.CBLMessenger;
import com.explain.cblmessenger.Const;
import com.explain.cblmessenger.common.logger.Log;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.text.ParseException;
import java.util.Date;

/**
 * has some global utility functions
 * Created by bamboo on 23.08.14.
 */
public class Utils {
    /**
     * Check if the network is connected
     *
     * @param c Context of the caller
     * @return true - network is connected, false - else
     */
    public static boolean isNetworkConnected(Context c) {

        ConnectivityManager connectivityManager = (ConnectivityManager)
                c.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();

        if (info != null && info.isConnected()) {
            return true;
        }

        return false;
    }

    /**
     * @return current GMT/UTC time as java Date Object
     */
    public static Date getGMTDateTimeAsDate() {
        // use joda time to easily get global time
        return new DateTime(DateTimeZone.UTC).toDate();
    }

    /**
     * @return current GMT/UTC time in format [yyyy-MM-dd HH:mm:ss]
     */
    public static String getGMTDateTimeAsString() {

        // get global time
        Date date = getGMTDateTimeAsDate();
        // format it
        final String gmtTime = Const.SDF.format(date);
        Log.d("time", gmtTime);

        return gmtTime;
    }

    public static Date stringDateToDate(String strDate) {
        Date dateToReturn = null;
        try {
            dateToReturn = Const.SDF.parse(strDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return dateToReturn;
    }

    /**
     * @param message
     */
    public static void toast(String message) {
        Toast.makeText(CBLMessenger.getAppContext(), message, Toast.LENGTH_SHORT)
                .show();
    }
}
