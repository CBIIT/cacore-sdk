/*L
 *  Copyright Ekagra Software Technologies Ltd.
 *  Copyright SAIC, SAIC-Frederick
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/cacore-sdk/LICENSE.txt for details.
 */

package gov.nih.nci.sdk.ide.generator;

import gov.nih.nci.sdk.ide.core.GroupPanel;
import gov.nih.nci.sdk.ide.core.PropertyVO;
import gov.nih.nci.sdk.ide.core.UIHelper;
import gov.nih.nci.sdk.ide.modelexplorer.Constants;
import gov.nih.nci.sdk.ide.modelexplorer.SDKUIManager;

import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

public class GeneratorDetailsGroupPanel extends GroupPanel {
	private String currentHandledEventName;
	
	public GeneratorDetailsGroupPanel(Composite parent, int style, Object data, String title) 
	{
		super(parent, style, data, title);
		SDKUIManager.getInstance().registerAsListener(Constants.GENERATOR_SELECTION_EVENT, this);
	}
	
	public void paint() 
	{
		String name = "Please select a generator";
		String description = "";
		GeneratorInfoVO giVO = new GeneratorInfoVO();
		giVO.setName(name);
		giVO.setDescription(description);

		GeneratorSelectionEvent event = new GeneratorSelectionEvent(giVO);
		_paint(event);
	}
	
	@Override
	public void handleEvent(Event event) 
	{
		if (event == null) { return; }
		
		if (event instanceof GeneratorSelectionEvent)
		{
			_paint((GeneratorSelectionEvent) event);
		}
	}
	
	private Composite composite;
	private Label nameLabel;
	private Text domainNameText;
	private Label domainDescLabel;
	private Text domainDescText;
	private Label propertiesLabel;
	private Table propertiesTable;

	private void _paint(GeneratorSelectionEvent event) {
		Composite parent = super.getUIComposite();
		if (event == null || parent == null) return;
		
		String eventName = event.getEventName();
		if (eventName.equals(currentHandledEventName)) return;
		currentHandledEventName = eventName;
		
		if (composite == null) {
			composite = new Composite(parent, SWT.NONE);
			composite.setLayout(UIHelper.getOneColumnLayout());
			composite.setLayoutData(UIHelper.getCoverAllGridData());
			UIHelper.setWhiteBackground(composite);
		}
		
		GeneratorInfoVO giVO = event.getGeneratorInfoVO();
		String name = giVO.getName();
		String description = giVO.getDescription();
		java.util.Properties properties = giVO.getProperties();

		if (nameLabel == null) {
			nameLabel = new Label(composite, SWT.NONE);
			nameLabel.setText("Name");
			UIHelper.setWhiteBackground(nameLabel);
		}
		
		if (domainNameText == null) {
			domainNameText = new Text(composite, SWT.BORDER | SWT.READ_ONLY);
			domainNameText.setLayoutData(UIHelper.getCoverAllGridData());
			UIHelper.setWhiteBackground(domainNameText);
		}
		domainNameText.setText(name);
		
		if (domainDescLabel == null) {
			domainDescLabel = new Label(composite, SWT.NONE);
			domainDescLabel.setText("Description");
			UIHelper.setWhiteBackground(domainDescLabel);
		}
		
		if (domainDescText == null) {
			domainDescText = new Text(composite, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER | SWT.WRAP | SWT.READ_ONLY);
			
			GridData domainDescTextGD = UIHelper.getCoverAllGridData();
			domainDescTextGD.heightHint = 100;
			domainDescTextGD.minimumWidth = 200;
			domainDescText.setLayoutData(domainDescTextGD);
			UIHelper.setWhiteBackground(domainDescText);
		}
		domainDescText.setText(description);
		
		if (propertiesLabel == null) {
			propertiesLabel = new Label(composite, SWT.NONE);
			propertiesLabel.setText("Properties");
			UIHelper.setWhiteBackground(propertiesLabel);
		}

		if (propertiesTable == null) {
			propertiesTable = new Table(composite, SWT.MULTI | SWT.BORDER);
			propertiesTable.setLinesVisible(true);
			propertiesTable.setHeaderVisible(true);
			GridData data = UIHelper.getCoverAllGridData();
			data.heightHint = 100;
			propertiesTable.setLayoutData(data);
			
			String[] titles = {"Name", "Value", "Default"};
			for (int i = 0; i < titles.length; i++) {
				TableColumn column = new TableColumn(propertiesTable, SWT.NONE);
				column.setText(titles[i]);
				propertiesTable.getColumn(i).pack();
			}
		}
		
		propertiesTable.removeAll();
				
		for (Map.Entry propertiesEntry: giVO.getProperties().entrySet())
		{
			String propertyName = (String)propertiesEntry.getKey();
			String propertyValue = (String)propertiesEntry.getValue();
			
			TableItem item = new TableItem(propertiesTable, SWT.NONE);
			item.setText(0, propertyName);
			item.setText(1, propertyValue);
			item.setText(2, propertyValue);
		}

		super.getUIComposite().redraw();
	}
}