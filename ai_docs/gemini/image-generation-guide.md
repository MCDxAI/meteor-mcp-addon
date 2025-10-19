# Gemini API Image Generation Guide

## Overview
Gemini can generate and process images conversationally using both native image generation capabilities and the specialized Imagen model. This guide covers image generation, editing, and best practices for JavaScript/TypeScript developers.

## Native Image Generation with Gemini

### Text-to-Image Generation
```javascript
import { GoogleGenAI, Modality } from "@google/genai";
import * as fs from "node:fs";

const ai = new GoogleGenAI({});

async function generateImage() {
  const contents = "Hi, can you create a 3d rendered image of a pig " +
    "with wings and a top hat flying over a happy " +
    "futuristic scifi city with lots of greenery?";

  // Must include responseModalities for image generation
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash-image-preview",
    contents: contents,
    config: {
      responseModalities: [Modality.TEXT, Modality.IMAGE],
    },
  });

  // Process response parts
  for (const part of response.candidates[0].content.parts) {
    if (part.text) {
      console.log(part.text);
    } else if (part.inlineData) {
      const imageData = part.inlineData.data;
      const buffer = Buffer.from(imageData, "base64");
      fs.writeFileSync("gemini-native-image.png", buffer);
      console.log("Image saved as gemini-native-image.png");
    }
  }
}
```

### Key Requirements
- **Model**: Use `gemini-2.5-flash-image-preview`
- **Response Modalities**: Must include `[Modality.TEXT, Modality.IMAGE]`
- **Output**: Image-only output is not supported; always includes text + image
- **Watermark**: All generated images include SynthID watermark

## Image Editing (Text-and-Image-to-Image)

### Basic Image Editing
```javascript
async function editImage() {
  // Load existing image
  const imagePath = "path/to/image.png";
  const imageData = fs.readFileSync(imagePath);
  const base64Image = imageData.toString("base64");

  const contents = [
    { text: "Can you add a llama next to the image?" },
    {
      inlineData: {
        mimeType: "image/png",
        data: base64Image,
      },
    },
  ];

  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash-image-preview",
    contents: contents,
    config: {
      responseModalities: [Modality.TEXT, Modality.IMAGE],
    },
  });

  // Save edited image
  for (const part of response.candidates[0].content.parts) {
    if (part.inlineData) {
      const buffer = Buffer.from(part.inlineData.data, "base64");
      fs.writeFileSync("edited-image.png", buffer);
    }
  }
}
```

## Advanced Image Generation Modes

### Text to Image(s) and Text (Interleaved)
```javascript
const response = await ai.models.generateContent({
  model: "gemini-2.5-flash-image-preview",
  contents: "Generate an illustrated recipe for a paella.",
  config: {
    responseModalities: [Modality.TEXT, Modality.IMAGE],
  },
});
```

### Multi-turn Image Editing (Chat)
```javascript
const chat = ai.chats.create({
  model: "gemini-2.5-flash-image-preview",
});

// Upload initial image
const initialImage = await ai.files.upload({
  file: "path/to/blue-car.jpg",
});

// First edit
const response1 = await chat.sendMessage({
  message: [
    createPartFromUri(initialImage.uri, initialImage.mimeType),
    "Turn this car into a convertible."
  ],
  config: {
    responseModalities: [Modality.TEXT, Modality.IMAGE],
  },
});

// Second edit
const response2 = await chat.sendMessage({
  message: "Now change the color to yellow.",
  config: {
    responseModalities: [Modality.TEXT, Modality.IMAGE],
  },
});
```

### Image Analysis with Generation
```javascript
// Analyze existing room and generate variations
const roomAnalysis = await ai.models.generateContent({
  model: "gemini-2.5-flash-image-preview",
  contents: [
    roomImage,
    "What other color sofas would work in my space? Can you update the image?"
  ],
  config: {
    responseModalities: [Modality.TEXT, Modality.IMAGE],
  },
});
```

## When to Use Gemini vs Imagen

### Choose Gemini When:
- Need contextually relevant images leveraging world knowledge
- Seamlessly blending text and images is important
- Want accurate visuals embedded within long text sequences
- Want to edit images conversationally while maintaining context
- Building interactive, multi-turn image generation experiences

### Choose Imagen When:
- Image quality, photorealism, or artistic detail are top priorities
- Need specific styles (impressionism, anime, etc.)
- Performing specialized editing (product backgrounds, upscaling)
- Creating branding, logos, or product designs
- Need the highest quality image generation available

### Imagen Models
- **Imagen 4**: Go-to model for most image generation tasks
- **Imagen 4 Ultra**: Advanced use-cases requiring best quality (one image at a time)

## Image Generation Best Practices

