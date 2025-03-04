/*L
 *  Copyright Ekagra Software Technologies Ltd.
 *  Copyright SAIC, SAIC-Frederick
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/cacore-sdk/LICENSE.txt for details.
 */

package gov.nih.nci.system.client.util.xml;

import gov.nih.nci.system.client.proxy.ListProxy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.exolab.castor.mapping.GeneralizedFieldHandler;
import org.hibernate.Hibernate;
import org.springframework.aop.framework.Advised;

/**
 * The FieldHandler for the Date class
 *
 */
public abstract class BaseCastorFieldHandler
extends GeneralizedFieldHandler
{

	private static Logger log = Logger.getLogger(BaseCastorFieldHandler.class);

	static public Object convertObject(Object oldObj, boolean getAssociation) throws Exception
	{
		String proxyClassName = oldObj.getClass().getName();
		String domainClassName;
		if (proxyClassName.indexOf('$') > 0) {
			domainClassName = proxyClassName.substring(0, proxyClassName.indexOf('$'));
		} else {
			domainClassName = proxyClassName;
		}

		Object convertedObj = null;
			convertedObj =  convertObject(oldObj, Class.forName(domainClassName), getAssociation);

		return convertedObj;

	}

	private static Object convertObject(Object obj, Class klass, boolean getAssociations) throws Exception {

		Object convertedObject = klass.newInstance();

		Method[] methods = klass.getMethods();
		for(Method method:methods)
		{
			if(method.getName().startsWith("get") && !method.getName().equals("getClass"))
			{
				try {
					Method setterMethod = convertedObject.getClass().getMethod("set" + method.getName().substring(3), method.getReturnType());
		    		Object value = null;
		    		Class type = setterMethod.getParameterTypes()[0];
					if (getAssociations
							|| ((!type.getName().equals("java.util.Collection")) && (type
									.getName().startsWith("java")))
							|| type.isPrimitive() || type.getName().startsWith("gov.nih.nci.iso21090."))
						value = method.invoke(obj, (Object[])null);

					if (!Hibernate.isInitialized(value)){
						value = null;
					} else {
						if (value != null){

							if (value instanceof ListProxy) {
								value = convertListProxy(value);
							} else {
								String className = value.getClass().getName();
								if (className.indexOf('$') > 0) {
									boolean includeAssociations = false;
									value = convertObject(value,includeAssociations);
								}
							}
						}
					}

					Object[] parameters = new Object[1];
					parameters[0] = value;

					setterMethod.invoke(convertedObject, (Object[])parameters);
				} catch (NoSuchMethodException e){
					//ignore - E.g., Strings have getChars(), getBytes() methods with no corresponding Setters
				} catch (Exception e){
					log.error("Exception caught trying to convert proxy object to domain object for method " + method.getName(), e);
				}
			}
		}

		return convertedObject;
	}

	private static Object convertListProxy(Object value) {
//		System.out.println("*** convertUponGet(Object value) called ***");
//		System.out.println("Value: " + value);
//		System.out.println("Value.class: " + value.getClass().getName());

		if (value == null) return null;

		String setMethodName, getMethodName;

		Class klass;
		Method[] methods;
		Method tempMethod;
		Object tempObject = null;

		java.util.Collection<Object> tempCollection = new ArrayList<Object>();
		HashSet<Object> tempList = new HashSet<Object>();
		Object[] args = {tempList};
		Class[] parameterTypes = {Collection.class};
//		System.out.println("args array initialized: " + args[0].getClass().getName());

//		Enumeration collIterator = (Enumeration)value;
		List list = (ArrayList)value;

		// convert the collection objects from proxy to domain objects
//		while (collIterator.hasMoreElements()){
		for(Object obj : list){
			try {
				tempCollection.add(convertObject(obj, false));
			} catch (Exception e) {
				log.error("Exception caught trying to convert proxy object to domain object: " + e.getMessage());
				e.printStackTrace();
			}
		}

		Iterator iter = tempCollection.iterator();
		while (iter.hasNext()){
			tempObject = iter.next();
			klass = tempObject.getClass();
			methods = klass.getMethods();

//			System.out.println("Number of methods: " + methods.length);

			for (int i=0; i < methods.length; i++){

				tempMethod = methods[i];

//				System.out.println("tempMethod[" + i + "].getName(): " + tempMethod.getName());
//				System.out.println("tempMethod[" + i + "].getReturnType().getName(): " + tempMethod.getReturnType().getName());

				// 'Erase' any collection attributes in order to prevent recursion
				if ("java.util.Collection".equalsIgnoreCase(tempMethod.getReturnType().getName())){
					try {

						getMethodName = tempMethod.getName();
//						System.out.println("getMethodName: " + getMethodName);
						setMethodName = 's' + getMethodName.substring(1);
//						System.out.println("setMethodName: " + setMethodName);

						tempMethod = klass.getMethod(setMethodName, parameterTypes);
						tempMethod.invoke(tempObject, args);
//						System.out.println("Successful: Collection Attribute set to empty ArrayList for method " + tempMethod.getName());

					} catch (Exception e) {
						log.error("Exception: " + e.getMessage());
						log.error("Unsuccessful:  Collection Attribute NOT set to empty ArrayList for method " + tempMethod.getName());
					}
				}
			}
		}

//		System.out.println("*** final tempCollection.size(): " + tempCollection.size());
		if (tempCollection.size() == 0){return null;}
		return tempCollection;
	}
}
