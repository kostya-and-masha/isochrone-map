package com.example.isochronemap.util;

import com.example.isochronemap.mapstructure.Coordinate;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TEMP_DummyResult {
    private static Random rand = new Random();

    public String string;
    public Coordinate coordinate;

    public TEMP_DummyResult(String str, Coordinate coordinate) {
        string = str;
        this.coordinate = coordinate;
    }

    public static TEMP_DummyResult getDummy() {
        StringBuilder sb = new StringBuilder();
        int limit = rand.nextInt(30);
        for (int i = 0; i < limit; i++) {
            char tempChar = (char) (rand.nextInt(96) + 32);
            sb.append(tempChar);
        }

        double latitude = 59.9;
        double longitude = 30.2;

        latitude += rand.nextFloat() % 0.1;
        longitude += rand.nextFloat() % 0.1;

        return new TEMP_DummyResult(sb.toString(), new Coordinate(latitude, longitude));
    }

    public static Random getRand() {
        return rand;
    }

    public static List<TEMP_DummyResult> getMultipleDummies(int lim) {
        ArrayList<TEMP_DummyResult> list = new ArrayList<>();
        int limit = TEMP_DummyResult.getRand().nextInt(lim);
        for (int i = 0; i < limit; i++ ){
            list.add(TEMP_DummyResult.getDummy());
        }
        return list;
    }
}
