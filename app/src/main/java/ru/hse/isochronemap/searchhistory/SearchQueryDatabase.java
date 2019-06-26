package ru.hse.isochronemap.searchhistory;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {SearchQuery.class}, version = 2, exportSchema = false)
abstract class SearchQueryDatabase extends RoomDatabase {
    abstract SearchQueryDao searchQueryDao();

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase database) {
            // There's nothing else to do here
            // Since we didn't alter the table
        }
    };
}
