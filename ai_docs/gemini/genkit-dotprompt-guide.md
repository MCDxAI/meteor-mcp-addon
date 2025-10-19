# Genkit Dotprompt Guide

## Overview
Dotprompt is Genkit's powerful prompt management system that treats prompts as code. It enables rapid iteration, version control, and collaborative prompt engineering through `.prompt` files with YAML front matter and Handlebars templating.

## Why Use Dotprompt?

### Key Benefits
1. **Prompts as Code**: Version control, collaboration, and systematic iteration
2. **Model-Agnostic**: Switch models and parameters without code changes
3. **Template System**: Dynamic prompts with Handlebars templating
4. **Schema Validation**: Type-safe inputs and structured outputs
5. **Developer UI Integration**: Visual prompt testing and refinement
6. **Reusable Components**: Partials and helpers for prompt composition

## Project Setup

### Directory Structure
```
your-project/
├── src/
├── prompts/                    # Default prompt directory
│   ├── chat/
│   │   ├── greeting.prompt
│   │   └── farewell.prompt
│   ├── analysis/
│   │   ├── sentiment.prompt
│   │   └── classification.prompt
│   ├── _partials/              # Reusable prompt components
│   │   ├── _personality.prompt
│   │   └── _context.prompt
│   └── menu-suggestion.prompt
├── package.json
└── tsconfig.json
```

### Custom Prompt Directory
```javascript
const ai = genkit({
  promptDir: './llm_prompts',  // Custom directory
  plugins: [googleAI()],
});
```

## Basic Prompt Files

### Simple Prompt Structure
```yaml
---
model: googleai/gemini-2.5-flash
config:
  temperature: 0.7
  maxOutputTokens: 1000
---
You are a helpful AI assistant. Please help the user with their request.
```

### Prompt with Input Schema
```yaml
---
model: googleai/gemini-2.5-flash
config:
  temperature: 0.8
input:
  schema:
    theme: string
    dietaryRestrictions?: string
  default:
    theme: "casual dining"
---
Create a creative menu item for a {{theme}} restaurant.
{{#if dietaryRestrictions}}
Please ensure it meets these dietary requirements: {{dietaryRestrictions}}
{{/if}}
```

### Structured Output Prompt
```yaml
---
model: googleai/gemini-2.5-flash
input:
  schema:
    productDescription: string
    targetAudience: string
output:
  schema:
    headline: string
    description: string
    keyFeatures: array
      items: string
    callToAction: string
    targetKeywords: array
      items: string
---
Create marketing copy for this product: {{productDescription}}
Target audience: {{targetAudience}}

Generate compelling marketing content that resonates with the target audience.
```

## Schema Definition Methods

### Picoschema (Recommended)
```yaml
---
model: googleai/gemini-2.5-flash
input:
  schema:
    title: string
    content: string
    metadata?: object
      author: string
      publishDate: string, ISO date format
      tags(array): string
      priority(enum): [HIGH, MEDIUM, LOW]
output:
  schema:
    summary: string
    keyPoints(array): string
    sentiment(enum): [POSITIVE, NEGATIVE, NEUTRAL]
    confidence: number, between 0 and 1
    recommendations(array):
      action: string
      priority: number
      reasoning: string
---
Analyze this article and provide insights:

Title: {{title}}
Content: {{content}}

{{#if metadata}}
Author: {{metadata.author}}
Published: {{metadata.publishDate}}
Tags: {{#each metadata.tags}}{{this}}{{#unless @last}}, {{/unless}}{{/each}}
{{/if}}
```

### JSON Schema
```yaml
---
model: googleai/gemini-2.5-flash
input:
  schema:
    type: object
    properties:
      userQuery:
        type: string
        description: "User's search query"
      filters:
        type: object
        properties:
          category:
            type: string
            enum: ["tech", "business", "health", "entertainment"]
          dateRange:
            type: string
            pattern: "^\\d{4}-\\d{2}-\\d{2}$"
        required: ["category"]
    required: ["userQuery"]
output:
  schema:
    type: object
    properties:
      results:
        type: array
        items:
          type: object
          properties:
            title: { type: string }
            snippet: { type: string }
            relevanceScore: { type: number, minimum: 0, maximum: 1 }
      totalFound: { type: number }
      searchTime: { type: number }
---
Search for: {{userQuery}}
{{#if filters.category}}
Category: {{filters.category}}
{{/if}}
{{#if filters.dateRange}}
Date range: {{filters.dateRange}}
{{/if}}
```

