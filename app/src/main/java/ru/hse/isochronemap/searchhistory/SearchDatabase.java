package ru.hse.isochronemap.searchhistory;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

/** Stores search results. **/
public class SearchDatabase implements AutoCloseable {
    private SearchDatabaseHelper databaseHelper;
    private SQLiteDatabase database;

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE IF NOT EXISTS " + SearchEntry.TABLE_NAME + " (" +
                    SearchEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    SearchEntry.COLUMN_QUERY + " TEXT)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + SearchEntry.TABLE_NAME;

    private static final String SQL_LIMIT = "10";

    /**
     * Creates new SearchDatabase instance holding connection with specified database..
     * New database is created only if database with specified name did not exist before.
     * @param context context in which database exists
     * @param databaseName database name
     */
    public SearchDatabase(@NonNull Context context, @NonNull String databaseName) {
        databaseHelper = new SearchDatabaseHelper(context, databaseName);
        database = databaseHelper.getWritableDatabase();
    }

    /**
     * Puts search query into database.
     * @param query search query content
     */
    public void putSearchQuery(@NonNull String query) {
        deleteSearchQuery(query);

        ContentValues values = new ContentValues();
        values.put(SearchEntry.COLUMN_QUERY, query);

        database.insert(SearchEntry.TABLE_NAME, null, values);
    }

    /**
     * Deletes search query from database.
     * @param query search query content
     */
    public void deleteSearchQuery(@NonNull String query) {
        String selection = "LOWER(" + SearchEntry.COLUMN_QUERY + ") = LOWER(?)";
        String[] selectionArgs = { query };
        database.delete(SearchEntry.TABLE_NAME, selection, selectionArgs);
    }

    /** Clears database. **/
    public void clearDatabase() {
        database.execSQL(SQL_DELETE_ENTRIES);
        database.execSQL(SQL_CREATE_ENTRIES);
    }

    /**
     * Get at most 10 first queries matching with specified prefix.
     * Queries are sorted by creation time in descending order.
     * @param queryPrefix search query prefix
     * @return list of matching queries
     */
    public List<String> getSearchQueries(@NonNull String queryPrefix) {
        String[] projection = { SearchEntry.COLUMN_QUERY };

        String selection = SearchEntry.COLUMN_QUERY + " LIKE ?";
        String[] selectionArgs = { queryPrefix + "%" };

        String sortOrder = BaseColumns._ID + " DESC";

        Cursor cursor = database.query(
                SearchEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder,
                SQL_LIMIT
        );
        List<String> result = new ArrayList<>();
        while (cursor.moveToNext()) {
            result.add(cursor.getString(cursor.getColumnIndexOrThrow(SearchEntry.COLUMN_QUERY)));
        }
        cursor.close();
        return result;
    }

    /** Closes connection with database. **/
    @Override
    public void close() {
        databaseHelper.close();
    }

    private static class SearchEntry implements BaseColumns {
        private static final String TABLE_NAME = "search";
        private static final String COLUMN_QUERY = "search_query";
    }

    private static class SearchDatabaseHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 1;

        private SearchDatabaseHelper(@NonNull Context context, @NonNull String databaseName) {
            super(context, databaseName, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(@NonNull SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_ENTRIES);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL(SQL_DELETE_ENTRIES);
            onCreate(db);
        }
    }
}
