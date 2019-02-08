# worker-workflow

## Summary

The Workflow Worker is used to load a script (from file) representing a workflow that can be executed against a document to send the document to another worker to have an action performed on it.
The workflow scripts and their settings have to be saved in the [src/main/docker/workflows](../worker-workflow-container/src/main/docker/workflows) folder.
In the same location, you can find a sample workflow and a sample settings file.

## Implementation

The Workflow Worker behaviour for a task message is as follows;

- Retrieve the workflow referenced by document in task message.
- Resolves customData sources to obtain their values and provide them to the workflow. For example, the customData field source 'projectId' can be resolved by the worker and provided to the workflow.
- Evaluate the document against the workflow script to mark the document with the next suitable action to perform on the document e.g. send to Language Detection Worker, and modify the response options on the document to direct it to the appropriate queue.
- Store the transformed workflow script on the document as a script to be executed by Document Workers during post processing. The intention is that once the next worker has completed its action it will evaluate the document against the workflow again and determine the next action to execute, sending the document to that next worker until all actions on the workflow are completed.

## Configuration

The configuration properties for use with this worker can be seen in the container for the worker, [here](../worker-workflow-container).

## Health Check

The worker health check verifies that the worker can communicate with the configured processing API by calling the health check method on the API.

## Failure Modes

The main places where the worker can fail are;

- Configuration errors: these will manifest on startup. Check the logs for clues, and double check your configuration settings.

## Settings

The settings js file may contain information about taskSettings, repositorySettings and tenantSettings.

*taskSettings*

A source of taskSettings may be specified to indicate that a value for a property should come from the input message's customdata.

```
"taskSettings": ["tenantId", "ee.operationmode"]
```

*repositorySettings*

A source of repositorySettings can be used to indicate that the worker should call out to get the effective repository configuration values for the key provided. 
Repository settings also specify where the repository id can be obtained on the document to make the request. The possible supported source values for the repository id are `CUSTOMDATA` or `FIELD`, the key then indicates the field name to check.

```
"repositorySettings": {
        "ee.grammarmap": {
            "source": "FIELD",
            "key": "REPOSITORY_ID"
            }
    }
```

*tenantSettings*

They have a structure similar to the taskSettings.

```
"tenantSettings": ["tenantId", "ee.operationmode"]
```
