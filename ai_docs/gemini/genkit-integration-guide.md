# Genkit Integration Guide

## Overview
Genkit is Google's AI application framework that provides a unified interface for building AI-powered applications with type safety, observability, and developer tooling. This guide shows how all the Gemini API capabilities covered in our documentation integrate seamlessly with Genkit, providing a more structured and production-ready approach to AI development.

## Why Use Genkit with Gemini API

### Key Benefits
- **Type Safety**: Zod schemas for inputs and outputs
- **Built-in Observability**: Automatic tracing and monitoring
- **Developer Tools**: Visual UI for testing and debugging
- **Flow Management**: Structured, reusable AI workflows
- **Production Ready**: Easy deployment and scaling
- **MCP Integration**: Model Context Protocol support for IDE integration

### Genkit vs Direct API Usage
```javascript
// Direct Gemini API (from our guides)
const response = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: prompt,
});

// Genkit approach - structured, type-safe, observable
const recipeFlow = ai.defineFlow({
  name: 'generateRecipe',
  inputSchema: RecipeInputSchema,
  outputSchema: RecipeSchema,
}, async (input) => {
  const { output } = await ai.generate({
    prompt: `Create a recipe for ${input.ingredient}`,
    output: { schema: RecipeSchema },
  });
  return output;
});
```

## Basic Genkit Setup

### Installation and Configuration
```javascript
// Install Genkit CLI globally
// npm install -g genkit-cli

// Install packages
// npm install genkit @genkit-ai/google-genai

import { genkit, z } from 'genkit';
import { googleAI } from '@genkit-ai/google-genai';

// Initialize Genkit with Google AI plugin
const ai = genkit({
  plugins: [googleAI()],
  model: googleAI.model('gemini-2.5-flash', {
    temperature: 0.8
  }),
});
```## Text Gen
eration with Genkit

### Structured Output (from structured-output-guide.md)
```javascript
// Convert our structured output patterns to Genkit flows
const PersonSchema = z.object({
  name: z.string(),
  age: z.number(),
  email: z.string(),
  isActive: z.boolean()
});

const extractPersonFlow = ai.defineFlow({
  name: 'extractPerson',
  inputSchema: z.object({ text: z.string() }),
  outputSchema: PersonSchema,
}, async (input) => {
  const { output } = await ai.generate({
    prompt: `Extract person information from: ${input.text}`,
    output: { schema: PersonSchema },
  });
  return output;
});

// Usage with type safety
const person = await extractPersonFlow({
  text: "John Doe, 30 years old, john@example.com, currently active"
});
// person is fully typed as PersonSchema
```

### Thinking Integration (from thinking-guide.md)
```javascript
// Integrate thinking capabilities with Genkit flows
const complexReasoningFlow = ai.defineFlow({
  name: 'complexReasoning',
  inputSchema: z.object({
    problem: z.string(),
    complexity: z.enum(['simple', 'medium', 'complex']).default('medium')
  }),
  outputSchema: z.object({
    solution: z.string(),
    reasoning: z.string().optional(),
    confidence: z.number()
  })
}, async (input) => {
  const budgetMap = { simple: 0, medium: 2048, complex: 8192 };
  
  const response = await ai.generate({
    model: googleAI.model('gemini-2.5-pro'),
    prompt: input.problem,
    config: {
      thinkingConfig: {
        thinkingBudget: budgetMap[input.complexity],
        includeThoughts: input.complexity !== 'simple'
      }
    }
  });

  return {
    solution: response.text(),
    reasoning: input.complexity !== 'simple' ? 'Reasoning available in trace' : undefined,
    confidence: 0.9
  };
});
```## 
Genkit Basics and Setup

### Installation and Project Setup
```javascript
// Global CLI installation
npm install -g genkit-cli

// Project dependencies
npm install genkit @genkit-ai/google-genai

// TypeScript setup (recommended)
npm install -D typescript tsx
npx tsc --init
```

