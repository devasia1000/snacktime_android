package com.snacktime.devasia.snacktimedelivery.network;

import android.content.Intent;
import android.util.Log;

import com.snacktime.devasia.snacktimedelivery.MainActivity;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.protocol.HTTP;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by devasia on 12/31/14.
 */
public class OrderSender implements Runnable {

    String json;

    public OrderSender(String json) {
        this.json = json;
    }

    public void run() {
        // Fetch restaurant data from server
        DefaultHttpClient httpclient = new DefaultHttpClient(new BasicHttpParams());
        HttpPost httppost = new HttpPost("https://dry-plains-7644.herokuapp.com/test_charge");
        httppost.setHeader("Content-type", "application/json");

        InputStream inputStream = null;
        String result;

        try {

            Log.d("PhoneMod", this.json);

            httppost.setEntity(new StringEntity(json, HTTP.UTF_8));

            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();

            inputStream = entity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
            StringBuilder sb = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            result = sb.toString();

            MainActivity.broadcastManager.sendBroadcast(new Intent("event")
                    .putExtra("orderUpdate", Integer.parseInt(result.trim())));

            Log.d("PhoneMod", "\n\n" + result + "\n\n");
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
