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
import javax.inject.Inject;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import org.apache.ofbiz.base.util.FileUtil;
import graphql.schema.GraphQLSchema;
import graphql.schema.StaticDataFetcher;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.servlet.SimpleGraphQLHttpServlet;

public class GraphQLServletRegistrationServletContextListener implements ServletContextListener {

    private GraphQLSchema schema;

    public GraphQLServletRegistrationServletContextListener() {}

    @Inject
    public GraphQLServletRegistrationServletContextListener(final GraphQLSchema schema) {
        this.schema = schema;
    }

    @Override
    public void contextInitialized(final ServletContextEvent event) {
		System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");

		SchemaParser schemaParser = new SchemaParser();
		File schemaFile = FileUtil.getFile("component://graphql/config/schema.graphqls");
		TypeDefinitionRegistry typeDefinitionRegistry = schemaParser.parse(schemaFile);

		RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .type("Query", builder -> builder.dataFetcher("hello", new StaticDataFetcher("world")))
                .build();

		SchemaGenerator schemaGenerator = new SchemaGenerator();
		schema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring);

		final SimpleGraphQLHttpServlet servlet = SimpleGraphQLHttpServlet.newBuilder(schema).build();

		final ServletRegistration.Dynamic dynamicGraphQLServlet = event.getServletContext()
				.addServlet("GraphQLEndpoint", servlet);
		dynamicGraphQLServlet.addMapping("/*");
	}

    @Override
    public void contextDestroyed(final ServletContextEvent event) {}

}
