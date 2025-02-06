package com.kawamind;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

@RegisterForReflection
public record ReleaseNote(List<Version> lastVersions,List<Version> olderVersions) {}
