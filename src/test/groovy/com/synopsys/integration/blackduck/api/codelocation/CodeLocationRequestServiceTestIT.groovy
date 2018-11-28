package com.synopsys.integration.blackduck.api.codelocation

import com.synopsys.integration.blackduck.api.generated.component.ProjectRequest
import com.synopsys.integration.blackduck.api.generated.view.CodeLocationView
import com.synopsys.integration.blackduck.api.generated.view.ProjectView
import com.synopsys.integration.blackduck.rest.RestConnectionTestHelper
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory
import com.synopsys.integration.blackduck.service.DryRunUploadResponse
import com.synopsys.integration.blackduck.service.DryRunUploadService
import com.synopsys.integration.blackduck.service.ProjectService
import com.synopsys.integration.blackduck.service.model.ProjectRequestBuilder
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper
import com.synopsys.integration.log.IntLogger
import com.synopsys.integration.log.LogLevel
import com.synopsys.integration.log.PrintStreamIntLogger
import com.synopsys.integration.rest.exception.IntegrationRestException
import org.apache.commons.lang3.StringUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.*

@Tag("integration")
class CodeLocationRequestServiceTestIT {
    private static final RestConnectionTestHelper restConnectionTestHelper = new RestConnectionTestHelper();

    private IntLogger logger = new PrintStreamIntLogger(System.out, LogLevel.INFO)

    private static File dryRunFile;

    @BeforeAll
    public static void init() {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader()
        dryRunFile = new File(classLoader.getResource('dryRun.json').getFile())
    }

    @AfterEach
    public void testCleanup() {
        BlackDuckServicesFactory services = restConnectionTestHelper.createBlackDuckServicesFactory(logger)
        Optional<ProjectView> project = services.createProjectService().getProjectByName(restConnectionTestHelper.getProperty("TEST_CREATE_PROJECT"))
        if (project.isPresent()) {
            services.createProjectService().deleteProject(project.get())
        }
    }

    @Test
    public void testDryRunUpload() {
        final String projectName = restConnectionTestHelper.getProperty("TEST_CREATE_PROJECT");
        final String versionName = restConnectionTestHelper.getProperty("TEST_CREATE_VERSION");

        BlackDuckServicesFactory services = restConnectionTestHelper.createBlackDuckServicesFactory(logger)
        DryRunUploadService dryRunUploadRequestService = new DryRunUploadService(services.createBlackDuckService(), logger)
        DryRunUploadResponse response = dryRunUploadRequestService.uploadDryRunFile(dryRunFile)
        assertNotNull(response)

        CodeLocationView codeLocationView = services.createCodeLocationService().getCodeLocationById(response.codeLocationId)
        assertNotNull(codeLocationView)
        assertTrue(StringUtils.isBlank(codeLocationView.mappedProjectVersion))

        ProjectRequestBuilder projectBuilder = new ProjectRequestBuilder()
        projectBuilder.setProjectName(projectName)
        projectBuilder.setVersionName(versionName)

        ProjectService projectService = services.createProjectService();
        ProjectRequest projectRequest = projectBuilder.build();

        ProjectVersionWrapper projectVersionWrapper = projectService.syncProjectAndVersion(projectRequest, false);

        services.createCodeLocationService().mapCodeLocation(codeLocationView, projectVersionWrapper.projectVersionView)
        codeLocationView = services.createCodeLocationService().getCodeLocationById(response.codeLocationId)
        assertNotNull(codeLocationView)
        assertTrue(StringUtils.isNotBlank(codeLocationView.mappedProjectVersion))

        services.createCodeLocationService().unmapCodeLocation(codeLocationView)
        codeLocationView = services.createCodeLocationService().getCodeLocationById(response.codeLocationId)
        assertNotNull(codeLocationView)
        assertTrue(StringUtils.isBlank(codeLocationView.mappedProjectVersion))

        services.createCodeLocationService().deleteCodeLocation(codeLocationView)
        try {
            services.createCodeLocationService().getCodeLocationById(response.codeLocationId)
            // TODO: Expects exception. integration-rest no longer throws an exception by default
            fail('This should have thrown an exception')
        } catch (IntegrationRestException e) {
            assertEquals(404, e.getHttpStatusCode())
        }
    }

}
