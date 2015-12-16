package com.example.manish.wordpressnetworking;

import com.android.volley.VolleyError;
import com.example.manish.wordpresscomrest.RestClient;
import com.example.manish.wordpresscomrest.RestRequest;
import com.example.manish.wordpresscomrest.Oauth;
import com.example.manish.wordpresscomrest.RestRequest.ErrorListener;

/**
 * Encapsulates the behaviour for asking the Authenticator for an access token. This
 * allows the request maker to disregard the authentication state when making requests.
 */
public class AuthenticatorRequest {
    private RestRequest mRequest;
    private RestRequest.ErrorListener mListener;
    private RestClient mRestClient;
    private Authenticator mAuthenticator;

    protected AuthenticatorRequest(RestRequest request, ErrorListener listener, RestClient restClient,
                                   Authenticator authenticator) {
        mRequest = request;
        mListener = listener;
        mRestClient = restClient;
        mAuthenticator = authenticator;
    }

    public String getSiteId() {
        return extractSiteIdFromUrl(mRequest.getUrl());
    }

    /**
     * Parse out the site ID from an URL.
     * Note: For batch REST API calls, only the first siteID is returned
     *
     * @return The site ID
     */
    public String extractSiteIdFromUrl(String url) {
        if (url == null) {
            return null;
        }

        final String restEndpointURL = mRestClient.getEndpointURL();
        final String sitePrefix = restEndpointURL.endsWith("/") ? restEndpointURL + "sites/" : restEndpointURL + "/sites/";
        final String batchCallPrefix = restEndpointURL.endsWith("/") ? restEndpointURL + "batch/?urls%5B%5D=%2Fsites%2F"
                : restEndpointURL + "/batch/?urls%5B%5D=%2Fsites%2F";

        if (url.startsWith(sitePrefix) && !sitePrefix.equals(url)) {
            int marker = sitePrefix.length();
            if (url.indexOf("/", marker) < marker) {
                return null;
            }
            return url.substring(marker, url.indexOf("/", marker));
        } else if (url.startsWith(batchCallPrefix) && !batchCallPrefix.equals(url)) {
            int marker = batchCallPrefix.length();
            if (url.indexOf("%2F", marker) < marker) {
                return null;
            }
            return url.substring(marker, url.indexOf("%2F", marker));
        }

        // not a sites/$siteId request or a batch request
        return null;
    }

    /**
     * Attempt to send the request, checks to see if we have an access token and if not
     * asks the Authenticator to authenticate the request.
     *
     * If no Authenticator is provided the request is always sent.
     */
    protected void send(){
        if (mAuthenticator == null) {
            mRestClient.send(mRequest);
        } else {
            mAuthenticator.authenticate(this);
        }
    }

    public void sendWithAccessToken(String token){
        mRequest.setAccessToken(token.toString());
        mRestClient.send(mRequest);
    }

    public void sendWithAccessToken(Oauth.Token token){
        sendWithAccessToken(token.toString());
    }

    /**
     * If an access token cannot be obtained the request can be aborted and the
     * handler's onFailure method is called
     */
    public void abort(VolleyError error){
        if (mListener != null) {
            mListener.onErrorResponse(error);
        }
    }
}
