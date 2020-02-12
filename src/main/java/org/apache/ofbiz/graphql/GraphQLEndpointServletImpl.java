/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.apache.ofbiz.graphql;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.ofbiz.base.component.ComponentConfig;
import org.apache.ofbiz.base.component.ComponentException;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.graphql.config.OFBizGraphQLObjectMapperConfigurer;
import org.apache.ofbiz.graphql.schema.GraphQLSchemaDefinition;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import graphql.ExecutionResultImpl;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.kickstart.execution.GraphQLObjectMapper;
import graphql.servlet.GraphQLConfiguration;
import graphql.servlet.SimpleGraphQLHttpServlet;


@SuppressWarnings("serial")
public class GraphQLEndpointServletImpl extends SimpleGraphQLHttpServlet {
	
	public static final String MODULE = GraphQLEndpointServletImpl.class.getName();
	private static final String APPLICATION_GRAPHQL = "application/graphql";
	private GraphQLConfiguration configuration;
	private GraphQLObjectMapper mapper;
	private Map<String, Element> graphQLSchemaElementMap = new HashMap<>();
	
	@Override
	protected GraphQLConfiguration getConfiguration() {
		mapper = GraphQLObjectMapper.newBuilder().withObjectMapperConfigurer(new OFBizGraphQLObjectMapperConfigurer()).build();
		loadSchemaElements();
		GraphQLSchemaDefinition schemaDef= new GraphQLSchemaDefinition();
		configuration = GraphQLConfiguration.with(schemaDef.newDynamicSchema()).with(false).with(mapper).build();
		return configuration;
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {	
		if (isContentTypeGraphQL(request)) {
			response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			response.setContentType(MediaType.APPLICATION_JSON);
			GraphQLError error = GraphqlErrorBuilder.newError().message("Content Type application/graphql is only allowed on POST", (Object[]) null).build();
			ExecutionResultImpl result = new ExecutionResultImpl(error);
			try {
				configuration.getObjectMapper().serializeResultAsJson(response.getWriter(), result);
				response.flushBuffer();
			} catch (IOException e) {
				response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			}
		}
		super.doGet(request, response);
	}

	@Override
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		setupCORSHeaders(req, resp);
		resp.flushBuffer();
	}
	

	private boolean isContentTypeGraphQL(HttpServletRequest request) {
		String contentType = request.getHeader(HttpHeaders.CONTENT_TYPE);
		return contentType != null && contentType.equals(APPLICATION_GRAPHQL);
	}

	/**
	 *
	 * @param httpServletRequest
	 * @param response
	 * @throws IOException
	 */
	public void setupCORSHeaders(HttpServletRequest httpServletRequest, ServletResponse response) throws IOException {
		if (response instanceof HttpServletResponse) {
			HttpServletResponse httpServletResponse = (HttpServletResponse) response;
			if (httpServletRequest != null && httpServletRequest.getHeader("Origin") != null) {
				httpServletResponse.setHeader("Access-Control-Allow-Origin", httpServletRequest.getHeader("Origin"));
			} else {
				httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");
			}
			httpServletResponse.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
			httpServletResponse.setHeader("Access-Control-Allow-Credentials", "true");
			httpServletResponse.setHeader("Access-Control-Allow-Methods", "OPTIONS, POST, GET");
		}
	}
	
	/**
	 * 
	 */
	private void loadSchemaElements() {

		Collection<ComponentConfig> components = ComponentConfig.getAllComponents();
		components.forEach(component -> {
			String cName = component.getComponentName();
			try {
				String cBaseLocation = ComponentConfig.getRootLocation(cName);
				File folder = new File(cBaseLocation);
				if (folder.isDirectory() && folder.exists()) {
					// Specifically Look for a file Named component-name.graphql.xml
					String gqlSchemaFileName = cName + ".graphql.xml";
					File graphqlSchemaFile = new File(folder + "/graphql-schema", gqlSchemaFileName);
					if (graphqlSchemaFile.exists()) {
						Debug.logInfo("Processing GraphQL Schema file " + gqlSchemaFileName, MODULE);
						Element element = null;
						try {
							element = UtilXml.readXmlDocument(new FileInputStream(graphqlSchemaFile), true, "GraphQL Schema File", true).getDocumentElement();
						} catch (SAXException | ParserConfigurationException | IOException e) {
							Debug.logError(e, MODULE);
						}
						graphQLSchemaElementMap.put(gqlSchemaFileName, element);
					}
				}

			} catch (ComponentException e) {
				e.printStackTrace();
			}
		});
	}

}
