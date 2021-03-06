/*
 * Software Name : ATK
 *
 * Copyright (C) 2007 - 2012 France Télécom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * ------------------------------------------------------------------
 * File Name   : AddDirTask.java
 *
 * Created     : 27/04/2007
 * Author(s)   : Aurore PENAULT
 */
package com.orange.atk.atkUI.coregui.tasks;

import java.awt.Cursor;
import java.io.File;
import java.util.Iterator;

import javax.swing.JOptionPane;

import com.orange.atk.atkUI.corecli.Alert;
import com.orange.atk.atkUI.corecli.Campaign;
import com.orange.atk.atkUI.corecli.Step;
import com.orange.atk.atkUI.coregui.AnalysisGUICommon;
import com.orange.atk.atkUI.coregui.CoreGUIPlugin;
import com.orange.atk.atkUI.coregui.MatosGUI;
import com.orange.atk.atkUI.coregui.StatusBar;

/**
 * This is a task that loads a campaign in the checklist.
 * @author Aurore PENAULT
 * @since JDK5.0
 */
public class AddDirTask extends UITask {

	private File dirToAdd;
	private StatusBar statusBar;
	private boolean load;

	/**
	 * A task to loads a campaign in the checklist.
	 * @param dialog the UI dialog to display while the task is performed.
	 * @param file the directory to add
	 * @param from the checklist starting index, -1 means at the end.
	 * @param load true if loads a new checklist, false if add to the current checklist.
	 */
	public AddDirTask(StatusBar statusBar, File dir, boolean load, int length) {
		this.dirToAdd = dir;
		//this.from = from; // always concidering adding at the end (-1)
		this.statusBar = statusBar;
		this.load = load;
		statusBar.setLength(length);
		new Thread(this).start();

	}

	public boolean isStop() {
		return statusBar.isStop();
	}

	/* (non-Javadoc)
	 * @see com.francetelecom.rd.matos.coregui.UITask#run()
	 */
	@Override
	public void run() {
		statusBar.setMessage("Adding directory " + dirToAdd.toString());
		
		// prevent multiples actions
		CoreGUIPlugin.mainFrame.enableUserActions(false);
		CoreGUIPlugin.mainFrame.setCursor(new Cursor(Cursor.WAIT_CURSOR));

		try {
			int added_steps = 0;
			
			Campaign tmpCampaign = new Campaign();
			for (AnalysisGUICommon guiCommon : MatosGUI.analysisPlugins) {
				tmpCampaign.addAll(guiCommon.buildCampaignFromDirectory(dirToAdd));
				increment();
			}
			CoreGUIPlugin.mainFrame.setModified(true);
			statusBar.setLength(tmpCampaign.size());
			statusBar.setMessage("Adding " + tmpCampaign.size() + " step(s)...");
			Iterator<Step> it = tmpCampaign.iterator();
			while ((!isStop())&&(it.hasNext())){
				Step step = it.next();
				step.setOutFilePath("");
				
				//remove add to all tab
				
				//for (AnalysisGUICommon plugin : MatosGUI.analysisPlugins) {
				MatosGUI.getSelectedGUI().getCheckListTable().addRow(step, -1, false, true);
				//}
				added_steps++;
				increment();
			}
			if (load) CoreGUIPlugin.mainFrame.setModified(false);
			CoreGUIPlugin.mainFrame.updateContentTabsTitle();

			//3. update checklist if needed. It is done after adding steps for a better end user experience
			// TODO DB
			/*if (CoreGUI.configuration.bool(Configuration.allowRetrievingPreviousResults)) {
				int length_update = MatosGUI.getStepNumber();
				statusBar.setMessage("Looking for previous results "+length_update+" steps...");
				for (AnalysisGUICommon plugin : MatosGUI.analysisPlugin) {
					plugin.getCheckListTable().updateModifiedAllRow(this);
				}
				statusBar.setMessage("CheckList updated with previous results.");
			}*/

			// clear job info in statusbar
			statusBar.clearJob(added_steps + " step(s) added.");
		} catch (Alert a) {
			statusBar.clearJob("aborted");
			JOptionPane.showMessageDialog(
					CoreGUIPlugin.mainFrame,
					a.getMessage(),
					"Error !",
					JOptionPane.ERROR_MESSAGE);
		}

		CoreGUIPlugin.mainFrame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		CoreGUIPlugin.mainFrame.enableUserActions(true); // implies updateButtons
	}

	/* (non-Javadoc)
	 * @see com.francetelecom.rd.matos.coregui.IProgressMonitor#increment(java.lang.String)
	 */
	public void increment(String message) {
		statusBar.increment(message);
	}

	/* (non-Javadoc)
	 * @see com.francetelecom.rd.matos.coregui.IProgressMonitor#setMessage(java.lang.String)
	 */
	public void setMessage(String message) {
		statusBar.setMessage(message);
	}

}
