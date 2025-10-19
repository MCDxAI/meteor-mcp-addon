# Genkit Observability Guide

## Overview
Genkit provides comprehensive observability features powered by OpenTelemetry, including automatic tracing, metrics collection, and logging. These features help you monitor, debug, and optimize your AI applications in both development and production environments.

## Built-in Observability Features

### Automatic Tracing
Genkit automatically collects traces without explicit configuration:
- **Step-by-step execution**: Detailed view of flow execution
- **Input/output logging**: Complete data flow visibility
- **Performance metrics**: Timing and resource usage statistics
- **Error tracking**: Automatic error capture and context

### Metrics Collection
- **Request metrics**: Throughput, latency, and success rates
- **Resource usage**: Token consumption and API call statistics
- **Custom metrics**: Application-specific measurements
- **Performance trends**: Historical performance analysis

## Local Development Observability

### Developer UI Features
```javascript
import { genkit } from 'genkit';
import { googleAI } from '@genkit-ai/googleai';

const ai = genkit({
  plugins: [googleAI()],
  // Observability is enabled by default
});

// All flows are automatically traced
export const tracedFlow = ai.defineFlow({
  name: 'tracedFlow',
  inputSchema: z.object({ query: z.string() }),
  outputSchema: z.object({ result: z.string() })
}, async (input) => {
  // This step will appear in traces
  const processedInput = await ai.run('preprocess', async () => {
    return input.query.toLowerCase().trim();
  });

  // Generation calls are automatically traced
  const { text } = await ai.generate({
    model: googleAI.model('gemini-2.5-flash'),
    prompt: `Process this query: ${processedInput}`
  });

  // Custom metrics can be added
  await ai.run('postprocess', async () => {
    // Log custom metrics
    console.log(`Processed query length: ${processedInput.length}`);
    return text.toUpperCase();
  });

  return { result: text };
});
```

### Accessing Traces
1. **Start Developer UI**: `genkit start -- npm run dev`
2. **Navigate to Traces**: Go to http://localhost:4000/traces
3. **View Execution Details**: Click on any trace to see step-by-step execution
4. **Analyze Performance**: Review timing, inputs, outputs, and errors

## Logging System

### Centralized Logging
```javascript
import { logger } from 'genkit/logging';

// Configure log level
logger.setLogLevel('debug'); // 'error', 'warn', 'info', 'debug'

// Use structured logging
export const loggingFlow = ai.defineFlow({
  name: 'loggingFlow',
  inputSchema: z.object({ userId: z.string(), action: z.string() }),
  outputSchema: z.object({ success: z.boolean() })
}, async (input) => {
  // Log with context
  logger.info('Flow started', {
    userId: input.userId,
    action: input.action,
    timestamp: new Date().toISOString()
  });

  try {
    const result = await ai.run('process-action', async () => {
      logger.debug('Processing action', { action: input.action });
      
      // Simulate processing
      await new Promise(resolve => setTimeout(resolve, 100));
      
      return { processed: true };
    });

    logger.info('Flow completed successfully', {
      userId: input.userId,
      processingTime: '100ms'
    });

    return { success: true };
  } catch (error) {
    logger.error('Flow failed', {
      userId: input.userId,
      error: error.message,
      stack: error.stack
    });
    
    throw error;
  }
});
```

