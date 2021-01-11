#### Version Number
${version-number}

#### New Features
* SCMOD-10362: GraalVM JavaScript engine added  
The worker has been enhanced to support Graal.js scripts in Workflows. This allows ECMAScript 2020 features
to be included in customization scripts. Existing Nashorn scripts continue to be supported.

#### Bug Fixes
* SCMOD-9981: Fix for index out of bounds exception when the `CAF_WORKFLOW_ACTION` field was missing  
* SCMOD-10917: Fix for some script errors not being recorded in the index when there were errors in multiple actions

#### Known Issues
- None

#### Breaking Changes
* Due to moving to Graal.js all `conditionFunction` elements in Workflows must be updated so that the function is named 
`condition` and be Graal.js compatible.
* Any `context.js` scripts must be Graal.js compatible.
* The `thisScriptObject` field exposed by `workflow-control.js` is no longer an object. It is now a string containing 
the `addFailures` function definition. Consumers must be updated to `eval()` this string into their context in order to 
access the function. 
* All downstream Workers must be upgraded to Worker-Document framework 4.5.0 to support executing Graal.js scripts.
