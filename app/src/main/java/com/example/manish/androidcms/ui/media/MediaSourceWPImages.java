package com.example.manish.androidcms.ui.media;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Parcel;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.android.volley.toolbox.ImageLoader;
import com.example.manish.androidcms.CMS;
import com.example.manish.androidcms.CMSDB;
import com.example.manish.androidcms.R;
import com.example.manish.androidcms.models.Blog;

import org.wordpress.mediapicker.MediaItem;
import org.wordpress.mediapicker.source.MediaSource;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Manish on 6/9/2015.
 */
public class MediaSourceWPImages implements MediaSource {

    private final List<MediaItem> mVerifiedItems = new ArrayList<MediaItem>();
    private final List<MediaItem> mMediaItems = new ArrayList<MediaItem>();
    private OnMediaChange mListener;

    @Override
    public void gather(Context context) {
        mMediaItems.clear();

        Blog blog = CMS.getCurrentBlog();

        if (blog != null) {
            Cursor imageCursor = WordPressMediaUtils.
                    getWordPressMediaImages(String.valueOf(blog.getLocalTableBlogId()));

            if (imageCursor != null) {
                addWordPressImagesFromCursor(imageCursor);
                imageCursor.close();
            } else if (mListener != null){
                mListener.onMediaLoaded(false);
            }
        } else if (mListener != null){
            mListener.onMediaLoaded(false);
        }
    }


    private void removeDeletedEntries() {
        List<MediaItem> existingItems = new ArrayList<>(mMediaItems);

        for (MediaItem mediaItem : existingItems) {
            final boolean callLoaded = existingItems.indexOf(mediaItem) == existingItems.size() - 1;

            AsyncTask<MediaItem, Void, MediaItem> backgroundCheck =
                    new AsyncTask<MediaItem, Void, MediaItem>() {
                int responseCode;

                @Override
                protected MediaItem doInBackground(MediaItem[] params) {
                    MediaItem mediaItem = params[0];
                    try {
                        URL mediaUrl = new URL(mediaItem.getSource().toString());
                        HttpURLConnection connection = (HttpURLConnection) mediaUrl.openConnection();
                        connection.setRequestMethod("GET");
                        connection.connect();
                        responseCode = connection.getResponseCode();

                    } catch (IOException ioException) {
                        Log.e("", "Error reading from " + mediaItem.getSource() + "\nexception:" + ioException);

                        return null;
                    }

                    return mediaItem;
                }

                @Override
                public void onPostExecute(MediaItem result) {
                    if (mListener != null && result != null) {
                        List<MediaItem> resultList = new ArrayList<>();
                        resultList.add(result);
                        if (responseCode == 200) {
                            mVerifiedItems.add(result);
                            mListener.onMediaAdded(MediaSourceWPImages.this, resultList);
                        }

                        if (callLoaded) {
                            mListener.onMediaLoaded(true);
                        }
                    }
                }
            };
            backgroundCheck.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mediaItem);
        }
    }


    private void addWordPressImagesFromCursor(Cursor cursor) {
        if (cursor.moveToFirst()) {
            do {
                int attachmentIdColumnIndex = cursor.getColumnIndex(CMSDB.COLUMN_NAME_MEDIA_ID);
                int fileUrlColumnIndex = cursor.getColumnIndex(CMSDB.COLUMN_NAME_FILE_URL);
                int filePathColumnIndex = cursor.getColumnIndex(CMSDB.COLUMN_NAME_FILE_PATH);
                int thumbnailColumnIndex = cursor.getColumnIndex(CMSDB.COLUMN_NAME_THUMBNAIL_URL);

                String id = "";
                if (attachmentIdColumnIndex != -1) {
                    id = String.valueOf(cursor.getInt(attachmentIdColumnIndex));
                }
                MediaItem newContent = new MediaItem();
                newContent.setTag(id);
                newContent.setTitle("");

                if (fileUrlColumnIndex != -1) {
                    String fileUrl = cursor.getString(fileUrlColumnIndex);

                    if (fileUrl != null) {
                        newContent.setSource(Uri.parse(fileUrl));
                        newContent.setPreviewSource(Uri.parse(fileUrl));
                    } else if (filePathColumnIndex != -1) {
                        String filePath = cursor.getString(filePathColumnIndex);

                        if (filePath != null) {
                            newContent.setSource(Uri.parse(filePath));
                            newContent.setPreviewSource(Uri.parse(filePath));
                        }
                    }
                }

                if (thumbnailColumnIndex != -1) {
                    String preview = cursor.getString(thumbnailColumnIndex);

                    if (preview != null) {
                        newContent.setPreviewSource(Uri.parse(preview));
                    }
                }

                mMediaItems.add(newContent);
            } while (cursor.moveToNext());

            removeDeletedEntries();
        } else if (mListener != null) {
            mListener.onMediaLoaded(true);
        }
    }

    public static Cursor getWordPressMediaImages(String blogId) {
        return CMS.cmsDB.getMediaImagesForBlog(blogId);
    }

    @Override
    public void cleanup() {
        mMediaItems.clear();
    }

    @Override
    public void setListener(OnMediaChange listener) {
        mListener = listener;
    }

    @Override
    public int getCount() {
        return mVerifiedItems.size();
    }

    @Override
    public MediaItem getMedia(int position) {
        return mVerifiedItems.get(position);
    }

    @Override
    public View getView(int position,
                        View convertView,
                        ViewGroup parent,
                        LayoutInflater inflater,
                        ImageLoader.ImageCache cache) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.media_item_wp_image, parent, false);
        }



        if (convertView != null) {
            MediaItem mediaItem = mVerifiedItems.get(position);
            Uri imageSource = mediaItem.getPreviewSource();
            ImageView imageView = (ImageView) convertView.findViewById(R.id.wp_image_view_background);
            if (imageView != null) {
                if (imageSource != null) {
                    Bitmap imageBitmap = null;
                    if (cache != null) {
                        imageBitmap = cache.getBitmap(imageSource.toString());
                    }

                    if (imageBitmap == null) {
                        imageView.setImageResource(R.color.grey_darken_10);
                        WordPressMediaUtils.BackgroundDownloadWebImage bgDownload
                                = new WordPressMediaUtils.BackgroundDownloadWebImage(imageView);
                        imageView.setTag(bgDownload);
                        bgDownload.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,
                                mediaItem.getPreviewSource());
                    } else {
                        imageView.setImageBitmap(imageBitmap);
                    }
                } else {
                    imageView.setTag(null);
                    imageView.setImageResource(R.color.grey_darken_10);
                }
            }
        }

        return convertView;
    }
    @Override
    public boolean onMediaItemSelected(MediaItem mediaItem, boolean selected) {
        return !selected;
    }

    /**
     * {@link android.os.Parcelable} interface
     */

    public static final Creator<MediaSourceWPImages> CREATOR =
            new Creator<MediaSourceWPImages>() {
                public MediaSourceWPImages createFromParcel(Parcel in) {
                    return new MediaSourceWPImages();
                }

                public MediaSourceWPImages[] newArray(int size) {
                    return new MediaSourceWPImages[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }
}
