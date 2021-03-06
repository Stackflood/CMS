package com.example.manish.androidcms.datasets;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import com.example.manish.androidcms.util.ReaderVideoUtils;

import org.wordpress.android.util.SqlUtils;

/**
 * Created by Manish on 9/8/2015.
 */
public class ReaderThumbnailTable {
    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbl_thumbnails ("
                + "	full_url	  TEXT COLLATE NOCASE PRIMARY KEY,"
                + " thumbnail_url TEXT NOT NULL,"
                + " post_id       INTEGER DEFAULT 0)");
    }

    protected static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS tbl_thumbnails");
    }

    /*
     * purge table of thumbnails attached to posts that no longer exist
     */
    protected static int purge(SQLiteDatabase db) {
        return db.delete("tbl_thumbnails", "post_id NOT IN (SELECT DISTINCT post_id FROM tbl_posts)", null);
    }

    public static void addThumbnail(long postId, String fullUrl, String thumbnailUrl) {
        if (TextUtils.isEmpty(fullUrl) || TextUtils.isEmpty(thumbnailUrl))
            return;

        SQLiteStatement stmt = ReaderDatabase.getWritableDb().compileStatement("INSERT OR REPLACE INTO tbl_thumbnails (full_url, thumbnail_url, post_id) VALUES (?1,?2,?3)");
        try {
            stmt.bindString(1, fullUrl);
            stmt.bindString(2, thumbnailUrl);
            stmt.bindLong  (3, postId);
            stmt.execute();
        } finally {
            SqlUtils.closeStatement(stmt);
        }
    }

    public static String getThumbnailUrl(String fullUrl) {
        if (TextUtils.isEmpty(fullUrl))
            return null;

        // if this is a YouTube video we can determine the thumbnail url from the passed url, so we
        // don't need to store the full url nor query for it
        if (ReaderVideoUtils.isYouTubeVideoLink(fullUrl))
            return ReaderVideoUtils.getYouTubeThumbnailUrl(fullUrl);

        return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(), "SELECT thumbnail_url FROM tbl_thumbnails WHERE full_url=?", new String[]{fullUrl});
    }
}
