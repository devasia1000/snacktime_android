package com.snacktime.devasia.snacktime.card;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.snacktime.devasia.snacktime.R;

/**
 * Created by devasia on 12/22/14.
 */
public class CardListActivity extends Activity {

    TextView titleText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.row_card);
    }
}