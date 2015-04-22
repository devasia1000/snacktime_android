package com.snacktime.devasia.snacktime.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.snacktime.devasia.snacktime.Constants;
import com.snacktime.devasia.snacktime.MainActivity;
import com.snacktime.devasia.snacktime.R;
import com.snacktime.devasia.snacktime.network.TokenFetcher;
import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.exception.AuthenticationException;

/**
 * Created by devasia on 12/23/14.
 */
public class PaymentEditDialog extends DialogFragment {

    View v;

    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        v = inflater.inflate(R.layout.payment_edit, null);
        builder.setView(v)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dismiss();
                    }
                })
                .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        MainActivity.broadcastManager.sendBroadcast(new Intent("event")
                                .putExtra("continueUpdate", ""));
                    }
                });


        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {
            Button positiveButton = (Button) d.getButton(Dialog.BUTTON_POSITIVE);
            Button negButton = (Button) d.getButton(Dialog.BUTTON_NEGATIVE);

            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    MainActivity.broadcastManager.sendBroadcast(new Intent("event")
                            .putExtra("paymentProgress", ""));

                    TextView errorText = (TextView) v.findViewById(R.id.errorText);
                    errorText.setVisibility(View.INVISIBLE);

                    TextView cardText = (TextView) v.findViewById(R.id.cardText);
                    TextView monthText = (TextView) v.findViewById(R.id.monthText);
                    TextView yearText = (TextView) v.findViewById(R.id.yearText);
                    TextView ccvText = (TextView) v.findViewById(R.id.ccvText);

                    String creditCard = cardText.getText().toString();
                    String month = monthText.getText().toString();
                    String year = yearText.getText().toString();
                    String ccv = ccvText.getText().toString();

                    if (creditCard.equals("") || month.equals("") || year.equals("") || ccv.equals("")) {
                        errorText.setVisibility(View.VISIBLE);
                        return;
                    }

                    Card card;
                    try {
                        card = new Card(
                                creditCard,
                                Integer.parseInt(month),
                                Integer.parseInt(year),
                                ccv
                        );
                    } catch (Exception e) {
                        errorText.setVisibility(View.VISIBLE);
                        return;
                    }

                    if (card == null) {
                        errorText.setVisibility(View.VISIBLE);
                        return;
                    }

                    if (!card.validateCard()) {
                        errorText.setVisibility(View.VISIBLE);
                        return;
                    }

                    Constants.creditCardLast4 = card.getLast4();
                    SharedPreferences.Editor editor = Constants.prefs.edit();
                    editor.putString("last4", Constants.creditCardLast4);
                    editor.commit();

                    Stripe stripe = null;
                    try {
                        stripe = new Stripe(Constants.stripeApiKeyLive);
                    } catch (AuthenticationException e) {
                        e.printStackTrace();
                    }

                    final String emailCopy = Constants.email;
                    stripe.createToken(
                            card,
                            new TokenCallback() {
                                public void onSuccess(Token token) {
                                    try {
                                        Thread t = new Thread(new TokenFetcher(token.getId(),
                                                emailCopy));
                                        t.start();
                                    } catch (Exception e) {
                                        MainActivity.broadcastManager.sendBroadcast(new Intent("event")
                                                .putExtra("cardFailed", ""));
                                        e.printStackTrace();
                                    }
                                }

                                public void onError(Exception error) {
                                    MainActivity.broadcastManager.sendBroadcast(new Intent("event")
                                            .putExtra("cardFailed", ""));
                                    error.printStackTrace();
                                }
                            }
                    );

                    dismiss();
                }

            });
        }
    }
}
