package com.snacktime.devasia.snacktimedelivery;

/**
 * Created by devasia on 12/22/14.
 */

import android.content.SharedPreferences;

import com.stripe.android.model.Card;

import org.json.JSONObject;

import java.util.ArrayList;

public class Constants {

    /* Stripe Urls */
    public static String stripeApiKeyTest = "pk_test_2oeMglsQk0aDi4myNL6qTepH";
    public static String stripeApiKeyLive = "pk_live_rSxCqBSrrsTeA7IRFTDFthpB";

    /* JSON Urls */
    public static String chargeTestUrl = "https://dry-plains-7644.herokuapp.com/test_charge";
    public static String chargeProdUrl = "https://dry-plains-7644.herokuapp.com/prod_charge";

    public static String restaurantInfoTestUrl = "https://dry-plains-7644.herokuapp.com/feed";
    public static String restaurantInfoProdUrl = "https://dry-plains-7644.herokuapp.com/prod_feed";

    public static String tokenTestUrl = "https://dry-plains-7644.herokuapp.com/test_token";
    public static String tokenProdUrl = "https://dry-plains-7644.herokuapp.com/prod_token";

    /* App specific info */
    public static String token;
    public static String creditCardLast4;

    public static String email;
    public static String phoneNumber;
    public  static double price;

    public static JSONObject serverInfo = null;

    public static int restaurantIdx;

    public static String streetAddress;
    public static String cityAddress;

    public static double lat;
    public static double lon;

    public static SharedPreferences prefs;

    public static int cardTag = 0;
}