### Custom Logging Patterns
```javascript
// Structured logging utility
class FlowLogger {
  constructor(flowName, context = {}) {
    this.flowName = flowName;
    this.context = context;
    this.startTime = Date.now();
  }

  log(level, message, data = {}) {
    logger[level](message, {
      flow: this.flowName,
      ...this.context,
      ...data,
      timestamp: new Date().toISOString()
    });
  }

  info(message, data) { this.log('info', message, data); }
  debug(message, data) { this.log('debug', message, data); }
  warn(message, data) { this.log('warn', message, data); }
  error(message, data) { this.log('error', message, data); }

  logDuration(stepName) {
    const duration = Date.now() - this.startTime;
    this.info(`${stepName} completed`, { duration: `${duration}ms` });
  }
}

// Usage in flows
export const observableFlow = ai.defineFlow({
  name: 'observableFlow',
  inputSchema: z.object({ task: z.string() }),
  outputSchema: z.object({ result: z.string() })
}, async (input, { context }) => {
  const flowLogger = new FlowLogger('observableFlow', {
    userId: context?.auth?.uid,
    sessionId: context?.session?.id
  });

  flowLogger.info('Flow execution started', { task: input.task });

  try {
    const step1Result = await ai.run('data-preparation', async () => {
      flowLogger.debug('Preparing data');
      const prepared = await prepareData(input.task);
      flowLogger.logDuration('data-preparation');
      return prepared;
    });

    const step2Result = await ai.run('ai-processing', async () => {
      flowLogger.debug('AI processing started');
      const { text } = await ai.generate({
        model: googleAI.model('gemini-2.5-flash'),
        prompt: `Process: ${step1Result}`
      });
      flowLogger.logDuration('ai-processing');
      return text;
    });

    flowLogger.info('Flow completed successfully');
    return { result: step2Result };
  } catch (error) {
    flowLogger.error('Flow failed', { 
      error: error.message,
      stack: error.stack 
    });
    throw error;
  }
});
```

## Custom Metrics and Monitoring

### Performance Metrics
```javascript
class PerformanceMonitor {
  constructor() {
    this.metrics = new Map();
  }

  startTimer(operation) {
    const startTime = Date.now();
    return {
      end: () => {
        const duration = Date.now() - startTime;
        this.recordMetric(operation, 'duration', duration);
        return duration;
      }
    };
  }

  recordMetric(operation, metric, value) {
    const key = `${operation}.${metric}`;
    if (!this.metrics.has(key)) {
      this.metrics.set(key, []);
    }
    this.metrics.get(key).push({
      value,
      timestamp: Date.now()
    });
  }

  getMetrics(operation) {
    const operationMetrics = {};
    for (const [key, values] of this.metrics) {
      if (key.startsWith(operation)) {
        const metricName = key.split('.')[1];
        operationMetrics[metricName] = {
          count: values.length,
          average: values.reduce((sum, v) => sum + v.value, 0) / values.length,
          min: Math.min(...values.map(v => v.value)),
          max: Math.max(...values.map(v => v.value)),
          latest: values[values.length - 1]?.value
        };
      }
    }
    return operationMetrics;
  }

  exportMetrics() {
    const summary = {};
    for (const [key, values] of this.metrics) {
      summary[key] = {
        count: values.length,
        average: values.reduce((sum, v) => sum + v.value, 0) / values.length,
        recent: values.slice(-10) // Last 10 values
      };
    }
    return summary;
  }
}

const monitor = new PerformanceMonitor();

export const monitoredFlow = ai.defineFlow({
  name: 'monitoredFlow',
  inputSchema: z.object({ data: z.string() }),
  outputSchema: z.object({ processed: z.string() })
}, async (input) => {
  const flowTimer = monitor.startTimer('flow_execution');
  
  try {
    const preprocessTimer = monitor.startTimer('preprocessing');
    const preprocessed = await ai.run('preprocess', async () => {
      const result = await preprocessData(input.data);
      preprocessTimer.end();
      return result;
    });

    const aiTimer = monitor.startTimer('ai_generation');
    const { text } = await ai.generate({
      model: googleAI.model('gemini-2.5-flash'),
      prompt: `Process: ${preprocessed}`
    });
    aiTimer.end();

    monitor.recordMetric('flow_execution', 'success', 1);
    monitor.recordMetric('flow_execution', 'input_length', input.data.length);
    monitor.recordMetric('flow_execution', 'output_length', text.length);

    return { processed: text };
  } catch (error) {
    monitor.recordMetric('flow_execution', 'error', 1);
    throw error;
  } finally {
    flowTimer.end();
  }
});

// Metrics reporting endpoint
export const metricsReportFlow = ai.defineFlow({
  name: 'metricsReport',
  inputSchema: z.object({ operation: z.string().optional() }),
  outputSchema: z.object({ metrics: z.any() })
}, async (input) => {
  const metrics = input.operation 
    ? monitor.getMetrics(input.operation)
    : monitor.exportMetrics();
    
  return { metrics };
});
```

