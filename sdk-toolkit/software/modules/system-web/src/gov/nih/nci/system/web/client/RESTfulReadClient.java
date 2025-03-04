/*L
 *  Copyright Ekagra Software Technologies Ltd.
 *  Copyright SAIC, SAIC-Frederick
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/cacore-sdk/LICENSE.txt for details.
 */

package gov.nih.nci.system.web.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.commons.codec.binary.Base64;

public class RESTfulReadClient {
	public RESTfulReadClient()
	{
	}

	public Response read(String url) {
	  try {
			if (url == null) {
			ResponseBuilder builder = Response.status(Status.BAD_REQUEST);
			builder.type("application/xml");
			StringBuffer buffer = new StringBuffer();
			buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			buffer.append("<response>");
			buffer.append("<type>ERROR</type>");
			buffer.append("<code>INVALID_URL</code>");
			buffer.append("<message>Invalid URL: "+url+"</message>");
			buffer.append("</response>");
			builder.entity(buffer.toString());
			return builder.build();
			}
		WebClient client = WebClient.create(url);
		client.type("application/xml").accept("application/xml");
		return client.get();

	  } catch (Exception e) {
		e.printStackTrace();
	  }
	  return null;
	}
	
	public Response read(String url, String userName, String password) {
		  try {
				if (url == null) {
				ResponseBuilder builder = Response.status(Status.BAD_REQUEST);
				builder.type("application/xml");
				StringBuffer buffer = new StringBuffer();
				buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
				buffer.append("<response>");
				buffer.append("<type>ERROR</type>");
				buffer.append("<code>INVALID_URL</code>");
				buffer.append("<message>Invalid URL: "+url+"</message>");
				buffer.append("</response>");
				builder.entity(buffer.toString());
				return builder.build();
				}
			WebClient client = WebClient.create(url);
			client.type("application/xml").accept("application/xml");
			String base64encodedUsernameAndPassword = new String(
					Base64.encodeBase64((userName + ":" + password)
							.getBytes()));
			client.header("Authorization", "Basic "
					+ base64encodedUsernameAndPassword);
		
			return client.get();

		  } catch (Exception e) {
			e.printStackTrace();
		  }
		  return null;
		}	
}