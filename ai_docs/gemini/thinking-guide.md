# Gemini API Thinking Guide

## Overview
Gemini 2.5 series models use an internal "thinking process" that significantly improves their reasoning and multi-step planning abilities, making them highly effective for complex tasks such as coding, advanced mathematics, and data analysis. This guide covers how to leverage Gemini's thinking capabilities effectively.

## Key Capabilities
- **Enhanced reasoning**: Internal thinking process for complex problem-solving
- **Multi-step planning**: Better handling of tasks requiring sequential reasoning
- **Thought summaries**: Access to synthesized versions of the model's reasoning process
- **Thinking budgets**: Control over computational resources allocated to thinking
- **Thought signatures**: Maintain context across multi-turn conversations with function calling
- **Tool integration**: Thinking works with all Gemini tools and capabilities

## Basic Setup

### JavaScript/TypeScript Implementation
```javascript
import { GoogleGenAI } from "@google/genai";

const ai = new GoogleGenAI({
  apiKey: process.env.GEMINI_API_KEY
});

// Basic thinking-enabled request
async function generateWithThinking(prompt) {
  const response = await ai.models.generateContent({
    model: "gemini-2.5-pro", // or gemini-2.5-flash
    contents: prompt,
    // Thinking is enabled by default for 2.5 models
  });

  return response.text;
}

// Usage
const result = await generateWithThinking(
  "Explain the concept of Occam's Razor and provide a simple, everyday example."
);
```##
 Thinking Budgets

### Understanding Thinking Budgets
The `thinkingBudget` parameter controls how many thinking tokens the model uses when generating a response. Higher budgets allow for more detailed reasoning, while lower budgets prioritize speed.

### Model-Specific Budget Settings
| Model | Default | Range | Disable | Dynamic |
|-------|---------|-------|---------|---------|
| 2.5 Pro | Dynamic | 128-32768 | N/A | -1 |
| 2.5 Flash | Dynamic | 0-24576 | 0 | -1 |
| 2.5 Flash-Lite | No thinking | 512-24576 | 0 | -1 |

### Configuring Thinking Budgets
```javascript
// Set specific thinking budget
async function generateWithBudget(prompt, budget = 1024) {
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: prompt,
    config: {
      thinkingConfig: {
        thinkingBudget: budget
      }
    }
  });

  return response.text;
}

// Disable thinking for simple tasks
async function generateWithoutThinking(prompt) {
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: prompt,
    config: {
      thinkingConfig: {
        thinkingBudget: 0
      }
    }
  });

  return response.text;
}

// Enable dynamic thinking
async function generateWithDynamicThinking(prompt) {
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: prompt,
    config: {
      thinkingConfig: {
        thinkingBudget: -1
      }
    }
  });

  return response.text;
}
```## Thou
ght Summaries

### Accessing Thought Summaries
Thought summaries provide insights into the model's reasoning process. Enable them by setting `includeThoughts: true`.

```javascript
async function generateWithThoughtSummary(prompt) {
  const response = await ai.models.generateContent({
    model: "gemini-2.5-pro",
    contents: prompt,
    config: {
      thinkingConfig: {
        includeThoughts: true
      }
    }
  });

  // Process thoughts and answer separately
  const thoughts = [];
  const answers = [];

  for (const part of response.candidates[0].content.parts) {
    if (!part.text) continue;
    
    if (part.thought) {
      thoughts.push(part.text);
    } else {
      answers.push(part.text);
    }
  }

  return {
    thoughts: thoughts.join('\n'),
    answer: answers.join('\n'),
    fullResponse: response
  };
}

// Usage
const result = await generateWithThoughtSummary(
  "What is the sum of the first 50 prime numbers?"
);

console.log("Model's reasoning:", result.thoughts);
console.log("Final answer:", result.answer);
```

