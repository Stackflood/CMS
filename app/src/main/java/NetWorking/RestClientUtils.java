/**
 * Interface to the WordPress.com REST API.
 */
package NetWorking;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.RetryPolicy;
import com.android.volley.toolbox.RequestFuture;
import Rest.Oauth;
import Rest.RestRequest;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class RestClientUtils {
    private static final String NOTIFICATION_FIELDS = "id,type,unread,body,subject,timestamp,meta";
    private static final String COMMENT_REPLY_CONTENT_FIELD = "content";
    private static String sUserAgent = "WordPress Networking Android";

    private Rest.RestClient mRestClient;
    private Authenticator mAuthenticator;

    /**
     * Socket timeout in milliseconds for rest requests
     */
    public static final int REST_TIMEOUT_MS = 30000;

    /**
     * Default number of retries for POST rest requests
     */
    public static final int REST_MAX_RETRIES_POST = 0;

    /**
     * Default number of retries for GET rest requests
     */
    public static final int REST_MAX_RETRIES_GET = 3;

    /**
     * Default backoff multiplier for rest requests
     */
    public static final float REST_BACKOFF_MULT = 2f;

    public static void setUserAgent(String userAgent) {
        sUserAgent = userAgent;
    }

    public RestClientUtils(RequestQueue queue, Authenticator authenticator, RestRequest.OnAuthFailedListener onAuthFailedListener) {
        this(queue, authenticator, onAuthFailedListener, Rest.RestClient.REST_CLIENT_VERSIONS.V1);
    }

    public RestClientUtils(RequestQueue queue, Authenticator authenticator, RestRequest.OnAuthFailedListener onAuthFailedListener, Rest.RestClient.REST_CLIENT_VERSIONS version) {
        // load an existing access token from prefs if we have one
        mAuthenticator = authenticator;
        mRestClient = RestClientFactory.instantiate(queue, version);
        if (onAuthFailedListener != null) {
            mRestClient.setOnAuthFailedListener(onAuthFailedListener);
        }
        mRestClient.setUserAgent(sUserAgent);
    }

    public Authenticator getAuthenticator() {
        return mAuthenticator;
    }

    public Rest.RestClient getRestClient() {
        return mRestClient;
    }

    /**
     * Reply to a comment
     * <p/>
     * https://developer.wordpress.com/docs/api/1/post/sites/%24site/posts/%24post_ID/replies/new/
     */
    public void replyToComment(String reply, String path, RestRequest.Listener listener, RestRequest.ErrorListener errorListener) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(COMMENT_REPLY_CONTENT_FIELD, reply);
        post(path, params, null, listener, errorListener);
    }

    /**
     * Reply to a comment.
     * <p/>
     * https://developer.wordpress.com/docs/api/1/post/sites/%24site/posts/%24post_ID/replies/new/
     */
    public void replyToComment(long siteId, long commentId, String content, RestRequest.Listener listener,
                               RestRequest.ErrorListener errorListener) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(COMMENT_REPLY_CONTENT_FIELD, content);
        String path = String.format("sites/%d/comments/%d/replies/new", siteId, commentId);
        post(path, params, null, listener, errorListener);
    }

    /**
     * Follow a site given an ID or domain
     * <p/>
     * https://developer.wordpress.com/docs/api/1/post/sites/%24site/follows/new/
     */
    public void followSite(String siteId, RestRequest.Listener listener, RestRequest.ErrorListener errorListener) {
        String path = String.format("sites/%s/follows/new", siteId);
        post(path, listener, errorListener);
    }

    /**
     * Unfollow a site given an ID or domain
     * <p/>
     * https://developer.wordpress.com/docs/api/1/post/sites/%24site/follows/mine/delete/
     */
    public void unfollowSite(String siteId, RestRequest.Listener listener, RestRequest.ErrorListener errorListener) {
        String path = String.format("sites/%s/follows/mine/delete", siteId);
        post(path, listener, errorListener);
    }

    /**
     * Get notifications with the provided params.
     * <p/>
     * https://developer.wordpress.com/docs/api/1/get/notifications/
     */
    public void getNotifications(Map<String, String> params, RestRequest.Listener listener, RestRequest.ErrorListener errorListener) {
        params.put("number", "40");
        params.put("num_note_items", "20");
        params.put("fields", NOTIFICATION_FIELDS);
        get("notifications", params, null, listener, errorListener);
    }

    /**
     * Get notifications with default params.
     * <p/>
     * https://developer.wordpress.com/docs/api/1/get/notifications/
     */
    public void getNotifications(RestRequest.Listener listener, RestRequest.ErrorListener errorListener) {
        getNotifications(new HashMap<String, String>(), listener, errorListener);
    }

    /**
     * Update the seen timestamp.
     * <p/>
     * https://developer.wordpress.com/docs/api/1/post/notifications/seen
     */
    public void markNotificationsSeen(String timestamp, RestRequest.Listener listener, RestRequest.ErrorListener errorListener) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("time", timestamp);
        post("notifications/seen", params, null, listener, errorListener);
    }

    /**
     * Moderate a comment.
     * <p/>
     * http://developer.wordpress.com/docs/api/1/sites/%24site/comments/%24comment_ID/
     */
    public void moderateComment(String siteId, String commentId, String status, RestRequest.Listener listener,
                                RestRequest.ErrorListener errorListener) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("status", status);
        String path = String.format("sites/%s/comments/%s/", siteId, commentId);
        post(path, params, null, listener, errorListener);
    }

    /**
     * Edit the content of a comment
     */
    public void editCommentContent(long siteId, long commentId, String content, RestRequest.Listener listener,
                                RestRequest.ErrorListener errorListener) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("content", content);
        String path = String.format("sites/%d/comments/%d/", siteId, commentId);
        post(path, params, null, listener, errorListener);
    }

    /**
     * Like or unlike a comment.
     */
    public void likeComment(String siteId, String commentId, boolean isLiked, RestRequest.Listener listener,
                                RestRequest.ErrorListener errorListener) {
        Map<String, String> params = new HashMap<String, String>();
        String path = String.format("sites/%s/comments/%s/likes/", siteId, commentId);

        if (!isLiked) {
            path += "mine/delete";
        } else {
            path += "new";
        }

        post(path, params, null, listener, errorListener);
    }

    /**
     * Get all a site's themes
     */
    public void getThemes(String siteId, int limit, int offset, RestRequest.Listener listener, RestRequest.ErrorListener errorListener) {
        String path = String.format("sites/%s/themes?limit=%d&offset=%d", siteId, limit, offset);
        get(path, listener, errorListener);
    }

    /**
     * Set a site's theme
     */
    public void setTheme(String siteId, String themeId, RestRequest.Listener listener, RestRequest.ErrorListener errorListener) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("theme", themeId);
        String path = String.format("sites/%s/themes/mine", siteId);
        post(path, params, null, listener, errorListener);
    }

    /**
     * Get a site's current theme
     */
    public void getCurrentTheme(String siteId, RestRequest.Listener listener, RestRequest.ErrorListener errorListener) {
        String path = String.format("sites/%s/themes/mine", siteId);
        get(path, listener, errorListener);
    }

    /**
     * Make GET request
     */
    public Request<JSONObject> get(String path, RestRequest.Listener listener, RestRequest.ErrorListener errorListener)  {
        return get(path, null, null, listener, errorListener);
    }

    /**
     * Make GET request with params
     */
    public Request<JSONObject> get(String path, Map<String, String> params, RetryPolicy retryPolicy, RestRequest.Listener listener,
                    RestRequest.ErrorListener errorListener) {
        // turn params into querystring

        RestRequest request = mRestClient.makeRequest(Method.GET, mRestClient.getAbsoluteURL(path, params), null,
                                                      listener, errorListener);
        if (retryPolicy == null) {
            retryPolicy = new DefaultRetryPolicy(REST_TIMEOUT_MS, REST_MAX_RETRIES_GET, REST_BACKOFF_MULT);
        }
        request.setRetryPolicy(retryPolicy);
        AuthenticatorRequest authCheck = new AuthenticatorRequest(request, errorListener, mRestClient, mAuthenticator);
        authCheck.send();
        return request;
    }

    /**
     * Make Synchronous GET request
     *
     * @throws java.util.concurrent.TimeoutException
     * @throws java.util.concurrent.ExecutionException
     * @throws InterruptedException
     */
    public JSONObject getSynchronous(String path) throws InterruptedException, ExecutionException, TimeoutException {
        return getSynchronous(path, null, null);
    }

    /**
     * Make Synchronous GET request with params
     *
     * @throws java.util.concurrent.TimeoutException
     * @throws java.util.concurrent.ExecutionException
     * @throws InterruptedException
     */
    public JSONObject getSynchronous(String path, Map<String, String> params, RetryPolicy retryPolicy)
            throws InterruptedException, ExecutionException, TimeoutException {
        RequestFuture<JSONObject> future = RequestFuture.newFuture();
        RestRequest request = mRestClient.makeRequest(Method.GET, mRestClient.getAbsoluteURL(path, params), null, future, future);

        if (retryPolicy == null) {
            retryPolicy = new DefaultRetryPolicy(REST_TIMEOUT_MS, REST_MAX_RETRIES_GET, REST_BACKOFF_MULT);
        }
        request.setRetryPolicy(retryPolicy);

        AuthenticatorRequest authCheck = new AuthenticatorRequest(request, null, mRestClient, mAuthenticator);
        authCheck.send(); //this insert the request into the queue. //TODO: Verify that everything is OK on REST calls without a valid token
        JSONObject response = future.get();
        return response;
    }

    /**
     * Make POST request
     */
    public void post(String path, RestRequest.Listener listener, RestRequest.ErrorListener errorListener) {
        post(path, null, null, listener, errorListener);
    }

    /**
     * Make POST request with params
     */
    public void post(final String path, Map<String, String> params,
                     RetryPolicy retryPolicy,
                     RestRequest.Listener listener,
                     RestRequest.ErrorListener errorListener) {
        final RestRequest request = mRestClient.makeRequest(Method.POST,
                mRestClient.getAbsoluteURL(path), params,
                                                            listener, errorListener);
        if (retryPolicy == null) {
            retryPolicy = new DefaultRetryPolicy(REST_TIMEOUT_MS, REST_MAX_RETRIES_POST,
                                                 REST_BACKOFF_MULT); //Do not retry on failure
        }
        request.setRetryPolicy(retryPolicy);
        AuthenticatorRequest authCheck = new AuthenticatorRequest(request,
                errorListener, mRestClient,
                mAuthenticator);
        authCheck.send();
    }
}
