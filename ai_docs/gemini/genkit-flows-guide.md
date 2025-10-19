# Genkit Flows Guide

## Overview
Flows are the core building blocks of Genkit applications - they're functions with built-in observability, type safety, and tooling integration. Flows wrap your AI logic to provide debugging, deployment, and monitoring capabilities while maintaining clean, testable code.

## Why Use Flows?

### Key Benefits
1. **Type Safety**: Input and output schemas with Zod validation
2. **Developer UI Integration**: Interactive testing and debugging
3. **Simplified Deployment**: Direct deployment as web API endpoints
4. **Observability**: Automatic tracing and step-by-step debugging
5. **Streaming Support**: Built-in streaming capabilities
6. **Context Propagation**: Automatic context passing through nested calls

## Basic Flow Definition

### Simple Flow Structure
```javascript
import { genkit, z } from 'genkit';
import { googleAI } from '@genkit-ai/google-genai';

const ai = genkit({
  plugins: [googleAI()],
});

// Basic flow with input/output schemas
export const menuSuggestionFlow = ai.defineFlow({
  name: 'menuSuggestionFlow',
  inputSchema: z.object({ 
    theme: z.string().describe('Restaurant theme (e.g., Italian, Mexican)'),
    dietaryRestrictions: z.array(z.string()).optional()
  }),
  outputSchema: z.object({ 
    menuItem: z.string(),
    description: z.string(),
    estimatedPrice: z.number()
  }),
}, async (input) => {
  const restrictions = input.dietaryRestrictions?.join(', ') || 'none';
  
  const { text } = await ai.generate({
    model: googleAI.model('gemini-2.5-flash'),
    prompt: `Create a menu item for a ${input.theme} themed restaurant. 
             Dietary restrictions: ${restrictions}
             Include name, description, and estimated price.`,
  });

  // Parse the response (in real app, use structured output)
  return {
    menuItem: "Generated Menu Item",
    description: text,
    estimatedPrice: 12.99
  };
});
```

### Flow with Structured Output
```javascript
const MenuItemSchema = z.object({
  dishName: z.string(),
  description: z.string(),
  ingredients: z.array(z.string()),
  price: z.number(),
  calories: z.number().optional(),
  allergens: z.array(z.string()).optional(),
  spiceLevel: z.enum(['mild', 'medium', 'hot']).optional()
});

export const structuredMenuFlow = ai.defineFlow({
  name: 'structuredMenuFlow',
  inputSchema: z.object({ 
    theme: z.string(),
    priceRange: z.enum(['budget', 'mid-range', 'premium']).default('mid-range')
  }),
  outputSchema: MenuItemSchema,
}, async (input) => {
  const { output } = await ai.generate({
    model: googleAI.model('gemini-2.5-flash'),
    prompt: `Create a ${input.priceRange} ${input.theme} menu item`,
    output: { schema: MenuItemSchema },
  });

  if (!output) {
    throw new Error("Failed to generate menu item");
  }

  return output;
});
```

## Input and Output Schema Best Practices

### Object-Based Schemas (Recommended)
```javascript
// ✅ GOOD: Object-based schemas
const goodFlow = ai.defineFlow({
  name: 'goodFlow',
  inputSchema: z.object({
    query: z.string().describe('User query'),
    options: z.object({
      includeImages: z.boolean().default(false),
      maxResults: z.number().default(10)
    }).optional()
  }),
  outputSchema: z.object({
    results: z.array(z.string()),
    totalFound: z.number(),
    processingTime: z.number()
  })
}, async (input) => {
  // Implementation
  return {
    results: ["result1", "result2"],
    totalFound: 2,
    processingTime: 150
  };
});

// ❌ AVOID: Primitive schemas
const avoidFlow = ai.defineFlow({
  name: 'avoidFlow',
  inputSchema: z.string(),  // Less flexible
  outputSchema: z.string()  // No structure
}, async (input) => {
  return "simple string";
});
```

