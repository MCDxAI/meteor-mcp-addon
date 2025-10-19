# Gemini API Basics

## Overview
The Gemini API is Google's generative AI API that provides access to powerful language models for text generation, multimodal understanding, and more. This steering file contains essential information for working with the Gemini API in JavaScript/TypeScript projects.

## Key Models Available

### Gemini 2.5 Pro
- Most powerful thinking model with features for complex reasoning
- Best for complex tasks requiring deep analysis

### Gemini 2.5 Flash  
- Newest multimodal model with next generation features and improved capabilities
- Balanced performance and speed

### Gemini 2.5 Flash-Lite
- Fastest and most cost-efficient multimodal model
- Great performance for high-frequency tasks

### Specialized Models
- **Veo 3**: State of the art video generation model
- **Imagen 4**: Highest quality image generation model
- **Gemini Embeddings**: First Gemini embedding model for production RAG workflows

## JavaScript/TypeScript Setup

### Installation
```bash
npm install @google/genai
```

### Basic Usage Pattern
```javascript
import { GoogleGenAI } from "@google/genai";

const ai = new GoogleGenAI({});

async function generateContent() {
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: "Your prompt here",
  });
  
  console.log(response.text);
}
```

## Key API Features

### Native Image Generation
- Generate and edit highly contextual images natively with Gemini 2.0 Flash
- Integrated multimodal capabilities

### Long Context Support
- Input millions of tokens to Gemini models
- Derive understanding from unstructured images, videos, and documents

### Structured Outputs
- Constrain Gemini to respond with JSON
- Structured data format suitable for automated processing

## Best Practices for JavaScript/TypeScript

1. **Error Handling**: Always wrap API calls in try-catch blocks
2. **API Key Management**: Use environment variables for API keys
3. **Rate Limiting**: Implement proper rate limiting for production applications
4. **Model Selection**: Choose the appropriate model based on your use case:
   - Use Flash-Lite for high-frequency, simple tasks
   - Use Flash for balanced performance
   - Use Pro for complex reasoning tasks

## Environment Setup
```javascript
// Recommended environment variable setup
const apiKey = process.env.GEMINI_API_KEY;
const ai = new GoogleGenAI({ apiKey });
```

## Common Patterns

### Text Generation
```javascript
const response = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: "Generate a summary of...",
});
```

### Multimodal Input
```javascript
const response = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: [
    { text: "Describe this image:" },
    { image: imageData }
  ],
});
```

### Structured Output
```javascript
const response = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: "Extract key information as JSON",
  generationConfig: {
    responseFormat: { type: "json" }
  }
});
```

## Documentation Resources
- Main API Documentation: https://ai.google.dev/gemini-api/docs
- JavaScript SDK: @google/genai package
- Google AI Studio: Interactive testing environment