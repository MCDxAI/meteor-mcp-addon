# Genkit Tool Calling Guide

## Overview
Tool calling (also known as function calling) in Genkit provides a structured way to give LLMs the ability to make requests back to your application. This enables LLMs to access external information, perform calculations, and take actions in the real world while maintaining type safety and observability.

## Key Use Cases

### Information Access
- **Real-time data**: Stock prices, weather, current events
- **Domain-specific information**: Product catalogs, user profiles, internal databases
- **Dynamic content**: User-generated data, frequently changing information

### Deterministic Operations
- **Calculations**: Mathematical operations the LLM cannot reliably perform
- **Exact text generation**: Terms of service, legal disclaimers, formatted responses
- **Data validation**: Input verification and sanitization

### Action Execution
- **External integrations**: API calls, database updates, file operations
- **IoT control**: Smart home devices, industrial systems
- **Business processes**: Reservations, orders, notifications

## Basic Tool Definition

### Simple Tool Structure
```javascript
import { genkit, z } from 'genkit';
import { googleAI } from '@genkit-ai/google-genai';

const ai = genkit({
  plugins: [googleAI()],
  model: googleAI.model('gemini-2.5-flash'),
});

// Basic weather tool
const getWeather = ai.defineTool(
  {
    name: 'getWeather',
    description: 'Gets the current weather in a given location',
    inputSchema: z.object({
      location: z.string().describe('The location to get the current weather for'),
      units: z.enum(['celsius', 'fahrenheit']).default('fahrenheit')
    }),
    outputSchema: z.string(),
  },
  async (input) => {
    // Your weather API integration
    const weatherData = await fetchWeatherAPI(input.location, input.units);
    return `The current weather in ${input.location} is ${weatherData.temperature}°${input.units === 'celsius' ? 'C' : 'F'} and ${weatherData.condition}.`;
  }
);
```

### Complex Tool with Structured Output
```javascript
const analyzeDocument = ai.defineTool(
  {
    name: 'analyzeDocument',
    description: 'Analyzes a document and extracts key information',
    inputSchema: z.object({
      documentUrl: z.string().describe('URL of the document to analyze'),
      analysisType: z.enum(['summary', 'extraction', 'classification']).describe('Type of analysis to perform'),
      extractFields: z.array(z.string()).optional().describe('Specific fields to extract (for extraction type)')
    }),
    outputSchema: z.object({
      summary: z.string(),
      keyPoints: z.array(z.string()),
      metadata: z.object({
        documentType: z.string(),
        confidence: z.number(),
        processingTime: z.number()
      }),
      extractedData: z.record(z.any()).optional()
    }),
  },
  async (input) => {
    const startTime = Date.now();
    
    // Document processing logic
    const document = await fetchDocument(input.documentUrl);
    const analysis = await processDocument(document, input.analysisType);
    
    let extractedData;
    if (input.analysisType === 'extraction' && input.extractFields) {
      extractedData = await extractSpecificFields(document, input.extractFields);
    }
    
    return {
      summary: analysis.summary,
      keyPoints: analysis.keyPoints,
      metadata: {
        documentType: analysis.type,
        confidence: analysis.confidence,
        processingTime: Date.now() - startTime
      },
      extractedData
    };
  }
);
```

## Using Tools in Generation

### Basic Tool Usage
```javascript
// Simple generation with tools
const response = await ai.generate({
  prompt: "What's the weather like in San Francisco?",
  tools: [getWeather],
});

console.log(response.text);
```

