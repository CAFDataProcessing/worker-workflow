/*
 * Copyright 2017-2023 Open Text.
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

public class CustomDataExpectationBuilder {
    private final ActionExpectation actionExpectation;
    private final ActionExpectationBuilder actionExpectationBuilder;

    public CustomDataExpectationBuilder (final ActionExpectation actionExpectation,
                                         final ActionExpectationBuilder actionExpectationBuilder) {
        this.actionExpectation = actionExpectation;
        this.actionExpectationBuilder = actionExpectationBuilder;
    }

    public CustomDataExpectationBuilder addCustomData(final String name, final String value) {
        actionExpectation.getCustomData().put(name, value);
        return this;
    }

    public ActionExpectationBuilder actionExpectationBuilder() {
        return actionExpectationBuilder;
    }

    public ActionExpectationsBuilder actionExpectationsBuilder() {
        return actionExpectationBuilder.actionExpectationsBuilder();
    }

}
