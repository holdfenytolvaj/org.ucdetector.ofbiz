/**
 * All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ucdetector.iterator;

import org.ucdetector.search.UCDProgressMonitor;

/** Small helper to show progress in eclipse */
class UIProgressHelper {
  private final int maxWorkEffort;
  private final int addOneWorkEffortPerCall;
  private final UCDProgressMonitor monitor;
  private int workEffort = 0;
  private int numberOfCalls = 0;

  public UIProgressHelper(UCDProgressMonitor monitor, int maxWorkEffort, int addOneWorkEffortPerCall) {
    this.monitor = monitor;
    this.maxWorkEffort = maxWorkEffort;
    this.addOneWorkEffortPerCall = addOneWorkEffortPerCall;
  }

  void showProgress() {
    numberOfCalls++;
    if (workEffort < maxWorkEffort && numberOfCalls % addOneWorkEffortPerCall == 0) {
      monitor.internalWorked(1);
      workEffort++;
    }
  }
}
