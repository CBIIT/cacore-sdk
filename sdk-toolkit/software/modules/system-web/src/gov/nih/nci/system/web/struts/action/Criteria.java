/*L
 *  Copyright Ekagra Software Technologies Ltd.
 *  Copyright SAIC, SAIC-Frederick
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/cacore-sdk/LICENSE.txt for details.
 */

package gov.nih.nci.system.web.struts.action;

import gov.nih.nci.system.web.ajax.tree.Category;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.struts2.ServletActionContext;

public class Criteria extends BaseActionSupport {

	private static final long serialVersionUID = 1234567890L;

	private static Logger log = Logger.getLogger(Criteria.class.getName());

	private String nodeId;
	private Category currentCategory;

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public String getNodeId() {
		return nodeId;
	}

	public String execute() throws Exception {
		
		currentCategory = Category.getById(nodeId);
		log.debug(currentCategory);

		HttpServletRequest request = ServletActionContext.getRequest();

		// A Workaround for Criteria.jsp, which requires embedded JSP logic
		request.setAttribute("klassName", getSelectedDomain());
		request.setAttribute("nodeId", nodeId);

		return SUCCESS;
	}
	
	public String getJavaDocsClassName() {
		return (getFullyQualClassName().replace('.','/')+".html");
	}

	public String getFullyQualClassName() {
		return getPackage() + "." + getNodeName();
	}

	public String getNodeName() {
		return currentCategory.getName();
	}

	public String getPackage() {
		return currentCategory.getPackageName();
	}

	public String getSelectedDomain() {
		return getPackage() + "." + getNodeName();
	}
	
}