### Zod Schema References
```javascript
// Define schemas in code
const ArticleSchema = ai.defineSchema(
  'ArticleSchema',
  z.object({
    title: z.string(),
    summary: z.string(),
    keyPoints: z.array(z.string()),
    sentiment: z.enum(['positive', 'negative', 'neutral']),
    confidence: z.number().min(0).max(1)
  })
);

const UserInputSchema = ai.defineSchema(
  'UserInputSchema',
  z.object({
    content: z.string(),
    analysisType: z.enum(['summary', 'sentiment', 'classification'])
  })
);
```

```yaml
---
model: googleai/gemini-2.5-flash
input:
  schema: UserInputSchema
output:
  schema: ArticleSchema
---
Perform {{analysisType}} analysis on this content:

{{content}}
```

## Handlebars Templating

### Basic Template Features
```yaml
---
model: googleai/gemini-2.5-flash
input:
  schema:
    userName?: string
    userType: string
    requestType: string
    urgency?: string
---
{{#if userName}}
Hello {{userName}},
{{else}}
Hello there,
{{/if}}

{{#switch userType}}
  {{#case "premium"}}
    As a premium member, you have priority support.
  {{/case}}
  {{#case "business"}}
    Thank you for choosing our business plan.
  {{/case}}
  {{#default}}
    Thank you for using our service.
  {{/default}}
{{/switch}}

Request type: {{requestType}}

{{#if urgency}}
  {{#eq urgency "high"}}
    🚨 This is marked as high priority and will be handled immediately.
  {{/eq}}
  {{#eq urgency "medium"}}
    ⚡ This request has medium priority.
  {{/eq}}
{{/if}}

How can I assist you today?
```

### Advanced Conditionals and Loops
```yaml
---
model: googleai/gemini-2.5-flash
input:
  schema:
    products(array):
      name: string
      price: number
      category: string
      inStock: boolean
      features(array): string
    filters?:
      maxPrice?: number
      category?: string
      inStockOnly?: boolean
---
Here are the available products:

{{#each products}}
  {{#if (and 
    (or (not ../filters.maxPrice) (lte price ../filters.maxPrice))
    (or (not ../filters.category) (eq category ../filters.category))
    (or (not ../filters.inStockOnly) inStock)
  )}}
    
    **{{name}}** - ${{price}}
    Category: {{category}}
    {{#if inStock}}✅ In Stock{{else}}❌ Out of Stock{{/if}}
    
    {{#if features}}
    Features:
    {{#each features}}
    - {{this}}
    {{/each}}
    {{/if}}
    
  {{/if}}
{{/each}}

{{#if filters}}
Applied filters:
{{#if filters.maxPrice}}• Max price: ${{filters.maxPrice}}{{/if}}
{{#if filters.category}}• Category: {{filters.category}}{{/if}}
{{#if filters.inStockOnly}}• In stock only{{/if}}
{{/if}}
```

## Multi-Message and Multi-Modal Prompts

### Multi-Message Prompts
```yaml
---
model: googleai/gemini-2.5-flash
input:
  schema:
    userQuestion: string
    context?: string
    expertise: string
---
{{role "system"}}
You are an expert {{expertise}} consultant with years of experience. 
Your responses should be professional, accurate, and tailored to the user's level of understanding.

{{#if context}}
Additional context: {{context}}
{{/if}}

{{role "user"}}
{{userQuestion}}

{{role "assistant"}}
I'll help you with your {{expertise}} question. Let me provide a comprehensive answer.

{{role "user"}}
Please provide your expert analysis and recommendations.
```

