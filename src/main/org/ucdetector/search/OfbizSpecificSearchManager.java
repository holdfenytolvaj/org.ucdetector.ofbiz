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

/**
 * Extends the basic search for 
 * - services
 * - ftl files
 * - bsh files
 * TODO: xml definitions
 */
public class OfbizSpecificSearchManager /*extends SearchManager*/{
  private Map<String, String> serviceDefinitionNameMap;
  private Set<String> referencedFtlList;
  private Set<String> referencedBshList;
  private Set<String> referencedServiceList;

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
      String msg = String.format(
          "Search %4s/%4s %s. Markers %4s. Exceptions %2s. Total %4s/%4s", //$NON-NLS-1$
          localSearchCurrentPosition, localSearchTotal, type, markerCreated, exceptionListDuringSearch.size(),
          globalSearchCurrentPosition, globalSearchTotal);
      Log.info(msg + " - " + UCDInfo.getNow(true));//$NON-NLS-1$ - adding timestamp
      monitor.subTask(msg);
    }
  }

  private void checkForCancel() {
    monitor.throwIfIsCanceled();
  }

  public int getMarkerCreated() {
    return markerCreated;
  }

  private boolean hasOfbizServiceReference(IMethod method) {
    return getOfbizServiceName(method) != null;
  }

  private String getOfbizServiceName(IMethod method) {
    String fullName = method.getDeclaringType().getFullyQualifiedName() + "." + method.getElementName(); //$NON-NLS-1$
    return serviceDefinitionNameMap.get(fullName);
  }

  @SuppressWarnings("hiding")
  public void init(Map<String, String> serviceDefinitionNameMap, Set<String> referencedFtlList,
      Set<String> referencedBshList, Set<String> referencedServiceList) {
    this.serviceDefinitionNameMap = serviceDefinitionNameMap;
    this.referencedFtlList = referencedFtlList;
    this.referencedBshList = referencedBshList;
    this.referencedServiceList = referencedServiceList;
  }

  @SuppressWarnings("boxing")
  public void searchServices(List<IMethod> serviceList, int workEffort) throws CoreException {
    int effortUnit = ((Double) Math.ceil(Double.valueOf(serviceList.size()) / workEffort)).intValue();
    int currentProgress = 0;

    for (IMethod method : serviceList) {
      if (currentProgress % effortUnit == 0) {
        monitor.internalWorked(1);
      }

      int line = lineManger.getLine(method);
      if (line == LineManger.LINE_NOT_FOUND) {
        Log.debug("Ignore method " + method.getElementName()); //$NON-NLS-1$
        return;
      }

      if (!hasOfbizServiceReference(method)) {
        markerFactory.createReferenceMarkerOther(method,
            "[OfBiz] Looks like a service but has no service definition!", line); //$NON-NLS-1$
        markerCreated++;
      }
    }
  }

  public void searchServicesDefinitions(Map<String, IResource> serviceNameToFilePathMap) throws CoreException {
    int localSearchCurrentPosition = 0;
    int localSearchTotal = serviceNameToFilePathMap.keySet().size();

    for (String serviceName : serviceNameToFilePathMap.keySet()) {
      checkForCancel();
      showProgress("Service Definitions", ++localSearchCurrentPosition, localSearchTotal);//$NON-NLS-1$

      if (referencedServiceList.contains(serviceName)) {
        continue;
      }
      List<SearchResult> resultList = SimpleSearch.searchTextSimple("\"" + serviceName + "\"", new String[] {
          "*.xml", "*.bsh", "*.java" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      if (resultList.size() == 1) {
        SearchResult sr = resultList.get(0);
        NonJavaIMember serviceDefinitionMember = new NonJavaIMember(serviceNameToFilePathMap.get(serviceName),
            sr.offset, sr.length);
        markerFactory.createReferenceMarker(serviceDefinitionMember,
            "[OfBiz] The service \"" + serviceName + "\" is not called from anywhere!", sr.lineNumber, 0);//$NON-NLS-1$
        markerCreated++;

        Log.info("Service: " + serviceName + " is not used"); //$NON-NLS-1$//$NON-NLS-2$
      }
    }
  }

  public void searchFtl(Set<NonJavaIMember> ftlList) throws CoreException {
    int localSearchCurrentPosition = 0;
    int localSearchTotal = ftlList.size();

    for (NonJavaIMember ftlMember : ftlList) {
      checkForCancel();
      showProgress("ftls", ++localSearchCurrentPosition, localSearchTotal);//$NON-NLS-1$

      if (!referencedFtlList.contains(ftlMember.getPathToFile())) {

        //second chance search for include statement (it would be probably faster to read the ftl files
        //earlier, but that would add a lot of complexity and it is not super important)
        boolean isIncluded = SimpleSearch.searchTextRegularExpression(
            "<#(include|import) \".*" + ftlMember.getFileName() + "\"", new String[] { "*.ftl" }).size() > 0;

        if (!isIncluded) {
          markerFactory.createReferenceMarker(ftlMember,
              "[OfBiz] This file is not referenced from screen definitions neither included from other files!", 1, 0);//$NON-NLS-1$
          markerCreated++;

          Log.info("Ftl: " + ftlMember.getPathToFile() + " is not used"); //$NON-NLS-1$//$NON-NLS-2$
        }
      }
    }
  }

  public void searchBsh(Set<NonJavaIMember> bshList) throws CoreException {
    int localSearchCurrentPosition = 0;
    int localSearchTotal = bshList.size();

    for (NonJavaIMember bshMember : bshList) {
      checkForCancel();
      showProgress("ftls", ++localSearchCurrentPosition, localSearchTotal);//$NON-NLS-1$

      if (!referencedBshList.contains(bshMember.getPathToFile())) {
        markerFactory.createReferenceMarker(bshMember,
            "[OfBiz] This file is not referenced from screen definitions!", 1, 0);//$NON-NLS-1$
        markerCreated++;

        Log.info("Bsh: " + bshMember.getPathToFile() + " is not used"); //$NON-NLS-1$//$NON-NLS-2$
      }
    }
  }

}
