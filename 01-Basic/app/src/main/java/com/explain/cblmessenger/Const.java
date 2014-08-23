package com.explain.cblmessenger;

import java.text.SimpleDateFormat;

/**
 * keeps some global constants
 * Created by bamboo on 23.08.14.
 */
public class Const {
    public static final String[] FACEBOOK_BASE_PERMISSIONS = new String[]{
            "public_profile",
            "email",
            "user_friends"
    };

    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static final SimpleDateFormat SDF = new SimpleDateFormat(DATE_FORMAT);

    public static final String[] EMPTY_STRING_ARRAY = new String[]{};

}
