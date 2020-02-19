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
 * under the License.aaaab	
 *******************************************************************************/
package org.apache.ofbiz.graphql.schema;

import static graphql.Scalars.GraphQLBigDecimal;
import static graphql.Scalars.GraphQLBigInteger;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLByte;
import static graphql.Scalars.GraphQLChar;
import static graphql.Scalars.GraphQLFloat;
import static graphql.Scalars.GraphQLID;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLShort;
import static graphql.Scalars.GraphQLString;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import graphql.schema.GraphQLScalarType;

public class GraphQLSchemaUtil {
	
	public static final Map<String, GraphQLScalarType> graphQLScalarTypes = new HashMap<String, GraphQLScalarType>();
	public static final Map<String, String> fieldTypeGraphQLMap = new HashMap<String, String>();
	public static final Map<String, String> javaTypeGraphQLMap = new HashMap<String, String>();

	public static final List<String> graphQLStringTypes = Arrays.asList("String", "ID", "Char");
	public static final List<String> graphQLDateTypes = Arrays.asList("Timestamp");
	public static final List<String> graphQLNumericTypes = Arrays.asList("Int", "Long", "Float", "BigInteger",
			"BigDecimal", "Short");
	public static final List<String> graphQLBoolTypes = Arrays.asList("Boolean");

	static {
		graphQLScalarTypes.put("Int", GraphQLInt);
		graphQLScalarTypes.put("Float", GraphQLFloat);
		graphQLScalarTypes.put("Boolean", GraphQLBoolean);
		graphQLScalarTypes.put("BigInteger", GraphQLBigInteger);
		graphQLScalarTypes.put("Byte", GraphQLByte);
		graphQLScalarTypes.put("Char", GraphQLChar);
		graphQLScalarTypes.put("String", GraphQLString);
		graphQLScalarTypes.put("ID", GraphQLID);
		graphQLScalarTypes.put("BigDecimal", GraphQLBigDecimal);
		graphQLScalarTypes.put("Short", GraphQLShort);

		fieldTypeGraphQLMap.put("id", "ID");
		fieldTypeGraphQLMap.put("indicator", "String");
		fieldTypeGraphQLMap.put("date", "String");
		fieldTypeGraphQLMap.put("id-vlong", "String");
		fieldTypeGraphQLMap.put("description", "String");
		fieldTypeGraphQLMap.put("numeric", "Int"); //
		fieldTypeGraphQLMap.put("long-varchar", "String");
		fieldTypeGraphQLMap.put("id-long", "String");
		fieldTypeGraphQLMap.put("currency-amount", "BigDecimal");
		fieldTypeGraphQLMap.put("value", "value");
		fieldTypeGraphQLMap.put("email", "String");
		fieldTypeGraphQLMap.put("currency-precise", "BigDecimal");
		fieldTypeGraphQLMap.put("very-short", "String");
		fieldTypeGraphQLMap.put("date-time", "Timestamp");
		fieldTypeGraphQLMap.put("credit-card-date", "String");
		fieldTypeGraphQLMap.put("url", "String");
		fieldTypeGraphQLMap.put("credit-card-number", "String");
		fieldTypeGraphQLMap.put("fixed-point", "BigDecimal");
		fieldTypeGraphQLMap.put("name", "String");
		fieldTypeGraphQLMap.put("short-varchar", "String");
		fieldTypeGraphQLMap.put("comment", "String");
		fieldTypeGraphQLMap.put("time", "String");
		fieldTypeGraphQLMap.put("very-long", "String");
		fieldTypeGraphQLMap.put("floating-point", "Float");

		javaTypeGraphQLMap.put("String", "String");
		javaTypeGraphQLMap.put("java.lang.String", "String");
		javaTypeGraphQLMap.put("CharSequence", "String");
		javaTypeGraphQLMap.put("java.lang.CharSequence", "String");
		javaTypeGraphQLMap.put("Date", "String");
		javaTypeGraphQLMap.put("java.sql.Date", "String");
		javaTypeGraphQLMap.put("Time", "String");
		javaTypeGraphQLMap.put("java.sql.Time", "String");
		javaTypeGraphQLMap.put("Timestamp", "Timestamp");
		javaTypeGraphQLMap.put("java.sql.Timestamp", "Timestamp");
		javaTypeGraphQLMap.put("Integer", "Int");
		javaTypeGraphQLMap.put("java.lang.Integer", "Int");
		javaTypeGraphQLMap.put("Long", "Long");
		javaTypeGraphQLMap.put("java.lang.Long", "Long");
		javaTypeGraphQLMap.put("BigInteger", "BigInteger");
		javaTypeGraphQLMap.put("java.math.BigInteger", "BigInteger");
		javaTypeGraphQLMap.put("Float", "Float");
		javaTypeGraphQLMap.put("java.lang.Float", "Float");
		javaTypeGraphQLMap.put("Double", "Float");
		javaTypeGraphQLMap.put("java.lang.Double", "Float");
		javaTypeGraphQLMap.put("BigDecimal", "BigDecimal");
		javaTypeGraphQLMap.put("java.math.BigDecimal", "BigDecimal");
		javaTypeGraphQLMap.put("Boolean", "Boolean");
		javaTypeGraphQLMap.put("java.lang.Boolean", "Boolean");

	}

}
