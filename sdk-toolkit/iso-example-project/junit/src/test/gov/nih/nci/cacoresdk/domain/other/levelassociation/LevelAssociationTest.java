/*L
 *  Copyright Ekagra Software Technologies Ltd.
 *  Copyright SAIC, SAIC-Frederick
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/cacore-sdk/LICENSE.txt for details.
 */

package test.gov.nih.nci.cacoresdk.domain.other.levelassociation;

import java.util.Collection;
import java.util.Iterator;

import gov.nih.nci.cacoresdk.domain.other.levelassociation.Card;
import gov.nih.nci.cacoresdk.domain.other.levelassociation.Deck;
import gov.nih.nci.cacoresdk.domain.other.levelassociation.Hand;
import gov.nih.nci.cacoresdk.domain.other.levelassociation.Suit;
import gov.nih.nci.iso21090.Ii;
import gov.nih.nci.system.applicationservice.ApplicationException;
import gov.nih.nci.system.query.hibernate.HQLCriteria;
import test.gov.nih.nci.cacoresdk.SDKISOTestBase;

public class LevelAssociationTest extends SDKISOTestBase
{
	public static String getTestCaseName()
	{
		return "Level association Test Case";
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
		Hand searchObject = new Hand();
		Collection results = getApplicationService().search("gov.nih.nci.cacoresdk.domain.other.levelassociation.Hand",searchObject );

		assertNotNull(results);
		assertEquals(5,results.size());
		
		for(Iterator i = results.iterator();i.hasNext();)
		{
			Hand result = (Hand)i.next();
			assertNotNull(result);
			assertNotNull(result.getId());
			assertEquals(result.getId().getRoot(),II_ROOT_GLOBAL_CONSTANT_VALUE);
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
		Card searchObject = new Card();
		Collection results = getApplicationService().search("gov.nih.nci.cacoresdk.domain.other.levelassociation.Card",searchObject );

		assertNotNull(results);
		assertEquals(53,results.size());
		
		for(Iterator i = results.iterator();i.hasNext();)
		{
			Card result = (Card)i.next();
			assertNotNull(result);
			assertNotNull(result.getId());
			assertEquals(result.getId().getRoot(),II_ROOT_GLOBAL_CONSTANT_VALUE);
			assertNotNull(result.getName());			
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
	public void testEntireObjectNestedSearch3() throws ApplicationException
	{
		Suit searchObject = new Suit();
		Collection results = getApplicationService().search("gov.nih.nci.cacoresdk.domain.other.levelassociation.Suit",searchObject );

		assertNotNull(results);
		assertEquals(4,results.size());
		
		for(Iterator i = results.iterator();i.hasNext();)
		{
			Suit result = (Suit)i.next();
			assertNotNull(result);
			assertNotNull(result.getId());
			assertEquals(result.getId().getRoot(),II_ROOT_GLOBAL_CONSTANT_VALUE);
			assertNotNull(result.getName());			
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
	public void testEntireObjectNestedSearch4() throws ApplicationException
	{
		Deck searchObject = new Deck();
		Collection results = getApplicationService().search("gov.nih.nci.cacoresdk.domain.other.levelassociation.Deck",searchObject );

		assertNotNull(results);
		assertEquals(1,results.size());
		
		for(Iterator i = results.iterator();i.hasNext();)
		{
			Deck result = (Deck)i.next();
			assertNotNull(result);
			assertNotNull(result.getId());
			assertEquals(result.getId().getRoot(),II_ROOT_GLOBAL_CONSTANT_VALUE);
			assertNotNull(result.getName());
		}
	}


	/**
	 * Verifies all the associations in the object graph
	 * 
	 * @throws ApplicationException
	 */
	public void testNullAssociationsInBeans() throws ApplicationException
	{
		Card searchObject = new Card();
		Ii ii=new Ii();
		ii.setExtension("53");
		searchObject.setId(ii);
		Collection results = getApplicationService().search("gov.nih.nci.cacoresdk.domain.other.levelassociation.Card",searchObject );

		assertNotNull(results);
		assertEquals(1,results.size());
		
		Iterator i = results.iterator();
		Card card = (Card)i.next();
		
		assertNotNull(card);
		assertNotNull(card.getId());
		assertEquals(card.getId().getRoot(),II_ROOT_GLOBAL_CONSTANT_VALUE);
		
		Suit suit = card.getSuit();
		
		assertNull(suit);
	}

	/**
	 * Verifies all the associations in the object graph
	 * 
	 * @throws ApplicationException
	 */
	public void testAssociationsInBeans() throws ApplicationException
	{
		Hand searchObject = new Hand();
		Ii ii=new Ii();
		ii.setExtension("1");
		searchObject.setId(ii);
		Collection results = getApplicationService().search("gov.nih.nci.cacoresdk.domain.other.levelassociation.Card",searchObject );

		assertNotNull(results);
		assertEquals(3,results.size());
		
		Iterator i = results.iterator();
		Card card = (Card)i.next();
		
		assertNotNull(card);
		assertNotNull(card.getId());
		assertEquals(card.getId().getRoot(),II_ROOT_GLOBAL_CONSTANT_VALUE);
		
		Suit suit = card.getSuit();
		
		assertNotNull(suit);
		assertEquals(suit.getCardCollection().size(),13);
		
		Deck deck = suit.getDeck();
		
		assertNotNull(deck);
		assertEquals(deck.getSuitCollection().size(),4);
	}
	
	/**
	 * Uses Nested Search Criteria for search
	 * Verifies that the results are returned 
	 * Verifies size of the result set
	 * 
	 * @throws ApplicationException
	 */
	public void testOneLevelAssociationNestedSearch1() throws ApplicationException
	{
		Hand searchObject = new Hand();
		Ii ii=new Ii();
		ii.setExtension("1");
		searchObject.setId(ii);
		Collection results = getApplicationService().search("gov.nih.nci.cacoresdk.domain.other.levelassociation.Card",searchObject );

		assertNotNull(results);
		assertEquals(3,results.size());
		
		for(Iterator i = results.iterator();i.hasNext();)
		{
			Card result = (Card)i.next();
			assertNotNull(result);
			assertNotNull(result.getId());
			assertEquals(result.getId().getRoot(),II_ROOT_GLOBAL_CONSTANT_VALUE);
		}
	}

	/**
	 * Uses Nested Search Criteria for search
	 * Verifies that the results are returned 
	 * Verifies size of the result set
	 * 
	 * @throws ApplicationException
	 */
	public void testOneLevelAssociationNestedSearch2() throws ApplicationException
	{
		Hand searchObject = new Hand();
		Ii ii=new Ii();
		ii.setExtension("1");
		searchObject.setId(ii);
		Collection results = getApplicationService().search("gov.nih.nci.cacoresdk.domain.other.levelassociation.Card,gov.nih.nci.cacoresdk.domain.other.levelassociation.Hand",searchObject );

		assertNotNull(results);
		assertEquals(3,results.size());
		
		for(Iterator i = results.iterator();i.hasNext();)
		{
			Card result = (Card)i.next();
			assertNotNull(result);
			assertNotNull(result.getId());
			assertEquals(result.getId().getRoot(),II_ROOT_GLOBAL_CONSTANT_VALUE);
		}
	}

	/**
	 * Uses Nested Search Criteria for search
	 * Verifies that the results are returned 
	 * Verifies size of the result set
	 * 
	 * @throws ApplicationException
	 */
	public void testTwoLevelAssociationNestedSearch() throws ApplicationException
	{
		Hand searchObject = new Hand();
		Ii ii=new Ii();
		ii.setExtension("1");
		searchObject.setId(ii);
		Collection results = getApplicationService().search("gov.nih.nci.cacoresdk.domain.other.levelassociation.Suit,gov.nih.nci.cacoresdk.domain.other.levelassociation.Card",searchObject );

		assertNotNull(results);
		assertEquals(3,results.size());
		
		for(Iterator i = results.iterator();i.hasNext();)
		{
			Suit result = (Suit)i.next();
			assertNotNull(result);
			assertNotNull(result.getId());
			assertEquals(result.getId().getRoot(),II_ROOT_GLOBAL_CONSTANT_VALUE);
		}
	}

	/**
	 * Uses Nested Search Criteria for search
	 * Verifies that the results are returned 
	 * Verifies size of the result set
	 * 
	 * @throws ApplicationException
	 */
	public void testThreeLevelAssociationNestedSearch() throws ApplicationException
	{
		Hand searchObject = new Hand();
		Ii ii=new Ii();
		ii.setExtension("1");
		searchObject.setId(ii);
		Collection results = getApplicationService().search("gov.nih.nci.cacoresdk.domain.other.levelassociation.Deck,gov.nih.nci.cacoresdk.domain.other.levelassociation.Suit,gov.nih.nci.cacoresdk.domain.other.levelassociation.Card",searchObject );

		assertNotNull(results);
		assertEquals(1,results.size());
		
		for(Iterator i = results.iterator();i.hasNext();)
		{
			Deck result = (Deck)i.next();
			assertNotNull(result);
			assertNotNull(result.getId());
			assertEquals(result.getId().getRoot(),II_ROOT_GLOBAL_CONSTANT_VALUE);
		}
	}

	public void testOneLevelAssociationHQLSearch1() throws ApplicationException {

		HQLCriteria hqlCriteria = new HQLCriteria(
				"select card from gov.nih.nci.cacoresdk.domain.other.levelassociation.Hand as hand "
						+ "left join hand.cardCollection as card where hand.id='1'");
		Collection results = search(hqlCriteria,
				"gov.nih.nci.cacoresdk.domain.other.levelassociation.Card");
		assertNotNull(results);
		assertEquals(3, results.size());

		for (Iterator i = results.iterator(); i.hasNext();) {
			Card result = (Card) i.next();
			assertNotNull(result);
			assertNotNull(result.getId());
			assertEquals(result.getId().getRoot(),II_ROOT_GLOBAL_CONSTANT_VALUE);
		}
	}


	public void testOneLevelAssociationHQLSearch2() throws ApplicationException {
		HQLCriteria hqlCriteria = new HQLCriteria(
				"select card from gov.nih.nci.cacoresdk.domain.other.levelassociation.Hand as hand "
						+ " left join hand.cardCollection as card where (hand.id='1' or hand.id='2') and (card.name='Ace')");
		Collection results = search(hqlCriteria,
				"gov.nih.nci.cacoresdk.domain.other.levelassociation.Card");
		
		assertNotNull(results);
		assertEquals(1, results.size());

		for (Iterator i = results.iterator(); i.hasNext();) {
			Card result = (Card) i.next();
			assertNotNull(result);
			assertNotNull(result.getId());
			assertEquals(result.getId().getRoot(),II_ROOT_GLOBAL_CONSTANT_VALUE);
		}
	}
}
