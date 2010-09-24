package gov.nih.nci.sdk.ide.modelexplorer.meaning;

import gov.nih.nci.sdk.ide.core.CategoryTabItem;
import gov.nih.nci.sdk.ide.modelexplorer.Constants;
import gov.nih.nci.sdk.ide.modelexplorer.ModelSelectionEvent;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.Text;

public class MeaningPropertiesTabItem extends CategoryTabItem {

	public MeaningPropertiesTabItem(TabFolder parent, int style, Object data)
	{
		super(parent, style, data, Constants.TAB_Properties);
	}

	@Override
	public void paint()
	{
		ModelSelectionEvent modelSelectionEvent = (ModelSelectionEvent)this.getData();
		EPackage ePackage = gov.nih.nci.sdk.ide.modelexplorer.SDKUIManager.getInstance().getRootEPackage();
		EClass eClass = gov.nih.nci.sdk.util.EcoreUtil.getEClass(ePackage, modelSelectionEvent.getFullModelName());		
		EList<EAttribute> eAttributeList = (eClass != null)?eClass.getEAttributes():null;
		String domainName = gov.nih.nci.sdk.util.SDKUtil.getTagValue(eClass, "class.mea.domain");
		domainName = (domainName == null) ? modelSelectionEvent.getModelName() : domainName;
		
		Composite composite = super.getUIComposite();
		composite.setLayout(super.getLayout());

		Group group = new Group(composite, SWT.SHADOW_OUT);
		group.setText(domainName + " Property Info");
		group.setLayout(super.getLayout());
		group.setLayoutData(super.getGridData());
		
		Group listGroup= new Group(composite, SWT.SHADOW_OUT);
		listGroup.setText("has properties ...");
		listGroup.setLayout(super.getLayout());
		listGroup.setLayoutData(super.getGridData());

		if (eAttributeList != null && eAttributeList.isEmpty() == false)
		{
			EAttribute selectedAttribute = eAttributeList.get(0);
		
			List propertiesList = new List(listGroup, SWT.NONE);
			
			for (org.eclipse.emf.ecore.EAttribute eAttribute: eAttributeList)
			{
				propertiesList.add(eAttribute.getName());
			}
			
			new Label(group, SWT.NONE).setText("Name");
			Text nameText = new Text(group, SWT.BORDER | SWT.READ_ONLY);
			nameText.setText(selectedAttribute.getName());
			nameText.setLayoutData(super.getGridData());
	
			new Label(group, SWT.NONE).setText("Description");
			Text descriptionText = new Text(group, SWT.BORDER | SWT.READ_ONLY | SWT.MULTI);
			String attributeDescription = gov.nih.nci.sdk.util.SDKUtil.getTagValue(selectedAttribute, "prop.mea.desc");
			attributeDescription = (attributeDescription == null) ? "No attribute description found" : attributeDescription;
			descriptionText.setText(attributeDescription);
			descriptionText.setLayoutData(new GridData());
	
			new Label(group, SWT.NONE).setText("Type");
			Text typeText = new Text(group, SWT.BORDER | SWT.READ_ONLY | SWT.MULTI);
			String type = (selectedAttribute.getEType() != null) ? selectedAttribute.getEType().getName() : "";
			typeText.setText(type);
			typeText.setLayoutData(new GridData());
		}
		else
		{
			new Label(group, SWT.NONE).setText("This domain has no properties");		
		}

		group.setSize(group.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		listGroup.setSize(listGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		composite.setSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}

	@Override
	public void prepareData() {}
}