### Multiple Tools
```javascript
const calculator = ai.defineTool(
  {
    name: 'calculator',
    description: 'Perform mathematical calculations',
    inputSchema: z.object({
      expression: z.string().describe('Mathematical expression to evaluate'),
    }),
    outputSchema: z.number(),
  },
  async (input) => {
    // Safe math evaluation (use a proper math parser in production)
    return evaluateMathExpression(input.expression);
  }
);

const stockLookup = ai.defineTool(
  {
    name: 'stockLookup',
    description: 'Get current stock price and metrics',
    inputSchema: z.object({
      symbol: z.string().describe('Stock symbol (e.g., AAPL)'),
    }),
    outputSchema: z.object({
      price: z.number(),
      change: z.number(),
      volume: z.number(),
    }),
  },
  async (input) => {
    const stockData = await fetchStockData(input.symbol);
    return {
      price: stockData.currentPrice,
      change: stockData.dailyChange,
      volume: stockData.volume
    };
  }
);

// Use multiple tools together
const response = await ai.generate({
  prompt: "Calculate the total value of my portfolio: 100 shares of AAPL and 50 shares of GOOGL. What's the percentage allocation?",
  tools: [calculator, stockLookup],
  maxTurns: 10, // Allow multiple tool interactions
});
```

## Tool Usage in Flows

### Flow with Tools
```javascript
export const researchFlow = ai.defineFlow({
  name: 'researchFlow',
  inputSchema: z.object({
    topic: z.string(),
    depth: z.enum(['basic', 'detailed', 'comprehensive']).default('detailed'),
    includeCalculations: z.boolean().default(false)
  }),
  outputSchema: z.object({
    research: z.string(),
    sources: z.array(z.string()),
    calculations: z.array(z.object({
      description: z.string(),
      result: z.number()
    })).optional()
  })
}, async (input) => {
  const tools = [webSearch, documentAnalyzer];
  if (input.includeCalculations) {
    tools.push(calculator);
  }

  const { text, toolCalls } = await ai.generate({
    prompt: `Research ${input.topic} with ${input.depth} analysis. ${
      input.includeCalculations ? 'Include relevant calculations.' : ''
    }`,
    tools,
    maxTurns: input.depth === 'comprehensive' ? 15 : 8
  });

  // Extract sources and calculations from tool calls
  const sources = toolCalls
    ?.filter(call => call.toolRequest.name === 'webSearch')
    .map(call => call.toolRequest.input.url) || [];
    
  const calculations = toolCalls
    ?.filter(call => call.toolRequest.name === 'calculator')
    .map(call => ({
      description: call.toolRequest.input.expression,
      result: call.toolResponse.output
    })) || [];

  return {
    research: text,
    sources,
    calculations: calculations.length > 0 ? calculations : undefined
  };
});
```

### Streaming with Tools
```javascript
export const streamingAnalysisFlow = ai.defineFlow({
  name: 'streamingAnalysisFlow',
  inputSchema: z.object({
    query: z.string(),
    enableTools: z.boolean().default(true)
  }),
  streamSchema: z.object({
    type: z.enum(['text', 'tool_call', 'tool_result']),
    content: z.string(),
    toolName: z.string().optional(),
    toolInput: z.any().optional(),
    toolOutput: z.any().optional()
  }),
  outputSchema: z.object({
    response: z.string(),
    toolsUsed: z.array(z.string())
  })
}, async (input, { sendChunk }) => {
  const tools = input.enableTools ? [webSearch, calculator, documentAnalyzer] : [];
  const toolsUsed = [];

  const { stream, response } = ai.generateStream({
    prompt: input.query,
    tools,
    maxTurns: 8
  });

  let fullResponse = '';

  for await (const chunk of stream) {
    if (chunk.text()) {
      const text = chunk.text();
      fullResponse += text;
      sendChunk({
        type: 'text',
        content: text
      });
    }

    // Handle tool calls in streaming
    if (chunk.toolRequests) {
      for (const toolRequest of chunk.toolRequests) {
        sendChunk({
          type: 'tool_call',
          content: `Using ${toolRequest.name}...`,
          toolName: toolRequest.name,
          toolInput: toolRequest.input
        });
        toolsUsed.push(toolRequest.name);
      }
    }
    
    if (chunk.toolResponses) {
      for (const toolResponse of chunk.toolResponses) {
        sendChunk({
          type: 'tool_result',
          content: `${toolResponse.name} completed`,
          toolName: toolResponse.name,
          toolOutput: toolResponse.output
        });
      }
    }
  }

  return {
    response: fullResponse,
    toolsUsed: [...new Set(toolsUsed)]
  };
});
```

