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
import org.ucdetector.Log;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** Parse OfBiz Specific xmls */
public class OfBizSpecificXmlParser {
  XPath xPath = XPathFactory.newInstance().newXPath();
  XPathExpression serviceDefinitionExpression;
  XPathExpression serviceCallFromControllerExpression;
  XPathExpression serviceCallFromEcaExpression;
  XPathExpression referencedFtlExpression;
  XPathExpression referencedBshOrGroovyExpression;

  public OfBizSpecificXmlParser() {
    try {
      //--- for service definition ---------------------------
      serviceDefinitionExpression = xPath.compile("/services/service"); //$NON-NLS-1$

      //--- for controller -----------------------------------
      serviceCallFromControllerExpression = xPath.compile("/site-conf/request-map/event[@type='service']"); //$NON-NLS-1$

      //--- for screen definition ----------------------------
      referencedFtlExpression = xPath.compile("/screens/screen/section//html-template"); //$NON-NLS-1$
      referencedBshOrGroovyExpression = xPath.compile("/screens/screen/section/actions/script"); //$NON-NLS-1$

      //--- for seca -----------------------------------------
      serviceCallFromEcaExpression = xPath.compile("/service-eca/eca/action"); //$NON-NLS-1$
    }

    catch (XPathExpressionException e) {
      throw new RuntimeException(e);
    }
  }

  public void extractDefinitionsFromXml(IResource file, Map<String, String> serviceMethodToNameMap,
      Map<String, IResource> serviceNameToFilePathMap, Set<String> referencedFtlList,
      Set<String> referencedBshOrGroovyList, Set<String> referencedServiceList) {

    DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = null;
    String pathToFile = file.getLocation().toString();
    try {
      builder = builderFactory.newDocumentBuilder();
      Document document = builder.parse(new FileInputStream(pathToFile));

      //--- for service definition ---------------------------
      getServiceDefinitionListFromXml(document, serviceMethodToNameMap, serviceNameToFilePathMap, file);

      //--- for controller -----------------------------------
      getReferencedServiceListFromControllerXml(document, referencedServiceList);

      //--- for screen definition ----------------------------
      getReferencedFtlListFromXml(document, referencedFtlList);
      getReferencedBshOrGroovyListFromXml(document, referencedBshOrGroovyList);

      //--- for seca -----------------------------------------
      getReferencedServiceListFromSecaXml(document, referencedServiceList);
    }
    catch (ParserConfigurationException e) {
      throw new RuntimeException("Error in file " + pathToFile, e); //$NON-NLS-1$
    }
    catch (FileNotFoundException e) {
      Log.error("Skipping file " + pathToFile + " because " + e.getMessage() + " not found?" + (e.getCause() == null ? "" : e.getCause().getMessage())); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
      //throw new RuntimeException("Error in file " + pathToFile, e); //$NON-NLS-1$
    }
    catch (SAXException e) {
      Log.error("Skipping file " + pathToFile + " because " + e.getMessage() + " not parsable? " //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
          + (e.getCause() == null ? "" : e.getCause().getMessage())); //$NON-NLS-1$
      //throw new RuntimeException("Error in file " + pathToFile, e); //$NON-NLS-1$
    }
    catch (IOException e) {
      throw new RuntimeException("Error in file " + pathToFile, e); //$NON-NLS-1$
    }
    catch (XPathExpressionException e) {
      throw new RuntimeException("Error in file " + pathToFile, e); //$NON-NLS-1$
    }
  }

  private void getReferencedBshOrGroovyListFromXml(Document document, Set<String> referencedBshOrGroovyList)
      throws XPathExpressionException {
    NodeList nodeList = (NodeList) referencedBshOrGroovyExpression.evaluate(document, XPathConstants.NODESET);

    for (int i = 0; i < nodeList.getLength(); i++) {
      Node n = nodeList.item(i);
      NamedNodeMap attributes = n.getAttributes();
      if (attributes.getNamedItem("location") != null) { //$NON-NLS-1$ 
        String path = attributes.getNamedItem("location").getTextContent().replaceFirst("component://", "/"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ 
        referencedBshOrGroovyList.add(path);
      }
    }
  }

  private void getReferencedFtlListFromXml(Document document, Set<String> referencedFtlList)
      throws XPathExpressionException {
    NodeList nodeList = (NodeList) referencedFtlExpression.evaluate(document, XPathConstants.NODESET);

    for (int i = 0; i < nodeList.getLength(); i++) {
      Node n = nodeList.item(i);
      NamedNodeMap attributes = n.getAttributes();
      if (attributes.getNamedItem("location") != null) { //$NON-NLS-1$ 
        String path = attributes.getNamedItem("location").getTextContent().replaceFirst("component://", "/"); //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
        referencedFtlList.add(path);
      }
    }
  }

  private void getServiceDefinitionListFromXml(Document document, Map<String, String> serviceMethodToNameMap,
      Map<String, IResource> serviceNameToFileMap, IResource file) throws XPathExpressionException {
    NodeList nodeList = (NodeList) serviceDefinitionExpression.evaluate(document, XPathConstants.NODESET);

    for (int i = 0; i < nodeList.getLength(); i++) {
      Node n = nodeList.item(i);
      NamedNodeMap attributes = n.getAttributes();
      String serviceName = attributes.getNamedItem("name").getTextContent(); //$NON-NLS-1$

      if (attributes.getNamedItem("location") != null && attributes.getNamedItem("invoke") != null) { //$NON-NLS-1$ //$NON-NLS-2$
        String qualifiedMethodName = attributes.getNamedItem("location").getTextContent() + "." + attributes.getNamedItem("invoke").getTextContent(); //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
        serviceMethodToNameMap.put(qualifiedMethodName, serviceName);
        serviceNameToFileMap.put(serviceName, file);
      }
    }
  }

  private void getReferencedServiceListFromControllerXml(Document document, Set<String> referencedServiceList)
      throws XPathExpressionException {
    NodeList nodeList = (NodeList) serviceCallFromControllerExpression.evaluate(document, XPathConstants.NODESET);

    for (int i = 0; i < nodeList.getLength(); i++) {
      Node n = nodeList.item(i);
      NamedNodeMap attributes = n.getAttributes();
      if (attributes.getNamedItem("invoke") != null) { //$NON-NLS-1$ 
        String serviceName = attributes.getNamedItem("invoke").getTextContent(); //$NON-NLS-1$
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
      if (attributes.getNamedItem("service") != null) { //$NON-NLS-1$ 
        String serviceName = attributes.getNamedItem("service").getTextContent(); //$NON-NLS-1$
        referencedServiceList.add(serviceName);
      }
    }
  }

}
