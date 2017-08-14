package com.oxygenxml.imageio.workspace;

import javax.imageio.ImageIO;

import ro.sync.exml.plugin.workspace.WorkspaceAccessPluginExtension;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

public class ImageIOWorkspaceAccess  implements WorkspaceAccessPluginExtension {

  @Override
  public void applicationStarted(StandalonePluginWorkspace pluginWorkspaceAccess) {
    Thread currentThread = Thread.currentThread();
    ClassLoader contextClassLoader = currentThread.getContextClassLoader();
    currentThread.setContextClassLoader(this.getClass().getClassLoader());
    try {
      ImageIO.scanForPlugins();
    } finally {
      currentThread.setContextClassLoader(contextClassLoader);
    }
  }

  @Override
  public boolean applicationClosing() {
    return false;
  }
}