### Basic Genkit Configuration
```javascript
import { googleAI } from '@genkit-ai/google-genai';
import { genkit, z } from 'genkit';

// Initialize Genkit with Google AI plugin
const ai = genkit({
  plugins: [googleAI()],
  model: googleAI.model('gemini-2.5-flash', {
    temperature: 0.8
  }),
});
```

### Environment Variables
```bash
# Required for Google AI Studio API
export GEMINI_API_KEY=<your-api-key>

# For development mode
export GENKIT_ENV=dev
```

## Core Genkit Concepts

### Flows - The Foundation
Flows are the primary building blocks in Genkit - they're functions with built-in observability, type safety, and tooling integration.

```javascript
// Basic flow structure
const myFlow = ai.defineFlow({
  name: 'myFlow',
  inputSchema: z.object({
    input: z.string()
  }),
  outputSchema: z.object({
    result: z.string()
  })
}, async (input) => {
  // Flow logic here
  return { result: `Processed: ${input.input}` };
});
```

### Developer UI and Testing
```bash
# Start the Developer UI
genkit start -- npx tsx --watch src/index.ts

# Alternative with npm script
npm run genkit:ui  # if configured in package.json

# Access at http://localhost:4000
```

The Developer UI provides:
- Interactive flow testing
- Visual trace inspection
- Schema validation
- Real-time debugging

## Integration with Our Existing Guides

### Text Generation (from text-generation-guide.md)
```javascript
// Convert basic text generation to Genkit flows
const textGenerationFlow = ai.defineFlow({
  name: 'generateText',
  inputSchema: z.object({
    prompt: z.string(),
    temperature: z.number().optional(),
    maxTokens: z.number().optional()
  }),
  outputSchema: z.object({
    text: z.string(),
    usage: z.object({
      inputTokens: z.number(),
      outputTokens: z.number()
    }).optional()
  })
}, async (input) => {
  const response = await ai.generate({
    prompt: input.prompt,
    config: {
      temperature: input.temperature || 0.7,
      maxOutputTokens: input.maxTokens || 1000
    }
  });

  return {
    text: response.text(),
    usage: response.usage ? {
      inputTokens: response.usage.inputTokens,
      outputTokens: response.usage.outputTokens
    } : undefined
  };
});
```

### Structured Output (from structured-output-guide.md)
```javascript
// Genkit makes structured output even more powerful with Zod schemas
const PersonSchema = z.object({
  name: z.string(),
  age: z.number(),
  email: z.string().email(),
  skills: z.array(z.string()),
  isActive: z.boolean()
});

const extractPersonFlow = ai.defineFlow({
  name: 'extractPerson',
  inputSchema: z.object({ 
    text: z.string(),
    format: z.enum(['detailed', 'basic']).default('basic')
  }),
  outputSchema: PersonSchema,
}, async (input) => {
  const { output } = await ai.generate({
    prompt: `Extract person information from: ${input.text}`,
    output: { schema: PersonSchema },
  });
  
  if (!output) {
    throw new Error('Failed to extract person information');
  }
  
  return output;
});

// Usage with full type safety
const person = await extractPersonFlow({
  text: "John Doe, 30 years old, john@example.com, skilled in JavaScript and Python, currently active"
});
// person is fully typed as PersonSchema
```

### Function Calling (from function-calling-guide.md)
```javascript
// Genkit provides built-in tool support
const weatherTool = ai.defineTool({
  name: 'getCurrentWeather',
  description: 'Gets current weather for a location',
  inputSchema: z.object({
    location: z.string().describe('City name, e.g. San Francisco'),
    unit: z.enum(['celsius', 'fahrenheit']).default('celsius')
  }),
  outputSchema: z.object({
    temperature: z.number(),
    condition: z.string(),
    humidity: z.number()
  })
}, async (input) => {
  // Your weather API integration
  const weatherData = await fetchWeatherAPI(input.location, input.unit);
  return weatherData;
});

// Use tools in flows
const weatherFlow = ai.defineFlow({
  name: 'getWeatherReport',
  inputSchema: z.object({
    location: z.string(),
    includeAdvice: z.boolean().default(false)
  }),
  outputSchema: z.object({
    weather: z.object({
      temperature: z.number(),
      condition: z.string(),
      humidity: z.number()
    }),
    advice: z.string().optional()
  })
}, async (input) => {
  const weather = await weatherTool(input);
  
  let advice;
  if (input.includeAdvice) {
    const adviceResponse = await ai.generate({
      prompt: `Based on weather: ${weather.condition}, ${weather.temperature}°, ${weather.humidity}% humidity. Give clothing advice.`,
    });
    advice = adviceResponse.text();
  }
  
  return { weather, advice };
});
```

