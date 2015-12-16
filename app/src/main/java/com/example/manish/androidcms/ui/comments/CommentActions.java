package com.example.manish.androidcms.ui.comments;


import android.os.Handler;
import android.text.TextUtils;

import com.android.volley.VolleyError;
import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.datasets.CommentTable;
import com.example.manish.androidcms.models.Blog;
import com.example.manish.androidcms.models.Comment;
<<<<<<< HEAD
import com.example.manish.androidcms.models.CommentList;
=======
>>>>>>> 8c6a8f14fabf851449baff56d7d39e9b9159c13d
import com.example.manish.androidcms.models.CommentStatus;
import com.example.manish.androidcms.models.Note;

import org.json.JSONObject;
import org.wordpress.android.util.AppLog;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
<<<<<<< HEAD
import java.util.Objects;
=======
>>>>>>> 8c6a8f14fabf851449baff56d7d39e9b9159c13d

import Rest.RestRequest;
import xmlrpc.android.XMLRPCClientInterface;
import xmlrpc.android.XMLRPCException;
import xmlrpc.android.XMLRPCFactory;

/**
 * actions related to comments - replies, moderating, etc.
 * methods below do network calls in the background & update local DB upon success
 * all methods below MUST be called from UI thread
 */


/**
 * actions related to comments - replies, moderating, etc.
 * methods below do network calls in the background & update local DB upon success
 * all methods below MUST be called from UI thread
 */
public class CommentActions {

    private CommentActions() {
        throw new AssertionError();
    }


    public static interface OnNoteCommentActionListener {
        public void onModerateCommentForNote(Note note, CommentStatus newStatus);
    }
    /*
     * listener when a comment action is performed
     */
    public interface CommentActionListener
    {
        public void onActionResult(boolean succeded);
    }

    /*
<<<<<<< HEAD
     * listener when comments are moderated or deleted
     */
    public interface OnCommentsModeratedListener {
        public void onCommentsModerated(final CommentList moderatedComments);
    }
    /*
=======
>>>>>>> 8c6a8f14fabf851449baff56d7d39e9b9159c13d
    * used by comment fragments to alert container activity of a change to one or more
    * comments (moderated, deleted, added, etc.)
    */
    public static enum ChangedFrom {COMMENT_LIST, COMMENT_DETAIL}
    public static enum ChangeType {EDITED, STATUS, REPLIED, TRASHED, SPAMMED}

    public static interface OnCommentChangeListener
    {
        public void onCommentChanged(ChangedFrom changedFrom,
                                     ChangeType changeType);
    }

