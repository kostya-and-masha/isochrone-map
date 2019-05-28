package ru.hse.isochronemap.mapstructure;

import gnu.trove.map.TLongCharMap;
import gnu.trove.map.TLongObjectMap;

class MapData {
    TLongObjectMap<Node> nodesMap;
    TLongCharMap waysMap;

    MapData(TLongObjectMap<Node> nodesMap, TLongCharMap waysMap) {
        this.nodesMap = nodesMap;
        this.waysMap = waysMap;
    }
}
