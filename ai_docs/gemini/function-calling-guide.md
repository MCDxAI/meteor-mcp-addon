# Gemini API Function Calling Guide

## Overview
Function calling enables Gemini models to connect with external tools and APIs, acting as a bridge between natural language and real-world actions. Instead of generating only text responses, the model can determine when to call specific functions and provide the necessary parameters for execution.

## Primary Use Cases

### 1. Augment Knowledge
Access information from external sources like databases, APIs, and knowledge bases.

### 2. Extend Capabilities  
Use external tools to perform computations and extend model limitations (calculators, chart creation, etc.).

### 3. Take Actions
Interact with external systems via APIs (scheduling, invoicing, emails, smart home control).

## How Function Calling Works

### The 4-Step Process

1. **Define Function Declaration**: Describe function name, parameters, and purpose to the model
2. **Call Model with Declarations**: Send user prompt + function declarations; model decides whether to call functions
3. **Execute Function Code**: Your application processes the response and executes the suggested function
4. **Create User-Friendly Response**: Send function results back to model for final response generation

## Basic Function Calling Setup

### JavaScript/TypeScript Implementation

```javascript
import { GoogleGenAI, Type } from '@google/genai';

const ai = new GoogleGenAI({
  apiKey: process.env.GEMINI_API_KEY
});

// Step 1: Define function declaration
const weatherFunctionDeclaration = {
  name: 'get_current_temperature',
  description: 'Gets the current temperature for a given location.',
  parameters: {
    type: Type.OBJECT,
    properties: {
      location: {
        type: Type.STRING,
        description: 'The city name, e.g. San Francisco',
      },
    },
    required: ['location'],
  },
};

// Step 2: Call model with function declarations
async function callModelWithFunction(userPrompt) {
  const response = await ai.models.generateContent({
    model: 'gemini-2.5-flash',
    contents: userPrompt,
    config: {
      tools: [{ functionDeclarations: [weatherFunctionDeclaration] }],
    },
  });

  return response;
}

// Step 3: Execute function and Step 4: Get final response
async function handleFunctionCall(userPrompt) {
  const response = await callModelWithFunction(userPrompt);
  
  if (response.functionCalls && response.functionCalls.length > 0) {
    const functionCall = response.functionCalls[0];
    
    // Execute your actual function here
    const result = await getCurrentTemperature(functionCall.args);
    
    // Send result back to model
    const contents = [
      { role: 'user', parts: [{ text: userPrompt }] },
      response.candidates[0].content,
      {
        role: 'user',
        parts: [{
          functionResponse: {
            name: functionCall.name,
            response: { result }
          }
        }]
      }
    ];
    
    const finalResponse = await ai.models.generateContent({
      model: 'gemini-2.5-flash',
      contents: contents,
      config: {
        tools: [{ functionDeclarations: [weatherFunctionDeclaration] }],
      },
    });
    
    return finalResponse.text;
  } else {
    return response.text;
  }
}
```

## Function Declaration Structure

### Complete Declaration Format
```javascript
const functionDeclaration = {
  name: 'function_name',                    // Required: unique identifier
  description: 'Clear function purpose',   // Required: detailed explanation
  parameters: {                            // Required: parameter schema
    type: Type.OBJECT,
    properties: {
      paramName: {
        type: Type.STRING,                 // string, number, boolean, array
        description: 'Parameter purpose', // Include examples and constraints
        enum: ['option1', 'option2']      // Optional: for fixed value sets
      }
    },
    required: ['paramName']               // Array of mandatory parameters
  }
};
```

### Best Practices for Declarations

#### Clear Naming
```javascript
// ✅ Good: Descriptive and clear
const goodDeclaration = {
  name: 'schedule_meeting',
  description: 'Schedules a meeting with specified attendees at a given time and date.'
};

// ❌ Bad: Vague and unclear  
const badDeclaration = {
  name: 'do_thing',
  description: 'Does something'
};
```

