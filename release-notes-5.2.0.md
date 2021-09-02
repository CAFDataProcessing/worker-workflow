!not-ready-for-release!

#### Version Number
${version-number}

#### New Features
- SCMOD-14252: Added support for resolving workflow argument values which use multivalue fields. When looking up argument values from the `settings-service`, the multiple values of a field are all passed with equal priority.
- SCMOD-6849: Added support for forcing a refresh of the Settings Service cache. To do this, supply a
`customData.settingsServiceLastUpdateTimeMillis` property. If the value of this is greater than the last time a setting was fetched
from the Settings Service, then the setting will be fetched from the Settings Service instead of from the cache.

#### Bug fixes
- SCMOD-14805: Settings Service cache fixed.

#### Known Issues
- None
