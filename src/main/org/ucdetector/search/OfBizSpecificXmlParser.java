/**
 * This program and the
 * accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ucdetector.search;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.ucdetector.Log;
import org.ucdetector.util.ReferenceAndLocation;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** 
 * Parse OfBiz Specific xmls like:
 * controller.xml
 * services.xml
 * seca.xml
 * screen.xml
 */
public class OfBizSpecificXmlParser {
  XPath xPath = XPathFactory.newInstance().newXPath();
  XPathExpression serviceDefinitionExpression;

  XPathExpression serviceCallFromControllerExpression;
  XPathExpression referencedViewFromControllerExpression;
  XPathExpression viewDefinitionFromControllerExpression;

  XPathExpression referencedFtlFromScreenExpression;
  XPathExpression referencedBshOrGroovyFromScreenExpression;
  XPathExpression screenDefinitionFromScreenExpression;
  XPathExpression referencedScreenFromScreenExpression;

  XPathExpression serviceCallFromEcaExpression;

  public OfBizSpecificXmlParser() {
    try {
      //--- for service definition ---------------------------
      //in case we only want to care about java engines it can be changed to /services/service[@engine='java']
      //now we simply ignore this since we are not looking up whether the implementation exists or not
      serviceDefinitionExpression = xPath.compile("/services/service");

      //--- for controller -----------------------------------
      serviceCallFromControllerExpression = xPath.compile("/site-conf/request-map/event[@type='service']");
      referencedViewFromControllerExpression = xPath.compile("/site-conf/request-map/response[@type='view']");
      viewDefinitionFromControllerExpression = xPath.compile("/site-conf/view-map");

      //--- for screen definition ----------------------------
      referencedFtlFromScreenExpression = xPath.compile("/screens/screen/section//html-template");
      referencedBshOrGroovyFromScreenExpression = xPath.compile("/screens/screen/section/actions/script");
      screenDefinitionFromScreenExpression = xPath.compile("/screens/screen");
      referencedScreenFromScreenExpression = xPath.compile("/screens/screen/widgets/decorator-screen");

      //--- for seca -----------------------------------------
      serviceCallFromEcaExpression = xPath.compile("/service-eca/eca/action");

    }
    catch (XPathExpressionException e) {
      throw new RuntimeException(e);
    }
  }

  public void extractDefinitionsFromXml(IResource file, Map<String, String> serviceMethodToNameMap,
      Map<String, IResource> serviceNameAndFilePathMap, Set<String> referencedFtlList,
      Set<String> referencedBshOrGroovyList, Set<String> referencedServiceList, Set<String> referencedViewList,
      Map<String, ReferenceAndLocation> viewDefinitionMap, Set<String> referencedScreenList,
      Map<String, IResource> screenNameAndFilePathMap) {

    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = null;
    String pathToFile = file.getLocation().toString();
    try {
      builder = builderFactory.newDocumentBuilder();
      Document document = builder.parse(new FileInputStream(pathToFile));

      //--- for service definition ---------------------------
      getServiceDefinitionListFromServicesXml(document, serviceMethodToNameMap, serviceNameAndFilePathMap, file);

      //--- for controller -----------------------------------
      getReferencedServiceListFromControllerXml(document, referencedServiceList);
      getReferencedViewFromControllerXml(document, referencedViewList, file);
      getViewDefinitionAndReferencedScreenFromControllerXml(document, viewDefinitionMap, referencedScreenList, file);

      //--- for screen definition ----------------------------
      getReferencedFtlListFromScreenXml(document, referencedFtlList);
      getReferencedBshOrGroovyListFromScreenXml(document, referencedBshOrGroovyList);
      getScreenDefinitionFromScreenXml(document, screenNameAndFilePathMap, file);
      getReferencedScreenFromScreenXml(document, referencedScreenList);

      //--- for seca -----------------------------------------
      getReferencedServiceListFromSecaXml(document, referencedServiceList);

    }
    catch (ParserConfigurationException e) {
      throw new RuntimeException("Error in file " + pathToFile, e);
    }
    catch (FileNotFoundException e) {
      Log.error("Skipping file " + pathToFile + " because " + e.getMessage() + " not found?"
          + (e.getCause() == null ? "" : e.getCause().getMessage()));
      //throw new RuntimeException("Error in file " + pathToFile, e); 
    }
    catch (SAXException e) {
      Log.error("Skipping file " + pathToFile + " because " + e.getMessage() + " not parsable? "
          + (e.getCause() == null ? "" : e.getCause().getMessage()));
      //throw new RuntimeException("Error in file " + pathToFile, e); 
    }
    catch (IOException e) {
      throw new RuntimeException("Error in file " + pathToFile, e);
    }
    catch (XPathExpressionException e) {
      throw new RuntimeException("Error in file " + pathToFile, e);
    }
  }