#### Detailed Descriptions
```javascript
const detailedDeclaration = {
  name: 'get_weather_forecast',
  description: 'Gets weather forecast for a location. Supports cities, zip codes, and coordinates.',
  parameters: {
    type: Type.OBJECT,
    properties: {
      location: {
        type: Type.STRING,
        description: 'The city and state (e.g., "San Francisco, CA") or zip code (e.g., "94102")'
      },
      days: {
        type: Type.NUMBER,
        description: 'Number of forecast days (1-7)',
        minimum: 1,
        maximum: 7
      }
    },
    required: ['location']
  }
};
```

#### Use Enums for Fixed Values
```javascript
const enumDeclaration = {
  name: 'set_light_values',
  description: 'Sets the brightness and color temperature of a light.',
  parameters: {
    type: Type.OBJECT,
    properties: {
      brightness: {
        type: Type.NUMBER,
        description: 'Light level from 0 to 100'
      },
      color_temp: {
        type: Type.STRING,
        enum: ['daylight', 'cool', 'warm'],  // ✅ Better than just describing options
        description: 'Color temperature of the light fixture'
      }
    },
    required: ['brightness', 'color_temp']
  }
};
```

## Real-World Examples

### Weather API Integration
```javascript
const weatherFunction = {
  name: 'get_current_temperature',
  description: 'Gets the current temperature for a given location.',
  parameters: {
    type: Type.OBJECT,
    properties: {
      location: {
        type: Type.STRING,
        description: 'The city name, e.g. San Francisco',
      },
    },
    required: ['location'],
  },
};

async function getCurrentTemperature({ location }) {
  // Your weather API integration
  const response = await fetch(`https://api.weather.com/current?location=${location}`);
  const data = await response.json();
  return { temperature: data.temperature, unit: 'celsius' };
}
```

### Meeting Scheduling
```javascript
const scheduleMeetingFunction = {
  name: 'schedule_meeting',
  description: 'Schedules a meeting with specified attendees at a given time and date.',
  parameters: {
    type: Type.OBJECT,
    properties: {
      attendees: {
        type: Type.ARRAY,
        items: { type: Type.STRING },
        description: 'List of people attending the meeting.',
      },
      date: {
        type: Type.STRING,
        description: 'Date of the meeting (e.g., "2024-07-29")',
      },
      time: {
        type: Type.STRING,
        description: 'Time of the meeting (e.g., "15:00")',
      },
      topic: {
        type: Type.STRING,
        description: 'The subject or topic of the meeting.',
      },
    },
    required: ['attendees', 'date', 'time', 'topic'],
  },
};

async function scheduleMeeting({ attendees, date, time, topic }) {
  // Your calendar API integration
  const meeting = await calendarAPI.createEvent({
    attendees,
    startTime: `${date}T${time}:00`,
    subject: topic
  });
  return { meetingId: meeting.id, status: 'scheduled' };
}
```

### Chart Creation
```javascript
const createChartFunction = {
  name: 'create_bar_chart',
  description: 'Creates a bar chart given a title, labels, and corresponding values.',
  parameters: {
    type: Type.OBJECT,
    properties: {
      title: {
        type: Type.STRING,
        description: 'The title for the chart.',
      },
      labels: {
        type: Type.ARRAY,
        items: { type: Type.STRING },
        description: 'List of labels for the data points (e.g., ["Q1", "Q2", "Q3"]).',
      },
      values: {
        type: Type.ARRAY,
        items: { type: Type.NUMBER },
        description: 'List of numerical values corresponding to the labels (e.g., [50000, 75000, 60000]).',
      },
    },
    required: ['title', 'labels', 'values'],
  },
};