### Google Search Grounding (from google-search-grounding-guide.md)
```javascript
// Grounding with Genkit flows
const groundedSearchFlow = ai.defineFlow({
  name: 'groundedSearch',
  inputSchema: z.object({
    query: z.string(),
    requireGrounding: z.boolean().default(false)
  }),
  outputSchema: z.object({
    answer: z.string(),
    isGrounded: z.boolean(),
    sources: z.array(z.object({
      title: z.string(),
      url: z.string()
    })).optional()
  })
}, async (input) => {
  const response = await ai.generate({
    prompt: input.query,
    tools: [googleSearchTool], // Genkit's built-in Google Search tool
  });

  // Process grounding metadata
  const groundingData = response.candidates?.[0]?.groundingMetadata;
  const isGrounded = !!groundingData;
  
  if (input.requireGrounding && !isGrounded) {
    throw new Error('Response was not grounded with search results');
  }

  const sources = groundingData?.groundingChunks?.map(chunk => ({
    title: chunk.web?.title || 'Unknown',
    url: chunk.web?.uri || ''
  })) || [];

  return {
    answer: response.text(),
    isGrounded,
    sources: sources.length > 0 ? sources : undefined
  };
});
```

### Code Execution (from code-execution-guide.md)
```javascript
// Code execution with Genkit
const codeAnalysisFlow = ai.defineFlow({
  name: 'analyzeWithCode',
  inputSchema: z.object({
    problem: z.string(),
    dataType: z.enum(['mathematical', 'statistical', 'visualization']).default('mathematical')
  }),
  outputSchema: z.object({
    solution: z.string(),
    code: z.string().optional(),
    result: z.string().optional(),
    visualization: z.string().optional() // base64 image if generated
  })
}, async (input) => {
  const response = await ai.generate({
    prompt: `Solve this ${input.dataType} problem: ${input.problem}. Generate and execute Python code to find the solution.`,
    tools: [codeExecutionTool], // Genkit's code execution tool
  });

  // Extract code and results from response
  const parts = response.candidates?.[0]?.content?.parts || [];
  const code = parts.find(p => p.executableCode)?.executableCode?.code;
  const result = parts.find(p => p.codeExecutionResult)?.codeExecutionResult?.output;
  const visualization = parts.find(p => p.inlineData?.mimeType?.startsWith('image/'))?.inlineData?.data;

  return {
    solution: response.text(),
    code,
    result,
    visualization
  };
});
```

### Audio Understanding (from audio-understanding-guide.md)
```javascript
// Audio processing with Genkit
const audioAnalysisFlow = ai.defineFlow({
  name: 'analyzeAudio',
  inputSchema: z.object({
    audioUrl: z.string(),
    analysisType: z.enum(['transcription', 'summary', 'sentiment', 'meeting']).default('transcription'),
    includeTimestamps: z.boolean().default(false)
  }),
  outputSchema: z.object({
    transcript: z.string().optional(),
    summary: z.string().optional(),
    sentiment: z.enum(['positive', 'negative', 'neutral']).optional(),
    keyPoints: z.array(z.string()).optional(),
    speakers: z.array(z.string()).optional()
  })
}, async (input) => {
  let prompt = '';
  switch (input.analysisType) {
    case 'transcription':
      prompt = 'Provide a complete transcript of this audio';
      break;
    case 'summary':
      prompt = 'Summarize the key points discussed in this audio';
      break;
    case 'sentiment':
      prompt = 'Analyze the sentiment and emotional tone of this audio';
      break;
    case 'meeting':
      prompt = 'Analyze this meeting audio for key decisions, action items, and participant contributions';
      break;
  }

  if (input.includeTimestamps) {
    prompt += ' with relevant timestamps';
  }

  const response = await ai.generate({
    prompt: [
      { media: { url: input.audioUrl } },
      { text: prompt }
    ]
  });

  // Parse response based on analysis type
  const text = response.text();
  
  return {
    transcript: input.analysisType === 'transcription' ? text : undefined,
    summary: input.analysisType === 'summary' ? text : undefined,
    // Additional parsing logic based on analysis type
  };
});
```

