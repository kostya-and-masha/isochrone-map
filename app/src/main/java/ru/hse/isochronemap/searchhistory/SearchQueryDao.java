package ru.hse.isochronemap.searchhistory;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

/** Search query data access object. **/
@Dao
interface SearchQueryDao {
    /**
     * Inserts search query into database.
     * @param searchQuery query to insert
     */
    @Insert
    void insert(SearchQuery searchQuery);

    /**
     * Deletes specified query from database (case insensitive).
     * @param query search query string representation
     */
    @Query("DELETE FROM search WHERE LOWER(search_query) = LOWER(:query)")
    void delete(String query);

    /**
     * Gets list of search queries with specified prefix.
     * @param queryPrefix query prefix
     * @param limit maximum number of queries
     * @return list of queries sorted by creation time in descending order
     */
    @Query("SELECT search_query FROM search WHERE search_query LIKE :queryPrefix || '%'" +
           "ORDER BY _id DESC LIMIT :limit")
    List<String> getAll(String queryPrefix, int limit);

    /** Clears database. **/
    @Query("DELETE FROM search")
    void clear();
}
