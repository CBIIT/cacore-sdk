/*L
 *  Copyright Ekagra Software Technologies Ltd.
 *  Copyright SAIC, SAIC-Frederick
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/cacore-sdk/LICENSE.txt for details.
 */

package gov.nih.nci.system.security.acegi.ui.globus;

import java.io.IOException;
import java.security.cert.X509Certificate;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.axis.gsi.GSIConstants;
import org.globus.gsi.bc.BouncyCastleUtil;
import org.globus.gsi.gssapi.GSSConstants;
import org.globus.gsi.gssapi.GlobusGSSContextImpl;


/**
 * This filter checks the X509 certificate chain in globus context and then puts it in the session under property name of
 * javax.servlet.request.X509Certificate 
 *
 * @author Luke Taylor
 * @version $Id: X509InitializationFilter.java,v 1.1 2009/01/27 15:37:49 satish79 Exp $
 */
public class X509InitializationFilter implements Filter {
    //~ Static fields/initializers =====================================================================================

    private static final Log logger = LogFactory.getLog(X509InitializationFilter.class);


    public void destroy() {}

    /**
     * This method checks the X509 certificate chain in globus context and then puts it in the session under property name of
     * javax.servlet.request.X509Certificate 
     * @param request DOCUMENT ME!
     * @param response DOCUMENT ME!
     * @param filterChain DOCUMENT ME!
     *
     * @throws IOException DOCUMENT ME!
     * @throws javax.servlet.ServletException DOCUMENT ME!
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
        throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest)) {
            throw new ServletException("Can only process HttpServletRequest");
        }

        if (!(response instanceof HttpServletResponse)) {
            throw new ServletException("Can only process HttpServletResponse");
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        if (logger.isDebugEnabled()) {
            logger.debug("Checking HTTP Request for Globus Context ");
        }
        
        try 
        {
        	Object obj = httpRequest.getAttribute(GSIConstants.GSI_CONTEXT);
        	if(obj!=null)
        	{
	        	GlobusGSSContextImpl ctx = (GlobusGSSContextImpl)obj;
	        	X509Certificate[] certChain = (X509Certificate[])ctx.inquireByOid(GSSConstants.X509_CERT_CHAIN);
	        	if(certChain!=null)
	        	{
	        		X509Certificate[] idCertChain = new X509Certificate[]{BouncyCastleUtil.getIdentityCertificate(certChain)}; 
		        	request.setAttribute("javax.servlet.request.X509Certificate", idCertChain);
	        	}
        	}
        }
        catch(Exception e)
        {
        	logger.error("Error getting X509Certificate chain from the globus context:",e);
        }
        filterChain.doFilter(request, response);
    }


    public void init(FilterConfig ignored) throws ServletException {}
}
