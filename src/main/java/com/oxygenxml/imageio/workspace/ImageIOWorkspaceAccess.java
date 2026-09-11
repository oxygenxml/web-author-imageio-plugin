package com.oxygenxml.imageio.workspace;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

public class ImageIOWorkspaceAccess  implements WorkspaceAccessPluginExtension {

  private static final Logger logger = Logger.getLogger(ImageIOWorkspaceAccess.class.getName());

  public void applicationStarted(StandalonePluginWorkspace pluginWorkspaceAccess) {
    Thread currentThread = Thread.currentThread();
    ClassLoader contextClassLoader = currentThread.getContextClassLoader();
    currentThread.setContextClassLoader(this.getClass().getClassLoader());
    try {
      ImageIO.scanForPlugins();
    } catch (Throwable t) {
      // A reader that cannot be instantiated, for example one compiled for a newer Java
      // version than the one running Web Author, aborts the whole scan: the readers after it
      // stay unregistered. Report it and let Web Author start with the formats it did get,
      // rather than failing the plugin.
      logger.log(Level.SEVERE, "Some image formats could not be registered: " + t, t);
    } finally {
      currentThread.setContextClassLoader(contextClassLoader);
    }
    logAvailableFormats();
  }

  /**
   * Logs the file extensions Web Author can render, so a format missing because of a failed
   * scan can be told apart from one this plugin never supported.
   */
  private void logAvailableFormats() {
    try {
      String[] suffixes = ImageIO.getReaderFileSuffixes();
      Arrays.sort(suffixes);
      logger.info("ImageIO readers available for: " + String.join(", ", suffixes));
    } catch (Throwable t) {
      // If the failure above happened while ImageIO itself was being initialized, the class
      // stays unusable for the whole JVM and even listing the formats fails.
      logger.log(Level.SEVERE, "ImageIO is not usable, no image format can be rendered: " + t, t);
    }
  }

  public boolean applicationClosing() {
    return false;
  }
}
