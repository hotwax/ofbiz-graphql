package org.apache.ofbiz.graphql.fetcher;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import org.apache.ofbiz.base.util.UtilValidate;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.util.EntityQuery;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.servlet.context.DefaultGraphQLServletContext;

public class EntityDataFetcher implements DataFetcher<Object> {

	@Override
	public Object get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {
		String productId = dataFetchingEnvironment.getArgument("id");
		DefaultGraphQLServletContext context = dataFetchingEnvironment.getContext();
		HttpServletRequest request = context.getHttpServletRequest();
		Delegator delegator = (Delegator)request.getAttribute("delegator");
		if(UtilValidate.isEmpty(delegator)) {
			ServletContext servContext = request.getServletContext();
			delegator = (Delegator)servContext.getAttribute("delegator");
		}
		GenericValue product = EntityQuery.use(delegator).from("Product").where("productId", productId).cache().queryOne();
		return product;
	}

}
