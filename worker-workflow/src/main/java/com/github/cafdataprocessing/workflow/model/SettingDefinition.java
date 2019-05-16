package com.github.cafdataprocessing.workflow.model;

import java.util.List;

public class SettingDefinition {
    private String name;
    private List<Source> sources;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Source> getSources() {
        return sources;
    }

    public void setSources(List<Source> sources) {
        this.sources = sources;
    }

    public static class Source {
        private String name;
        private SourceType type;
        private String options;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public SourceType getType() {
            return type;
        }

        public void setType(SourceType type) {
            this.type = type;
        }

        public String getOptions() {
            return options;
        }

        public void setOptions(String options) {
            this.options = options;
        }
    }

    public enum SourceType {
        FIELD, CUSTOM_DATA, SETTINGS_SERVICE
    }
}
