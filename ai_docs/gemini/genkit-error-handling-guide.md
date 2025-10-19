# Genkit Error Handling Guide

## Overview
Genkit provides specialized error types and comprehensive error handling patterns to help you build robust AI applications. Understanding Genkit's error system is crucial for creating reliable, secure, and user-friendly applications.

## Genkit Error Types

### GenkitError
- **Purpose**: Used by Genkit itself and Genkit plugins
- **Scope**: Internal framework and plugin errors
- **Visibility**: Should not be exposed directly to end users
- **Use case**: System-level errors, plugin failures, configuration issues

### UserFacingError
- **Purpose**: Intended for ContextProviders and your application code
- **Scope**: Application-level errors that can be safely shown to users
- **Visibility**: Safe to expose to end users
- **Use case**: Validation errors, business logic failures, user input issues

**Important**: The separation between these two error types helps you better understand where your error is coming from. Genkit plugins for web hosting (e.g. @genkit-ai/express or @genkit-ai/next) SHOULD capture all other Error types and instead report them as an internal error in the response. This adds a layer of security to your application by ensuring that internal details of your application do not leak to attackers.

```javascript
import { genkit, z } from 'genkit';
import { googleAI } from '@genkit-ai/googleai';
import { GenkitError, UserFacingError } from 'genkit/errors';

const ai = genkit({
  plugins: [googleAI()],
});

// Example of UserFacingError usage
export const userInputFlow = ai.defineFlow({
  name: 'userInputFlow',
  inputSchema: z.object({
    email: z.string(),
    age: z.number()
  }),
  outputSchema: z.object({
    result: z.string()
  })
}, async (input) => {
  // Validate user input
  if (!input.email.includes('@')) {
    throw new UserFacingError('Please provide a valid email address');
  }
  
  if (input.age < 0 || input.age > 150) {
    throw new UserFacingError('Age must be between 0 and 150');
  }

  try {
    const result = await processUserData(input);
    return { result };
  } catch (error) {
    // Convert internal errors to user-friendly messages
    if (error.code === 'DATABASE_CONNECTION_FAILED') {
      throw new UserFacingError('Service temporarily unavailable. Please try again later.');
    }
    
    // Re-throw as GenkitError for internal issues
    throw new GenkitError('Internal processing error', error);
  }
});
```

## Error Handling Patterns

### Input Validation and Sanitization
```javascript
class InputValidator {
  static validateEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      throw new UserFacingError('Invalid email format');
    }
  }

  static validateStringLength(value, fieldName, min = 1, max = 1000) {
    if (typeof value !== 'string') {
      throw new UserFacingError(`${fieldName} must be a string`);
    }
    if (value.length < min) {
      throw new UserFacingError(`${fieldName} must be at least ${min} characters`);
    }
    if (value.length > max) {
      throw new UserFacingError(`${fieldName} must not exceed ${max} characters`);
    }
  }

  static sanitizeInput(input) {
    if (typeof input !== 'string') return input;
    
    // Remove potentially harmful content
    return input
      .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
      .replace(/javascript:/gi, '')
      .trim();
  }
}

export const validatedFlow = ai.defineFlow({
  name: 'validatedFlow',
  inputSchema: z.object({
    userMessage: z.string(),
    userEmail: z.string(),
    priority: z.enum(['low', 'medium', 'high']).optional()
  }),
  outputSchema: z.object({
    response: z.string(),
    ticketId: z.string()
  })
}, async (input) => {
  try {
    // Validate inputs
    InputValidator.validateEmail(input.userEmail);
    InputValidator.validateStringLength(input.userMessage, 'Message', 10, 5000);
    
    // Sanitize inputs
    const sanitizedMessage = InputValidator.sanitizeInput(input.userMessage);
    
    // Process the request
    const { text } = await ai.generate({
      model: googleAI.model('gemini-2.5-flash'),
      prompt: `Help with this user request: ${sanitizedMessage}`
    });

    const ticketId = generateTicketId();
    
    return {
      response: text,
      ticketId
    };
  } catch (error) {
    if (error instanceof UserFacingError) {
      throw error; // Re-throw user-facing errors as-is
    }
    
    // Log internal errors for debugging
    console.error('Validation flow error:', error);
    throw new UserFacingError('Unable to process your request. Please check your input and try again.');
  }
});
```

