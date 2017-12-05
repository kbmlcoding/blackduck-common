/**
 * Hub Common
 *
 * Copyright (C) 2017 Black Duck Software, Inc.
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
package com.blackducksoftware.integration.hub.dataservice.license;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.api.component.ComponentRequestService;
import com.blackducksoftware.integration.hub.api.license.LicenseRequestService;
import com.blackducksoftware.integration.hub.model.response.ComponentSearchResultResponse;
import com.blackducksoftware.integration.hub.model.view.ComplexLicenseView;
import com.blackducksoftware.integration.hub.model.view.ComponentVersionView;
import com.blackducksoftware.integration.hub.model.view.LicenseView;
import com.blackducksoftware.integration.hub.model.view.components.VersionBomLicenseView;
import com.blackducksoftware.integration.log.IntLogger;

public class LicenseDataService {

    private final ComponentRequestService componentRequestService;
    private final LicenseRequestService licenseRequestService;

    public LicenseDataService(final IntLogger logger, final ComponentRequestService componentRequestService, final LicenseRequestService licenseRequestService) {
        this.componentRequestService = componentRequestService;
        this.licenseRequestService = licenseRequestService;
    }

    public ComplexLicenseView getComplexLicenseItemFromComponent(final String namespace, final String groupId, final String artifactId, final String version) throws IntegrationException {
        final ComponentSearchResultResponse component = componentRequestService.getExactComponentMatch(namespace, groupId, artifactId, version);
        final String versionUrl = component.version;
        final ComponentVersionView componentVersion = componentRequestService.getItem(versionUrl, ComponentVersionView.class);
        return componentVersion.license;
    }

    public LicenseView getLicenseView(final VersionBomLicenseView versionBomLicenseView) throws IntegrationException {
        final String licenseUrl = versionBomLicenseView.license;
        if (licenseUrl == null) {
            return null;
        }
        final LicenseView licenseView = licenseRequestService.getItem(licenseUrl, LicenseView.class);
        return licenseView;
    }

    public String getLicenseText(final LicenseView licenseView) throws IntegrationException {
        final String licenseText = licenseRequestService.getLicenseText(licenseView);
        return licenseText;
    }

}