    public static interface OnCommentActionListener {
        public void onModerateComment(int accountId, Comment comment,
                                      CommentStatus newStatus);
    }

<<<<<<< HEAD
    /**
     * change the status of multiple comments
     * TODO: investigate using system.multiCall to perform a single call to moderate the list
     */
    static void moderateComments(final int accountId,
                                 final CommentList comments,
                                 final CommentStatus newStatus,
                                 final OnCommentsModeratedListener actionListener) {

        // deletion is handled separately
        if (newStatus != null && newStatus.equals(CommentStatus.TRASH)) {
            deleteComments(accountId, comments, actionListener);
            return;
        }

        final Blog blog = CMS.getBlog(accountId);

        if (blog==null || comments==null || comments.size() == 0 || newStatus==null || newStatus==CommentStatus.UNKNOWN) {
            if (actionListener != null)
                actionListener.onCommentsModerated(new CommentList());
            return;
        }

        final CommentList moderatedComments = new CommentList();
        final String newStatusStr = CommentStatus.toString(newStatus);
        final int localBlogId = blog.getLocalTableBlogId();
        final int remoteBlogId = blog.getRemoteBlogId();

        final Handler handler = new Handler();
        new Thread()
        {
            @Override
            public void run()
            {
                XMLRPCClientInterface client = XMLRPCFactory.instantiate(blog.getUri(), blog.getHttpuser(),
                        blog.getHttppassword());

                for(Comment comment : comments)
                {
                    Map<String, String> postHash = new HashMap<String, String>();
                    postHash.put("status", newStatusStr);
                    postHash.put("content", comment.getCommentText());
                    postHash.put("author", comment.getAuthorName());
                    postHash.put("author_url", comment.getAuthorUrl());
                    postHash.put("author_email", comment.getAuthorEmail());

                    Object[] params = {
                            remoteBlogId,
                            blog.getUsername(),
                            blog.getPassword(),
                            Long.toString(comment.commentID),
                            postHash};

                    Object result;
                    try {
                        result = client.call("wp.editComment", params);
                        boolean success = (result != null && Boolean.parseBoolean(result.toString()));
                        if (success) {
                            comment.setStatus(newStatusStr);
                            moderatedComments.add(comment);
                        }
                    } catch (XMLRPCException e) {
                        AppLog.e(AppLog.T.COMMENTS, "Error while editing comment", e);
                    } catch (IOException e) {
                        AppLog.e(AppLog.T.COMMENTS, "Error while editing comment", e);
                    } catch (XmlPullParserException e) {
                        AppLog.e(AppLog.T.COMMENTS, "Error while editing comment", e);
                    }
                }

                // update status in SQLite of successfully moderated comments
                CommentTable.updateCommentsStatus(localBlogId, moderatedComments, newStatusStr);

                if(actionListener != null)
                {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            actionListener.onCommentsModerated(moderatedComments);
                        }
                    });
                }
            }
        }.start();

    }

    /**
     * delete multiple comments
     */

    private static void deleteComments (
            final int accountId,
            final CommentList comments,
            final OnCommentsModeratedListener actionListener
    )
    {
        final Blog blog = CMS.getBlog(accountId);

        if (blog==null || comments==null || comments.size() == 0) {
            if (actionListener != null)
                actionListener.onCommentsModerated(new CommentList());
            return;
        }


        final CommentList deletedComments = new CommentList();
        final int localBlogId = blog.getLocalTableBlogId();
        final int remoteBlogId = blog.getRemoteBlogId();

        final Handler handler = new Handler();
        new Thread()
        {
            @Override
            public void run()
            {
                XMLRPCClientInterface client = XMLRPCFactory.instantiate(blog.getUri(), blog.getHttpuser(),
                        blog.getHttppassword());

                for(Comment comment: comments)
                {
                    Object[] params = {remoteBlogId,
                            blog.getUsername(),
                            blog.getPassword(),
                            comment.commentID};

                    Object result;

                    try {
                        result = client.call("wp.deleteComment", params);
                        boolean success = (result != null && Boolean.parseBoolean(result.toString()));
                        if (success)
                            deletedComments.add(comment);
                    }
                    catch (XMLRPCException e) {
                        AppLog.e(AppLog.T.COMMENTS, "Error while deleting comment", e);
                    } catch (IOException e) {
                        AppLog.e(AppLog.T.COMMENTS, "Error while deleting comment", e);
                    } catch (XmlPullParserException e) {
                        AppLog.e(AppLog.T.COMMENTS, "Error while deleting comment", e);
                    }
                }

                // remove successfully deleted comments from SQLite
                CommentTable.deleteComments(localBlogId, deletedComments);

                if(actionListener != null)
                {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            actionListener.onCommentsModerated(deletedComments);
                        }
                    });
                }
            }
        }.start();
    }
