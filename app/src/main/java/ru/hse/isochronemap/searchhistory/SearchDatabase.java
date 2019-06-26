package ru.hse.isochronemap.searchhistory;

import android.content.Context;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.room.Room;

/** Stores search results. **/
public class SearchDatabase {
    private static final int LIMIT = 10;
    private SearchQueryDao searchQueryDao;

    /**
     * Creates new SearchDatabase instance holding connection with specified database.
     * @param context context in which database exists
     * @param databaseName database name
     */
    public SearchDatabase(@NonNull Context context, @NonNull String databaseName) {
        SearchQueryDatabase database =
                Room.databaseBuilder(context, SearchQueryDatabase.class, databaseName)
                       .addMigrations(SearchQueryDatabase.MIGRATION_1_2)
                       .allowMainThreadQueries()
                       .build();
        searchQueryDao = database.searchQueryDao();
    }

    /**
     * Puts search query into database.
     * @param query search query content
     */
    public void putSearchQuery(@NonNull String query) {
        deleteSearchQuery(query);
        searchQueryDao.insert(new SearchQuery(query));
    }

    /**
     * Deletes search query from database (case insensitive).
     * @param query search query content
     */
    public void deleteSearchQuery(@NonNull String query) {
        searchQueryDao.delete(query);
    }

    /** Clears database. **/
    public void clearDatabase() {
        searchQueryDao.clear();
    }

    /**
     * Get at most 10 first queries matching with specified prefix (case insensitive).
     * Queries are sorted by creation time in descending order.
     * @param queryPrefix search query prefix
     * @return list of matching queries
     */
    public List<String> getSearchQueries(@NonNull String queryPrefix) {
        return searchQueryDao.getAll(queryPrefix, LIMIT);
    }
}
