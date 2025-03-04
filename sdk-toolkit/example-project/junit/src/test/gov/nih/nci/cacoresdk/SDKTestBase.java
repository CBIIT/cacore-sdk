/*L
 *  Copyright Ekagra Software Technologies Ltd.
 *  Copyright SAIC, SAIC-Frederick
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/cacore-sdk/LICENSE.txt for details.
 */

package test.gov.nih.nci.cacoresdk;
import junit.framework.TestCase;
import gov.nih.nci.system.applicationservice.ApplicationService;
import gov.nih.nci.system.client.ApplicationServiceProvider;
import gov.nih.nci.system.util.ClassCache;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


/**
 * @author Satish Patel
 *
 */
public abstract class SDKTestBase extends TestCase {

	private ApplicationService appService;
	private ApplicationService appServiceFromUrl;
	protected String baseURL = "http://localhost:21080/example";
	static String paths[] = {"application-config.xml"};
	private static ApplicationContext ctx = new ClassPathXmlApplicationContext(
			paths);;
	private ClassCache classCache;

	protected void setUp() throws Exception {
		super.setUp();
		appService = ApplicationServiceProvider.getApplicationService();
		classCache = (ClassCache) ctx.getBean("ClassCache");
	}


	protected void tearDown() throws Exception
	{
		appService = null;
		super.tearDown();
	}

	protected ApplicationService getApplicationService()
	{
		return appService;
	}

	public ClassCache getClassCache()
	{
		return classCache;
	}

	protected ApplicationService getApplicationServiceFromUrl() throws Exception
	{
		String url = "http://localhost:21080/example";
		appServiceFromUrl = ApplicationServiceProvider.getApplicationServiceFromUrl(url);
		return appServiceFromUrl;
	}

	protected ApplicationService getBadApplicationServiceFromUrl() throws Exception
	{
		String url = "http://badhost:21080/badcontext";
		appServiceFromUrl = ApplicationServiceProvider.getApplicationServiceFromUrl(url);
		return appServiceFromUrl;
	}

	public static String getTestCaseName()
	{
		return "SDK Base Test Case";
	}

}
