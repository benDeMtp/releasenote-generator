package com.kawamind;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class Version {

    String name;
    String date;

    List<String> features = new ArrayList<>();
    List<String> fixes = new ArrayList<>();
    List<String> docs = new ArrayList<>();
    List<String> refactors = new ArrayList<>();
    List<String> ops = new ArrayList<>();
    List<String> builds = new ArrayList<>();
    List<String> styles = new ArrayList<>();
    List<String> chores = new ArrayList<>();
    List<String> tests = new ArrayList<>();

}
