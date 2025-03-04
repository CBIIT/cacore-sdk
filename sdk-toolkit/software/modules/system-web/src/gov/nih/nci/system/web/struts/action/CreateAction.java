/*L
 *  Copyright Ekagra Software Technologies Ltd.
 *  Copyright SAIC, SAIC-Frederick
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/cacore-sdk/LICENSE.txt for details.
 */

package gov.nih.nci.system.web.struts.action;

import gov.nih.nci.system.util.ClassCache;
import gov.nih.nci.system.util.SystemConstant;
import gov.nih.nci.system.web.RESTfulResource;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.io.InputStream;
import java.lang.reflect.*;
import java.net.URI;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.log4j.Logger;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.dispatcher.SessionMap;
import org.apache.struts2.util.ServletContextAware;
import org.jdom.Element;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import org.apache.struts2.dispatcher.SessionMap;
import org.acegisecurity.Authentication;
import javax.ws.rs.core.Response.Status;
import com.opensymphony.xwork2.ActionContext;
import org.apache.commons.codec.binary.Base64;
import gov.nih.nci.system.web.util.RESTUtil;

public class CreateAction extends RestQuery {

    private static final long serialVersionUID = 1234567890L;

    public static Logger log = Logger.getLogger(CreateAction.class.getName());

    //Query parameters
    private String query;
    private String btnSearch;
    private String searchObj;
    private String selectedDomain;