### Schema Composition and Reuse
```javascript
// Reusable schemas
const UserSchema = z.object({
  id: z.string(),
  email: z.string().email(),
  name: z.string(),
  roles: z.array(z.string())
});

const PaginationSchema = z.object({
  page: z.number().min(1).default(1),
  limit: z.number().min(1).max(100).default(20),
  sortBy: z.string().optional(),
  sortOrder: z.enum(['asc', 'desc']).default('asc')
});

const SearchResultSchema = z.object({
  items: z.array(UserSchema),
  pagination: z.object({
    currentPage: z.number(),
    totalPages: z.number(),
    totalItems: z.number(),
    hasNext: z.boolean(),
    hasPrev: z.boolean()
  }),
  searchTerm: z.string(),
  executionTime: z.number()
});

// Composed flow using reusable schemas
export const searchUsersFlow = ai.defineFlow({
  name: 'searchUsersFlow',
  inputSchema: z.object({
    searchTerm: z.string().min(1),
    filters: z.object({
      roles: z.array(z.string()).optional(),
      verified: z.boolean().optional(),
      createdAfter: z.string().datetime().optional()
    }).optional(),
    pagination: PaginationSchema.optional()
  }),
  outputSchema: SearchResultSchema
}, async (input) => {
  const startTime = Date.now();
  
  // Search implementation
  const users = await searchUsers({
    term: input.searchTerm,
    filters: input.filters,
    pagination: input.pagination
  });
  
  return {
    items: users.items,
    pagination: {
      currentPage: users.page,
      totalPages: Math.ceil(users.total / users.limit),
      totalItems: users.total,
      hasNext: users.page < Math.ceil(users.total / users.limit),
      hasPrev: users.page > 1
    },
    searchTerm: input.searchTerm,
    executionTime: Date.now() - startTime
  };
});
```

## Streaming Flows

### Basic Streaming Flow
```javascript
export const streamingChatFlow = ai.defineFlow({
  name: 'streamingChatFlow',
  inputSchema: z.object({
    message: z.string(),
    conversationId: z.string().optional()
  }),
  streamSchema: z.object({
    chunk: z.string(),
    type: z.enum(['text', 'thinking', 'tool_call']).default('text')
  }),
  outputSchema: z.object({
    response: z.string(),
    conversationId: z.string(),
    tokensUsed: z.number()
  }),
}, async (input, { sendChunk }) => {
  const conversationId = input.conversationId || generateId();
  
  // Load conversation history if needed
  const history = input.conversationId 
    ? await getConversationHistory(conversationId)
    : [];

  const { stream, response } = ai.generateStream({
    model: googleAI.model('gemini-2.5-flash'),
    prompt: input.message,
    // Add history to context if needed
  });

  let fullResponse = '';
  
  for await (const chunk of stream) {
    const text = chunk.text();
    fullResponse += text;
    
    // Send chunk to client with metadata
    sendChunk({
      chunk: text,
      type: 'text'
    });
  }

  // Save conversation
  await saveConversation(conversationId, input.message, fullResponse);

  const finalResponse = await response;
  
  return {
    response: fullResponse,
    conversationId,
    tokensUsed: finalResponse.usage?.totalTokens || 0
  };
});
```

### Advanced Streaming with Tool Calls
```javascript
const streamingToolFlow = ai.defineFlow({
  name: 'streamingToolFlow',
  inputSchema: z.object({
    query: z.string(),
    enableTools: z.boolean().default(true)
  }),
  streamSchema: z.object({
    type: z.enum(['text', 'tool_call', 'tool_result', 'thinking']),
    content: z.string(),
    toolName: z.string().optional(),
    toolInput: z.any().optional(),
    toolOutput: z.any().optional()
  }),
  outputSchema: z.object({
    finalAnswer: z.string(),
    toolsUsed: z.array(z.string()),
    processingSteps: z.number()
  })
}, async (input, { sendChunk }) => {
  const tools = input.enableTools ? [weatherTool, calculatorTool] : [];
  const toolsUsed = [];
  let processingSteps = 0;

  const { stream, response } = ai.generateStream({
    model: googleAI.model('gemini-2.5-flash'),
    prompt: input.query,
    tools
  });

  let finalAnswer = '';

  for await (const chunk of stream) {
    processingSteps++;
    
    if (chunk.text()) {
      const text = chunk.text();
      finalAnswer += text;
      
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
          content: `Calling ${toolRequest.name}...`,
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
          content: `Tool ${toolResponse.name} completed`,
          toolName: toolResponse.name,
          toolOutput: toolResponse.output
        });
      }
    }
  }

  return {
    finalAnswer,
    toolsUsed,
    processingSteps
  };
});
```

## Calling Flows

