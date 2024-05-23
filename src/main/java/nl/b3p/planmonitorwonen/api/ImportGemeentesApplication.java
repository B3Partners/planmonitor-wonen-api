package nl.b3p.planmonitorwonen.api;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;

public class ImportGemeentesApplication {
  public static void main(String[] args) throws IOException {
    System.out.println("Loading gemeentes from WFS...");
    String sql = getGemeentesSql();
    new OutputStreamWriter(new FileOutputStream("gemeentes.sql"), UTF_8).append(sql).close();
    System.out.println("Done");
  }

  public static String getGemeentesSql() throws IOException {
    String wfs = "https://service.pdok.nl/kadaster/bestuurlijkegebieden/wfs/v1_0";
    String typename = "bestuurlijkegebieden:Gemeentegebied";

    Map<String, Object> params = new HashMap<>();
    params.put(WFSDataStoreFactory.URL.key, wfs);
    DataStore ds = DataStoreFinder.getDataStore(params);

    SimpleFeatureSource fs = ds.getFeatureSource(typename);
    StringWriter sw = new StringWriter();
    try (SimpleFeatureIterator features = fs.getFeatures().features();
        PrintWriter pw = new PrintWriter(sw)) {
      while (features.hasNext()) {
        SimpleFeature feature = features.next();

        String sql =
            "insert into gemeente(identificatie, naam, provincie, geometry) values('%s', '%s', '%s', '%s');\n";
        Geometry g = (Geometry) feature.getDefaultGeometry();
        g = TopologyPreservingSimplifier.simplify(g, 50);
        g = new GeometryPrecisionReducer(new PrecisionModel(1.0)).reduce(g);
        String wkb = WKBWriter.toHex(new WKBWriter().write(g)).trim();
        pw.printf(
            sql,
            escapeSql(feature.getAttribute("identificatie")),
            escapeSql(feature.getAttribute("naam")),
            escapeSql(feature.getAttribute("ligtInProvincieNaam")),
            wkb);
      }
    }

    return sw.toString();
  }

  private static String escapeSql(Object s) {
    return s.toString().replaceAll("'", "''");
  }
}
