package com.snacktime.devasia.snacktimedelivery;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.snacktime.devasia.snacktimedelivery.card.CardListActivity;
import com.snacktime.devasia.snacktimedelivery.card.FoodCard;
import com.snacktime.devasia.snacktimedelivery.card.RestaurantCard;
import com.snacktime.devasia.snacktimedelivery.dialog.PaymentEditDialog;
import com.snacktime.devasia.snacktimedelivery.map.MarkerDragManager;
import com.snacktime.devasia.snacktimedelivery.network.RestaurantInfoFetcher;
import com.snacktime.devasia.snacktimedelivery.util.Geocoding;
import com.snacktime.devasia.snacktimedelivery.util.ReverseGeocoding;
import com.snacktime.devasia.snacktimedelivery.network.OrderSender;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardArrayAdapter;
import it.gmariotti.cardslib.library.internal.CardGridArrayAdapter;
import it.gmariotti.cardslib.library.view.CardGridView;
import it.gmariotti.cardslib.library.view.CardListView;


public class MainActivity extends Activity implements OnMapReadyCallback {

    public enum ViewTracker {
        APP_TRACKER, // Tracker used only in this app.
    }
    private HashMap<ViewTracker, Tracker> mTrackers = new HashMap<>();

    private Marker pinMarker;
    private GoogleMap map;

    public static CardArrayAdapter mCardArrayAdapter;
    public static LocalBroadcastManager broadcastManager;

    private ViewFlipper flipper;
    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d("PhoneMod", "started app\n");
        this.setTitle("Snacktime Delivery");

        Tracker t =  getTracker(ViewTracker.APP_TRACKER);
        t.setScreenName("AddressSelectionScreenWithGoogleMap");
        t.send(new HitBuilders.AppViewBuilder().build());

        broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(mMessageReceiver, new IntentFilter("event"));

        // Init shared prefs
        Constants.prefs = getSharedPreferences("com.devasia.snacktime.snacktimedelivery",
                Context.MODE_PRIVATE);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        flipper = (ViewFlipper) findViewById(R.id.viewflipper);

        flipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_in));
        flipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_out));

        initCreditCard();
        addShortcutToHomescreen();

        Constants.phoneNumber = getPhoneNumber();
        Constants.email = getEmail();

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getStringExtra("priceUpdate") != null) {

                Constants.price = (double) mCardArrayAdapter.getCount() * 9.95;
                BigDecimal roundedPrice = new BigDecimal(Constants.price)
                        .setScale(2, BigDecimal.ROUND_HALF_UP);
                TextView priceText = (TextView) findViewById(R.id.priceText);
                priceText.setText("Total: $" + roundedPrice.toString());
                TextView paymentText = (TextView) findViewById(R.id.paymentText);
                paymentText.setText("Total Cost: $" + roundedPrice.toString() +
                        "\nCredit Card: ****-****-****-" + Constants.creditCardLast4);
                mCardArrayAdapter.notifyDataSetChanged();

                Button checkoutButton = (Button) findViewById(R.id.checkoutButton);
                checkoutButton.setEnabled(Constants.price > 0);

                Button continueButton = (Button) findViewById(R.id.continueButton);
                continueButton.setEnabled(canSafelyContinue());

            } else if (intent.getStringExtra("addressUpdate") != null) {

                final TextView address1 = (TextView) findViewById(R.id.addressInputStreet);
                final TextView address2 = (TextView) findViewById(R.id.addressInputCity);

                address1.setText(Constants.streetAddress);
                address2.setText(Constants.cityAddress);

            } else if (intent.getDoubleArrayExtra("pinUpdate") != null) {

                double[] location = intent.getDoubleArrayExtra("pinUpdate");
                final LatLng newLoc = new LatLng(location[0], location[1]);
                pinMarker.setPosition(newLoc);
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(newLoc, 17));

                Constants.lat = location[0];
                Constants.lon = location[1];

            } else if (intent.getStringExtra("restaurantInfoUpdate") != null) {

                showRestaurantSelection();

            } else if (intent.getStringExtra("restaurantClick") != null) {

                showFoodSelection();

            } else if (intent.getStringExtra("paymentUpdate") != null) {

                ProgressBar paymentProgress = (ProgressBar) findViewById(R.id.paymentUpdateProgres);
                paymentProgress.setVisibility(View.INVISIBLE);

                TextView paymentText = (TextView) findViewById(R.id.paymentText);
                paymentText.setVisibility(View.VISIBLE);

                Constants.price = (double) mCardArrayAdapter.getCount() * 9.95;
                BigDecimal roundedPrice = new BigDecimal(Constants.price)
                        .setScale(2, BigDecimal.ROUND_HALF_UP);
                paymentText.setText("Total Cost: $" + roundedPrice.toString() +
                        "\nCredit Card: ****-****-****-" + Constants.creditCardLast4);

                Button continueButton = (Button) findViewById(R.id.continueButton);

                continueButton.setEnabled(canSafelyContinue());

            } else if (intent.getStringExtra("contactInfoUpdate") != null) {

                Button continueButton = (Button) findViewById(R.id.continueButton);

                continueButton.setEnabled(canSafelyContinue());

            } else if (intent.getIntExtra("orderUpdate", -1) >= 0) {

                int val = intent.getIntExtra("orderUpdate", -1);
                TextView orderStatus = (TextView) findViewById(R.id.orderStatusText);
                ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
                Button exitButton = (Button) findViewById(R.id.exitButton);

                if (val == 1) {
                    progressBar.setVisibility(View.INVISIBLE);
                    orderStatus.setText("Order Successful");

                    TextView timeText = (TextView) findViewById(R.id.timeText);
                    timeText.setVisibility(View.VISIBLE);

                    ImageView tick = (ImageView) findViewById(R.id.tickImage);
                    tick.setVisibility(View.VISIBLE);

                    exitButton.setText("Exit");

                    exitButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            finish();
                        }
                    });
                    exitButton.setVisibility(View.VISIBLE);

                } else {

                    progressBar.setVisibility(View.INVISIBLE);
                    orderStatus.setText("Order Failed");

                    ImageView cross = (ImageView) findViewById(R.id.crossImage);
                    cross.setVisibility(View.VISIBLE);

                    TextView timeText = (TextView) findViewById(R.id.timeText);
                    timeText.setText("We're sorry for the inconvenience\n\nPlease try again later");
                    timeText.setVisibility(View.VISIBLE);

                    exitButton.setText("Home");

                    exitButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            flipper.showPrevious();
                            flipper.showPrevious();
                            flipper.showPrevious();
                            flipper.showPrevious();
                        }
                    });
                    exitButton.setVisibility(View.VISIBLE);

                    SharedPreferences.Editor editor = Constants.prefs.edit();
                    editor.remove("token");
                    editor.remove("last4");
                    editor.commit();
                }

            } else if (intent.getStringExtra("paymentProgress") != null) {

                ProgressBar paymentProgress = (ProgressBar) findViewById(R.id.paymentUpdateProgres);
                TextView paymentText = (TextView) findViewById(R.id.paymentText);

                paymentText.setVisibility(View.INVISIBLE);
                paymentProgress.setVisibility(View.VISIBLE);

            }
        }
    };

    @Override
    public void onMapReady(GoogleMap map) {
        this.map = map;

        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setMapToolbarEnabled(false);

        final double[] location = getGPS();

        Constants.lat = location[0];
        Constants.lon = location[1];

        final LatLng curLocation = new LatLng(location[0], location[1]);
        pinMarker = map.addMarker(new MarkerOptions()
                .position(curLocation));
        pinMarker.setDraggable(true);
        pinMarker.setTitle("Hold to drag");
        pinMarker.showInfoWindow();
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(curLocation, 17));
        map.setOnMarkerDragListener(new MarkerDragManager());

        new Thread(new Runnable() {
            public void run() {
                ReverseGeocoding geocoding = new ReverseGeocoding();
                geocoding.getAddress(location[0], location[1]);

                if (geocoding.getAddress2().equals("")) {
                    Constants.streetAddress = geocoding.getAddress1();
                } else {
                    Constants.streetAddress = geocoding.getAddress1()
                            + ", " + geocoding.getAddress2();
                }
                Constants.cityAddress = geocoding.getCity() + ", " + geocoding.getState() +
                        " - " + geocoding.getPIN();

                MainActivity.broadcastManager.sendBroadcast(new Intent("event")
                        .putExtra("addressUpdate", ""));
            }
        }).start();

        final TextView addr1Text = (TextView) findViewById(R.id.addressInputStreet);
        final TextView addr2Text = (TextView) findViewById(R.id.addressInputCity);

        addr1Text.addTextChangedListener(addr1Watcher);
        addr2Text.addTextChangedListener(addr2Watcher);
    }

    public void getRestaurantInfo(View v) {
        flipper.showNext();

        Tracker t =  getTracker(ViewTracker.APP_TRACKER);
        t.setScreenName("RestaurantSelectionScreen");
        t.send(new HitBuilders.AppViewBuilder().build());

        ProgressBar restaurantProgress = (ProgressBar) findViewById(R.id.restaurantProgress);
        restaurantProgress.setVisibility(View.VISIBLE);

        CardGridView cardGridView = (CardGridView) findViewById(R.id.cardGrid);
        cardGridView.setVisibility(View.INVISIBLE);

        new RestaurantInfoFetcher().start();
    }

    public void showRestaurantSelection() {

        ProgressBar restaurantProgress = (ProgressBar) findViewById(R.id.restaurantProgress);
        restaurantProgress.setVisibility(View.INVISIBLE);

        CardGridView cardGridView = (CardGridView) findViewById(R.id.cardGrid);
        cardGridView.setVisibility(View.VISIBLE);

        ArrayList<Card> cards = new ArrayList<Card>();

        JSONArray resArr;
        try {
            resArr = (JSONArray) Constants.serverInfo.get("vendors");
            for (int i=0 ; i<resArr.length() ; i++) {
                JSONObject res = (JSONObject) resArr.get(i);
                Card card = new RestaurantCard(this.getApplicationContext(), res);
                cards.add(card);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        CardGridArrayAdapter mCardArrayAdapter = new CardGridArrayAdapter(this.getApplicationContext(), cards);
        CardGridView gridView = (CardGridView) findViewById(R.id.cardGrid);
        if (gridView != null) {
            gridView.setAdapter(mCardArrayAdapter);
        }
    }

    public void showFoodSelection() {

        Tracker t =  getTracker(ViewTracker.APP_TRACKER);
        t.setScreenName("FoodSelectionScreen");
        t.send(new HitBuilders.AppViewBuilder().build());

        MenuItem addButton = menu.getItem(0);
        addButton.setVisible(true);

        flipper.showNext();

        mCardArrayAdapter = new CardArrayAdapter(this, new ArrayList<Card>());
        CardListView listView = (CardListView) this.findViewById(R.id.card_list);
        if (listView != null) {
            listView.setAdapter(mCardArrayAdapter);
        }

        JSONObject drinkChoice = null;
        JSONObject foodChoice = null;

        try {
            JSONArray arr = Constants.serverInfo.getJSONArray("vendors");
            for (int i=0 ; i<arr.length() ; i++) {
                JSONObject res =  (JSONObject) arr.get(i);
                if (Constants.restaurantIdx == res.getInt("vendor_id")) {
                    drinkChoice = res.getJSONObject("addons")
                            .getJSONObject("0").getJSONObject("choices");
                    foodChoice = res.getJSONObject("types");
                }
            }
        } catch (Exception e) {
            Log.d("PhoneMod", e.toString());
        }

        FoodCard card = new FoodCard(new CardListActivity(), foodChoice, drinkChoice);
        card.setInnerLayout(R.layout.row_card);
        card.setShadow(true);
        card.setCardElevation(100);
        card.setSwipeable(true);
        card.setOnSwipeListener(swipeHandler);
        mCardArrayAdapter.add(card);
        mCardArrayAdapter.notifyDataSetChanged();
        MainActivity.broadcastManager.sendBroadcast(new Intent("event").putExtra("priceUpdate", ""));
    }

    public void showCheckoutScreen(View v) {



        Tracker t =  getTracker(ViewTracker.APP_TRACKER);
        t.setScreenName("CheckoutScreen");
        t.send(new HitBuilders.AppViewBuilder().build());

        MenuItem addButton = menu.getItem(0);
        addButton.setVisible(false);

        flipper.showNext();

        Button continueButton = (Button) findViewById(R.id.continueButton);

        if (!canSafelyContinue()) {
            continueButton.setEnabled(false);
        }

        Button editPaymentButton = (Button) findViewById(R.id.editPaymentButton);
        editPaymentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = new PaymentEditDialog();
                newFragment.show(getFragmentManager(), "");
            }
        });

        TextView addressText = (TextView) findViewById(R.id.addressText);
        if (Constants.streetAddress != null && Constants.cityAddress != null) {
            addressText.setText(Constants.streetAddress + "\n" + Constants.cityAddress);
        } else {
            addressText.setText("Address could not be found");
            continueButton.setEnabled(false);
        }

        TextView paymentText = (TextView) findViewById(R.id.paymentText);

        if (Constants.token != null && Constants.creditCardLast4 != null) {
            Constants.price = (double) mCardArrayAdapter.getCount() * 9.95;
            BigDecimal roundedPrice = new BigDecimal(Constants.price)
                    .setScale(2, BigDecimal.ROUND_HALF_UP);
            paymentText.setText("Total Cost: $" + roundedPrice.toString() +
                    "\nCredit Card: ****-****-****-" + Constants.creditCardLast4);

            if (!canSafelyContinue()) {
                continueButton.setEnabled(false);
            }
        } else {
            paymentText.setText("Credit card not found");
            continueButton.setEnabled(false);
        }

        EditText phoneText = (EditText) findViewById(R.id.phoneNumberText);
        phoneText.addTextChangedListener(phoneNumberWatcher);
        phoneText.setText(Constants.phoneNumber);

        EditText emailText = (EditText) findViewById(R.id.emailText);
        emailText.addTextChangedListener(emailWatcher);
        emailText.setText(Constants.email);
    }

    public void showProgressScreen(View v)  {

        Tracker t = getTracker(ViewTracker.APP_TRACKER);
        t.setScreenName("ProgressScreen");
        t.send(new HitBuilders.AppViewBuilder().build());

        flipper.showNext();

        TextView status = (TextView) findViewById(R.id.orderStatusText);
        status.setText("Processing...");

        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        ImageView tickImage = (ImageView) findViewById(R.id.tickImage);
        tickImage.setVisibility(View.INVISIBLE);

        ImageView crossImage = (ImageView) findViewById(R.id.crossImage);
        crossImage.setVisibility(View.INVISIBLE);

        TextView timeText = (TextView) findViewById(R.id.timeText);
        timeText.setVisibility(View.INVISIBLE);

        EditText phoneText = (EditText) findViewById(R.id.phoneNumberText);
        EditText emailText = (EditText) findViewById(R.id.emailText);

        Constants.phoneNumber = phoneText.getText().toString();
        Constants.email = emailText.getText().toString();

        String addr1Val = Constants.streetAddress;
        String addr2Val = Constants.cityAddress;

        final JSONObject sendObj = new JSONObject();
        try {

            sendObj.put("phone", Constants.phoneNumber);
            sendObj.put("coupon", "");
            sendObj.put("addr1", addr1Val);

            JSONArray orderArr = new JSONArray();
            for (int i=0 ; i<mCardArrayAdapter.getCount(); i++) {
                FoodCard card = (FoodCard) mCardArrayAdapter.getItem(i);
                JSONObject orderObj = new JSONObject();
                orderObj.put("type", card.flavorIdx);
                JSONObject drinkObj = new JSONObject();
                drinkObj.put("0", card.drinkIdx);
                orderObj.put("addons", drinkObj);
                orderArr.put(orderObj);
            }
            sendObj.put("order", orderArr);

            sendObj.put("special", "");
            sendObj.put("vendor", Constants.restaurantIdx);
            sendObj.put("email", Constants.email);
            sendObj.put("addr2", addr2Val);
            sendObj.put("stripeToken", Constants.token);
            sendObj.put("platform", "android");

        } catch (Exception e) {
            e.printStackTrace();
        }

        Thread t1 = new Thread(new OrderSender(sendObj.toString()));
        t1.start();
    }

    /*********************************** ALTERNATE LISTENERS **************************************/

    TextWatcher addr1Watcher = new TextWatcher() {
        private long after;
        private Thread t;
        private Runnable runnable_EditTextWatcher = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if ((System.currentTimeMillis() - after) > 600) {

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                double[] loc = Geocoding.getCoordinates(Constants.streetAddress,
                                        Constants.cityAddress);
                                MainActivity.broadcastManager.sendBroadcast(new Intent("event")
                                        .putExtra("pinUpdate", loc));
                            }
                        }).start();

                        t = null;
                        break;
                    }
                }
            }
        };

        @Override
        public void onTextChanged(CharSequence ss, int start, int before, int count) {

        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void afterTextChanged(Editable ss) {
            Constants.streetAddress = ss.toString();
            after = System.currentTimeMillis();
            if (t == null) {
                t = new Thread(runnable_EditTextWatcher);
                t.start();
            }
        }
    };

    TextWatcher addr2Watcher = new TextWatcher() {
        private long after;
        private Thread t;
        private Runnable runnable_EditTextWatcher = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if ((System.currentTimeMillis() - after) > 600) {

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                double[] loc = Geocoding.getCoordinates(Constants.streetAddress,
                                        Constants.cityAddress);
                                MainActivity.broadcastManager.sendBroadcast(new Intent("event")
                                        .putExtra("pinUpdate", loc));
                            }
                        }).start();

                        t = null;
                        break;
                    }
                }
            }
        };

        @Override
        public void onTextChanged(CharSequence ss, int start, int before, int count) {

        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void afterTextChanged(Editable ss) {
            Constants.cityAddress = ss.toString();
            after = System.currentTimeMillis();
            if (t == null) {
                t = new Thread(runnable_EditTextWatcher);
                t.start();
            }
        }
    };

    TextWatcher phoneNumberWatcher = new TextWatcher() {

        @Override
        public void onTextChanged(CharSequence ss, int start, int before, int count) {}

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void afterTextChanged(Editable ss) {
            Button continueButton = (Button) findViewById(R.id.continueButton);
            Constants.phoneNumber = ss.toString();
            if (ss.toString().equals("")) {
                continueButton.setEnabled(false);
            } else {
                MainActivity.broadcastManager.sendBroadcast(new Intent("event")
                        .putExtra("contactInfoUpdate", ""));
            }
        }
    };

    TextWatcher emailWatcher = new TextWatcher() {

        @Override
        public void onTextChanged(CharSequence ss, int start, int before, int count) {}

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void afterTextChanged(Editable ss) {
            Button continueButton = (Button) findViewById(R.id.continueButton);
            Constants.email = ss.toString();
            if (ss.toString().equals("")) {
                continueButton.setEnabled(false);
            } else {
                MainActivity.broadcastManager.sendBroadcast(new Intent("event")
                                .putExtra("contactInfoUpdate", ""));
            }
        }
    };

    public void onContinueButtonPressed(View v) {
        flipper.showNext();
    }

    public void onBackPressed() {
        if (flipper.getDisplayedChild() == 0) {
            super.onBackPressed();
        } else if (flipper.getDisplayedChild() == 5) { // do nothing on processing screen

        } else if (flipper.getDisplayedChild() == 3) {
            flipper.showPrevious();
            MenuItem addButton = menu.getItem(0);
            addButton.setVisible(false);
        } else if (flipper.getDisplayedChild() == 4) {
            flipper.showPrevious();
            MenuItem addButton = menu.getItem(0);
            addButton.setVisible(true);
        } else {
            flipper.showPrevious();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add) {

            JSONObject drinkChoice = null;
            JSONObject foodChoice = null;

            try {
                JSONArray arr = Constants.serverInfo.getJSONArray("vendors");
                for (int i=0 ; i<arr.length() ; i++) {
                    JSONObject res =  (JSONObject) arr.get(i);
                    if (Constants.restaurantIdx == res.getInt("vendor_id")) {
                        drinkChoice = res.getJSONObject("addons")
                                .getJSONObject("0").getJSONObject("choices");
                        //Log.d("PhoneMod", drinkChoice.toString() + "\n");
                        foodChoice = res.getJSONObject("types");
                        //Log.d("PhoneMod", foodChoice.toString() + "\n");
                    }
                }
            } catch (Exception e) {
                Log.d("PhoneMod", e.toString());
            }

            FoodCard card = new FoodCard(new CardListActivity(), foodChoice, drinkChoice);
            card.setInnerLayout(R.layout.row_card);
            card.setShadow(true);
            card.setCardElevation(100);
            card.setSwipeable(true);
            card.setOnSwipeListener(swipeHandler);

            mCardArrayAdapter.add(card);
            mCardArrayAdapter.notifyDataSetChanged();
            MainActivity.broadcastManager.sendBroadcast(new Intent("event").putExtra("titleUpdate", ""));
            MainActivity.broadcastManager.sendBroadcast(new Intent("event").putExtra("priceUpdate", ""));
        }
        return super.onOptionsItemSelected(item);
    }

    Card.OnSwipeListener swipeHandler = new Card.OnSwipeListener() {
        @Override
        public void onSwipe(Card card) {
            mCardArrayAdapter.remove(card);
            MainActivity.mCardArrayAdapter.notifyDataSetChanged();
            MainActivity.broadcastManager.sendBroadcast(new Intent("event").putExtra("priceUpdate", ""));
        }
    };

    /*********************************** UTIL METHODS *******************************************/

    private void initCreditCard() {
        Constants.token = Constants.prefs.getString("token", null);
        Constants.creditCardLast4 = Constants.prefs.getString("last4", null);
    }

    private void addShortcutToHomescreen() {

        if (Constants.prefs.getBoolean("shortcut", false)) {
            return;
        }

        SharedPreferences.Editor edit = Constants.prefs.edit();
        edit.putBoolean("shortcut", true);
        edit.commit();

        Intent shortcutIntent = new Intent(getApplicationContext(),
                MainActivity.class);

        shortcutIntent.setAction(Intent.ACTION_MAIN);

        Intent addIntent = new Intent();
        addIntent
                .putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "Snacktime");
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(getApplicationContext(),
                        R.drawable.ic_launcher));

        addIntent
                .setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        getApplicationContext().sendBroadcast(addIntent);
    }

    double[] getGPS() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = lm.getProviders(true);

        /* Loop over the array backwards, and if you get an accurate location, then break out the loop*/
        Location l = null;

        for (int i = providers.size() - 1; i >= 0; i--) {
            l = lm.getLastKnownLocation(providers.get(i));
            if (l != null) break;
        }

        double[] gps = new double[2];
        if (l != null) {
            gps[0] = l.getLatitude();
            gps[1] = l.getLongitude();
        }

        return gps;
    }

    public String getEmail () {
        Pattern emailPattern = Patterns.EMAIL_ADDRESS;
        Account[] accounts = AccountManager.get(this).getAccounts();
        for (Account account : accounts) {
            if (emailPattern.matcher(account.name).matches()) {
                return account.name;
            }
        }
        return "";
    }

    public String getPhoneNumber () {
        try {
            TelephonyManager tMgr = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
            return tMgr.getLine1Number();
        } catch (Exception e) {
            return "";
        }
    }

    synchronized Tracker getTracker(ViewTracker trackerId) {
        if (!mTrackers.containsKey(trackerId)) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            Tracker t = analytics.newTracker(R.xml.app_tracker);
            mTrackers.put(trackerId, t);
        }
        return mTrackers.get(trackerId);
    }

    private boolean canSafelyContinue() {
        return (!(Constants.creditCardLast4 == null || Constants.token == null
                || Constants.streetAddress == null || Constants.cityAddress == null
                || Constants.phoneNumber == null || Constants.email == null
                || Constants.phoneNumber.equals("") || Constants.email.equals("")
                || Constants.price <= 0));
    }
}
