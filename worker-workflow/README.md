# worker-workflow

## Summary

The Workflow Worker is used to create a script representing a workflow that can be executed against a document to send the document to another worker to have an action performed on it.

## Implementation

The Workflow Worker behaviour for a task message is as follows;

- Retrieve the workflow referenced by document in task message by communicating with external processing API.
- Transform the workflow to a JavaScript script that the document can be evaluated against.
- Resolves customData sources to obtain their values and replace them in the workflow. For example, the customData field source 'projectId' can be resolved by the worker and replaced in the workflow with the actual projectId for the tenant.
- Evaluate the document against the workflow script to mark the document with the next suitable action to perform on the document e.g. send to Language Detection Worker, and modify the response options on the document to direct it to the appropriate queue.
- Store the transformed workflow script on the document as a script to be executed by Document Workers during post processing. The intention is that once the next worker has completed its action it will evaluate the document against the workflow again and determine the next action to execute, sending the document to that next worker until all actions on the workflow are completed.

Details of the actions supported by this worker can be found [here](../workflow-to-js-transformer/README.md).

## Configuration

The configuration properties for use with this worker can be seen in the container for the worker, [here](../worker-workflow-container).

## Health Check

The worker health check verifies that the worker can communicate with the configured processing API by calling the health check method on the API.

## Failure Modes

The main places where the worker can fail are;

- Configuration errors: these will manifest on startup. Check the logs for clues, and double check your configuration settings.
