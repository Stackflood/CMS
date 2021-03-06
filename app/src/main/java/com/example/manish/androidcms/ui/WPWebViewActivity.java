package com.example.manish.androidcms.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.R;
import com.example.manish.androidcms.models.Blog;
import com.example.manish.androidcms.util.WPWebViewClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.helpers.WPWebChromeClient;
import org.wordpress.passcodelock.AppLockManager;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Created by Manish on 5/11/2015.
 */
public class WPWebViewActivity extends WebViewActivity {

    public static final String AUTHENTICATION_URL = "authenticated_url";
    public static final String AUTHENTICATION_USER = "authenticated_user";
    public static final String AUTHENTICATION_PASSWD = "authenticated_passwd";
    public static final String URL_TO_LOAD = "url_to_load";
    public static final String WPCOM_LOGIN_URL = "https://wordpress.com/wp-login.php";
    public static final String LOCAL_BLOG_ID = "local_blog_id";

    private static final String ENCODING_UTF8 = "UTF-8";
    /**
     * Get the URL of the WordPress login page.
     *
     * @return URL of the login page.
     */
    public static String getBlogLoginUrl(Blog blog) {
        String loginURL = null;
        Gson gson = new Gson();
        Type type = new TypeToken<Map<?, ?>>() {}.getType();
        Map<?, ?> blogOptions = gson.fromJson(blog.getBlogOptions(), type);
        if (blogOptions != null) {
            Map<?, ?> homeURLMap = (Map<?, ?>) blogOptions.get("login_url");
            if (homeURLMap != null) {
                loginURL = homeURLMap.get("value").toString();
            }
        }
        // Try to guess the login URL if blogOptions is null (blog not added to the app),
        // or WP version is < 3.6
        if (loginURL == null) {
            if (blog.getUrl().lastIndexOf("/") != -1) {
                return blog.getUrl().substring(0, blog.getUrl().lastIndexOf("/"))
                        + "/wp-login.php";
            } else {
                return blog.getUrl().replace("xmlrpc.php", "wp-login.php");
            }
        }

        return loginURL;
    }

