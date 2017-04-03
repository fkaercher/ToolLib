/*
 * Copyright (c) 2016 eilslabs.
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.execution.io;

import de.dkfz.roddy.core.InfoObject;

import java.util.List;

/**
 * Stores the result of a command execution.
 * Commands can i.e. be executed via ssh or on the local command line.
 * @author michael
 */
public class ExecutionResult extends ExecutionHelper.ExtendedProcessExecutionResult {

    /**
     * Successful or not?
     */
    public final boolean successful;
    /**
     * An error number. Normally the return code for shell stuff.
     */
    @Deprecated
    public final int errorNumber;
    /**
     * All result lines.
     */
    public final List<String> resultLines;
    /**
     * First line of the result array.
     * Null if no entries are in the array.
     */
    public final String firstLine;

    public final int exitCode;

    public ExecutionResult(boolean successful, int exitCode, List<String> resultLines, String processID) {
        super(exitCode, processID, resultLines);
        this.successful = successful;
        this.errorNumber = exitCode;
        this.exitCode = exitCode;
        this.resultLines = resultLines;
//        this.processID = processID;
        if(resultLines.size() > 0)
            firstLine = resultLines.get(0);
        else
            firstLine = null;
    }

    @Override
    public boolean isSuccessful() {
        return exitCode == 0;
    }

    public boolean getSuccessful() {
        return isSuccessful();
    }
}
