package com.example.manish.androidcms.ui.accounts;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.R;
import com.example.manish.androidcms.models.Blog;
import com.example.manish.androidcms.networking.SelfSignedSSLCertsManager;
import com.example.manish.androidcms.ui.accounts.helpers.FetchBlogListAbstract.Callback;
import com.example.manish.androidcms.ui.accounts.helpers.FetchBlogListWPCom;
import com.example.manish.androidcms.ui.accounts.helpers.LoginAbstract;
import com.example.manish.androidcms.ui.accounts.helpers.LoginWPCom;
import com.example.manish.androidcms.util.ABTestingUtils;
import com.example.manish.androidcms.util.GenericCallback;
import com.example.manish.androidcms.widgets.CMSTextView;
import Rest.RestRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.MapUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import xmlrpc.android.ApiHelper;

/**
 * Created by Manish on 4/8/2015.
 */
public class SignInFragment extends AbstractFragment implements TextWatcher {
    private static final String DOT_COM_BASE_URL = "https://wordpress.com";
    private static final String FORGOT_PASSWORD_RELATIVE_URL = "/wp-login.php?action=lostpassword";
    private static final int WPCOM_ERRONEOUS_LOGIN_THRESHOLD = 3;
    private static final String FROM_LOGIN_SCREEN_KEY = "FROM_LOGIN_SCREEN_KEY";

    public static final String ENTERED_URL_KEY = "ENTERED_URL_KEY";
    public static final String ENTERED_USERNAME_KEY = "ENTERED_USERNAME_KEY";

    private EditText mUsernameEditText;
    private EditText mPasswordEditText;
    private EditText mUrlEditText;
    private EditText mTwoStepEditText;

    private CMSTextView mSignInButton;
    private CMSTextView mCreateAccountButton;
    private CMSTextView mAddSelfHostedButton;
    private CMSTextView mProgressTextSignIn;
    private CMSTextView mForgotPassword;
    private CMSTextView mJetpackAuthLabel;

    private LinearLayout mBottomButtonsLayout;
    private RelativeLayout mUsernameLayout;
    private RelativeLayout mPasswordLayout;
    private RelativeLayout mProgressBarSignIn;
    private RelativeLayout mUrlButtonLayout;
    private RelativeLayout mTwoStepLayout;
    private LinearLayout mTwoStepFooter;

    private ImageView mInfoButton;
    private ImageView mInfoButtonSecondary;

    //private final EmailChecker mEmailChecker;

    private boolean mSelfHosted;
    private boolean mEmailAutoCorrected;
    private boolean mShouldSendTwoStepSMS;
    private int mErroneousLogInCount;
    private String mUsername;
    private String mPassword;
    private String mTwoStepCode;
    private String mHttpUsername;
    private String mHttpPassword;
    private Blog mJetpackBlog;

    public SignInFragment() {
        //mEmailChecker = new EmailChecker();
    }

    @Override
    protected void onDoneAction() {

    }

    @Override
    protected boolean isUserDataValid() {

        final String username = EditTextUtils.getText(mUsernameEditText).trim();
        final String password = EditTextUtils.getText(mPasswordEditText).trim();
        boolean retValue = true;

        if (username.equals("")) {
            mUsernameEditText.setError(getString(R.string.required_field));
            mUsernameEditText.requestFocus();
            retValue = false;
        }

        if (password.equals("")) {
            mPasswordEditText.setError(getString(R.string.required_field));
            mPasswordEditText.requestFocus();
            retValue = false;
        }
        return retValue;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.signin_fragment, container, false);
        mUrlButtonLayout = (RelativeLayout) rootView.findViewById(R.id.url_button_layout);
        mTwoStepLayout = (RelativeLayout) rootView.findViewById(R.id.two_factor_layout);
        mTwoStepFooter = (LinearLayout) rootView.findViewById(R.id.two_step_footer);
        mUsernameLayout = (RelativeLayout) rootView.findViewById(R.id.nux_username_layout);
        mUsernameLayout.setOnClickListener(mOnLoginFormClickListener);
        mPasswordLayout = (RelativeLayout) rootView.findViewById(R.id.nux_password_layout);
        mPasswordLayout.setOnClickListener(mOnLoginFormClickListener);

