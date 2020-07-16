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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.apache.ofbiz.entity.util.EntityFindOptions;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.graphql.fetcher.utils.DataFetcherUtils;
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

		abstract List<Map<String, Object>> searchFormMapList(Map<String, Object> sourceItem, Map<String, Object> inputFieldsMap,
				DataFetchingEnvironment environment);

		abstract Map<String, Object> searchFormMap(Map<String, Object> sourceItem, Map<String, Object> inputFieldsMap,
				DataFetchingEnvironment environment);

		abstract Map<String, Object> searchFormMapWithPagination(Map<String, Object> sourceItem,
				Map<String, Object> inputFieldsMap, Map<String, Object> operationMap, DataFetchingEnvironment environment);
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
		List<Map<String, Object>> searchFormMapList(Map<String, Object> sourceItem, Map<String, Object> inputFieldsMap,
				DataFetchingEnvironment environment) {
			List<GenericValue> result = null;
			List<Map<String, Object>> resultWrapper = new ArrayList<>();
			List<EntityCondition> entityConditions = new ArrayList<EntityCondition>();
			if(inputFieldsMap.size() != 0) {
				entityConditions.add(EntityCondition.makeCondition(inputFieldsMap));	
			} else {
				DataFetcherUtils.addEntityConditions(entityConditions, inputFieldsMap, GraphQLSchemaUtil.getEntityDefinition(entityName, delegator));
			}
			
			if (environment != null) {
				for (Map.Entry<String, String> entry : relKeyMap.entrySet()) {
					entityConditions.add(EntityCondition.makeCondition(entry.getValue(), EntityOperator.EQUALS, ((Map<?, ?>) environment.getSource()).get(entry.getKey())));
				}	
			}
			
			try {
				result = delegator.findList(entityName, EntityCondition.makeCondition(entityConditions), null, null, null, false);
			} catch (GenericEntityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			result.forEach((gv) -> {
				resultWrapper.add(new HashMap<String, Object>(gv));
			});	
			return resultWrapper;
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
		Map<String, Object> searchFormMapWithPagination(Map<String, Object> sourceItem, Map<String, Object> inputFieldsMap, Map<String, Object> operationMap,
				DataFetchingEnvironment environment) {
			List<GenericValue> result = null;
			List<Map<String, Object>> resultWrapper = new ArrayList<>();
			Map<String, Object> arguments = environment.getArguments();
			Map<String, Object> paginationMap = (Map<String, Object>) arguments.get("pagination");
			EntityFindOptions options = new EntityFindOptions();
			int pageIndex = (int) paginationMap.get("pageIndex");
			int pageSize = (int) paginationMap.get("pageSize");
			String orderBy = (String) paginationMap.get("orderByField");
			options.setLimit(pageSize);
			options.setMaxRows(pageSize);
			options.setOffset(pageSize * pageIndex);
			int count = 0;
			List<EntityCondition> entityConditions = new ArrayList<EntityCondition>();
			if(inputFieldsMap.size() != 0) {
				entityConditions.add(EntityCondition.makeCondition(inputFieldsMap));	
			} else {
				DataFetcherUtils.addEntityConditions(entityConditions, operationMap, GraphQLSchemaUtil.getEntityDefinition(entityName, delegator));
			}
			for (Map.Entry<String, String> entry : relKeyMap.entrySet()) {
				entityConditions.add(EntityCondition.makeCondition(entry.getValue(), EntityOperator.EQUALS, ((Map<?, ?>) environment.getSource()).get(entry.getKey())));
			}
			try {
				count = (int)delegator.findCountByCondition(entityName, EntityCondition.makeCondition(entityConditions), null, options);
				result = delegator.findList(entityName, EntityCondition.makeCondition(entityConditions), null, UtilValidate.isNotEmpty(orderBy) ? Arrays.asList(orderBy.split(",")) : null, options, false);
			} catch (GenericEntityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			result.forEach((gv) -> {
				resultWrapper.add(new HashMap<String, Object>(gv));
			});
			
			 Map<String, Object> resultMap = new HashMap<>();
			 resultMap.put("pageIndex", pageIndex);
			 resultMap.put("pageSize", pageSize);
			 resultMap.put("count", count);
			 resultMap.put("data", resultWrapper);
			
			return resultMap;
		}

	}

	static class InternalServiceDataFetcher extends InternalDataFetcher {
		InternalServiceDataFetcher(Element node, FieldDefinition fieldDef, Delegator delegator,
				Map<String, String> relKeyMap) {
			super(fieldDef, delegator, relKeyMap);
		}

		@Override
		List<Map<String, Object>> searchFormMapList(Map<String, Object> sourceItem, Map<String, Object> inputFieldsMap,
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
		Map<String, Object> searchFormMapWithPagination(Map<String, Object> sourceItem, Map<String, Object> inputFieldsMap, Map<String, Object> operationMap,
				DataFetchingEnvironment environment) {
			// TODO Auto-generated method stub
			return null;
		}

	}

	private List<Map<String, Object>> mergeWithConcreteValue(List<Map<String, Object>> interfaceValueList) {
		Set<String> resolverValues = new HashSet<>();
		interfaceValueList.forEach((Map<String, Object> it) -> {
			if((String) it.get(resolverField) != null) {
				resolverValues.add((String) it.get(resolverField));	
			}
			
		});
		
		for (String resolverValue : resolverValues) {
			InternalDataFetcher resolverFetcher = resolverFetcherMap.get(resolverValue);
			if (resolverFetcher == null)
				continue;

			List<Map<String, Object>> filterValueList = interfaceValueList.stream().filter((it) -> (resolverValue).equals((String) it.get(resolverField))).collect(Collectors.toList());
			List<Map<String, Object>> concreteValueList = resolverFetcher.searchFormMapList(null, new HashMap(), null);
			concreteValueList.forEach((concreteValue) -> {
				Map<String, Object> interValue = filterValueList.stream().filter((it) -> {
					boolean res = it.get(primaryField).equals(concreteValue.get(primaryField));
					return res;
				}).findAny().orElse(null);
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
		int relKeyCount = relKeyMap.size();
		Map<String, Object> inputFieldsMap = new HashMap<>();
		Map<String, Object> operationMap = new HashMap<>();
		Map<String, Object> resultMap = new HashMap<>();
		Map<String, Object> interfaceValuedMap = null;
		Map source = environment.getSource();
		GraphQLSchemaUtil.transformArguments(environment.getArguments(), inputFieldsMap, operationMap);
		if (operation.equals("one")) {
			resultMap = defaultFetcher.searchFormMap(source, inputFieldsMap, environment);
			mergeWithConcreteValue(resultMap);
			return resultMap;
		} else if (operation.equals("list")) {
             Map<String, Object> edgesData;
             List<Map<String, Object>> edgesDataList;
             if (GraphQLSchemaUtil.requirePagination(environment)) {
            	Map<String, Object> arguments = environment.getArguments();
 				Map<String, Object> paginationMap = (Map<String, Object>) arguments.get("pagination");
            	int pageIndex = (int) paginationMap.get("pageIndex");
 				int pageSize = (int) paginationMap.get("pageSize");
 				int pageRangeLow = pageIndex * pageSize + 1;
 				int pageRangeHigh = (pageIndex * pageSize) + pageSize;
            	interfaceValuedMap = defaultFetcher.searchFormMapWithPagination(source, inputFieldsMap, operationMap, environment);
            	int count = (int)interfaceValuedMap.get("count");
            	int pageMaxIndex = new BigDecimal(count - 1).divide(new BigDecimal(pageSize), 0, BigDecimal.ROUND_DOWN).intValue();
            	Map<String, Object> pageInfo = new HashMap<String, Object>();
            	boolean hasPreviousPage = pageIndex > 0;
				pageInfo.put("pageIndex", pageIndex);
				pageInfo.put("pageSize", pageSize);
				pageInfo.put("pageRangeLow", pageRangeLow);
				pageInfo.put("pageRangeHigh", pageRangeHigh);
				pageInfo.put("hasPreviousPage", hasPreviousPage);
 				if (pageRangeHigh > count)
 					pageRangeHigh = count;
 				boolean hasNextPage = pageMaxIndex > pageIndex;
 				pageInfo.put("hasNextPage", hasNextPage);
 				pageInfo.put("totalCount", count);
 				List<Map<String, Object>> interfaceValueList = (List<Map<String, Object>>)interfaceValuedMap.get("data");
 				interfaceValueList = mergeWithConcreteValue(interfaceValueList);
 				edgesDataList = new ArrayList<Map<String, Object>>(interfaceValueList != null ? interfaceValueList.size() : 0);
 				String cursor = null;
 				pageInfo.put("startCursor", GraphQLSchemaUtil.encodeRelayCursor(interfaceValueList.get(0), Arrays.asList(primaryField))); //TODO
 				pageInfo.put("endCursor", GraphQLSchemaUtil.encodeRelayCursor(interfaceValueList.get(interfaceValueList.size() - 1),  Arrays.asList(primaryField))); //TODO
 				for (Map<String, Object> gv : interfaceValueList) {
 					edgesData = new HashMap<>(2);
 					cursor = GraphQLSchemaUtil.encodeRelayCursor(gv,  Arrays.asList(primaryField));
 					edgesData.put("cursor", cursor); //TODO
 					edgesData.put("node", gv);
 					edgesDataList.add(edgesData);
 				}
 				resultMap.put("edges", edgesDataList);	
 				resultMap.put("pageInfo", pageInfo);     	 
            	
             } else {
            	 List<Map<String, Object>> interfaceValueList = defaultFetcher.searchFormMapList(source, inputFieldsMap, environment);
            	 interfaceValueList = mergeWithConcreteValue(interfaceValueList); 
            	 List<Map<String, Object>> jointOneList = relKeyCount == 0 ? interfaceValueList : interfaceValueList.stream().filter((list) -> {
            		 return matchParentByRelKeyMap(source, list, relKeyMap);
            	 }).collect(Collectors.toList());
            	edgesDataList = new ArrayList<Map<String, Object>>(jointOneList != null ? jointOneList.size() : 0);
 				String cursor = null;
 					for (Map<String, Object> gv : jointOneList) {
 						edgesData = new HashMap<>(2);
 						cursor = GraphQLSchemaUtil.encodeRelayCursor(gv,  Arrays.asList(primaryField));
 						edgesData.put("cursor", cursor);
 						edgesData.put("node", gv);
 						edgesDataList.add(edgesData);
 					}
 					resultMap = new HashMap<>(1);
 	                resultMap.put("edges", edgesDataList);
             }
		}
		return resultMap;
	}

}
