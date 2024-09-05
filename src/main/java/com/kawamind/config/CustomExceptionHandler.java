package com.kawamind.config;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.NoHeadException;
import picocli.CommandLine;

@Slf4j
public class CustomExceptionHandler implements CommandLine.IExecutionExceptionHandler {

    @Override
    public int handleExecutionException(Exception ex, CommandLine commandLine, CommandLine.ParseResult fullParseResult) {

        if(ex instanceof NoHeadException){
            System.err.println("There is no commit in the repository");
        }
        else{
            log.error("",ex);
        }
        return 1;
    }
}