  private void getReferencedScreenFromScreenXml(Document document, Set<String> referencedScreenList)
      throws XPathExpressionException {
    NodeList nodeList = (NodeList) referencedScreenFromScreenExpression.evaluate(document, XPathConstants.NODESET);

    for (int i = 0; i < nodeList.getLength(); i++) {
      Node n = nodeList.item(i);
      NamedNodeMap attributes = n.getAttributes();
      if (attributes.getNamedItem("name") != null && attributes.getNamedItem("location") != null) {
        //<decorator-screen name="main-decorator" location="component://accounting/widget/screens/common/CommonScreens.xml">
        String reference = attributes.getNamedItem("location").getTextContent().replaceAll("component://", "") + "#"
            + attributes.getNamedItem("name").getTextContent();

        referencedScreenList.add(reference);
      }
    }

  }

  private void getScreenDefinitionFromScreenXml(Document document, Map<String, IResource> screenNameAndFilePathMap,
      IResource file) throws XPathExpressionException {
    NodeList nodeList = (NodeList) screenDefinitionFromScreenExpression.evaluate(document, XPathConstants.NODESET);

    for (int i = 0; i < nodeList.getLength(); i++) {
      Node n = nodeList.item(i);
      NamedNodeMap attributes = n.getAttributes();
      if (attributes.getNamedItem("name") != null) {
        //<screen name="main-application-decorator">

        String screenName = file.getFullPath().toString()
            .replaceAll("/" + file.getProject().getName() + "/hot-deploy/", "")
            + "#" + attributes.getNamedItem("name").getTextContent();
        screenNameAndFilePathMap.put(screenName, file);
        //
      }
    }
  }

  private void getViewDefinitionAndReferencedScreenFromControllerXml(Document document,
      Map<String, ReferenceAndLocation> viewDefinitionMap, Set<String> referencedScreenList, IResource file)
      throws XPathExpressionException {
    NodeList nodeList = (NodeList) viewDefinitionFromControllerExpression.evaluate(document, XPathConstants.NODESET);

    for (int i = 0; i < nodeList.getLength(); i++) {
      Node n = nodeList.item(i);
      NamedNodeMap attributes = n.getAttributes();
      if (attributes.getNamedItem("page") != null) {
        //<view-map name="vatReportOverview" type="screen" page="component://accounting/widget/accounting/screens/accounting.xml#vatReportOverview"/>
        //for view in order to make unique we add the filepath as a prefix
        String viewName = file.getFullPath() + "#" + attributes.getNamedItem("name").getTextContent();
        String reference = attributes.getNamedItem("page").getTextContent().replaceAll("component://", "");
        viewDefinitionMap.put(viewName, new ReferenceAndLocation(reference, file));
        referencedScreenList.add(reference);
      }
    }
  }

  private void getReferencedViewFromControllerXml(Document document, Set<String> referencedViewList, IResource file)
      throws XPathExpressionException {
    NodeList nodeList = (NodeList) referencedViewFromControllerExpression.evaluate(document, XPathConstants.NODESET);

    for (int i = 0; i < nodeList.getLength(); i++) {
      Node n = nodeList.item(i);
      NamedNodeMap attributes = n.getAttributes();
      if (attributes.getNamedItem("value") != null) {
        //<response name="error" type="view" value="accountingViewDetails"/>
        //for view in order to make unique we add the filepath as a prefix
        referencedViewList.add(file.getFullPath() + "#" + attributes.getNamedItem("value").getTextContent());
      }
    }
  }