async function createBarChart({ title, labels, values }) {
  // Your charting library integration
  const chart = await chartingLibrary.createChart({
    type: 'bar',
    title,
    data: labels.map((label, index) => ({ label, value: values[index] }))
  });
  return { chartUrl: chart.url, chartId: chart.id };
}
```

## Advanced Function Calling Patterns

### Parallel Function Calling
Execute multiple independent functions simultaneously:

```javascript
const partyFunctions = [
  {
    name: 'power_disco_ball',
    description: 'Powers the spinning disco ball.',
    parameters: {
      type: Type.OBJECT,
      properties: {
        power: {
          type: Type.BOOLEAN,
          description: 'Whether to turn the disco ball on or off.'
        }
      },
      required: ['power']
    }
  },
  {
    name: 'start_music',
    description: 'Play some music matching the specified parameters.',
    parameters: {
      type: Type.OBJECT,
      properties: {
        energetic: {
          type: Type.BOOLEAN,
          description: 'Whether the music is energetic or not.'
        },
        loud: {
          type: Type.BOOLEAN,
          description: 'Whether the music is loud or not.'
        }
      },
      required: ['energetic', 'loud']
    }
  },
  {
    name: 'dim_lights',
    description: 'Dim the lights.',
    parameters: {
      type: Type.OBJECT,
      properties: {
        brightness: {
          type: Type.NUMBER,
          description: 'The brightness of the lights, 0.0 is off, 1.0 is full.'
        }
      },
      required: ['brightness']
    }
  }
];

async function handleParallelFunctions() {
  const response = await ai.models.generateContent({
    model: 'gemini-2.5-flash',
    contents: 'Turn this place into a party!',
    config: {
      tools: [{ functionDeclarations: partyFunctions }],
      toolConfig: {
        functionCallingConfig: { mode: 'any' }  // Force function calling
      }
    },
  });

  // Execute all function calls in parallel
  const results = await Promise.all(
    response.functionCalls.map(async (call) => {
      const result = await executeFunctionByName(call.name, call.args);
      return { name: call.name, response: { result } };
    })
  );

  return results;
}
```

### Compositional Function Calling
Chain multiple function calls for complex workflows:

```javascript
const weatherAndThermostatFunctions = [
  {
    name: 'get_weather_forecast',
    description: 'Gets the current weather temperature for a given location.',
    parameters: {
      type: Type.OBJECT,
      properties: {
        location: { type: Type.STRING }
      },
      required: ['location']
    }
  },
  {
    name: 'set_thermostat_temperature',
    description: 'Sets the thermostat to a desired temperature.',
    parameters: {
      type: Type.OBJECT,
      properties: {
        temperature: { type: Type.NUMBER }
      },
      required: ['temperature']
    }
  }
];

async function handleCompositionalCalling(prompt) {
  const toolFunctions = {
    get_weather_forecast: ({ location }) => {
      // Mock weather API call
      return { temperature: 25, unit: 'celsius' };
    },
    set_thermostat_temperature: ({ temperature }) => {
      // Mock thermostat API call
      return { status: 'success' };
    }
  };

  let contents = [
    { role: 'user', parts: [{ text: prompt }] }
  ];

  // Loop until no more function calls
  while (true) {
    const result = await ai.models.generateContent({
      model: 'gemini-2.5-flash',
      contents,
      config: { tools: [{ functionDeclarations: weatherAndThermostatFunctions }] }
    });

    if (result.functionCalls && result.functionCalls.length > 0) {
      const functionCall = result.functionCalls[0];
      const toolResponse = toolFunctions[functionCall.name](functionCall.args);

      // Add model response and function result to conversation
      contents.push({
        role: 'model',
        parts: [{ functionCall: functionCall }]
      });
      contents.push({
        role: 'user',
        parts: [{
          functionResponse: {
            name: functionCall.name,
            response: { result: toolResponse }
          }
        }]
      });
    } else {
      return result.text;
    }
  }
}

