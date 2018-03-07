# worker-workflow-container

## Summary

A Docker container for the Workflow Worker Java application. Specific detail on the functioning of the Workflow Worker can be found [here](../worker-workflow).

## Input Task & Response

The input task this worker receives should be a composite document task as defined in the worker-document project [here](https://github.com/CAFDataProcessing/worker-document/tree/develop/worker-document-shared#composite-document-handling). The response should match the worker response from that page also. 

### Input Task Custom Data

Properties specific to this worker that can be provided on the custom data of the input task are described below;

#### outputPartialReference

The data store partial reference to use when storing the generated workflow. This is optional.

#### projectId

The project ID that the workflow to transform is associated with. This is required.

#### workflowId

The ID of the workflow to transform. This is required.

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
| processingApiUrl   | URL to a Processing Service that the worker will use to retrieve workflows e.g. http://processing-service:8080/data-processing-service/v1 |  `CAF_WORKFLOW_WORKER_PROCESSING_API_URL`  |                       |
| workflowCachePeriod | The period of time that a transformed workflow script should remain cached after it is created. This should be in IS0-8601 time duration format e.g. PT5M. | `CAF_WORKFLOW_WORKER_CACHE_PERIOD`        | PT5M                  |