## Advanced Tool Patterns

### Dynamic Tool Definition
```javascript
export const dynamicAPIFlow = ai.defineFlow({
  name: 'dynamicAPIFlow',
  inputSchema: z.object({
    apiEndpoint: z.string(),
    apiKey: z.string(),
    operation: z.string(),
    parameters: z.record(z.any())
  }),
  outputSchema: z.object({
    result: z.any(),
    success: z.boolean()
  })
}, async (input) => {
  // Create tool dynamically based on API endpoint
  const dynamicApiTool = ai.dynamicTool(
    {
      name: 'dynamicApiCall',
      description: `Call ${input.operation} on ${input.apiEndpoint}`,
      inputSchema: z.object({
        params: z.record(z.any())
      }),
      outputSchema: z.object({
        data: z.any(),
        status: z.number()
      })
    },
    async (toolInput) => {
      const response = await fetch(input.apiEndpoint, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${input.apiKey}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          operation: input.operation,
          ...toolInput.params
        })
      });

      const data = await response.json();
      return {
        data,
        status: response.status
      };
    }
  );

  const { output } = await ai.generate({
    prompt: `Execute ${input.operation} with the provided parameters`,
    tools: [dynamicApiTool],
    output: {
      schema: z.object({
        result: z.any(),
        success: z.boolean()
      })
    }
  });

  return output;
});
```

### Tool Interrupts for User Confirmation
```javascript
const deleteUserData = ai.defineTool({
  name: 'deleteUserData',
  description: 'Delete user data (requires confirmation)',
  inputSchema: z.object({
    userId: z.string(),
    dataType: z.enum(['profile', 'history', 'all']),
    reason: z.string()
  }),
  outputSchema: z.object({
    confirmed: z.boolean(),
    message: z.string()
  })
  // No implementation - this creates an interrupt
});

export const dataManagementFlow = ai.defineFlow({
  name: 'dataManagementFlow',
  inputSchema: z.object({
    action: z.string(),
    userId: z.string(),
    autoApprove: z.boolean().default(false)
  }),
  outputSchema: z.object({
    result: z.string(),
    requiresApproval: z.boolean(),
    approvalId: z.string().optional()
  })
}, async (input) => {
  if (input.action.includes('delete') && !input.autoApprove) {
    // This will create an interrupt for manual approval
    const response = await ai.generate({
      prompt: `User requests to ${input.action} for user ${input.userId}`,
      tools: [deleteUserData],
      returnToolRequests: true
    });

    if (response.toolRequests?.length > 0) {
      return {
        result: 'Approval required',
        requiresApproval: true,
        approvalId: generateApprovalId()
      };
    }
  }

  // Process approved or auto-approved actions
  const result = await processDataAction(input.action, input.userId);
  
  return {
    result: `Action completed: ${result}`,
    requiresApproval: false
  };
});
```