### Streaming Thought Summaries
```javascript
async function streamWithThoughts(prompt) {
  let thoughts = "";
  let answer = "";
  let thoughtsStarted = false;
  let answerStarted = false;

  const response = await ai.models.generateContentStream({
    model: "gemini-2.5-pro",
    contents: prompt,
    config: {
      thinkingConfig: {
        includeThoughts: true
      }
    }
  });

  for await (const chunk of response) {
    for (const part of chunk.candidates[0].content.parts) {
      if (!part.text) continue;

      if (part.thought) {
        if (!thoughtsStarted) {
          console.log("🤔 Model is thinking...");
          thoughtsStarted = true;
        }
        process.stdout.write(part.text);
        thoughts += part.text;
      } else {
        if (!answerStarted) {
          console.log("\n\n💡 Answer:");
          answerStarted = true;
        }
        process.stdout.write(part.text);
        answer += part.text;
      }
    }
  }

  return { thoughts, answer };
}
```## Tho
ught Signatures and Multi-Turn Conversations

### Understanding Thought Signatures
Thought signatures are encrypted representations of the model's internal thought process, used to maintain context across conversation turns when function calling is enabled.

```javascript
// Thought signatures are automatically included when:
// 1. Thinking is enabled
// 2. Function declarations are present

const weatherFunction = {
  name: 'get_weather',
  description: 'Get current weather for a location',
  parameters: {
    type: 'object',
    properties: {
      location: { type: 'string' }
    },
    required: ['location']
  }
};

async function chatWithThinking() {
  const chat = ai.chats.create({
    model: "gemini-2.5-flash",
    config: {
      tools: [{ functionDeclarations: [weatherFunction] }],
      thinkingConfig: {
        includeThoughts: true
      }
    }
  });

  // First turn - model will generate thought signatures
  const response1 = await chat.sendMessage({
    message: "What's the weather like in Tokyo? Think through the best approach."
  });

  // Thought signatures are automatically preserved in subsequent turns
  const response2 = await chat.sendMessage({
    message: "Now compare that with the weather in London."
  });

  return { response1, response2 };
}
```

### Important Notes for Thought Signatures
- Only available when function calling is enabled
- Don't concatenate parts with signatures
- Don't merge parts with and without signatures
- Return entire response with all parts to maintain context#
# Task Complexity and Thinking Strategy

### Easy Tasks (Thinking OFF)
For straightforward requests where complex reasoning isn't required:

```javascript
async function handleSimpleTasks(prompt) {
  // Disable thinking for efficiency
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: prompt,
    config: {
      thinkingConfig: {
        thinkingBudget: 0
      }
    }
  });

  return response.text;
}

// Examples of simple tasks:
const factRetrieval = await handleSimpleTasks("Where was DeepMind founded?");
const classification = await handleSimpleTasks("Is this email asking for a meeting or providing information?");
```

### Medium Tasks (Default Thinking)
For tasks requiring moderate reasoning:

```javascript
async function handleModerateTasks(prompt) {
  // Use default thinking (dynamic)
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: prompt
    // Default thinking is enabled
  });

  return response.text;
}

// Examples:
const analogy = await handleModerateTasks("Analogize photosynthesis and growing up.");
const comparison = await handleModerateTasks("Compare and contrast electric cars and hybrid cars.");
```

### Hard Tasks (Maximum Thinking)
For complex challenges requiring deep reasoning:

```javascript
async function handleComplexTasks(prompt) {
  const response = await ai.models.generateContent({
    model: "gemini-2.5-pro", // Use Pro for complex tasks
    contents: prompt,
    config: {
      thinkingConfig: {
        thinkingBudget: 32768, // Maximum budget for Pro
        includeThoughts: true   // See the reasoning process
      }
    }
  });

  return response;
}

// Examples:
const mathProblem = await handleComplexTasks(
  "Solve: Find the sum of all integer bases b > 9 for which 17_b is a divisor of 97_b."
);

const codingTask = await handleComplexTasks(
  "Write Python code for a web application that visualizes real-time stock market data, including user authentication. Make it as efficient as possible."
);
```## Think
ing with Tools and Capabilities

