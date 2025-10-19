# Gemini API Text Generation Guide

## Overview
This guide covers text generation capabilities of the Gemini API, including basic text generation, system instructions, configuration options, and advanced features for JavaScript/TypeScript developers.

## Basic Text Generation

### Simple Text Generation
```javascript
import { GoogleGenAI } from "@google/genai";

const ai = new GoogleGenAI({});

async function generateText() {
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: "How does AI work?",
  });
  
  console.log(response.text);
}
```

### Key Models for Text Generation
- **gemini-2.5-flash**: Balanced performance and speed
- **gemini-2.5-pro**: Most powerful for complex reasoning
- **gemini-2.5-flash-lite**: Fastest and most cost-efficient

## Thinking Configuration (Gemini 2.5)

### Default Behavior
- 2.5 Flash and Pro models have "thinking" enabled by default
- Enhances quality but may increase processing time and token usage

### Disabling Thinking
```javascript
const response = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: "How does AI work?",
  config: {
    thinkingConfig: {
      thinkingBudget: 0, // Disables thinking
    },
  }
});
```
## 
System Instructions

### Basic System Instructions
```javascript
const response = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: "Hello there",
  config: {
    systemInstruction: "You are a cat. Your name is Neko.",
  },
});
```

### Best Practices for System Instructions
- Be specific and clear about the desired behavior
- Define the role, tone, and response style
- Include constraints and guidelines
- Specify output format requirements

## Generation Configuration

### Temperature Control
```javascript
const response = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: "Explain how AI works",
  config: {
    temperature: 0.1, // Lower = more deterministic
  },
});
```

### Advanced Configuration Options
```javascript
const config = {
  temperature: 0.9,        // Creativity level (0.0-1.0)
  topP: 0.8,              // Nucleus sampling
  topK: 40,               // Top-k sampling
  maxOutputTokens: 1000,   // Response length limit
  stopSequences: ["END"],  // Stop generation triggers
  responseMimeType: "application/json" // Output format
};
```

## Multimodal Text Generation

### Text + Image Input
```javascript
import { createUserContent, createPartFromUri } from "@google/genai";

const image = await ai.files.upload({
  file: "/path/to/image.png",
});

const response = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: createUserContent([
    "Tell me about this instrument",
    createPartFromUri(image.uri, image.mimeType),
  ]),
});
```

## Streaming Responses

### Basic Streaming
```javascript
const response = await ai.models.generateContentStream({
  model: "gemini-2.5-flash",
  contents: "Explain how AI works",
});

for await (const chunk of response) {
  console.log(chunk.text);
}
```

### Benefits of Streaming
- More fluid user interactions
- Faster perceived response times
- Better for long-form content generation
- Allows for real-time processing

## Multi-turn Conversations (Chat)

### Creating a Chat Session
```javascript
const chat = ai.chats.create({
  model: "gemini-2.5-flash",
  history: [
    {
      role: "user",
      parts: [{ text: "Hello" }],
    },
    {
      role: "model", 
      parts: [{ text: "Great to meet you. What would you like to know?" }],
    },
  ],
});
```

### Sending Messages
```javascript
const response1 = await chat.sendMessage({
  message: "I have 2 dogs in my house.",
});

const response2 = await chat.sendMessage({
  message: "How many paws are in my house?",
});
```

### Streaming Chat
```javascript
const stream = await chat.sendMessageStream({
  message: "Tell me a story",
});

for await (const chunk of stream) {
  console.log(chunk.text);
}
```