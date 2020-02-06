package org.apache.ofbiz.graphql;

import graphql.ErrorClassification;

public enum GraphQLErrorType implements ErrorClassification {
	InvalidSyntax, ValidationError, DataFetchingException, OperationNotSupported, ExecutionAborted, AuthenticationError

}
