/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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
package com.github.cafdataprocessing.workflow.testing;

public class ActionExpectationBuilder {
    private final ActionExpectation actionExpectation;
    private final ActionExpectationsBuilder actionExpectationsBuilder;

    public ActionExpectationBuilder(final ActionExpectation actionExpectation,
                                    final ActionExpectationsBuilder actionExpectationsBuilder) {

        this.actionExpectation = actionExpectation;
        this.actionExpectationsBuilder = actionExpectationsBuilder;
    }

    public ActionExpectationBuilder successQueue(final String successQueue) {
        actionExpectation.setSuccessQueue(successQueue);
        return this;
    }

    public ActionExpectationBuilder failureQueue(final String failureQueue) {
        actionExpectation.setFailureQueue(failureQueue);
        return this;
    }

    public CustomDataExpectationBuilder withCustomData() {
        return new CustomDataExpectationBuilder(actionExpectation, this);
    }
    
    public ActionExpectationBuilder terminateOnFailure(final boolean terminateOnFailure) {
        actionExpectation.setTerminateOnFailure(terminateOnFailure);
        return this;
    }

    public ActionExpectationsBuilder actionExpectationsBuilder() {
        return actionExpectationsBuilder;
    }

}
