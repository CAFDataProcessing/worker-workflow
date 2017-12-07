# worker-workflow-container

## Summary

A Docker container for the Workflow Worker Java application. Specific detail on the functioning of the Workflow Worker can be found [here](../worker-workflow).

## Input Task & Response

The input task this worker receives should be a composite document task as defined in the worker-document project [here](https://github.com/CAFDataProcessing/worker-document/tree/develop/worker-document-shared#composite-document-handling). The response should match the worker response from that page also. 

### Input Task Custom Data

Properties specific to this worker that should be provided on the custom data of the input task are described below;

#### outputPartialReference

The data store partial reference to use when storing the generated workflow.

#### projectId

The project ID that the workflow to transform is associated with.

#### workflowId

The ID of the workflow to transform.

### Output Custom Data

The worker adds the following properties to the custom data of its task response.

#### postProcessingScript

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
| maxBatchSize  |  `CAF_WORKER_MAX_BATCH_SIZE`          |             |
| maxBatchTime  |  `CAF_WORKER_MAX_BATCH_TIME`          |             |


#### WorkflowWorkerConfiguration

| Property | Checked Environment Variables                        | Default               |
|----------|------------------------------------------------------|-----------------------|
| processingApiUrl   |  `CAF_WORKFLOW_WORKER_PROCESSING_API_URL`  |                       |
| workflowCachePeriod | `CAF_WORKFLOW_WORKER_CACHE_PERIOD`        | PT5M                  |