### Video Understanding (from video-understanding-guide.md)
```javascript
// Video analysis with Genkit
const videoAnalysisFlow = ai.defineFlow({
  name: 'analyzeVideo',
  inputSchema: z.object({
    videoUrl: z.string(),
    analysisType: z.enum(['description', 'highlights', 'chapters', 'transcription']).default('description'),
    maxHighlights: z.number().default(5)
  }),
  outputSchema: z.object({
    description: z.string().optional(),
    highlights: z.array(z.object({
      timestamp: z.string(),
      description: z.string(),
      importance: z.number()
    })).optional(),
    chapters: z.array(z.object({
      title: z.string(),
      startTime: z.string(),
      endTime: z.string(),
      description: z.string()
    })).optional(),
    transcript: z.string().optional()
  })
}, async (input) => {
  const prompts = {
    description: 'Provide a comprehensive description of this video content',
    highlights: `Identify the top ${input.maxHighlights} highlights with timestamps and importance ratings`,
    chapters: 'Break down this video into logical chapters with timestamps',
    transcription: 'Generate a complete transcript of the speech in this video'
  };

  const response = await ai.generate({
    prompt: [
      { media: { url: input.videoUrl } },
      { text: prompts[input.analysisType] }
    ]
  });

  // Parse response based on analysis type
  const text = response.text();
  
  return {
    description: input.analysisType === 'description' ? text : undefined,
    // Additional parsing logic for other types
  };
});
```

## MCP Server Integration

### What is the Genkit MCP Server?
The Genkit MCP (Model Context Protocol) Server enables seamless integration with AI development tools like:
- Cursor AI IDE
- Claude Code
- Windsurf
- Cline
- Gemini CLI

### Setting up MCP Server
```bash
# Start the MCP server
genkit mcp

# Or configure in your IDE's MCP settings
{
  "mcpServers": {
    "genkit": {
      "command": "genkit",
      "args": ["mcp"],
      "cwd": "/path/to/your/genkit/project",
      "timeout": 30000,
      "trust": false
    }
  }
}
```

### MCP Server Capabilities
1. **List Flows**: Discover all defined Genkit flows
2. **Run Flows**: Execute flows with proper input validation
3. **Get Traces**: Retrieve detailed execution traces
4. **Documentation Lookup**: Access Genkit documentation

### Using MCP Tools
```bash
# List available flows
@genkit:list_flows {}

# Run a specific flow
@genkit:run_flow { 
  "flowName": "recipeGeneratorFlow", 
  "input": "{\"ingredient\": \"avocado\", \"dietaryRestrictions\": \"vegetarian\"}" 
}

# Get trace details
@genkit:get_trace { "traceId": "ecf38e20f418b2964f7ab472b799" }

# Look up documentation
@genkit:lookup_genkit_docs { "language": "js" }
```

## Advanced Genkit Patterns

### Streaming Flows
```javascript
const streamingFlow = ai.defineStreamingFlow({
  name: 'streamingChat',
  inputSchema: z.object({
    message: z.string(),
    conversationId: z.string().optional()
  }),
  outputSchema: z.object({
    response: z.string()
  }),
  streamSchema: z.object({
    chunk: z.string()
  })
}, async (input, streamingCallback) => {
  const stream = await ai.generateStream({
    prompt: input.message,
  });

  let fullResponse = '';
  for await (const chunk of stream) {
    const text = chunk.text();
    fullResponse += text;
    
    // Stream chunks to client
    streamingCallback({ chunk: text });
  }

  return { response: fullResponse };
});
```

