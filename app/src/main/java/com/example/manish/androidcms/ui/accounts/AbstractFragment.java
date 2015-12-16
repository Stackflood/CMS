package com.example.manish.androidcms.ui.accounts;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.example.manish.androidcms.R;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;

/**
 * A fragment representing a single step in a wizard. The fragment shows a dummy title indicating
 * the page number, along with some dummy text.
 */
public abstract class AbstractFragment extends Fragment {

    protected static RequestQueue requestQueue;
    protected ConnectivityManager mSystemService;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppLog.v(AppLog.T.NUX, "NewAccountAbstractOage.onCreate()");
        mSystemService = (ConnectivityManager) getActivity()
                .getApplicationContext().
                getSystemService(Context.CONNECTIVITY_SERVICE);
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(getActivity());
        }
    }


    protected void startProgress(String message) {
    }

    protected void updateProgress(String message) {
    }

    protected void endProgress() {
    }

    protected abstract void onDoneAction();

    protected abstract boolean isUserDataValid();

    protected enum ErrorType {USERNAME, PASSWORD, SITE_URL, EMAIL, TITLE, UNDEFINED}

    protected int getErrorMessageForErrorCode(String errorCode) {
        if (errorCode.equals("username_only_lowercase_letters_and_numbers")) {
            return R.string.username_only_lowercase_letters_and_numbers;
        }
        if (errorCode.equals("username_required")) {
            return R.string.username_required;
        }
        if (errorCode.equals("username_not_allowed")) {
            return R.string.username_not_allowed;
        }
        if (errorCode.equals("email_cant_be_used_to_signup")) {
            return R.string.email_cant_be_used_to_signup;
        }
        if (errorCode.equals("username_must_be_at_least_four_characters")) {
            return R.string.username_must_be_at_least_four_characters;
        }
        if (errorCode.equals("username_contains_invalid_characters")) {
            return R.string.username_contains_invalid_characters;
        }
        if (errorCode.equals("username_must_include_letters")) {
            return R.string.username_must_include_letters;
        }
        if (errorCode.equals("email_invalid")) {
            return R.string.email_invalid;
        }
        if (errorCode.equals("email_not_allowed")) {
            return R.string.email_not_allowed;
        }
        if (errorCode.equals("username_exists")) {
            return R.string.username_exists;
        }
        if (errorCode.equals("email_exists")) {
            return R.string.email_exists;
        }
        if (errorCode.equals("username_reserved_but_may_be_available")) {
            return R.string.username_reserved_but_may_be_available;
        }
        if (errorCode.equals("email_reserved")) {
            return R.string.email_reserved;
        }
        if (errorCode.equals("blog_name_required")) {
            return R.string.blog_name_required;
        }
        if (errorCode.equals("blog_name_not_allowed")) {
            return R.string.blog_name_not_allowed;
        }
        if (errorCode.equals("blog_name_must_be_at_least_four_characters")) {
            return R.string.blog_name_must_be_at_least_four_characters;
        }
        if (errorCode.equals("blog_name_must_be_less_than_sixty_four_characters")) {
            return R.string.blog_name_must_be_less_than_sixty_four_characters;
        }
        if (errorCode.equals("blog_name_contains_invalid_characters")) {
            return R.string.blog_name_contains_invalid_characters;
        }
        if (errorCode.equals("blog_name_cant_be_used")) {
            return R.string.blog_name_cant_be_used;
        }
        if (errorCode.equals("blog_name_only_lowercase_letters_and_numbers")) {
            return R.string.blog_name_only_lowercase_letters_and_numbers;
        }
        if (errorCode.equals("blog_name_must_include_letters")) {
            return R.string.blog_name_must_include_letters;
        }
        if (errorCode.equals("blog_name_exists")) {
            return R.string.blog_name_exists;
        }
        if (errorCode.equals("blog_name_reserved")) {
            return R.string.blog_name_reserved;
        }
        if (errorCode.equals("blog_name_reserved_but_may_be_available")) {
            return R.string.blog_name_reserved_but_may_be_available;
        }
        if (errorCode.equals("password_invalid")) {
            return R.string.password_invalid;
        }
        if (errorCode.equals("blog_name_invalid")) {
            return R.string.blog_name_invalid;
        }
        if (errorCode.equals("blog_title_invalid")) {
            return R.string.blog_title_invalid;
        }
        if (errorCode.equals("username_invalid")) {
            return R.string.username_invalid;
        }
        return 0;
    }

    public class ErrorListener implements Rest.RestRequest.ErrorListener {
        @Override
        public void onErrorResponse(VolleyError error) {
            String message = null;
            int messageId;
            AppLog.e(AppLog.T.NUX, error);
            if (error.networkResponse != null && error.networkResponse.data != null) {
                AppLog.e(AppLog.T.NUX, String.format("Error message: %s", new String(error.networkResponse.data)));
                String jsonString = new String(error.networkResponse.data);
                try {
                    JSONObject errorObj = new JSONObject(jsonString);
                    messageId = getErrorMessageForErrorCode((String) errorObj.get("error"));
                    if (messageId == 0) {
                        // Not one of our common errors. Show the error message from the server.
                        message = (String) errorObj.get("message");
                    }
                } catch (JSONException e) {
                    AppLog.e(AppLog.T.NUX, e);
                    messageId = R.string.error_generic;
                }
            } else {
                if (error.getMessage() != null) {
                    if (error.getMessage().contains("Limit reached")) {
                        messageId = R.string.limit_reached;
                    } else {
                        messageId = R.string.error_generic;
                    }
                } else {
                    messageId = R.string.error_generic;
                }
            }
            endProgress();
            if (messageId == 0) {
                showError(message);
            } else {
                showError(messageId);
            }
        }
    }
    protected boolean specificShowError(int messageId) {
        return false;
    }

    protected void showError(int messageId) {
        if (!isAdded()) {
            return;
        }
        if (specificShowError(messageId)) {
            return;
        }
        // Failback if it's not a specific error
        showError(getString(messageId));
    }

    protected void showError(String message) {
        if (!isAdded()) {
            return;
        }
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        SignInDialogFragment nuxAlert = SignInDialogFragment.newInstance(getString(R.string.error), message,
                R.drawable.noticon_alert_big, getString(R.string.nux_tap_continue));
        ft.add(nuxAlert, "alert");
        ft.commitAllowingStateLoss();
    }
}
