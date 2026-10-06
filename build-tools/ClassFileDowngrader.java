import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Rewrites the class file version of every class in a JAR.
 *
 * <p>jcgm-image is published without a source/target setting in its Ant build, so its class
 * file version follows whatever JDK the maintainer used: 0.1.1 is Java 8, 0.2.2 is Java 21.
 * Web Author requires only Java 17, where the Java 21 classes cannot be loaded at all: the
 * CGM reader SPI fails with UnsupportedClassVersionError and no CGM image is rendered.
 *
 * <p>The library itself uses no API or language feature newer than Java 17, so lowering the
 * version stamp is enough.
 *
 * <p>Usage: {@code ClassFileDowngrader <in.jar> <out.jar> <targetMajor> <maxAcceptedMajor>}
 */
public final class ClassFileDowngrader {

  private static final int MAJOR_VERSION_OFFSET = 6;

  public static void main(String[] args) throws IOException {
    if (args.length < 4) {
      throw new IllegalArgumentException(
          "Usage: ClassFileDowngrader <in.jar> <out.jar> <targetMajor> <maxAcceptedMajor> [excludedEntry...]");
    }
    File input = new File(args[0]);
    File output = new File(args[1]);
    int targetMajor = Integer.parseInt(args[2]);
    int maxAcceptedMajor = Integer.parseInt(args[3]);
    String[] excluded = new String[args.length - 4];
    System.arraycopy(args, 4, excluded, 0, excluded.length);

    int downgraded = 0;
    File parent = output.getParentFile();
    if (parent != null) {
      Files.createDirectories(parent.toPath());
    }
    try (ZipFile in = new ZipFile(input);
        ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(output.toPath()))) {
      for (ZipEntry entry : java.util.Collections.list(in.entries())) {
        if (isExcluded(entry.getName(), excluded)) {
          continue;
        }
        byte[] content = readAll(in.getInputStream(entry));
        if (entry.getName().endsWith(".class")) {
          downgraded += downgrade(entry.getName(), content, targetMajor, maxAcceptedMajor);
        }
        ZipEntry copy = new ZipEntry(entry.getName());
        copy.setTime(entry.getTime());
        out.putNextEntry(copy);
        out.write(content);
        out.closeEntry();
      }
    }
    System.out.println("ClassFileDowngrader: " + downgraded + " class(es) set to major version "
        + targetMajor + " in " + output.getName());
  }

  /**
   * Lowers the class file version in place.
   *
   * @return 1 if the class was rewritten, 0 if it was already at or below the target.
   */
  private static int downgrade(String name, byte[] content, int targetMajor, int maxAcceptedMajor) {
    int major = ((content[MAJOR_VERSION_OFFSET] & 0xFF) << 8) | (content[MAJOR_VERSION_OFFSET + 1] & 0xFF);
    if (major <= targetMajor) {
      return 0;
    }
    if (major > maxAcceptedMajor) {
      // Only versions we have checked for post-target language features may be rewritten:
      // the stamp is a claim, not a verification, so a newer compiler needs a human look.
      throw new IllegalStateException(name + " is class file version " + major
          + ", above the reviewed maximum of " + maxAcceptedMajor
          + ". Confirm the sources are still compatible with major " + targetMajor
          + " before raising it.");
    }
    content[MAJOR_VERSION_OFFSET] = (byte) ((targetMajor >> 8) & 0xFF);
    content[MAJOR_VERSION_OFFSET + 1] = (byte) (targetMajor & 0xFF);
    return 1;
  }

  private static boolean isExcluded(String name, String[] excluded) {
    for (String e : excluded) {
      if (name.equals(e)) {
        return true;
      }
    }
    return false;
  }

  private static byte[] readAll(InputStream in) throws IOException {
    try (InputStream stream = in) {
      return stream.readAllBytes();
    }
  }
}