### Code Execution with Thinking
```javascript
async function solveWithCodeExecution(prompt) {
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: prompt,
    config: {
      tools: [{ codeExecution: {} }],
      thinkingConfig: {
        includeThoughts: true,
        thinkingBudget: 4096
      }
    }
  });

  return response;
}

// Usage
const mathSolution = await solveWithCodeExecution(
  "Calculate the standard deviation of the first 100 prime numbers and create a visualization."
);
```

### Google Search with Thinking
```javascript
async function researchWithThinking(prompt) {
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: prompt,
    config: {
      tools: [{ googleSearch: {} }],
      thinkingConfig: {
        includeThoughts: true
      }
    }
  });

  return response;
}

// Usage
const research = await researchWithThinking(
  "Research the latest developments in quantum computing and analyze their potential impact on cryptography."
);
```

### Function Calling with Thinking
```javascript
const databaseFunction = {
  name: 'query_database',
  description: 'Query customer database',
  parameters: {
    type: 'object',
    properties: {
      query: { type: 'string' },
      filters: { type: 'object' }
    },
    required: ['query']
  }
};

async function analyzeWithFunctions(prompt) {
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: prompt,
    config: {
      tools: [{ functionDeclarations: [databaseFunction] }],
      thinkingConfig: {
        includeThoughts: true,
        thinkingBudget: 2048
      }
    }
  });

  return response;
}
```#
# Pricing and Token Management

### Understanding Thinking Costs
```javascript
async function analyzeThinkingCosts(prompt) {
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: prompt,
    config: {
      thinkingConfig: {
        includeThoughts: true,
        thinkingBudget: 2048
      }
    }
  });

  const usage = response.usageMetadata;
  
  console.log(`Thinking tokens: ${usage.thoughtsTokenCount}`);
  console.log(`Output tokens: ${usage.candidatesTokenCount}`);
  console.log(`Total tokens: ${usage.totalTokenCount}`);
  
  // Calculate costs (example rates)
  const thinkingCost = usage.thoughtsTokenCount * 0.000125; // Input rate
  const outputCost = usage.candidatesTokenCount * 0.000375; // Output rate
  const totalCost = thinkingCost + outputCost;
  
  return {
    response: response.text,
    costs: {
      thinking: thinkingCost,
      output: outputCost,
      total: totalCost
    },
    tokens: usage
  };
}
```

### Cost Optimization Strategies
```javascript
class ThinkingOptimizer {
  constructor() {
    this.ai = new GoogleGenAI({});
  }

  async optimizeForTask(prompt, taskComplexity = 'medium') {
    const budgetMap = {
      simple: 0,        // No thinking
      medium: -1,       // Dynamic thinking
      complex: 4096,    // High thinking budget
      maximum: 16384    // Maximum thinking for hardest tasks
    };

    const modelMap = {
      simple: 'gemini-2.5-flash-lite',
      medium: 'gemini-2.5-flash',
      complex: 'gemini-2.5-flash',
      maximum: 'gemini-2.5-pro'
    };

    const response = await this.ai.models.generateContent({
      model: modelMap[taskComplexity],
      contents: prompt,
      config: {
        thinkingConfig: {
          thinkingBudget: budgetMap[taskComplexity],
          includeThoughts: taskComplexity !== 'simple'
        }
      }
    });

    return response;
  }

  async batchOptimize(tasks) {
    const results = [];
    
    for (const task of tasks) {
      const result = await this.optimizeForTask(task.prompt, task.complexity);
      results.push({
        task: task.prompt,
        complexity: task.complexity,
        result: result.text,
        tokens: result.usageMetadata
      });
    }

    return results;
  }
}

// Usage
const optimizer = new ThinkingOptimizer();
const results = await optimizer.batchOptimize([
  { prompt: "What is 2+2?", complexity: 'simple' },
  { prompt: "Explain quantum entanglement", complexity: 'medium' },
  { prompt: "Design a distributed system architecture", complexity: 'complex' }
]);
```## Advance
d Thinking Patterns

