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

package gov.nih.nci.restgen.ui.jgraph;

import gov.nih.nci.restgen.ui.jgraph.action.GraphDeleteAction;
import gov.nih.nci.restgen.ui.jgraph.action.GraphDeleteAllAction;
import gov.nih.nci.restgen.ui.jgraph.action.NameMappingAction;

import javax.swing.SwingUtilities;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;

import org.jgraph.JGraph;
import org.jgraph.graph.BasicMarqueeHandler;
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphSelectionModel;
import org.jgraph.graph.PortView;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * This marquee handler overrides basic functionalities provided by basic Marquee handler
 * to plug in customized handlings of key and mouse driven events.
 * The MiddlePanelJGraphController class will deal with real implementation of some of those actions
 * and mainly focuses on drag-and-drop and handlings of repaint of graph, for example.
 *
 * @author Prakash Vinjamuri
 * @author Prakash Vinjamuri LAST UPDATE $Author:
 * @since     CMTS v1.0
 * @version    $Revision: 1.4 $
 * @date       $Date: 2013-01-11
 *
 */
public class MiddlePanelMarqueeHandler extends BasicMarqueeHandler
{
	private MiddlePanelJGraphController controller;
	// Holds the First and the Current Port
	protected PortView port, firstPort;

	GraphDeleteAction deleteAction = null;
	NameMappingAction nameMapping = null;
	private JPopupMenu popupMenu = null;

	/**
	 * @param controller the controller to set
	 */
	public void setController(MiddlePanelJGraphController controller) {
		this.controller = controller;
	}

	// Override to Gain Control (for PopupMenu and ConnectMode)
	public boolean isForceMarqueeEvent(MouseEvent e)
	{
		if (e.isShiftDown())
		{
			return false;
		}
		// If Right Mouse Button we want to Display the PopupMenu
		if (SwingUtilities.isRightMouseButton(e))
			// Return Immediately
			return true;
		// Find and Remember Port
		port = getSourcePortAt(e.getPoint());
		if (port==null)
			return false;
		
		/*FunctionBoxGraphCell functionCell=(FunctionBoxGraphCell)((DefaultPort)port.getCell()).getParent();
		if (!functionCell.getInputElements().isEmpty())
		{
			port =null;
			return false;
		}*/
		// If Port Found and in ConnectMode (=Ports Visible)
		JGraph graph =controller.getMiddlePanel().getGraph();
		if (port != null && graph.isPortsVisible())
		{
			return isValidPort(port);
		}//end if (port!=null ...
		else
		{
			return false;
		}
	}

	// Display PopupMenu or Remember Start Location and First Port
	public void mousePressed(final MouseEvent e)
	{
		//System.out.println("MiddlePanelMarqueeHandler.mousePressed() mousePressed().:(x="+e.getX()+",y="+e.getY()+")" );
		// following if block was inserted by umkis   (defect# 196)
		// For selection clearing when mouse clicking(pressing) on any empty place of middle (JGraph) panel.
		JGraph graph =controller.getMiddlePanel().getGraph();
		if (SwingUtilities.isLeftMouseButton(e))
		{
			//System.out.println("mouse Left Pressed().");
			GraphSelectionModel gModel = graph.getSelectionModel();
			gModel.clearSelection();
			/*if(controller.getMappingPanel().getSourceTree()!=null)
				controller.getMappingPanel().getSourceTree().clearSelection();
			if(controller.getMappingPanel().getTargetTree()!=null)
				controller.getMappingPanel().getTargetTree().clearSelection();*/
		}

		currentPoint = e.getPoint();
		// If Right Mouse Button
		if (SwingUtilities.isRightMouseButton(e))
		{
			// Find Cell in Model Coordinates
			Object cell = graph.getFirstCellForLocation(e.getX(), e.getY());
			// Create PopupMenu for the Cell
			JPopupMenu menu = createPopupMenu(e.getPoint(), cell);
			// Display PopupMenu
			menu.show(graph, e.getX(), e.getY());
			// Else if in ConnectMode and Remembered Port is Valid
		}
		else if (port != null && graph.isPortsVisible())
		{
			// Remember Start Location
			startPoint = graph.toScreen(port.getLocation(null));
			// Remember First Port
			firstPort = port;
		}
	}

