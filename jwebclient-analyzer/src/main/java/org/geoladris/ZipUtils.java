package org.geoladris;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

public class ZipUtils {
  private static final Logger logger = Logger.getLogger(ZipUtils.class);

  /**
   * Unzips all jar files from the given context, using {@link Context#getLibPaths()} to obtain the
   * paths and {@link Context#getLibAsStream(String)} to obtain the streams from the paths.
   * 
   * @param context The context containing the jar files.
   * @return The directory where the jar files have been unzipped.
   */
  public static File unzipJars(Context context) {
    File dir;
    try {
      dir = File.createTempFile("geoladris", "");
      dir.delete();
      dir.mkdirs();
    } catch (IOException e) {
      throw new RuntimeException("Cannot process plugins from jar files", e);
    }

    Set<String> jars = context.getLibPaths();
    for (String jar : jars) {
      ZipInputStream zis = new ZipInputStream(context.getLibAsStream(jar));
      try {
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
          String name = entry.getName();
          File file = new File(dir, name);
          if (name.endsWith("/")) {
            file.mkdirs();
            continue;
          }

          File parent = file.getParentFile();
          if (parent != null) {
            parent.mkdirs();
          }

          FileOutputStream fos = new FileOutputStream(file);
          IOUtils.copy(zis, fos);
          fos.close();
        }
      } catch (IOException e) {
        logger.warn("Error reading plugin from jar file: " + jar + ". Ignoring", e);
      } finally {
        try {
          zis.close();
        } catch (IOException e) {
        }
      }
    }

    return dir;
  }
}
