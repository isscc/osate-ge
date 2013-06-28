package edu.uah.rsesc.aadl.age.xtext;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

public class Initializer implements IStartup {
	public void earlyStartup() {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				// TODO: This only works for the active page?
				final IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				activePage.addPartListener(new EditorListener(activePage));
			}
		});
	}
}