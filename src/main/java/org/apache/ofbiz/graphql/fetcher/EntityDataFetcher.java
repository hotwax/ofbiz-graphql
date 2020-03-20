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
import java.util.List;
import java.util.Map;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityOperator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.graphql.schema.GraphQLSchemaDefinition.FieldDefinition;
import org.apache.ofbiz.graphql.schema.GraphQLSchemaUtil;
import org.w3c.dom.Element;
import graphql.schema.DataFetchingEnvironment;

public class EntityDataFetcher extends BaseEntityDataFetcher {
	
	public EntityDataFetcher() {
		super(null, null, null);
	}

	public EntityDataFetcher(Delegator delegator, Element node, FieldDefinition fieldDef) {
		super(delegator, node, fieldDef);
	}

	EntityDataFetcher(Delegator delegator, FieldDefinition fieldDef, String entityName, Map<String, String> relKeyMap) {
		this(delegator, fieldDef, entityName, null, relKeyMap);
	}

	EntityDataFetcher(Delegator delegator, FieldDefinition fieldDef, String entityName, String interfaceEntityName,
			Map<String, String> relKeyMap) {
		super(delegator, fieldDef, entityName, interfaceEntityName, relKeyMap);
	}

	Object fetch(DataFetchingEnvironment environment) {
		Map<String, Object> inputFieldsMap = new HashMap<>();
		GraphQLSchemaUtil.transformArguments(environment.getArguments(), inputFieldsMap);
		if (operation.equals("one")) {
			try {
				GenericValue entity = null;
				EntityQuery entityQuery = EntityQuery.use(delegator).from(entityName).where(inputFieldsMap);
				for (Map.Entry<String, String> entry : relKeyMap.entrySet()) {
					entityQuery.where(EntityCondition.makeCondition(entry.getValue(), EntityOperator.EQUALS, ((Map<?, ?>) environment.getSource()).get(entry.getKey())));
				}
				entity = entityQuery.queryOne();
				if (UtilValidate.isEmpty(entity)) {
					return null;
				}
				if (interfaceEntityName == null || interfaceEntityName.isEmpty() || entityName.equals(interfaceEntityName)) {
					return entity;
				} else {
					GenericValue interfaceEntity = null;
					entityQuery = EntityQuery.use(delegator).from(interfaceEntityName).where(EntityCondition.makeCondition(entity.getPrimaryKey().getAllFields()));
					interfaceEntity = entityQuery.queryOne();
					Map<String, Object> jointOneMap = new HashMap<>();
					if (interfaceEntity != null)
						jointOneMap.putAll(interfaceEntity);
					jointOneMap.putAll(entity);
					return jointOneMap;
				}

			} catch (GenericEntityException e) {
				e.printStackTrace();
				return null;
			}
		} else if (operation.equals("list")) {
			List<GenericValue> result = new ArrayList<>();
			EntityQuery entityQuery = EntityQuery.use(delegator).from(entityName).where(inputFieldsMap);
			for (Map.Entry<String, String> entry : relKeyMap.entrySet()) {
				entityQuery.where(EntityCondition.makeCondition(entry.getValue(), EntityOperator.EQUALS, ((Map<?, ?>) environment.getSource()).get(entry.getKey())));
			}
			try {
				result = entityQuery.queryList();
			} catch (GenericEntityException e) {
				return null;
			}
			return result;
		}
		return null;
	}

}
