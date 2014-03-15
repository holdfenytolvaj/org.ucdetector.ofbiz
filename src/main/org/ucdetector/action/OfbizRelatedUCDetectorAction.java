/**
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.ucdetector.action;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.ucdetector.Messages;
import org.ucdetector.UCDetectorPlugin;
import org.ucdetector.iterator.AbstractUCDetectorIterator;
import org.ucdetector.iterator.OfBizSpecificUCDetectorIterator;
import org.ucdetector.report.ReportNameManager;

public class OfbizRelatedUCDetectorAction extends AbstractUCDetectorAction {
  OfBizSpecificUCDetectorIterator iterator;

  @Override
  protected AbstractUCDetectorIterator createIterator() {
    iterator = new OfBizSpecificUCDetectorIterator();
    return iterator;
  }

  @Override
  protected IStatus postIteration() {
    int created = iterator.getMarkerCreated();
    StringBuilder mes = new StringBuilder();
    mes.append(NLS.bind(Messages.UCDetectorAction_ResultMessage, String.valueOf(created)));
    //
    String reportFolder = ReportNameManager.getReportDir(false);
    if (reportFolder != null && created > 0) {
      mes.append(". "); //$NON-NLS-1$
      String s = NLS.bind(Messages.UCDetectorAction_ResultReport, reportFolder);
      mes.append(s);
    }
    return new Status(IStatus.INFO, UCDetectorPlugin.ID, mes.toString());
  }
}