### Multi-Modal Prompts
```yaml
---
model: googleai/gemini-2.5-flash
input:
  schema:
    imageUrl: string
    analysisType: string
    includeDetails?: boolean
---
{{#switch analysisType}}
  {{#case "product"}}
    Analyze this product image for marketing purposes:
  {{/case}}
  {{#case "medical"}}
    Provide a medical analysis of this image (for educational purposes only):
  {{/case}}
  {{#case "technical"}}
    Perform a technical analysis of this image:
  {{/case}}
  {{#default}}
    Analyze this image:
  {{/default}}
{{/switch}}

{{media url=imageUrl}}

{{#if includeDetails}}
Please provide detailed observations including:
- Visual elements and composition
- Technical aspects (if applicable)
- Potential issues or areas of interest
- Recommendations or next steps
{{else}}
Provide a concise analysis focusing on the key points.
{{/if}}
```

### Document Analysis with Context
```yaml
---
model: googleai/gemini-2.5-flash
input:
  schema:
    documentUrl: string
    analysisGoals(array): string
    outputFormat: string
    confidential?: boolean
---
{{#if confidential}}
⚠️ CONFIDENTIAL DOCUMENT ANALYSIS ⚠️
Please maintain strict confidentiality of all content.
{{/if}}

Analyze the following document:
{{media url=documentUrl}}

Analysis Goals:
{{#each analysisGoals}}
{{@index}}. {{this}}
{{/each}}

{{#switch outputFormat}}
  {{#case "executive_summary"}}
    Provide an executive summary suitable for C-level presentation.
  {{/case}}
  {{#case "technical_report"}}
    Create a detailed technical report with specific findings.
  {{/case}}
  {{#case "bullet_points"}}
    Summarize key findings in bullet point format.
  {{/case}}
  {{#default}}
    Provide a comprehensive analysis.
  {{/default}}
{{/switch}}

{{#if confidential}}
Remember: This analysis must remain confidential and should not reference specific proprietary information in the output.
{{/if}}
```

## Partials and Reusable Components

### Creating Partials
```yaml
# _partials/_personality.prompt
---
# This is a partial - no model configuration needed
---
{{#switch style}}
  {{#case "professional"}}
    I'm here to provide professional, accurate assistance.
  {{/case}}
  {{#case "friendly"}}
    Hey there! I'm excited to help you out today! 😊
  {{/case}}
  {{#case "technical"}}
    I'll provide precise, technical information to address your query.
  {{/case}}
  {{#default}}
    I'm here to help you with whatever you need.
  {{/default}}
{{/switch}}
```

```yaml
# _partials/_context_header.prompt
---
# Context header partial
---
{{#if @auth.uid}}
User: {{@auth.email}}
{{#if @auth.roles}}
Roles: {{#each @auth.roles}}{{this}}{{#unless @last}}, {{/unless}}{{/each}}
{{/if}}
{{#if @session.preferences}}
Preferences: Language={{@session.preferences.language}}, Timezone={{@session.preferences.timezone}}
{{/if}}
---
{{/if}}
```

### Using Partials in Prompts
```yaml
---
model: googleai/gemini-2.5-flash
input:
  schema:
    query: string
    style?: string
    includeContext?: boolean
---
{{#if includeContext}}
{{>context_header}}
{{/if}}

{{>personality style=style}}

User Query: {{query}}

Please provide a helpful response that matches the requested style and takes into account any user context provided.
```

### Advanced Partial Usage
```yaml
# _partials/_product_card.prompt
---
# Product card partial for e-commerce
---
**{{name}}** {{#if onSale}}🏷️ SALE{{/if}}
Price: {{#if salePrice}}~~${{originalPrice}}~~ ${{salePrice}}{{else}}${{price}}{{/if}}
Rating: {{#repeat rating}}⭐{{/repeat}} ({{reviewCount}} reviews)

{{#if features}}
Key Features:
{{#each features}}
• {{this}}
{{/each}}
{{/if}}

{{#if inStock}}
✅ In Stock - Ships within {{shippingDays}} days
{{else}}
❌ Currently out of stock
{{/if}}
```

