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

import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.graphql.schema.GraphQLSchemaDefinition.FieldDefinition;
import org.apache.ofbiz.service.LocalDispatcher;
import org.w3c.dom.Element;

import graphql.schema.DataFetchingEnvironment;

public class InterfaceDataFetcher extends BaseDataFetcher {
	String primaryField;
	String resolverField;
	String requireAuthentication;
	String operation;
	String fieldRawType;
	Map<String, String> relKeyMap = new HashMap<>(1);
	InternalDataFetcher defaultFetcher;
	Map<String, InternalDataFetcher> resolverFetcherMap = new HashMap<>(1);
	boolean useCache;

	public InterfaceDataFetcher(Element node, Element refNode, FieldDefinition fieldDef, Delegator delegator,
			LocalDispatcher dispatcher) {
		super(fieldDef, delegator);
		primaryField = node.getAttribute("primary-field") != null ? node.getAttribute("primary-field")
				: (refNode != null ? refNode.getAttribute("primary-field") : "");
		resolverField = node.getAttribute("resolver-field") != null ? node.getAttribute("resolver-field")
				: (refNode != null ? refNode.getAttribute("resolver-field") : "");
		useCache = "true" == (node.getAttribute("cache") != null ? node.getAttribute("cache")
				: (refNode != null ? refNode.getAttribute("cache") : "false"));

		Map<String, String> pkRelMap = new HashMap<>(1);
		pkRelMap.put(primaryField, primaryField);

		List<? extends Element> keyMapNodeElements = UtilXml.childElementList(node, "key-map");
		List<? extends Element> keyMapRefNodeElements = UtilXml.childElementList(refNode, "key-map");
		List<? extends Element> keyMapChildren = keyMapNodeElements != null ? keyMapNodeElements
				: keyMapRefNodeElements;

		for (Element keyMapNode : keyMapChildren) {
			relKeyMap.put(keyMapNode.getAttribute("field-name"),
					keyMapNode.getAttribute("related") != null ? keyMapNode.getAttribute("related")
							: keyMapNode.getAttribute("field-name"));
		}

		List<? extends Element> defaultFetcherNodeElements = UtilXml.childElementList(node, "default-fetcher");
		List<? extends Element> defaultFetcherRefNodeElements = UtilXml.childElementList(refNode, "default-fetcher");
		List<? extends Element> defaultFetcherChildren = defaultFetcherNodeElements.size() != 0 ? defaultFetcherNodeElements: defaultFetcherRefNodeElements;

		if (defaultFetcherChildren.size() != 1)
			throw new IllegalArgumentException("interface-fetcher.default-fetcher not found");
		Element defaultFetcherNode = defaultFetcherChildren.get(0);
		defaultFetcher = buildDataFetcher(UtilXml.childElementList(defaultFetcherNode).get(0), fieldDef, delegator, relKeyMap);


		List<? extends Element> resolverFetcherChildren = UtilXml.childElementList(node, "resolver-fetcher");
		resolverFetcherChildren = resolverFetcherChildren != null ? resolverFetcherChildren : UtilXml.childElementList(refNode, "resolver-fetcher");
		for (Element resolverFetcherNode : resolverFetcherChildren) {
			String resolverValue = resolverFetcherNode.getAttribute("resolver-value");
			InternalDataFetcher dataFetcher = buildDataFetcher(UtilXml.childElementList(resolverFetcherNode).get(0), fieldDef, delegator, pkRelMap);
			resolverFetcherMap.put(resolverValue, dataFetcher);
		}
		
		initializeFields();

	}

	private void initializeFields() {
		this.requireAuthentication = fieldDef.getRequireAuthentication() != null ? fieldDef.getRequireAuthentication()
				: "true";
		this.fieldRawType = fieldDef.getType();
		if ("true".equals(fieldDef.getIsList()))
			this.operation = "list";
		else
			this.operation = "one";
	}

	private static InternalDataFetcher buildDataFetcher(Element node, FieldDefinition fieldDef, Delegator delegator,
			Map<String, String> relKeyMap) {
		switch (node.getNodeName()) {
		case "entity-fetcher":
			return new InternalEntityDataFetcher(node, fieldDef, delegator, relKeyMap);
		case "service-fetcher":
			return new InternalServiceDataFetcher(node, fieldDef, delegator, relKeyMap);
	     default :
	    throw new IllegalArgumentException("interface-fetcher.default-fetcher not found");
		}
	}

	static abstract class InternalDataFetcher {
		FieldDefinition fieldDef;
		Delegator delegator;
		Map<String, String> relKeyMap = new HashMap<>(1);

		InternalDataFetcher(FieldDefinition fieldDef, Delegator delegator, Map<String, String> relKeyMap) {
			this.fieldDef = fieldDef;
			this.delegator = delegator;
			this.relKeyMap.putAll(relKeyMap);
		}
	}

	static class InternalEntityDataFetcher extends InternalDataFetcher {
		String entityName;
		boolean useCache = false;

		InternalEntityDataFetcher(Element node, FieldDefinition fieldDef, Delegator delegator, Map<String, String> relKeyMap) {
            super(fieldDef, delegator, relKeyMap);
            entityName = node.getAttribute("entity-name");
        }

	}

	static class InternalServiceDataFetcher extends InternalDataFetcher {
		InternalServiceDataFetcher(Element node, FieldDefinition fieldDef, Delegator delegator, Map<String, String> relKeyMap) {
            super(fieldDef, delegator, relKeyMap);
        }

	}
	
	
	@Override
	Object fetch(DataFetchingEnvironment environment) {
		return super.fetch(environment);
	}
	

}
