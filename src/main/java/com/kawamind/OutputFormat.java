package com.kawamind;

public enum OutputFormat {
    ADOC("asciidoc"),
    MARKDOWN("markdown");

    final String description;

    OutputFormat(String description) {
        this.description = description;
    }

}