### Error Handling and Validation
```javascript
const robustFlow = ai.defineFlow({
  name: 'robustFlow',
  inputSchema: z.object({
    data: z.string(),
    retries: z.number().default(3)
  }),
  outputSchema: z.object({
    result: z.string(),
    attempts: z.number()
  })
}, async (input) => {
  let attempts = 0;
  let lastError;

  while (attempts < input.retries) {
    try {
      attempts++;
      
      const response = await ai.generate({
        prompt: `Process this data: ${input.data}`,
      });

      return {
        result: response.text(),
        attempts
      };
    } catch (error) {
      lastError = error;
      if (attempts < input.retries) {
        // Wait before retry with exponential backoff
        await new Promise(resolve => setTimeout(resolve, 1000 * attempts));
      }
    }
  }

  throw new Error(`Failed after ${attempts} attempts: ${lastError?.message}`);
});
```

### Multi-Model Flows
```javascript
// Using both Google AI and Vertex AI in the same project
const ai = genkit({
  plugins: [
    googleAI(),
    vertexAI({ location: 'us-central1' })
  ],
});

const multiModelFlow = ai.defineFlow({
  name: 'multiModelAnalysis',
  inputSchema: z.object({
    text: z.string(),
    useVertexAI: z.boolean().default(false)
  }),
  outputSchema: z.object({
    analysis: z.string(),
    model: z.string()
  })
}, async (input) => {
  const model = input.useVertexAI 
    ? vertexAI.model('gemini-2.5-pro')
    : googleAI.model('gemini-2.5-flash');

  const response = await ai.generate({
    model,
    prompt: `Analyze this text: ${input.text}`,
  });

  return {
    analysis: response.text(),
    model: input.useVertexAI ? 'vertex-ai' : 'google-ai'
  };
});
```

## Deployment and Production

### Environment Configuration
```javascript
// Different configs for different environments
const getConfig = () => {
  const env = process.env.NODE_ENV || 'development';
  
  const configs = {
    development: {
      plugins: [googleAI()],
      model: googleAI.model('gemini-2.5-flash', { temperature: 0.8 })
    },
    production: {
      plugins: [vertexAI({ location: 'us-central1' })],
      model: vertexAI.model('gemini-2.5-pro', { temperature: 0.3 })
    }
  };
  
  return configs[env] || configs.development;
};

const ai = genkit(getConfig());
```

### API Deployment
```javascript
// Express.js integration
import express from 'express';

const app = express();
app.use(express.json());

// Expose flows as REST endpoints
app.post('/api/flows/:flowName', async (req, res) => {
  try {
    const { flowName } = req.params;
    const input = req.body;
    
    // Get flow by name (you'd implement flow registry)
    const flow = getFlowByName(flowName);
    if (!flow) {
      return res.status(404).json({ error: 'Flow not found' });
    }
    
    const result = await flow(input);
    res.json({ success: true, result });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

app.listen(3000, () => {
  console.log('Genkit API server running on port 3000');
});
```

## Best Practices Summary

### Schema Design
1. **Use descriptive schemas**: Include descriptions for better AI understanding
2. **Validate inputs**: Leverage Zod's validation capabilities
3. **Type safety**: Always define input/output schemas for flows
4. **Optional fields**: Use `.optional()` for non-required fields

### Flow Organization
1. **Single responsibility**: Each flow should have one clear purpose
2. **Composability**: Build complex workflows from simpler flows
3. **Error handling**: Implement proper error handling and retries
4. **Observability**: Use the Developer UI for debugging and optimization

### Performance
1. **Model selection**: Choose appropriate models for your use case
2. **Caching**: Implement caching for expensive operations
3. **Streaming**: Use streaming flows for real-time interactions
4. **Batch processing**: Group related operations when possible

### Security
1. **Input validation**: Always validate and sanitize inputs
2. **API keys**: Use environment variables for sensitive data
3. **Rate limiting**: Implement rate limiting for production APIs
4. **Error messages**: Don't expose sensitive information in errors