### Debugging with Thought Summaries
```javascript
async function debugWithThinking(prompt, expectedOutput) {
  const response = await ai.models.generateContent({
    model: "gemini-2.5-pro",
    contents: prompt,
    config: {
      thinkingConfig: {
        includeThoughts: true,
        thinkingBudget: 4096
      }
    }
  });

  const result = extractThoughtsAndAnswer(response);
  
  // Analyze if output matches expectations
  if (!result.answer.includes(expectedOutput)) {
    console.log("🔍 Debugging - Model's reasoning:");
    console.log(result.thoughts);
    
    // Provide corrective guidance
    const correctedResponse = await ai.models.generateContent({
      model: "gemini-2.5-pro",
      contents: `${prompt}\n\nPrevious reasoning: ${result.thoughts}\n\nPlease reconsider and ensure your answer includes: ${expectedOutput}`,
      config: {
        thinkingConfig: {
          includeThoughts: true,
          thinkingBudget: 4096
        }
      }
    });
    
    return extractThoughtsAndAnswer(correctedResponse);
  }
  
  return result;
}

function extractThoughtsAndAnswer(response) {
  const thoughts = [];
  const answers = [];
  
  for (const part of response.candidates[0].content.parts) {
    if (!part.text) continue;
    if (part.thought) {
      thoughts.push(part.text);
    } else {
      answers.push(part.text);
    }
  }
  
  return {
    thoughts: thoughts.join('\n'),
    answer: answers.join('\n')
  };
}
```

### Guided Reasoning
```javascript
async function guidedReasoning(prompt, reasoningConstraints) {
  const guidedPrompt = `${prompt}

Please approach this systematically:
${reasoningConstraints.map((constraint, i) => `${i + 1}. ${constraint}`).join('\n')}

Think through each step carefully before providing your final answer.`;

  const response = await ai.models.generateContent({
    model: "gemini-2.5-pro",
    contents: guidedPrompt,
    config: {
      thinkingConfig: {
        includeThoughts: true,
        thinkingBudget: 8192 // Higher budget for guided reasoning
      }
    }
  });

  return extractThoughtsAndAnswer(response);
}

// Usage
const result = await guidedReasoning(
  "Design a recommendation system for an e-commerce platform",
  [
    "First, identify the key stakeholders and their needs",
    "Consider different types of recommendation algorithms",
    "Evaluate data requirements and privacy concerns", 
    "Design the system architecture",
    "Plan for scalability and performance",
    "Consider A/B testing and evaluation metrics"
  ]
);
```## Re
al-World Applications

### Complex Problem Solving
```javascript
class ProblemSolver {
  constructor() {
    this.ai = new GoogleGenAI({});
  }

  async solveMathProblem(problem) {
    const response = await this.ai.models.generateContent({
      model: "gemini-2.5-pro",
      contents: `Solve this mathematical problem step by step: ${problem}`,
      config: {
        tools: [{ codeExecution: {} }],
        thinkingConfig: {
          includeThoughts: true,
          thinkingBudget: 16384
        }
      }
    });

    return this.processResponse(response);
  }

  async designSystem(requirements) {
    const response = await this.ai.models.generateContent({
      model: "gemini-2.5-pro",
      contents: `Design a system with these requirements: ${requirements}. Consider scalability, security, and maintainability.`,
      config: {
        thinkingConfig: {
          includeThoughts: true,
          thinkingBudget: 12288
        }
      }
    });

    return this.processResponse(response);
  }

  async debugCode(code, error) {
    const response = await this.ai.models.generateContent({
      model: "gemini-2.5-flash",
      contents: `Debug this code and fix the error:\n\nCode:\n${code}\n\nError:\n${error}`,
      config: {
        tools: [{ codeExecution: {} }],
        thinkingConfig: {
          includeThoughts: true,
          thinkingBudget: 4096
        }
      }
    });

    return this.processResponse(response);
  }

  processResponse(response) {
    const thoughts = [];
    const answers = [];
    const codeResults = [];

    for (const part of response.candidates[0].content.parts) {
      if (part.text) {
        if (part.thought) {
          thoughts.push(part.text);
        } else {
          answers.push(part.text);
        }
      } else if (part.codeExecutionResult) {
        codeResults.push(part.codeExecutionResult.output);
      }
    }

    return {
      reasoning: thoughts.join('\n'),
      solution: answers.join('\n'),
      codeOutput: codeResults.join('\n'),
      tokens: response.usageMetadata
    };
  }
}

// Usage
const solver = new ProblemSolver();

const mathResult = await solver.solveMathProblem(
  "Find the area under the curve y = x^2 from x = 0 to x = 5"
);

const systemDesign = await solver.designSystem(
  "A real-time chat application supporting 1M concurrent users"
);
```## 
Best Practices and Optimization

