/*L
 *  Copyright Ekagra Software Technologies Ltd.
 *  Copyright SAIC, SAIC-Frederick
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/cacore-sdk/LICENSE.txt for details.
 */

package gov.nih.nci.system.web.util;

import gov.nih.nci.codegen.util.TransformerUtils;
import gov.nih.nci.system.client.util.xml.JAXBMarshaller;
import gov.nih.nci.system.client.util.xml.JAXBUnmarshaller;
import gov.nih.nci.system.client.util.xml.Marshaller;
import gov.nih.nci.system.client.util.xml.Unmarshaller;
import gov.nih.nci.system.client.util.xml.XMLUtility;
import gov.nih.nci.system.client.util.xml.XMLUtilityException;
import gov.nih.nci.system.metadata.MetadataCache;
import gov.nih.nci.system.metadata.MetadataInjector;
import gov.nih.nci.system.metadata.caDSRMetadata;
import gov.nih.nci.system.web.ResourceLink;
import gov.nih.nci.system.web.CollectionBean;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.StringReader;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.log4j.Logger;
import org.dom4j.io.DocumentResult;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

@Provider
@Produces("application/xml")
@Consumes("application/xml")
public class SDKRESTContentHandler implements MessageBodyReader,
		MessageBodyWriter {

	private static Logger log = Logger.getLogger(SDKRESTContentHandler.class);

	public boolean isReadable(Class type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return Serializable.class.isAssignableFrom(type);
	}

	public boolean isWriteable(Class type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return Serializable.class.isAssignableFrom(type);
	}

	public Object readFrom(Class type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap httpHeaders, InputStream is) throws IOException,
			WebApplicationException {
		InputStreamReader reader = new InputStreamReader(is);
		try {
			String packageName = type.getName().substring(0, type.getName().lastIndexOf("."));

			Unmarshaller unmarshaller = new JAXBUnmarshaller(true,
					packageName);
			return unmarshaller.fromXML(reader);
		} catch (XMLUtilityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public long getSize(Object o, Class type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	public void writeTo(Object target, Class type, Type genericType,
			Annotation[] annotations, MediaType mediaType,
			MultivaluedMap httpHeaders, OutputStream os) throws IOException,
			WebApplicationException {
		OutputStreamWriter writer = null;
		MetadataCache mCache = null;
		Reader in = null;
		StringWriter strWriter = null;
		try {
			if(target == null)
				return;

			writer = new OutputStreamWriter(os);
			if(target instanceof java.lang.String)
			{
				writer.write(target.toString());
				writer.flush();
				return;
			}
			boolean includeAssociations = true;
			try
			{
				mCache = MetadataCache.getInstance();
			}
			catch(Exception e)
			{
				log.error("ERROR: Failed to load Metadata: ", e);
			}
			
			if(!(target instanceof CollectionBean))
			{
				Object convertedObj = XMLUtility.convertFromProxy(target,
						false);

				String namespace = "gme://caCORE.caCORE/4.5/";
				try
				{
					Method method = type.getDeclaredMethod("getNamespacePrefix", (Class[])null);
					namespace = (String)method.invoke(target, null);
				} catch (NoSuchMethodException e) {
					log.error("ERROR: ", e);
				}

				List<ResourceLink> links = null;
				try
				{
					Method method = type.getDeclaredMethod("getLinks", (Class[])null);
					links = (List)method.invoke(target, null);
				} catch (NoSuchMethodException e) {
					log.error("ERROR: ", e);
				}

				String packageName = convertedObj.getClass().getPackage().getName();
				strWriter = new StringWriter();
				Marshaller marshaller = new JAXBMarshaller(true,
						packageName, namespace);
				marshaller.toXML(convertedObj, strWriter);
				in = new StringReader(strWriter.toString());
				SAXBuilder builder = new SAXBuilder();
				Document doc = builder.build(in);
				if(mCache != null)
					MetadataInjector.injectMetadata(mCache, caDSRMetadata.CONTEXT_NAME, doc, convertedObj.getClass().getName());
				Element rootEle = doc.getRootElement();
				if(links != null)
				{
					for(ResourceLink link: links)
					{
						Element linkElement = new Element("link", rootEle.getNamespace());
						linkElement.setAttribute("ref", link.getRelationship());
						linkElement.setAttribute("type", link.getType());
						linkElement.setAttribute("href", link.getHref());
						rootEle.addContent(linkElement);
					}
				}
				XMLOutputter outputter = new XMLOutputter();
				outputter.output(doc, writer);
			}
			else
			{
				handleCollection((CollectionBean)target, writer, type, mCache);
			}

		} catch (XMLUtilityException e) {
			e.printStackTrace();
			log.error("ERROR: ", e);
			throw new WebApplicationException(e);
		} catch (SecurityException e) {
			log.error("ERROR: ", e);
			e.printStackTrace();
			throw new WebApplicationException(e);
		} catch (IllegalArgumentException e) {
			log.error("ERROR: ", e);
			e.printStackTrace();
			throw new WebApplicationException(e);
		} catch (IllegalAccessException e) {
			log.error("ERROR: ", e);
			e.printStackTrace();
			throw new WebApplicationException(e);
		} catch (InvocationTargetException e) {
			log.error("ERROR: ", e);
			e.printStackTrace();
			throw new WebApplicationException(e);
		} catch (Exception e) {
			e.printStackTrace();
			throw new WebApplicationException(e);
		}
		finally
		{
			if(writer != null)
			{
				writer.close();
				writer = null;
			}
			if(strWriter != null)
			{
				strWriter.close();
				strWriter = null;
			}
			if(in != null)
			{
				in.close();
				in = null;
			}
		}
	}

	private void handleCollection(CollectionBean collectionObj, OutputStreamWriter writer, Class type, MetadataCache mCache) throws XMLUtilityException, IOException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, JDOMException
	{

		String collectionType = collectionObj.getType();
		gov.nih.nci.system.client.proxy.ListProxy proxy = null;
		String getMethod = "get"+collectionType.substring(collectionType.lastIndexOf(".")+1, collectionType.length())+"s";

		try
		{
			Method method = collectionObj.getClass().getDeclaredMethod(getMethod, (Class<?>[])null);
			proxy = (gov.nih.nci.system.client.proxy.ListProxy)method.invoke(collectionObj, null);
		} catch (NoSuchMethodException e) {
			log.error("ERROR: ", e);
		}

		boolean includeAssociations = true;
		List results = new ArrayList();
		String targetClassName = proxy.getTargetClassName();
		int counter = proxy.size();
		String packageName = "";
		boolean isFirst = true;
		StringBuffer outputStr = new StringBuffer();

		Object obj = proxy.get(0);
		
		if(obj == null)
			return;
		
		String collectionFullName = obj.getClass().getName();
		String namespace = "gme://caCORE.caCORE/4.5/";
		try
		{
			Class klass = obj.getClass();
			Method method = klass.getDeclaredMethod("getNamespacePrefix", (Class[])null);
			namespace = (String)method.invoke(obj, null);
		} catch (NoSuchMethodException e) {
			log.error("ERROR: ", e);
		}

		String collectionName = collectionFullName.substring(collectionFullName.lastIndexOf(".")+1, collectionFullName.lastIndexOf("Bean"))+"s";
		String className = type.getName().substring(0, type.getName().length()-1);
		org.jdom.Element httpQuery = new org.jdom.Element(collectionName,namespace);
		Collection<ResourceLink> collectionLinks = collectionObj.getLinks();
		if(collectionLinks != null)
		{
			for(ResourceLink link : collectionLinks)
			{
				Element linkElement = new Element("link", namespace);
				linkElement.setAttribute("ref", link.getRelationship());
				linkElement.setAttribute("type", link.getType());
				linkElement.setAttribute("href", link.getHref());
				httpQuery.addContent(linkElement);
			}
		}

		for (int i = 0; i < counter; i++) {
			obj = proxy.get(i);
			if(obj instanceof ResourceLink)
			{
				ResourceLink link = (ResourceLink)obj;
				Element linkElement = new Element("link", namespace);
				linkElement.setAttribute("ref", link.getRelationship());
				linkElement.setAttribute("type", link.getType());
				linkElement.setAttribute("href", link.getHref());
				httpQuery.addContent(linkElement);
				continue;
			}

			Marshaller marshaller = new JAXBMarshaller(false,
				packageName, namespace);

			List<ResourceLink> links = null;
			try
			{
				Method method = obj.getClass().getDeclaredMethod("getLinks", (Class[])null);
				links = (List)method.invoke(obj, null);
			} catch (NoSuchMethodException e) {
				log.error("ERROR: ", e);
			}

			Object convertedObj = XMLUtility.convertFromProxy(obj,
					false);
			packageName = convertedObj.getClass().getPackage().getName();
			if(isFirst)
			{
				try
				{
					Method method = convertedObj.getClass().getDeclaredMethod("getNamespacePrefix", (Class<?>[])null);
					namespace = (String)method.invoke(convertedObj, null);
				} catch (NoSuchMethodException e) {
					log.error("ERROR: ", e);
				}
				isFirst = false;
			}
			StringWriter strWriter = null;
			Reader in = null;
			try
			{
				strWriter = new StringWriter();
				DocumentResult dr = new DocumentResult();
				marshaller.toXML(convertedObj, strWriter);
				in = new StringReader(strWriter.toString());
				SAXBuilder builder = new SAXBuilder();
				Document doc = builder.build(in);
				if(mCache.hasMetadata(caDSRMetadata.CONTEXT_NAME, className))
					doc = MetadataInjector.injectMetadata(mCache, caDSRMetadata.CONTEXT_NAME, doc, className);
				Element rootEle = (Element)doc.getRootElement().clone();
				if(links != null)
				{
					for(ResourceLink link: links)
					{
						Element linkElement = new Element("link", rootEle.getNamespace());
						linkElement.setAttribute("ref", link.getRelationship());
						linkElement.setAttribute("type", link.getType());
						linkElement.setAttribute("href", link.getHref());
						rootEle.addContent(linkElement);
					}
				}
				httpQuery.addContent(rootEle);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				if(strWriter != null)
				{
					strWriter.close();
					strWriter = null;
				}
				if(in != null)
				{
					in.close();
					in = null;
				}
			}
		}
		org.jdom.Document xmlDoc = new org.jdom.Document(httpQuery);
		XMLOutputter outputter = new XMLOutputter();
		outputter.output(xmlDoc, writer);
	}

}