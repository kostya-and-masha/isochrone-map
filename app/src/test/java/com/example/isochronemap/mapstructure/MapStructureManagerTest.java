package com.example.isochronemap.mapstructure;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.*;

class MapStructureManagerTest {
    private static File testFile;

    @BeforeAll
    static void createTemporaryFile() throws IOException {
        testFile = File.createTempFile("map", "json");
    }

    @Test
    void basicUsage() throws IOException {
        MapStructureRequest request = new MapStructureRequest(
                new Coordinate(59.980547, 30.324066),
                0.050,
                6,
                TransportType.FOOT
        );
        MapStructureManager.getMapStructure(request);

        request.setTransportType(TransportType.CAR);
        MapStructureManager.getMapStructure(request);
    }

    @Test
    void downloadMap() throws IOException {
        MapStructureRequest request = new MapStructureRequest(
                new Coordinate(59.980547, 30.324066),
                0.050,
                6,
                TransportType.FOOT
        );
        OutputStream out = new FileOutputStream(testFile);
        MapStructureManager.downloadMap(request, out);
        out.close();

        InputStream in = new FileInputStream(testFile);
        MapStructureManager.getMapStructure(request, in);
    }
}