### Thinking Budget Selection Guide
```javascript
function selectOptimalBudget(taskType, complexity, latencyRequirement) {
  const budgetMatrix = {
    'fact-retrieval': { low: 0, medium: 0, high: 0 },
    'classification': { low: 0, medium: 512, high: 1024 },
    'analysis': { low: 1024, medium: 2048, high: 4096 },
    'coding': { low: 2048, medium: 4096, high: 8192 },
    'math': { low: 4096, medium: 8192, high: 16384 },
    'research': { low: 2048, medium: 4096, high: 8192 },
    'creative': { low: 1024, medium: 2048, high: 4096 }
  };

  let budget = budgetMatrix[taskType]?.[complexity] || 2048;
  
  // Adjust for latency requirements
  if (latencyRequirement === 'fast') {
    budget = Math.min(budget, 1024);
  } else if (latencyRequirement === 'quality') {
    budget = Math.max(budget, 2048);
  }
  
  return budget;
}

async function optimizedGeneration(prompt, taskType, complexity = 'medium', latencyRequirement = 'balanced') {
  const budget = selectOptimalBudget(taskType, complexity, latencyRequirement);
  const model = complexity === 'high' ? 'gemini-2.5-pro' : 'gemini-2.5-flash';
  
  const response = await ai.models.generateContent({
    model,
    contents: prompt,
    config: {
      thinkingConfig: {
        thinkingBudget: budget,
        includeThoughts: complexity !== 'low'
      }
    }
  });

  return response;
}
```

### Error Handling and Retry Logic
```javascript
async function robustThinkingGeneration(prompt, options = {}) {
  const {
    maxRetries = 3,
    fallbackBudget = 1024,
    timeoutMs = 60000
  } = options;

  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

      const response = await ai.models.generateContent({
        model: "gemini-2.5-flash",
        contents: prompt,
        config: {
          thinkingConfig: {
            thinkingBudget: attempt === 1 ? options.thinkingBudget || -1 : fallbackBudget,
            includeThoughts: options.includeThoughts || false
          }
        }
      });

      clearTimeout(timeoutId);
      return response;
    } catch (error) {
      console.error(`Thinking generation attempt ${attempt} failed:`, error.message);
      
      if (attempt === maxRetries) {
        // Final fallback: disable thinking
        return await ai.models.generateContent({
          model: "gemini-2.5-flash",
          contents: prompt,
          config: {
            thinkingConfig: {
              thinkingBudget: 0
            }
          }
        });
      }
      
      // Wait before retry
      await new Promise(resolve => setTimeout(resolve, 1000 * attempt));
    }
  }
}
```## In
tegration Examples

