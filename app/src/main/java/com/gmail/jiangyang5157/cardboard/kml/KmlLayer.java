package com.gmail.jiangyang5157.cardboard.kml;

import android.content.Context;

import com.gmail.jiangyang5157.cardboard.scene.model.AtomMap;
import com.google.android.gms.maps.MapsInitializer;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Document class allows for users to input their KML data and output it onto the map
 * <p/>
 * Reference https://github.com/googlemaps/android-maps-utils/tree/master/library/src/com/google/maps/android/kml
 * Get rid of GoogleMap dependence
 */
public class KmlLayer {

    private final KmlRender mRenderer;

    /**
     * Creates a new KmlLayer object - addLayerToMap() must be called to trigger rendering onto a map.
     *
     * @param map        Map object
     * @param resourceId Raw resource KML file
     * @param context    Context object
     * @throws XmlPullParserException if file cannot be parsed
     */
    public KmlLayer(AtomMap map, int resourceId, Context context)
            throws XmlPullParserException, IOException {
        this(map, context.getResources().openRawResource(resourceId), context);
    }

    /**
     * Creates a new KmlLayer object
     *
     * @param map    Map object
     * @param stream InputStream containing KML file
     * @throws XmlPullParserException if file cannot be parsed
     */
    public KmlLayer(AtomMap map, InputStream stream, Context context)
            throws XmlPullParserException, IOException {
        if (stream == null) {
            throw new IllegalArgumentException("KML InputStream cannot be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("KML Context cannot be null");
        }
        MapsInitializer.initialize(context);

        mRenderer = new KmlRender(map, context);
        XmlPullParser xmlPullParser = createXmlParser(stream);
        KmlParser parser = new KmlParser(xmlPullParser);
        parser.parseKml();
        stream.close();
        mRenderer.storeKmlData(parser.getStyles(), parser.getStyleMaps(), parser.getPlacemarks(), parser.getNetworkLinks(), parser.getContainers());
    }

    /**
     * Creates a new XmlPullParser to allow for the KML file to be parsed
     *
     * @param stream InputStream containing KML file
     * @return XmlPullParser containing the KML file
     * @throws XmlPullParserException if KML file cannot be parsed
     */
    private static XmlPullParser createXmlParser(InputStream stream) throws XmlPullParserException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(stream, null);
        return parser;
    }

    /**
     * Adds the KML data to the map
     */
    public void addLayerToMap() throws IOException, XmlPullParserException {
        mRenderer.addLayerToMap();
    }

    /**
     * Removes all the KML data from the map and clears all the stored placemarks
     */
    public void removeLayerFromMap() {
        mRenderer.removeLayerFromMap();
    }

    /**
     * Checks if the layer contains placemarks
     *
     * @return true if there are placemarks, false otherwise
     */

    public boolean hasPlacemarks() {
        return mRenderer.hasKmlPlacemarks();
    }

    /**
     * Gets an iterable of KmlPlacemark objects
     *
     * @return iterable of KmlPlacemark objects
     */
    public Iterable<KmlPlacemark> getPlacemarks() {
        return mRenderer.getKmlPlacemarks();
    }

    public boolean hasNetworkLinks() {
        return mRenderer.hasKmlNetworkLinks();
    }

    public Iterable<KmlNetworkLink> getNetworkLinks() {
        return mRenderer.getKmlNetworkLinks();
    }

    public HashSet<KmlNetworkLink> getNetworkLinksCollection() {
        HashSet<KmlNetworkLink> ret = new HashSet<>();

        Iterator<KmlNetworkLink> networkLinks =  getNetworkLinks().iterator();
        while (networkLinks.hasNext()){
            ret.add(networkLinks.next());
        }

        if (hasContainers()) {
            collectNetworkLinks(ret, getContainers());
        }
        return ret;
    }

    public void collectNetworkLinks(HashSet<KmlNetworkLink> collection, Iterable<KmlContainer> containers) {
        for (KmlContainer container : containers) {
            Iterator<KmlNetworkLink> networkLinks =  container.getNetworkLinks().iterator();
            while (networkLinks.hasNext()){
                collection.add(networkLinks.next());
            }
            if (container.hasContainers()) {
                collectNetworkLinks(collection, container.getContainers());
            }
        }
    }


    /**
     * Checks if the layer contains any KmlContainers
     *
     * @return true if there is at least 1 container within the KmlLayer, false otherwise
     */
    public boolean hasContainers() {
        return mRenderer.hasNestedContainers();
    }

    /**
     * Gets an iterable of KmlContainerInterface objects
     *
     * @return iterable of KmlContainerInterface objects
     */
    public Iterable<KmlContainer> getContainers() {
        return mRenderer.getNestedContainers();
    }

    /**
     * Gets the map that objects are being placed on
     *
     * @return map
     */
    public AtomMap getMap() {
        return mRenderer.getMap();
    }

    /**
     * Sets the map that objects are being placed on
     *
     * @param map map to place placemark, container, style and ground overlays on
     */
    public void setMap(AtomMap map) {
        mRenderer.setMap(map);
    }
}
