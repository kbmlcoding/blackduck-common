/*
 * blackduck-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.blackduck.http.client.cache;

import java.io.IOException;
import java.util.Optional;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;

import com.synopsys.integration.blackduck.http.client.BlackDuckHttpClient;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.rest.HttpMethod;
import com.synopsys.integration.rest.HttpUrl;
import com.synopsys.integration.rest.proxy.ProxyInfo;
import com.synopsys.integration.rest.request.Request;
import com.synopsys.integration.rest.response.ErrorResponse;
import com.synopsys.integration.rest.response.Response;

public class CachingHttpClient implements BlackDuckHttpClient {
    private final BlackDuckHttpClient blackDuckHttpClient;
    private final LRUMap<Request, Response> cache;

    public CachingHttpClient(BlackDuckHttpClient blackDuckHttpClient) {
        this.blackDuckHttpClient = blackDuckHttpClient;
        this.cache = new LRUMap<>(1000);
    }

    public void emptyCache() {
        cache.clear();
    }

    @Override
    public Response execute(Request request) throws IntegrationException {
        if (HttpMethod.GET == request.getMethod() && cache.containsKey(request)) {
            return cache.get(request);
        }
        Response response = blackDuckHttpClient.execute(request);

        // the usage of the response will determine whether or not it is cached, because we can only cache responses IFF they are retrieved by string content
        CacheableResponse cacheableResponse = new CacheableResponse(request, response, cache);
        return cacheableResponse;
    }

    @Override
    public Optional<Response> executeGetRequestIfModifiedSince(Request getRequest, long timeToCheck) throws IntegrationException, IOException {
        return blackDuckHttpClient.executeGetRequestIfModifiedSince(getRequest, timeToCheck);
    }

    @Override
    public Response attemptAuthentication() throws IntegrationException {
        return blackDuckHttpClient.attemptAuthentication();
    }

    @Override
    public boolean isAlreadyAuthenticated(HttpUriRequest request) {
        return blackDuckHttpClient.isAlreadyAuthenticated(request);
    }

    @Override
    public Optional<ErrorResponse> extractErrorResponse(String responseContent) {
        return blackDuckHttpClient.extractErrorResponse(responseContent);
    }

    @Override
    public void handleErrorResponse(HttpUriRequest request, Response response) {
        blackDuckHttpClient.handleErrorResponse(request, response);
    }

    @Override
    public void throwExceptionForError(Response response) throws IntegrationException {
        blackDuckHttpClient.throwExceptionForError(response);
    }

    @Override
    public HttpUrl getBaseUrl() {
        return blackDuckHttpClient.getBaseUrl();
    }

    @Override
    public String getUserAgentString() {
        return blackDuckHttpClient.getUserAgentString();
    }

    @Override
    public HttpClientBuilder getHttpClientBuilder() {
        return blackDuckHttpClient.getHttpClientBuilder();
    }

    @Override
    public int getTimeoutInSeconds() {
        return blackDuckHttpClient.getTimeoutInSeconds();
    }

    @Override
    public boolean isAlwaysTrustServerCertificate() {
        return blackDuckHttpClient.isAlwaysTrustServerCertificate();
    }

    @Override
    public ProxyInfo getProxyInfo() {
        return blackDuckHttpClient.getProxyInfo();
    }

    @Override
    public IntLogger getLogger() {
        return blackDuckHttpClient.getLogger();
    }

}
