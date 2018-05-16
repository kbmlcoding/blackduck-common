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
package com.blackducksoftware.integration.hub.notification.content.detail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.blackducksoftware.integration.hub.api.view.CommonNotificationState;
import com.blackducksoftware.integration.hub.notification.content.LicenseLimitNotificationContent;
import com.blackducksoftware.integration.hub.notification.content.NotificationContent;
import com.blackducksoftware.integration.hub.notification.content.PolicyOverrideNotificationContent;
import com.blackducksoftware.integration.hub.notification.content.RuleViolationClearedNotificationContent;
import com.blackducksoftware.integration.hub.notification.content.RuleViolationNotificationContent;
import com.blackducksoftware.integration.hub.notification.content.VulnerabilityNotificationContent;

public class ContentDetailCollector {
    Map<Class<? extends NotificationContent>, NotificationDetailFactory> factoryMap;

    public ContentDetailCollector() {
        factoryMap = new HashMap<>();
        factoryMap.put(RuleViolationNotificationContent.class, new RuleViolationDetailFactory());
        factoryMap.put(RuleViolationClearedNotificationContent.class, new RuleViolationClearedDetailFactory());
        factoryMap.put(PolicyOverrideNotificationContent.class, new PolicyOverrideDetailFactory());
        factoryMap.put(VulnerabilityNotificationContent.class, new VulnerabilityDetailFactory());
        factoryMap.put(LicenseLimitNotificationContent.class, new LicenseLimitDetailFactory());
    }

    public List<NotificationContentDetail> collect(final List<CommonNotificationState> commonNotificationStates) {
        if (commonNotificationStates.isEmpty()) {
            return Collections.emptyList();
        }
        final List<NotificationContentDetail> contentDetailList = new ArrayList<>(50);
        commonNotificationStates.stream().map(CommonNotificationState::getContent).forEach(content -> {
            collectDetails(contentDetailList, content);
        });

        return contentDetailList;
    }

    private void collectDetails(final List<NotificationContentDetail> contentDetailList, final NotificationContent notificationContent) {
        final Class<?> key = notificationContent.getClass();
        if (factoryMap.containsKey(key)) {
            final NotificationDetailFactory factory = factoryMap.get(key);
            contentDetailList.addAll(factory.createDetails(notificationContent));
        }
    }
}
