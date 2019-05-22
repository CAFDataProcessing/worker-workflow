The workflows are in a yaml format and would contain the actions (workers) that needs to be called in that workflow.
 
 
|  Key Names  |    explanation     |  
|:------------------:|:-------------------:|
|     arguments |   List of CAF_Settings that are passed to the workflow|
|     actions |   List of workers that gets called in the workflow |

*Arguments*

Arguments are the settings or data needed for the worker to effeciently process the document.
Aruments has 3 fields 
1. Name
2. Source (`FIELD` or `CUSTOM_DATA` or `SETTINGS_SERVICE`)
3. DefaultValue

```yaml
arguments:
  - name: tenantId  				# arg name
    sources:	     				# argument source, can be custom_data, field or settings_service
      - name: TASK_SETTING_TENANTID		# arg name passed in the source
        type: SETTING_SERVICE			# source name i.e custom_data, field or settings_service
    defaultValue: DETECT			# if no value, then default is taken
        options: repository-%f:TENANT_ID%   # if type is settings_service then option is used for getting value from other source (i.e field (%f) or custom_data (%cd))
```

Note:  if the Source is `SETTINGS_SERVICE` then we need to mention the `options` i.e to specify where the data needs to be taken from.
It can be either from another field in the document (%f) or from custom_data(%cd)


*Actions*

Actions are the worker that needs to be called for processing the document.en indicates the field name to check.
Actions 5 fields
1. Name
2. QueueName
3. ConditionFunction
4. CustomData
5. Scripts

```yaml
- name: lang_detect					# action name					
    conditionFunction: |                           	# condition (if any) for the worker to be actioned
        function (document) { 
        return fieldExists(document, 'CONTENT_PRIMARY'); 
        }
    queueName: dataprocessing-langdetect-in		# queue name of the worker
    customData:						# custom_data to be passed to the worker
      fieldSpecs: "'CONTENT_PRIMARY'"
    scripts:						# list of custom scripts that can be passed to the worker
      - name: recordProcessingTimes.js			# script name
        script: |
          function onProcessTask(e) {
            var startTime = new Date();
            e........}
```