```yaml
---
model: googleai/gemini-2.5-flash
input:
  schema:
    products(array):
      name: string
      price: number
      salePrice?: number
      originalPrice?: number
      rating: number
      reviewCount: number
      features(array): string
      inStock: boolean
      shippingDays: number
      onSale?: boolean
    userQuery: string
---
Based on your search for "{{userQuery}}", here are the matching products:

{{#each products}}
{{>product_card this}}

---
{{/each}}

Would you like more details about any of these products, or would you like to refine your search?
```

## Custom Helpers

### Defining Custom Helpers
```javascript
// Register custom helpers
ai.defineHelper('formatCurrency', (amount, currency = 'USD') => {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: currency
  }).format(amount);
});

ai.defineHelper('formatDate', (dateString, format = 'short') => {
  const date = new Date(dateString);
  const options = {
    short: { month: 'short', day: 'numeric', year: 'numeric' },
    long: { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' },
    time: { hour: '2-digit', minute: '2-digit', timeZoneName: 'short' }
  };
  return date.toLocaleDateString('en-US', options[format]);
});

ai.defineHelper('truncate', (text, length = 100) => {
  return text.length > length ? text.substring(0, length) + '...' : text;
});

ai.defineHelper('highlight', (text, searchTerm) => {
  if (!searchTerm) return text;
  const regex = new RegExp(`(${searchTerm})`, 'gi');
  return text.replace(regex, '**$1**');
});

ai.defineHelper('calculateDiscount', (originalPrice, salePrice) => {
  const discount = ((originalPrice - salePrice) / originalPrice) * 100;
  return Math.round(discount);
});
```

### Using Custom Helpers in Prompts
```yaml
---
model: googleai/gemini-2.5-flash
input:
  schema:
    orders(array):
      id: string
      customerName: string
      items(array):
        name: string
        price: number
        quantity: number
      orderDate: string
      status: string
      total: number
    searchTerm?: string
---
# Order Management Dashboard

{{#each orders}}
## Order #{{id}}
**Customer:** {{#if ../searchTerm}}{{highlight customerName ../searchTerm}}{{else}}{{customerName}}{{/if}}
**Date:** {{formatDate orderDate 'long'}}
**Status:** {{status}}
**Total:** {{formatCurrency total}}

### Items:
{{#each items}}
- {{name}} ({{quantity}}x {{formatCurrency price}})
{{/each}}

**Order Summary:** {{truncate (concat customerName " ordered " items.length " items") 50}}

---
{{/each}}

Generated on: {{formatDate (now) 'time'}}
```

## Prompt Variants and A/B Testing

### Creating Variants
```
prompts/
├── greeting.prompt              # Baseline version
├── greeting.friendly.prompt     # Friendly variant
├── greeting.professional.prompt # Professional variant
└── greeting.casual.prompt       # Casual variant
```

### Baseline Prompt
```yaml
# greeting.prompt
---
model: googleai/gemini-2.5-flash
config:
  temperature: 0.7
input:
  schema:
    userName?: string
    timeOfDay: string
---
{{#if userName}}
Good {{timeOfDay}}, {{userName}}! 
{{else}}
Good {{timeOfDay}}!
{{/if}}

How can I assist you today?
```

### Friendly Variant
```yaml
# greeting.friendly.prompt
---
model: googleai/gemini-2.5-flash
config:
  temperature: 0.9
input:
  schema:
    userName?: string
    timeOfDay: string
---
{{#if userName}}
Hey {{userName}}! 🌟 Hope you're having a wonderful {{timeOfDay}}!
{{else}}
Hello there! 🌟 Hope you're having a great {{timeOfDay}}!
{{/if}}

I'm super excited to help you out today! What can I do for you? 😊
```

### Professional Variant
```yaml
# greeting.professional.prompt
---
model: googleai/gemini-2.5-flash
config:
  temperature: 0.3
input:
  schema:
    userName?: string
    timeOfDay: string
---
{{#if userName}}
Good {{timeOfDay}}, {{userName}}.
{{else}}
Good {{timeOfDay}}.
{{/if}}

I am ready to provide professional assistance with your inquiries. Please let me know how I may be of service.
```

