package com.snacktime.devasia.snacktime.card;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.snacktime.devasia.snacktime.Constants;
import com.snacktime.devasia.snacktime.MainActivity;
import com.snacktime.devasia.snacktime.R;

import org.json.JSONException;
import org.json.JSONObject;

import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by devasia on 12/19/14.
 */
public class FoodCard extends Card {

    JSONObject foodOptions;
    JSONObject drinkOptions;

    public int flavorIdx;
    public int drinkIdx;

    public String flavorName;
    public String drinkName;
    public String ingredients;

    public int cardTag = -1;

    private double price;

    public FoodCard(Context context, JSONObject foodOptions, JSONObject drinkOptions) {
        super(context, R.layout.row_card);

        this.foodOptions = foodOptions;
        this.drinkOptions = drinkOptions;

        flavorIdx = 0;
        drinkIdx = 0;

        try {
            price = Double.parseDouble(foodOptions.getJSONObject(flavorIdx + "")
                    .getString("price"))/100;
            ingredients = foodOptions.getJSONObject(flavorIdx + "")
                    .getString("desc");
            flavorName = foodOptions.getJSONObject("0").getString("cart_name");
            drinkName = drinkOptions.getString("0");
        } catch (JSONException e) {
            e.printStackTrace();
            System.exit(0); // failing to parse the price is a critical fault, we should end the app
        }

        cardTag = Constants.cardTag;
        Constants.cardTag++;
    }

    public double getPrice() {
        return price;
    }

    public void setupInnerViewElements(ViewGroup parent, View view) {

        final TextView idText = (TextView) view.findViewById(R.id.idTag);
        idText.setText(cardTag + "");

        final TextView title = (TextView) view.findViewById(R.id.card_main_inner_simple_title);
        title.setText(flavorName);

        final TextView ingredientText = (TextView) view.findViewById(R.id.ingredientText);
        ingredientText.setText(ingredients);

        ImageView deleteButton = (ImageView) view.findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(deleteHandler);

        // Create listeners for radio buttons
        RadioButton foodButton0 = (RadioButton) view.findViewById(R.id.radio1);
        RadioButton foodButton1 = (RadioButton) view.findViewById(R.id.radio2);
        RadioButton foodButton2 = (RadioButton) view.findViewById(R.id.radio3);
        RadioButton foodButton3 = (RadioButton) view.findViewById(R.id.radio4);

        try {
            foodButton0.setText(foodOptions.getJSONObject("0").getString("name"));
            foodButton1.setText(foodOptions.getJSONObject("1").getString("name"));
            foodButton2.setText(foodOptions.getJSONObject("2").getString("name"));
            foodButton3.setText(foodOptions.getJSONObject("3").getString("name"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RadioButton drinkButton0 = (RadioButton) view.findViewById(R.id.radio5);
        RadioButton drinkButton1 = (RadioButton) view.findViewById(R.id.radio6);
        RadioButton drinkButton2 = (RadioButton) view.findViewById(R.id.radio7);
        RadioButton drinkButton3 = (RadioButton) view.findViewById(R.id.radio8);

        try {
            drinkButton0.setText(drinkOptions.getString("0"));
            drinkButton1.setText(drinkOptions.getString("1"));
            drinkButton2.setText(drinkOptions.getString("2"));
            drinkButton3.setText(drinkOptions.getString("3"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        switch (flavorIdx) {
            case 0:
                foodButton0.setChecked(true);
                break;
            case 1:
                foodButton1.setChecked(true);
                break;
            case 2:
                foodButton2.setChecked(true);
                break;
            case 3:
                foodButton3.setChecked(true);
                break;
        }

        switch (drinkIdx) {
            case 0:
                drinkButton0.setChecked(true);
                break;
            case 1:
                drinkButton1.setChecked(true);
                break;
            case 2:
                drinkButton2.setChecked(true);
                break;
            case 3:
                drinkButton3.setChecked(true);
                break;
        }

        foodButton0.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        try {
                            flavorIdx = 0;
                            flavorName = foodOptions.getJSONObject(flavorIdx + "").getString("cart_name");
                            price = Double.parseDouble(foodOptions.getJSONObject(flavorIdx + "")
                                    .getString("price"))/100;
                            ingredients = foodOptions.getJSONObject(flavorIdx + "")
                                    .getString("desc");
                            Log.d("PhoneMod", foodOptions.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        title.setText(flavorName);
                        ingredientText.setText(ingredients);
                        MainActivity.mCardArrayAdapter.notifyDataSetChanged();
                        MainActivity.broadcastManager.sendBroadcast(new Intent("event").putExtra("priceUpdate", ""));
                    }
                });

        foodButton1.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        try {
                            flavorIdx = 1;
                            flavorName = foodOptions.getJSONObject(flavorIdx + "").getString("cart_name");
                            price = Double.parseDouble(foodOptions.getJSONObject(flavorIdx + "")
                                    .getString("price"))/100;
                            ingredients = foodOptions.getJSONObject(flavorIdx + "")
                                    .getString("desc");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        title.setText(flavorName);
                        ingredientText.setText(ingredients);
                        MainActivity.mCardArrayAdapter.notifyDataSetChanged();
                        MainActivity.broadcastManager.sendBroadcast(new Intent("event").putExtra("priceUpdate", ""));
                    }
                });

        foodButton2.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        try {
                            flavorIdx = 2;
                            flavorName = foodOptions.getJSONObject(flavorIdx + "").getString("cart_name");
                            price = Double.parseDouble(foodOptions.getJSONObject(flavorIdx + "")
                                    .getString("price"))/100;
                            ingredients = foodOptions.getJSONObject(flavorIdx + "")
                                    .getString("desc");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        title.setText(flavorName);
                        ingredientText.setText(ingredients);
                        MainActivity.mCardArrayAdapter.notifyDataSetChanged();
                        MainActivity.broadcastManager.sendBroadcast(new Intent("event").putExtra("priceUpdate", ""));
                    }
                });

        foodButton3.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        try {
                            flavorIdx = 3;
                            flavorName = foodOptions.getJSONObject(flavorIdx + "").getString("cart_name");
                            price = Double.parseDouble(foodOptions.getJSONObject(flavorIdx + "")
                                    .getString("price"))/100;
                            ingredients = foodOptions.getJSONObject(flavorIdx + "")
                                    .getString("desc");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        title.setText(flavorName);
                        ingredientText.setText(ingredients);
                        MainActivity.mCardArrayAdapter.notifyDataSetChanged();
                        MainActivity.broadcastManager.sendBroadcast(new Intent("event").putExtra("priceUpdate", ""));
                    }
                });

