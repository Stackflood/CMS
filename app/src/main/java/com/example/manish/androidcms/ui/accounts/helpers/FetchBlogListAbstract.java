package com.example.manish.androidcms.ui.accounts.helpers;

import java.util.List;
import java.util.Map;

/**
 * Created by Manish on 4/9/2015.
 */
public abstract class FetchBlogListAbstract {
    protected String mUsername;
    protected String mPassword;
    protected Callback mCallback;

    public interface Callback {
        void onSuccess(List<Map<String, Object>> userBlogList);
        void onError(int errorMessageId, boolean twoStepCodeRequired, boolean httpAuthRequired, boolean erroneousSslCertificate,
                     String clientResponse);
    }

    public FetchBlogListAbstract(String username, String password) {
        mUsername = username;
        mPassword = password;
    }

    public void execute(final Callback callback) {
        mCallback = callback;
        new Thread() {
            @Override
            public void run() {
                fetchBlogList(callback);
            }
        }.start();
    }

    protected abstract void fetchBlogList(final Callback callback);
}

