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
package org.apache.ofbiz.graphql.fetcher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.graphql.schema.GraphQLSchemaDefinition.FieldDefinition;
import org.apache.ofbiz.graphql.schema.GraphQLSchemaUtil;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ModelService;
import org.apache.ofbiz.service.ServiceUtil;
import org.w3c.dom.Element;

import graphql.schema.DataFetchingEnvironment;
import graphql.servlet.context.DefaultGraphQLServletContext;

public class ServiceDataFetcher extends BaseDataFetcher {

	private String serviceName;
	private String requireAuthentication;

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getRequireAuthentication() {
		return requireAuthentication;
	}

	public boolean isEntityAutoService() {
		return isEntityAutoService;
	}

	public boolean isResultPrimitive() {
		return resultPrimitive;
	}

	public Map<String, String> getRelKeyMap() {
		return relKeyMap;
	}

	boolean isEntityAutoService;
	boolean resultPrimitive = false;
	String defaultEntity;
	Map<String, String> relKeyMap = new HashMap<>();

	public ServiceDataFetcher(Element node, FieldDefinition fieldDef, Delegator delegator, LocalDispatcher dispatcher) {
		super(fieldDef, delegator);
		this.requireAuthentication = node.getAttribute("require-authentication") != null
				? node.getAttribute("require-authentication")
				: "true";
		this.serviceName = node.getAttribute("service");
		List<? extends Element> elements = UtilXml.childElementList(node, "key-map");
		for (Element keyMapNode : elements) {
			relKeyMap.put(keyMapNode.getAttribute("field-name"),
					keyMapNode.getAttribute("related") != null ? keyMapNode.getAttribute("related")
							: keyMapNode.getAttribute("field-name"));
		}

		try {
			ModelService service = dispatcher.getDispatchContext().getModelService(serviceName);
			if (service == null) {
				throw new IllegalArgumentException("Service ${serviceName} not found");
			}
			if (service.engineName.equalsIgnoreCase("entity-auto")) {
				isEntityAutoService = true;
			}

			defaultEntity = service.defaultEntityName;

			if (this.isEntityAutoService) {
				if (!fieldDef.isMutation())
					throw new IllegalArgumentException("Query should not use entity auto service ${serviceName}");
			} else {
				if (service.getParam("_graphql_result_primitive") != null)
					resultPrimitive = true;
			}

		} catch (GenericServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	Object fetch(DataFetchingEnvironment environment) {
		System.out.println("Here, came - ");
		System.out.println("environment " + environment);
		System.out.println("Arguments " + environment.getArguments());

		String productId = environment.getArgument("id");
		DefaultGraphQLServletContext context = environment.getContext();
		HttpServletRequest request = context.getHttpServletRequest();
		Delegator delegator = (Delegator) request.getAttribute("delegator");
		LocalDispatcher dispatcher = (LocalDispatcher) request.getAttribute("dispatcher");
		System.out.println(" this.serviceName needs to be called - " + this.serviceName);

		GenericValue userLogin = (GenericValue) request.getAttribute("userLogin");

		if (delegator == null) {
			delegator = (Delegator) request.getServletContext().getAttribute("delegator");
		}
		if (dispatcher == null) {
			dispatcher = (LocalDispatcher) request.getServletContext().getAttribute("dispatcher");
		}

		ModelService service = null;
		;
		try {
			service = dispatcher.getDispatchContext().getModelService(serviceName);
		} catch (GenericServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Map<String, Object> inputFieldsMap = new HashMap<>();
		inputFieldsMap.put("userLogin", userLogin);
		if (fieldDef.isMutation()) {
			GraphQLSchemaUtil.transformArguments(environment.getArguments(), inputFieldsMap);
		} else {
			GraphQLSchemaUtil.transformQueryServiceArguments(service, environment.getArguments(), inputFieldsMap);
			Map source = environment.getSource();
			GraphQLSchemaUtil.transformQueryServiceRelArguments(source, relKeyMap, inputFieldsMap);
		}

		Map<String, Object> result = null;

		try {
			if (fieldDef.isMutation()) {
				result = dispatcher.runSync(serviceName, inputFieldsMap);
				String verb = GraphQLSchemaUtil.getVerbFromName(serviceName, dispatcher);
				if (this.isEntityAutoService) {
					if (verb.equals("delete")) { // delete return result object { error, message }

					} else {
						String entityName = GraphQLSchemaUtil.getDefaultEntityName(verb, dispatcher);
					}
				}
			} else {
				result = dispatcher.runSync(serviceName, inputFieldsMap);

			}
		} catch (GenericServiceException e) {
           e.printStackTrace();
		}

		if (ServiceUtil.isSuccess(result)) {
			return result.get("_graphql_result_");
		} else {
			return ServiceUtil.getErrorMessage(result);
		}
	}
}