### Retry Logic and Circuit Breaker
```javascript
class RetryHandler {
  constructor(maxRetries = 3, baseDelay = 1000, maxDelay = 10000) {
    this.maxRetries = maxRetries;
    this.baseDelay = baseDelay;
    this.maxDelay = maxDelay;
  }

  async executeWithRetry(operation, context = {}) {
    let lastError;
    
    for (let attempt = 1; attempt <= this.maxRetries; attempt++) {
      try {
        return await operation();
      } catch (error) {
        lastError = error;
        
        // Don't retry user-facing errors
        if (error instanceof UserFacingError) {
          throw error;
        }
        
        // Don't retry on final attempt
        if (attempt === this.maxRetries) {
          break;
        }
        
        // Calculate delay with exponential backoff
        const delay = Math.min(
          this.baseDelay * Math.pow(2, attempt - 1),
          this.maxDelay
        );
        
        console.warn(`Attempt ${attempt} failed, retrying in ${delay}ms:`, error.message);
        await new Promise(resolve => setTimeout(resolve, delay));
      }
    }
    
    // All retries failed
    throw new GenkitError(
      `Operation failed after ${this.maxRetries} attempts`,
      lastError,
      { context }
    );
  }
}

class CircuitBreaker {
  constructor(threshold = 5, timeout = 60000) {
    this.threshold = threshold;
    this.timeout = timeout;
    this.failureCount = 0;
    this.lastFailureTime = null;
    this.state = 'CLOSED'; // CLOSED, OPEN, HALF_OPEN
  }

  async execute(operation) {
    if (this.state === 'OPEN') {
      if (Date.now() - this.lastFailureTime > this.timeout) {
        this.state = 'HALF_OPEN';
      } else {
        throw new UserFacingError('Service temporarily unavailable');
      }
    }

    try {
      const result = await operation();
      this.onSuccess();
      return result;
    } catch (error) {
      this.onFailure();
      throw error;
    }
  }

  onSuccess() {
    this.failureCount = 0;
    this.state = 'CLOSED';
  }

  onFailure() {
    this.failureCount++;
    this.lastFailureTime = Date.now();
    
    if (this.failureCount >= this.threshold) {
      this.state = 'OPEN';
    }
  }
}

const retryHandler = new RetryHandler();
const circuitBreaker = new CircuitBreaker();

export const resilientFlow = ai.defineFlow({
  name: 'resilientFlow',
  inputSchema: z.object({
    query: z.string(),
    options: z.object({
      retries: z.boolean().default(true),
      circuitBreaker: z.boolean().default(true)
    }).optional()
  }),
  outputSchema: z.object({
    result: z.string(),
    metadata: z.object({
      attempts: z.number(),
      processingTime: z.number()
    })
  })
}, async (input) => {
  const startTime = Date.now();
  let attempts = 1;

  const operation = async () => {
    const { text } = await ai.generate({
      model: googleAI.model('gemini-2.5-flash'),
      prompt: input.query
    });
    return text;
  };

  try {
    let result;
    
    if (input.options?.circuitBreaker) {
      result = await circuitBreaker.execute(async () => {
        if (input.options?.retries) {
          return await retryHandler.executeWithRetry(operation);
        } else {
          return await operation();
        }
      });
    } else if (input.options?.retries) {
      result = await retryHandler.executeWithRetry(operation);
    } else {
      result = await operation();
    }

    return {
      result,
      metadata: {
        attempts,
        processingTime: Date.now() - startTime
      }
    };
  } catch (error) {
    if (error instanceof UserFacingError) {
      throw error;
    }
    
    throw new UserFacingError('Unable to process your request at this time');
  }
});
```

### Graceful Degradation
```javascript
class FallbackHandler {
  constructor() {
    this.fallbackStrategies = new Map();
  }

  registerFallback(operation, fallbackFn) {
    this.fallbackStrategies.set(operation, fallbackFn);
  }

  async executeWithFallback(operation, operationName, input) {
    try {
      return await operation();
    } catch (error) {
      console.warn(`Primary operation ${operationName} failed:`, error.message);
      
      const fallback = this.fallbackStrategies.get(operationName);
      if (fallback) {
        try {
          console.log(`Executing fallback for ${operationName}`);
          return await fallback(input, error);
        } catch (fallbackError) {
          console.error(`Fallback for ${operationName} also failed:`, fallbackError.message);
          throw new UserFacingError('Service temporarily degraded. Some features may be limited.');
        }
      }
      
      throw error;
    }
  }
}

const fallbackHandler = new FallbackHandler();

// Register fallback strategies
fallbackHandler.registerFallback('ai_generation', async (input, originalError) => {
  // Fallback to simpler model or cached response
  return {
    text: "I'm experiencing technical difficulties. Please try again later.",
    fallback: true
  };
});

fallbackHandler.registerFallback('database_query', async (input, originalError) => {
  // Fallback to cached data or default response
  return getCachedResponse(input) || getDefaultResponse();
});

export const degradationFlow = ai.defineFlow({
  name: 'degradationFlow',
  inputSchema: z.object({
    query: z.string(),
    requireFullFeatures: z.boolean().default(false)
  }),
  outputSchema: z.object({
    response: z.string(),
    degraded: z.boolean(),
    availableFeatures: z.array(z.string())
  })
}, async (input) => {
  const availableFeatures = [];
  let degraded = false;

  // Try AI generation with fallback
  const aiResult = await fallbackHandler.executeWithFallback(
    async () => {
      const { text } = await ai.generate({
        model: googleAI.model('gemini-2.5-flash'),
        prompt: input.query
      });
      availableFeatures.push('ai_generation');
      return { text, fallback: false };
    },
    'ai_generation',
    input
  );

  if (aiResult.fallback) {
    degraded = true;
  } else {
    availableFeatures.push('full_ai_capabilities');
  }

  // Try additional features with fallbacks
  try {
    await performEnhancedAnalysis(input.query);
    availableFeatures.push('enhanced_analysis');
  } catch (error) {
    console.warn('Enhanced analysis unavailable:', error.message);
    degraded = true;
  }

  // Check if degradation is acceptable
  if (degraded && input.requireFullFeatures) {
    throw new UserFacingError('Full features are currently unavailable. Please try again later.');
  }

  return {
    response: aiResult.text,
    degraded,
    availableFeatures
  };
});
```