### Using Variants in Code
```javascript
// Load specific variant
const friendlyGreeting = ai.prompt('greeting', { variant: 'friendly' });
const professionalGreeting = ai.prompt('greeting', { variant: 'professional' });

// A/B testing implementation
async function greetUser(userData, testGroup) {
  const variants = {
    control: ai.prompt('greeting'),
    friendly: ai.prompt('greeting', { variant: 'friendly' }),
    professional: ai.prompt('greeting', { variant: 'professional' })
  };

  const selectedPrompt = variants[testGroup] || variants.control;
  
  const response = await selectedPrompt({
    userName: userData.name,
    timeOfDay: getTimeOfDay()
  });

  // Log for A/B testing analysis
  logVariantUsage(testGroup, userData.id, response.text);
  
  return response.text;
}
```

## Running and Testing Prompts

### Loading and Executing Prompts
```javascript
// Basic prompt execution
const menuPrompt = ai.prompt('menu-suggestion');
const result = await menuPrompt({
  theme: 'Italian',
  dietaryRestrictions: 'vegetarian'
});

// With configuration overrides
const result2 = await menuPrompt(
  { theme: 'Japanese' },
  {
    config: {
      temperature: 0.5,
      maxOutputTokens: 500
    }
  }
);

// Streaming execution
const { response, stream } = menuPrompt.stream({
  theme: 'French',
  dietaryRestrictions: 'gluten-free'
});

for await (const chunk of stream) {
  console.log(chunk.text);
}

const finalResponse = await response;
```

### Prompt Testing Framework
```javascript
class PromptTester {
  constructor() {
    this.testResults = [];
  }

  async testPrompt(promptName, testCases, variant = null) {
    const prompt = ai.prompt(promptName, variant ? { variant } : {});
    
    for (const testCase of testCases) {
      const startTime = Date.now();
      
      try {
        const result = await prompt(testCase.input);
        const executionTime = Date.now() - startTime;
        
        const testResult = {
          promptName,
          variant,
          input: testCase.input,
          output: result.text || result,
          expected: testCase.expected,
          passed: this.evaluateResult(result, testCase.expected),
          executionTime,
          timestamp: new Date().toISOString()
        };
        
        this.testResults.push(testResult);
        
        if (testCase.validator) {
          testResult.customValidation = testCase.validator(result);
        }
        
      } catch (error) {
        this.testResults.push({
          promptName,
          variant,
          input: testCase.input,
          error: error.message,
          passed: false,
          executionTime: Date.now() - startTime,
          timestamp: new Date().toISOString()
        });
      }
    }
  }

  evaluateResult(actual, expected) {
    if (typeof expected === 'string') {
      return actual.text?.includes(expected) || false;
    }
    if (typeof expected === 'function') {
      return expected(actual);
    }
    return true; // Default pass if no specific expectation
  }

  generateReport() {
    const passed = this.testResults.filter(r => r.passed).length;
    const total = this.testResults.length;
    const avgExecutionTime = this.testResults.reduce((sum, r) => sum + r.executionTime, 0) / total;

    return {
      summary: {
        total,
        passed,
        failed: total - passed,
        passRate: (passed / total) * 100,
        avgExecutionTime
      },
      results: this.testResults
    };
  }
}

// Usage
const tester = new PromptTester();

const testCases = [
  {
    input: { theme: 'Italian', dietaryRestrictions: 'vegetarian' },
    expected: (result) => result.text.toLowerCase().includes('vegetarian'),
    validator: (result) => result.text.length > 50
  },
  {
    input: { theme: 'Mexican' },
    expected: 'Mexican'
  }
];

await tester.testPrompt('menu-suggestion', testCases);
await tester.testPrompt('menu-suggestion', testCases, 'creative');

const report = tester.generateReport();
console.log(report);
```

## Integration with Flows

