/**
 * This program and the
 * accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ucdetector.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IMethod;
import org.ucdetector.Log;
import org.ucdetector.UCDInfo;
import org.ucdetector.report.ReportParam;
import org.ucdetector.search.SimpleSearch.SearchResult;
import org.ucdetector.util.MarkerFactory;
import org.ucdetector.util.NonJavaIMember;
import org.ucdetector.util.ReferenceAndLocation;

/**
 * When the data collection is done, this class will analyze the "items" (service/screen/bsh/ftl etc) 
 * and create markers when necessary 
 */
public class OfbizSpecificSearchManager /*extends SearchManager*/{

  private int markerCreated = 0;
  private final LineManger lineManger = new LineManger();
  private final UCDProgressMonitor monitor;
  private final int globalSearchTotal;
  private int globalSearchCurrentPosition = 0;
  private final MarkerFactory markerFactory;

  private final List<IStatus> exceptionListDuringSearch = new ArrayList<IStatus>();

  public OfbizSpecificSearchManager(UCDProgressMonitor monitor, int searchTotal, MarkerFactory markerFactory) {
    this.monitor = monitor;
    this.globalSearchTotal = searchTotal;
    this.markerFactory = markerFactory;
    ReportParam.lineManager = lineManger;// Hack :-(
  }

  @SuppressWarnings("boxing")
  public final void showProgress(String type, int localSearchCurrentPosition, int localSearchTotal) {
    globalSearchCurrentPosition++;

    if (localSearchCurrentPosition == 1 || localSearchCurrentPosition % 10 == 0
        || localSearchCurrentPosition == localSearchTotal) {
      String msg = String.format("Search %4s/%4s %s. Markers %4s. Exceptions %2s. Total %4s/%4s",
          localSearchCurrentPosition, localSearchTotal, type, markerCreated, exceptionListDuringSearch.size(),
          globalSearchCurrentPosition, globalSearchTotal);
      Log.info(msg + " - " + UCDInfo.getNow(true)); //adding timestamp
      monitor.subTask(msg);
    }
  }

  private void checkForCancel() {
    monitor.throwIfIsCanceled();
  }

  public int getMarkerCreated() {
    return markerCreated;
  }

  private static boolean hasOfbizServiceReference(IMethod method, Map<String, String> serviceMethodToNameMap) {
    return getOfbizServiceName(method, serviceMethodToNameMap) != null;
  }

  private static String getOfbizServiceName(IMethod method, Map<String, String> serviceMethodToNameMap) {
    String fullName = method.getDeclaringType().getFullyQualifiedName() + "." + method.getElementName();
    return serviceMethodToNameMap.get(fullName);
  }

  /**
   * Check whether methods that looks like a service, has service definition or not.
   */
  @SuppressWarnings({ "javadoc", "boxing" })
  public void searchServices(List<IMethod> serviceList, Map<String, String> serviceMethodToNameMap, int workEffort)
      throws CoreException {
    int effortUnit = ((Double) Math.ceil(Double.valueOf(serviceList.size()) / workEffort)).intValue();
    int currentProgress = 0;

    for (IMethod method : serviceList) {
      if (currentProgress % effortUnit == 0) {
        monitor.internalWorked(1);
      }

      int line = lineManger.getLine(method);
      if (line == LineManger.LINE_NOT_FOUND) {
        Log.debug("Ignore method " + method.getElementName());
        return;
      }

      if (!hasOfbizServiceReference(method, serviceMethodToNameMap)) {
        markerFactory.createReferenceMarkerOther(method, "[OfBiz] Looks like a service but has no service definition!",
            line);
        markerCreated++;
      }
    }
  }

  /**
   * Check whether the service is
   * - referenced from a controller/seca 
   * - called from anywhere 
   */
  @SuppressWarnings("javadoc")
  public void searchServicesDefinitions(Map<String, IResource> serviceNameToFilePathMap,
      Set<String> referencedServiceList) throws CoreException {
    int localSearchCurrentPosition = 0;
    int localSearchTotal = serviceNameToFilePathMap.keySet().size();

    for (String serviceName : serviceNameToFilePathMap.keySet()) {
      checkForCancel();
      showProgress("Service Definitions", ++localSearchCurrentPosition, localSearchTotal);

      if (referencedServiceList.contains(serviceName)) {
        continue;
      }
      List<SearchResult> resultList = SimpleSearch.searchTextSimple("\"" + serviceName + "\"", new String[] { "*.xml",
          "*.bsh", "*.java" });
      if (resultList.size() == 1) {
        SearchResult sr = resultList.get(0);
        NonJavaIMember serviceDefinitionMember = new NonJavaIMember(serviceNameToFilePathMap.get(serviceName),
            sr.offset, sr.length);
        markerFactory.createReferenceMarker(serviceDefinitionMember, "[OfBiz] The service \"" + serviceName
            + "\" is not called from anywhere!", sr.lineNumber, 0);
        markerCreated++;

        Log.info("Service: " + serviceName + " is not used");
      }
    }
  }

  /**
   * Check whether the ftl file 
   * - is referenced from a screen
   * - included/imported from another ftl
   */
  @SuppressWarnings("javadoc")
  public void searchFtls(Set<NonJavaIMember> ftlList, Set<String> referencedFtlList) throws CoreException {
    int localSearchCurrentPosition = 0;
    int localSearchTotal = ftlList.size();

    for (NonJavaIMember ftlMember : ftlList) {
      checkForCancel();
      showProgress("ftls", ++localSearchCurrentPosition, localSearchTotal);

      if (!referencedFtlList.contains(ftlMember.getPathToFile())) {

        //second chance search for include statement (it would be probably faster to read the ftl files
        //earlier, but that would add a lot of complexity and it is not super important)
        boolean isIncluded = SimpleSearch.searchTextRegularExpression(
            "<#(include|import) \".*" + ftlMember.getFileName() + "\"", new String[] { "*.ftl" }).size() > 0;

        if (!isIncluded) {
          markerFactory.createReferenceMarker(ftlMember,
              "[OfBiz] This file is not referenced from screen definitions neither included from other files!", 1, 0);
          markerCreated++;

          Log.info("Ftl: " + ftlMember.getPathToFile() + " is not used");
        }
      }
    }
  }

