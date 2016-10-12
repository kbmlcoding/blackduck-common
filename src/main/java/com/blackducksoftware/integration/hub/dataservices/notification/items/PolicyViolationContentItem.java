/*******************************************************************************
 * Copyright (C) 2016 Black Duck Software, Inc.
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
 *******************************************************************************/
package com.blackducksoftware.integration.hub.dataservices.notification.items;

import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;

import com.blackducksoftware.integration.hub.api.policy.PolicyRule;
import com.blackducksoftware.integration.hub.api.project.ProjectVersion;

public class PolicyViolationContentItem extends NotificationContentItem {

	private final List<PolicyRule> policyRuleList;

	public PolicyViolationContentItem(final Date createdAt, final ProjectVersion projectVersion,
			final String componentName,
			final String componentVersion, final String componentVersionUrl,
			final List<PolicyRule> policyRuleList) throws URISyntaxException {
		super(createdAt, projectVersion, componentName, componentVersion, componentVersionUrl);
		this.policyRuleList = policyRuleList;
	}

	public List<PolicyRule> getPolicyRuleList() {
		return policyRuleList;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("PolicyViolationContentItem [projectVersion=");
		builder.append(getProjectVersion());
		builder.append(", componentName=");
		builder.append(getComponentName());
		builder.append(", componentVersion=");
		builder.append(getComponentVersion());
		builder.append(", componentVersionUrl=");
		builder.append(getComponentVersionUrl());
		builder.append(", policyRuleList=");
		builder.append(policyRuleList);
		builder.append("]");
		return builder.toString();
	}

}
