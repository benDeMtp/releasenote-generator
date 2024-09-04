package com.kawamind.config;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.Getter;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
@Getter
public class ConfigService {

    @ConfigProperty(name = "history.section.title")
    String historySectionTitle;

    @ConfigProperty(name = "type.chore.title")
    String chore;

    @ConfigProperty(name = "type.feature.title")
    String feature;

    @ConfigProperty(name = "type.build.title")
    String build;

    @ConfigProperty(name = "type.doc.title")
    String doc;

    @ConfigProperty(name = "type.ops.title")
    String ops;

    @ConfigProperty(name = "type.fix.title")
    String fix;

    @ConfigProperty(name = "type.refactor.title")
    String refactor;

    @ConfigProperty(name = "type.style.title")
    String style;

}
