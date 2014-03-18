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
  private final MarkerFactory markerFactory;

  private final List<IStatus> exceptionListDuringSearch = new ArrayList<IStatus>();

  public OfbizSpecificSearchManager(MarkerFactory markerFactory) {
    this.markerFactory = markerFactory;
    ReportParam.lineManager = lineManger;// Hack :-(
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
  @SuppressWarnings({ "javadoc" })
  public void searchServices(List<IMethod> serviceList, Map<String, String> serviceMethodToNameMap,
      UISearchProgressHelper progressHelper) throws CoreException {

    for (IMethod method : serviceList) {
      progressHelper.showProgress("service methods", markerCreated, exceptionListDuringSearch.size());

      int line = lineManger.getLine(method);
      if (line == LineManger.LINE_NOT_FOUND) {
        Log.debug("Ignore method " + method.getElementName());
        return;
      }

      if (!hasOfbizServiceReference(method, serviceMethodToNameMap)) {
        markerFactory.createReferenceMarkerOther(method, "[Ofbiz] Looks like a service but has no service definition!",
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
      Set<String> referencedServiceList, UISearchProgressHelper progressHelper) throws CoreException {

    for (String serviceName : serviceNameToFilePathMap.keySet()) {
      progressHelper.showProgress("service definitions", markerCreated, exceptionListDuringSearch.size());

      if (referencedServiceList.contains(serviceName)) {
        continue;
      }
      List<SearchResult> resultList = SimpleSearch.searchTextSimple("\"" + serviceName + "\"", new String[] { "*.xml",
          "*.bsh", "*.java" });
      if (resultList.size() == 1) {
        SearchResult sr = resultList.get(0);
        NonJavaIMember serviceDefinitionMember = new NonJavaIMember(serviceNameToFilePathMap.get(serviceName),
            sr.offset, sr.length, sr.lineNumber);
        markerFactory.createReferenceMarker(serviceDefinitionMember, "[Ofbiz] The service \"" + serviceName
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
  public void searchFtls(Set<NonJavaIMember> ftlList, Set<String> referencedFtlList,
      UISearchProgressHelper progressHelper) throws CoreException {

    for (NonJavaIMember ftlMember : ftlList) {
      progressHelper.showProgress("ftls", markerCreated, exceptionListDuringSearch.size());

      if (!referencedFtlList.contains(ftlMember.getPathToFile())) {

        //second chance search for include statement (it would be probably faster to read the ftl files
        //earlier, but that would add a lot of complexity and it is not super important)
        boolean isIncluded = SimpleSearch.searchTextRegularExpression(
            "<#(include|import) \".*" + ftlMember.getFileName() + "\"", new String[] { "*.ftl" }).size() > 0;

        if (!isIncluded) {
          markerFactory.createReferenceMarker(ftlMember,
              "[Ofbiz] This file is not referenced from screen definitions neither included from other files!", 1, 0);
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
  public void searchBshOrGroovyFiles(Set<NonJavaIMember> bshOrGroovyList, Set<String> referencedBshOrGroovyList,
      UISearchProgressHelper progressHelper) throws CoreException {

    for (NonJavaIMember bshOrGroovyMember : bshOrGroovyList) {
      progressHelper.showProgress("bsh/groovy files", markerCreated, exceptionListDuringSearch.size());

      if (!referencedBshOrGroovyList.contains(bshOrGroovyMember.getPathToFile())) {
        markerFactory.createReferenceMarker(bshOrGroovyMember,
            "[Ofbiz] This file is not referenced from screen definitions!", 1, 0);
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
      Map<String, IResource> screenNameAndFilePathMap, UISearchProgressHelper progressHelper) throws CoreException {

    //view references
    for (String viewName : viewDefinitionMap.keySet()) {
      progressHelper.showProgress("views", markerCreated, exceptionListDuringSearch.size());

      if (!referencedViewList.contains(viewName)) {
        NonJavaIMember viewIMember = getNonJavaIMemberForViewDefinition(viewName,
            viewDefinitionMap.get(viewName).location);
        markerFactory.createReferenceMarker(viewIMember, "[Ofbiz] This view is not referenced!",
            viewIMember.getLineNumber(), 0);
        markerCreated++;

        Log.info("View: " + viewName + " is not used");
      }

      if (!screenNameAndFilePathMap.containsKey(viewDefinitionMap.get(viewName).referencedItem)) {
        NonJavaIMember viewIMember = getNonJavaIMemberForViewDefinition(viewName,
            viewDefinitionMap.get(viewName).location);
        markerFactory.createReferenceMarker(viewIMember, "[Ofbiz] The screen of this view does not exists!",
            viewIMember.getLineNumber(), 0);
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
      return new NonJavaIMember(location, sr.offset, sr.length, sr.lineNumber);
    }

    //we only search for name="viewName" in theory it is enough in the controller
    resultList = SimpleSearch.searchTextSimpleInResource("name=\"" + viewName + "\"", location);
    if (resultList.size() == 1) {
      SearchResult sr = resultList.get(0);
      return new NonJavaIMember(location, sr.offset, sr.length, sr.lineNumber);
    }

    //giving up for now
    Log.error("view " + viewName + " is not found in " + location.getFullPath().toString());
    return null;

  }

  public void searchScreens(Map<String, IResource> screenNameAndFilePathMap, Set<String> referencedScreenList,
      UISearchProgressHelper progressHelper) throws CoreException {

    for (String screenName : screenNameAndFilePathMap.keySet()) {
      progressHelper.showProgress("screens", markerCreated, exceptionListDuringSearch.size());

      if (!referencedScreenList.contains(screenName)) {
        markerFactory.createReferenceMarker(
            getNonJavaIMemberForScreenDefinition(screenName, screenNameAndFilePathMap.get(screenName)),
            "[Ofbiz] This screen is not referenced from anywhere!", 1, 0);
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
      return new NonJavaIMember(location, sr.offset, sr.length, sr.lineNumber);
    }

    //giving up for now
    Log.error("screen definition for " + screenName + " is not found in " + location.getFullPath().toString());
    return null;

  }
}