### Express.js API with Thinking
```javascript
app.post('/api/solve', async (req, res) => {
  try {
    const { prompt, complexity = 'medium', includeReasoning = false } = req.body;
    
    const budgetMap = {
      simple: 0,
      medium: 2048,
      complex: 8192,
      maximum: 16384
    };

    const response = await ai.models.generateContent({
      model: complexity === 'maximum' ? 'gemini-2.5-pro' : 'gemini-2.5-flash',
      contents: prompt,
      config: {
        thinkingConfig: {
          thinkingBudget: budgetMap[complexity],
          includeThoughts: includeReasoning
        }
      }
    });

    const result = {
      answer: response.text,
      tokens: response.usageMetadata,
      complexity
    };

    if (includeReasoning) {
      const processed = extractThoughtsAndAnswer(response);
      result.reasoning = processed.thoughts;
      result.answer = processed.answer;
    }

    res.json(result);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});
```

### Thinking Service Class
```javascript
class ThinkingService {
  constructor() {
    this.ai = new GoogleGenAI({});
    this.cache = new Map();
  }

  async solve(prompt, options = {}) {
    const {
      complexity = 'medium',
      includeReasoning = false,
      useCache = true,
      tools = []
    } = options;

    const cacheKey = this.generateCacheKey(prompt, options);
    
    if (useCache && this.cache.has(cacheKey)) {
      return this.cache.get(cacheKey);
    }

    const config = this.buildConfig(complexity, includeReasoning, tools);
    
    const response = await this.ai.models.generateContent({
      model: this.selectModel(complexity),
      contents: prompt,
      config
    });

    const result = this.processResponse(response, includeReasoning);
    
    if (useCache) {
      this.cache.set(cacheKey, result);
    }

    return result;
  }

  buildConfig(complexity, includeReasoning, tools) {
    const budgetMap = {
      simple: 0,
      medium: 2048,
      complex: 8192,
      maximum: 16384
    };

    const config = {
      thinkingConfig: {
        thinkingBudget: budgetMap[complexity],
        includeThoughts: includeReasoning
      }
    };

    if (tools.length > 0) {
      config.tools = tools;
    }

    return config;
  }

  selectModel(complexity) {
    return complexity === 'maximum' ? 'gemini-2.5-pro' : 'gemini-2.5-flash';
  }

  processResponse(response, includeReasoning) {
    if (!includeReasoning) {
      return {
        answer: response.text,
        tokens: response.usageMetadata
      };
    }

    return {
      ...extractThoughtsAndAnswer(response),
      tokens: response.usageMetadata
    };
  }

  generateCacheKey(prompt, options) {
    return `${prompt}:${JSON.stringify(options)}`;
  }
}

// Usage
const thinkingService = new ThinkingService();

const result = await thinkingService.solve(
  "Design a scalable microservices architecture for an e-commerce platform",
  {
    complexity: 'complex',
    includeReasoning: true,
    tools: [{ codeExecution: {} }]
  }
);
```## Mode
l Capabilities and Limitations

### Supported Models
```javascript
const THINKING_MODELS = {
  'gemini-2.5-pro': {
    thinking: true,
    defaultBudget: 'dynamic',
    budgetRange: [128, 32768],
    canDisable: false,
    bestFor: ['complex reasoning', 'advanced math', 'system design']
  },
  'gemini-2.5-flash': {
    thinking: true,
    defaultBudget: 'dynamic', 
    budgetRange: [0, 24576],
    canDisable: true,
    bestFor: ['general tasks', 'coding', 'analysis']
  },
  'gemini-2.5-flash-lite': {
    thinking: false,
    defaultBudget: 'none',
    budgetRange: [512, 24576],
    canDisable: true,
    bestFor: ['simple tasks', 'fast responses']
  }
};

function getModelCapabilities(modelName) {
  return THINKING_MODELS[modelName] || null;
}

function recommendModel(taskType, complexityLevel, latencyRequirement) {
  if (latencyRequirement === 'fast' && complexityLevel === 'simple') {
    return 'gemini-2.5-flash-lite';
  }
  
  if (complexityLevel === 'maximum' || taskType === 'advanced-math') {
    return 'gemini-2.5-pro';
  }
  
  return 'gemini-2.5-flash';
}
```