### Error Context and Logging
```javascript
class ErrorLogger {
  static logError(error, context = {}) {
    const errorInfo = {
      timestamp: new Date().toISOString(),
      type: error.constructor.name,
      message: error.message,
      stack: error.stack,
      context,
      severity: this.determineSeverity(error)
    };

    if (error instanceof UserFacingError) {
      console.warn('User-facing error:', errorInfo);
    } else if (error instanceof GenkitError) {
      console.error('Genkit error:', errorInfo);
    } else {
      console.error('Unexpected error:', errorInfo);
    }

    // Send to monitoring service in production
    if (process.env.NODE_ENV === 'production') {
      this.sendToMonitoring(errorInfo);
    }
  }

  static determineSeverity(error) {
    if (error instanceof UserFacingError) {
      return 'warning';
    } else if (error.message.includes('timeout') || error.message.includes('network')) {
      return 'error';
    } else {
      return 'critical';
    }
  }

  static sendToMonitoring(errorInfo) {
    // Implementation depends on your monitoring service
    // Examples: Sentry, DataDog, CloudWatch, etc.
  }
}

export const errorLoggingFlow = ai.defineFlow({
  name: 'errorLoggingFlow',
  inputSchema: z.object({
    operation: z.string(),
    data: z.any()
  }),
  outputSchema: z.object({
    result: z.string(),
    success: z.boolean()
  })
}, async (input, { context }) => {
  try {
    const result = await performOperation(input.operation, input.data);
    
    return {
      result: result.toString(),
      success: true
    };
  } catch (error) {
    // Log error with context
    ErrorLogger.logError(error, {
      operation: input.operation,
      userId: context?.auth?.uid,
      sessionId: context?.session?.id,
      inputData: input.data,
      flowName: 'errorLoggingFlow'
    });

    // Transform error for user
    if (error instanceof UserFacingError) {
      throw error;
    } else {
      throw new UserFacingError('An error occurred while processing your request');
    }
  }
});
```

## Web Framework Integration

### Express.js Error Handling
```javascript
import express from 'express';
import { GenkitError, UserFacingError } from 'genkit/errors';

const app = express();

// Global error handler middleware
app.use((error, req, res, next) => {
  if (error instanceof UserFacingError) {
    // Safe to expose to users
    res.status(400).json({
      error: error.message,
      type: 'user_error'
    });
  } else if (error instanceof GenkitError) {
    // Log but don't expose details
    console.error('Genkit error:', error);
    res.status(500).json({
      error: 'Internal service error',
      type: 'service_error'
    });
  } else {
    // Unknown error - log and return generic message
    console.error('Unexpected error:', error);
    res.status(500).json({
      error: 'An unexpected error occurred',
      type: 'internal_error'
    });
  }
});

// Flow endpoint with error handling
app.post('/api/process', async (req, res, next) => {
  try {
    const result = await errorLoggingFlow(req.body, {
      context: {
        auth: req.user,
        session: req.session
      }
    });
    
    res.json(result);
  } catch (error) {
    next(error); // Pass to error handler middleware
  }
});
```

### Next.js Error Handling
```javascript
// pages/api/genkit-flow.js
import { GenkitError, UserFacingError } from 'genkit/errors';

export default async function handler(req, res) {
  try {
    const result = await yourGenkitFlow(req.body);
    res.status(200).json(result);
  } catch (error) {
    if (error instanceof UserFacingError) {
      res.status(400).json({
        error: error.message,
        type: 'validation_error'
      });
    } else if (error instanceof GenkitError) {
      console.error('Genkit error:', error);
      res.status(500).json({
        error: 'Service temporarily unavailable',
        type: 'service_error'
      });
    } else {
      console.error('Unexpected error:', error);
      res.status(500).json({
        error: 'Internal server error',
        type: 'internal_error'
      });
    }
  }
}
```