### Basic Flow Execution
```javascript
// Simple flow call
const result = await menuSuggestionFlow({
  theme: 'Italian',
  dietaryRestrictions: ['vegetarian']
});

console.log(result.menuItem); // Fully typed response

// With error handling
try {
  const result = await structuredMenuFlow({
    theme: 'Japanese',
    priceRange: 'premium'
  });
  
  console.log(`${result.dishName}: ${result.description}`);
  console.log(`Price: $${result.price}`);
} catch (error) {
  console.error('Flow execution failed:', error.message);
}
```

### Streaming Flow Execution
```javascript
// Streaming flow execution
const streamResponse = streamingChatFlow.stream({
  message: "Tell me about quantum computing",
  conversationId: "conv_123"
});

// Process streaming chunks
for await (const chunk of streamResponse.stream) {
  console.log(`[${chunk.type}] ${chunk.chunk}`);
  
  // Update UI with streaming content
  updateChatUI(chunk);
}

// Get final result
const finalResult = await streamResponse.output;
console.log('Final response:', finalResult.response);
console.log('Tokens used:', finalResult.tokensUsed);
```

## Flow Steps and Observability

### Adding Custom Steps with ai.run()
```javascript
export const dataProcessingFlow = ai.defineFlow({
  name: 'dataProcessingFlow',
  inputSchema: z.object({
    dataSource: z.string(),
    processingType: z.enum(['clean', 'analyze', 'transform'])
  }),
  outputSchema: z.object({
    processedData: z.any(),
    statistics: z.object({
      recordsProcessed: z.number(),
      errors: z.number(),
      processingTime: z.number()
    })
  })
}, async (input) => {
  // Step 1: Data retrieval (tracked in Developer UI)
  const rawData = await ai.run('fetch-data', async () => {
    console.log(`Fetching data from ${input.dataSource}`);
    return await fetchDataFromSource(input.dataSource);
  });

  // Step 2: Data validation (tracked in Developer UI)
  const validatedData = await ai.run('validate-data', async () => {
    console.log('Validating data structure');
    return validateDataStructure(rawData);
  });

  // Step 3: AI-powered processing
  const { output } = await ai.generate({
    model: googleAI.model('gemini-2.5-flash'),
    prompt: `Process this data using ${input.processingType} method: ${JSON.stringify(validatedData.sample)}`,
    output: { 
      schema: z.object({
        processedSample: z.any(),
        processingInstructions: z.string()
      })
    }
  });

  // Step 4: Apply processing (tracked in Developer UI)
  const processedData = await ai.run('apply-processing', async () => {
    console.log('Applying AI-generated processing instructions');
    return applyProcessingInstructions(validatedData.data, output.processingInstructions);
  });

  // Step 5: Generate statistics (tracked in Developer UI)
  const statistics = await ai.run('generate-statistics', async () => {
    return {
      recordsProcessed: processedData.length,
      errors: processedData.filter(r => r.error).length,
      processingTime: Date.now() - startTime
    };
  });

  return {
    processedData,
    statistics
  };
});
```