// Usage
const result = await handleCompositionalCalling(
  "If it's warmer than 20°C in London, set the thermostat to 20°C, otherwise set it to 18°C."
);
```

## Function Calling Modes

### AUTO Mode (Default)
Model decides whether to call functions or respond directly:
```javascript
const config = {
  tools: [{ functionDeclarations: [myFunction] }],
  // No toolConfig needed - AUTO is default
};
```

### ANY Mode
Force the model to always call a function:
```javascript
const config = {
  tools: [{ functionDeclarations: [myFunction] }],
  toolConfig: {
    functionCallingConfig: {
      mode: 'any',
      allowedFunctionNames: ['specific_function']  // Optional: limit to specific functions
    }
  }
};
```

### NONE Mode
Temporarily disable function calling:
```javascript
const config = {
  tools: [{ functionDeclarations: [myFunction] }],
  toolConfig: {
    functionCallingConfig: { mode: 'none' }
  }
};
```

## Function Calling with Thinking

Enable "thinking" for improved function call performance:

```javascript
// Thinking is enabled by default in 2.5 models
const response = await ai.models.generateContent({
  model: 'gemini-2.5-flash',
  contents: 'Complex request requiring reasoning',
  config: {
    tools: [{ functionDeclarations: [complexFunction] }]
  }
});

// Inspect thought signatures (optional, for debugging)
const part = response.candidates[0].content.parts[0];
if (part.thoughtSignature) {
  console.log('Thought signature present:', part.thoughtSignature);
}
```

### Managing Thought Signatures
For multi-turn conversations, preserve the complete model response:

```javascript
// ✅ Correct: Preserve complete response
contents.push(response.candidates[0].content);

// ❌ Incorrect: Don't modify parts with thought signatures
// This breaks the positional context
```

## Model Context Protocol (MCP) Integration

### JavaScript MCP Setup
```javascript
import { GoogleGenAI, mcpToTool } from '@google/genai';
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";

// Setup MCP server connection
const serverParams = new StdioClientTransport({
  command: "npx",
  args: ["-y", "@philschmid/weather-mcp"]
});

const mcpClient = new Client({
  name: "example-client",
  version: "1.0.0"
});

await mcpClient.connect(serverParams);

// Use MCP tools with Gemini
const response = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: "What is the weather in London?",
  config: {
    tools: [mcpToTool(mcpClient)],  // Automatic tool calling enabled
  },
});

console.log(response.text);
await mcpClient.close();
```

## Error Handling and Best Practices

### Robust Function Execution
```javascript
async function safeExecuteFunction(functionCall) {
  try {
    const result = await executeFunctionByName(functionCall.name, functionCall.args);
    return {
      name: functionCall.name,
      response: { result }
    };
  } catch (error) {
    console.error(`Function ${functionCall.name} failed:`, error);
    return {
      name: functionCall.name,
      response: { 
        error: error.message,
        status: 'failed'
      }
    };
  }
}
```

### Function Registry Pattern
```javascript
class FunctionRegistry {
  constructor() {
    this.functions = new Map();
    this.declarations = [];
  }

  register(declaration, implementation) {
    this.functions.set(declaration.name, implementation);
    this.declarations.push(declaration);
  }

  async execute(functionCall) {
    const func = this.functions.get(functionCall.name);
    if (!func) {
      throw new Error(`Function ${functionCall.name} not found`);
    }
    return await func(functionCall.args);
  }

  getDeclarations() {
    return this.declarations;
  }
}

// Usage
const registry = new FunctionRegistry();

registry.register(weatherFunctionDeclaration, getCurrentTemperature);
registry.register(scheduleMeetingFunction, scheduleMeeting);

const response = await ai.models.generateContent({
  model: 'gemini-2.5-flash',
  contents: userPrompt,
  config: {
    tools: [{ functionDeclarations: registry.getDeclarations() }]
  }
});

if (response.functionCalls) {
  for (const call of response.functionCalls) {
    const result = await registry.execute(call);
    // Handle result...
  }
}
```

### Validation and Security
```javascript
function validateFunctionCall(functionCall, expectedSchema) {
  // Validate required parameters
  for (const required of expectedSchema.parameters.required || []) {
    if (!(required in functionCall.args)) {
      throw new Error(`Missing required parameter: ${required}`);
    }
  }

  // Validate parameter types
  for (const [key, value] of Object.entries(functionCall.args)) {
    const paramSchema = expectedSchema.parameters.properties[key];
    if (paramSchema && !validateType(value, paramSchema.type)) {
      throw new Error(`Invalid type for parameter ${key}`);
    }
  }

  return true;
}