### Health Check Implementation
```javascript
export const healthCheckFlow = ai.defineFlow({
  name: 'healthCheck',
  inputSchema: z.object({ detailed: z.boolean().default(false) }),
  outputSchema: z.object({
    status: z.enum(['healthy', 'degraded', 'unhealthy']),
    timestamp: z.string(),
    checks: z.record(z.any()),
    metrics: z.any().optional()
  })
}, async (input) => {
  const checks = {};
  let overallStatus = 'healthy';

  // Check AI model availability
  try {
    const testTimer = monitor.startTimer('health_check_ai');
    await ai.generate({
      model: googleAI.model('gemini-2.5-flash'),
      prompt: 'Health check test'
    });
    testTimer.end();
    checks.ai_model = { status: 'healthy', latency: testTimer.end() };
  } catch (error) {
    checks.ai_model = { status: 'unhealthy', error: error.message };
    overallStatus = 'unhealthy';
  }

  // Check database connectivity (if applicable)
  try {
    await checkDatabaseConnection();
    checks.database = { status: 'healthy' };
  } catch (error) {
    checks.database = { status: 'unhealthy', error: error.message };
    overallStatus = 'degraded';
  }

  // Check external APIs
  try {
    await checkExternalAPIs();
    checks.external_apis = { status: 'healthy' };
  } catch (error) {
    checks.external_apis = { status: 'degraded', error: error.message };
    if (overallStatus === 'healthy') overallStatus = 'degraded';
  }

  const result = {
    status: overallStatus,
    timestamp: new Date().toISOString(),
    checks
  };

  if (input.detailed) {
    result.metrics = monitor.exportMetrics();
  }

  return result;
});
```

## Production Observability

### Firebase Genkit Monitoring
```javascript
import { firebase } from '@genkit-ai/firebase';

const ai = genkit({
  plugins: [
    googleAI(),
    firebase({
      projectId: 'your-project-id',
      // Enables automatic export to Firebase Genkit Monitoring
      telemetry: {
        instrumentation: 'firebase',
        logger: 'firebase'
      }
    })
  ]
});

// All traces and logs are automatically exported to Firebase
export const productionFlow = ai.defineFlow({
  name: 'productionFlow',
  inputSchema: z.object({ request: z.string() }),
  outputSchema: z.object({ response: z.string() })
}, async (input) => {
  // This will be visible in Firebase Genkit Monitoring
  logger.info('Production flow started', {
    requestId: generateRequestId(),
    inputLength: input.request.length
  });

  const result = await ai.generate({
    model: googleAI.model('gemini-2.5-flash'),
    prompt: input.request
  });

  logger.info('Production flow completed', {
    outputLength: result.text.length,
    success: true
  });

  return { response: result.text };
});
```

**Note**: The Genkit Monitoring dashboard in Firebase console helps you understand the overall health of your Genkit features and is useful for debugging stability and content issues that may indicate problems with your LLM prompts and/or Genkit Flows.

### Custom OpenTelemetry Export
```javascript
import { NodeSDK } from '@opentelemetry/sdk-node';
import { getNodeAutoInstrumentations } from '@opentelemetry/auto-instrumentations-node';
import { Resource } from '@opentelemetry/resources';
import { SemanticResourceAttributes } from '@opentelemetry/semantic-conventions';

// Configure OpenTelemetry for custom export
const sdk = new NodeSDK({
  resource: new Resource({
    [SemanticResourceAttributes.SERVICE_NAME]: 'genkit-app',
    [SemanticResourceAttributes.SERVICE_VERSION]: '1.0.0',
  }),
  instrumentations: [getNodeAutoInstrumentations()],
  // Configure your preferred exporters (Jaeger, Zipkin, etc.)
});

sdk.start();

// Genkit will automatically use the configured OpenTelemetry setup
const ai = genkit({
  plugins: [googleAI()],
  // Telemetry configuration
  telemetry: {
    instrumentation: 'opentelemetry',
    logger: 'winston' // or your preferred logger
  }
});
```

