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
        MapStructure result = MapStructureManager.getMapStructure(request);
        System.out.println("6 km foot nodes: " + result.getNodes().size());
        System.out.println("50 m foot start nodes: " + result.getStartNodes().size());

        request.setTransportType(TransportType.CAR);
        result = MapStructureManager.getMapStructure(request);
        System.out.println("6 km car nodes: " + result.getNodes().size());
        System.out.println("50 m car start nodes: " + result.getStartNodes().size());
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
        MapStructure result = MapStructureManager.getMapStructure(request, in);

        System.out.println("6 km foot nodes: " + result.getNodes().size());
        System.out.println("50 m foot start nodes: " + result.getStartNodes().size());
    }
}