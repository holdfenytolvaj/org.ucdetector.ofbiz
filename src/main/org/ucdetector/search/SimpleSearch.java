/**
 * This program and the
 * accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ucdetector.search;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.search.core.text.TextSearchEngine;
import org.eclipse.search.core.text.TextSearchMatchAccess;
import org.eclipse.search.core.text.TextSearchRequestor;
import org.eclipse.search.ui.text.FileTextSearchScope;
import org.ucdetector.Log;
import org.ucdetector.UCDetectorPlugin;

public class SimpleSearch {

  public static final class SearchResult {
    public String path;
    public int offset;
    public int length;
    public int lineNumber;

    SearchResult(String path, int offset, int length, int lineNumber) {
      this.path = path;
      this.offset = offset;
      this.length = length;
      this.lineNumber = lineNumber;
    }
  }

  /**
   * The simplest search possible, same as "file search" in eclipse. 
   */
  @SuppressWarnings("javadoc")
  public static List<SearchResult> searchTextSimple(String stringToSearch, String[] fileNamePattern)
      throws CoreException {

    FileTextSearchScope scope = FileTextSearchScope.newWorkspaceScope(fileNamePattern, false);

    Pattern searchPattern = Pattern.compile(Pattern.quote(stringToSearch));
    SearchWithResultRequestor requestor = new SearchWithResultRequestor();
    try {
      TextSearchEngine.createDefault().search(scope, requestor, searchPattern, null);
    }
    catch (OperationCanceledException e) {
      Log.info("Text search canceled"); //$NON-NLS-1$
    }
    catch (OutOfMemoryError e) {
      UCDetectorPlugin.handleOutOfMemoryError(e);
    }
    return requestor.matchedFiles;
  }

  private static final class SearchWithResultRequestor extends TextSearchRequestor {
    final List<SearchResult> matchedFiles = new ArrayList<SearchResult>();

    SearchWithResultRequestor() {
    }

    @Override
    public boolean acceptPatternMatch(TextSearchMatchAccess matchAccess) throws CoreException {
      int offset = matchAccess.getMatchOffset();
      int length = matchAccess.getMatchLength();
      int lineNumber = getLineNumber(offset, matchAccess);
      String path = matchAccess.getFile().getFullPath().toString();
      matchedFiles.add(new SearchResult(path, offset, length, lineNumber));
      return true;
    }

    private static int getLineNumber(int offset, TextSearchMatchAccess matchAccess) {
      int lineNumber = 1;
      int i = 0;

      int contentLength = matchAccess.getFileContentLength();
      while (i < Math.min(offset, contentLength)) {
        if (matchAccess.getFileContentChar(i++) == '\n') {
          lineNumber++;
        }
      }
      return lineNumber;
    }
  }

  public static List<SearchResult> searchTextRegularExpression(String stringToSearch, String[] fileNamePattern)
      throws CoreException {
    FileTextSearchScope scope = FileTextSearchScope.newWorkspaceScope(fileNamePattern, false);

    Pattern searchPattern = Pattern.compile(stringToSearch);
    SearchWithResultRequestor requestor = new SearchWithResultRequestor();
    try {
      TextSearchEngine.createDefault().search(scope, requestor, searchPattern, null);
    }
    catch (OperationCanceledException e) {
      Log.info("Text search canceled"); //$NON-NLS-1$
    }
    catch (OutOfMemoryError e) {
      UCDetectorPlugin.handleOutOfMemoryError(e);
    }
    return requestor.matchedFiles;
  }
}