## Testing Error Scenarios

### Error Simulation for Testing
```javascript
class ErrorSimulator {
  constructor() {
    this.errorScenarios = new Map();
  }

  registerScenario(name, errorFn) {
    this.errorScenarios.set(name, errorFn);
  }

  simulate(scenarioName, probability = 1.0) {
    if (Math.random() < probability) {
      const errorFn = this.errorScenarios.get(scenarioName);
      if (errorFn) {
        throw errorFn();
      }
    }
  }
}

const errorSimulator = new ErrorSimulator();

// Register test scenarios
errorSimulator.registerScenario('network_timeout', () => 
  new Error('Network timeout')
);

errorSimulator.registerScenario('invalid_input', () => 
  new UserFacingError('Invalid input provided')
);

errorSimulator.registerScenario('service_unavailable', () => 
  new GenkitError('External service unavailable')
);

export const testableFlow = ai.defineFlow({
  name: 'testableFlow',
  inputSchema: z.object({
    data: z.string(),
    simulateError: z.string().optional(),
    errorProbability: z.number().default(0)
  }),
  outputSchema: z.object({
    result: z.string()
  })
}, async (input) => {
  // Simulate errors in test environment
  if (process.env.NODE_ENV === 'test' && input.simulateError) {
    errorSimulator.simulate(input.simulateError, input.errorProbability);
  }

  const { text } = await ai.generate({
    model: googleAI.model('gemini-2.5-flash'),
    prompt: input.data
  });

  return { result: text };
});
```

### Unit Testing Error Handling
```javascript
import { describe, it, expect, beforeEach } from 'vitest';
import { UserFacingError, GenkitError } from 'genkit/errors';

describe('Error Handling', () => {
  beforeEach(() => {
    // Reset error simulator
    errorSimulator.errorScenarios.clear();
  });

  it('should handle user input validation errors', async () => {
    await expect(
      userInputFlow({
        email: 'invalid-email',
        age: 25
      })
    ).rejects.toThrow(UserFacingError);
  });

  it('should retry on transient failures', async () => {
    let attempts = 0;
    const mockOperation = () => {
      attempts++;
      if (attempts < 3) {
        throw new Error('Transient failure');
      }
      return 'success';
    };

    const result = await retryHandler.executeWithRetry(mockOperation);
    expect(result).toBe('success');
    expect(attempts).toBe(3);
  });

  it('should not retry user-facing errors', async () => {
    let attempts = 0;
    const mockOperation = () => {
      attempts++;
      throw new UserFacingError('Invalid input');
    };

    await expect(
      retryHandler.executeWithRetry(mockOperation)
    ).rejects.toThrow(UserFacingError);
    
    expect(attempts).toBe(1); // Should not retry
  });

  it('should activate circuit breaker after threshold', async () => {
    const cb = new CircuitBreaker(2, 60000);
    
    // Trigger failures to open circuit
    for (let i = 0; i < 2; i++) {
      try {
        await cb.execute(() => { throw new Error('Service down'); });
      } catch (e) {}
    }
    
    // Circuit should now be open
    await expect(
      cb.execute(() => 'should not execute')
    ).rejects.toThrow('Service temporarily unavailable');
  });
});
```

## Best Practices

### Error Classification
1. **User Errors**: Use UserFacingError for validation, input, and business logic errors
2. **System Errors**: Use GenkitError for internal failures and plugin issues
3. **External Errors**: Wrap third-party errors appropriately based on context
4. **Security**: Never expose internal system details in user-facing errors

### Error Recovery
1. **Retry Logic**: Implement exponential backoff for transient failures
2. **Circuit Breakers**: Prevent cascade failures in distributed systems
3. **Fallback Strategies**: Provide degraded functionality when possible
4. **Graceful Degradation**: Maintain core functionality during partial failures

### Monitoring and Alerting
1. **Error Tracking**: Log all errors with sufficient context for debugging
2. **Metrics Collection**: Track error rates, types, and patterns
3. **Alerting**: Set up alerts for critical error thresholds
4. **User Communication**: Provide clear, actionable error messages to users

### Testing Strategy
1. **Error Simulation**: Test error scenarios systematically
2. **Recovery Testing**: Verify retry and fallback mechanisms
3. **User Experience**: Test error message clarity and helpfulness
4. **Performance**: Ensure error handling doesn't impact system performance

This comprehensive error handling guide provides the foundation for building robust, reliable Genkit applications that gracefully handle failures and provide excellent user experiences.