## Debugging and Troubleshooting

### Trace Analysis
```javascript
// Enhanced tracing for debugging
export const debuggableFlow = ai.defineFlow({
  name: 'debuggableFlow',
  inputSchema: z.object({ 
    query: z.string(),
    debug: z.boolean().default(false)
  }),
  outputSchema: z.object({ 
    result: z.string(),
    debugInfo: z.any().optional()
  })
}, async (input) => {
  const debugInfo = input.debug ? {} : undefined;
  
  if (input.debug) {
    debugInfo.startTime = Date.now();
    debugInfo.inputAnalysis = {
      length: input.query.length,
      wordCount: input.query.split(' ').length,
      hasSpecialChars: /[^a-zA-Z0-9\s]/.test(input.query)
    };
  }

  const preprocessed = await ai.run('preprocess', async () => {
    const result = input.query.toLowerCase().trim();
    if (input.debug) {
      debugInfo.preprocessing = {
        originalLength: input.query.length,
        processedLength: result.length,
        transformations: ['lowercase', 'trim']
      };
    }
    return result;
  });

  const { text, usage } = await ai.generate({
    model: googleAI.model('gemini-2.5-flash'),
    prompt: `Process this query: ${preprocessed}`,
    config: {
      temperature: 0.7,
      maxOutputTokens: 1000
    }
  });

  if (input.debug) {
    debugInfo.generation = {
      inputTokens: usage?.inputTokens,
      outputTokens: usage?.outputTokens,
      totalTokens: usage?.totalTokens,
      model: 'gemini-2.5-flash',
      temperature: 0.7
    };
    debugInfo.totalTime = Date.now() - debugInfo.startTime;
  }

  return { 
    result: text,
    debugInfo
  };
});
```

### Error Context Capture
```javascript
class ErrorContextCapture {
  static captureContext(error, additionalContext = {}) {
    return {
      error: {
        message: error.message,
        stack: error.stack,
        name: error.name
      },
      context: {
        timestamp: new Date().toISOString(),
        ...additionalContext
      },
      system: {
        nodeVersion: process.version,
        platform: process.platform,
        memory: process.memoryUsage()
      }
    };
  }
}

export const errorAwareFlow = ai.defineFlow({
  name: 'errorAwareFlow',
  inputSchema: z.object({ operation: z.string() }),
  outputSchema: z.object({ result: z.string() })
}, async (input, { context }) => {
  try {
    return await performOperation(input.operation);
  } catch (error) {
    const errorContext = ErrorContextCapture.captureContext(error, {
      operation: input.operation,
      userId: context?.auth?.uid,
      sessionId: context?.session?.id,
      flowName: 'errorAwareFlow'
    });

    logger.error('Flow execution failed', errorContext);
    
    // Re-throw with enhanced context
    const enhancedError = new Error(`Operation failed: ${error.message}`);
    enhancedError.context = errorContext;
    throw enhancedError;
  }
});
```

## Best Practices

### Observability Strategy
1. **Comprehensive Logging**: Log all significant events and state changes
2. **Structured Data**: Use consistent log formats and structured data
3. **Context Preservation**: Include relevant context in all log entries
4. **Performance Tracking**: Monitor key performance indicators consistently

### Development Workflow
1. **Local Monitoring**: Use Developer UI for development and testing
2. **Trace Analysis**: Regularly review traces for optimization opportunities
3. **Error Investigation**: Use detailed error context for debugging
4. **Performance Profiling**: Identify and address performance bottlenecks

### Production Monitoring
1. **Automated Alerting**: Set up alerts for critical metrics and errors
2. **Dashboard Creation**: Build dashboards for key operational metrics
3. **Trend Analysis**: Monitor long-term trends and patterns
4. **Capacity Planning**: Use metrics for resource planning and scaling

This observability guide provides comprehensive monitoring and debugging capabilities for your Genkit applications across all environments.