### Explicit Tool Call Handling
```javascript
export const customToolHandlingFlow = ai.defineFlow({
  name: 'customToolHandlingFlow',
  inputSchema: z.object({
    query: z.string(),
    maxIterations: z.number().default(5)
  }),
  outputSchema: z.object({
    response: z.string(),
    iterations: z.number(),
    toolCallHistory: z.array(z.any())
  })
}, async (input) => {
  const tools = [webSearch, calculator, documentAnalyzer];
  const toolCallHistory = [];
  let iterations = 0;
  
  let generateOptions = {
    prompt: input.query,
    tools,
    returnToolRequests: true,
  };

  let llmResponse;
  
  while (iterations < input.maxIterations) {
    llmResponse = await ai.generate(generateOptions);
    iterations++;
    
    const toolRequests = llmResponse.toolRequests;
    if (!toolRequests || toolRequests.length === 0) {
      break;
    }

    // Custom tool call processing with logging
    const toolResponses = await Promise.all(
      toolRequests.map(async (request) => {
        const startTime = Date.now();
        
        try {
          let output;
          switch (request.toolRequest.name) {
            case 'webSearch':
              output = await webSearch(request.toolRequest.input);
              break;
            case 'calculator':
              output = await calculator(request.toolRequest.input);
              break;
            case 'documentAnalyzer':
              output = await documentAnalyzer(request.toolRequest.input);
              break;
            default:
              throw new Error(`Unknown tool: ${request.toolRequest.name}`);
          }

          const toolCall = {
            tool: request.toolRequest.name,
            input: request.toolRequest.input,
            output,
            duration: Date.now() - startTime,
            success: true
          };
          
          toolCallHistory.push(toolCall);

          return {
            toolResponse: {
              name: request.toolRequest.name,
              ref: request.toolRequest.ref,
              output,
            },
          };
        } catch (error) {
          const toolCall = {
            tool: request.toolRequest.name,
            input: request.toolRequest.input,
            error: error.message,
            duration: Date.now() - startTime,
            success: false
          };
          
          toolCallHistory.push(toolCall);
          
          return {
            toolResponse: {
              name: request.toolRequest.name,
              ref: request.toolRequest.ref,
              output: `Error: ${error.message}`,
            },
          };
        }
      })
    );

    // Update options for next iteration
    generateOptions = {
      messages: llmResponse.messages,
      prompt: toolResponses,
      tools,
      returnToolRequests: true,
    };
  }

  return {
    response: llmResponse?.text || 'No response generated',
    iterations,
    toolCallHistory
  };
});
```

## Tool Integration with Prompts

### Prompt Files with Tools
```yaml
---
tools: [getWeather, calculator]
maxTurns: 8
input:
  schema:
    location: string
    calculation?: string
---
What is the weather in {{location}}?
{{#if calculation}}
Also, please calculate: {{calculation}}
{{/if}}
```

### Context-Aware Tool Usage
```javascript
const contextAwareTool = ai.defineTool(
  {
    name: 'getUserData',
    description: 'Retrieve user-specific data and preferences',
    inputSchema: z.object({
      dataType: z.enum(['profile', 'preferences', 'history']),
      includePrivate: z.boolean().default(false)
    }),
    outputSchema: z.object({
      data: z.any(),
      lastUpdated: z.string()
    })
  },
  async (input, { context }) => {
    // Use context for authentication and authorization
    const userId = context.auth?.uid;
    if (!userId) {
      throw new Error('Authentication required');
    }

    // Check permissions for private data
    if (input.includePrivate && !context.auth.roles?.includes('verified_user')) {
      throw new Error('Insufficient permissions for private data');
    }

    const userData = await fetchUserData(userId, input.dataType, input.includePrivate);
    
    return {
      data: userData.data,
      lastUpdated: userData.lastUpdated
    };
  }
);
```

## Error Handling and Best Practices

### Robust Tool Implementation
```javascript
const robustApiTool = ai.defineTool(
  {
    name: 'robustApiCall',
    description: 'Make API calls with retry logic and error handling',
    inputSchema: z.object({
      endpoint: z.string(),
      method: z.enum(['GET', 'POST', 'PUT', 'DELETE']).default('GET'),
      data: z.any().optional(),
      retries: z.number().default(3)
    }),
    outputSchema: z.object({
      success: z.boolean(),
      data: z.any().optional(),
      error: z.string().optional(),
      attempts: z.number()
    })
  },
  async (input) => {
    let lastError;
    
    for (let attempt = 1; attempt <= input.retries; attempt++) {
      try {
        const response = await fetch(input.endpoint, {
          method: input.method,
          headers: {
            'Content-Type': 'application/json',
          },
          body: input.data ? JSON.stringify(input.data) : undefined,
        });

        if (!response.ok) {
          throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        const data = await response.json();
        
        return {
          success: true,
          data,
          attempts: attempt
        };
      } catch (error) {
        lastError = error;
        
        if (attempt < input.retries) {
          // Exponential backoff
          await new Promise(resolve => setTimeout(resolve, 1000 * Math.pow(2, attempt - 1)));
        }
      }
    }

    return {
      success: false,
      error: lastError?.message || 'Unknown error',
      attempts: input.retries
    };
  }
);
```

