# worker-workflow-container

## Summary

A Docker container for the Workflow Worker Java application. Specific detail on the functioning of the Workflow Worker can be found [here](../worker-workflow).

## Input Task & Response

The input task this worker receives should be a composite document task as defined in the worker-document project [here](https://github.com/CAFDataProcessing/worker-document/tree/develop/worker-document-shared#composite-document-handling). The response should match the worker response from that page also. 

### Input Task Custom Data

Properties specific to this worker that can be provided on the custom data of the input task are described below;

#### outputPartialReference

The data store partial reference to use when storing the generated workflow. This is optional.

#### tenantId

A tenant ID that can be used in the evaluation of the workflow. This is required.

#### workflowName

The name of the workflow to script to use. This is required.

#### settingsServiceLastUpdateTimeMillis

The last time (in milliseconds, of type `long`) the Settings Service was updated, for example `1630591453107`. When a setting is fetched
from the Settings Service, the Workflow Worker caches it for a maximum time of 5 minutes. This optional property can be supplied in order
to force a refresh of the Settings Service cache, in the scenario where a setting has been updated after it has been cached and before
the normal cache expiration time.

### Output Task Scripts

The worker adds the following script to the document task of its response.

#### workflow.js

The storage reference of the workflow script so that the next worker may execute the workflow against the document after processing is complete.

## Container Configuration

The worker container reads its configuration from environment variables. A listing of the RabbitMQ and Storage properties is available [here](https://github.com/WorkerFramework/worker-framework/tree/develop/worker-default-configs).

Further Workflow Worker container configuration that can be controlled through environment variables is described below.

### DocumentWorkerConfiguration

| Property | Checked Environment Variables | Default               |
|----------|-------------------------------|-----------------------|
| outputQueue   |  `CAF_WORKER_OUTPUT_QUEUE`                  |   |
|               |   `CAF_WORKER_BASE_QUEUE_NAME` with '-out' appended to the value if present     |             |
|               |  `CAF_WORKER_NAME` with '-out' appended to the value if present                 |             |
| failureQueue  |  `CAF_WORKER_FAILURE_QUEUE`          |             |
| threads       |  `CAF_WORKFLOW_WORKER_THREADS`          |      1       |
|               |  `CAF_WORKER_THREADS`          |             |


#### WorkflowWorkerConfiguration

| Property | Description | Checked Environment Variables                        | Default               |
|----------|--------|------------------------------------------------------|-----------------------|
| workflowsDirectory | The location within the container that the workflow scripts can be found. | CAF_WORKFLOW_WORKER_WORKFLOW_DIRECTORY | undefined |
| contextScriptFilePath | The location within the container that the context script can be found. | CAF_WORKFLOW_WORKER_CONTEXT_JAVASCRIPT_PATH | undefined |
