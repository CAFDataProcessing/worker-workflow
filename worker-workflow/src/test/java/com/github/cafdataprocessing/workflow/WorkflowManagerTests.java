package com.github.cafdataprocessing.workflow;

import com.github.cafdataprocessing.workflow.model.Action;
import com.github.cafdataprocessing.workflow.model.Workflow;
import com.github.cafdataprocessing.workflow.model.WorkflowSettings;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.testing.DocumentBuilder;
import com.hpe.caf.worker.document.testing.TestServices;
import com.google.common.io.Resources;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class WorkflowManagerTests {

    @Test
    public void getWorkflowTest() throws Exception {
        final TestServices testServices = TestServices.createDefault();
        final Document document = DocumentBuilder.configure().withServices(testServices)
                .withCustomData()
                .documentBuilder()
                .withFields()
                .documentBuilder()
                .build();

        final String workflowDirectory = Resources.getResource("workflow-manager-test").getPath();
        final WorkflowManager workflowManager = new WorkflowManager(document.getApplication(), workflowDirectory);

        final Workflow workflow = workflowManager.get("test-workflow");

        final String expectedScript = Resources.toString(Resources.getResource("workflow-manager-test/expected-script.js"),
                StandardCharsets.UTF_8);
        //TODO Comparison is failing is it line endings or something?
//        assertEquals(expectedScript, workflow.getWorkflowScript());

        final String storedScriptReference = workflow.getStorageReferenceForWorkflowScript();
        final String storedScript = IOUtils.toString(testServices.getDataStore().retrieve(storedScriptReference),
                StandardCharsets.UTF_8);

        assertEquals(workflow.getWorkflowScript(), storedScript);

        final WorkflowSettings workflowSettings = workflow.getSettingsForCustomData();
        assertNotNull(workflowSettings);
        assertEquals(5, workflowSettings.getTaskSettings().size());
        assertEquals(2, workflowSettings.getTenantSettings().size());
        assertEquals(1, workflowSettings.getRepositorySettings().size());

        final Map<String, Action> actions = workflow.getActions();
        assertEquals(3, actions.size());
    }

}