### Complex Multi-Step Flow
```javascript
export const complexAnalysisFlow = ai.defineFlow({
  name: 'complexAnalysisFlow',
  inputSchema: z.object({
    documents: z.array(z.string()),
    analysisType: z.enum(['sentiment', 'summary', 'classification']),
    options: z.object({
      includeConfidence: z.boolean().default(true),
      batchSize: z.number().default(10)
    }).optional()
  }),
  outputSchema: z.object({
    results: z.array(z.object({
      documentId: z.string(),
      analysis: z.any(),
      confidence: z.number().optional(),
      processingTime: z.number()
    })),
    summary: z.object({
      totalDocuments: z.number(),
      successfulAnalyses: z.number(),
      averageConfidence: z.number().optional(),
      totalProcessingTime: z.number()
    })
  })
}, async (input) => {
  const startTime = Date.now();
  const results = [];
  
  // Step 1: Document preprocessing
  const preprocessedDocs = await ai.run('preprocess-documents', async () => {
    return input.documents.map((doc, index) => ({
      id: `doc_${index}`,
      content: doc,
      length: doc.length
    }));
  });

  // Step 2: Batch processing
  const batchSize = input.options?.batchSize || 10;
  const batches = chunkArray(preprocessedDocs, batchSize);
  
  for (let batchIndex = 0; batchIndex < batches.length; batchIndex++) {
    const batch = batches[batchIndex];
    
    const batchResults = await ai.run(`process-batch-${batchIndex}`, async () => {
      const batchPromises = batch.map(async (doc) => {
        const docStartTime = Date.now();
        
        try {
          const { output } = await ai.generate({
            model: googleAI.model('gemini-2.5-flash'),
            prompt: `Perform ${input.analysisType} analysis on: ${doc.content}`,
            output: {
              schema: z.object({
                analysis: z.any(),
                confidence: z.number().min(0).max(1).optional()
              })
            }
          });

          return {
            documentId: doc.id,
            analysis: output.analysis,
            confidence: input.options?.includeConfidence ? output.confidence : undefined,
            processingTime: Date.now() - docStartTime
          };
        } catch (error) {
          return {
            documentId: doc.id,
            analysis: { error: error.message },
            confidence: 0,
            processingTime: Date.now() - docStartTime
          };
        }
      });

      return await Promise.all(batchPromises);
    });

    results.push(...batchResults);
  }

  // Step 3: Generate summary statistics
  const summary = await ai.run('generate-summary', async () => {
    const successful = results.filter(r => !r.analysis.error);
    const confidenceScores = successful
      .map(r => r.confidence)
      .filter(c => c !== undefined);

    return {
      totalDocuments: results.length,
      successfulAnalyses: successful.length,
      averageConfidence: confidenceScores.length > 0 
        ? confidenceScores.reduce((a, b) => a + b, 0) / confidenceScores.length
        : undefined,
      totalProcessingTime: Date.now() - startTime
    };
  });

  return { results, summary };
});
```

## Flow Composition and Reusability

### Composing Flows
```javascript
// Base flows for reuse
const documentAnalysisFlow = ai.defineFlow({
  name: 'documentAnalysisFlow',
  inputSchema: z.object({
    document: z.string(),
    analysisType: z.string()
  }),
  outputSchema: z.object({
    analysis: z.string(),
    confidence: z.number()
  })
}, async (input) => {
  const { output } = await ai.generate({
    model: googleAI.model('gemini-2.5-flash'),
    prompt: `Analyze this document for ${input.analysisType}: ${input.document}`,
    output: {
      schema: z.object({
        analysis: z.string(),
        confidence: z.number()
      })
    }
  });
  
  return output;
});

const summaryGenerationFlow = ai.defineFlow({
  name: 'summaryGenerationFlow',
  inputSchema: z.object({
    analyses: z.array(z.string()),
    summaryType: z.string()
  }),
  outputSchema: z.object({
    summary: z.string()
  })
}, async (input) => {
  const { text } = await ai.generate({
    model: googleAI.model('gemini-2.5-flash'),
    prompt: `Create a ${input.summaryType} summary from these analyses: ${input.analyses.join('\n\n')}`
  });
  
  return { summary: text };
});

// Composite flow using base flows
export const comprehensiveReportFlow = ai.defineFlow({
  name: 'comprehensiveReportFlow',
  inputSchema: z.object({
    documents: z.array(z.string()),
    reportType: z.enum(['executive', 'technical', 'summary'])
  }),
  outputSchema: z.object({
    individualAnalyses: z.array(z.object({
      document: z.string(),
      analysis: z.string(),
      confidence: z.number()
    })),
    overallSummary: z.string(),
    recommendations: z.array(z.string()),
    metadata: z.object({
      documentsProcessed: z.number(),
      averageConfidence: z.number(),
      processingTime: z.number()
    })
  })
}, async (input) => {
  const startTime = Date.now();
  
  // Step 1: Analyze each document using composed flow
  const individualAnalyses = [];
  for (const doc of input.documents) {
    const analysis = await documentAnalysisFlow({
      document: doc,
      analysisType: input.reportType
    });
    
    individualAnalyses.push({
      document: doc.substring(0, 100) + '...',
      analysis: analysis.analysis,
      confidence: analysis.confidence
    });
  }

  // Step 2: Generate overall summary using composed flow
  const summaryResult = await summaryGenerationFlow({
    analyses: individualAnalyses.map(a => a.analysis),
    summaryType: input.reportType
  });

  // Step 3: Generate recommendations
  const { output: recommendations } = await ai.generate({
    model: googleAI.model('gemini-2.5-flash'),
    prompt: `Based on this summary, provide 3-5 actionable recommendations: ${summaryResult.summary}`,
    output: {
      schema: z.object({
        recommendations: z.array(z.string())
      })
    }
  });

  // Step 4: Calculate metadata
  const avgConfidence = individualAnalyses.reduce((sum, a) => sum + a.confidence, 0) / individualAnalyses.length;

  return {
    individualAnalyses,
    overallSummary: summaryResult.summary,
    recommendations: recommendations.recommendations,
    metadata: {
      documentsProcessed: input.documents.length,
      averageConfidence: avgConfidence,
      processingTime: Date.now() - startTime
    }
  };
});
```

