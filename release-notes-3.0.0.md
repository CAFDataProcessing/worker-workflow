#### Version Number
${version-number}

#### Breaking Changes
* [SCMOD-5747](https://portal.digitalsafe.net/browse/SCMOD-5747) - The workflow worker does not use the processing-service and the
workflow-admin anymore. Workflows are loaded from files saved on disk (look at the worker-workflow [README](worker-workflow/README.md) for more info).

#### New Features
* [SCMOD-5739](https://portal.digitalsafe.net/browse/SCMOD-5739) -  The Data Processing Service and Policy were replaced with Settings Service in Workflow worker,
Collection Center, Darwin Settings Service and Deploy.

#### Deprecated Feature
* [SCMOD-5739](https://portal.digitalsafe.net/browse/SCMOD-5739) - The Data Processing Service and Policy were deprecated from Workflow worker,
Collection Center, Darwin Settings Service and Deploy.

#### Known Issues
- None
