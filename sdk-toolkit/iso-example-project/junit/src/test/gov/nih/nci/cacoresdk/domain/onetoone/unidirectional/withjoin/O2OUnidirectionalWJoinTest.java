/*L
 *  Copyright Ekagra Software Technologies Ltd.
 *  Copyright SAIC, SAIC-Frederick
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/cacore-sdk/LICENSE.txt for details.
 */

package test.gov.nih.nci.cacoresdk.domain.onetoone.unidirectional.withjoin;

import gov.nih.nci.cacoresdk.domain.onetoone.unidirectional.withjoin.Handle;
import gov.nih.nci.cacoresdk.domain.onetoone.unidirectional.withjoin.Bag;
import gov.nih.nci.iso21090.Ii;
import gov.nih.nci.system.applicationservice.ApplicationException;
import gov.nih.nci.system.query.hibernate.HQLCriteria;

import java.util.Collection;
import java.util.Iterator;

import test.gov.nih.nci.cacoresdk.SDKISOTestBase;

public class O2OUnidirectionalWJoinTest extends SDKISOTestBase
{
	public static String getTestCaseName()
	{
		return "One to One Unidirectional With Join Test Case";
	}
	
	/**
	 * Uses Nested Search Criteria for search
	 * Verifies that the results are returned 
	 * Verifies size of the result set
	 * Verifies that none of the attribute is null
	 * 
	 * @throws ApplicationException
	 */
	public void testEntireObjectNestedSearch1() throws ApplicationException
	{
		Bag searchObject = new Bag();
		Collection results = getApplicationService().search("gov.nih.nci.cacoresdk.domain.onetoone.unidirectional.withjoin.Bag",searchObject );

		assertNotNull(results);
		assertEquals(11,results.size());
		
		for(Iterator i = results.iterator();i.hasNext();)
		{
			Bag result = (Bag)i.next();
			assertNotNull(result);
			assertNotNull(result.getId());
			assertEquals(result.getId().getRoot(),II_ROOT_GLOBAL_CONSTANT_VALUE);
			assertNotNull(result.getStyle());
		}
	}

	/**
	 * Uses Nested Search Criteria for search
	 * Verifies that the results are returned 
	 * Verifies size of the result set
	 * Verifies that none of the attribute is null
	 * 
	 * @throws ApplicationException
	 */
	public void testEntireObjectNestedSearch2() throws ApplicationException
	{
		Handle searchObject = new Handle();
		Collection results = getApplicationService().search("gov.nih.nci.cacoresdk.domain.onetoone.unidirectional.withjoin.Handle",searchObject );

		assertNotNull(results);
		assertEquals(12,results.size());
		
		for(Iterator i = results.iterator();i.hasNext();)
		{
			Handle result = (Handle)i.next();
			assertNotNull(result);
			assertNotNull(result.getId());
			assertEquals(result.getId().getRoot(),II_ROOT_GLOBAL_CONSTANT_VALUE);
			assertNotNull(result.getColor());
		}
	}

	/**
	 * Uses Nested Search Criteria for search
	 * Verifies that the results are returned 
	 * Verifies size of the result set
	 * erifies that the associated object is null
	 * 
	 * @throws ApplicationException
	 */
	public void testZeroAssociatedObjectsNestedSearch1() throws ApplicationException
	{
		Bag searchObject = new Bag();
		Ii ii=new Ii();
		ii.setExtension("11");
		searchObject.setId(ii);
		Collection results = getApplicationService().search("gov.nih.nci.cacoresdk.domain.onetoone.unidirectional.withjoin.Bag",searchObject );

		assertNotNull(results);
		assertEquals(1,results.size());
		
		Iterator i = results.iterator();
		Bag result = (Bag)i.next();
		assertNotNull(result);
		assertNotNull(result.getId());
		assertEquals(result.getId().getRoot(),II_ROOT_GLOBAL_CONSTANT_VALUE);
		assertNotNull(result.getStyle());
		
		Handle handle = result.getHandle();
		assertNull(handle);
	}

	/**
	 * Uses Nested Search Criteria for search to get associated object
	 * Verifies that the results are returned 
	 * Verifies size of the result set is 0
	 * 
	 * @throws ApplicationException
	 */
	public void testZeroAssociatedObjectsNestedSearch2() throws ApplicationException
	{
		Bag searchObject = new Bag();
		Ii ii=new Ii();
		ii.setExtension("11");
		searchObject.setId(ii);
		Collection results = getApplicationService().search("gov.nih.nci.cacoresdk.domain.onetoone.unidirectional.withjoin.Handle",searchObject );

		assertNotNull(results);
		assertEquals(0,results.size());
	}	
	