	// Find Port under Mouse and Repaint Connector
	public void mouseDragged(MouseEvent e)
	{
		//		System.out.println("mouseDragged().:(x="+e.getX()+",y="+e.getY()+")" );
		// If remembered Start Point is Valid
		if (startPoint != null)
		{
			// Fetch Graphics from Graph
			JGraph graph =controller.getMiddlePanel().getGraph();
			Graphics g = graph.getGraphics();
			// Reset Remembered Port
			PortView newPort = getTargetPortAt(e.getPoint());
			// Do not flicker (repaint only on real changes)
			if (isValidPort(port) && isValidPort(newPort))//newPort == null || newPort != port)
			{//paint the port and connector if and only if it is valid one
				// Xor-Paint the old Connector (Hide old Connector)
				//FunctionData targetFunctionPort=(FunctionData)((DefaultPort)newPort.getCell()).getUserObject();
				//if (!targetFunctionPort.isInput())
				//{
				//	return;
				//}
				paintConnector(Color.black, graph.getBackground(), g);
				// If Port was found then Point to Port Location
				port = newPort;
				if (port != null)
					currentPoint = graph.toScreen(port.getLocation(null));
				// Else If no Port was found then Point to Mouse Location
				else
					currentPoint = graph.snap(e.getPoint());
				// Xor-Paint the new Connector
				paintConnector(graph.getBackground(), Color.black, g);
			}
		}
		else
		{//do nothing

		}
		// Call Superclass
		//		super.mouseDragged(e);
	}

	public PortView getSourcePortAt(Point2D point)
	{
		JGraph graph =controller.getMiddlePanel().getGraph();
		// Disable jumping
		graph.setJumpToDefaultPort(false);
		PortView result;
		try
		{
			// Find a Port View in Model Coordinates and Remember
			result = graph.getPortViewAt(point.getX(), point.getY());
		}
		finally
		{
			graph.setJumpToDefaultPort(true);
		}
		return result;
	}

	// Find a Cell at point and Return its first Port as a PortView
	protected PortView getTargetPortAt(Point2D point)
	{
		// Find a Port View in Model Coordinates and Remember
		JGraph graph =controller.getMiddlePanel().getGraph();
		return graph.getPortViewAt(point.getX(), point.getY());
	}

	// Connect the First Port and the Current Port in the Graph or Repaint
	public void mouseReleased(MouseEvent e)
	{
		//		System.out.println("mouseReleased(). :(x="+e.getX()+",y="+e.getY()+")" );
		// If Valid Event, Current and First Port
		JGraph graph =controller.getMiddlePanel().getGraph();
		graph.repaint();
				// Reset Global Vars
		firstPort = port = null;
		startPoint = currentPoint = null;
		// Call Superclass
		super.mouseReleased(e);
	}

	// Show Special Cursor if Over Port
	public void mouseMoved(MouseEvent e)
	{
		//	System.out.println("mouseMoved().:(x="+e.getX()+",y="+e.getY()+")" );
		// Check Mode and Find Port
		JGraph graph =controller.getMiddlePanel().getGraph();
		if (e != null && getSourcePortAt(e.getPoint()) != null
				&& graph.isPortsVisible())
		{
			// Set Cusor on Graph (Automatically Reset)
			graph.setCursor(new Cursor(Cursor.HAND_CURSOR));
			// Consume Event
			// Note: This is to signal the BasicGraphUI's
			// MouseHandle to stop further event processing.
			e.consume();
		}
	}