## Flow Deployment

### Express.js Deployment
```javascript
import { startFlowServer } from '@genkit-ai/express';

// Define your flows
export const chatFlow = ai.defineFlow({
  name: 'chatFlow',
  inputSchema: z.object({
    message: z.string(),
    sessionId: z.string().optional()
  }),
  outputSchema: z.object({
    response: z.string(),
    sessionId: z.string()
  })
}, async (input) => {
  // Flow implementation
  return {
    response: "AI response",
    sessionId: input.sessionId || generateSessionId()
  };
});

export const analysisFlow = ai.defineFlow({
  name: 'analysisFlow',
  inputSchema: z.object({
    data: z.string(),
    analysisType: z.string()
  }),
  outputSchema: z.object({
    results: z.any()
  })
}, async (input) => {
  // Analysis implementation
  return { results: {} };
});

// Start the flow server
startFlowServer({
  flows: [chatFlow, analysisFlow],
  port: 3000,
  cors: {
    origin: ['http://localhost:3000', 'https://myapp.com'],
    credentials: true
  }
});

// Server will expose:
// POST /chatFlow
// POST /analysisFlow
```

### Custom Express Integration
```javascript
import express from 'express';

const app = express();
app.use(express.json());

// Custom flow endpoint with middleware
app.post('/api/chat', authenticateUser, async (req, res) => {
  try {
    const result = await chatFlow(
      req.body,
      {
        context: {
          auth: req.user,
          request: {
            ip: req.ip,
            userAgent: req.headers['user-agent']
          }
        }
      }
    );
    
    res.json({ success: true, data: result });
  } catch (error) {
    res.status(500).json({ 
      success: false, 
      error: error.message 
    });
  }
});

// Streaming endpoint
app.post('/api/chat/stream', authenticateUser, async (req, res) => {
  res.setHeader('Content-Type', 'text/event-stream');
  res.setHeader('Cache-Control', 'no-cache');
  res.setHeader('Connection', 'keep-alive');

  try {
    const streamResponse = streamingChatFlow.stream(req.body, {
      context: { auth: req.user }
    });

    for await (const chunk of streamResponse.stream) {
      res.write(`data: ${JSON.stringify(chunk)}\n\n`);
    }

    const finalResult = await streamResponse.output;
    res.write(`data: ${JSON.stringify({ type: 'final', data: finalResult })}\n\n`);
    res.end();
  } catch (error) {
    res.write(`data: ${JSON.stringify({ type: 'error', error: error.message })}\n\n`);
    res.end();
  }
});

app.listen(3000, () => {
  console.log('Flow server running on port 3000');
});
```

## Testing and Debugging Flows

### Unit Testing Flows
```javascript
import { describe, it, expect, beforeEach } from 'vitest';

describe('Menu Suggestion Flow', () => {
  beforeEach(() => {
    // Setup test environment
  });

  it('should generate Italian menu item', async () => {
    const result = await menuSuggestionFlow({
      theme: 'Italian',
      dietaryRestrictions: ['vegetarian']
    });

    expect(result).toHaveProperty('menuItem');
    expect(result).toHaveProperty('description');
    expect(result).toHaveProperty('estimatedPrice');
    expect(typeof result.estimatedPrice).toBe('number');
  });

  it('should handle empty dietary restrictions', async () => {
    const result = await menuSuggestionFlow({
      theme: 'Mexican'
    });

    expect(result.menuItem).toBeTruthy();
  });

  it('should validate input schema', async () => {
    await expect(menuSuggestionFlow({
      // Missing required theme
      dietaryRestrictions: ['vegan']
    })).rejects.toThrow();
  });
});

describe('Streaming Chat Flow', () => {
  it('should stream responses correctly', async () => {
    const chunks = [];
    const streamResponse = streamingChatFlow.stream({
      message: "Hello, how are you?"
    });

    for await (const chunk of streamResponse.stream) {
      chunks.push(chunk);
    }

    expect(chunks.length).toBeGreaterThan(0);
    expect(chunks[0]).toHaveProperty('chunk');
    expect(chunks[0]).toHaveProperty('type');

    const finalResult = await streamResponse.output;
    expect(finalResult).toHaveProperty('response');
    expect(finalResult).toHaveProperty('conversationId');
  });
});
```

