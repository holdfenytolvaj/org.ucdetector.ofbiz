/**
 * This program and the
 * accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ucdetector.util;

import org.eclipse.core.resources.IResource;

/** Just a container */
public class ReferenceAndLocation {

  public final String referencedItem;
  public final IResource location;

  public ReferenceAndLocation(String referencedItem, IResource location) {
    this.referencedItem = referencedItem;
    this.location = location;
  }
}
