package ru.hse.isochronemap.mapstructure;

enum OverpassRequestType {
    TAGS_CSV(
            "[out:csv(::type, ::id, ::lat, ::lon, highway, oneway; false; '\\t')]",
            "(._;>;);out body qt;"
    ),
    WAYS_JSON(
            "[out: json]",
            "out skel qt;"
    );

    final String HEADING, ENDING;

    OverpassRequestType(String heading, String ending) {
        HEADING = heading;
        ENDING = ending;
    }
}
