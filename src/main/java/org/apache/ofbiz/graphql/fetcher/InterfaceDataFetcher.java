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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.model.ModelEntity;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.graphql.schema.GraphQLSchemaDefinition.FieldDefinition;
import org.apache.ofbiz.graphql.schema.GraphQLSchemaUtil;
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
		primaryField = UtilValidate.isNotEmpty(node.getAttribute("primary-field")) ? node.getAttribute("primary-field") : (UtilValidate.isNotEmpty(refNode) ? refNode.getAttribute("primary-field") : "");
		resolverField = UtilValidate.isNotEmpty(node.getAttribute("resolver-field"))? node.getAttribute("resolver-field")
				: (UtilValidate.isNotEmpty(refNode) ? refNode.getAttribute("resolver-field") : "");
		useCache = "true" == (UtilValidate.isNotEmpty(node.getAttribute("cache")) ? node.getAttribute("cache")
				: (UtilValidate.isNotEmpty(refNode)? refNode.getAttribute("cache") : "false"));

		Map<String, String> pkRelMap = new HashMap<>(1);
		pkRelMap.put(primaryField, primaryField);

		List<? extends Element> keyMapNodeElements = UtilXml.childElementList(node, "key-map");
		List<? extends Element> keyMapRefNodeElements = UtilXml.childElementList(refNode, "key-map");
		List<? extends Element> keyMapChildren = UtilValidate.isNotEmpty(keyMapNodeElements) ? keyMapNodeElements : keyMapRefNodeElements;

		for (Element keyMapNode : keyMapChildren) {
			relKeyMap.put(keyMapNode.getAttribute("field-name"), UtilValidate.isNotEmpty(keyMapNode.getAttribute("related")) ? keyMapNode.getAttribute("related") : keyMapNode.getAttribute("field-name"));
		}

		List<? extends Element> defaultFetcherNodeElements = UtilXml.childElementList(node, "default-fetcher");
		List<? extends Element> defaultFetcherRefNodeElements = UtilXml.childElementList(refNode, "default-fetcher");
		List<? extends Element> defaultFetcherChildren = defaultFetcherNodeElements.size() != 0 ? defaultFetcherNodeElements : defaultFetcherRefNodeElements;

		if (defaultFetcherChildren.size() != 1)
			throw new IllegalArgumentException("interface-fetcher.default-fetcher not found");
		Element defaultFetcherNode = defaultFetcherChildren.get(0);
		defaultFetcher = buildDataFetcher(UtilXml.childElementList(defaultFetcherNode).get(0), fieldDef, delegator, relKeyMap);

		List<? extends Element> resolverFetcherChildren = UtilXml.childElementList(node, "resolver-fetcher");
		resolverFetcherChildren = UtilValidate.isNotEmpty(resolverFetcherChildren) ? resolverFetcherChildren
				: UtilXml.childElementList(refNode, "resolver-fetcher");
		for (Element resolverFetcherNode : resolverFetcherChildren) {
			String resolverValue = resolverFetcherNode.getAttribute("resolver-value");
			InternalDataFetcher dataFetcher = buildDataFetcher(UtilXml.childElementList(resolverFetcherNode).get(0),
					fieldDef, delegator, pkRelMap);
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
		default:
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

		abstract List<Map<String, Object>> searchFormMap(List sourceItems, Map<String, Object> inputFieldsMap,
				DataFetchingEnvironment environment);

		abstract Map<String, Object> searchFormMap(Map<String, Object> sourceItem, Map<String, Object> inputFieldsMap,
				DataFetchingEnvironment environment);

		abstract Map<String, Object> searchFormMapWithPagination(List<Object> sourceItems,
				Map<String, Object> inputFieldsMap, DataFetchingEnvironment environment);
	}

	static class InternalEntityDataFetcher extends InternalDataFetcher {
		String entityName;
		boolean useCache = false;

		InternalEntityDataFetcher(Element node, FieldDefinition fieldDef, Delegator delegator,
				Map<String, String> relKeyMap) {
			super(fieldDef, delegator, relKeyMap);
			entityName = node.getAttribute("entity-name");
			ModelEntity entity = GraphQLSchemaUtil.getEntityDefinition(entityName, delegator);
			if (entity == null)
				throw new IllegalArgumentException("Entity [" + entityName + "] not found");
			useCache = entity.getNeverCache() ? false : true;
		}

		@Override
		List<Map<String, Object>> searchFormMap(List sourceItems, Map<String, Object> inputFieldsMap,
				DataFetchingEnvironment environment) {
			return null;
		}

		@Override
		Map<String, Object> searchFormMap(Map<String, Object> sourceItem, Map<String, Object> inputFieldsMap,
				DataFetchingEnvironment environment) {
			GenericValue entity = null;
			try {
				EntityQuery entityQuery = EntityQuery.use(delegator).from(entityName).where(inputFieldsMap);
				if (environment != null) {
					for (Map.Entry<String, String> entry : relKeyMap.entrySet()) {
						entityQuery.where(EntityCondition.makeCondition(entry.getValue(), EntityOperator.EQUALS, ((Map<?, ?>) environment.getSource()).get(entry.getKey())));
					}
				}
				if (sourceItem != null) {
					for (Map.Entry<String, String> entry : relKeyMap.entrySet()) {
						String relParentFieldName = entry.getKey();
					    String relFieldName = entry.getValue();
						entityQuery.where(EntityCondition.makeCondition(relFieldName, EntityOperator.EQUALS, sourceItem.get(relParentFieldName)));
					}
				}
				entity = entityQuery.queryOne();
				if (UtilValidate.isEmpty(entity)) {
					return null;
				}
			} catch (GenericEntityException e) {
				e.printStackTrace();
			}

			return new HashMap<String, Object>(entity);
		}

		@Override
		Map<String, Object> searchFormMapWithPagination(List<Object> sourceItems, Map<String, Object> inputFieldsMap,
				DataFetchingEnvironment environment) {
			return null;
		}

	}

	static class InternalServiceDataFetcher extends InternalDataFetcher {
		InternalServiceDataFetcher(Element node, FieldDefinition fieldDef, Delegator delegator,
				Map<String, String> relKeyMap) {
			super(fieldDef, delegator, relKeyMap);
		}

		@Override
		List<Map<String, Object>> searchFormMap(List sourceItems, Map<String, Object> inputFieldsMap,
				DataFetchingEnvironment environment) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		Map<String, Object> searchFormMap(Map<String, Object> sourceItem, Map<String, Object> inputFieldsMap,
				DataFetchingEnvironment environment) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		Map<String, Object> searchFormMapWithPagination(List<Object> sourceItems, Map<String, Object> inputFieldsMap,
				DataFetchingEnvironment environment) {
			// TODO Auto-generated method stub
			return null;
		}

	}

	private List<Map<String, Object>> mergeWithConcreteValue(List<Map<String, Object>> interfaceValueList) {
		Set<String> resolverValues = new HashSet<>();
		interfaceValueList.forEach((Map<String, Object> it) -> {
			resolverValues.add((String) it.get(resolverField));
		});
		for (String resolverValue : resolverValues) {
			InternalDataFetcher resolverFetcher = resolverFetcherMap.get(resolverValue);
			if (resolverFetcher == null)
				continue;

			List<Map<String, Object>> filterValueList = interfaceValueList.stream().filter((it) -> ((String) it.get(resolverField)).equals(resolverValue)).collect(Collectors.toList());

			List<Map<String, Object>> concreteValueList = resolverFetcher.searchFormMap(filterValueList, new HashMap(), null);
			concreteValueList.forEach((concreteValue) -> {
				Map<String, Object> interValue = interfaceValueList.stream().filter((it) -> {
					return it.get(primaryField).equals(concreteValue.get(primaryField));
				}).collect(Collectors.toList()).get(0);
				if (interValue != null)
					interValue.putAll(concreteValue);
			});
		}
		return interfaceValueList;
	}

	private Map<String, Object> mergeWithConcreteValue(Map<String, Object> interfaceValue) {
		if (interfaceValue == null)
			return interfaceValue;
		String resolverValue = (String) interfaceValue.get(resolverField);
		InternalDataFetcher resolverFetcher = resolverFetcherMap.get(resolverValue);
		if (resolverFetcher == null)
			return interfaceValue;

		Map<String, Object> concreteValue = resolverFetcher.searchFormMap(interfaceValue, new HashMap(), null);
		if (concreteValue != null)
			interfaceValue.putAll(concreteValue);
		return interfaceValue;
	}

	private static boolean matchParentByRelKeyMap(Map<String, Object> sourceItem, Map<String, Object> self,
			Map<String, String> relKeyMap) {
		int found = -1;
		for (Map.Entry<String, String> entry : relKeyMap.entrySet()) {
			found = (found == -1) ? (sourceItem.get(entry.getKey()) == self.get(entry.getValue()) ? 1 : 0) : (found == 1 && sourceItem.get(entry.getKey()) == self.get(entry.getValue()) ? 1 : 0);
		}
		return found == 1;
	}

	@Override
	Object fetch(DataFetchingEnvironment environment) {
		Map<String, Object> inputFieldsMap = new HashMap<>();
		Map<String, Object> operationMap = new HashMap<>();
		Map<String, Object> resultMap = new HashMap<>();
		Map source = environment.getSource();
		GraphQLSchemaUtil.transformArguments(environment.getArguments(), inputFieldsMap, operationMap);
		if (operation.equals("one")) {
			resultMap = defaultFetcher.searchFormMap(source, inputFieldsMap, environment);
			mergeWithConcreteValue(resultMap);
			return resultMap;
		} else if (operation.equals("list")) {

		}
		return null;
	}

}
