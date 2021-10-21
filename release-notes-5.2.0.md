#### Version Number
${version-number}

#### New Features
- **SCMOD-14252**: Added support for resolving workflow argument values which use multivalue fields. When looking up argument values from
  the `settings-service`, the multiple values of a field are all passed with equal priority.
- **SCMOD-6849**: Added support for forcing a refresh of the Settings Service cache. See the [README.md]
  (https://github.com/CAFDataProcessing/worker-workflow/tree/develop/worker-workflow-container#settingsServiceLastUpdateTimeMillis)
  for details.

#### Bug fixes
- **SCMOD-14805**: Settings Service cache fixed.

#### Known Issues
- None