	public String execute() throws Exception {

		HttpServletRequest request = ServletActionContext.getRequest();
		ServletContext context = ServletActionContext.getServletContext();

		String selectedSearchDomain=null;
		String query=null;

		String submitValue = getBtnSearch();
		log.debug("submitValue: " + submitValue);

		init();
		String className = getSelectedDomain();
		String base64encodedUsernameAndPassword = null;

		log.debug("className (selectedDomain): "+ getSelectedDomain());

		if(submitValue != null && submitValue.equalsIgnoreCase("Submit"))
		{
		    query = "GetHTML?query=";

		   	selectedSearchDomain = getSearchObj();
		   	log.debug("selectedSearchDomain: "+ selectedSearchDomain);
			try
			{

		   	Object instance = prepareObject(request);

		   	String url = request.getRequestURL().toString();
		   	String restURL = url.substring(0, url.indexOf("Create.action"));
		   	WebClient client = WebClient.create(restURL);
		   	client.path("rest/"+selectedDomain.substring(selectedDomain.lastIndexOf(".")+1, selectedDomain.length()));
		   	client.type("application/xml").accept("application/xml");
			SessionMap session = (SessionMap) ActionContext.getContext().get(
					ActionContext.SESSION.toString());
			org.acegisecurity.context.SecurityContext scontext = (org.acegisecurity.context.SecurityContext) session
					.get("ACEGI_SECURITY_CONTEXT");
			if(scontext != null)
			{
				Authentication authentication = scontext .getAuthentication();
				String userName = ((org.acegisecurity.userdetails.User) authentication
						.getPrincipal()).getUsername();
				String password = authentication.getCredentials().toString();
				base64encodedUsernameAndPassword = new String(Base64.encodeBase64((userName + ":" + password).getBytes()));
				client.header("Authorization", "Basic " + base64encodedUsernameAndPassword);
			}
			else
			{
				if(secured)
				{
					request.setAttribute("message", "Invalid authentication");
					return SUCCESS;
				}

			}

			   	prepareAssociations(request, instance, className, base64encodedUsernameAndPassword);

				Response r = client.put(instance);
		   		//log.debug("Create status: "+r.getStatus());
		   		if(r.getStatus() == Status.OK.getStatusCode() || r.getStatus() == Status.CREATED.getStatusCode())
		   		{
					InputStream is = (InputStream) r.getEntity();

					org.jdom.input.SAXBuilder builder = new org.jdom.input.SAXBuilder(
							false);
					org.jdom.Document jDoc = builder.build(is);
					Element root = jDoc.getRootElement();
					Element messageEle = root.getChild("message");
					String href = messageEle.getText();
					String newId = href.substring(href.lastIndexOf("/")+1);
					String message = "Successfully created "+ selectedDomain.substring(selectedDomain.lastIndexOf(".")+1, selectedDomain.length()) +" with Id: "+newId;
					request.setAttribute("message", org.apache.commons.lang.StringEscapeUtils.escapeHtml(message));
					request.setAttribute("created", "true");
		   		}
		   		else
		   		{
					InputStream is = (InputStream) r.getEntity();

					org.jdom.input.SAXBuilder builder = new org.jdom.input.SAXBuilder(
							false);
					org.jdom.Document jDoc = builder.build(is);
					Element root = jDoc.getRootElement();
					Element message = root.getChild("message");
					String error = root.getText();
					if(message != null)
						error = message.getText();
					
					String messageStr = "Failed to create: "+org.apache.commons.lang.StringEscapeUtils.escapeHtml(error);
					request.setAttribute("message", messageStr);
					request.setAttribute("created", "false");
		   		}
			}
			catch(WebApplicationException e)
			{
				e.printStackTrace();
				String message = "Failed to create due to: "+ org.apache.commons.lang.StringEscapeUtils.escapeHtml(e.getMessage());
				log.debug("message2 "+message);
				request.setAttribute("message", message);
				request.setAttribute("created", "false");

			}
			catch(NumberFormatException e)
			{
				e.printStackTrace();
				String message = "Failed to create due to: "+org.apache.commons.lang.StringEscapeUtils.escapeHtml(e.getMessage());
				log.debug("message1 "+message);
				request.setAttribute("message", message);
				request.setAttribute("created", "false");

			}
			catch(Exception e)
			{
				e.printStackTrace();
				String message = "Failed to create due to: "+ org.apache.commons.lang.StringEscapeUtils.escapeHtml(e.getMessage());
				log.debug("message1 "+message);
				request.setAttribute("message", message);
				request.setAttribute("created", "false");

			}
		}
		return SUCCESS;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getBtnSearch() {
		return btnSearch;
	}

	public void setBtnSearch(String btnSearch) {
		this.btnSearch = btnSearch;
	}

	public String getSearchObj() {
		return searchObj;
	}

	public void setSearchObj(String searchObj) {
		this.searchObj = searchObj;
	}

	public String getSelectedDomain() {
		return selectedDomain;
	}

	public void setSelectedDomain(String selectedDomain) {
		this.selectedDomain = selectedDomain;
	}


	private Object prepareObject(HttpServletRequest request)
	throws NumberFormatException, Exception
	{

		StringBuilder sb = new StringBuilder();
		Enumeration<String> parameters = request.getParameterNames();

		Object instance = null;
		try
		{
			Class klass = Class.forName(selectedDomain);
			instance = klass.newInstance();
	 		while(parameters.hasMoreElements())
	 		{
	     		String parameterName = (String)parameters.nextElement();
	     		if(!parameterName.equals("klassName") && !parameterName.equals("searchObj") && !parameterName.equals("BtnSearch") && !parameterName.equals("username") && !parameterName.equals("password") && !parameterName.equals("selectedDomain"))
	     		{
	     			String parameterValue = (request.getParameter(parameterName)).trim();
	     			setParameterValue(klass, instance, parameterName, parameterValue);
	     		}
	     	}
		}
		catch(NumberFormatException e)
		{
			throw e;
		}
		catch(Exception e)
		{
			throw e;
		}
		return instance;
	}


	private void setParameterValue(Class klass, Object instance, String name, String value)
	throws NumberFormatException, Exception
	{
		if(value != null && value.trim().length()==0)
			value = null;

		try
		{
			String paramName = name.substring(0,1).toUpperCase()+name.substring(1);
			Method[] allMethods = klass.getMethods();
		    for (Method m : allMethods) {
				String mname = m.getName();
				if(mname.equals("get"+paramName))
				{
					Class type = m.getReturnType();
					Class[] argTypes = new Class[] { type };

					Method method = null;
					while (klass != Object.class) {
					     try {
					    	  Method setMethod = klass.getDeclaredMethod("set"+paramName, argTypes);
					    	  setMethod.setAccessible(true);
					    	  Object converted = convertValue(type, value);
					    	  if(converted != null)
					          	setMethod.invoke(instance, converted);
					          break;
					     } catch(NumberFormatException e) {
					    	 throw e;
					     } catch (NoSuchMethodException ex) {
					    	 klass = klass.getSuperclass();
					     }
					     catch(Exception e)
					     {
							 throw e;
						 }
					}
				}
		    }
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw e;
		}
	}

	public Object convertValue(Class klass, Object value) throws NumberFormatException, Exception {

		String fieldType = klass.getName();
		if(value == null)
			return null;

		Object convertedValue = null;
		try {
			if (fieldType.equals("java.lang.Long")) {
					convertedValue = new Long((String) value);
			} else if (fieldType.equals("java.lang.Integer")) {
					convertedValue = new Integer((String) value);
			} else if (fieldType.equals("java.lang.String")) {
				convertedValue =  value;
			} else if (fieldType.equals("java.lang.Float")) {
					convertedValue = new Float((String) value);
			} else if (fieldType.equals("java.lang.Double")) {
					convertedValue = new Double((String) value);
			} else if (fieldType.equals("java.lang.Boolean")) {
					convertedValue = new Boolean((String) value);
			} else if (fieldType.equals("java.util.Date")) {
				SimpleDateFormat format = new SimpleDateFormat("MM-dd-yyyy");
				convertedValue = format.parse((String) value);
			} else if (fieldType.equals("java.net.URI")) {
					convertedValue = new URI((String) value);
			} else if (fieldType.equals("java.lang.Character")) {
					convertedValue = new Character(((String) value).charAt(0));
			} else if (klass.isEnum()) {
					Class enumKlass = Class.forName(fieldType);
					convertedValue = Enum.valueOf(enumKlass, (String) value);
			} else {
				throw new Exception("type mismatch - " + fieldType);
			}

		} catch(NumberFormatException e) {
			 e.printStackTrace();
			 log.error("ERROR : " + e.getMessage());
			 throw e;
		}
		catch (Exception ex) {
			 ex.printStackTrace();
			 log.error("ERROR : " + ex.getMessage());
			 throw ex;
		}
		return convertedValue;
	}

}