        mUsernameEditText = (EditText) rootView.findViewById(R.id.nux_username);
        mUsernameEditText.addTextChangedListener(this);
        mUsernameEditText.setOnClickListener(mOnLoginFormClickListener);
        mPasswordEditText = (EditText) rootView.findViewById(R.id.nux_password);
        mPasswordEditText.addTextChangedListener(this);
        mPasswordEditText.setOnClickListener(mOnLoginFormClickListener);
        mJetpackAuthLabel = (CMSTextView) rootView.findViewById(R.id.nux_jetpack_auth_label);
        mUrlEditText = (EditText) rootView.findViewById(R.id.nux_url);
        mSignInButton = (CMSTextView)rootView.findViewById(R.id.nux_sign_in_button);
        mSignInButton.setOnClickListener(mSignInClickListener);
        mProgressBarSignIn = (RelativeLayout) rootView.findViewById(R.id.nux_sign_in_progress_bar);
        mProgressTextSignIn = (CMSTextView)rootView.findViewById(R.id.nux_sign_in_progress_text);
        mCreateAccountButton = (CMSTextView) rootView.findViewById(R.id.nux_create_account_button);
        mCreateAccountButton.setOnClickListener(mCreateAccountListener);
        mAddSelfHostedButton = (CMSTextView) rootView.findViewById(R.id.nux_add_selfhosted_button);

        mAddSelfHostedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mUrlButtonLayout.getVisibility() == View.VISIBLE) {
                    mUrlButtonLayout.setVisibility(View.GONE);
                    mAddSelfHostedButton.setText(getString(R.string.nux_add_selfhosted_blog));
                    mSelfHosted = false;
                } else {
                    mUrlButtonLayout.setVisibility(View.VISIBLE);
                    mAddSelfHostedButton.setText(getString(R.string.nux_oops_not_selfhosted_blog));
                    mSelfHosted = true;
                }
            }
        });

        mTwoStepEditText = (EditText) rootView.findViewById(R.id.nux_two_step);

        mForgotPassword = (CMSTextView) rootView.findViewById(R.id.forgot_password);
        //mForgotPassword.setOnClickListener(mForgotPasswordListener);
        /*mUsernameEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    autocorrectUsername();
                }
            }
        });

        mPasswordEditText.setOnEditorActionListener(mEditorAction);
        mUrlEditText.setOnEditorActionListener(mEditorAction);

        mTwoStepEditText = (EditText) rootView.findViewById(R.id.nux_two_step);
        mTwoStepEditText.addTextChangedListener(this);
        mTwoStepEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (keyCode == EditorInfo.IME_ACTION_DONE)) {
                    if (fieldsFilled()) {
                        signIn();
                    }
                }

                return false;
            }
        });*/

        CMSTextView twoStepFooterButton = (CMSTextView) rootView.findViewById(R.id.two_step_footer_button);
        twoStepFooterButton.setText(Html.fromHtml("<u>" + getString(R.string.two_step_footer_button) + "</u>"));
        twoStepFooterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestSMSTwoStepCode();
            }
        });

        mBottomButtonsLayout = (LinearLayout) rootView.findViewById(R.id.nux_bottom_buttons);
        //initPasswordVisibilityButton(rootView, mPasswordEditText);
       // initInfoButtons(rootView);
        //moveBottomButtons();

        return rootView;
    }

    private void requestSMSTwoStepCode() {
        if (!isAdded()) return;

        ToastUtils.showToast(getActivity(), R.string.two_step_sms_sent);
        mTwoStepEditText.setText("");
        mShouldSendTwoStepSMS = true;

        signIn();
    }

    public void showAuthErrorMessage() {
        if (mJetpackAuthLabel != null) {
            mJetpackAuthLabel.setVisibility(View.VISIBLE);
            mJetpackAuthLabel.setText(getResources().getString(R.string.auth_required));
        }
    }

    /**
     * Hide toggle button "add self hosted / sign in with WordPress.com" and show self hosted URL
     * edit box
     */
    public void forceSelfHostedMode() {
        mUrlButtonLayout.setVisibility(View.VISIBLE);
        mAddSelfHostedButton.setVisibility(View.GONE);
        mCreateAccountButton.setVisibility(View.GONE);
        mSelfHosted = true;
    }
    // Set blog for Jetpack auth
    public void setBlog(Blog blog) {
        mJetpackBlog = blog;

        if (mAddSelfHostedButton != null) {
            mJetpackAuthLabel.setVisibility(View.VISIBLE);
            mAddSelfHostedButton.setVisibility(View.GONE);
            mCreateAccountButton.setVisibility(View.GONE);
            mUsernameEditText.setText("");
        }
    }

            public void askForSslTrust() {
       SelfSignedSSLCertsManager.askForSslTrust(getActivity(),
               new GenericCallback<Void>() {
                   @Override
                   public void callback(Void aVoid) {
                       // Try to signin again
                       signIn();
                   }
               });
        endProgress();
    }

    public void signInDotComUser(String username, String password) {
        if (username != null && password != null) {
            mUsernameEditText.setText(username);
            mPasswordEditText.setText(password);
            signIn();
        }
    }

    private final View.OnClickListener mOnLoginFormClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Don't change layout if we are performing a network operation            if (mProgressBarSignIn.getVisibility() == View.VISIBLE) return;

            if (mTwoStepLayout.getVisibility() == View.VISIBLE) {
                setTwoStepAuthVisibility(false);
            }
        }
    };

    private final View.OnClickListener mSignInClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            signIn();
        }
    };

    private final View.OnClickListener mCreateAccountListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent newAccountIntent = new Intent(getActivity(), NewAccountActivity.class);
            Activity activity = getActivity();
            if (activity != null) {
                activity.startActivityForResult(newAccountIntent,
                        SignInActivity.CREATE_ACCOUNT_REQUEST);
            }
        }
    };

    private void signIn() {
        if (!isUserDataValid()) {
            return;
        }

        if (!checkNetworkConnectivity()) {
            return;
        }

        mUsername = EditTextUtils.getText(mUsernameEditText).trim();
        mPassword = EditTextUtils.getText(mPasswordEditText).trim();
        mTwoStepCode = EditTextUtils.getText(mTwoStepEditText).trim();
        if (isWPComLogin()) {
            startProgress(getString(R.string.connecting_wpcom));
            signInAndFetchBlogListWPCom();
        } else {
            startProgress(getString(R.string.signing_in));
            //signInAndFetchBlogListWPOrg();
        }
    }

    private final Callback mFetchBlogListCallback = new Callback() {
        @Override
        public void onSuccess(final List<Map<String, Object>> userBlogList) {

            if (!isAdded()) return;

            if (userBlogList != null) {
                if (isWPComLogin()) {
                    BlogUtils.addBlogs(userBlogList, mUsername);
                } else {

                    // If app is signed out, check for a matching username. No match? Then delete existing accounts
                    /*SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    if (settings.contains(WordPress.IS_SIGNED_OUT_PREFERENCE)) {
                        SharedPreferences.Editor editor = settings.edit();
                        editor.remove(WordPress.IS_SIGNED_OUT_PREFERENCE);
                        editor.apply();

                        if (userBlogList.size() > 0) {
                            String xmlrpcUrl = MapUtils.getMapStr(userBlogList.get(0), "xmlrpc");
                            if (!WordPress.wpDB.hasDotOrgAccountForUsernameAndUrl(mUsername, xmlrpcUrl)) {
                                WordPress.wpDB.dangerouslyDeleteAllContent();
                                // Clear WPCom login info (could have been set up for Jetpack stats auth)
                                WordPress.removeWpComUserRelatedData(WordPress.getContext());
                                WordPress.currentBlog = null;
                            }
                        }
                    }*/
                    BlogUtils.addBlogs(userBlogList,
                            mUsername,
                            mPassword,
                            mHttpUsername,
                            mHttpPassword);
                }

                // refresh first blog
                refreshFirstBlogContent();
            }

            //trackAnalyticsSignIn();

            if (isWPComLogin()) {
                wpcomPostLoginActions();
                // Fire off a request to get current user data
                CMS.getRestClientUtils().get("me", new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        // Update Reader Current user.
                        //ReaderUserActions.setCurrentUser(jsonObject);

                        // Set primary blog
                       setPrimaryBlog(jsonObject);
                      finishCurrentActivity(userBlogList);
                    }
                }, null);
            } else {
                finishCurrentActivity(userBlogList);
            }
        }

        private void finishCurrentActivity(final List<Map<String, Object>> userBlogList) {
            if (!isAdded()) {
                return;
            }
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (userBlogList != null) {
                        getActivity().setResult(Activity.RESULT_OK);
                        getActivity().finish();
                    }
                }
            });
        }

        private void setPrimaryBlog(JSONObject jsonObject) {
            try {
                String primaryBlogId = jsonObject.getString("primary_blog");
                // Look for a visible blog with this id in the DB
                List<Map<String, Object>> blogs = CMS.cmsDB.getAccountsBy("isHidden = 0 AND blogId = " + primaryBlogId,
                        null, 1);
                if (blogs != null && !blogs.isEmpty()) {
                    Map<String, Object> primaryBlog = blogs.get(0);
                    // Ask for a refresh and select it
                    refreshBlogContent(primaryBlog);
                    CMS.setCurrentBlog((Integer) primaryBlog.get("id"));
                }
            } catch (JSONException e) {
                AppLog.e(AppLog.T.NUX, e);
            }
        }
        private void wpcomPostLoginActions() {
            // get reader tags so they're available as soon as the Reader is accessed - note that
            // this uses the application context since the activity is finished immediately below
            /*if (isAdded()) {
                ReaderUpdateService.startService(getActivity().getApplicationContext(), EnumSet.of(
                        UpdateTask.TAGS));
            }*/
        }

        @Override
        public void onError(final int messageId, final boolean twoStepCodeRequired, final boolean httpAuthRequired,
                            final boolean erroneousSslCertificate, final String clientResponse) {
            if (!isAdded()) {
                return;
            }
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (twoStepCodeRequired) {
                        setTwoStepAuthVisibility(true);
                        endProgress();
                        return;
                    }

                    if (erroneousSslCertificate) {
                        askForSslTrust();
                        return;
                    }
                    if (httpAuthRequired) {
                        askForHttpAuthCredentials();
                        return;
                    }
                    if (messageId != 0) {
                        signInError(messageId, clientResponse);
                        return;
                    }

                    endProgress();
                }
            });

           // AnalyticsTracker.track(Stat.LOGIN_FAILED);
        }
    };
