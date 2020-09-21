# Workflow Structure

A workflow is described using yaml and contains workflow arguments and actions.


|  Key Names  |    explanation     |  
|:------------------:|:-------------------:|
| arguments | A list of argument names, default values and sources for argument values. |
| actions | A list of action names, output queues, conditions and custom data.  |

## arguments

Workflow arguments are resolved by the workflow worker and placed into the `CAF_WORKFLOW_SETTINGS` document field and `CAF_WORKFLOW_SETTINGS` document custom data. Arguments are referenced by name when defining the custom data for an action.

An argument has 3 fields.

1. name (The name of the argument, used in an actions custom data section)
2. defaultValue (Optional. The default value if no sources are supplied or the supplied sources yeild no value)
3. sources (A list of source objects which are resolved in order until a value is returned)

A source has 3 fields.

1. name (The name of a document field, custom data key or setting key from the settings service)
2. type (`FIELD` or `CUSTOM_DATA` or `SETTINGS_SERVICE`)
3. options (When source type is `SETTINGS_SERVICE` options are supplied as the scopes argument to the settings service)

The following patterns will be substituted in an options string.

1. `%f:FIELD_NAME%` will be replaced by the first value of the document field called `FIELD_NAME`
2. `%cd:CUSTOM_DATA_NAME%` will be replaced by the value of the custom data key `CUSTOM_DATA_NAME`

```yaml
arguments:
  - name: OPERATION_MODE
    defaultValue: DETECT
    sources:
      - name: EE.OPERATIONMODE
        type: SETTINGS_SERVICE
        options: repository-%f:REPOSITORY_ID%,tenant-%cd:TENANT_ID%
```

## actions

An action has 4 fields.

1. name (The name of the action)
2. conditionFunction (A function named `condition` that accepts a document and a map of arguments as parameters and returns true if the document meets the conditions of the action)
3. customData (Custom data required by the worker executing the action) 
4. scripts (Additional scripts that will be executed by the worker executing the action)

Note: The target queue for an action will be retrieved from an environment variable named `CAF_WORKFLOW_ACTION_{actionName}_INPUT_QUEUE`. The `actionName` defined in the workflow will be upper cased. 
If the environment variable is null or an empty string then the target queue name will be constructed from the action name and suffixed with -in
 
`eg for environment variable: CAF_WORKFLOW_ACTION_LANG_DETECT_INPUT_QUEUE=dataprocessing-langdetect-in`

`eg for default value: action name :lang_detect so queueName: lang_detect-in`

### customData

Custom data is comprised of keys and values, each key and value is added to the custom data of the document queued for a document worker. The value of a custom data key can be a string literal `"'My value'"` or the name of an argument `OPERATION_MODE` defined in the arguments section.

### scripts

Additional information about how scripts are executed by a document worker can be found [here](https://github.com/CAFDataProcessing/worker-document#document-worker-event-handlers).

```yaml
- name: lang_detect					# action name					
    conditionFunction: |                           	# condition (if any) for the worker to be actioned
        function condition (document, arguments) { 
          return fieldExists(document, 'CONTENT_PRIMARY'); 
        }
    queueName: dataprocessing-langdetect-in		# queue name of the worker
    customData:						# custom_data to be passed to the worker
      fieldSpecs: "'CONTENT_PRIMARY'"
    scripts:						# list of custom scripts that can be passed to the worker
      - name: recordProcessingTimes.js			# script name
        engine: GRAAL_JS 				# script engine to execute on 
        script: |
          function onProcessTask(e) {
            var startTime = new Date();
            e........}
```
