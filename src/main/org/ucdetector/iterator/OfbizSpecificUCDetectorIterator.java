/**
 * This program and the
 * accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ucdetector.iterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.ucdetector.Messages;
import org.ucdetector.preferences.Prefs;
import org.ucdetector.search.OfbizSpecificSearchManager;
import org.ucdetector.search.OfbizSpecificXmlParser;
import org.ucdetector.search.UISearchProgressHelper;
import org.ucdetector.util.MarkerFactory;
import org.ucdetector.util.NonJavaIMember;
import org.ucdetector.util.ReferenceAndLocation;

/**
 * 
 * The main function is handleEndGlobal
 * 
 * It first collects information about services/screens etc (for more see scanOfbizProjectOrFolder)
 * Secondly it goes through it and identifies unused or not referenced items
 * 
 */
public class OfbizSpecificUCDetectorIterator extends AbstractUCDetectorIterator {

  private final Map<String, String> serviceMethodToNameMap = new HashMap<String, String>();
  private final Map<String, IResource> serviceNameAndFilePathMap = new HashMap<String, IResource>();
  private final Set<String> referencedServiceList = new HashSet<String>();

  private final Set<NonJavaIMember> ftlList = new HashSet<NonJavaIMember>();
  private final Set<String> referencedFtlList = new HashSet<String>();

  private final Set<NonJavaIMember> bshOrGroovyList = new HashSet<NonJavaIMember>();
  private final Set<String> referencedBshOrGroovyList = new HashSet<String>();

  private final List<IMethod> ofbizServiceList = new ArrayList<IMethod>();

  //controller-screen specific
  private final Set<String> referencedViewList = new HashSet<String>();
  private final Map<String, ReferenceAndLocation> viewDefinitionMap = new HashMap<String, ReferenceAndLocation>();
  private final Set<String> referencedScreenList = new HashSet<String>();
  private final Map<String, IResource> screenNameAndFilePathMap = new HashMap<String, IResource>();

  private final OfbizSpecificXmlParser helper = new OfbizSpecificXmlParser();

  private final int SCAN_WORKEFFORT = 20;
  private final int SEARCH_WORKEFFORT = 80;

  private int markerCreated;

  @Override
  public String getJobName() {
    return "Search for Ofbiz related elements";
  }

  public int getMarkerCreated() {
    return markerCreated;
  }

  @Override
  public void handleStartSelectedElement(IJavaElement javaElement) throws CoreException {
    MarkerFactory.deleteMarkers(javaElement);
  }

  @Override
  public void handleEndGlobal(IJavaElement[] objects) throws CoreException {
    getMonitor().beginTask(Messages.UCDetectorIterator_MONITOR_INFO, 100);

    if (objects.length > 0) {
      UIScanProgressHelper progressHelper = new UIScanProgressHelper(getMonitor(), SCAN_WORKEFFORT, /*guess*/
      SCAN_WORKEFFORT * 40);
      IProject project = objects[0].getCorrespondingResource().getProject();
      scanOfbizProjectOrFolder(project, progressHelper);
    }

    OfbizSpecificSearchManager searchManager = new OfbizSpecificSearchManager(getMarkerFactory());

    try {
      UISearchProgressHelper searchProgressHelper = new UISearchProgressHelper(getMonitor(), SEARCH_WORKEFFORT,
          getElelementsToDetectCount());

      searchProgressHelper.resetLocal(ofbizServiceList.size());
      searchManager.searchServices(ofbizServiceList, serviceMethodToNameMap, searchProgressHelper);

      searchProgressHelper.resetLocal(serviceNameAndFilePathMap.size());
      searchManager.searchServicesDefinitions(serviceNameAndFilePathMap, referencedServiceList, searchProgressHelper);

      searchProgressHelper.resetLocal(ftlList.size());
      searchManager.searchFtls(ftlList, referencedFtlList, searchProgressHelper);

      searchProgressHelper.resetLocal(bshOrGroovyList.size());
      searchManager.searchBshOrGroovyFiles(bshOrGroovyList, referencedBshOrGroovyList, searchProgressHelper);

      searchProgressHelper.resetLocal(viewDefinitionMap.size());
      searchManager.searchViews(viewDefinitionMap, referencedViewList, screenNameAndFilePathMap, searchProgressHelper);

      searchProgressHelper.resetLocal(screenNameAndFilePathMap.size());
      searchManager.searchScreens(screenNameAndFilePathMap, referencedScreenList, searchProgressHelper);

    }
    finally {
      markerCreated = searchManager.getMarkerCreated();
    }
  }

