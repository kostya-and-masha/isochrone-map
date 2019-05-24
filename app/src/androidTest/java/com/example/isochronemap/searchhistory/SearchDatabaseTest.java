package com.example.isochronemap.searchhistory;

import android.content.Context;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
public class SearchDatabaseTest {
    @Test
    public void dummyTest() {
        Context context = ApplicationProvider.getApplicationContext();
        SearchDatabase database = new SearchDatabase(context, "test.db");

        database.clearDatabase();

        Assert.assertEquals(
                Collections.emptyList(),
                database.getSearchQueries("")
        );

        database.putSearchQuery("central street 28");
        database.putSearchQuery("кантемировская 3а");
        database.putSearchQuery("central street 28");

        Assert.assertEquals(
                Arrays.asList("central street 28", "кантемировская 3а"),
                database.getSearchQueries("")
        );

        database.putSearchQuery("площадь ленина 3");
        database.putSearchQuery("площадь мира 4");
        database.putSearchQuery("планерная улица 7");
        database.putSearchQuery("площадь ленина 3");

        Assert.assertEquals(
                database.getSearchQueries("cen"),
                Collections.singletonList("central street 28")
        );
        Assert.assertEquals(
                Arrays.asList("площадь ленина 3", "планерная улица 7", "площадь мира 4"),
                database.getSearchQueries("пл")
        );
        Assert.assertEquals(
                Arrays.asList(
                        "площадь ленина 3", "планерная улица 7", "площадь мира 4",
                        "central street 28", "кантемировская 3а"
                ),
                database.getSearchQueries("")
        );

        database.deleteSearchQuery("планерная улица 7");
        database.deleteSearchQuery("кантемировская 3");
        Assert.assertEquals(
                Arrays.asList(
                        "площадь ленина 3", "площадь мира 4",
                        "central street 28", "кантемировская 3а"
                ),
                database.getSearchQueries("")
        );

        database.close();
    }
}