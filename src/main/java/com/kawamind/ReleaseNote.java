package com.kawamind;

import java.util.List;

public record ReleaseNote(List<Version> lastVersions,List<Version> olderVersions) {}
