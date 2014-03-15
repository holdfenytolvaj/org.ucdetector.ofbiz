/**
 * This program and the
 * accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ucdetector.util;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Helper class for the markers in non java file
 * (e.g. bsh/ftl and xml)
 */
public class NonJavaIMember implements IMember {
  private final IResource resource;
  private final int offset;
  private final int length;

  public NonJavaIMember(IResource resource) {
    this(resource, 0, 0);
  }

  public NonJavaIMember(IResource resource, int offset, int length) {
    this.resource = resource;
    this.offset = offset;
    this.length = length;
  }

  public IResource getResource() {
    return resource;
  }

  public ISourceRange getNameRange() throws JavaModelException {
    return new ISourceRange() {

      public int getOffset() {
        return offset;
      }

      public int getLength() {
        return length;
      }
    };
  }

  public ISourceRange getSourceRange() throws JavaModelException {
    return new ISourceRange() {

      public int getOffset() {
        return offset;
      }

      public int getLength() {
        return length;
      }
    };
  }

  public String getElementName() {
    return getPathToFile();
  }

  public String getPathToFile() {
    return resource.getProjectRelativePath().toString().replaceFirst("hot-deploy", ""); //$NON-NLS-1$ //$NON-NLS-2$
  }

  public String getFileName() {
    return resource.getName();
  }

  public IJavaProject getJavaProject() {
    return null; //(IJavaProject) resource.getProject();
  }

  public IOpenable getOpenable() {
    return null;
  }

  public IJavaElement getParent() {
    return null;
  }

  public ISourceRange getJavadocRange() throws JavaModelException {
    return null;
  }

  public boolean exists() {
    throw new RuntimeException("Not implemented exception"); //$NON-NLS-1$
  }

  public IJavaElement getAncestor(int arg0) {
    throw new RuntimeException("Not implemented exception"); //$NON-NLS-1$
  }

  public String getAttachedJavadoc(IProgressMonitor arg0) throws JavaModelException {
    throw new RuntimeException("Not implemented exception"); //$NON-NLS-1$
  }

  public IResource getCorrespondingResource() throws JavaModelException {
    throw new RuntimeException("Not implemented exception"); //$NON-NLS-1$
  }

  public int getElementType() {
    throw new RuntimeException("Not implemented exception"); //$NON-NLS-1$
  }

  public String getHandleIdentifier() {
    throw new RuntimeException("Not implemented exception"); //$NON-NLS-1$
  }

  public IJavaModel getJavaModel() {
    throw new RuntimeException("Not implemented exception"); //$NON-NLS-1$
  }

  public IPath getPath() {
    throw new RuntimeException("Not implemented exception"); //$NON-NLS-1$
  }

  public IJavaElement getPrimaryElement() {
    throw new RuntimeException("Not implemented exception"); //$NON-NLS-1$
  }

  public ISchedulingRule getSchedulingRule() {
    throw new RuntimeException("Not implemented exception"); //$NON-NLS-1$
  }

  public IResource getUnderlyingResource() throws JavaModelException {
    throw new RuntimeException("Not implemented exception"); //$NON-NLS-1$
  }

  public boolean isReadOnly() {
    throw new RuntimeException("Not implemented exception"); //$NON-NLS-1$
  }

  public boolean isStructureKnown() throws JavaModelException {
    throw new RuntimeException("Not implemented exception"); //$NON-NLS-1$
  }

  @SuppressWarnings("rawtypes")
  public Object getAdapter(Class adapter) {
    throw new RuntimeException("Not implemented exception"); //$NON-NLS-1$
  }

  public String getSource() throws JavaModelException {
    throw new RuntimeException("Not implemented exception"); //$NON-NLS-1$
  }

  public void copy(IJavaElement arg0, IJavaElement arg1, String arg2, boolean arg3, IProgressMonitor arg4)
      throws JavaModelException {
    throw new RuntimeException("Not implemented exception"); //$NON-NLS-1$
  }

  public void delete(boolean arg0, IProgressMonitor arg1) throws JavaModelException {
    throw new RuntimeException("Not implemented exception"); //$NON-NLS-1$
  }

  public void move(IJavaElement arg0, IJavaElement arg1, String arg2, boolean arg3, IProgressMonitor arg4)
      throws JavaModelException {
    throw new RuntimeException("Not implemented exception"); //$NON-NLS-1$
  }

  public void rename(String arg0, boolean arg1, IProgressMonitor arg2) throws JavaModelException {
    throw new RuntimeException("Not implemented exception"); //$NON-NLS-1$
  }

  public IJavaElement[] getChildren() throws JavaModelException {
    throw new RuntimeException("Not implemented exception"); //$NON-NLS-1$
  }

  public boolean hasChildren() throws JavaModelException {
    throw new RuntimeException("Not implemented exception"); //$NON-NLS-1$
  }

  public String[] getCategories() throws JavaModelException {
    throw new RuntimeException("Not implemented exception"); //$NON-NLS-1$
  }

  public IClassFile getClassFile() {
    throw new RuntimeException("Not implemented exception"); //$NON-NLS-1$
  }

  public ICompilationUnit getCompilationUnit() {
    throw new RuntimeException("Not implemented exception"); //$NON-NLS-1$
  }

  public IType getDeclaringType() {
    throw new RuntimeException("Not implemented exception"); //$NON-NLS-1$
  }

  public int getFlags() throws JavaModelException {
    throw new RuntimeException("Not implemented exception"); //$NON-NLS-1$
  }

  public int getOccurrenceCount() {
    throw new RuntimeException("Not implemented exception"); //$NON-NLS-1$
  }

  public IType getType(String arg0, int arg1) {
    throw new RuntimeException("Not implemented exception"); //$NON-NLS-1$
  }

  public ITypeRoot getTypeRoot() {
    throw new RuntimeException("Not implemented exception"); //$NON-NLS-1$
  }

  public boolean isBinary() {
    throw new RuntimeException("Not implemented exception"); //$NON-NLS-1$
  }

}