/*
    private void wpcomPostLoginActions() {
        // get reader tags so they're available as soon as the Reader is accessed - note that
        // this uses the application context since the activity is finished immediately below
        if (isAdded()) {
            ReaderUpdateService.startService(getActivity().getApplicationContext(), EnumSet.of(
                    UpdateTask.TAGS));
        }
    }*/
    private void refreshBlogContent(Map<String, Object> blogMap) {
        String blogId = blogMap.get("blogId").toString();
        String xmlRpcUrl = blogMap.get("url").toString();
        int intBlogId = StringUtils.stringToInt(blogId, -1);
        if (intBlogId == -1) {
            AppLog.e(AppLog.T.NUX, "Can't refresh blog content - invalid blogId: " + blogId);
            return;
        }
        int blogLocalId = CMS.cmsDB.getLocalTableBlogIdForRemoteBlogIdAndXmlRpcUrl(intBlogId, xmlRpcUrl);
        Blog firstBlog = CMS.cmsDB.instantiateBlogByLocalId(blogLocalId);
       // new ApiHelper.RefreshBlogContentTask(firstBlog, null).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, false);
    }

    private void askForHttpAuthCredentials() {
        // Prompt for http credentials
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle(R.string.http_authorization_required);

        View httpAuth = getActivity().getLayoutInflater().inflate(R.layout.alert_http_auth, null);
        final EditText usernameEditText = (EditText) httpAuth.findViewById(R.id.http_username);
        final EditText passwordEditText = (EditText) httpAuth.findViewById(R.id.http_password);
        alert.setView(httpAuth);
        alert.setPositiveButton(R.string.sign_in, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                mHttpUsername = EditTextUtils.getText(usernameEditText);
                mHttpPassword = EditTextUtils.getText(passwordEditText);
                signIn();
            }
        });

        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
        endProgress();
    }

    protected void handleInvalidUsernameOrPassword(int messageId) {
        mErroneousLogInCount += 1;
        if (mErroneousLogInCount >= WPCOM_ERRONEOUS_LOGIN_THRESHOLD) {
            // Clear previous errors
            mPasswordEditText.setError(null);
            mUsernameEditText.setError(null);
            showInvalidUsernameOrPasswordDialog();
        } else {
            showUsernameError(messageId);
            showPasswordError(messageId);
        }
        endProgress();
    }

    private void showPasswordError(int messageId) {
        mPasswordEditText.setError(getString(messageId));
        mPasswordEditText.requestFocus();
    }

    private void showUsernameError(int messageId) {
        mUsernameEditText.setError(getString(messageId));
        mUsernameEditText.requestFocus();
    }

    protected void showInvalidUsernameOrPasswordDialog() {
        // Show a dialog
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        SignInDialogFragment nuxAlert;
        if (ABTestingUtils.isFeatureEnabled(ABTestingUtils.Feature.HELPSHIFT)) {
            // create a 3 buttons dialog ("Contact us", "Forget your password?" and "Cancel")
            nuxAlert = SignInDialogFragment.newInstance(getString(R.string.nux_cannot_log_in),
                    getString(R.string.username_or_password_incorrect),
                    R.drawable.noticon_alert_big, 3, getString(
                            R.string.cancel), getString(
                            R.string.forgot_password), getString(
                            R.string.contact_us), SignInDialogFragment.ACTION_OPEN_URL,
                    SignInDialogFragment.ACTION_OPEN_SUPPORT_CHAT);
        } else {
            // create a 2 buttons dialog ("Forget your password?" and "Cancel")
            nuxAlert = SignInDialogFragment.newInstance(getString(R.string.nux_cannot_log_in),
                    getString(R.string.username_or_password_incorrect),
                    R.drawable.noticon_alert_big, 2, getString(
                            R.string.cancel), getString(
                            R.string.forgot_password), null, SignInDialogFragment.ACTION_OPEN_URL,
                    0);
        }

        // Put entered url and entered username args, that could help our support team
        Bundle bundle = nuxAlert.getArguments();
        bundle.putString(SignInDialogFragment.ARG_OPEN_URL_PARAM, getForgotPasswordURL());
        bundle.putString(ENTERED_URL_KEY, EditTextUtils.getText(mUrlEditText));
        bundle.putString(ENTERED_USERNAME_KEY, EditTextUtils.getText(mUsernameEditText));
        nuxAlert.setArguments(bundle);
        ft.add(nuxAlert, "alert");
        ft.commitAllowingStateLoss();
    }

    private String getForgotPasswordURL() {
        String baseUrl = DOT_COM_BASE_URL;
        if (!isWPComLogin()) {
            baseUrl = EditTextUtils.getText(mUrlEditText).trim();
            String lowerCaseBaseUrl = baseUrl.toLowerCase(Locale.getDefault());
            if (!lowerCaseBaseUrl.startsWith("https://") && !lowerCaseBaseUrl.startsWith("http://")) {
                baseUrl = "http://" + baseUrl;
            }
        }
        return baseUrl + FORGOT_PASSWORD_RELATIVE_URL;
    }
    private void showUrlError(int messageId) {
        mUrlEditText.setError(getString(messageId));
        mUrlEditText.requestFocus();
    }

    protected void signInError(int messageId, String clientResponse) {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        SignInDialogFragment nuxAlert;
        if (messageId == R.string.username_or_password_incorrect) {
            handleInvalidUsernameOrPassword(messageId);
            return;
        } else if (messageId == R.string.invalid_verification_code) {
            endProgress();
           // showTwoStepCodeError(messageId);
            return;
        } else if (messageId == R.string.invalid_url_message) {
            showUrlError(messageId);
            endProgress();
            return;
        } else {
            AppLog.e(AppLog.T.NUX, "Server response: " + clientResponse);
            nuxAlert = SignInDialogFragment.newInstance(getString(R.string.nux_cannot_log_in),
                    getString(messageId), R.drawable.noticon_alert_big, 3,
                    getString(R.string.cancel), getString(R.string.contact_us), getString(R.string.reader_title_applog),
                    SignInDialogFragment.ACTION_OPEN_SUPPORT_CHAT,
                    SignInDialogFragment.ACTION_OPEN_APPLICATION_LOG);
        }
        ft.add(nuxAlert, "alert");
        ft.commitAllowingStateLoss();
        endProgress();
    }

    /**
     * Get first blog and call RefreshBlogContentTask. First blog will be autoselected when user login.
     * Also when a user add a new self hosted blog, userBlogList contains only one element.
     * We don't want to refresh the whole list because it can be huge and each blog is refreshed when
     * user selects it.
     */
    private void refreshFirstBlogContent() {
        List<Map<String, Object>> visibleBlogs = CMS.cmsDB.getAccountsBy("isHidden = 0", null, 1);
        if (visibleBlogs != null && !visibleBlogs.isEmpty()) {
            Map<String, Object> firstBlog = visibleBlogs.get(0);
            refreshBlogContent(firstBlog);
        }
    }



    private boolean isJetpackAuth() {
        return mJetpackBlog != null;
    }

        private void signInAndFetchBlogListWPCom() {
            LoginWPCom login = new LoginWPCom(mUsername,
                    mPassword,
                    mTwoStepCode,
                    mShouldSendTwoStepSMS,
                    mJetpackBlog);

            login.execute(new LoginAbstract.Callback() {
                @Override
                public void onSuccess() {
                    mShouldSendTwoStepSMS = false;

                    // Finish this activity if we've authenticated to a Jetpack site
                if (isJetpackAuth() && getActivity() != null) {
                    getActivity().setResult(Activity.RESULT_OK);
                    getActivity().finish();
                    return;
                }

                    FetchBlogListWPCom fetchBlogListWPCom = new FetchBlogListWPCom();
                    fetchBlogListWPCom.execute(mFetchBlogListCallback);
                }

                @Override
                public void onError(int errorMessageId, boolean twoStepCodeRequired, boolean httpAuthRequired, boolean erroneousSslCertificate) {
                     mFetchBlogListCallback.onError(errorMessageId, twoStepCodeRequired, httpAuthRequired, erroneousSslCertificate, "");
                    mShouldSendTwoStepSMS = false;
                }
            });
        }


        protected void startProgress(String message) {
            mProgressBarSignIn.setVisibility(View.VISIBLE);
            mProgressTextSignIn.setVisibility(View.VISIBLE);
            mSignInButton.setVisibility(View.GONE);
            mProgressBarSignIn.setEnabled(false);
            mProgressTextSignIn.setText(message);
            mUsernameEditText.setEnabled(false);
            mPasswordEditText.setEnabled(false);
            mTwoStepEditText.setEnabled(false);
            mUrlEditText.setEnabled(false);
            mAddSelfHostedButton.setEnabled(false);
            mCreateAccountButton.setEnabled(false);
            mForgotPassword.setEnabled(false);
        }

        private boolean isWPComLogin() {
            String selfHostedUrl = EditTextUtils.getText(mUrlEditText).trim();
            return !mSelfHosted ||
                    TextUtils.isEmpty(selfHostedUrl) ||
                    selfHostedUrl.contains("wordpress.com");
        }

        private boolean checkNetworkConnectivity() {
            if (!NetworkUtils.isNetworkAvailable(getActivity())) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                SignInDialogFragment nuxAlert;
                nuxAlert = SignInDialogFragment.newInstance
                        (getString(R.string.no_network_title),
                                getString(R.string.no_network_message),
                                R.drawable.noticon_alert_big,
                                getString(R.string.cancel));
                ft.add(nuxAlert, "alert");
                ft.commitAllowingStateLoss();
                return false;
            }
            return true;
        }

        private void setTwoStepAuthVisibility(boolean isVisible) {
            mTwoStepLayout.setVisibility(isVisible ? View.VISIBLE : View.GONE);
            mTwoStepFooter.setVisibility(isVisible ? View.VISIBLE : View.GONE);
            mSignInButton.setText(isVisible ? getString(R.string.verify) : getString(R.string.sign_in));
            mForgotPassword.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            mBottomButtonsLayout.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            mUsernameEditText.setFocusableInTouchMode(!isVisible);
            mUsernameLayout.setAlpha(isVisible ? 0.6f : 1.0f);
            mPasswordEditText.setFocusableInTouchMode(!isVisible);
            mPasswordLayout.setAlpha(isVisible ? 0.6f : 1.0f);

            if (isVisible) {
                mTwoStepEditText.requestFocus();
                mTwoStepEditText.setText("");
            } else {
                mTwoStepEditText.setText("");
                mTwoStepEditText.clearFocus();
            }
        }
    }

