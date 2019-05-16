package com.github.cafdataprocessing.workflow;

import com.google.common.io.Resources;
import com.hpe.caf.worker.document.model.Document;
import com.hpe.caf.worker.document.model.Failure;
import com.hpe.caf.worker.document.testing.DocumentBuilder;
import com.hpe.caf.worker.document.testing.TestServices;
import com.microfocus.darwin.settings.client.SettingsApi;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class SettingsManagerTests {

    @Test
    public void myTest() throws Exception {

        final SettingsApi settingsApi = mock(SettingsApi.class);

        final SettingsManager settingsManager = new SettingsManager(settingsApi);
        settingsManager.applySettingsCustomData(workflowSettings, document);

    }

//    @Test
//    public void myTest() throws Exception {
//
//        final WorkflowWorkerConfiguration workflowWorkerConfiguration = new WorkflowWorkerConfiguration();
//        workflowWorkerConfiguration.setWorkflowsDirectory(Resources.getResource("workflow-worker-test").getPath());
//
//        final SettingsManager settingsManager = mock(SettingsManager.class);
//
//        final Document document = DocumentBuilder.configure().withServices(TestServices.createDefault())
//                .withReference("test-document")
//                .withCustomData()
//                    .add("workflowName", "sample-workflow")
//                .documentBuilder()
//                .withFields()
//                .documentBuilder()
//                .build();
//
//        final WorkflowWorker workflowWorker = new WorkflowWorker(document.getApplication(), workflowWorkerConfiguration,
//                settingsManager);
//
//        workflowWorker.processDocument(document);
//
//        assertTrue(failuresToString(document), document.getFailures().isEmpty());
//        assertEquals("", document.getCustomData("CAF_WORKFLOW_SETTINGS"));
//    }
//
//    private String failuresToString(final Document document){
//        final StringBuilder stringBuilder = new StringBuilder();
//        for(final Failure failure: document.getFailures()){
//            stringBuilder.append(failure.getFailureMessage());
//            stringBuilder.append("\n");
//        }
//        return stringBuilder.toString();
//    }
}