  /**
   * Check whether the bsh/groovy file is 
   * - referenced from a screen
   */
  @SuppressWarnings("javadoc")
  public void searchBshOrGroovyFiles(Set<NonJavaIMember> bshOrGroovyList, Set<String> referencedBshOrGroovyList)
      throws CoreException {
    int localSearchCurrentPosition = 0;
    int localSearchTotal = bshOrGroovyList.size();

    for (NonJavaIMember bshOrGroovyMember : bshOrGroovyList) {
      checkForCancel();
      showProgress("ftls", ++localSearchCurrentPosition, localSearchTotal);

      if (!referencedBshOrGroovyList.contains(bshOrGroovyMember.getPathToFile())) {
        markerFactory.createReferenceMarker(bshOrGroovyMember,
            "[OfBiz] This file is not referenced from screen definitions!", 1, 0);
        markerCreated++;

        Log.info("Bsh: " + bshOrGroovyMember.getPathToFile() + " is not used");
      }
    }
  }

  /**
   * Check whether 
   * - a view is referenced from a controller
   * - the screen that is referenced from the view exists
   * @throws CoreException 
   */
  @SuppressWarnings({ "javadoc" })
  public void searchViews(Map<String, ReferenceAndLocation> viewDefinitionMap, Set<String> referencedViewList,
      Map<String, IResource> screenNameAndFilePathMap) throws CoreException {
    int localSearchCurrentPosition = 0;
    int localSearchTotal = viewDefinitionMap.size();

    //view references
    for (String viewName : viewDefinitionMap.keySet()) {
      checkForCancel();
      showProgress("views", ++localSearchCurrentPosition, localSearchTotal);

      if (!referencedViewList.contains(viewName)) {
        markerFactory.createReferenceMarker(
            getNonJavaIMemberForViewDefinition(viewName, viewDefinitionMap.get(viewName).location),
            "[OfBiz] This view is not referenced!", 1, 0);
        markerCreated++;

        Log.info("View: " + viewName + " is not used");
      }

      if (!screenNameAndFilePathMap.containsKey(viewDefinitionMap.get(viewName).referencedItem)) {
        markerFactory.createReferenceMarker(
            getNonJavaIMemberForViewDefinition(viewName, viewDefinitionMap.get(viewName).location),
            "[OfBiz] The screen of this view does not exists!", 1, 0);
        markerCreated++;

        Log.info("View's screen: " + viewDefinitionMap.get(viewName).referencedItem + " is not used");
      }
    }
  }

  /**
   * viewName is sth like pathToFile#actualNameOfView
   */
  private static NonJavaIMember getNonJavaIMemberForViewDefinition(String viewNameFull, IResource location)
      throws CoreException {
    String viewName = viewNameFull.substring(viewNameFull.indexOf('#') + 1);
    List<SearchResult> resultList = SimpleSearch.searchTextRegularExpressionInResource("<view-map\\s*name=\""
        + viewName + "\"", location);

    //easy case it was put as a one-liner
    if (resultList.size() == 1) {
      SearchResult sr = resultList.get(0);
      return new NonJavaIMember(location, sr.offset, sr.length);
    }

    //we only search for name="viewName" in theory it is enough in the controller
    resultList = SimpleSearch.searchTextSimpleInResource("name=\"" + viewName + "\"", location);
    if (resultList.size() == 1) {
      SearchResult sr = resultList.get(0);
      return new NonJavaIMember(location, sr.offset, sr.length);
    }

    //giving up for now
    Log.error("view " + viewName + " is not found in " + location.getFullPath().toString());
    return null;

  }

  public void searchScreens(Map<String, IResource> screenNameAndFilePathMap, Set<String> referencedScreenList)
      throws CoreException {
    int localSearchCurrentPosition = 0;
    int localSearchTotal = screenNameAndFilePathMap.size();

    for (String screenName : screenNameAndFilePathMap.keySet()) {
      checkForCancel();
      showProgress("screens", ++localSearchCurrentPosition, localSearchTotal);

      if (!referencedScreenList.contains(screenName)) {
        markerFactory.createReferenceMarker(
            getNonJavaIMemberForScreenDefinition(screenName, screenNameAndFilePathMap.get(screenName)),
            "[OfBiz] This screen is not referenced from anywhere!", 1, 0);
        markerCreated++;

        Log.info("Screen: " + screenName + " is not used");
      }
    }
  }

  /**
   * viewName is sth like pathToFile#actualNameOfView
   */
  private static NonJavaIMember getNonJavaIMemberForScreenDefinition(String screenNameFull, IResource location)
      throws CoreException {
    String screenName = screenNameFull.substring(screenNameFull.indexOf('#') + 1);
    List<SearchResult> resultList = SimpleSearch.searchTextRegularExpressionInResource("<screen\\s*name=\""
        + screenName + "\"", location);

    //easy case it was put as a one-liner
    if (resultList.size() == 1) {
      SearchResult sr = resultList.get(0);
      return new NonJavaIMember(location, sr.offset, sr.length);
    }

    //giving up for now
    Log.error("screen definition for " + screenName + " is not found in " + location.getFullPath().toString());
    return null;

  }
}