  private void getReferencedBshOrGroovyListFromScreenXml(Document document, Set<String> referencedBshOrGroovyList)
      throws XPathExpressionException {
    NodeList nodeList = (NodeList) referencedBshOrGroovyFromScreenExpression.evaluate(document, XPathConstants.NODESET);

    for (int i = 0; i < nodeList.getLength(); i++) {
      Node n = nodeList.item(i);
      NamedNodeMap attributes = n.getAttributes();
      if (attributes.getNamedItem("location") != null) {
        //<script location="component://crmsfa/webapp/crmsfa/WEB-INF/actions/includes/main-decorator.bsh"/>
        String path = attributes.getNamedItem("location").getTextContent().replaceFirst("component://", "/");
        referencedBshOrGroovyList.add(path);
      }
    }
  }

  private void getReferencedFtlListFromScreenXml(Document document, Set<String> referencedFtlList)
      throws XPathExpressionException {
    NodeList nodeList = (NodeList) referencedFtlFromScreenExpression.evaluate(document, XPathConstants.NODESET);

    for (int i = 0; i < nodeList.getLength(); i++) {
      Node n = nodeList.item(i);
      NamedNodeMap attributes = n.getAttributes();
      if (attributes.getNamedItem("location") != null) {
        //<html-template location="component://crmsfa/webapp/crmsfa/includes/test.ftl"/>
        String path = attributes.getNamedItem("location").getTextContent().replaceFirst("component://", "/");
        referencedFtlList.add(path);
      }
    }
  }

  private void getServiceDefinitionListFromServicesXml(Document document, Map<String, String> serviceMethodToNameMap,
      Map<String, IResource> serviceNameAndFilePathMap, IResource file) throws XPathExpressionException {
    NodeList nodeList = (NodeList) serviceDefinitionExpression.evaluate(document, XPathConstants.NODESET);

    for (int i = 0; i < nodeList.getLength(); i++) {
      Node n = nodeList.item(i);
      NamedNodeMap attributes = n.getAttributes();
      String serviceName = attributes.getNamedItem("name").getTextContent();

      if (attributes.getNamedItem("location") != null && attributes.getNamedItem("invoke") != null) {
        //<service name="crmsfa.createAccount" engine="java" location="com.opensourcestrategies.crmsfa.accounts.AccountsServices" invoke="createAccount">
        String qualifiedMethodName = attributes.getNamedItem("location").getTextContent() + "."
            + attributes.getNamedItem("invoke").getTextContent();
        serviceMethodToNameMap.put(qualifiedMethodName, serviceName);
        serviceNameAndFilePathMap.put(serviceName, file);
      }
    }
  }

  private void getReferencedServiceListFromControllerXml(Document document, Set<String> referencedServiceList)
      throws XPathExpressionException {
    NodeList nodeList = (NodeList) serviceCallFromControllerExpression.evaluate(document, XPathConstants.NODESET);

    for (int i = 0; i < nodeList.getLength(); i++) {
      Node n = nodeList.item(i);
      NamedNodeMap attributes = n.getAttributes();
      if (attributes.getNamedItem("invoke") != null) {
        //<event type="java" path="org.ofbiz.common.CommonEvents" invoke="setSessionLocale"/>
        String serviceName = attributes.getNamedItem("invoke").getTextContent();
        referencedServiceList.add(serviceName);
      }
    }
  }

  private void getReferencedServiceListFromSecaXml(Document document, Set<String> referencedServiceList)
      throws XPathExpressionException {
    NodeList nodeList = (NodeList) serviceCallFromEcaExpression.evaluate(document, XPathConstants.NODESET);

    for (int i = 0; i < nodeList.getLength(); i++) {
      Node n = nodeList.item(i);
      NamedNodeMap attributes = n.getAttributes();
      if (attributes.getNamedItem("service") != null) {
        //<action service="company.webservice.esb.outgoing.updateOrderPaymentStatus" mode="sync"/>
        String serviceName = attributes.getNamedItem("service").getTextContent();
        referencedServiceList.add(serviceName);
      }
    }
  }

  /**
   * searching patterns like "component://financials/widget/financials/screens/reports/ReportsScreens.xml#d2dSummary"
   */
  @SuppressWarnings("javadoc")
  public void searchForProgrammaticallyRenderedScreen(IResource resource, Set<String> referencedScreenList)
      throws CoreException {
    List<String> resultList = SimpleSearch.searchRegularExpressionReturnWithSelection(
        "\"component://[\\w-/\\.]+#[\\w]+\"", resource);
    for (String referenceUri : resultList) {
      referencedScreenList.add(referenceUri);
    }
  }
}
