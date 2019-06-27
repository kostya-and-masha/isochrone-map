package ru.hse.isochronemap.searchhistory;

import android.content.Context;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import androidx.test.core.app.ApplicationProvider;

public class SearchDatabaseTest {
    private static final String FILLED_DATABASE_NAME = "FILLED_DB";
    private static final String FRESH_DATABASE_NAME = "FRESH_DB";

    private static final String LONDON = "london";
    private static final String LONDON_CAPITAL = "London";
    private static final String LONDON_TOWER = "LoNdOn ToWeR";
    private static final String LONG_ISLAND = "Long Island";
    private static final String MECCA = "Mecca";
    private static final String MOSCOW = "Moscow";
    private static final String NEW_YORK = "new york";
    private static final String NEW_YORK_CAPITAL = "New York";

    private SearchDatabase filledDatabase;
    private SearchDatabase freshDatabase;

    @Before
    public void init() {
        Context context = ApplicationProvider.getApplicationContext();

        filledDatabase = new SearchDatabase(context, FILLED_DATABASE_NAME);
        filledDatabase.clearDatabase();
        fillDatabase(filledDatabase);

        freshDatabase = new SearchDatabase(context, FRESH_DATABASE_NAME);
        freshDatabase.clearDatabase();
    }

    private void fillDatabase(SearchDatabase database) {
        database.putSearchQuery(LONDON);
        database.putSearchQuery(MOSCOW);
        database.putSearchQuery(NEW_YORK);
        assertEquals(Arrays.asList(NEW_YORK, MOSCOW, LONDON),
                     database.getSearchQueries(""));
    }

    @Test
    public void testPutBasic() {
        freshDatabase.putSearchQuery(LONDON);
        assertEquals(Collections.singletonList(LONDON),
                     freshDatabase.getSearchQueries(""));
        freshDatabase.putSearchQuery(MOSCOW);
        assertEquals(Arrays.asList(MOSCOW, LONDON),
                     freshDatabase.getSearchQueries(""));
        freshDatabase.putSearchQuery(NEW_YORK);
        assertEquals(Arrays.asList(NEW_YORK, MOSCOW, LONDON),
                     freshDatabase.getSearchQueries(""));
    }

    @Test
    public void testPutUpdatesQueryPosition() {
        // Content: NEW_YORK, MOSCOW, LONDON

        filledDatabase.putSearchQuery(MOSCOW);
        assertEquals(Arrays.asList(MOSCOW, NEW_YORK, LONDON),
                     filledDatabase.getSearchQueries(""));

        filledDatabase.putSearchQuery(LONDON);
        assertEquals(Arrays.asList(LONDON, MOSCOW, NEW_YORK),
                     filledDatabase.getSearchQueries(""));
    }

    @Test
    public void testPutUpdatesQueryPositionCaseInsensitive() {
        // Content: NEW_YORK, MOSCOW, LONDON

        filledDatabase.putSearchQuery(LONDON_CAPITAL);
        assertEquals(Arrays.asList(LONDON_CAPITAL, NEW_YORK, MOSCOW),
                     filledDatabase.getSearchQueries(""));

        filledDatabase.putSearchQuery(NEW_YORK_CAPITAL);
        assertEquals(Arrays.asList(NEW_YORK_CAPITAL, LONDON_CAPITAL, MOSCOW),
                     filledDatabase.getSearchQueries(""));
    }

    @Test
    public void testDeleteBasic() {
        // Content: NEW_YORK, MOSCOW, LONDON

        filledDatabase.deleteSearchQuery(LONDON);
        assertEquals(Arrays.asList(NEW_YORK, MOSCOW),
                     filledDatabase.getSearchQueries(""));

        filledDatabase.deleteSearchQuery(NEW_YORK);
        assertEquals(Collections.singletonList(MOSCOW),
                     filledDatabase.getSearchQueries(""));
    }

    @Test
    public void testDeleteNotExisting() {
        // Content: NEW_YORK, MOSCOW, LONDON

        filledDatabase.deleteSearchQuery(LONDON_TOWER);
        assertEquals(Arrays.asList(NEW_YORK, MOSCOW, LONDON),
                     filledDatabase.getSearchQueries(""));

        filledDatabase.deleteSearchQuery("");
        assertEquals(Arrays.asList(NEW_YORK, MOSCOW, LONDON),
                     filledDatabase.getSearchQueries(""));

        filledDatabase.deleteSearchQuery("l");
        assertEquals(Arrays.asList(NEW_YORK, MOSCOW, LONDON),
                     filledDatabase.getSearchQueries(""));
    }

    @Test
    public void testDeleteCaseInsensitive() {
        // Content: NEW_YORK, MOSCOW, LONDON

        filledDatabase.deleteSearchQuery(LONDON_CAPITAL);
        assertEquals(Arrays.asList(NEW_YORK, MOSCOW),
                     filledDatabase.getSearchQueries(""));

        filledDatabase.deleteSearchQuery(NEW_YORK_CAPITAL);
        assertEquals(Collections.singletonList(MOSCOW),
                     filledDatabase.getSearchQueries(""));
    }

    @Test
    public void testClear() {
        // Content: NEW_YORK, MOSCOW, LONDON

        filledDatabase.clearDatabase();
        assertEquals(Collections.emptyList(), filledDatabase.getSearchQueries(""));
    }

    @Test
    public void testGet() {
        freshDatabase.putSearchQuery(LONDON);
        freshDatabase.putSearchQuery(MOSCOW);
        freshDatabase.putSearchQuery(LONDON_TOWER);
        freshDatabase.putSearchQuery(NEW_YORK_CAPITAL);
        freshDatabase.putSearchQuery(LONG_ISLAND);
        freshDatabase.putSearchQuery(MECCA);

        assertEquals(Arrays.asList(MECCA,
                                   LONG_ISLAND,
                                   NEW_YORK_CAPITAL,
                                   LONDON_TOWER,
                                   MOSCOW,
                                   LONDON),
                     freshDatabase.getSearchQueries(""));

        assertEquals(Arrays.asList(MECCA, MOSCOW),
                     freshDatabase.getSearchQueries("m"));
        assertEquals(Collections.singletonList(MOSCOW),
                     freshDatabase.getSearchQueries("mo"));
        assertEquals(Collections.emptyList(),
                     freshDatabase.getSearchQueries("mi"));

        assertEquals(Arrays.asList(LONG_ISLAND, LONDON_TOWER, LONDON),
                     freshDatabase.getSearchQueries("l"));
        assertEquals(Arrays.asList(LONG_ISLAND, LONDON_TOWER, LONDON),
                     freshDatabase.getSearchQueries("LON"));
        assertEquals(Arrays.asList(LONDON_TOWER, LONDON),
                     freshDatabase.getSearchQueries("lond"));
    }
}