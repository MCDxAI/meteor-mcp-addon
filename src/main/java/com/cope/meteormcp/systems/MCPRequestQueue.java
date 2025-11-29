package com.cope.meteormcp.systems;

import com.cope.meteormcp.MeteorMCPAddon;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages a sequential request queue for a single MCP server connection.
 * Ensures only one tool call executes at a time, preventing "Failed to enqueue message" errors.
 * <p>
 * A dedicated worker thread processes requests from the queue in FIFO order.
 *
 * @author GhostTypes
 */
public class MCPRequestQueue {
    private final String serverName;
    private final McpSyncClient client;
    private final BlockingQueue<ToolRequest> requestQueue;
    private final Thread workerThread;
    private final AtomicBoolean running;

    /**
     * Creates a new request queue for the given MCP server.
     *
     * @param serverName display name for logging
     * @param client MCP sync client to execute requests
     */
    public MCPRequestQueue(String serverName, McpSyncClient client) {
        this.serverName = serverName;
        this.client = client;
        this.requestQueue = new LinkedBlockingQueue<>();
        this.running = new AtomicBoolean(true);
        this.workerThread = createWorkerThread();
        this.workerThread.start();
    }

    /**
     * Submits a tool execution request to the queue.
     * The request will be processed asynchronously by the worker thread.
     *
     * @param request the tool request to execute
     */
    public void submitRequest(ToolRequest request) {
        if (!running.get()) {
            request.resultFuture().completeExceptionally(
                new IllegalStateException("Request queue is shut down")
            );
            return;
        }

        try {
            requestQueue.put(request);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            request.resultFuture().completeExceptionally(e);
        }
    }

    /**
     * Shuts down the request queue and stops the worker thread.
     * Any pending requests will be completed exceptionally.
     */
    public void shutdown() {
        if (!running.compareAndSet(true, false)) {
            return; // Already shut down
        }

        workerThread.interrupt();

        // Drain queue and fail all pending requests
        ToolRequest request;
        while ((request = requestQueue.poll()) != null) {
            request.resultFuture().completeExceptionally(
                new IllegalStateException("Server disconnected")
            );
        }
    }

    /**
     * Gets the current number of pending requests in the queue.
     *
     * @return queue size
     */
    public int getQueueSize() {
        return requestQueue.size();
    }

    /**
     * Checks if the queue is still running.
     *
     * @return true if accepting requests, false if shut down
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Creates the worker thread that processes requests sequentially.
     *
     * @return configured thread
     */
    private Thread createWorkerThread() {
        Thread thread = new Thread(() -> {
            MeteorMCPAddon.LOG.info("MCP request queue worker started for server: {}", serverName);

            while (running.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    // Block until a request is available
                    ToolRequest request = requestQueue.take();

                    // Execute the tool call (blocking, but only one at a time)
                    try {
                        CallToolRequest mcpRequest = new CallToolRequest(
                            request.toolName(),
                            request.arguments()
                        );
                        CallToolResult result = client.callTool(mcpRequest);
                        request.resultFuture().complete(result);

                    } catch (Exception e) {
                        MeteorMCPAddon.LOG.error("Tool execution failed for {} on {}: {}",
                            request.toolName(), serverName, e.getMessage());
                        request.resultFuture().completeExceptionally(e);
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            MeteorMCPAddon.LOG.info("MCP request queue worker stopped for server: {}", serverName);
        }, "MCP-Queue-" + serverName);

        thread.setDaemon(true);
        return thread;
    }
}