### Performance Monitoring
```javascript
class ThinkingPerformanceMonitor {
  constructor() {
    this.metrics = [];
  }

  async measurePerformance(prompt, config) {
    const startTime = Date.now();
    
    const response = await ai.models.generateContent({
      model: config.model,
      contents: prompt,
      config: config.generationConfig
    });
    
    const endTime = Date.now();
    const duration = endTime - startTime;
    
    const metrics = {
      prompt,
      model: config.model,
      thinkingBudget: config.generationConfig?.thinkingConfig?.thinkingBudget,
      duration,
      tokens: response.usageMetadata,
      timestamp: new Date().toISOString()
    };
    
    this.metrics.push(metrics);
    return { response, metrics };
  }

  getAveragePerformance(model, budgetRange) {
    const filtered = this.metrics.filter(m => 
      m.model === model && 
      m.thinkingBudget >= budgetRange[0] && 
      m.thinkingBudget <= budgetRange[1]
    );
    
    if (filtered.length === 0) return null;
    
    const avgDuration = filtered.reduce((sum, m) => sum + m.duration, 0) / filtered.length;
    const avgThinkingTokens = filtered.reduce((sum, m) => sum + (m.tokens.thoughtsTokenCount || 0), 0) / filtered.length;
    
    return {
      sampleSize: filtered.length,
      averageDuration: avgDuration,
      averageThinkingTokens: avgThinkingTokens,
      budgetRange
    };
  }

  exportMetrics() {
    return {
      totalRequests: this.metrics.length,
      metrics: this.metrics,
      summary: this.generateSummary()
    };
  }

  generateSummary() {
    const byModel = {};
    
    this.metrics.forEach(m => {
      if (!byModel[m.model]) {
        byModel[m.model] = { count: 0, totalDuration: 0, totalThinkingTokens: 0 };
      }
      
      byModel[m.model].count++;
      byModel[m.model].totalDuration += m.duration;
      byModel[m.model].totalThinkingTokens += m.tokens.thoughtsTokenCount || 0;
    });
    
    Object.keys(byModel).forEach(model => {
      const data = byModel[model];
      data.avgDuration = data.totalDuration / data.count;
      data.avgThinkingTokens = data.totalThinkingTokens / data.count;
    });
    
    return byModel;
  }
}
```## B
est Practices Summary

### When to Use Thinking
1. **Enable thinking for:**
   - Complex problem-solving tasks
   - Multi-step reasoning requirements
   - Mathematical computations
   - Code generation and debugging
   - System design and architecture
   - Research and analysis tasks

2. **Disable thinking for:**
   - Simple fact retrieval
   - Basic classification tasks
   - Quick responses needed
   - Cost-sensitive applications
   - High-frequency API calls

### Optimization Strategies
1. **Budget Management:**
   - Start with dynamic thinking (-1) for unknown complexity
   - Use specific budgets for known task types
   - Monitor token usage and adjust accordingly
   - Consider latency vs. quality trade-offs

2. **Model Selection:**
   - Use Flash-Lite for simple, fast tasks
   - Use Flash for general-purpose thinking
   - Use Pro for maximum reasoning capability
   - Consider cost implications of model choice

3. **Thought Summaries:**
   - Enable for debugging and transparency
   - Use streaming for real-time insight
   - Analyze reasoning patterns for prompt improvement
   - Disable for production when not needed

### Performance Tips
1. **Caching:** Cache results for repeated similar queries
2. **Batching:** Group similar complexity tasks together
3. **Monitoring:** Track performance metrics and token usage
4. **Fallbacks:** Implement graceful degradation strategies
5. **Testing:** A/B test different thinking configurations

### Integration Considerations
1. **Function Calling:** Thought signatures maintain context automatically
2. **Tools:** All Gemini tools work with thinking models
3. **Streaming:** Use for better user experience with long reasoning
4. **Error Handling:** Implement robust retry logic with budget fallbacks
5. **Cost Management:** Monitor thinking token usage in production

This comprehensive guide provides everything needed to effectively leverage Gemini's thinking capabilities for enhanced reasoning and problem-solving in JavaScript/TypeScript applications.