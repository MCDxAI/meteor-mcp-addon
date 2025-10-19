# Gemini API Prompting Strategies Guide

## Overview
Prompt design is the process of creating prompts that elicit accurate, high-quality responses from Gemini models. This guide covers essential strategies and best practices for JavaScript/TypeScript developers.

## Clear and Specific Instructions

### Input Types

#### Question Input
```javascript
const response = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: "What's a good name for a flower shop that specializes in selling bouquets of dried flowers? Create a list of 5 options with just the names.",
});
```

#### Task Input
```javascript
const response = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: "Give me a simple list of just the things that I must bring on a camping trip. The list should have 5 items.",
});
```

#### Entity Input
```javascript
const response = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: "Classify the following items as [large, small]: Elephant Mouse Snail",
});
```

### Partial Input Completion
Use completion strategy for structured outputs:

```javascript
const prompt = `Valid fields are cheeseburger, hamburger, fries, and drink.

Order: Give me a cheeseburger and fries
Output: { "cheeseburger": 1, "fries": 1 }

Order: I want two burgers, a drink, and fries.
Output:`;

const response = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: prompt,
});
```

## Zero-shot vs Few-shot Prompts

### Zero-shot Prompting
```javascript
const response = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: "Please choose the best explanation to the question: Question: How is snow formed? Explanation1: Snow is formed when water vapor in the air freezes into ice crystals in the atmosphere... Explanation2: Water vapor freezes into ice crystals forming snow. Answer:",
});
```

### Few-shot Prompting (Recommended)
```javascript
const fewShotPrompt = `Below are some examples showing a question, explanation, and answer format:

Question: Why is the sky blue?
Explanation1: The sky appears blue because of Rayleigh scattering...
Explanation2: Due to Rayleigh scattering effect.
Answer: Explanation2

Question: What is the cause of earthquakes?
Explanation1: Sudden release of energy in the Earth's crust.
Explanation2: Earthquakes happen when tectonic plates suddenly slip...
Answer: Explanation1

Now, Answer the following question given the example formats above:
Question: How is snow formed?
Explanation1: Snow is formed when water vapor in the air freezes...
Explanation2: Water vapor freezes into ice crystals forming snow.
Answer:`;

const response = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: fewShotPrompt,
});
```

### Few-shot Best Practices
1. **Always include examples** - More effective than zero-shot
2. **Optimal number**: 2-5 examples typically work best
3. **Consistent formatting**: Maintain same structure across examples
4. **Positive patterns**: Show what to do, not what to avoid
5. **Varied examples**: Use diverse, realistic examples

## Response Format Control

### Specify Output Format
```javascript
const response = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: "Summarize this text in one sentence: [long text here]",
});
```

### Completion Strategy for Formatting
```javascript
const prompt = `Create an outline for an essay about hummingbirds.

I. Introduction
*`;

const response = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: prompt,
});
```

## Adding Context

### Contextual Information
```javascript
const contextualPrompt = `Answer the question using the text below. Respond with only the text provided.

Question: What should I do to fix my disconnected wifi? The light on my Google Wifi router is yellow and blinking slowly.

Text: Color: Slowly pulsing yellow
What it means: There is a network error.
What to do: Check that the Ethernet cable is connected to both your router and your modem and both devices are turned on. You might need to unplug and plug in each device again.`;

const response = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: contextualPrompt,
});
```

## Using Prefixes

### Input and Output Prefixes
```javascript
const prefixPrompt = `Classify the text as one of the following categories.
- large
- small

Text: Rhino
The answer is: large

Text: Mouse  
The answer is: small

Text: Elephant
The answer is:`;

const response = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: prefixPrompt,
});
```

## Breaking Down Complex Prompts

### Prompt Chaining Strategy
```javascript
// Step 1: Extract information
const step1Response = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: "Extract the key facts from this document: [document text]",
});

// Step 2: Analyze extracted information  
const step2Response = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: `Based on these facts: ${step1Response.text}, provide recommendations for...`,
});
```

### Component Breakdown
1. **Break down instructions**: One prompt per instruction
2. **Chain prompts**: Sequential steps where output becomes input
3. **Aggregate responses**: Parallel tasks on different data portions

## Model Parameters for Prompting

### Temperature Control
```javascript
const config = {
  temperature: 0.0,  // Deterministic responses
  // temperature: 0.7,  // Balanced creativity
  // temperature: 1.0,  // Maximum creativity
};

const response = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: "Write a creative story",
  config,
});
```

### Advanced Parameters
```javascript
const config = {
  temperature: 0.8,
  topK: 40,           // Consider top 40 tokens
  topP: 0.95,         // Nucleus sampling threshold
  maxOutputTokens: 1000,
  stopSequences: ["END", "STOP"],
};
```

## Prompt Iteration Strategies

### Different Phrasing
```javascript
// Version 1
"How do I bake a pie?"

// Version 2  
"Suggest a recipe for a pie."

// Version 3
"What's a good pie recipe?"
```

### Analogous Tasks
```javascript
// Instead of: "Categorize this book"
// Try: "Multiple choice problem: Which of the following options describes the book The Odyssey? Options: * thriller * sci-fi * mythology * biography"
```

### Content Order Variations
```javascript
// Version 1: [examples] [context] [input]
// Version 2: [input] [examples] [context]  
// Version 3: [examples] [input] [context]
```

## Common Patterns

### Constraints and Guidelines
```javascript
const response = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: "Summarize this text in exactly 3 sentences: [text]",
  config: {
    systemInstruction: "Always follow the exact sentence count specified.",
  }
});
```

### Structured JSON Output
```javascript
const response = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: "Extract person information as JSON with fields: name, age, occupation",
  config: {
    responseMimeType: "application/json",
  }
});
```

## Troubleshooting Prompts

### Fallback Responses
If you get safety filter responses:
- Try increasing temperature
- Rephrase the prompt
- Add more context
- Break down complex requests

### Common Issues
1. **Too generic responses**: Add more specific instructions
2. **Wrong format**: Use few-shot examples showing desired format
3. **Inconsistent results**: Lower temperature, add constraints
4. **Safety blocks**: Rephrase content, add context

## Best Practices Summary

1. **Always use few-shot examples** when possible
2. **Be specific** about desired output format
3. **Provide context** rather than assuming model knowledge
4. **Use consistent formatting** in examples
5. **Break down complex tasks** into simpler components
6. **Experiment with parameters** to optimize results
7. **Iterate and refine** prompts based on outputs
8. **Test different phrasings** for better results