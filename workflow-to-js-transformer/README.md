# workflow-to-js-transformer

## Summary

This Java library offers functionality to retrieve a specified workflow (via an external processing API) and transform the workflow including its actions to a JavaScript representation that can be executed against Document Worker documents, adding fields to indicate the actions that have executed, are executing, updating the queue that the document should be sent to and passing any post processing script that is set to execute.

## Action Types Supported

The types of action supported by the JavaScript workflow are detailed in this section.

### Chained Action Type

This action is used to send a document to a document worker with configured custom data values for that worker. The action will set the response options on the task of the document so that the task is forwarded to the worker specified by the action. Details of the properties that can be set in the settings of this action are listed below.

#### Usage

Actions of this type should have their type ID set to the action type with internal name 'ChainedActionType'.

#### Settings

##### customData

Used to send additional worker-specific data to the worker. This is optional. This should be a map of keys to raw values/objects that are understood by the worker the document is to be sent to.


###### Sources support

Properties under customData may pull their values from another source beyond a raw value passed.

*inlineJson*

A source of inlineJson may be specified to indicate that a value for a property has been passed as a JSON structure that should be
passed as a string representation in customData. When source is 'inlineJson' a 'data' property should also be set containing the
JSON object that should be set as the string value.

```
"customData": {
     "myProperty" : {
        "source" : "inlineJson",
        "data": {
            "extractMetadata": true,
            "extractContent": true
        }
     }
 }
```

Which would be output as;

```
"customData": {
     "myProperty" : '{"extractMetadata":true,"extractContent":true}'
```

*projectId*

A source of projectId may be specified to indicate that a value for a property should come from a `projectId` parameter that will
 be available during workflow transformation.

```
"customData": {
     "myProperty" : { "source" : "projectId"}
 }
```

Which given a projectId of "myProjectId" would be output after transformation as;

```
"customData": {
    "myProperty": 'myProjectId'
}
```

*tenantId*

A source of tenantId may be specified to indicate that a value for a property should come from a `tenantId` parameter that will
 be available during workflow transformation.

```
"customData": {
     "myProperty" : { "source" : "tenantId"}
 }
```

Which given a projectId of "1234" would be output after transformation as;

```
"customData": {
    "myProperty": '1234'
}
```

*tenantData*

A source of tenantData may be specified to indicate that the value of the property should be retrieved from the data processing service's api using the source's data as the key. This allows for per tenant customization of a tenant agnostic workflow.

```
"customData": {
    "ee.grammarMap": { "source" : "tenantData",
                       "data": "ee.grammarMap"}
}
```
Which given a value of `"{"pii.xml": []}" would output after transformation as:

```
"customData": {
    "ee.grammarMap": '{"pii.xml": []}'
}
```


##### queueName

The name of a queue to send the document to. Can be used to specify the queue to send the document to for this action. This is an optional property. If it is not specified then an environment variable made up of the specified `workerName` plus '.taskqueue' is checked for a value to use. If that is not set then a default queue name of `workerName` plus 'Input' is set for the action.

##### workerName

The name of the worker the document will be sent to. Some configuration details, such as the default queue names, will be looked up based on this name.

### FieldMapping Action Type

This action is used to rename the fields of a document, according to a configurable mapping of field names.

#### Usage

Actions of this type should have their type ID set to the action type with internal name 'FieldMappingActionType'.

#### Settings

##### mappings

Defines the mapping of field names that will be applied by the action. Each entry in the map specifies the current name of a field as its key and the desired new name for the field as its value. Swapping of field names is supported - in other words, the new name for a field may be the same as the current name of another field.

###### Examples

A simple renaming of two fields
```
{
  "mappings": 
   {
      "abc": "def",
      "pqr": "xyz"
   }
}
```

Renaming of three fields, two mapping to the same new field name
```
{
  "mappings":
  {
    "DRETITLE": "TITLE",
    "EXTENSION": "FILE_EXTENSION",
    "IMPORTMAGICEXTENSION": "FILE_EXTENSION"
  }
}
```

Swapping the names of fields
```
{
  "mappings": 
   {
      "abc": "def",
      "def": "pqr",
      "pqr": "xyz",
      "xyz": "abc"
   }
}
```

## Workflow Script Error Handling

The generated workflow script assumes that any failures are recorded on the document outside of the script execution. If an
action was marked for execution on a document and then failed it is the job of the code that failed to ensure that an
appropriate failure is added to the document. The script will mark such action as completed so that it can determine the next
action to try and execute on the document.
