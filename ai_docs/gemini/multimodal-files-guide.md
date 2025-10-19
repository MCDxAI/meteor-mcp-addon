# Gemini API Multimodal Files Guide

## Overview
The Gemini API supports multimodal inputs including text, images, audio, video, and documents. This guide covers file handling, upload strategies, and multimodal prompting best practices for JavaScript/TypeScript developers.

## Files API Basics

### File Upload
```javascript
import { GoogleGenAI, createUserContent, createPartFromUri } from "@google/genai";

const ai = new GoogleGenAI({});

// Upload a file
const myfile = await ai.files.upload({
  file: "path/to/sample.mp3",
  config: { mimeType: "audio/mpeg" },
});

// Use in generation
const response = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: createUserContent([
    createPartFromUri(myfile.uri, myfile.mimeType),
    "Describe this audio clip",
  ]),
});
```

### File Management
```javascript
// Get file metadata
const fileName = myfile.name;
const fetchedFile = await ai.files.get({ name: fileName });

// List all files
const listResponse = await ai.files.list({ config: { pageSize: 10 } });
for await (const file of listResponse) {
  console.log(file.name);
}

// Delete a file
await ai.files.delete({ name: fileName });
```

### File Limitations
- **Storage**: Up to 20 GB per project
- **File size**: Maximum 2 GB per file
- **Retention**: Files stored for 48 hours
- **Request size**: Use Files API when total request > 20 MB
- **Cost**: Files API available at no cost

## Multimodal Prompting Strategies

### Prompt Design Fundamentals

#### Be Specific in Instructions
```javascript
// Generic prompt
const response1 = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: [imageFile, "Describe this image."],
});

// Specific prompt (better)
const response2 = await ai.models.generateContent({
  model: "gemini-2.5-flash", 
  contents: [imageFile, "Parse the time and city from the airport board shown in this image into a list."],
});
```

#### Add Few-shot Examples
```javascript
const fewShotPrompt = `Determine the city along with the landmark.

Example 1: [Rome image]
city: Rome, landmark: the Colosseum

Example 2: [Beijing image] 
city: Beijing, landmark: Forbidden City

Now analyze this image:`;

const response = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: [imageFile, fewShotPrompt],
});
```

#### Break Down Step-by-Step
```javascript
const stepByStepPrompt = `1. First, count how many toilet paper rolls are in this picture.
2. Then, determine how much toilet paper a typical person uses per day.
3. Calculate how long these rolls of toilet paper will last.`;

const response = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: [imageFile, stepByStepPrompt],
});
```

#### Specify Output Format
```javascript
// Markdown table output
const response = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: [imageFile, "Parse the table in this image into markdown format"],
});

// JSON output
const response2 = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: [imageFile, "Provide a list of all the following attributes: ingredients, type of cuisine, vegetarian or not, in JSON format"],
  config: { responseMimeType: "application/json" },
});
```

### Image Placement Best Practices

#### Put Image First for Single-Image Prompts
```javascript
// Recommended approach
const response = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: [
    imageFile,  // Image first
    "Tell me about this instrument"
  ],
});
```

## Troubleshooting Multimodal Prompts

### Model Not Drawing from Relevant Image Parts
```javascript
// Problem: Generic response
const generic = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: [imageFile, "How many days will these diapers last a baby?"],
});

// Solution: Add specific guidance
const specific = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: [imageFile, `How long will these diapers last before I run out?

Use the weight shown on the box to determine the child's age, and use the total number of diapers in the box.
Divide the total number by how many diapers the child goes through per day.`],
});
```

### Output Too Generic
```javascript
// Problem: Generic analysis
const generic = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: [imageFile, "What is in common between these images?"],
});

// Solution: Ask for description first
const detailed = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: [imageFile, "First, describe what's in each image in detail. What's in common between these images?"],
});

// Alternative: Ask to refer to image content
const referenced = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: [imageFile, "What is in common between these images? Refer to what's in the images in your response."],
});
```

### Troubleshooting Failed Prompts
```javascript
// Debug by asking for image description
const debug = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: [imageFile, "Describe what's in this image."],
});

// Ask for reasoning explanation
const reasoning = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: [imageFile, "What's a snack I can make in 1 minute that would go well with this? Please explain why."],
});
```

### Handling Hallucinated Content
```javascript
const response = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: [imageFile, "Describe this image briefly"],
  config: {
    temperature: 0.1,  // Lower temperature reduces hallucination
    maxOutputTokens: 100,  // Shorter responses reduce extrapolation
  },
});
```

## Common Multimodal Patterns

### Document Analysis
```javascript
const documentAnalysis = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: [
    documentFile,
    "Extract the key information from this document and format as JSON with fields: title, date, summary, action_items"
  ],
  config: { responseMimeType: "application/json" },
});
```

### Audio Transcription and Analysis
```javascript
const audioAnalysis = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: [
    audioFile,
    "Transcribe this audio and then provide a summary of the main topics discussed"
  ],
});
```

### Video Understanding
```javascript
const videoAnalysis = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: [
    videoFile,
    "Describe what happens in this video, including any text that appears on screen"
  ],
});
```

### Image + Text Combination
```javascript
const combinedAnalysis = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: [
    "Based on the image and the following context:",
    "Context: This is a product photo for our e-commerce site.",
    imageFile,
    "Write a compelling product description that highlights the key features visible in the image."
  ],
});
```

## Advanced Multimodal Techniques

### Multi-Image Analysis
```javascript
const multiImageAnalysis = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: [
    "Compare these images and identify similarities and differences:",
    image1File,
    image2File,
    image3File,
    "Provide your analysis in a structured format."
  ],
});
```

### Creative Applications
```javascript
// Image-based creative writing
const creativeWriting = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: [
    imageFile,
    "Can you write me a descriptive and dramatic poem about this image and include the location?"
  ],
  config: { temperature: 0.8 },
});

// Visual storytelling
const storytelling = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: [
    imageFile,
    "Create a short story inspired by this image. Include dialogue and describe the setting in detail."
  ],
});
```

### Data Extraction
```javascript
// Table extraction
const tableExtraction = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: [
    imageFile,
    "Extract all data from this table and convert it to CSV format"
  ],
});

// Form processing
const formProcessing = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: [
    documentFile,
    "Extract all form fields and their values from this document as JSON"
  ],
  config: { responseMimeType: "application/json" },
});
```

## Error Handling and Best Practices

### File Upload Error Handling
```javascript
try {
  const file = await ai.files.upload({
    file: "path/to/large-file.mp4",
    config: { mimeType: "video/mp4" },
  });
  
  // Wait for processing if needed
  let fileStatus = await ai.files.get({ name: file.name });
  while (fileStatus.state === "PROCESSING") {
    await new Promise(resolve => setTimeout(resolve, 1000));
    fileStatus = await ai.files.get({ name: file.name });
  }
  
  if (fileStatus.state === "ACTIVE") {
    // File ready for use
    const response = await ai.models.generateContent({
      model: "gemini-2.5-flash",
      contents: [createPartFromUri(file.uri, file.mimeType), "Analyze this video"],
    });
  }
} catch (error) {
  console.error("File upload failed:", error);
}
```

### Multimodal Best Practices
1. **File size optimization**: Compress large files when possible
2. **Appropriate model selection**: Use models that support your media type
3. **Clear instructions**: Be specific about what you want extracted/analyzed
4. **Format specification**: Always specify desired output format
5. **Error handling**: Handle file processing states and API errors
6. **Cost management**: Monitor file storage and API usage
7. **Security**: Validate file types and content before upload