### Effective Prompting
```javascript
// Detailed, specific prompts work better
const detailedPrompt = `Create a photorealistic image of a modern kitchen with:
- White marble countertops
- Stainless steel appliances
- Natural wood cabinets
- Large windows with natural light
- Minimalist design aesthetic
- Plants on the windowsill`;

const response = await ai.models.generateContent({
  model: "gemini-2.5-flash-image-preview",
  contents: detailedPrompt,
  config: {
    responseModalities: [Modality.TEXT, Modality.IMAGE],
  },
});
```

### Iterative Refinement
```javascript
// Start with basic prompt
let prompt = "A cozy coffee shop interior";

// Refine based on results
prompt = "A cozy coffee shop interior with warm lighting, wooden furniture, and books on shelves";

// Further refinement
prompt = "A cozy coffee shop interior with warm Edison bulb lighting, reclaimed wooden furniture, vintage books on floating shelves, and customers reading";
```

### Style and Mood Control
```javascript
// Artistic styles
const artisticPrompt = "A landscape painting in the style of Van Gogh, with swirling clouds and vibrant colors";

// Mood and atmosphere
const moodPrompt = "A mysterious forest path at twilight, with soft purple light filtering through ancient trees";

// Technical specifications
const technicalPrompt = "A product photo of a smartphone, studio lighting, white background, professional photography, high resolution";
```

## Limitations and Considerations

### Current Limitations
- **Language support**: Best performance with EN, es-MX, ja-JP, zh-CN, hi-IN
- **Input restrictions**: No audio or video inputs for image generation
- **Generation reliability**: May not always trigger image generation
- **Regional availability**: Not available in all regions/countries

### Troubleshooting Generation Issues

#### When Image Generation Doesn't Trigger
```javascript
// Explicitly request image generation
const explicitPrompt = "Generate an image of a sunset over mountains. Please provide images as you go along.";

// Alternative approach
const alternativePrompt = "Create a visual representation of a peaceful garden scene. Update the image to show different seasons.";
```

#### When Generation Stops Partway
```javascript
// Try different phrasing
const rephrased = "Illustrate a bustling marketplace scene with vendors and customers";

// Break into steps
const stepped = "First, generate an image of a marketplace. Then, add vendors selling fruits and vegetables.";
```

### Performance Optimization

#### Text-First Approach
```javascript
// Generate text description first, then image
const textFirst = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: "Describe a futuristic cityscape in detail",
});

// Use description for image generation
const imageGeneration = await ai.models.generateContent({
  model: "gemini-2.5-flash-image-preview",
  contents: `Generate an image based on this description: ${textFirst.text}`,
  config: {
    responseModalities: [Modality.TEXT, Modality.IMAGE],
  },
});
```

## Error Handling and Safety

### Robust Error Handling
```javascript
async function safeImageGeneration(prompt) {
  try {
    const response = await ai.models.generateContent({
      model: "gemini-2.5-flash-image-preview",
      contents: prompt,
      config: {
        responseModalities: [Modality.TEXT, Modality.IMAGE],
      },
    });

    // Check if image was actually generated
    const hasImage = response.candidates[0].content.parts.some(
      part => part.inlineData
    );

    if (!hasImage) {
      console.log("No image generated, trying alternative prompt...");
      return await safeImageGeneration(`Generate an image: ${prompt}`);
    }

    return response;
  } catch (error) {
    console.error("Image generation failed:", error);
    
    if (error.message.includes("safety")) {
      console.log("Content filtered for safety, try rephrasing...");
    } else if (error.message.includes("quota")) {
      console.log("Rate limit reached, implementing backoff...");
    }
    
    throw error;
  }
}
```

### Content Safety
```javascript
// Avoid potentially problematic content
const safePrompt = "A family-friendly illustration of children playing in a park";

// Be specific to avoid misinterpretation
const specificPrompt = "A cartoon-style drawing of a red apple on a wooden table, suitable for educational materials";
```

## Integration Patterns

### Batch Image Generation
```javascript
async function generateImageBatch(prompts) {
  const results = [];
  
  for (const prompt of prompts) {
    try {
      const response = await ai.models.generateContent({
        model: "gemini-2.5-flash-image-preview",
        contents: prompt,
        config: {
          responseModalities: [Modality.TEXT, Modality.IMAGE],
        },
      });
      
      results.push({ prompt, response, success: true });
      
      // Rate limiting delay
      await new Promise(resolve => setTimeout(resolve, 1000));
    } catch (error) {
      results.push({ prompt, error, success: false });
    }
  }
  
  return results;
}
```

### Image Variation Generation
```javascript
async function generateVariations(basePrompt, variations) {
  const results = [];
  
  for (const variation of variations) {
    const fullPrompt = `${basePrompt}, ${variation}`;
    
    const response = await ai.models.generateContent({
      model: "gemini-2.5-flash-image-preview",
      contents: fullPrompt,
      config: {
        responseModalities: [Modality.TEXT, Modality.IMAGE],
      },
    });
    
    results.push({ variation, response });
  }
  
  return results;
}

// Usage
const variations = await generateVariations(
  "A modern living room",
  ["with blue accents", "with plants", "minimalist style", "cozy atmosphere"]
);
```