function validateType(value, expectedType) {
  switch (expectedType) {
    case 'string': return typeof value === 'string';
    case 'number': return typeof value === 'number';
    case 'boolean': return typeof value === 'boolean';
    case 'array': return Array.isArray(value);
    default: return true;
  }
}
```

## Performance Optimization

### Function Selection Strategy
```javascript
// ✅ Good: Contextual function selection
function selectRelevantFunctions(userIntent, allFunctions) {
  const intentKeywords = userIntent.toLowerCase();
  
  return allFunctions.filter(func => {
    const description = func.description.toLowerCase();
    return intentKeywords.split(' ').some(keyword => 
      description.includes(keyword)
    );
  }).slice(0, 10); // Limit to 10 most relevant
}

// Usage
const relevantFunctions = selectRelevantFunctions(
  "What's the weather like?", 
  allAvailableFunctions
);

const response = await ai.models.generateContent({
  model: 'gemini-2.5-flash',
  contents: userPrompt,
  config: {
    tools: [{ functionDeclarations: relevantFunctions }]
  }
});
```

### Caching and Rate Limiting
```javascript
class FunctionCallManager {
  constructor() {
    this.cache = new Map();
    this.rateLimiter = new Map();
  }

  async executeWithCache(functionCall, ttl = 300000) { // 5 min cache
    const cacheKey = `${functionCall.name}:${JSON.stringify(functionCall.args)}`;
    
    // Check cache
    if (this.cache.has(cacheKey)) {
      const cached = this.cache.get(cacheKey);
      if (Date.now() - cached.timestamp < ttl) {
        return cached.result;
      }
    }

    // Check rate limit
    const rateLimitKey = functionCall.name;
    const lastCall = this.rateLimiter.get(rateLimitKey) || 0;
    const minInterval = 1000; // 1 second between calls
    
    if (Date.now() - lastCall < minInterval) {
      await new Promise(resolve => 
        setTimeout(resolve, minInterval - (Date.now() - lastCall))
      );
    }

    // Execute function
    const result = await this.executeFunction(functionCall);
    
    // Update cache and rate limiter
    this.cache.set(cacheKey, { result, timestamp: Date.now() });
    this.rateLimiter.set(rateLimitKey, Date.now());
    
    return result;
  }
}
```

## Supported Models and Capabilities

| Model | Function Calling | Parallel Calling | Compositional Calling |
|-------|-----------------|------------------|----------------------|
| Gemini 2.5 Pro | ✅ | ✅ | ✅ |
| Gemini 2.5 Flash | ✅ | ✅ | ✅ |
| Gemini 2.5 Flash-Lite | ✅ | ✅ | ✅ |
| Gemini 2.0 Flash | ✅ | ✅ | ✅ |
| Gemini 2.0 Flash-Lite | ❌ | ❌ | ❌ |

## Best Practices Summary

### Function Design
1. **Clear naming**: Use descriptive function names without spaces or special characters
2. **Detailed descriptions**: Provide comprehensive explanations with examples
3. **Strong typing**: Use specific types and enums for parameters
4. **Validation**: Implement parameter validation and error handling

### Performance
1. **Tool selection**: Limit active functions to 10-20 most relevant
2. **Temperature**: Use low temperature (0-0.3) for deterministic function calls
3. **Caching**: Cache function results when appropriate
4. **Rate limiting**: Implement proper rate limiting for external APIs

### Security
1. **Input validation**: Validate all function parameters
2. **Authentication**: Use proper auth mechanisms for external APIs
3. **Error handling**: Return informative error messages
4. **User confirmation**: Validate high-consequence actions with users

### Integration
1. **Modular design**: Use function registries for organization
2. **Error recovery**: Implement graceful error handling
3. **Monitoring**: Log function calls and performance metrics
4. **Testing**: Test function calls with various inputs and edge cases