### Tool Validation and Security
```javascript
const secureDataTool = ai.defineTool(
  {
    name: 'secureDataAccess',
    description: 'Access sensitive data with security checks',
    inputSchema: z.object({
      resourceId: z.string(),
      operation: z.enum(['read', 'write', 'delete']),
      data: z.any().optional()
    }),
    outputSchema: z.object({
      success: z.boolean(),
      data: z.any().optional(),
      message: z.string()
    })
  },
  async (input, { context }) => {
    // Validate authentication
    if (!context.auth?.uid) {
      return {
        success: false,
        message: 'Authentication required'
      };
    }

    // Validate authorization
    const hasPermission = await checkResourcePermission(
      context.auth.uid,
      input.resourceId,
      input.operation
    );
    
    if (!hasPermission) {
      return {
        success: false,
        message: 'Insufficient permissions'
      };
    }

    // Validate input data
    if (input.operation === 'write' && !input.data) {
      return {
        success: false,
        message: 'Data required for write operation'
      };
    }

    // Perform operation
    try {
      let result;
      switch (input.operation) {
        case 'read':
          result = await readResource(input.resourceId);
          break;
        case 'write':
          result = await writeResource(input.resourceId, input.data);
          break;
        case 'delete':
          result = await deleteResource(input.resourceId);
          break;
      }

      // Log the operation
      await logSecurityEvent({
        userId: context.auth.uid,
        operation: input.operation,
        resourceId: input.resourceId,
        timestamp: new Date().toISOString(),
        success: true
      });

      return {
        success: true,
        data: result,
        message: `${input.operation} operation completed successfully`
      };
    } catch (error) {
      // Log the error
      await logSecurityEvent({
        userId: context.auth.uid,
        operation: input.operation,
        resourceId: input.resourceId,
        timestamp: new Date().toISOString(),
        success: false,
        error: error.message
      });

      return {
        success: false,
        message: `Operation failed: ${error.message}`
      };
    }
  }
);
```

### Performance Optimization
```javascript
class ToolPerformanceManager {
  constructor() {
    this.cache = new Map();
    this.metrics = new Map();
  }

  createCachedTool(toolDefinition, implementation, cacheOptions = {}) {
    const { ttl = 300000, keyGenerator } = cacheOptions; // 5 minutes default

    return ai.defineTool(
      toolDefinition,
      async (input, context) => {
        const cacheKey = keyGenerator ? 
          keyGenerator(input, context) : 
          JSON.stringify(input);
        
        // Check cache
        const cached = this.cache.get(cacheKey);
        if (cached && Date.now() - cached.timestamp < ttl) {
          this.recordMetric(toolDefinition.name, 'cache_hit');
          return cached.result;
        }

        // Execute tool
        const startTime = Date.now();
        try {
          const result = await implementation(input, context);
          const duration = Date.now() - startTime;
          
          // Cache result
          this.cache.set(cacheKey, {
            result,
            timestamp: Date.now()
          });
          
          this.recordMetric(toolDefinition.name, 'execution', duration);
          return result;
        } catch (error) {
          this.recordMetric(toolDefinition.name, 'error');
          throw error;
        }
      }
    );
  }

  recordMetric(toolName, type, value = 1) {
    if (!this.metrics.has(toolName)) {
      this.metrics.set(toolName, {
        executions: 0,
        cache_hits: 0,
        errors: 0,
        total_duration: 0,
        avg_duration: 0
      });
    }

    const metrics = this.metrics.get(toolName);
    
    switch (type) {
      case 'execution':
        metrics.executions++;
        metrics.total_duration += value;
        metrics.avg_duration = metrics.total_duration / metrics.executions;
        break;
      case 'cache_hit':
        metrics.cache_hits++;
        break;
      case 'error':
        metrics.errors++;
        break;
    }
  }

  getMetrics(toolName) {
    return this.metrics.get(toolName) || null;
  }

  getAllMetrics() {
    return Object.fromEntries(this.metrics);
  }
}

// Usage
const performanceManager = new ToolPerformanceManager();

const cachedWeatherTool = performanceManager.createCachedTool(
  {
    name: 'cachedWeather',
    description: 'Get weather with caching',
    inputSchema: z.object({
      location: z.string()
    }),
    outputSchema: z.string()
  },
  async (input) => {
    const weather = await fetchWeatherAPI(input.location);
    return `Weather in ${input.location}: ${weather.description}`;
  },
  {
    ttl: 600000, // 10 minutes
    keyGenerator: (input) => `weather:${input.location.toLowerCase()}`
  }
);
```

