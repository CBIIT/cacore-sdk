/*L
 *  Copyright Ekagra Software Technologies Ltd.
 *  Copyright SAIC, SAIC-Frederick
 *
 *  Distributed under the OSI-approved BSD 3-Clause License.
 *  See http://ncip.github.com/cacore-sdk/LICENSE.txt for details.
 */

package gov.nih.nci.sdk.example.generator;

import java.io.File;
import gov.nih.nci.sdk.core.ScriptContext;

public abstract class Generator
{
	public static final String SERVICE_PACKAGE_NAME="service";
	public static final String SERVICE_CLIENT_PACKAGE_NAME="client";
	public static final String JAXBPOJO_PACKAGE_NAME="jaxb";
	public static final String POJO_PACKAGE_NAME="pojo";
	public static final String TEMPLATES_PACKAGE_NAME="templates";
	public static final String GENERATED_PACKAGE_NAME="generated";
	public static final String IMPL_PACKAGE_NAME="impl";

	public static final String GENERATOR_TARGET_PATH = File.separator + "src" + File.separator + "gen" + File.separator + "java";
	public static final String WEBINF_PATH = File.separator + GENERATOR_TARGET_PATH + File.separator + "WEB-INF";

	protected ScriptContext scriptContext;

	public void setScriptContext(ScriptContext _scriptContext) { scriptContext = _scriptContext; }
	public ScriptContext getScriptContext() { return scriptContext; }

	public Generator(ScriptContext _scriptContext) { setScriptContext(_scriptContext); }

	protected abstract void init();
	protected abstract void preProcess();
	protected abstract void validate();
	public abstract void runProcess();
	protected abstract void postProcess();

	public void generate()
	{
		getScriptContext().getLogger().info("Executing init()");
		init();
		getScriptContext().getLogger().info("Executing preProcess()");
		preProcess();
		getScriptContext().getLogger().info("Executing validate()");
		validate();
		getScriptContext().getLogger().info("Executing runProcess()");
		runProcess();
		getScriptContext().getLogger().info("Executing postProcess()");
		postProcess();
		getScriptContext().getLogger().info("Completed generate()");
	}

	public String determineJavaType(String _substituteType)
	{
		String substituteType = _substituteType;

		if ("string".equalsIgnoreCase(substituteType) == true)
		{
			substituteType = "java.lang.String";
		}

		return substituteType;
	}
}
