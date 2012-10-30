package org.jetbrains.jps.incremental;

import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.model.serialization.JpsProjectLoader;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * @author Eugene Zhuravlev
 *         Date: 10/20/11
 */
public class Utils {
  public static final Key<Map<ModuleBuildTarget, Collection<String>>> REMOVED_SOURCES_KEY = Key.create("_removed_sources_");
  public static final Key<Boolean> PROCEED_ON_ERROR_KEY = Key.create("_proceed_on_error_");
  public static final Key<Boolean> ERRORS_DETECTED_KEY = Key.create("_errors_detected_");
  private static volatile File ourSystemRoot = new File(System.getProperty("user.home"), ".idea-build");
  public static final boolean IS_TEST_MODE = Boolean.parseBoolean(System.getProperty("test.mode", "false"));
  public static final long TIMESTAMP_ACCURACY = SystemInfo.isMac ? 1000 : 1;

  private Utils() {
  }

  public static File getSystemRoot() {
    return ourSystemRoot;
  }

  public static void setSystemRoot(File systemRoot) {
    ourSystemRoot = systemRoot;
  }

  @Nullable
  public static File getDataStorageRoot(String projectPath) {
    return getDataStorageRoot(ourSystemRoot, projectPath);
  }

  public static File getDataStorageRoot(final File systemRoot, String projectPath) {
    projectPath = FileUtil.toCanonicalPath(projectPath);
    if (projectPath == null) {
      return null;
    }

    String name;
    final int locationHash;

    final File rootFile = new File(projectPath);
    if (!rootFile.isDirectory() && projectPath.endsWith(".ipr")) {
      name = StringUtil.trimEnd(rootFile.getName(), ".ipr");
      locationHash = projectPath.hashCode();
    }
    else {
      File directoryBased = null;
      if (".idea".equals(rootFile.getName())) {
        directoryBased = rootFile;
      }
      else {
        File child = new File(rootFile, ".idea");
        if (child.exists()) {
          directoryBased = child;
        }
      }
      if (directoryBased == null) {
        return null;
      }
      name = JpsProjectLoader.getDirectoryBaseProjectName(directoryBased);
      locationHash = directoryBased.getPath().hashCode();
    }

    return new File(systemRoot, name.toLowerCase(Locale.US) + "_" + Integer.toHexString(locationHash));
  }


  public static URI toURI(String localPath) {
    return toURI(localPath, true);
  }

  private static URI toURI(String localPath, boolean convertSpaces) {
    try {
      String p = FileUtil.toSystemIndependentName(localPath);
      if (!p.startsWith("/")) {
        p = "/" + p;
      }
      if (p.startsWith("//")) {
        p = "//" + p;
      }
      return new URI("file", null, convertSpaces? p.replaceAll(" ", "%20") : p, null);
    }
    catch (URISyntaxException e) {
      throw new Error(e);
    }
  }

  @Nullable
  public static File convertToFile(final URI uri) {
    if (uri == null) {
      return null;
    }
    final String path = uri.getPath();
    if (path == null) {
      return null;
    }
    return new File(toURI(path, false));
  }

  public static boolean intersects(Set<JpsModule> set1, Set<JpsModule> set2) {
    if (set1.size() < set2.size()) {
      return new HashSet<JpsModule>(set1).removeAll(set2);
    }
    return new HashSet<JpsModule>(set2).removeAll(set1);
  }

  public static boolean errorsDetected(CompileContext context) {
    return ERRORS_DETECTED_KEY.get(context, Boolean.FALSE);
  }

  public static String formatDuration(long duration) {
    final long minutes = duration / 60000;
    final long seconds = ((duration + 500L) % 60000) / 1000;
    if (minutes > 0L) {
      return minutes + " min " + seconds + " sec";
    }
    return seconds + " sec";
  }
}