	/**
	 * Uses Nested Search Criteria for search
	 * Verifies that the results are returned 
	 * Verifies size of the result set
	 * Verifies that none of the attribute is null
	 * Verifies that the associated object has required Id
	 * 
	 * @throws ApplicationException
	 */
	public void testOneAssociatedObjectNestedSearch1() throws ApplicationException
	{
		Bag searchObject = new Bag();
		Ii ii=new Ii();
		ii.setExtension("1");
		searchObject.setId(ii);
		Collection results = getApplicationService().search("gov.nih.nci.cacoresdk.domain.onetoone.unidirectional.withjoin.Bag",searchObject );

		assertNotNull(results);
		assertEquals(1,results.size());
		
		Iterator i = results.iterator();
		Bag result = (Bag)i.next();
		assertNotNull(result);
		assertNotNull(result.getId());
		assertEquals(result.getId().getRoot(),II_ROOT_GLOBAL_CONSTANT_VALUE);
		assertNotNull(result.getStyle());
		
		Handle handle = result.getHandle();
		assertNotNull(handle);
		
		assertNotNull(handle);
		assertNotNull(handle.getId());
		assertEquals(result.getId().getRoot(),II_ROOT_GLOBAL_CONSTANT_VALUE);
		assertNotNull(handle.getColor());
		assertEquals("1",handle.getId().getExtension());
	}

	/**
	 * Uses Nested Search Criteria for search to get associated object
	 * Verifies that the results are returned 
	 * Verifies size of the result set
	 * Verifies that none of the attribute is null
	 * Verified the Id attribute's value of the returned object 
	 * 
	 * @throws ApplicationException
	 */
	public void testOneAssociatedObjectNestedSearch2() throws ApplicationException
	{
		Bag searchObject = new Bag();
		Ii ii=new Ii();
		ii.setExtension("1");
		searchObject.setId(ii);
		Collection results = getApplicationService().search("gov.nih.nci.cacoresdk.domain.onetoone.unidirectional.withjoin.Handle",searchObject );

		assertNotNull(results);
		assertEquals(1,results.size());
		
		Iterator i = results.iterator();
		
		Handle handle = (Handle)i.next();
		assertNotNull(handle);
		
		assertNotNull(handle);
		assertNotNull(handle.getId());
		assertEquals(handle.getId().getRoot(),II_ROOT_GLOBAL_CONSTANT_VALUE);
		assertNotNull(handle.getColor());
		assertEquals("1",handle.getId().getExtension());
	}
	
	public void testOneAssociatedObjectHQL() throws ApplicationException {
		HQLCriteria hqlCriteria = new HQLCriteria(
				"select bag.handle from gov.nih.nci.cacoresdk.domain.onetoone.unidirectional.withjoin.Bag bag " +
				" where bag.id='1'");

		Collection results = search(hqlCriteria,
				"gov.nih.nci.cacoresdk.domain.onetoone.unidirectional.withjoin.Handle");
		assertNotNull(results);
		assertEquals(1, results.size());

		Iterator i = results.iterator();

		Handle handle = (Handle) i.next();
		assertNotNull(handle);

		assertNotNull(handle);
		assertNotNull(handle.getId());
		assertEquals(handle.getId().getRoot(),II_ROOT_GLOBAL_CONSTANT_VALUE);
		assertNotNull(handle.getColor());
		assertEquals("1", handle.getId().getExtension());
	}
	
	public void testGetAssociation() throws ApplicationException
	{

		Bag searchObject = new Bag();
		Collection results = getApplicationService().search("gov.nih.nci.cacoresdk.domain.onetoone.unidirectional.withjoin.Bag",searchObject );

		assertNotNull(results);
		assertEquals(11,results.size());
		
		Handle handle;
		for(Iterator i = results.iterator();i.hasNext();)
		{
			Bag result = (Bag)i.next();
			assertNotNull(result);
			assertNotNull(result.getId());
			assertEquals(result.getId().getRoot(),II_ROOT_GLOBAL_CONSTANT_VALUE);
			assertNotNull(result.getStyle());
			
			if (new Integer(result.getId().getExtension()) < 11){
				handle = result.getHandle();
				assertNotNull(handle);
				assertNotNull(handle.getId());
				assertEquals(handle.getId().getRoot(),II_ROOT_GLOBAL_CONSTANT_VALUE);
				assertNotNull(handle.getColor());
			}
		}
	}	
}