=======
>>>>>>> 8c6a8f14fabf851449baff56d7d39e9b9159c13d

    /**
     * Moderate a comment from a WPCOM notification
     */
    public static void moderateCommentRestApi(long siteId,
                                              long commentId,
                                              CommentStatus newStatus,
                                              final CommentActionListener actionListener) {

        CMS.getRestClientUtils().moderateComment(
                String.valueOf(siteId),
                String.valueOf(commentId),
                CommentStatus.toRESTString(newStatus),
                new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (actionListener != null) {
                            actionListener.onActionResult(true);
                        }
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (actionListener != null) {
                            actionListener.onActionResult(false);
                        }
                    }
                }
        );
    }
    /**
     * change the status of a single comment
     */
    static void moderateComment(final int accountId,
                                final Comment comment,
                                final CommentStatus newStatus,
                                final CommentActionListener actionListener)
    {
        // deletion is handled separately
        if(newStatus != null && newStatus.equals(CommentStatus.TRASH))
        {
            deleteComment(accountId, comment, actionListener);
            return;
        }

        final Blog blog = CMS.getBlog(accountId);

        if (blog==null || comment==null || newStatus==null || newStatus==CommentStatus.UNKNOWN) {
            if (actionListener != null)
                actionListener.onActionResult(false);
            return;
        }

        final Handler handler = new Handler();

        new Thread()
        {
            @Override
            public void run()
            {
                XMLRPCClientInterface client = XMLRPCFactory.instantiate(blog.getUri(),
                        blog.getHttpuser(),
                        blog.getHttppassword());

                Map<String, String> postHash = new HashMap<>();

                postHash.put("status", CommentStatus.toString(newStatus));
                postHash.put("content", comment.getCommentText());
                postHash.put("author", comment.getAuthorName());
                postHash.put("author_url", comment.getAuthorUrl());
                postHash.put("author_email", comment.getAuthorEmail());

                Object[] params = { blog.getRemoteBlogId(),
                        blog.getUsername(),
                        blog.getPassword(),
                        Long.toString(comment.commentID),
                        postHash};

                Object result;
                try {
                    result = client.call("wp.editComment", params);
                } catch (XMLRPCException e) {
                    AppLog.e(AppLog.T.COMMENTS, "Error while editing comment", e);
                    result = null;
                } catch (IOException e) {
                    AppLog.e(AppLog.T.COMMENTS, "Error while editing comment", e);
                    result = null;
                } catch (XmlPullParserException e) {
                    AppLog.e(AppLog.T.COMMENTS, "Error while editing comment", e);
                    result = null;
                }
                final boolean success = (result != null && Boolean.parseBoolean(result.toString()));

                if(success)
                {
                    CommentTable.updateCommentSttaus(blog.getLocalTableBlogId(), comment.commentID,
                            CommentStatus.toString(newStatus));
                }

                if(actionListener != null)
                {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            actionListener.onActionResult(success);
                        }
                    });
                }
            }
        }.start();

    }

    /**
     * reply to an individual comment
     */
    static void submitReplyToComment(final int accountId,
                 final Comment comment,
                 final String replyText,
                 final CommentActionListener actionListener)
    {
        final Blog blog = CMS.getBlog(accountId);
        if (blog==null || comment==null || TextUtils.isEmpty(replyText)) {
            if (actionListener != null)
                actionListener.onActionResult(false);
            return;
        }

        final Handler handler = new Handler();

        new Thread()
        {
            @Override
            public void run()
            {
                XMLRPCClientInterface client = XMLRPCFactory.instantiate(blog.getUri(), blog.getHttpuser(),
                        blog.getHttppassword());

                Map<String, Object> replyHash = new HashMap<String, Object>();
                replyHash.put("comment_parent", Long.toString(comment.commentID));
                replyHash.put("content", replyText);
                replyHash.put("author", "");
                replyHash.put("author_url", "");
                replyHash.put("author_email", "");

                Object[] params = {
                        blog.getRemoteBlogId(),
                        blog.getUsername(),
                        blog.getPassword(),
                        Long.toString(comment.postID),
                        replyHash };

                long newCommentID;
                try {
                    Object newCommentIDObject = client.call("wp.newComment", params);
                    if (newCommentIDObject instanceof Integer) {
                        newCommentID = ((Integer) newCommentIDObject).longValue();
                    } else if (newCommentIDObject instanceof Long) {
                        newCommentID = (Long) newCommentIDObject;
                    } else {
                        AppLog.e(AppLog.T.COMMENTS, "wp.newComment returned the wrong data type");
                        newCommentID = -1;
                    }
                } catch (XMLRPCException e) {
                    AppLog.e(AppLog.T.COMMENTS, "Error while sending the new comment", e);
                    newCommentID = -1;
                } catch (IOException e) {
                    AppLog.e(AppLog.T.COMMENTS, "Error while sending the new comment", e);
                    newCommentID = -1;
                } catch (XmlPullParserException e) {
                    AppLog.e(AppLog.T.COMMENTS, "Error while sending the new comment", e);
                    newCommentID = -1;
                }

                final boolean succeeded = (newCommentID >= 0);

                if(actionListener != null)
                {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            actionListener.onActionResult(succeeded);
                        }
                    });
                }

            }
        }.start();

    }

    /**
     * delete (trash) a single comment
     */
    private static void deleteComment(final int accountId,
                                      final Comment comment,
                                      final CommentActionListener actionListener)
    {
        final Blog blog = CMS.getBlog(accountId);

        if(blog == null || comment == null)
        {
            if(actionListener != null)
            {
                actionListener.onActionResult(false);
            }
            return;

        }

        final Handler handler = new Handler();

        new Thread()
        {
            @Override
        public void run() {
                XMLRPCClientInterface client = XMLRPCFactory.instantiate(blog.getUri(),
                        blog.getHttpuser(),
                        blog.getHttppassword());

                Object[] params = {
                        blog.getRemoteBlogId(),
                        blog.getUsername(),
                        blog.getPassword(),
                        comment.commentID};

                Object result;
                try {
                    result = client.call("wp.deleteComment", params);
                } catch (final XMLRPCException e) {
                    AppLog.e(AppLog.T.COMMENTS, "Error while deleting comment", e);
                    result = null;
                } catch (IOException e) {
                    AppLog.e(AppLog.T.COMMENTS, "Error while deleting comment", e);
                    result = null;
                } catch (XmlPullParserException e) {
                    AppLog.e(AppLog.T.COMMENTS, "Error while deleting comment", e);
                    result = null;
                }
                final boolean suceess = (result != null &&
                        Boolean.parseBoolean(result.toString()));

                if (suceess) {
                    CommentTable.deleteComment(accountId, comment.commentID);
                }

                if (actionListener != null)
                {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            actionListener.onActionResult(suceess);
                        }
                    });
                }
            }
        }.start();
    }

    /*
 * add a comment for the passed post
 */
    public static void addComment(final int accountId,
                                  final String postID,
                                  final String commentText,
                                  final CommentActionListener actionListener) {
        final Blog blog = CMS.getBlog(accountId);
        if (blog==null || TextUtils.isEmpty(commentText)) {
            if (actionListener != null)
                actionListener.onActionResult(false);
            return;
        }

        final Handler handler = new Handler();

        new Thread() {
            @Override
            public void run() {
                XMLRPCClientInterface client = XMLRPCFactory.instantiate(blog.getUri(),
                        blog.getHttpuser(),
                        blog.getHttppassword());

                Map<String, Object> commentHash = new HashMap<String, Object>();
                commentHash.put("content", commentText);
                commentHash.put("author", "");
                commentHash.put("author_url", "");
                commentHash.put("author_email", "");

                Object[] params = {
                        blog.getRemoteBlogId(),
                        blog.getUsername(),
                        blog.getPassword(),
                        postID,
                        commentHash};

                int newCommentID;
                try {
                    newCommentID = (Integer) client.call("wp.newComment", params);
                } catch (XMLRPCException e) {
                    AppLog.e(AppLog.T.COMMENTS, "Error while sending new comment", e);
                    newCommentID = -1;
                } catch (IOException e) {
                    AppLog.e(AppLog.T.COMMENTS, "Error while sending new comment", e);
                    newCommentID = -1;
                } catch (XmlPullParserException e) {
                    AppLog.e(AppLog.T.COMMENTS, "Error while sending new comment", e);
                    newCommentID = -1;
                }

                final boolean succeeded = (newCommentID >= 0);

                if (actionListener != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            actionListener.onActionResult(succeeded);
                        }
                    });
                }
            }
        }.start();
    }

}
