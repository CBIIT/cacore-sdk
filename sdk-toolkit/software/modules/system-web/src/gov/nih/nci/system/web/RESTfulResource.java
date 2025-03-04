/*L
 *  Copyright Ekagra Software Technologies Ltd.
 *  Copyright SAIC, SAIC-Frederick
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/cacore-sdk/LICENSE.txt for details.
 */

package gov.nih.nci.system.web;

import gov.nih.nci.system.applicationservice.ApplicationException;
import gov.nih.nci.system.applicationservice.ApplicationService;
import gov.nih.nci.system.applicationservice.WritableApplicationService;
import gov.nih.nci.system.client.proxy.ApplicationServiceProxy;
import gov.nih.nci.system.metadata.MetadataCache;
import gov.nih.nci.system.query.SDKQueryResult;
import gov.nih.nci.system.query.example.DeleteExampleQuery;
import gov.nih.nci.system.query.example.InsertExampleQuery;
import gov.nih.nci.system.query.example.UpdateExampleQuery;
import gov.nih.nci.system.query.hibernate.HQLCriteria;
import gov.nih.nci.system.query.hql.DeleteHQLQuery;
import gov.nih.nci.system.util.ClassCache;
import gov.nih.nci.system.web.util.RESTUtil;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.Method;
import java.net.URI;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.acegisecurity.Authentication;
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import org.aopalliance.aop.Advice;
import org.apache.log4j.Logger;
import org.springframework.aop.Advisor;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.apache.log4j.Logger;

/**
 * Super class for all RESTful resources providing common functionality
 * 
 * 
 */
public class RESTfulResource {

	public static Logger log = Logger
			.getLogger(RESTfulResource.class.getName());
	public static final String LESS_THAN = "<";
	public static final String GREATER_THAN = ">";
	public static final String NOT_EQUAL = "<>";

	protected WebApplicationContext ctx;
	protected ClassCache classCache;
	protected ApplicationService applicationService;
	protected WritableApplicationService writableApplicationService;
	protected int pageSize;
	protected boolean secured = false;
	protected boolean metadata = false;
	protected boolean isoEnabled = false;
	String isoprefix = "gov.nih.nci.iso21090.";

