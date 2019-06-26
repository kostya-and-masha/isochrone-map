package ru.hse.isochronemap.searchhistory;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** This class represents search query entity in database. **/
@Entity(tableName = "search")
class SearchQuery {
    // ID is nullable because we do not want to recreate table after migration from SQLite.
    // This operation may take a long time and it is the only way to handle such changes in
    // database scheme.
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    private Integer id;

    @ColumnInfo(name = "search_query")
    private String query;

    SearchQuery(@NonNull String query) {
        this.query = query;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public String getQuery() {
        return query;
    }
}
