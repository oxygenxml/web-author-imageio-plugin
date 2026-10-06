package com.oxygenxml.imageio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.Test;

/**
 * Guards the vendored copy of {@code net.sf.jcgm.imageio.plugins.cgm.CGMImageReader}.
 *
 * <p>The plugin ships its own patched copy of that class (see the OXYGEN PATCH block in
 * src/main/java) and strips the original from jcgm-image at build time, so the copy is the
 * only one on the class path. Nothing in the build ties the two together: if jcgm-image is
 * upgraded and the upstream class changed, the copy would silently keep the old behaviour.
 *
 * <p>This test pins the upstream class. When it fails, diff the class in the new jcgm-image
 * against src/main/java/net/sf/jcgm/imageio/plugins/cgm/CGMImageReader.java, port the upstream
 * changes into the copy, keep the OXYGEN PATCH block, and update the digest below.
 */
public class VendoredCgmImageReaderTest {

  /** The class the vendored copy was last synchronized with: jcgm-image 0.2.2. */
  private static final String UPSTREAM_CLASS_SHA256 =
      "c000c9c15edece9f8070df689ceefbeea06d17d82f9e1bbac84e9f866c214e67";

  private static final String CLASS_ENTRY =
      "net/sf/jcgm/imageio/plugins/cgm/CGMImageReader.class";

  @Test
  public void testVendoredCopyIsInSyncWithJcgmImage() throws Exception {
    File jar = findJcgmImageJar();
    String actual;
    try (ZipFile zipFile = new ZipFile(jar)) {
      ZipEntry entry = zipFile.getEntry(CLASS_ENTRY);
      assertNotNull(CLASS_ENTRY + " not found in " + jar.getName(), entry);
      try (InputStream in = zipFile.getInputStream(entry)) {
        actual = sha256(in);
      }
    }
    assertEquals(
        "CGMImageReader changed in " + jar.getName() + ". The plugin ships a patched copy of it,"
            + " which now replaces a different upstream version. Port the upstream changes into"
            + " src/main/java/" + CLASS_ENTRY.replace(".class", ".java")
            + ", keep the OXYGEN PATCH block, then update UPSTREAM_CLASS_SHA256.",
        UPSTREAM_CLASS_SHA256, actual);
  }

  /**
   * The JAR as checked in, not the one in target/lib: the build strips CGMImageReader from that
   * copy, and lowers its class file version, so it can no longer be compared with upstream.
   */
  private static File findJcgmImageJar() {
    File libDir = new File("lib");
    File[] jars = libDir.listFiles(
        (dir, name) -> name.startsWith("jcgm-image-") && name.endsWith(".jar"));
    assertTrue("jcgm-image JAR not found in " + libDir.getAbsolutePath(),
        jars != null && jars.length > 0);
    assertEquals("Expected a single jcgm-image JAR in " + libDir.getAbsolutePath()
        + ", a leftover from a previous version would ship unprocessed", 1, jars.length);
    return jars[0];
  }

  private static String sha256(InputStream in) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] buffer = new byte[8192];
    int read;
    while ((read = in.read(buffer)) != -1) {
      digest.update(buffer, 0, read);
    }
    StringBuilder hex = new StringBuilder();
    for (byte b : digest.digest()) {
      hex.append(String.format("%02x", b));
    }
    return hex.toString();
  }
}
