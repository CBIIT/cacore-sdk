/*L
 *  Copyright Ekagra Software Technologies Ltd.
 *  Copyright SAIC, SAIC-Frederick
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/cacore-sdk/LICENSE.txt for details.
 */

/**
 * The content of this file is subject to the caCore SDK Software License (the "License").  
 * A copy of the License is available at:
 * [caCore SDK CVS home directory]\etc\license\caCore SDK_license.txt. or at:
 * http://ncicb.nci.nih.gov/infrastructure/cacore_overview/caCore SDK/indexContent
 * /docs/caCore SDK_License
 */
package gov.nih.nci.restgen.ui.dnd;

import gov.nih.nci.restgen.ui.common.MappableNode;
import gov.nih.nci.restgen.ui.common.UIHelper;
import gov.nih.nci.restgen.ui.mapping.MappingMainPanel;
import gov.nih.nci.restgen.ui.tree.DefaultMappableTreeNode;
import gov.nih.nci.restgen.ui.tree.DefaultSourceTreeNode;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

/**
 * This class 
 *
 * @author Prakash Vinjamuri
 * @author Prakash Vinjamuri LAST UPDATE $Author:
 * @since     CMTS v1.0
 * @version    $Revision: 1.8 $
 * @date       $Date: 2013-01-11
 *
 */
public class TreeTransferHandler extends TreeDragTransferHandler {

	private MappingMainPanel panel;
	/**
	 * @param tree
	 */
	public TreeTransferHandler(MappingMainPanel panel) {
		this.panel = panel;
	}


	/* (non-Javadoc)
	 * @see javax.swing.TransferHandler#canImport(javax.swing.TransferHandler.TransferSupport)
	 */
	@Override
	public boolean canImport(TransferSupport info) {


        if (!info.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            return false;
        }
        //System.out.println("Inside can import data...");
        JTree.DropLocation dl = (JTree.DropLocation)info.getDropLocation();
        TreePath path = dl.getPath();
        if (path == null) {
            return false;
        }
        /*DefaultMappableTreeNode dropTreeNode=(DefaultMappableTreeNode)path.getLastPathComponent();
        if(dropTreeNode instanceof DefaultSourceTreeNode){
        	return false;
        }
        //only one source is allowed for one target node
        if (dropTreeNode.isMapped())
        	return false;*/
        //System.out.println("Inside can import data before returning true...");
        return true;
	}


	/* (non-Javadoc)
	 * @see javax.swing.TransferHandler#importData(javax.swing.TransferHandler.TransferSupport)
	 */
	@Override
	public boolean importData(TransferSupport info) {
        Object data;
        //System.out.println("Inside import data...");
        try {
           //data = info.getTransferable().getTransferData(DataFlavor.stringFlavor);
           data = info.getTransferable().getTransferData(TreeTransferableNode.mutableTreeNodeFlavor);
        } catch (UnsupportedFlavorException e) {
        	e.printStackTrace();
            return false;
        } catch (IOException e) {
        	e.printStackTrace();
            return false;
        }
       try{
       if (data instanceof DefaultMutableTreeNode )
       {
    	   //System.out.println("inside import data default mutable"+data);
 	        JTree.DropLocation dl = (JTree.DropLocation)info.getDropLocation();
	        TreePath path = dl.getPath();
	        DefaultMutableTreeNode targetNode = (DefaultMutableTreeNode) path.getLastPathComponent();
	        DefaultMutableTreeNode sourceNodeTransfered = (DefaultMutableTreeNode)data ;
	        String srcNodePath=UIHelper.getPathStringForNode(sourceNodeTransfered);
	        // PV below two lines
	        //DefaultMutableTreeNode sourceNode = (DefaultMutableTreeNode)data;
	        DefaultMutableTreeNode sourceNode=UIHelper.findTreeNodeWithXmlPath((DefaultMutableTreeNode)panel.getSourceTree().getModel().getRoot(), srcNodePath);
	        //System.out.println("inside import data before calling create mapping");
	        boolean ret = this.panel.getGraphController().createMapping((MappableNode)sourceNode, (MappableNode)targetNode);
	        //System.out.println("inside import data before returning");
	        return ret;
       }
       }
       catch(Exception ex)
       {
    	   ex.printStackTrace();
    	   //System.out.println("Exception"+ex.toString());
       }
       
	
       //System.out.println("TreeTransferHandler.importData()...not accepted Object:"+data);
       return false;
}
}
/**
 * HISTORY: $Log: not supported by cvs2svn $
 * HISTORY: Revision 1.7  2009/11/03 18:31:15  wangeug
 * HISTORY: clean codes: keep MiddlePanelJGraphController only with MiddleMappingPanel
 * HISTORY:
 * HISTORY: Revision 1.6  2009/10/28 16:45:56  wangeug
 * HISTORY: clean codes
 * HISTORY:
 * HISTORY: Revision 1.5  2009/10/27 18:23:10  wangeug
 * HISTORY: clean codes
 * HISTORY:
 * HISTORY: Revision 1.4  2008/12/29 22:18:18  linc
 * HISTORY: function UI added.
 * HISTORY:
 * HISTORY: Revision 1.3  2008/12/10 15:43:03  linc
 * HISTORY: Fixed component id generator and delete link.
 * HISTORY:
 * HISTORY: Revision 1.2  2008/12/09 19:04:17  linc
 * HISTORY: First GUI release
 * HISTORY:
 * HISTORY: Revision 1.1  2008/12/04 21:34:20  linc
 * HISTORY: Drap and Drop support with new Swing.
 * HISTORY:
 */