	// Use Xor-Mode on Graphics to Paint Connector
	protected void paintConnector(Color fg, Color bg, Graphics g)
	{
		// Set Foreground
		g.setColor(fg);
		// Set Xor-Mode Color
		g.setXORMode(bg);
		// Highlight the Current Port
		JGraph graph =controller.getMiddlePanel().getGraph();
		paintPort(graph.getGraphics());
		// If Valid First Port, Start and Current Point
		if (firstPort != null && startPoint != null && currentPoint != null)
			// Then Draw A Line From Start to Current Point
			g.drawLine((int) startPoint.getX(), (int) startPoint.getY(),
					(int) currentPoint.getX(), (int) currentPoint.getY());
	}

	// Use the Preview Flag to Draw a Highlighted Port
	protected void paintPort(Graphics g)
	{
		// If Current Port is Valid
		if (port != null)
		{
			// If Not Floating Port...
			boolean o = (GraphConstants.getOffset(port.getAttributes()) != null);
			// ...Then use Parent's Bounds
			//			Rectangle2D r = (o) ? port.getBounds() : port.getParentView()
			//					.getBounds();
			Rectangle2D r = port.getBounds();
			// Scale from Model to Screen
			JGraph graph =controller.getMiddlePanel().getGraph();
			r = graph.toScreen((Rectangle2D) r.clone());
			// Add Space For the Highlight Border
			r.setFrame(r.getX() - 3, r.getY() - 3, r.getWidth() + 6, r
					.getHeight() + 6);
			// Paint Port in Preview (=Highlight) Mode
			graph.getUI().paintCell(g, port, r, true);
		}
	}

	/**
	 * Create popup menu
	 * @param pt
	 * @param cell
	 * @return
	 */
	protected JPopupMenu createPopupMenu(final Point pt, final Object cell)
	{
		
		if(popupMenu==null)
		{
			popupMenu = new JPopupMenu();
			deleteAction=new GraphDeleteAction(controller);
			//System.out.println("delete action's mnemonic is: " + deleteAction.getMnemonic());
			JMenuItem menuItem = new JMenuItem(deleteAction);

			popupMenu.add(menuItem);
			popupMenu.addSeparator();
			//Delete All Action
			GraphDeleteAllAction deleteAllAction = new GraphDeleteAllAction(controller.getMiddlePanel(), controller);
			menuItem = new JMenuItem(deleteAllAction);
			popupMenu.add(menuItem);
			
			popupMenu.addSeparator();
			nameMapping = new NameMappingAction(controller);
			menuItem = new JMenuItem(nameMapping);
			popupMenu.add(menuItem);
		}

		//        vacabularyMappingEditAction.setEnabled(false);
		JGraph graph =controller.getMiddlePanel().getGraph();
		deleteAction.setEnabled(true);
		return popupMenu;
	}

	/**
	 * Return true if and only if the port is a child of FunctionBoxCell, future enhancement may be needed.
	 * @param localPort
	 * @return true if and only if the port is a child of FunctionBoxCell, future enhancement may be needed.
	 */
	private boolean isValidPort(PortView localPort)
	{
		if(localPort==null)
		{
			return false;
		}
		Object obj = localPort.getCell();
		if (obj instanceof DefaultPort)
		{
			DefaultPort portCell = (DefaultPort) obj;
			if(!portCell.getEdges().isEmpty())
			{//return false if the port has been linked by edge.
				return false;
			}
			/*else if (portCell.getParent() instanceof FunctionBoxGraphCell)
			{
//				System.out.println("port " + localPort + "'s parent is FunctionBoxCell. Will return true.");
				return true;
			}*/
			else
			{
				return false;
			}
		}
		else
		{
			return false;
		}
	}

}
/**
 * HISTORY: $Log: not supported by cvs2svn $
 * HISTORY: Revision 1.3  2009/10/30 14:35:52  wangeug
 * HISTORY: clean codes, clean selection on tree nodes and graph
 * HISTORY:
 * HISTORY: Revision 1.2  2008/12/10 15:43:02  linc
 * HISTORY: Fixed component id generator and delete link.
 * HISTORY:
 * HISTORY: Revision 1.1  2008/10/30 16:02:14  linc
 * HISTORY: updated.
 * HISTORY:
 */