        drinkButton0.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        try {
                            drinkIdx = 0;
                            drinkName = drinkOptions.getString(drinkIdx + "");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        title.setText(flavorName);
                        MainActivity.mCardArrayAdapter.notifyDataSetChanged();
                        MainActivity.broadcastManager.sendBroadcast(new Intent("event").putExtra("priceUpdate", ""));
                    }
                });

        drinkButton1.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        try {
                            drinkIdx = 1;
                            drinkName = drinkOptions.getString(drinkIdx + "");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        title.setText(flavorName);
                        MainActivity.mCardArrayAdapter.notifyDataSetChanged();
                        MainActivity.broadcastManager.sendBroadcast(new Intent("event").putExtra("priceUpdate", ""));
                    }
                });

        drinkButton2.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        try {
                            drinkIdx = 2;
                            drinkName = drinkOptions.getString(drinkIdx + "");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        title.setText(flavorName);
                        MainActivity.mCardArrayAdapter.notifyDataSetChanged();
                        MainActivity.broadcastManager.sendBroadcast(new Intent("event").putExtra("priceUpdate", ""));
                    }
                });

        drinkButton3.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        try {
                            drinkIdx = 3;
                            drinkName = drinkOptions.getString(drinkIdx + "");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        title.setText(flavorName);
                        MainActivity.mCardArrayAdapter.notifyDataSetChanged();
                        MainActivity.broadcastManager.sendBroadcast(new Intent("event").putExtra("priceUpdate", ""));
                    }
                });
    }


    private void removeCard() {
        //TODO(devasia): remove based on position
        MainActivity.mCardArrayAdapter.remove(this);
        MainActivity.mCardArrayAdapter.notifyDataSetChanged();
        MainActivity.broadcastManager.sendBroadcast(new Intent("event").putExtra("priceUpdate", ""));
    }

    View.OnClickListener deleteHandler = new View.OnClickListener() {
        public void onClick(View v) {
            Log.d("PhoneMod", "remove has been clicked");
            removeCard();
        }
    };
}