  /**
   * Only used to estimate "work effort" for eclipse progress dialogue.
   */
  @Override
  public int getElelementsToDetectCount() {
    return ofbizServiceList.size() + serviceNameAndFilePathMap.size() + ftlList.size() + bshOrGroovyList.size()
        + viewDefinitionMap.size() + screenNameAndFilePathMap.size();
  }

  /**
   * Extract service names / methods / screens etc. for faster searches
   * @throws CoreException 
   */
  private void scanOfbizProjectOrFolder(IContainer project, UIScanProgressHelper progressHelper) throws CoreException {
    List<IResource> resourceList = new ArrayList<IResource>(Arrays.asList(project.members(IContainer.EXCLUDE_DERIVED)));

    for (IResource resource : resourceList) {
      checkForCancel();

      if (resource.getType() == IResource.FOLDER) {
        progressHelper.showProgress();
        showStatus(resource);
        scanOfbizProjectOrFolder((IFolder) resource, progressHelper);
      }
      else if ("xml".equals(resource.getFileExtension())) {

        helper.extractDefinitionsFromXml(resource, serviceMethodToNameMap, serviceNameAndFilePathMap,
            referencedFtlList, referencedBshOrGroovyList, referencedServiceList, referencedViewList, viewDefinitionMap,
            referencedScreenList, screenNameAndFilePathMap);

      }
      else if ("ftl".equals(resource.getFileExtension())) {
        ftlList.add(new NonJavaIMember(resource));
        helper.searchForProgrammaticallyRenderedScreen(resource, referencedScreenList);
      }
      else if ("bsh".equals(resource.getFileExtension())) {
        bshOrGroovyList.add(new NonJavaIMember(resource));
        helper.searchForProgrammaticallyRenderedScreen(resource, referencedScreenList);
      }
      else if ("bsh".equals(resource.getFileExtension())) {
        helper.searchForProgrammaticallyRenderedScreen(resource, referencedScreenList);
      }
      else if ("groovy".equals(resource.getFileExtension())) {
        bshOrGroovyList.add(new NonJavaIMember(resource));
      }
    }
  }

  /** For eclipse's progress dialogue */
  @SuppressWarnings("javadoc")
  public final void showStatus(IResource resource) {
    getMonitor().subTask(String.format("Scanning %s", resource.getFullPath().toString()));
  }

  private void checkForCancel() {
    getMonitor().throwIfIsCanceled();
  }

  /**
   * Overridden method from AbstractUCDetectorIterator that goes through all Java elements and methods
   * and analyze them for usage. For us we only care about methods that might be Ofbiz services.
   */
  @Override
  protected void handleMethod(IMethod method) throws CoreException {
    if (!Prefs.isUCDetectionInMethods()) {
      debugNotHandle(method, "not isUCDetectionInMethods");
      return;
    }
    if (isOfbizService(method)) {
      ofbizServiceList.add(method);
    }
  }

  /** @return true if the method is static has two parameters namely a DispatchContext and a map */
  private static boolean isOfbizService(IMethod method) throws JavaModelException {
    if (isStatic(method) && isPublic(method)) {
      String[] paramTypes = method.getParameterTypes();
      if (paramTypes.length == 2
          && paramTypes[0].equals("QDispatchContext;")
          && (paramTypes[1].equals("QMap<QString;QObject;>;") || paramTypes[1].equals("QMap<QString;+QObject;>;")
              || paramTypes[1].equals("QMap<QString;*>;") || paramTypes[1].equals("QMap;")

          )) {
        return true;
      }
    }
    return false;
  }

  protected static final boolean isStatic(IMember member) throws JavaModelException {
    return Flags.isStatic(member.getFlags());
  }

}
