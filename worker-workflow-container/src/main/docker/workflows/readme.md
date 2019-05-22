====
    Copyright 2015-2018 Micro Focus or one of its affiliates.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
====



The workflows are in a yaml format and would contain the actions (workers) that needs to be called in that workflow.
 
 
|  Key Names  |    explanation     |  
|:------------------:|:-------------------:|
|     arguments |   List of CAF_Settings that are passed to the workflow|
|     actions |   List of workers that gets called in the workflow |




#### arguments:

```yaml
arguments:
  - name: tenantId  				# arg name
    sources:	     				# argument source, can be custom_data, field or settings_service
      - name: TASK_SETTING_TENANTID		# arg name passed in the source
        type: CUSTOM_DATA			# source name i.e custom_data, field or settings_service
    defaultValue: DETECT			# if no value, then default is taken
        options: repository-%f:REPOSITORY_ID%   # if type is settings_service then option is used for getting value from other source (i.e field (%f) or custom_data (%cd))
```

#### actions:	
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