    public static void openUrlByUsingBlogCredentials(Context context, Blog blog, String url) {
        if (context == null) {
            AppLog.e(AppLog.T.UTILS, "Context is null!!!");
            return;
        }

        if (blog == null) {
            AppLog.e(AppLog.T.UTILS, "Blog obj is null!!!");
            return;
        }

        if (TextUtils.isEmpty(url)) {
            AppLog.e(AppLog.T.UTILS, "Empty or null URL!!");
            Toast.makeText(context, context.getResources().getText(R.string.invalid_url_message),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String authURL = WPWebViewActivity.getBlogLoginUrl(blog);
        Intent intent = new Intent(context, WPWebViewActivity.class);
        intent.putExtra(WPWebViewActivity.AUTHENTICATION_USER, blog.getUsername());
        intent.putExtra(WPWebViewActivity.AUTHENTICATION_PASSWD, blog.getPassword());
        intent.putExtra(WPWebViewActivity.URL_TO_LOAD, url);
        intent.putExtra(WPWebViewActivity.AUTHENTICATION_URL, authURL);
        intent.putExtra(WPWebViewActivity.LOCAL_BLOG_ID, blog.getLocalTableBlogId());
        context.startActivity(intent);
    }

    public static void openURL(Context context, String url) {
        if (context == null) {
            AppLog.e(AppLog.T.UTILS, "Context is null!!!");
            return;
        }

        if (TextUtils.isEmpty(url)) {
            AppLog.e(AppLog.T.UTILS, "Empty or null URL!!");
            Toast.makeText(context, context.getResources().getText(R.string.invalid_url_message),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(context, WPWebViewActivity.class);
        intent.putExtra(WPWebViewActivity.URL_TO_LOAD, url);
        context.startActivity(intent);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();

        if (extras == null) {
            AppLog.e(AppLog.T.UTILS, "No valid parameters passed to WPWebViewActivity!!");
            finish();
            return;
        }

        if (extras.getInt(LOCAL_BLOG_ID, -1) > -1) {
            Blog blog = CMS.getBlog(extras.getInt(LOCAL_BLOG_ID, -1));
            if (blog == null) {
                AppLog.e(AppLog.T.UTILS, "No valid parameters passed to WPWebViewActivity!!");
                finish();
            }
            mWebView.setWebViewClient(new WPWebViewClient(this, blog));
        } else {
            mWebView.setWebViewClient(new WebViewClient());
        }
        mWebView.setWebChromeClient(new WPWebChromeClient(this,
                (ProgressBar) findViewById(R.id.progress_bar)));
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setDomStorageEnabled(true);

        String addressToLoad = extras.getString(URL_TO_LOAD);
        String username = extras.getString(AUTHENTICATION_USER, "");
        String password = extras.getString(AUTHENTICATION_PASSWD, "");
        String authURL = extras.getString(AUTHENTICATION_URL);

        if (TextUtils.isEmpty(addressToLoad) || !UrlUtils.isValidUrlAndHostNotNull(addressToLoad)) {
            AppLog.e(AppLog.T.UTILS, "Empty or null or invalid URL passed to WPWebViewActivity!!");
            Toast.makeText(this, getText(R.string.invalid_url_message),
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        if (TextUtils.isEmpty(authURL)
                && TextUtils.isEmpty(username)
                && TextUtils.isEmpty(password))
        {
            // Only the URL to load is passed to this activity.
            // Use a the normal loader not authenticated.
            loadUrl(addressToLoad);
        } else {
            if (TextUtils.isEmpty(authURL) || !UrlUtils.isValidUrlAndHostNotNull(authURL)) {
                AppLog.e(AppLog.T.UTILS, "Empty or null or invalid auth URL passed to WPWebViewActivity!!");
                Toast.makeText(this, getText(R.string.invalid_url_message),
                        Toast.LENGTH_SHORT).show();
                finish();
            }

            if (TextUtils.isEmpty(username)) {
                AppLog.e(AppLog.T.UTILS, "Username empty/null!!!");
                Toast.makeText(this, getText(R.string.incorrect_credentials), Toast.LENGTH_SHORT).show();
                finish();
            }

           this.loadAuthenticatedUrl(authURL, addressToLoad, username, password);
        }
    }

    /**
     * Login to the WordPress.com and load the specified URL.
     *
     */
    protected void loadAuthenticatedUrl(String authenticationURL, String urlToLoad, String username, String password) {
        String postData = getAuthenticationPostData(authenticationURL, urlToLoad, username, password,
                CMS.getDotComToken(this));

        mWebView.postUrl(authenticationURL, postData.getBytes());
    }

    public static String getAuthenticationPostData(String authenticationUrl,
                                                   String urlToLoad,
                                                   String username,
                                                   String password,
                                                   String token) {
        if (TextUtils.isEmpty(authenticationUrl)) return "";

        try {
            String postData = String.format("log=%s&pwd=%s&redirect_to=%s",
                    URLEncoder.encode(StringUtils.notNullStr(username), ENCODING_UTF8),
                    URLEncoder.encode(StringUtils.notNullStr(password), ENCODING_UTF8),
                    URLEncoder.encode(StringUtils.notNullStr(urlToLoad), ENCODING_UTF8)
            );

            // Add token authorization when signing in to WP.com
            if (authenticationUrl.contains("wordpress.com/wp-login.php") &&
                    !TextUtils.isEmpty(token)) {
                postData += "&authorization=Bearer " +
                        URLEncoder.encode(token, ENCODING_UTF8);
            }

            return postData;
        } catch (UnsupportedEncodingException e) {
            AppLog.e(AppLog.T.UTILS, e);
        }

        return "";
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.webview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (mWebView == null) {
            return false;
        }

        int itemID = item.getItemId();
        if (itemID == R.id.menu_refresh) {
            mWebView.reload();
            return true;
        } else if (itemID == R.id.menu_share) {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, mWebView.getUrl());
            startActivity(Intent.createChooser(share, getText(R.string.share_link)));
            return true;
        } else if (itemID == R.id.menu_browser) {
            String url = mWebView.getUrl();
            if (url != null) {
                Uri uri = Uri.parse(url);
                if (uri != null) {
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(uri);
                    startActivity(i);
                    AppLockManager.getInstance().setExtendedTimeout();
                }
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
