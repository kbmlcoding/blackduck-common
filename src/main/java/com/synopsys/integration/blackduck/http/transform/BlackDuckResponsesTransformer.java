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
package com.synopsys.integration.blackduck.http.transform;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.synopsys.integration.blackduck.api.core.BlackDuckResponse;
import com.synopsys.integration.blackduck.exception.BlackDuckIntegrationException;
import com.synopsys.integration.blackduck.http.BlackDuckPageResponse;
import com.synopsys.integration.blackduck.http.PagedRequest;
import com.synopsys.integration.blackduck.http.client.BlackDuckHttpClient;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.rest.request.Request;
import com.synopsys.integration.rest.response.Response;

public class BlackDuckResponsesTransformer {
    private final BlackDuckHttpClient blackDuckHttpClient;
    private final BlackDuckJsonTransformer blackDuckJsonTransformer;

    public BlackDuckResponsesTransformer(BlackDuckHttpClient blackDuckHttpClient, BlackDuckJsonTransformer blackDuckJsonTransformer) {
        this.blackDuckHttpClient = blackDuckHttpClient;
        this.blackDuckJsonTransformer = blackDuckJsonTransformer;
    }

    public <T extends BlackDuckResponse> BlackDuckPageResponse<T> getSomeMatchingResponses(PagedRequest pagedRequest, Class<T> clazz, Predicate<T> predicate, int totalLimit) throws IntegrationException {
        return getInternalMatchingResponse(pagedRequest, clazz, totalLimit, predicate);
    }

    public <T extends BlackDuckResponse> BlackDuckPageResponse<T> getAllResponses(PagedRequest pagedRequest, Class<T> clazz) throws IntegrationException {
        return getInternalMatchingResponse(pagedRequest, clazz, Integer.MAX_VALUE, alwaysTrue());
    }

    public <T extends BlackDuckResponse> BlackDuckPageResponse<T> getSomeResponses(PagedRequest pagedRequest, Class<T> clazz, int totalLimit) throws IntegrationException {
        return getInternalMatchingResponse(pagedRequest, clazz, totalLimit, alwaysTrue());
    }

    public <T extends BlackDuckResponse> BlackDuckPageResponse<T> getOnePageOfResponses(PagedRequest pagedRequest, Class<T> clazz) throws IntegrationException {
        return getInternalMatchingResponse(pagedRequest, clazz, pagedRequest.getLimit(), alwaysTrue());
    }

    private <T extends BlackDuckResponse> Predicate<T> alwaysTrue() {
        return (blackDuckResponse) -> true;
    }

    private <T extends BlackDuckResponse> BlackDuckPageResponse<T> getInternalMatchingResponse(PagedRequest pagedRequest, Class<T> clazz, int maxToReturn, Predicate<T> predicate) throws IntegrationException {
        List<T> allResponses = new LinkedList<>();
        int totalCount = 0;
        int currentOffset = pagedRequest.getOffset();
        Request request = pagedRequest.createRequest();
        try (Response initialResponse = blackDuckHttpClient.execute(request)) {
            blackDuckHttpClient.throwExceptionForError(initialResponse);
            String initialJsonResponse = initialResponse.getContentString();
            BlackDuckPageResponse<T> blackDuckPageResponse = blackDuckJsonTransformer.getResponses(initialJsonResponse, clazz);

            allResponses.addAll(this.matchPredicate(blackDuckPageResponse, predicate));

            totalCount = blackDuckPageResponse.getTotalCount();
            int totalItemsToRetrieve = Math.min(totalCount, maxToReturn);

            while (allResponses.size() < totalItemsToRetrieve && currentOffset < totalCount) {
                currentOffset += pagedRequest.getLimit();
                PagedRequest offsetPagedRequest = new PagedRequest(pagedRequest.getRequestBuilder(), currentOffset, pagedRequest.getLimit());
                request = offsetPagedRequest.createRequest();
                try (Response response = blackDuckHttpClient.execute(request)) {
                    blackDuckHttpClient.throwExceptionForError(response);
                    String jsonResponse = response.getContentString();
                    blackDuckPageResponse = blackDuckJsonTransformer.getResponses(jsonResponse, clazz);
                    allResponses.addAll(this.matchPredicate(blackDuckPageResponse, predicate));
                } catch (IOException e) {
                    throw new BlackDuckIntegrationException(e);
                }
            }

            allResponses = onlyReturnMaxRequested(maxToReturn, allResponses);
            return new BlackDuckPageResponse<>(totalCount, allResponses);
        } catch (IOException e) {
            throw new BlackDuckIntegrationException(e.getMessage(), e);
        }
    }

    @NotNull
    private <T extends BlackDuckResponse> List<T> onlyReturnMaxRequested(int maxToReturn, List<T> allResponses) {
        return allResponses.stream().limit(maxToReturn).collect(Collectors.toList());
    }

    private <T extends BlackDuckResponse> List<T> matchPredicate(BlackDuckPageResponse<T> blackDuckPageResponse, Predicate<T> predicate) {
        return blackDuckPageResponse
                   .getItems()
                   .stream()
                   .filter(predicate)
                   .collect(Collectors.toList());
    }

}