## Integration with Existing Guides

### Tool Calling + Structured Output
```javascript
const structuredAnalysisTool = ai.defineTool(
  {
    name: 'structuredAnalysis',
    description: 'Perform structured data analysis',
    inputSchema: z.object({
      data: z.array(z.any()),
      analysisType: z.enum(['statistical', 'trend', 'comparative'])
    }),
    outputSchema: z.object({
      summary: z.string(),
      insights: z.array(z.object({
        category: z.string(),
        finding: z.string(),
        confidence: z.number(),
        supporting_data: z.array(z.any())
      })),
      recommendations: z.array(z.string()),
      metadata: z.object({
        sample_size: z.number(),
        analysis_duration: z.number(),
        quality_score: z.number()
      })
    })
  },
  async (input) => {
    const startTime = Date.now();
    
    // Perform analysis based on type
    const analysis = await performDataAnalysis(input.data, input.analysisType);
    
    return {
      summary: analysis.summary,
      insights: analysis.insights.map(insight => ({
        category: insight.category,
        finding: insight.description,
        confidence: insight.confidence,
        supporting_data: insight.evidence
      })),
      recommendations: analysis.recommendations,
      metadata: {
        sample_size: input.data.length,
        analysis_duration: Date.now() - startTime,
        quality_score: analysis.qualityScore
      }
    };
  }
);
```

### Tool Calling + Context Integration
```javascript
const contextAwareRecommendationTool = ai.defineTool(
  {
    name: 'personalizedRecommendations',
    description: 'Generate personalized recommendations based on user context',
    inputSchema: z.object({
      category: z.string(),
      preferences: z.record(z.any()).optional(),
      limit: z.number().default(5)
    }),
    outputSchema: z.array(z.object({
      item: z.string(),
      score: z.number(),
      reasoning: z.string(),
      personalization_factors: z.array(z.string())
    }))
  },
  async (input, { context }) => {
    // Use context for personalization
    const userId = context.auth?.uid;
    const userProfile = userId ? await getUserProfile(userId) : null;
    
    // Combine explicit preferences with user profile
    const effectivePreferences = {
      ...userProfile?.preferences,
      ...input.preferences
    };
    
    // Generate recommendations
    const recommendations = await generateRecommendations({
      category: input.category,
      preferences: effectivePreferences,
      userHistory: userProfile?.history,
      limit: input.limit
    });
    
    return recommendations.map(rec => ({
      item: rec.name,
      score: rec.relevanceScore,
      reasoning: rec.explanation,
      personalization_factors: rec.personalizationFactors
    }));
  }
);
```

## Best Practices Summary

### Tool Design
1. **Clear naming**: Use descriptive, action-oriented tool names
2. **Detailed descriptions**: Provide comprehensive tool descriptions with examples
3. **Strong typing**: Use Zod schemas for input/output validation
4. **Error handling**: Implement proper error handling and user-friendly messages

### Performance
1. **Caching**: Cache expensive operations when appropriate
2. **Batch operations**: Group related operations when possible
3. **Resource limits**: Set appropriate limits on tool execution
4. **Monitoring**: Track tool usage and performance metrics

### Security
1. **Context validation**: Always validate authentication and authorization
2. **Input sanitization**: Sanitize and validate all tool inputs
3. **Rate limiting**: Implement rate limiting for expensive operations
4. **Audit logging**: Log all tool usage for security and debugging

### Integration
1. **Tool selection**: Choose appropriate tools for each use case
2. **Context awareness**: Use context for personalization and security
3. **Error recovery**: Provide fallback options when tools fail
4. **Observability**: Monitor tool usage and performance in production