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
import org.ucdetector.search.OfBizSpecificXmlParser;
import org.ucdetector.search.OfbizSpecificSearchManager;
import org.ucdetector.util.MarkerFactory;
import org.ucdetector.util.NonJavaIMember;
import org.ucdetector.util.ReferenceAndLocation;

/**
 * 
 * The main function is handleEndGlobal
 * 
 * It first collects information about services/screens etc (for more see scanOfBizProjectOrFolder)
 * Secondly it goes through it and identifies unused or not referenced items
 * 
 */
public class OfBizSpecificUCDetectorIterator extends AbstractUCDetectorIterator {

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

  private final OfBizSpecificXmlParser helper = new OfBizSpecificXmlParser();

  private final int SCAN_WORKEFFORT = 20;
  private final int SERVICE_METHOD_WORKEFFORT = 50;
  private final int SERVICE_DEF_WORKEFFORT = 20;
  private final int FTL_WORKEFFORT = 5;
  private final int BSH_WORKEFFORT = 5;

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
    getMonitor().internalWorked(1);

    if (objects.length > 0) {
      IProject project = objects[0].getCorrespondingResource().getProject();
      scanOfBizProjectOrFolder(project);
      getMonitor().internalWorked(SCAN_WORKEFFORT);
    }

    int totalSize = getElelementsToDetectCount();
    OfbizSpecificSearchManager searchManager = new OfbizSpecificSearchManager(getMonitor(), totalSize,
        getMarkerFactory());

    try {
      searchManager.searchServices(ofbizServiceList, serviceMethodToNameMap, SERVICE_METHOD_WORKEFFORT);
      searchManager.searchServicesDefinitions(serviceNameAndFilePathMap, referencedServiceList);
      getMonitor().internalWorked(SERVICE_DEF_WORKEFFORT);

      searchManager.searchFtls(ftlList, referencedFtlList);
      getMonitor().internalWorked(FTL_WORKEFFORT);

      searchManager.searchBshOrGroovyFiles(bshOrGroovyList, referencedBshOrGroovyList);
      getMonitor().internalWorked(BSH_WORKEFFORT);

      searchManager.searchViews(viewDefinitionMap, referencedViewList, screenNameAndFilePathMap);
      searchManager.searchScreens(screenNameAndFilePathMap, referencedScreenList);

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
    int result = 0;
    result += bshOrGroovyList.size() + ftlList.size() + ofbizServiceList.size() + serviceMethodToNameMap.size();
    return result;
  }

  /**
   * Extract service names / methods / screens etc. for faster searches
   * @throws CoreException 
   */
  private void scanOfBizProjectOrFolder(IContainer project) throws CoreException {
    List<IResource> resourceList = new ArrayList<IResource>(Arrays.asList(project.members(IContainer.EXCLUDE_DERIVED)));

    for (IResource resource : resourceList) {
      checkForCancel();

      if (resource.getType() == IResource.FOLDER) {
        showProgress(resource);
        scanOfBizProjectOrFolder((IFolder) resource);
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
  public final void showProgress(IResource resource) {
    String msg = String.format("Scanning %s", resource.getFullPath().toString());
    getMonitor().subTask(msg);
  }

  private void checkForCancel() {
    getMonitor().throwIfIsCanceled();
  }

  /**
   * Overrriden method from AbstractUCDetectorIterator that goes through all Java elements and methods
   * and analyze them for usage. For us we only care about methods that might be ofbiz services.
   */
  @Override
  protected void handleMethod(IMethod method) throws CoreException {
    if (!Prefs.isUCDetectionInMethods()) {
      debugNotHandle(method, "not isUCDetectionInMethods");
      return;
    }
    if (isOfBizService(method)) {
      ofbizServiceList.add(method);
    }
  }

  /** @return true if the method is static has two parameters namely a DispatchContext and a map */
  private static boolean isOfBizService(IMethod method) throws JavaModelException {
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
