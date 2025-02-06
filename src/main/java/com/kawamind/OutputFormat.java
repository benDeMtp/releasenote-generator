package com.kawamind;


import lombok.Getter;

@Getter
public enum OutputFormat {
    ADOC("adoc"),
    MARKDOWN("md");

    final String extension;

    OutputFormat(String extension) {
        this.extension = extension;
    }

}
