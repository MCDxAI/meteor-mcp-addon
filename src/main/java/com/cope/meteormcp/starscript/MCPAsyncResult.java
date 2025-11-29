package com.cope.meteormcp.starscript;

import java.util.Objects;

/**
 * Holds the state of an asynchronous MCP tool call.
 * Stores the last known result and tracks whether a background fetch is in progress.
 *
 * @author GhostTypes
 */
class MCPAsyncResult {
    private volatile String lastResult;
    private volatile boolean taskInProgress;

    /**
     * Creates a new async result holder with a default "Loading..." state.
     */
    public MCPAsyncResult() {
        this.lastResult = "Loading...";
        this.taskInProgress = false;
    }

    /**
     * Gets the last known result. This is safe to call from any thread.
     *
     * @return the last result string, never null
     */
    public String getLastResult() {
        return lastResult;
    }

    /**
     * Updates the last known result. Thread-safe.
     *
     * @param result the new result to store
     */
    public void setLastResult(String result) {
        this.lastResult = Objects.requireNonNullElse(result, "");
    }

    /**
     * Checks if a background task is currently in progress.
     *
     * @return true if a fetch is running, false otherwise
     */
    public boolean isTaskInProgress() {
        return taskInProgress;
    }

    /**
     * Atomically attempts to start a new task.
     *
     * @return true if the task was started (no other task was running), false if already in progress
     */
    public synchronized boolean tryStartTask() {
        if (taskInProgress) {
            return false;
        }
        taskInProgress = true;
        return true;
    }

    /**
     * Marks the current task as completed.
     */
    public synchronized void completeTask() {
        taskInProgress = false;
    }
}
