package com.snacktime.devasia.snacktimedelivery.card;

import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.snacktime.devasia.snacktimedelivery.Constants;
import com.snacktime.devasia.snacktimedelivery.MainActivity;
import com.snacktime.devasia.snacktimedelivery.R;

import org.json.JSONObject;
import org.w3c.dom.Text;

import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by devasia on 12/21/14.
 */
public class RestaurantCard extends Card {

    String restaurantName;
    int restaurantIndex;

    public RestaurantCard(Context context, JSONObject restaurantInfo) throws Exception {

        super(context, R.layout.restaurant_card);

        this.restaurantName = restaurantInfo.getString("name");
        this.restaurantIndex = restaurantInfo.getInt("vendor_id");
    }

    public void setupInnerViewElements(ViewGroup parent, View view) {
        // TODO(devasia): setup image

        // setup text
        TextView text = (TextView) view.findViewById(R.id.textDescription);
        text.setText(restaurantName);

        View layout = (View) view.findViewById(R.id.restaurantLayout);

        layout.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        Constants.restaurantIdx = restaurantIndex;
                        MainActivity.broadcastManager.sendBroadcast(new Intent("event")
                                .putExtra("restaurantClick", ""));
                    }
                });
    }
}
