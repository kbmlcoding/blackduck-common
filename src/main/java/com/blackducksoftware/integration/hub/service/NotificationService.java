/**
 * hub-common
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
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
package com.blackducksoftware.integration.hub.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.api.UriSingleResponse;
import com.blackducksoftware.integration.hub.api.core.HubPathMultipleResponses;
import com.blackducksoftware.integration.hub.api.core.HubResponse;
import com.blackducksoftware.integration.hub.api.generated.discovery.ApiDiscovery;
import com.blackducksoftware.integration.hub.api.generated.enumeration.NotificationType;
import com.blackducksoftware.integration.hub.api.generated.view.NotificationUserView;
import com.blackducksoftware.integration.hub.api.generated.view.NotificationView;
import com.blackducksoftware.integration.hub.api.generated.view.UserView;
import com.blackducksoftware.integration.hub.api.view.CommonNotificationState;
import com.blackducksoftware.integration.hub.notification.NotificationResults;
import com.blackducksoftware.integration.hub.notification.NotificationViewResult;
import com.blackducksoftware.integration.hub.notification.NotificationViewResults;
import com.blackducksoftware.integration.hub.notification.content.LicenseLimitNotificationContent;
import com.blackducksoftware.integration.hub.notification.content.NotificationContent;
import com.blackducksoftware.integration.hub.notification.content.PolicyOverrideNotificationContent;
import com.blackducksoftware.integration.hub.notification.content.RuleViolationClearedNotificationContent;
import com.blackducksoftware.integration.hub.notification.content.RuleViolationNotificationContent;
import com.blackducksoftware.integration.hub.notification.content.VulnerabilityNotificationContent;
import com.blackducksoftware.integration.hub.notification.content.detail.NotificationContentDetail;
import com.blackducksoftware.integration.hub.notification.content.detail.NotificationContentDetailFactory;
import com.blackducksoftware.integration.hub.service.bucket.HubBucket;
import com.blackducksoftware.integration.hub.service.bucket.HubBucketService;
import com.blackducksoftware.integration.hub.service.model.RequestFactory;
import com.blackducksoftware.integration.rest.RestConstants;
import com.blackducksoftware.integration.rest.request.Request;
import com.google.gson.JsonObject;

public class NotificationService extends DataService {
    private final HubBucketService hubBucketService;

    public NotificationService(final HubService hubService, final HubBucketService hubBucketService) {
        super(hubService);
        this.hubBucketService = hubBucketService;
    }

    public NotificationResults getAllNotificationResults(final Date startDate, final Date endDate) throws IntegrationException {
        final NotificationViewResults notificationViewResults = getAllNotificationViewResults(startDate, endDate);
        final NotificationResults results = createNotificationResults(notificationViewResults);
        return results;
    }

    public NotificationResults getAllUserNotificationResults(final UserView user, final Date startDate, final Date endDate) throws IntegrationException {
        final NotificationViewResults notificationViewResults = getAllNotificationViewResults(user, startDate, endDate);
        final NotificationResults results = createNotificationResults(notificationViewResults);
        return results;
    }

    public List<NotificationView> getAllNotifications(final Date startDate, final Date endDate) throws IntegrationException {
        final Request.Builder requestBuilder = createNotificationRequestBuilder(startDate, endDate);
        final HubPathMultipleResponses<NotificationView> notificationLinkResponse = new HubPathMultipleResponses<>(ApiDiscovery.NOTIFICATIONS_LINK, NotificationView.class);
        final List<NotificationView> allNotificationItems = hubService.getResponses(notificationLinkResponse, requestBuilder, true);
        return allNotificationItems;
    }

    public List<NotificationUserView> getAllUserNotifications(final UserView user, final Date startDate, final Date endDate) throws IntegrationException {
        final Request.Builder requestBuilder = createNotificationRequestBuilder(startDate, endDate);
        final String userNotificationsUri = hubService.getFirstLink(user, UserView.NOTIFICATIONS_LINK);
        requestBuilder.uri(userNotificationsUri);

        final List<NotificationUserView> allUserNotificationItems = hubService.getResponses(requestBuilder, NotificationUserView.class, true);
        return allUserNotificationItems;
    }

    /**
     * @return The java.util.Date of the most recent notification. If there are no notifications, the current date will be returned. This can set an initial start time window for all future notifications.
     * @throws IntegrationException
     */
    public Date getLatestNotificationDate() throws IntegrationException {
        final Request.Builder requestBuilder = RequestFactory.createCommonGetRequestBuilder(1, RequestFactory.DEFAULT_OFFSET);
        final List<NotificationView> notifications = hubService.getResponses(ApiDiscovery.NOTIFICATIONS_LINK_RESPONSE, requestBuilder, false);
        if (notifications.size() == 1) {
            return notifications.get(0).createdAt;
        } else {
            return new Date();
        }
    }

    public NotificationViewResults getAllNotificationViewResults(final Date startDate, final Date endDate) throws IntegrationException {
        final List<NotificationView> allNotificationItems = getAllNotifications(startDate, endDate);
        final List<CommonNotificationState> commonNotifications = getCommonNotifications(allNotificationItems);
        return createNotificationViewResults(commonNotifications);
    }

    public NotificationViewResults getAllNotificationViewResults(final UserView user, final Date startDate, final Date endDate) throws IntegrationException {
        final List<NotificationUserView> allNotificationItems = getAllUserNotifications(user, startDate, endDate);
        final List<CommonNotificationState> commonNotifications = getCommonUserNotifications(allNotificationItems);
        return createNotificationViewResults(commonNotifications);
    }

    private NotificationViewResults createNotificationViewResults(final List<CommonNotificationState> commonNotifications) {
        if (commonNotifications == null || commonNotifications.isEmpty()) {
            return new NotificationViewResults(Collections.emptyList(), Optional.empty(), Optional.empty());
        }

        final SimpleDateFormat sdf = new SimpleDateFormat(RestConstants.JSON_DATE_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        // we know that the first notification in the list is the most current
        final Date latestCreatedAtDate = commonNotifications.get(0).getCreatedAt();
        final String latestCreatedAtString = sdf.format(latestCreatedAtDate);
        final NotificationContentDetailFactory detailFactory = new NotificationContentDetailFactory();
        final List<NotificationViewResult> notificationViewResultList = new ArrayList<>();
        commonNotifications.forEach(commonNotification -> {
            final NotificationViewResult result = new NotificationViewResult(commonNotification, detailFactory.generateNotificationContentDetails(commonNotification));
            notificationViewResultList.add(result);
        });
        return new NotificationViewResults(notificationViewResultList, Optional.of(latestCreatedAtDate), Optional.of(latestCreatedAtString));
    }

    public List<CommonNotificationState> getCommonNotifications(final List<NotificationView> notificationViews) {
        final List<CommonNotificationState> commonStates = notificationViews
                .stream()
                .map(view -> {
                    final Optional<NotificationContent> notificationContent = parseNotificationContent(view.json, view.type);
                    return new CommonNotificationState(view, notificationContent.orElse(null));
                }).collect(Collectors.toList());

        return commonStates;
    }

    public List<CommonNotificationState> getCommonUserNotifications(final List<NotificationUserView> notificationUserViews) {
        final List<CommonNotificationState> commonStates = notificationUserViews
                .stream()
                .map(view -> {
                    final Optional<NotificationContent> notificationContent = parseNotificationContent(view.json, view.type);
                    return new CommonNotificationState(view, notificationContent.orElse(null));
                }).collect(Collectors.toList());

        return commonStates;
    }

    public List<UriSingleResponse<? extends HubResponse>> getAllLinks(final List<NotificationContentDetail> details) {
        final List<UriSingleResponse<? extends HubResponse>> uriResponses = new ArrayList<>();
        details.forEach(detail -> {
            uriResponses.addAll(detail.getPresentLinks());
        });

        return uriResponses;
    }

    private NotificationResults createNotificationResults(final NotificationViewResults notificationViewResults) throws IntegrationException {
        final NotificationResults results;

        final List<UriSingleResponse<? extends HubResponse>> uriResponseList = new ArrayList<>();
        notificationViewResults.getResultList().forEach(notificationViewResult -> {
            uriResponseList.addAll(getAllLinks(notificationViewResult.getNotificationContentDetails()));
        });
        final HubBucket bucket = hubBucketService.startTheBucket(uriResponseList);

        results = new NotificationResults(notificationViewResults, bucket);
        return results;
    }

    private Optional<NotificationContent> parseNotificationContent(final String notificationJson, final NotificationType type) {
        final JsonObject jsonObject = hubService.getJsonParser().parse(notificationJson).getAsJsonObject();
        if (type == NotificationType.LICENSE_LIMIT) {
            return Optional.of(hubService.getGson().fromJson(jsonObject.get("content"), LicenseLimitNotificationContent.class));
        } else if (type == NotificationType.POLICY_OVERRIDE) {
            return Optional.of(hubService.getGson().fromJson(jsonObject.get("content"), PolicyOverrideNotificationContent.class));
        } else if (type == NotificationType.RULE_VIOLATION) {
            return Optional.of(hubService.getGson().fromJson(jsonObject.get("content"), RuleViolationNotificationContent.class));
        } else if (type == NotificationType.RULE_VIOLATION_CLEARED) {
            return Optional.of(hubService.getGson().fromJson(jsonObject.get("content"), RuleViolationClearedNotificationContent.class));
        } else if (type == NotificationType.VULNERABILITY) {
            return Optional.of(hubService.getGson().fromJson(jsonObject.get("content"), VulnerabilityNotificationContent.class));
        }
        return Optional.empty();
    }

    private Request.Builder createNotificationRequestBuilder(final Date startDate, final Date endDate) {
        final SimpleDateFormat sdf = new SimpleDateFormat(RestConstants.JSON_DATE_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String startDateString = sdf.format(startDate);
        final String endDateString = sdf.format(endDate);

        return RequestFactory.createCommonGetRequestBuilder().addQueryParameter("startDate", startDateString).addQueryParameter("endDate", endDateString);
    }

}
