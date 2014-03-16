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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.search.core.text.TextSearchEngine;
import org.eclipse.search.core.text.TextSearchMatchAccess;
import org.eclipse.search.core.text.TextSearchRequestor;
import org.eclipse.search.ui.text.FileTextSearchScope;
import org.ucdetector.Log;
import org.ucdetector.UCDetectorPlugin;

/** Helper class that simulate Eclipse's "File search" dialogue. */
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

  public static List<SearchResult> searchTextSimpleInResource(String stringToSearch, IResource resource)
      throws CoreException {

    FileTextSearchScope scope = FileTextSearchScope.newSearchScope(new IResource[] { resource }, new String[] { "*" },
        false);
    Pattern searchPattern = Pattern.compile(Pattern.quote(stringToSearch));
    return search(scope, searchPattern);

  }

  public static List<SearchResult> searchTextSimple(String stringToSearch, String[] fileNamePattern)
      throws CoreException {

    FileTextSearchScope scope = FileTextSearchScope.newWorkspaceScope(fileNamePattern, false);
    Pattern searchPattern = Pattern.compile(Pattern.quote(stringToSearch));
    return search(scope, searchPattern);

  }

  public static List<SearchResult> searchTextRegularExpressionInResource(String stringToSearch, IResource resource)
      throws CoreException {
    FileTextSearchScope scope = FileTextSearchScope.newSearchScope(new IResource[] { resource }, new String[] { "*" },
        false);
    Pattern searchPattern = Pattern.compile(stringToSearch);
    return search(scope, searchPattern);
  }

  public static List<SearchResult> searchTextRegularExpression(String stringToSearch, String[] fileNamePattern)
      throws CoreException {
    FileTextSearchScope scope = FileTextSearchScope.newWorkspaceScope(fileNamePattern, false);
    Pattern searchPattern = Pattern.compile(stringToSearch);
    return search(scope, searchPattern);
  }

  private static List<SearchResult> search(FileTextSearchScope scope, Pattern searchPattern) throws CoreException {
    MatchedLocationRequestor requestor = new MatchedLocationRequestor();
    try {
      TextSearchEngine.createDefault().search(scope, requestor, searchPattern, null);
    }
    catch (OperationCanceledException e) {
      Log.info("Text search canceled");
    }
    catch (OutOfMemoryError e) {
      UCDetectorPlugin.handleOutOfMemoryError(e);
    }
    return requestor.matchedFiles;
  }

  private static final class MatchedLocationRequestor extends TextSearchRequestor {
    final List<SearchResult> matchedFiles = new ArrayList<SearchResult>();

    @Override
    public boolean acceptPatternMatch(TextSearchMatchAccess matchAccess) throws CoreException {
      int offset = matchAccess.getMatchOffset();
      int length = matchAccess.getMatchLength();
      int lineNumber = getLineNumber(offset, matchAccess);
      String path = matchAccess.getFile().getFullPath().toString();

      return matchedFiles.add(new SearchResult(path, offset, length, lineNumber));
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

  public static List<String> searchRegularExpressionReturnWithSelection(String stringToSearch, IResource resource)
      throws CoreException {

    FileTextSearchScope scope = FileTextSearchScope.newSearchScope(new IResource[] { resource }, new String[] { "*" },
        false);
    Pattern searchPattern = Pattern.compile(stringToSearch);
    MatchedSelectionRequestor requestor = new MatchedSelectionRequestor();
    try {
      TextSearchEngine.createDefault().search(scope, requestor, searchPattern, null);
    }
    catch (OperationCanceledException e) {
      Log.info("Text search canceled");
    }
    catch (OutOfMemoryError e) {
      UCDetectorPlugin.handleOutOfMemoryError(e);
    }
    return requestor.matchedSelection;

  }

  private static final class MatchedSelectionRequestor extends TextSearchRequestor {
    final List<String> matchedSelection = new ArrayList<String>();

    @Override
    public boolean acceptPatternMatch(TextSearchMatchAccess matchAccess) throws CoreException {
      int offset = matchAccess.getMatchOffset();
      int length = matchAccess.getMatchLength();

      return matchedSelection.add(matchAccess.getFileContent(offset, length));
    }
  }

}
