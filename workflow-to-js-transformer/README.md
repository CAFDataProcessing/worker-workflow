# workflow-to-js-transformer

## Summary

This Java library offers functionality to retrieve a specified workflow (via an external processing API) and transform the workflow including its actions to a JavaScript representation that can be executed against Document Worker documents, adding fields to indicate the actions that have executed, are executing, updating the queue that the document should be sent to and passing any psot processing script that is set to execute.