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
package gov.nih.nci.restgen.ui.mapping;

import org.jgraph.JGraph;
import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.ConnectionSet;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.GraphModel;

import gov.nih.nci.restgen.mapping.model.Link;
import gov.nih.nci.restgen.mapping.model.Mapping;
import gov.nih.nci.restgen.mapping.model.Method;
import gov.nih.nci.restgen.mapping.model.Source;
import gov.nih.nci.restgen.mapping.model.Target;
import gov.nih.nci.restgen.ui.common.DefaultSettings;
import gov.nih.nci.restgen.ui.common.UIHelper;
import gov.nih.nci.restgen.ui.dnd.GraphDropTransferHandler;
import gov.nih.nci.restgen.ui.jgraph.MiddlePanelGraphModel;
import gov.nih.nci.restgen.ui.jgraph.MiddlePanelGraphScrollAdjustmentHandler;
import gov.nih.nci.restgen.ui.jgraph.MiddlePanelJGraphController;
import gov.nih.nci.restgen.ui.jgraph.MiddlePanelJGraphViewFactory;
import gov.nih.nci.restgen.ui.jgraph.MiddlePanelMarqueeHandler;
import gov.nih.nci.restgen.ui.tree.DefaultSourceTreeNode;
import gov.nih.nci.restgen.ui.tree.DefaultTargetTreeNode;


import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;
import javax.swing.ScrollPaneConstants;
import javax.swing.tree.DefaultMutableTreeNode;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The panel is used to render graphical respresentation of the mapping relations between
 * source and target tree panel.
 *
 * @author Prakash Vinjamuri
 * @author Prakash Vinjamuri LAST UPDATE $Author:
 * @since     CMTS v1.0
 * @version    $Revision: 1.5 $
 * @date       $Date: 2013-01-11
 *
 */
public class MappingMiddlePanel extends JPanel
{
	private JGraph graph = null;
	private Color graphBackgroundColor = new Color(222, 238, 255);
	private MappingMainPanel mappingPanel = null;
	private JScrollPane graphScrollPane = new JScrollPane();
	
	public MappingMiddlePanel(MappingMainPanel mappingPane)
	{
		super();
		mappingPanel = mappingPane;
		setBorder(BorderFactory.createRaisedBevelBorder());
		setLayout(new BorderLayout());
		setSize(new Dimension((DefaultSettings.FRAME_DEFAULT_WIDTH / 3), (int) (DefaultSettings.FRAME_DEFAULT_HEIGHT / 1.5)));
		// initialize graph
		initGraph();
		graphScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		graphScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		graphScrollPane.getViewport().setView(graph);
		add(graphScrollPane, BorderLayout.CENTER);
		//update graph as scroll
		MiddlePanelGraphScrollAdjustmentHandler grapScrollHandler=new MiddlePanelGraphScrollAdjustmentHandler();
		graphScrollPane.getVerticalScrollBar().addAdjustmentListener(grapScrollHandler);
		graphScrollPane.getHorizontalScrollBar().addAdjustmentListener(grapScrollHandler);
    }
	
	private void initGraph()
	{
		GraphModel model = new MiddlePanelGraphModel();
		graph = new JGraph(model);
		// Make Ports Visible by Default
		graph.setPortsVisible(true);
		// Use the Grid (but don't make it Visible)
		graph.setGridEnabled(true);
		// Set the Grid Size to 10 Pixel
		graph.setGridSize(6);
		// Set the Tolerance to 2 Pixel
		graph.setTolerance(2);
		// Accept edits if click on background
		graph.setInvokesStopCellEditing(true);
		// Allows control-drag
		graph.setCloneable(false);
		// Jump to default port on connect
		graph.setJumpToDefaultPort(false);
		graph.setDoubleBuffered(true);
		graph.setSizeable(true);
		MiddlePanelJGraphViewFactory	graphViewFactory = new MiddlePanelJGraphViewFactory();
		graph.getGraphLayoutCache().setFactory(graphViewFactory);
		graph.setDropEnabled(true);
		graph.setDragEnabled(true);
		graph.setEditable(false);
		graph.setMoveable(true);
		MiddlePanelMarqueeHandler marqueeHandler = new MiddlePanelMarqueeHandler();//this);
		graph.setMarqueeHandler(marqueeHandler);
		graph.setBackground( graphBackgroundColor);
		// added PV
		GraphDropTransferHandler gDropHandler=new GraphDropTransferHandler();
		graph.setTransferHandler(gDropHandler);
		// added PV
	}

	/**
	 * @return the graph
	 */
	public JGraph getGraph() {
		return graph;
	}

	public MiddlePanelJGraphController getGraphController(){
      return mappingPanel.getGraphController();// graphController;
    }

	/**
	 * @return the graphScrollPane
	 */
	public JScrollPane getGraphScrollPane() {
		return graphScrollPane;
	}

	public MappingMainPanel getMappingPanel()
	{
		return mappingPanel;
	}
	
