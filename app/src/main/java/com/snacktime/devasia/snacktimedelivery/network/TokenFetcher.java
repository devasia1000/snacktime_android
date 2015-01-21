package com.snacktime.devasia.snacktimedelivery.network;

import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.snacktime.devasia.snacktimedelivery.Constants;
import com.snacktime.devasia.snacktimedelivery.MainActivity;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by devasia on 12/31/14.
 */
public class TokenFetcher implements Runnable {

    String stripeToken;
    String email;

    public TokenFetcher(String stripeToken, String email) {
        this.stripeToken = stripeToken;
        this.email = email;
    }

    @Override
    public void run() {
        // Fetch restaurant data from server
        DefaultHttpClient httpclient = new DefaultHttpClient(new BasicHttpParams());
        HttpPost httppost = new HttpPost(Constants.tokenProdUrl);
        httppost.setHeader("Content-type", "application/x-www-form-urlencoded");

        InputStream inputStream = null;

        try {
            httppost.setEntity(new StringEntity("stripeToken=" + stripeToken + "&email=" + email));
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();

            inputStream = entity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
            StringBuilder sb = new StringBuilder();

            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }

            Constants.token = sb.toString().replace("\n", "").trim();

            if (Constants.token.length() < 500) {
                SharedPreferences.Editor editor = Constants.prefs.edit();
                editor.putString("token", Constants.token);
                editor.commit();
            } else {
                SharedPreferences.Editor editor = Constants.prefs.edit();
                editor.remove("token");
                editor.remove("last4");
                editor.commit();
            }

            MainActivity.broadcastManager.sendBroadcast(new Intent("event")
                    .putExtra("paymentUpdate", ""));

        } catch (Exception e) {
            Log.d("PhoneMod", e.toString());
        } finally {
            try {
                if (inputStream != null) inputStream.close();
            } catch (Exception e) {
                Log.d("PhoneMod", e.toString());
            }
        }
    }
}
