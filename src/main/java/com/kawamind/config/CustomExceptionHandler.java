package com.kawamind.config;

import org.eclipse.jgit.api.errors.NoHeadException;
import picocli.CommandLine;

public class CustomExceptionHandler implements CommandLine.IExecutionExceptionHandler {

    @Override
    public int handleExecutionException(Exception ex, CommandLine commandLine, CommandLine.ParseResult fullParseResult) throws Exception {

        if(ex instanceof NoHeadException){
            System.err.println("There is no commit in the repository");
            return 1;
        }
        return 0;
    }
}