	/*
	 * Initialize resource with ApplicationService and other properties
	 */
	public RESTfulResource(@Context ServletContext context) {
		try {
			ctx = WebApplicationContextUtils.getWebApplicationContext(context);
			Properties systemProperties = (Properties) ctx
					.getBean("WebSystemProperties");

			classCache = (ClassCache) ctx.getBean("ClassCache");
			applicationService = (ApplicationService) ctx
					.getBean("ApplicationServiceImpl");

			try {
				String pageCount = systemProperties.getProperty("rowCounter");
				if (pageCount != null) {
					pageSize = Integer.parseInt(pageCount);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				pageSize = 200;
			}

			String securityEnabled = (String) systemProperties
					.getProperty("securityEnabled");
			secured = "yes".equalsIgnoreCase(securityEnabled)
					|| "true".equalsIgnoreCase(securityEnabled);

			String metadataEnabled = (String) systemProperties
					.getProperty("caDSRMetadataEnabled");
			metadata = "yes".equalsIgnoreCase(metadataEnabled)
					|| "true".equalsIgnoreCase(metadataEnabled);
			
			String isoEnabledStr = (String) systemProperties
					.getProperty("enableISO21090DataTypes");
			isoEnabled = "yes".equalsIgnoreCase(isoEnabledStr)
					|| "true".equalsIgnoreCase(isoEnabledStr);
		} catch (Exception e) {
			log.error("Error in constructing REST resource: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/*
	 * Validate given criteria. If not valid, respond with XML including valid
	 * fields If ISO is enabled, attribute name can have sub attribute
	 * 
	 * @param Map matrix params from the request
	 * 
	 * @param List<Field> searchable fields for a selected domain class
	 */
	protected void validateCriteria(String className, Map matrixParams,
			List<Field> searchFields) throws WebApplicationException {
		List<String> validAttrs = new ArrayList();
		log.debug("validateCriteria: " + "className: " + className
				+ "matrixParams: " + matrixParams + "searchFields: "
				+ searchFields);

		// If ISO is enabled, get expanded searchable fields
		// ex: value1.part_0.code
		// Add Id column(non-expanded) to searchable fileds to consider
		if (isoEnabled) {
			String idColName = null;
			try {
				idColName = classCache.getClassIdName(Class.forName(className));
			} catch (ClassNotFoundException e) {
				log.error("Failed to get Id column for class: " + className
						+ " Message:" + e.getMessage());
				e.printStackTrace();
			}
			for (Field field : searchFields) {
				List isoFields = RESTUtil.getSearchableIsoDataTypeFields(
						className, field.getName(), classCache);
				List<String> fNames = RESTUtil
						.getSearchableIsoDataTypeFieldsWithFN(field, isoFields);
				validAttrs.addAll(fNames);
				if (idColName != null)
					validAttrs.add(idColName);
			}

		} else {
			for (Field field : searchFields)
				validAttrs.add(field.getName());
		}
		log.debug("validAttrs: " + validAttrs);
		if (matrixParams == null) {
			ResponseBuilder builder = Response.status(Status.BAD_REQUEST);
			builder.type("application/xml");
			StringBuffer buffer = new StringBuffer();
			buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			buffer.append("<response>");
			buffer.append("<type>ERROR</type>");
			buffer.append("<code>SEARCH_CRITERIA_1</code>");
			buffer.append("<resource>"+className+"</resource>");
			buffer.append("<path>search</path>");
			buffer.append("<message>Search criteria is missing</message>");
			buffer.append("<valid>");
			for (String attrName : validAttrs) {
				buffer.append("<attribute>");
				buffer.append(attrName);
				buffer.append("</attribute>");
			}
			buffer.append("</valid>");
			buffer.append("</response>");
			builder.entity(buffer.toString());
			throw new WebApplicationException(builder.build());
		}

		Iterator iter = matrixParams.keySet().iterator();
		List<String> invalidAttrs = new ArrayList<String>();

		while (iter.hasNext()) {
			boolean found = false;
			String fullName = (String) iter.next();
			log.debug("fullName: " + fullName);
			String attrName = getAttributeName(fullName);
			log.debug("attrName: " + attrName);
			if (!validAttrs.contains(attrName))
				invalidAttrs.add(fullName);
		}

		log.debug("invalidAttrs: " + invalidAttrs);
		if (invalidAttrs.size() > 0) {
			log.debug("inside invalidAttrs: " + invalidAttrs);
			ResponseBuilder builder = Response.status(Status.BAD_REQUEST);
			builder.type("application/xml");
			String attrs = "";
			StringBuffer buffer = new StringBuffer();
			buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			buffer.append("<response>");
			buffer.append("<type>ERROR</type>");
			buffer.append("<code>SEARCH_CRITERIA_2</code>");
			buffer.append("<resource>"+className+"</resource>");
			buffer.append("<path>search</path>");
			buffer.append("<message>Invalid Search criteria</message>");
			buffer.append("<valid>");
			for (String attrName : validAttrs) {
				buffer.append("<attribute>");
				buffer.append(attrName);
				buffer.append("</attribute>");
			}

			buffer.append("</valid>");
			buffer.append("<invalid>");
			for (String attr : invalidAttrs) {
				attrs = attr + " " + attrs;
				buffer.append("<attribute>");
				buffer.append(attr);
				buffer.append("</attribute>");

			}
			buffer.append("</invalid>");
			buffer.append("</response>");
			builder.entity(buffer.toString());
			throw new WebApplicationException(builder.build());
		}
	}

	private String getAttributeName(String name) {
		if (name.indexOf(NOT_EQUAL) > 0)
			return name.substring(0, name.indexOf(NOT_EQUAL));
		else if (name.indexOf(GREATER_THAN) > 0)
			return name.substring(0, name.indexOf(GREATER_THAN));
		else if (name.indexOf(LESS_THAN) > 0)
			return name.substring(0, name.indexOf(LESS_THAN));
		// else if (isoEnabled && name.indexOf(".") > 0)
		// return name.substring(0, name.indexOf("."));
		else
			return name;
	}

	private String getAttributeValue(String name) {
		if (name.indexOf(NOT_EQUAL) > 0)
			return name.substring(name.indexOf(NOT_EQUAL) + 2, name.length());
		else if (name.indexOf(GREATER_THAN) > 0)
			return name
					.substring(name.indexOf(GREATER_THAN) + 1, name.length());
		else if (name.indexOf(LESS_THAN) > 0)
			return name.substring(name.indexOf(LESS_THAN) + 1, name.length());
		else
			return name;
	}

	private String getAttributeOperator(String name) {
		if (name.indexOf(NOT_EQUAL) > 0)
			return "<>";
		else if (name.indexOf(GREATER_THAN) > 0)
			return ">";
		else if (name.indexOf(LESS_THAN) > 0)
			return "<";
		else
			return "=";
	}

	protected ResourceLink getSelfLink(UriInfo uriInfo) {
		String fullpath = uriInfo.getAbsolutePath().toString();
		ResourceLink link = new ResourceLink("self", "application/xml",
				fullpath);
		return link;
	}

	protected ResourceLink getNextLink(UriInfo uriInfo, int resultsetCount,
			int totalResults, int maxCountPerQuery) {
		// log.debug("resultsetCount: "+resultsetCount);
		// log.debug("totalResults: "+totalResults);
		// log.debug("maxCountPerQuery: "+maxCountPerQuery);
		if (resultsetCount == totalResults)
			return null;

		int startIndex = 1;
		int batchSize = pageSize;
		ResourceLink link = null;
		String bStart = uriInfo.getQueryParameters().getFirst("start");
		if (bStart != null)
			startIndex = Integer.parseInt(bStart);

		String bSize = uriInfo.getQueryParameters().getFirst("size");
		if (bSize != null) {
			batchSize = Integer.parseInt(bSize);
		}

		String startStr = null;
		String sizeStr = null;
		int newStart = 1;
		int newSize = maxCountPerQuery;

		// No start is given
		if (startIndex == -1) {
			newStart = resultsetCount + 1;
		} else {
			if ((startIndex + batchSize) > totalResults)
				return null;

			newStart = startIndex + resultsetCount;
			if (newStart > totalResults)
				return null;
		}

		// No start is given
		if (batchSize == -1) {
			newSize = maxCountPerQuery;
		} else {
			if (batchSize > maxCountPerQuery)
				newSize = maxCountPerQuery;
			else
				newSize = batchSize;
		}

		String fullpath = uriInfo.getAbsolutePath().toString();
		String path = removeStartSize(uriInfo, fullpath);

		String href = null;
		if (path.indexOf("?") > 0)
			href = path + "&start=" + newStart + "&size=" + newSize + "&total="+totalResults;
		else
			href = path + "?start=" + newStart + "&size=" + newSize + "&total="+totalResults;

		link = new ResourceLink("next", "application/xml", href);
		// log.debug(link.toString());
		return link;

	}

	protected ResourceLink getPreviousLink(UriInfo uriInfo, int resultsetCount,
			int totalResults, int maxCountPerQuery) {
		if (resultsetCount == totalResults)
			return null;

		int startIndex = 1;
		int batchSize = pageSize;
		ResourceLink link = null;
		String bStart = uriInfo.getQueryParameters().getFirst("start");
		if (bStart != null)
			startIndex = Integer.parseInt(bStart);

		String bSize = uriInfo.getQueryParameters().getFirst("size");
		if (bSize != null) {
			batchSize = Integer.parseInt(bSize);
		}

		String startStr = null;
		String sizeStr = null;
		int newStart = 1;
		int newSize = maxCountPerQuery;

		// No start is given
		if (startIndex == -1) {
			return null;
		} else {
			if (startIndex >= resultsetCount) {
				newStart = startIndex - maxCountPerQuery;
			} else
				return null;

		}
		if (newStart < 1)
			newStart = 1;

		// No start is given
		if (batchSize == -1) {
			newSize = maxCountPerQuery;
		} else {
			if (batchSize > maxCountPerQuery)
				newSize = maxCountPerQuery;
			else
				newSize = batchSize;
		}

		String fullpath = uriInfo.getAbsolutePath().toString();
		String path = removeStartSize(uriInfo, fullpath);

		String href = null;
		if (path.indexOf("?") > 0)
			href = path + "&start=" + newStart + "&size=" + newSize + "&total="+totalResults;
		else
			href = path + "?start=" + newStart + "&size=" + newSize + "&total="+totalResults;

		link = new ResourceLink("previous", "application/xml", href);
		// log.debug(link.toString());
		return link;

	}

	private String removeStartSize(UriInfo uriInfo, String fullpath) {
		String bStart = uriInfo.getQueryParameters().getFirst("start");
		String startStr = null;
		String sizeStr = null;
		String preHref = null;
		if (bStart != null)
			startStr = "start=" + bStart;

		String bSize = uriInfo.getQueryParameters().getFirst("size");
		if (bSize != null)
			sizeStr = "size=" + bSize;

		if (startStr != null && fullpath.indexOf(startStr) > 0) {
			String preHref1 = fullpath.substring(0, fullpath.indexOf(startStr));
			// log.debug("preHref1: "+preHref1);
			if (preHref1.endsWith("&"))
				preHref1 = preHref1.substring(0, preHref1.length() - 1);

			String preHref2 = fullpath.substring(fullpath.indexOf(startStr)
					+ startStr.length(), fullpath.length());
			// log.debug("preHref2: "+preHref2);
			preHref = preHref1 + preHref2;
			// log.debug("preHref3: "+preHref1);

		} else
			preHref = fullpath;

		// log.debug("preHref: "+preHref);

		if (sizeStr != null && preHref.indexOf(sizeStr) > 0) {
			String preHref1 = preHref.substring(0, preHref.indexOf(sizeStr));
			// log.debug("preHref4: "+preHref1);
			if (preHref1.endsWith("&"))
				preHref1 = preHref1.substring(0, preHref1.length() - 1);

			String preHref2 = fullpath.substring(preHref.indexOf(sizeStr)
					+ sizeStr.length(), preHref.length());
			// log.debug("preHref5: "+preHref2);
			preHref = preHref1 + preHref2;
			// log.debug("preHref6: "+preHref1);

		} else {
			if (preHref == null)
				preHref = fullpath;

		}

		// log.debug("preHref****: "+preHref);
		return preHref;

	}

	protected HQLCriteria buildHQLCriteria(String className,
			List<Field> searchFields, Map matrixParams, UriInfo uriInfo,
			String dbType) {
		int startIndex = -1;
		int totalSize = -1;
		validateCriteria(className, matrixParams, searchFields);
		// System.out.println("uriInfo.getPathParameters(): "
		// + uriInfo.getPathParameters());
		// System.out.println("uriInfo.getQueryParameters(): "
		// + uriInfo.getQueryParameters());
		String bStart = uriInfo.getQueryParameters().getFirst("start");
		// System.out.println("bStart: " + bStart);
		if (bStart != null)
			startIndex = Integer.parseInt(bStart);
		else 
			startIndex = 1;
		
		String bSize = uriInfo.getQueryParameters().getFirst("size");
		// System.out.println("bSize: " + bSize);
		if (bSize != null)
			totalSize = Integer.parseInt(bSize);
		else 
			totalSize = pageSize;
		
		// log.debug("startIndex: "+startIndex);
		// log.debug("totalSize: "+totalSize);

		HQLCriteria hcriteria = null;
		Map<String, List> whereMap = buildWhereCriteria(className,
				searchFields, matrixParams, uriInfo, null, dbType);
		String whereCriteria = whereMap.keySet().iterator().next();
		List params = whereMap.get(whereCriteria);
		if (whereCriteria.length() > 0)
			hcriteria = new HQLCriteria("from " + className + " a " + " where "
					+ whereCriteria, params, startIndex - 1, totalSize);
		else
			hcriteria = new HQLCriteria("from " + className + " a ", params,
					startIndex - 1, totalSize);

		return hcriteria;
	}

	/**
	 * Generate HQLCriteria for an association
	 * 
	 * @param sourceClass
	 *            source class of the association
	 * @param associationName
	 *            association name to the source class
	 * @param start
	 *            if paging, start index
	 * @param size
	 *            if paging, size of the page
	 * @param searchFields
	 * @param matrixParams
	 * @param uriInfo
	 * @return HQLCriteria
	 * @throws WebApplicationException
	 */
	public HQLCriteria getAssociationCriteria(Class sourceClass,
			String associationName, int start, int size,
			List<Field> searchFields, Map matrixParams, UriInfo uriInfo,
			String dbType) throws WebApplicationException {

		try {
			validateCriteria(sourceClass.getName(), matrixParams, searchFields);
			String assocType = "";
			try {
				assocType = classCache.getAssociationType(sourceClass,
						associationName);
			} catch (Exception e) {
				throw new ApplicationException(e);
			}
			String hql = "";
			log.debug("sourceClass.getName() " + sourceClass.getName());
			log.debug("associationName " + associationName);
			boolean isCollection = classCache.isCollection(
					sourceClass.getName(), associationName);
			log.debug("isCollection " + isCollection);
			log.debug("sourceClass: " + sourceClass);
			String idName = classCache.getClassIdName(sourceClass, true);
			log.debug("idName: " + idName);
			log.debug("assocType: " + assocType);
			String assosIdName = classCache.getClassIdName(
					Class.forName(assocType), true);
			if (assosIdName == null || assosIdName.trim().length() == 0) {
				ResponseBuilder builder = Response
						.status(Status.NOT_ACCEPTABLE);
				StringBuffer buffer = new StringBuffer();
				buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
				buffer.append("<response>");
				buffer.append("<type>MESSAGE</type>");
				buffer.append("<code>MESSAGE_100</code>");
				buffer.append("<resource>"+sourceClass.getName()+"</resource>");
				buffer.append("<path>search</path>");
				buffer.append("<message>No primary key present on " + assocType
						+ ". Aborting Query!</message>");
				buffer.append("</response>");
				builder.entity(buffer.toString());
				throw new WebApplicationException(builder.build());
			}

			log.debug("assosIdName: " + assosIdName);
			String whereCriteria = null;
			List params = null;

			if (isCollection) {
				Map<String, List> whereMap = buildWhereCriteria(
						sourceClass.getName(), searchFields, matrixParams,
						uriInfo, "src", dbType);

				if (whereMap.size() == 0) {
					ResponseBuilder builder = Response.status(Status.NOT_FOUND);
					builder.type("application/xml");
					builder.entity("<message>Invalid Criteria</message>");
					throw new WebApplicationException(builder.build());
				}

				whereCriteria = whereMap.keySet().iterator().next();
				params = whereMap.get(whereCriteria);

				hql = "select dest from " + sourceClass.getName()
						+ " as src inner join src." + associationName
						+ " dest where " + whereCriteria;
				log.debug("hql: collection: " + hql);
			} else {
				Map<String, List> whereMap = buildWhereCriteria(
						sourceClass.getName(), searchFields, matrixParams,
						uriInfo, "src", dbType);

				whereCriteria = whereMap.keySet().iterator().next();
				params = whereMap.get(whereCriteria);

				if (whereMap.size() == 0) {
					ResponseBuilder builder = Response.status(Status.NOT_FOUND);
					builder.type("application/xml");
					builder.entity("<message>Invalid Criteria</message>");
					throw new WebApplicationException(builder.build());
				}

				hql = "select dest from " + assocType + " as dest,"
						+ sourceClass.getName() + " as src where "
						+ whereCriteria + " and src." + associationName + "."
						+ idName + "=dest." + assosIdName;
				log.debug("hql2: " + hql);
			}

			HQLCriteria hqlCriteria = new HQLCriteria(hql, params, start, size);
			return hqlCriteria;
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception ex) {
			ex.printStackTrace();
			ResponseBuilder builder = Response
					.status(Status.INTERNAL_SERVER_ERROR);
			StringBuffer buffer = new StringBuffer();
			buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			buffer.append("<response>");
			buffer.append("<type>ERROR</type>");
			buffer.append("<code>INTERNAL_ERROR_3</code>");
			buffer.append("<resource>"+sourceClass.getName()+"</resource>");
			buffer.append("<path>search</path>");
			buffer.append("<message>Failed to construct association criteria: "
					+ ex.getMessage() + "</message>");
			buffer.append("</response>");
			builder.entity(buffer.toString());
			throw new WebApplicationException(builder.build());
		}

	}

	public HQLCriteria getAssociationCriteria(Class sourceClass,
			String associationName, String id, int start, int size)
			throws WebApplicationException {

		try {
			// validateCriteria(sourceClass.getName(), matrixParams,
			// searchFields);
			String assocType = "";
			try {
				assocType = classCache.getAssociationType(sourceClass,
						associationName);
			} catch (Exception e) {
				throw new ApplicationException(e);
			}
			String hql = "";
			boolean isCollection = classCache.isCollection(
					sourceClass.getName(), associationName);
			log.debug("sourceClass.getName() " + sourceClass.getName());
			log.debug("associationName " + associationName);
			log.debug("isCollection " + isCollection);

			String idName = classCache.getClassIdName(sourceClass, true);
			log.debug("idName2: " + idName);
			String assosIdName = classCache.getClassIdName(
					Class.forName(assocType), true);
			log.debug("assosIdName2: " + assosIdName);
			if (assosIdName == null || assosIdName.trim().length() == 0) {
				ResponseBuilder builder = Response
						.status(Status.NOT_ACCEPTABLE);
				StringBuffer buffer = new StringBuffer();
				buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
				buffer.append("<response>");
				buffer.append("<type>MESSAGE</type>");
				buffer.append("<code>MESSAGE_100</code>");
				buffer.append("<resource>"+sourceClass.getName()+"</resource>");
				buffer.append("<path>search</path>");
				buffer.append("<message>No primary key present on " + assocType
						+ ". Aborting Query!</message>");
				buffer.append("</response>");
				builder.entity(buffer.toString());
				throw new WebApplicationException(builder.build());
			}

			if (isCollection) {
				hql = "select dest from " + sourceClass.getName()
						+ " as src inner join src." + associationName
						+ " dest where src." + assosIdName + " = ?";
				log.debug("hql: getAssociationCriteria collection: " + hql);
			} else {
				hql = "select dest from " + assocType + " as dest,"
						+ sourceClass.getName() + " as src where src."
						+ associationName + "." + idName + "=dest."
						+ assosIdName + " and dest." + assosIdName + " = ?";
				log.debug("hql: getAssociationCriteria : " + hql);
			}

			// log.debug("hql: "+hql);

			Field[] fields = classCache.getAllFields(sourceClass);
			Field idField = RESTUtil.getIdField(fields, idName, classCache);
			List params = new ArrayList(1);
			params.add(convertValues(idField, id));

			HQLCriteria hqlCriteria = new HQLCriteria(hql, params, start, size);
			return hqlCriteria;
		} catch (WebApplicationException e) {
			throw e;
		} catch (Exception ex) {
			ex.printStackTrace();
			ResponseBuilder builder = Response
					.status(Status.INTERNAL_SERVER_ERROR);
			StringBuffer buffer = new StringBuffer();
			buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			buffer.append("<response>");
			buffer.append("<type>ERROR</type>");
			buffer.append("<code>INTERNAL_ERROR_4</code>");
			buffer.append("<resource>"+sourceClass.getName()+"</resource>");
			buffer.append("<path>search</path>");
			buffer.append("<message>Failed to construct criteria: "
					+ ex.getMessage() + "</message>");
			buffer.append("</response>");
			builder.entity(buffer.toString());
			throw new WebApplicationException(builder.build());
		}

	}

	/**
	 * Build where criteria for query based on given search matrix parameters
	 * 
	 * @param className
	 * @param searchFields
	 * @param matrixParams
	 * @param uriInfo
	 * @param alias
	 * @return
	 */
	protected Map<String, List> buildWhereCriteria(String className,
			List<Field> searchFields, Map matrixParams, UriInfo uriInfo,
			String alias, String dbType) throws WebApplicationException {
		int startIndex = -1;
		int totalSize = -1;
		log.debug("uriInfo.getPathParameters(): " + uriInfo.getPathParameters());
		log.debug("uriInfo.getQueryParameters(): "
				+ uriInfo.getQueryParameters());
		String bStart = uriInfo.getQueryParameters().getFirst("start");
		log.debug("bStart: " + bStart);
		if (bStart != null)
			startIndex = Integer.parseInt(bStart);

		String bSize = uriInfo.getQueryParameters().getFirst("size");
		if (bSize != null)
			totalSize = Integer.parseInt(bSize);

		// log.debug("startIndex: "+startIndex);
		// log.debug("totalSize: "+totalSize);

		String whereCriteria = "";
		List criteria = new ArrayList();
		List params = new ArrayList();

		List<String> invalidAttrs = new ArrayList<String>();
		Iterator iter = matrixParams.keySet().iterator();
		String idColName = null;
		try {
			idColName = classCache.getClassIdName(Class.forName(className));
			log.debug("idColName3: " + idColName);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		while (iter.hasNext()) {
			boolean found = false;
			boolean adPart = false;
			String fullName = (String) iter.next();
			String attrName = getAttributeName(fullName);
			log.debug("Building where criteria attrName " + attrName);
			Object attrVal = null;
			String operator = "=";
			if (!fullName.equals(attrName)) {
				attrVal = getAttributeValue(fullName);
				operator = getAttributeOperator(fullName);
			} else
				attrVal = matrixParams.get(fullName);
			log.debug("Building where criteria operator " + operator);
			log.debug("Building where criteria attrVal " + attrVal);
			String attrValue = null;
			if (attrVal instanceof java.util.ArrayList)
				attrValue = (String) ((ArrayList) attrVal).get(0);
			else
				attrValue = (String) attrVal;

			Field field = null;
			Iterator fIter = searchFields.iterator();
			while (fIter.hasNext()) {
				field = (Field) fIter.next();
				log.debug("field...." + field.getName());
				log.debug("field...." + field.getType().getName());
				log.debug("field...." + field.getGenericType().toString());

				if (isoEnabled) {
					List isoFields = RESTUtil.getSearchableIsoDataTypeFields(
							className, field.getName(), classCache);
					List<String> fNames = RESTUtil
							.getSearchableIsoDataTypeFieldsWithFN(field,
									isoFields);
					if (idColName != null)
						fNames.add(idColName);

					if (field.getType().getName()
							.equals("gov.nih.nci.iso21090.Ad"))
						adPart = true;

					if (fNames.contains(attrName)) {
						found = true;
						break;
					}
				} else {
					if (field.getName().equals(attrName)) {
						found = true;
						break;
					}
				}
			}

			if (found) {
				String convertedAttrName = attrName;
				log.debug("convertedAttrName1: " + convertedAttrName);
				log.debug("(idColName .extension " + (idColName + ".extension"));
				if (adPart)
					convertedAttrName = attrName.substring(0,
							attrName.indexOf("part"))
							+ "part_"
							+ attrName.substring(attrName.indexOf("part") + 4,
									attrName.length());
				else if (attrName.equals((idColName + ".extension")))
					convertedAttrName = idColName;

				log.debug("convertedAttrName2: " + convertedAttrName);

				log.debug("field.getType().getName(): "
						+ field.getType().getName());
				if (field.getType().getName().equals("java.lang.String")) {
					if ((attrValue.indexOf("*") != -1)
							|| (attrValue.indexOf("%") != -1)) {
						criteria.add(((alias != null) ? alias + "." : "")
								+ convertedAttrName + " like '"
								+ attrValue.replace("*", "%") + "'");
					} else {
						params.add(attrValue);
						criteria.add(((alias != null) ? alias + "." : "")
								+ convertedAttrName + operator + "? ");
					}
				} else {
					if (attrValue.equals("*")) {
						if (dbType.equalsIgnoreCase("postgresql")) {
							String attName = ((alias != null) ? alias + "."
									: "") + convertedAttrName;
							String castName = "CAST(" + attName + " AS text)";
							criteria.add(castName + " like '%' ");
						} else
							criteria.add(((alias != null) ? alias + "." : "")
									+ convertedAttrName + " like '%' ");

					} else {
						String fieldType = field.getType().getName();
						Object paramValue = null;
						if (fieldType.startsWith(isoprefix)) {
							if (idColName.equals(convertedAttrName))
								paramValue = convertValues(field, attrValue);
							else
								paramValue = convertISOValues(field,
										convertedAttrName, attrValue, idColName);
						} else
							paramValue = convertValues(field, attrValue);

						if (paramValue != null) {

							params.add(paramValue);
							criteria.add(((alias != null) ? alias + "." : "")
									+ convertedAttrName + operator + "? ");
						} else {
							StringBuffer buffer = new StringBuffer();
							ResponseBuilder builder = Response
									.status(Status.INTERNAL_SERVER_ERROR);
							buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
							buffer.append("<response>");
							buffer.append("<type>ERROR</type>");
							buffer.append("<code>INTERNAL_ERROR_2</code>");
							buffer.append("<resource>"+className+"</resource>");
							buffer.append("<path>search</path>");
							buffer.append("<message>Failed to construct criteria</message>");
							buffer.append("</response>");
							builder.entity(buffer.toString());
							throw new WebApplicationException(builder.build());
						}
					}
				}
			} else {
				invalidAttrs.add(attrName);
			}
		}

		if (criteria.size() == 0) {
			StringBuffer buffer = new StringBuffer();
			ResponseBuilder builder = Response.status(Status.NOT_ACCEPTABLE);
			buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			buffer.append("<response>");
			buffer.append("<type>ERROR</type>");
			buffer.append("<code>INVALID_CRITERIA</code>");
			buffer.append("<resource>"+className+"</resource>");
			buffer.append("<path>search</path>");
			buffer.append("<message>Invalid Criteria</message>");
			buffer.append("</response>");
			builder.entity(buffer.toString());
			throw new WebApplicationException(builder.build());
		}

		java.util.Iterator cIter = criteria.iterator();

		while (cIter.hasNext()) {
			String crtra = (String) cIter.next();
			whereCriteria = whereCriteria + crtra + " ";
			if (cIter.hasNext())
				whereCriteria = whereCriteria + " and ";
		}

		log.debug("whereCriteria: " + whereCriteria);
		Map returnMap = new HashMap();
		returnMap.put(whereCriteria, params);
		return returnMap;
	}

	public Object convertISOValues(Field field, String attrName, Object value,
			String idColName) {
		try {
			String fieldType = field.getType().getName();
			// String valueType = value.getClass().getName();
			log.debug("fieldType: " + fieldType);
			log.debug("attrName " + attrName);
			log.debug("fieldName " + field.getName());
			if (!idColName.equals(attrName) && attrName.indexOf(".") == -1) {
				String msg = "Invalid attribute name: " + attrName;

				ResponseBuilder builder = Response
						.status(Status.INTERNAL_SERVER_ERROR);
				StringBuffer buffer = new StringBuffer();
				buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
				buffer.append("<response>");
				buffer.append("<type>ERROR</type>");
				buffer.append("<code>INTERNAL_ERROR_7</code>");
				buffer.append("<resource>convertISOValues</resource>");
				buffer.append("<path>search</path>");
				buffer.append("<message>Failed to construct criteria: " + msg
						+ "</message>");
				buffer.append("</response>");
				builder.entity(buffer.toString());
				throw new WebApplicationException(builder.build());
			}

			String subAttrName = attrName;
			if (attrName.indexOf(".") > 0)
				subAttrName = attrName.substring(attrName.indexOf(".") + 1,
						attrName.length());

			Object partObj = null;
			Object attr = null;
			Field partField = field;
			Class classType = partField.getDeclaringClass();

			while (true) {
				int index = subAttrName.indexOf(".");
				log.debug("index: " + index);
				if (index != -1) {
					String part = subAttrName.substring(0,
							subAttrName.indexOf("."));
					log.debug("part1: " + part);
					Field partField2 = RESTUtil.getField(partField.getType(),
							part, classCache);
					log.debug("partField2: " + partField2);
					log.debug("partField.getDeclaringClass(): "
							+ partField.getType().getName());
					if (partField2 == null) {
						String newPart = part;
						log.debug("new part: " + newPart);

						if (partField.getType().getName()
								.equals("gov.nih.nci.iso21090.Ad")) {
							if (part.startsWith("part"))
								newPart = "part";
						}
						log.debug("new part: " + newPart);

						if (partField.getType().getName()
								.equals("java.util.Set")) {

							log.debug("partField.getGenericType(): "
									+ partField.getGenericType().toString());
							Class<?> dataType = null;
							Type type = partField.getGenericType();
							if (type instanceof ParameterizedType) {
								ParameterizedType paramType = (ParameterizedType) type;
								dataType = (Class<?>) paramType
										.getActualTypeArguments()[0];
							} else if (type instanceof Class) {
								dataType = (Class<?>) type;
							}

							log.debug("partField.getGenericType(): type "
									+ dataType.getName());
							log.debug("partField.getGenericType(): type "
									+ dataType.toString());

							// partField =
							// integerListClass.getDeclaredField(newPart);
							if (part.startsWith("part"))
								newPart = "part";
						} else
							partField = partField.getType().getDeclaredField(
									newPart);
					} else
						partField = partField2;
					subAttrName = subAttrName.substring(
							subAttrName.indexOf(".") + 1, subAttrName.length());
					log.debug("subAttrName1: " + subAttrName);
					log.debug("partField1: " + partField.getName());
					log.debug("partField1 type " + field.getType().getName());
				} else {
					log.debug("subAttrName2: " + subAttrName);
					log.debug("partField2: " + partField.getName());
					if (field.getType().getName()
							.equals("gov.nih.nci.iso21090.Ad")) {
						log.debug("partField2222 type "
								+ field.getType().getName());
						log.debug("partField.getType()xxxx "
								+ partField.getType().getName());
						log.debug("subAttrName " + subAttrName);
						Field partFieldAd = null;
						if (partField.getType().getName()
								.equals("java.util.List")) {
							log.debug("Getting partField");
							Class adxpClass = Class
									.forName("gov.nih.nci.iso21090.Adxp");
							partField = adxpClass.getDeclaredField(subAttrName);
							log.debug("Getting partField " + partField);
							break;
						}
					}
					log.debug("partField2 type " + field.getType().getName());
					Field partField2 = RESTUtil.getField(partField.getType(),
							subAttrName, classCache);
					log.debug("partField.getType(): " + partField.getType());
					if (partField2 == null)
						partField = partField.getType().getDeclaredField(
								subAttrName);
					else
						partField = partField2;
					log.debug("partField2: " + partField.getName());
					break;
				}
			}

			return convertValues(partField, value);
		} catch (Exception ex) {
			String msg = "Failed to convert ISO data type value "
					+ field.getName() + " \n " + ex.getMessage();
			ex.printStackTrace();
			log.error("ERROR : " + msg);
			ResponseBuilder builder = Response
					.status(Status.INTERNAL_SERVER_ERROR);
			StringBuffer buffer = new StringBuffer();
			buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			buffer.append("<response>");
			buffer.append("<type>ERROR</type>");
			buffer.append("<code>INTERNAL_ERROR_8</code>");
			buffer.append("<resource>convertISOValues</resource>");
			buffer.append("<path>search</path>");
			buffer.append("<message>Failed to construct criteria: " + msg
					+ "</message>");
			buffer.append("</response>");
			builder.entity(buffer.toString());
			throw new WebApplicationException(builder.build());
		}
	}

	/**
	 * Converts the specified value to the field class type
	 * 
	 * @param field
	 *            Specifies the field
	 * @param value
	 *            Specifies the values that needs to be stored
	 * @return returns an object with the new value
	 * @throws Exception
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object convertValues(Field field, Object value)
			throws WebApplicationException {

		String fieldType = field.getType().getName();
		String valueType = value.getClass().getName();

		log.debug("convertValues valueType: " + valueType);
		log.debug("convertValues fieldType: " + fieldType);
		Object convertedValue = null;
		try {
			if (fieldType.equals("java.lang.Long")) {
				if (valueType.equals("java.lang.String")) {
					convertedValue = new Long((String) value);
				}
			} else if (fieldType.equals("java.lang.String")) {
				if (valueType.equals("java.lang.String")) {
					convertedValue = (String) value;
				}
			} else if (fieldType.equals("java.lang.Integer")) {
				if (valueType.equals("java.lang.String")) {
					convertedValue = new Integer((String) value);
				}
			} else if (fieldType.equals("java.lang.Float")) {
				if (valueType.equals("java.lang.String")) {
					convertedValue = new Float((String) value);
				}
			} else if (fieldType.equals("java.lang.Double")) {
				if (valueType.equals("java.lang.String")) {
					convertedValue = new Double((String) value);
				}
			} else if (fieldType.equals("java.lang.Boolean")) {
				if (valueType.equals("java.lang.String")) {
					convertedValue = new Boolean((String) value);
				}
			} else if (fieldType.equals("java.util.Date")) {
				SimpleDateFormat format = new SimpleDateFormat("MM-dd-yyyy");
				if (valueType.equals("java.lang.String")) {
					convertedValue = format.parse((String) value);
				}
			} else if (fieldType.equals("java.net.URI")) {
				if (valueType.equals("java.lang.String")) {
					convertedValue = new URI((String) value);
				}
			} else if (fieldType.equals("java.lang.Character")) {
				if (valueType.equals("java.lang.String")) {
					convertedValue = new Character(((String) value).charAt(0));
				}
			} else if (field.getType().isEnum()) {
				if (valueType.equals("java.lang.String")) {
					Class enumKlass = Class.forName(fieldType);
					convertedValue = Enum.valueOf(enumKlass, (String) value);
				}
			} else if (fieldType.equals("gov.nih.nci.iso21090.Ii")) {
				if (valueType.equals("java.lang.String")) {
					convertedValue = new gov.nih.nci.iso21090.Ii();
					((gov.nih.nci.iso21090.Ii) convertedValue)
							.setExtension((String) value);
				}
			} else {
				throw new Exception("type mismatch - " + valueType);
			}

		} catch (Exception ex) {
			String msg = "Type mismatch " + field.getName() + " is of type - "
					+ fieldType + " \n " + ex.getMessage();
			ex.printStackTrace();
			log.error("ERROR : " + msg);
			ResponseBuilder builder = Response
					.status(Status.INTERNAL_SERVER_ERROR);
			StringBuffer buffer = new StringBuffer();
			buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			buffer.append("<response>");
			buffer.append("<type>ERROR</type>");
			buffer.append("<code>INTERNAL_ERROR_5</code>");
			buffer.append("<resource>convertValues</resource>");
			buffer.append("<path>search</path>");
			buffer.append("<message>Failed to construct criteria: " + msg
					+ "</message>");
			buffer.append("</response>");
			builder.entity(buffer.toString());
			throw new WebApplicationException(builder.build());
		}

		return convertedValue;
	}

	protected ApplicationService getApplicationService() throws Exception {
		// return ApplicationServiceProvider.getApplicationService();
		return applicationService;
	}

	protected ApplicationService getApplicationService(String username,
			String password) throws Exception {
		log.debug("getApplicationService(String username, String password) ------");
		Authentication auth = new UsernamePasswordAuthenticationToken(username,
				password);
		if (applicationService instanceof org.springframework.aop.framework.Advised) {
			log.debug("getApplicationService(String username, String password) --aop----");
			org.springframework.aop.framework.Advised proxy = (org.springframework.aop.framework.Advised) applicationService;
			for (Advisor advisor : proxy.getAdvisors()) {
				Advice advice = advisor.getAdvice();
				if (advice instanceof ApplicationServiceProxy) {
					ApplicationServiceProxy asp = (ApplicationServiceProxy) advice;
					asp.setApplicationService(applicationService);
					asp.setAuthentication(auth);
				}
			}
		}
		return applicationService;
		// return ApplicationServiceProvider.getApplicationService(username,
		// password);
	}

	/*
	 * Save Object by calling writalbe API
	 * 
	 * @param Object
	 */
	public Object save(final Object obj) throws WebApplicationException {
		try {
			//gov.nih.nci.system.web.util.RESTUtil.printObject(obj,
			//		obj.getClass(), true);
			final InsertExampleQuery sdkQuery = new InsertExampleQuery(obj);
			sdkQuery.setCommit(true);
			SDKQueryResult queryResult = ((WritableApplicationService) applicationService)
					.executeQuery(sdkQuery);
			return queryResult.getObjectResult();
		} catch (ApplicationException e) {
			StringBuffer strEx = new StringBuffer();

			Throwable t = e.getCause();
			if(t instanceof SQLException)
			{
				SQLException ex = (SQLException) t.getCause();

				while (ex != null) {
					while (t != null) {
						t = t.getCause();
					}
					log.debug(ex.getLocalizedMessage());
					strEx.append(ex.getLocalizedMessage() + "\n");
	
					ex = ex.getNextException();
				}
			}
			else
			{
				while(t != null)
				{
					log.debug("Localized: "+t.getLocalizedMessage());
					strEx.append(t.getLocalizedMessage() + ":   ");
					t = t.getCause();
				}
			}

			log.error("Error in Saving REST resource: " + e.getMessage());
			ResponseBuilder builder = Response
					.status(Status.INTERNAL_SERVER_ERROR);
			builder.type("application/xml");
			StringBuffer buffer = new StringBuffer();
			buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			buffer.append("<response>");
			buffer.append("<type>ERROR</type>");
			buffer.append("<code>INTERNAL_ERROR_5</code>");
			buffer.append("<resource>"+obj.getClass().getName()+"</resource>");
			buffer.append("<path>save</path>");
			buffer.append("<message>>Failed to Save due to: " + strEx.toString()
					+ "</message>");
			buffer.append("</response>");
			builder.entity(buffer.toString());
			throw new WebApplicationException(builder.build());		
		} catch (Exception e) {
			log.error("Error in Saving REST resource: " + e.getMessage());

			ResponseBuilder builder = Response
					.status(Status.INTERNAL_SERVER_ERROR);
			builder.type("application/xml");
			StringBuffer buffer = new StringBuffer();
			buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			buffer.append("<response>");
			buffer.append("<type>ERROR</type>");
			buffer.append("<code>INTERNAL_ERROR_5</code>");
			buffer.append("<resource>"+obj.getClass().getName()+"</resource>");
			buffer.append("<path>save</path>");
			buffer.append("<message>Failed to Save due to: " + e.getMessage()
					+ "</message>");
			buffer.append("</response>");
			builder.entity(buffer.toString());
			throw new WebApplicationException(builder.build());		
		}
	}

	/*
	 * Update Object by calling writalbe API
	 * 
	 * @param Object
	 */
	public void update(Object obj) throws WebApplicationException {
		try {
			//gov.nih.nci.system.web.util.RESTUtil.printObject(obj,
			//		obj.getClass(), true);
			final UpdateExampleQuery sdkQuery = new UpdateExampleQuery(obj);
			sdkQuery.setCommit(true);
			new BaseUtilWrapper() {

				@Override
				public List execute() throws Exception {
					((WritableApplicationService) applicationService)
							.executeQuery(sdkQuery);
					return null;
				}
			}.executeLogic();
		} catch(RuntimeException e) 
		{
			Throwable t = e.getCause();
			StringBuffer strEx = new StringBuffer();
			while(t != null)
			{
				strEx.append(t.getLocalizedMessage() + ":   ");
				t = t.getCause();
			}
		
			
			ResponseBuilder builder = Response
					.status(Status.INTERNAL_SERVER_ERROR);
			builder.type("application/xml");
			StringBuffer buffer = new StringBuffer();
			buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			buffer.append("<response>");
			buffer.append("<type>ERROR</type>");
			buffer.append("<code>INTERNAL_ERROR_5</code>");
			buffer.append("<resource>UPDATE</resource>");
			buffer.append("<path>UPDATE</path>");
			buffer.append("<message>Failed to update due to: " +strEx.toString()
					+ "</message>");
			buffer.append("</response>");
			builder.entity(buffer.toString());
			throw new WebApplicationException(builder.build());		
		} catch (Exception e) {
			ResponseBuilder builder = Response
					.status(Status.INTERNAL_SERVER_ERROR);
			builder.type("application/xml");
			StringBuffer buffer = new StringBuffer();
			buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			buffer.append("<response>");
			buffer.append("<type>ERROR</type>");
			buffer.append("<code>INTERNAL_ERROR_5</code>");
			buffer.append("<resource>UPDATE</resource>");
			buffer.append("<path>UPDATE</path>");
			buffer.append("<message>Failed to update due to: " + e.getMessage()
					+ "</message>");
			buffer.append("</response>");
			builder.entity(buffer.toString());
			throw new WebApplicationException(builder.build());		
		}
	}

	/*
	 * Delete Object by calling writalbe API
	 * 
	 * @param Object
	 */
	public void delete(Object obj) throws WebApplicationException {
		try {
			final DeleteExampleQuery sdkQuery = new DeleteExampleQuery(obj);
			sdkQuery.setCommit(true);
			new BaseUtilWrapper() {

				@Override
				public List execute() throws Exception {
					((WritableApplicationService) applicationService)
							.executeQuery(sdkQuery);
					return null;
				}
			}.executeLogic();
		} 
		catch(RuntimeException e) 
		{
			Throwable t = e.getCause();
			StringBuffer strEx = new StringBuffer();
			while(t != null)
			{
				strEx.append(t.getLocalizedMessage() + ":   ");
				t = t.getCause();
			}
		
			
			ResponseBuilder builder = Response
					.status(Status.INTERNAL_SERVER_ERROR);
			builder.type("application/xml");
			StringBuffer buffer = new StringBuffer();
			buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			buffer.append("<response>");
			buffer.append("<type>ERROR</type>");
			buffer.append("<code>INTERNAL_ERROR_5</code>");
			buffer.append("<resource>DELETE</resource>");
			buffer.append("<path>DELETE</path>");
			buffer.append("<message>Failed to delete due to: " +strEx.toString()
					+ "</message>");
			buffer.append("</response>");
			builder.entity(buffer.toString());
			throw new WebApplicationException(builder.build());		
		}	
		catch (Exception e) {
			e.printStackTrace();
			log.error("Error in Updating REST resource: " + e.getMessage());
			ResponseBuilder builder = Response
					.status(Status.INTERNAL_SERVER_ERROR);
			builder.type("application/xml");
			StringBuffer buffer = new StringBuffer();
			buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			buffer.append("<response>");
			buffer.append("<type>ERROR</type>");
			buffer.append("<code>INTERNAL_ERROR_5</code>");
			buffer.append("<resource>DELETE</resource>");
			buffer.append("<path>DELETE</path>");
			buffer.append("<message>Failed to delete due to: " + e.getMessage()
					+ "</message>");
			buffer.append("</response>");
			builder.entity(buffer.toString());
			throw new WebApplicationException(builder.build());				
		}
	}

	/*
	 * Delete Object by calling writalbe API
	 * 
	 * @param Object
	 */
	public void delete(DeleteHQLQuery query) throws WebApplicationException {
		try {
			((WritableApplicationService) applicationService)
					.executeQuery(query);
		} catch (ApplicationException e) {
			e.printStackTrace();
			log.error("Error in Updating REST resource: " + e.getMessage());
			ResponseBuilder builder = Response
					.status(Status.INTERNAL_SERVER_ERROR);
			builder.type("application/xml");
			StringBuffer buffer = new StringBuffer();
			buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			buffer.append("<response>");
			buffer.append("<type>ERROR</type>");
			buffer.append("<code>INTERNAL_ERROR_5</code>");
			buffer.append("<resource>DELETE</resource>");
			buffer.append("<path>DELETE</path>");
			buffer.append("<message>Failed to delete due to: " + e.getMessage()
					+ "</message>");
			buffer.append("</response>");
			builder.entity(buffer.toString());
			throw new WebApplicationException(builder.build());		
		}
	}

	@SuppressWarnings("unchecked")
	private abstract class BaseUtilWrapper {
		public abstract List execute() throws Exception;

		public void executeLogic() {
			try {
				execute();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	protected void checkCollections(Object object) {
		if (object == null)
			return;
		try {
			Class klass = object.getClass();
			Method[] methods = klass.getMethods();
			for (int i = 0; i < methods.length; i++) {
				Method method = methods[i];
				Class returnType = method.getReturnType();
				if (!returnType.getName().equals("java.util.Collection"))
					continue;

				String methodName = method.getName();
				log.debug("methodName " + methodName);
				Object collectionValue = method.invoke(object, null);
				if (!(collectionValue instanceof java.util.List))
					continue;
				String setMethodName = "s"
						+ methodName.substring(1, methodName.length());
				log.debug("setMethod " + setMethodName);
				Class[] argTypes = new Class[] { returnType };
				Method setMethod = klass.getMethod(setMethodName, argTypes);
				setMethod.invoke(object,
						RESTUtil.convertListToSet((List) collectionValue));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