	public List<DefaultEdge> retrieveLinks()
	{
		ArrayList<DefaultEdge> links=new ArrayList<DefaultEdge>();
		for (Object child:getGraph().getRoots())
		{
			if (child instanceof DefaultEdge)
				links.add((DefaultEdge)child);
		}
		return links;		
	}
	
	
	public void setMappingNamesforLinkInGraph(Mapping mapping)
	{
		List<Link> links =  mapping.getLinks();
		Link link = null;
		/** the real renderer */
		for(DefaultEdge linkEdge:retrieveLinks())
		{
			
			// check for mapping Name here start
			DefaultPort tgtPort=(DefaultPort)linkEdge.getTarget();
			Object sourceNode = tgtPort.getUserObject();
			DefaultTargetTreeNode tgtNodeTemp = (DefaultTargetTreeNode) sourceNode;
			if(links!=null && links.size()>0)
			{
    		Iterator<Link> linkit =links.iterator();
    		while(linkit.hasNext())
	    	{
    			link = (Link)linkit.next();
    			if(link!=null)
    			{
    				Target tgt = (Target)link.getTarget();
    				
    				if(tgt!=null && tgt.getComponentId().equals(tgtNodeTemp.getOperationName()))
    				{
    					if(link.getPath()!=null && !link.getPath().equals(""))
    					{
    						getGraph().getModel().valueForCellChanged(linkEdge,link.getPath());
    						getGraph().getSelectionModel().clearSelection();
    						break;
    					}
    				}
    				
    			}
    		}
			}
			// end			
		}
	}		
	
	public void renderInJGraph()
	{
		/** the real renderer */
		ConnectionSet cs = new ConnectionSet();
		Map<Object, AttributeMap> attributes = new Hashtable<Object, AttributeMap>();
		for(DefaultEdge linkEdge:retrieveLinks())
		{
			DefaultPort srcPort=(DefaultPort)linkEdge.getSource();
			DefaultPort trgtPort=(DefaultPort)linkEdge.getTarget();
			DefaultGraphCell sourceCell =(DefaultGraphCell)srcPort.getParent();
			DefaultGraphCell targetCell =(DefaultGraphCell)trgtPort.getParent();
			AttributeMap lineStyle = linkEdge.getAttributes();
			AttributeMap sourceNodeCellAttribute =null;
			if (sourceCell!=null)
				sourceNodeCellAttribute=sourceCell.getAttributes();
			AttributeMap targetNodeCellAttribute =null;
			if (targetCell!=null)
				targetNodeCellAttribute=targetCell.getAttributes();

			Object sourceNode = srcPort.getUserObject();
			Object targetNode = trgtPort.getUserObject();
			try {
					DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) targetNode;
					adjustToNewPosition(treeNode, targetNodeCellAttribute);
					DefaultMutableTreeNode srcNode = (DefaultMutableTreeNode) sourceNode;
					adjustToNewPosition(srcNode, sourceNodeCellAttribute);
				
				if ( sourceNodeCellAttribute != null
						&&targetNodeCellAttribute!=null) {// put in attribute if and only if it is constructed.
					attributes.put(sourceCell, sourceNodeCellAttribute);
					attributes.put(targetCell, targetNodeCellAttribute);
					attributes.put(linkEdge, lineStyle);
					// cs.connect(linkEdge, sourceCell.getChildAt(0), targetCell.getChildAt(0));
					// Log.logInfo(this, "Drew line for : " + mappingComponent.toString());
				}
			} catch (Throwable e) {
				e.printStackTrace();
				//Log.logInfo(this, "Did not draw line for : " + mappingComponent.toString(true));
			}
		}// end of for
		getGraph().getGraphLayoutCache().edit(attributes, cs, null, null);
		getGraph().getGraphLayoutCache().setSelectsAllInsertedCells(false);
	}
	/**
	 * Adjust the given treenode's display coordinates. If given tree node is null or the root, will simply ignore.
	 * 
	 * @param treeNode
	 *            the tree node
	 * @param oldAttributeMap
	 *            the existing attribute on the graph associated with the given tree node
	 * @return the oldAttributeMap after applying the newly calculated attribute.
	 */
	private AttributeMap adjustToNewPosition(DefaultMutableTreeNode treeNode, AttributeMap oldAttributeMap)
	{
		if ( treeNode != null && !treeNode.isRoot() ) {// change the render value if and only if neither it is null nor a root.
			boolean isFromSourceTree = UIHelper.isDataFromSourceTree(treeNode);
			int sourceYpos = -1;
			AttributeMap newTreeNodeAttribute = null;
			if ( isFromSourceTree ) {
				// Find the Y position for the source for this mappingNode.
				// find the # of pixels hidden. For example : 30
				sourceYpos = getGraphController().calculateScrolledDistanceOnY(mappingPanel.getSourceScrollPane(), treeNode, true);
				// To hide the vertex body from the graph
				newTreeNodeAttribute = UIHelper.getDefaultInvisibleVertexBounds(new Point(0, sourceYpos-31), true);
			} else {
				sourceYpos = getGraphController().calculateScrolledDistanceOnY(mappingPanel.getTargetScrollPane(), treeNode, true);
				newTreeNodeAttribute = UIHelper.getDefaultInvisibleVertexBounds(new Point( (int) graphScrollPane.getVisibleRect().getWidth()-5, sourceYpos+210), false);
			}
			if ( oldAttributeMap == null ) {// never return null.
				oldAttributeMap = new AttributeMap();
			}
			oldAttributeMap.applyMap(newTreeNodeAttribute);
		}
		return oldAttributeMap;
	}
}
/**
 * HISTORY: $Log: not supported by cvs2svn $
 * HISTORY: Revision 1.4  2009/11/03 18:32:26  wangeug
 * HISTORY: clean codes: keep MiddlePanelJGraphController only with MiddleMappingPanel
 * HISTORY:
 * HISTORY: Revision 1.3  2009/10/30 14:45:09  wangeug
 * HISTORY: simplify code: only respond to link highter
 * HISTORY:
 * HISTORY: Revision 1.2  2008/12/04 21:34:20  linc
 * HISTORY: Drap and Drop support with new Swing.
 * HISTORY:
 * HISTORY: Revision 1.1  2008/10/30 16:02:14  linc
 * HISTORY: updated.
 * HISTORY:
 */
