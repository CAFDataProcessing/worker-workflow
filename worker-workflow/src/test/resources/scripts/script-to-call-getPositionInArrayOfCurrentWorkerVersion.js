/* 
 * Copyright 2019 EntIT Software LLC, a Micro Focus company.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
var actionFamilyHashing = {name: "family_hashing", terminateOnFailure: false};
var actionBulkIndexer = {name: "bulk_indexer", terminateOnFailure: true};
var actionElastic = {name: "elastic", terminateOnFailure: false};
var ACTIONS = [actionFamilyHashing, actionBulkIndexer, actionElastic];

var workerVersionOne = {NAME: "worker-entityextract", VERSION: "1.0.0-SNASHOT"};
var workerVersionTwo = {NAME: "worker-familyhashing", VERSION: "2.4.0-SNASHOT"};

var testArray = [workerVersionOne, workerVersionTwo];

function callGetPositionInArrayOfCurrentWorkerVersion(name) {
    return getPositionInArrayOfCurrentWorkerVersion(testArray, name);
}