### Prompt-Based Flows
```javascript
// Define prompts in files, use in flows
export const analysisFlow = ai.defineFlow({
  name: 'analysisFlow',
  inputSchema: z.object({
    content: z.string(),
    analysisType: z.enum(['sentiment', 'summary', 'classification']),
    options: z.object({
      includeConfidence: z.boolean().default(true),
      detailLevel: z.enum(['brief', 'detailed']).default('detailed')
    }).optional()
  }),
  outputSchema: z.object({
    analysis: z.any(),
    confidence: z.number().optional(),
    processingTime: z.number()
  })
}, async (input) => {
  const startTime = Date.now();
  
  // Load appropriate prompt based on analysis type
  const analysisPrompt = ai.prompt(`analysis-${input.analysisType}`);
  
  const result = await analysisPrompt({
    content: input.content,
    detailLevel: input.options?.detailLevel || 'detailed',
    includeConfidence: input.options?.includeConfidence || true
  });

  return {
    analysis: result.output || result.text,
    confidence: result.output?.confidence,
    processingTime: Date.now() - startTime
  };
});
```

### Dynamic Prompt Selection
```javascript
export const adaptiveResponseFlow = ai.defineFlow({
  name: 'adaptiveResponseFlow',
  inputSchema: z.object({
    userMessage: z.string(),
    userProfile: z.object({
      experienceLevel: z.enum(['beginner', 'intermediate', 'expert']),
      preferredStyle: z.enum(['casual', 'professional', 'technical']),
      language: z.string().default('en')
    }),
    context: z.object({
      previousMessages: z.array(z.string()).optional(),
      sessionType: z.enum(['support', 'sales', 'general']).default('general')
    }).optional()
  }),
  outputSchema: z.object({
    response: z.string(),
    promptUsed: z.string(),
    adaptations: z.array(z.string())
  })
}, async (input) => {
  // Select prompt based on user profile and context
  const promptName = selectOptimalPrompt(input.userProfile, input.context);
  const prompt = ai.prompt(promptName);
  
  // Prepare prompt input with user context
  const promptInput = {
    userMessage: input.userMessage,
    experienceLevel: input.userProfile.experienceLevel,
    style: input.userProfile.preferredStyle,
    previousContext: input.context?.previousMessages?.slice(-3).join('\n'),
    sessionType: input.context?.sessionType
  };

  const result = await prompt(promptInput);
  
  return {
    response: result.text,
    promptUsed: promptName,
    adaptations: getAdaptationReasons(input.userProfile, input.context)
  };
});

function selectOptimalPrompt(userProfile, context) {
  const { experienceLevel, preferredStyle } = userProfile;
  const sessionType = context?.sessionType || 'general';
  
  // Dynamic prompt selection logic
  if (sessionType === 'support' && experienceLevel === 'beginner') {
    return 'support-beginner-friendly';
  }
  
  if (sessionType === 'sales' && preferredStyle === 'professional') {
    return 'sales-professional';
  }
  
  return `${sessionType}-${preferredStyle}-${experienceLevel}`;
}
```

## Best Practices Summary

### Prompt Design
1. **Clear structure**: Use consistent YAML front matter and template organization
2. **Schema validation**: Always define input/output schemas for type safety
3. **Template logic**: Use Handlebars conditionals and loops effectively
4. **Reusable components**: Create partials for common prompt patterns

### Model Configuration
1. **Model selection**: Choose appropriate models for different prompt types
2. **Parameter tuning**: Adjust temperature, tokens, and other parameters per prompt
3. **Variant testing**: Use variants for A/B testing and optimization
4. **Environment configs**: Different settings for development vs production

### Development Workflow
1. **Version control**: Commit prompt files to track changes over time
2. **Collaborative editing**: Enable non-technical team members to edit prompts
3. **Testing framework**: Implement systematic prompt testing and validation
4. **Performance monitoring**: Track prompt performance and effectiveness

### Integration
1. **Flow integration**: Use prompts within flows for complex workflows
2. **Dynamic selection**: Implement adaptive prompt selection based on context
3. **Error handling**: Graceful fallbacks when prompts fail
4. **Observability**: Monitor prompt usage and performance in production