### Integration Testing
```javascript
describe('Flow Integration Tests', () => {
  it('should handle flow composition correctly', async () => {
    const documents = [
      "This is a positive review of the product.",
      "The service was terrible and disappointing.",
      "Average experience, nothing special."
    ];

    const result = await comprehensiveReportFlow({
      documents,
      reportType: 'summary'
    });

    expect(result.individualAnalyses).toHaveLength(3);
    expect(result.overallSummary).toBeTruthy();
    expect(result.recommendations).toBeInstanceOf(Array);
    expect(result.metadata.documentsProcessed).toBe(3);
    expect(result.metadata.averageConfidence).toBeGreaterThan(0);
  });

  it('should propagate context through nested flows', async () => {
    const mockContext = {
      auth: { uid: 'test-user', roles: ['admin'] }
    };

    const result = await parentFlow(
      { task: 'test-task' },
      { context: mockContext }
    );

    // Verify context was used in nested operations
    expect(result).toBeTruthy();
  });
});
```

## Error Handling and Resilience

### Robust Flow Implementation
```javascript
export const resilientFlow = ai.defineFlow({
  name: 'resilientFlow',
  inputSchema: z.object({
    data: z.string(),
    retryOptions: z.object({
      maxRetries: z.number().default(3),
      backoffMs: z.number().default(1000)
    }).optional()
  }),
  outputSchema: z.object({
    result: z.string(),
    attempts: z.number(),
    processingTime: z.number()
  })
}, async (input) => {
  const startTime = Date.now();
  const maxRetries = input.retryOptions?.maxRetries || 3;
  const backoffMs = input.retryOptions?.backoffMs || 1000;
  
  let lastError;
  
  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      const result = await ai.run(`attempt-${attempt}`, async () => {
        // Simulate potentially failing operation
        const { text } = await ai.generate({
          model: googleAI.model('gemini-2.5-flash'),
          prompt: `Process this data: ${input.data}`,
          config: {
            temperature: 0.1 // Lower temperature for consistency
          }
        });
        
        // Validate result
        if (!text || text.length < 10) {
          throw new Error('Generated response too short');
        }
        
        return text;
      });

      return {
        result,
        attempts: attempt,
        processingTime: Date.now() - startTime
      };
    } catch (error) {
      lastError = error;
      console.warn(`Attempt ${attempt} failed:`, error.message);
      
      if (attempt < maxRetries) {
        // Exponential backoff
        const delay = backoffMs * Math.pow(2, attempt - 1);
        await new Promise(resolve => setTimeout(resolve, delay));
      }
    }
  }

  throw new Error(`Flow failed after ${maxRetries} attempts. Last error: ${lastError?.message}`);
});
```

## Best Practices Summary

### Schema Design
1. **Use object schemas**: Always wrap inputs/outputs in objects for flexibility
2. **Add descriptions**: Use `.describe()` for better AI understanding
3. **Validate thoroughly**: Use Zod's validation features extensively
4. **Compose schemas**: Reuse common schema patterns

### Flow Structure
1. **Single responsibility**: Each flow should have one clear purpose
2. **Composability**: Build complex workflows from simpler flows
3. **Error handling**: Implement proper error handling and retries
4. **Observability**: Use `ai.run()` for important steps

### Performance
1. **Streaming**: Use streaming for long-running or interactive flows
2. **Batching**: Process multiple items efficiently
3. **Caching**: Cache expensive operations when appropriate
4. **Context management**: Minimize context size and complexity

### Deployment
1. **Environment config**: Use different configs for dev/prod
2. **Health checks**: Implement health check endpoints
3. **Monitoring**: Add proper logging and metrics
4. **Security**: Validate inputs and implement authentication

### Testing
1. **Unit tests**: Test individual flows in isolation
2. **Integration tests**: Test flow composition and context propagation
3. **Schema validation**: Test input/output schema compliance
4. **Error scenarios**: Test error handling and edge cases