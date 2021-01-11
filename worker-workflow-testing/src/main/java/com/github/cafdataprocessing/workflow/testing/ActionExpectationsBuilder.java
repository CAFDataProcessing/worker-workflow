/*
 * Copyright 2017-2021 Micro Focus or one of its affiliates.
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

import java.util.ArrayList;
import java.util.List;

public class ActionExpectationsBuilder {

    private final List<ActionExpectation> actionExpectations = new ArrayList<>();

    public ActionExpectationBuilder withAction(final String action) {

        final ActionExpectation actionExpectation = new ActionExpectation();
        actionExpectation.setAction(action);
        actionExpectations.add(actionExpectation);

        return new ActionExpectationBuilder(actionExpectation, this);
    }

    public List<ActionExpectation> build() {
        return actionExpectations;
    }
}

