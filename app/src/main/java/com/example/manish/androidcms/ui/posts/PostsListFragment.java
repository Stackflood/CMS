package com.example.manish.androidcms.ui.posts;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.R;
import com.example.manish.androidcms.models.Blog;
import com.example.manish.androidcms.models.Post;
import com.example.manish.androidcms.models.PostsListPost;
import com.example.manish.androidcms.ui.EmptyViewAnimationHandler;
import com.example.manish.androidcms.ui.EmptyViewMessageType;
import com.example.manish.androidcms.ui.posts.adapters.PostsListAdapter;
import com.example.manish.androidcms.widgets.FloatingActionButton;
//import com.example.manish.androidcms.widgets.FloatingActionButton;

import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ServiceUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout;

import java.util.List;
import java.util.Vector;

import xmlrpc.android.ApiHelper;

/**
 * Created by Manish on 4/1/2015.
 */
public class PostsListFragment extends ListFragment
        implements CMS.OnPostUploadedListener,
        EmptyViewAnimationHandler.OnAnimationProgressListener

{

    //private FloatingActionButton mFabButton;
    private boolean mIsPage, mShouldSelectFirstPost, mIsFetchingPosts;
    private OnPostSelectedListener mOnPostSelectedListener;
    private OnSinglePostLoadedListener mOnSinglePostLoadedListener;
    private FloatingActionButton mFabButton;
    private PostsListAdapter mPostsListAdapter;
    private boolean mCanLoadMorePosts = true;
    private SwipeToRefreshHelper mSwipeToRefreshHelper;
    private boolean mKeepSwipeRefreshLayoutVisible;
    private View mProgressFooterView;
    private EmptyViewAnimationHandler mEmptyViewAnimationHandler;
    private EmptyViewMessageType mEmptyViewMessage = EmptyViewMessageType.NO_CONTENT;
    private View mEmptyView;
    private TextView mEmptyViewTitle;
    private boolean mSwipedToRefresh;
    public static final int POSTS_REQUEST_COUNT = 20;

    //private EmptyViewAnimationHandler mEmptyViewAnimationHandler;
    private View mEmptyViewImage;

    private ApiHelper.FetchPostsTask mCurrentFetchPostsTask;

    @Override
    public void onSequenceStarted(EmptyViewMessageType emptyViewMessageType) {
        mEmptyViewMessage = emptyViewMessageType;
    }

    @Override
    public void onNewTextFadingIn() {
        switch (mEmptyViewMessage) {
            case LOADING:
                mEmptyViewTitle.setText(mIsPage ? R.string.pages_fetching :
                        R.string.posts_fetching);
                break;
            case NO_CONTENT:
                mEmptyViewTitle.setText(mIsPage ? R.string.pages_empty_list :
                        R.string.posts_empty_list);
                mSwipeToRefreshHelper.setRefreshing(false);
                mKeepSwipeRefreshLayoutVisible = false;
                break;
            default:
                break;
        }
    }

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (isAdded()) {

            Bundle extras = getActivity().getIntent().getExtras();
            if(extras != null)
            {
                mIsPage = extras.getBoolean(PostsActivity.EXTRA_VIEW_PAGES);
            }

            // If PostUploadService is not running, check for posts stuck with an uploading state
            Blog currentBlog = CMS.getCurrentBlog();

            //--R--//
            /*if (!ServiceUtils.isServiceRunning(getActivity(), PostUploadService.class) && currentBlog != null) {
                CMS.cmsDB.clearAllUploadingPosts(currentBlog.getLocalTableBlogId(), mIsPage);
            }*/
        }
    }

    /*Called to have the fragment instantiate its user interface view.
    This is optional, and non-graphical fragments can return null
     (which is the default implementation). This will be called between onCreate(Bundle) and onActivityCreated(Bundle).

    If you return a View from here, you will later be called in onDestroyView() when the view is being released.*/
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.post_listview, container, false);
        mEmptyView = view.findViewById(R.id.empty_view);
        mEmptyViewImage = view.findViewById(R.id.empty_tags_box_top);
        mEmptyViewTitle = (TextView) view.findViewById(R.id.title_empty);
        return view;
    }

    private void initSwipeToRefreshHelper() {
        mSwipeToRefreshHelper = new SwipeToRefreshHelper(
                getActivity(),
                (CustomSwipeRefreshLayout) getView().findViewById(R.id.ptr_layout),
                new SwipeToRefreshHelper.RefreshListener() {
                    @Override
                    public void onRefreshStarted() {
                        if (!isAdded()) {
                            return;
                        }
                        if (!NetworkUtils.checkConnection(getActivity())) {
                            mSwipeToRefreshHelper.setRefreshing(false);
                            updateEmptyView(EmptyViewMessageType.NETWORK_ERROR);
                            return;
                        }
                        mSwipedToRefresh = true;
                        refreshPosts((PostsActivity) getActivity());
                    }
                });
    }

    private void refreshPosts(PostsActivity postsActivity) {
        Blog currentBlog = CMS.getCurrentBlog();
        if (currentBlog == null) {
            ToastUtils.showToast(getActivity(), mIsPage ?
                            R.string.error_refresh_pages : R.string.error_refresh_posts,
                    ToastUtils.Duration.LONG);
            return;
        }
        boolean hasLocalChanges = CMS.cmsDB.findLocalChanges(currentBlog.getLocalTableBlogId(),
                mIsPage);
        if (hasLocalChanges) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(postsActivity);
            dialogBuilder.setTitle(getResources().getText(R.string.local_changes));
            dialogBuilder.setMessage(getResources().getText(R.string.overwrite_local_changes));
            dialogBuilder.setPositiveButton(getResources().getText(R.string.yes),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            mSwipeToRefreshHelper.setRefreshing(true);
                            requestPosts(false);
                        }
                    }
            );
            dialogBuilder.setNegativeButton(getResources().getText(R.string.no), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    mSwipeToRefreshHelper.setRefreshing(false);
                }
            });
            dialogBuilder.setCancelable(true);
            dialogBuilder.create().show();
        } else {
            mSwipeToRefreshHelper.setRefreshing(true);
            requestPosts(false);
        }
    }

    public PostsListAdapter getPostListAdapter() {
        if (mPostsListAdapter == null) {
            PostsListAdapter.OnLoadMoreListener loadMoreListener = new PostsListAdapter.OnLoadMoreListener() {
                @Override
                public void onLoadMore() {
                    if (mCanLoadMorePosts && !mIsFetchingPosts)
                        requestPosts(true);
                }
            };

            PostsListAdapter.OnPostsLoadedListener postsLoadedListener = new PostsListAdapter.OnPostsLoadedListener() {
                @Override
                public void onPostsLoaded(int postCount) {
                    if (!isAdded()) {
                        return;
                    }

                    // Now that posts have been loaded, show the empty view if there are no results to display
                    // This avoids the problem of the empty view immediately appearing when set at design time
                    if (postCount == 0) {
                        mEmptyView.setVisibility(View.VISIBLE);
                    } else {
                        mEmptyView.setVisibility(View.GONE);
                    }

                    if (!isRefreshing() || mKeepSwipeRefreshLayoutVisible) {
                        // No posts and not currently refreshing. Display the "no posts/pages" message
                        updateEmptyView(EmptyViewMessageType.NO_CONTENT);
                    }
                    if (postCount == 0 && mCanLoadMorePosts) {
                        // No posts, let's request some if network available
                        if (isAdded() && NetworkUtils.isNetworkAvailable(getActivity())) {
                            setRefreshing(true);
                            requestPosts(false);
                        } else {
                            updateEmptyView(EmptyViewMessageType.NETWORK_ERROR);
                        }
                    } else if (mShouldSelectFirstPost) {
                        // Select the first row on a tablet, if requested
                        mShouldSelectFirstPost = false;
                        if (mPostsListAdapter.getCount() > 0) {
                            PostsListPost postsListPost = (PostsListPost) mPostsListAdapter.getItem(0);
                            if (postsListPost != null) {
                                showPost(postsListPost.getPostId());
                                getListView().setItemChecked(0, true);
                            }
                        }
                    } else if (isAdded() && ((PostsActivity) getActivity()).isDualPane()) {
                        // Reload the last selected position, if available
                        int selectedPosition = getListView().getCheckedItemPosition();
                        if (selectedPosition != ListView.INVALID_POSITION && selectedPosition < mPostsListAdapter.getCount()) {
                            PostsListPost postsListPost = (PostsListPost) mPostsListAdapter.getItem(selectedPosition);
                            if (postsListPost != null) {
                                showPost(postsListPost.getPostId());
                            }
                        }
                    }
                }
            };
            mPostsListAdapter = new PostsListAdapter(getActivity(), mIsPage, loadMoreListener, postsLoadedListener);
        }

        return mPostsListAdapter;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mProgressFooterView = View.inflate(getActivity(), R.layout.list_footer_progress, null);
        getListView().addFooterView(mProgressFooterView, null, false);
        mProgressFooterView.setVisibility(View.GONE);
        getListView().setDivider(getResources().getDrawable(R.drawable.list_divider));
        getListView().setDividerHeight(1);

        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
                if (position >= getPostListAdapter().getCount()) //out of bounds
                    return;
                if (v == null) //view is gone
                    return;
                PostsListPost postsListPost = (PostsListPost)
                        getPostListAdapter().getItem(position);
                if (postsListPost == null)
                    return;
                if (!mIsFetchingPosts || isLoadingMorePosts()) {
                    showPost(postsListPost.getPostId());
                } else if (isAdded()) {
                    Toast.makeText(getActivity(), mIsPage ? R.string.pages_fetching : R.string.posts_fetching,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        initSwipeToRefreshHelper();
        CMS.setOnPostUploadedListener(this);

        mFabButton = (FloatingActionButton) getView().findViewById(R.id.fab_button);
        mFabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newPost();
            }
        });

        mEmptyViewAnimationHandler = new EmptyViewAnimationHandler
                (mEmptyViewTitle, mEmptyViewImage, this);


        if (NetworkUtils.isNetworkAvailable(getActivity())) {
            ((PostsActivity) getActivity()).requestPosts();
        } else {
            updateEmptyView(EmptyViewMessageType.NETWORK_ERROR);
        }
    }

    private void newPost() {
        if (getActivity() instanceof PostsActivity) {
            ((PostsActivity)getActivity()).newPost();
        }
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            // check that the containing activity implements our callback
            mOnPostSelectedListener = (OnPostSelectedListener) activity;
            mOnSinglePostLoadedListener = (OnSinglePostLoadedListener) activity;
        } catch (ClassCastException e) {
            activity.finish();
            throw new ClassCastException(activity.toString()
                    + " must implement Callback");
        }
    }

    public void onResume() {
        super.onResume();
        if (CMS.getCurrentBlog() != null) {
            if (getListView().getAdapter() == null) {
                getListView().setAdapter(getPostListAdapter());
            }

            getPostListAdapter().loadPosts();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

//        if (mFabButton != null) {
//            mFabButton.setVisibility(hidden ? View.GONE : View.VISIBLE);
//        }
    }


    public boolean isRefreshing() {
        return mSwipeToRefreshHelper.isRefreshing();
    }

    /*Called when the fragment's activity has been created and this fragment's view hierarchy instantiated. It can be used to do final initialization once these pieces are in place, such as retrieving views or restoring state. It is also useful for fragments that use setRetainInstance(boolean) to retain their instance, as this callback tells the fragment when it is fully associated with the new activity instance. This is called after onCreateView(LayoutInflater, ViewGroup, Bundle) and before onViewStateRestored(Bundle).

    Parameters
    savedInstanceState	If the fragment is being re-created from a previous saved state, this is the state.
*/


    public void setRefreshing(boolean refreshing) {
        mSwipeToRefreshHelper.setRefreshing(refreshing);
    }
/*
    @Override
    public void OnPostUploaded(int localBlogId, String postId, boolean isPage) {

    }*/


    boolean isLoadingMorePosts() {
        return mIsFetchingPosts && (mProgressFooterView != null && mProgressFooterView.getVisibility() == View.VISIBLE);
    }




    private void showPost(long selectedId) {
        if (CMS.getCurrentBlog() == null)
            return;

        Post post = CMS.cmsDB.getPostForLocalTablePostId(selectedId);
        if (post != null) {
            CMS.currentPost = post;
            mOnPostSelectedListener.onPostSelected(post);
        } else {
            if (!getActivity().isFinishing()) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                //WPAlertDialogFragment alert = WPAlertDialogFragment.newAlertDialog(getString(R.string.post_not_found));
                //ft.add(alert, "alert");
                //ft.commitAllowingStateLoss();
            }
        }
    }

    public void requestPosts(boolean loadMore) {
        if (!isAdded() || CMS.getCurrentBlog() == null || mIsFetchingPosts) {
            return;
        }

        if (!NetworkUtils.checkConnection(getActivity())) {
            mSwipeToRefreshHelper.setRefreshing(false);
            updateEmptyView(EmptyViewMessageType.NETWORK_ERROR);
            return;
        }

        updateEmptyView(EmptyViewMessageType.LOADING);

        int postCount = getPostListAdapter().getRemotePostCount() + POSTS_REQUEST_COUNT;
        if (!loadMore) {
            mCanLoadMorePosts = true;
            postCount = POSTS_REQUEST_COUNT;
        }
        List<Object> apiArgs = new Vector<Object>();
        apiArgs.add(CMS.getCurrentBlog());
        apiArgs.add(mIsPage);
        apiArgs.add(postCount);
        apiArgs.add(loadMore);
        if (mProgressFooterView != null && loadMore) {
            mProgressFooterView.setVisibility(View.VISIBLE);
        }

        mCurrentFetchPostsTask = new ApiHelper.FetchPostsTask(new ApiHelper.FetchPostsTask.Callback() {
            @Override
            public void onSuccess(int postCount) {
                mCurrentFetchPostsTask = null;
                mIsFetchingPosts = false;
                if (!isAdded())
                    return;

                if (mEmptyViewAnimationHandler.isShowingLoadingAnimation() ||
                        mEmptyViewAnimationHandler.isBetweenSequences()) {
                    // Keep the SwipeRefreshLayout animation visible until the EmptyViewAnimationHandler dismisses it
                    mKeepSwipeRefreshLayoutVisible = true;
                } else {
                    mSwipeToRefreshHelper.setRefreshing(false);
                }

                if (mProgressFooterView != null) {
                    mProgressFooterView.setVisibility(View.GONE);
                }

                if (postCount == 0) {
                    mCanLoadMorePosts = false;
                } else if (postCount == getPostListAdapter().getRemotePostCount() && postCount != POSTS_REQUEST_COUNT) {
                    mCanLoadMorePosts = false;
                }

                getPostListAdapter().loadPosts();
            }

            @Override
            public void onFailure(ApiHelper.ErrorType errorType, String errorMessage, Throwable throwable) {
                mCurrentFetchPostsTask = null;
                mIsFetchingPosts = false;
                if (!isAdded()) {
                    return;
                }
                mSwipeToRefreshHelper.setRefreshing(false);
                if (mProgressFooterView != null) {
                    mProgressFooterView.setVisibility(View.GONE);
                }
                if (errorType != ApiHelper.ErrorType.TASK_CANCELLED && errorType != ApiHelper.ErrorType.NO_ERROR) {
                    switch (errorType) {
                        case UNAUTHORIZED:
                            if (mEmptyView == null || mEmptyView.getVisibility() != View.VISIBLE) {
                                ToastUtils.showToast(getActivity(),
                                        mIsPage ? R.string.error_refresh_unauthorized_pages :
                                                R.string.error_refresh_unauthorized_posts, ToastUtils.Duration.LONG);
                            }
                            updateEmptyView(EmptyViewMessageType.PERMISSION_ERROR);
                            return;
                        default:
                            ToastUtils.showToast(getActivity(),
                                    mIsPage ? R.string.error_refresh_pages : R.string.error_refresh_posts,
                                    ToastUtils.Duration.LONG);
                            updateEmptyView(EmptyViewMessageType.GENERIC_ERROR);
                            return;
                    }
                }
            }
        });


        mIsFetchingPosts = true;
        mCurrentFetchPostsTask.execute(apiArgs);
    }

    protected void clear() {
        if (getPostListAdapter() != null) {
            getPostListAdapter().clear();
        }
        mCanLoadMorePosts = true;
        if (mProgressFooterView != null && mProgressFooterView.getVisibility() == View.VISIBLE) {
            mProgressFooterView.setVisibility(View.GONE);
        }
        //mEmptyViewAnimationHandler.clear();
    }

    private void updateEmptyView(final EmptyViewMessageType emptyViewMessageType) {
        if (mPostsListAdapter != null && mPostsListAdapter.getCount() == 0) {
            // Handle animation display
            if (mEmptyViewMessage == EmptyViewMessageType.NO_CONTENT &&
                    emptyViewMessageType == EmptyViewMessageType.LOADING) {
                // Show the NO_CONTENT > LOADING sequence, but only if the user swiped to refresh
                if (mSwipedToRefresh) {
                    mSwipedToRefresh = false;
                    mEmptyViewAnimationHandler.showLoadingSequence();
                    return;
                }
            } else if (mEmptyViewMessage == EmptyViewMessageType.LOADING &&
                    emptyViewMessageType == EmptyViewMessageType.NO_CONTENT) {
                // Show the LOADING > NO_CONTENT sequence
                mEmptyViewAnimationHandler.showNoContentSequence();
                return;
            }
            } else {
                // Dismiss the SwipeRefreshLayout animation if it was set to persist
            if (mKeepSwipeRefreshLayoutVisible) {
                mSwipeToRefreshHelper.setRefreshing(false);
                mKeepSwipeRefreshLayoutVisible = false;
            }
            }

            if (mEmptyView != null) {
                int stringId = 0;

                // Don't modify the empty view image if the NO_CONTENT > LOADING sequence has already run -
                // let the EmptyViewAnimationHandler take care of it
            if (!mEmptyViewAnimationHandler.isBetweenSequences()) {
                if (emptyViewMessageType == EmptyViewMessageType.NO_CONTENT) {
                    mEmptyViewImage.setVisibility(View.VISIBLE);
                } else {
                    mEmptyViewImage.setVisibility(View.GONE);
                }
            }

                switch (emptyViewMessageType) {
                    case LOADING:
                        stringId = mIsPage ? R.string.pages_fetching : R.string.posts_fetching;
                        break;
                    case NO_CONTENT:
                        stringId = mIsPage ? R.string.pages_empty_list : R.string.posts_empty_list;
                        break;
                    case NETWORK_ERROR:
                        stringId = R.string.no_network_message;
                        break;
                    case PERMISSION_ERROR:
                        stringId = mIsPage ? R.string.error_refresh_unauthorized_pages :
                                R.string.error_refresh_unauthorized_posts;
                        break;
                    case GENERIC_ERROR:
                        stringId = mIsPage ? R.string.error_refresh_pages : R.string.error_refresh_posts;
                        break;
                }

                mEmptyViewTitle.setText(getText(stringId));
                mEmptyViewMessage = emptyViewMessageType;
            }
        }




    /*onActivityCreated(Bundle savedInstanceState)
    Called when the fragment's activity has been created and this fragment's view hierarchy instantiated*/

    public void setShouldSelectFirstPost(boolean shouldSelect) {
        mShouldSelectFirstPost = shouldSelect;
    }

    public interface OnPostSelectedListener {
        public void onPostSelected(Post post);
    }

    public interface OnPostActionListener {
        public void onPostAction(int action, Post post);
    }

    public interface OnSinglePostLoadedListener {
        public void onSinglePostLoaded();
    }


    public  void OnPostUploaded(int localBlogId, String postId, boolean isPage) {

    }

    public  void OnPostUploadFailed(int localBlogId)
    {

    }




}
