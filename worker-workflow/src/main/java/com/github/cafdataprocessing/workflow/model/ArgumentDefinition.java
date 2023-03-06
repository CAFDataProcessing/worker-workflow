/*
 * Copyright 2017-2023 Micro Focus or one of its affiliates.
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
package com.github.cafdataprocessing.workflow.model;

import java.util.List;

public class ArgumentDefinition {
    private String name;
    private String defaultValue;
    private List<Source> sources;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public List<Source> getSources() {
        return sources;
    }

    public void setSources(final List<Source> sources) {
        this.sources = sources;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(final String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public static class Source {
        private String name;
        private SourceType type;
        private String options;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public SourceType getType() {
            return type;
        }

        public void setType(final SourceType type) {
            this.type = type;
        }

        public String getOptions() {
            return options;
        }

        public void setOptions(final String options) {
            this.options = options;
        }
    }

    public enum SourceType {
        FIELD, CUSTOM_DATA, SETTINGS_SERVICE
    }
}
