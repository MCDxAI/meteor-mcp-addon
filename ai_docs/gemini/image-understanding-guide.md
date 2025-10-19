# Gemini API Image Understanding Guide

## Overview
Gemini models are built to be multimodal from the ground up, enabling comprehensive image processing and computer vision tasks including image captioning, classification, visual question answering, object detection, and segmentation without requiring specialized ML models.

## Key Capabilities
- **Image captioning and description**: Generate detailed descriptions of image content
- **Visual question answering**: Answer specific questions about images
- **Image classification**: Categorize and classify image content
- **Object detection**: Identify and locate objects with bounding boxes (Gemini 2.0+)
- **Image segmentation**: Generate detailed segmentation masks (Gemini 2.5+)
- **Multi-image analysis**: Compare and analyze multiple images simultaneously

## Basic Setup

### JavaScript/TypeScript Implementation
```javascript
import { GoogleGenAI } from "@google/genai";
import * as fs from "node:fs";

const ai = new GoogleGenAI({
  apiKey: process.env.GEMINI_API_KEY
});

// Basic image analysis
async function analyzeImage(imagePath, prompt) {
  const base64Image = fs.readFileSync(imagePath, { encoding: "base64" });
  
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: [
      {
        inlineData: {
          mimeType: "image/jpeg",
          data: base64Image,
        },
      },
      { text: prompt }
    ],
  });

  return response.text;
}

// Usage
const description = await analyzeImage("./photo.jpg", "Caption this image.");
```

## Image Input Methods

### Inline Image Data
```javascript
async function processInlineImage(imagePath, prompt) {
  const base64Image = fs.readFileSync(imagePath, { encoding: "base64" });
  
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: [
      {
        inlineData: {
          mimeType: "image/jpeg",
          data: base64Image,
        },
      },
      { text: prompt }
    ],
  });

  return response.text;
}

// Usage
const analysis = await processInlineImage("./product.png", "Describe the features of this product");
```

### Image from URL
```javascript
async function processImageFromURL(imageUrl, prompt) {
  const response = await fetch(imageUrl);
  const imageArrayBuffer = await response.arrayBuffer();
  const base64ImageData = Buffer.from(imageArrayBuffer).toString('base64');

  const result = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: [
      {
        inlineData: {
          mimeType: 'image/jpeg',
          data: base64ImageData,
        },
      },
      { text: prompt }
    ],
  });

  return result.text;
}

// Usage
const caption = await processImageFromURL(
  "https://example.com/image.jpg",
  "What is happening in this image?"
);
```

### File API for Large Images
```javascript
import { createPartFromUri } from "@google/genai";

async function processLargeImage(imagePath, prompt) {
  // Upload the image file
  const file = await ai.files.upload({
    file: imagePath,
    config: { mimeType: "image/jpeg" }
  });

  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: [
      createPartFromUri(file.uri, file.mimeType),
      { text: prompt }
    ]
  });

  return response.text;
}

// Usage
const analysis = await processLargeImage("./high-res-image.jpg", "Analyze this image in detail");
```

## Multi-Image Analysis

### Compare Multiple Images
```javascript
async function compareImages(imagePaths, comparisonPrompt) {
  const contents = [{ text: comparisonPrompt }];
  
  // Add all images to the content
  for (const imagePath of imagePaths) {
    const base64Image = fs.readFileSync(imagePath, { encoding: "base64" });
    contents.push({
      inlineData: {
        mimeType: "image/jpeg",
        data: base64Image,
      },
    });
  }

  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: contents,
  });

  return response.text;
}

// Usage
const comparison = await compareImages([
  "./before.jpg",
  "./after.jpg"
], "What are the differences between these two images?");
```

### Mixed File API and Inline Images
```javascript
async function analyzeMixedImages(images, prompt) {
  const contents = [{ text: prompt }];
  
  for (const image of images) {
    if (image.type === 'file') {
      // Use File API
      const uploadedFile = await ai.files.upload({
        file: image.path,
        config: { mimeType: image.mimeType }
      });
      contents.push(createPartFromUri(uploadedFile.uri, uploadedFile.mimeType));
    } else if (image.type === 'inline') {
      // Use inline data
      const base64Image = fs.readFileSync(image.path, { encoding: "base64" });
      contents.push({
        inlineData: {
          mimeType: image.mimeType,
          data: base64Image,
        },
      });
    }
  }

  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: contents,
  });

  return response.text;
}

// Usage
const analysis = await analyzeMixedImages([
  { type: 'file', path: './large-image.jpg', mimeType: 'image/jpeg' },
  { type: 'inline', path: './small-image.png', mimeType: 'image/png' }
], "Compare the content and quality of these images");
```

## Object Detection (Gemini 2.0+)

### Basic Object Detection
```javascript
async function detectObjects(imagePath) {
  const base64Image = fs.readFileSync(imagePath, { encoding: "base64" });
  
  const prompt = "Detect all prominent items in the image. The box_2d should be [ymin, xmin, ymax, xmax] normalized to 0-1000.";
  
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: [
      {
        inlineData: {
          mimeType: "image/jpeg",
          data: base64Image,
        },
      },
      { text: prompt }
    ],
    config: {
      responseMimeType: "application/json"
    }
  });

  return JSON.parse(response.text);
}

// Convert normalized coordinates to absolute coordinates
function convertBoundingBoxes(detections, imageWidth, imageHeight) {
  return detections.map(detection => {
    const [ymin, xmin, ymax, xmax] = detection.box_2d;
    
    return {
      ...detection,
      absoluteBox: {
        x1: Math.round(xmin / 1000 * imageWidth),
        y1: Math.round(ymin / 1000 * imageHeight),
        x2: Math.round(xmax / 1000 * imageWidth),
        y2: Math.round(ymax / 1000 * imageHeight)
      }
    };
  });
}

// Usage
const detections = await detectObjects("./scene.jpg");
const convertedDetections = convertBoundingBoxes(detections, 1920, 1080);
```

### Custom Object Detection
```javascript
async function detectSpecificObjects(imagePath, objectTypes) {
  const base64Image = fs.readFileSync(imagePath, { encoding: "base64" });
  
  const prompt = `Detect ${objectTypes.join(', ')} in this image. Provide bounding boxes in [ymin, xmin, ymax, xmax] format normalized to 0-1000.`;
  
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: [
      {
        inlineData: {
          mimeType: "image/jpeg",
          data: base64Image,
        },
      },
      { text: prompt }
    ],
    config: {
      responseMimeType: "application/json"
    }
  });

  return JSON.parse(response.text);
}

// Usage
const carDetections = await detectSpecificObjects("./street.jpg", ["cars", "trucks", "motorcycles"]);
```

### Object Detection with Labels
```javascript
async function detectObjectsWithLabels(imagePath, labelType = 'general') {
  const base64Image = fs.readFileSync(imagePath, { encoding: "base64" });
  
  const labelPrompts = {
    general: "Detect and label all objects in this image",
    safety: "Detect safety hazards and label them with risk levels",
    food: "Detect food items and label them with nutritional categories",
    medical: "Detect medical equipment and label them with their functions"
  };
  
  const prompt = `${labelPrompts[labelType]}. Provide bounding boxes and descriptive labels.`;
  
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: [
      {
        inlineData: {
          mimeType: "image/jpeg",
          data: base64Image,
        },
      },
      { text: prompt }
    ],
    config: {
      responseMimeType: "application/json"
    }
  });

  return JSON.parse(response.text);
}
```

## Image Segmentation (Gemini 2.5+)

### Basic Segmentation
```javascript
import { createCanvas, loadImage } from 'canvas';

async function segmentObjects(imagePath, objectTypes) {
  const base64Image = fs.readFileSync(imagePath, { encoding: "base64" });
  
  const prompt = `Give the segmentation masks for ${objectTypes.join(' and ')} items. Output a JSON list of segmentation masks where each entry contains the 2D bounding box in the key "box_2d", the segmentation mask in key "mask", and the text label in the key "label". Use descriptive labels.`;
  
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: [
      {
        inlineData: {
          mimeType: "image/jpeg",
          data: base64Image,
        },
      },
      { text: prompt }
    ],
    config: {
      thinkingConfig: { thinkingBudget: 0 } // Disable thinking for better results
    }
  });

  return parseSegmentationResponse(response.text);
}

function parseSegmentationResponse(responseText) {
  // Parse JSON from markdown fencing if present
  const lines = responseText.split('\n');
  let jsonStart = -1;
  
  for (let i = 0; i < lines.length; i++) {
    if (lines[i].trim() === '```json') {
      jsonStart = i + 1;
      break;
    }
  }
  
  if (jsonStart !== -1) {
    const jsonLines = [];
    for (let i = jsonStart; i < lines.length; i++) {
      if (lines[i].trim() === '```') break;
      jsonLines.push(lines[i]);
    }
    responseText = jsonLines.join('\n');
  }
  
  return JSON.parse(responseText);
}

// Process segmentation masks
async function processSegmentationMasks(imagePath, segmentations, outputDir = './segmentation_output') {
  const image = await loadImage(imagePath);
  const results = [];
  
  if (!fs.existsSync(outputDir)) {
    fs.mkdirSync(outputDir, { recursive: true });
  }
  
  for (let i = 0; i < segmentations.length; i++) {
    const segmentation = segmentations[i];
    const box = segmentation.box_2d;
    
    // Convert normalized coordinates to absolute
    const y0 = Math.round(box[0] / 1000 * image.height);
    const x0 = Math.round(box[1] / 1000 * image.width);
    const y1 = Math.round(box[2] / 1000 * image.height);
    const x1 = Math.round(box[3] / 1000 * image.width);
    
    // Skip invalid boxes
    if (y0 >= y1 || x0 >= x1) continue;
    
    // Process mask
    const maskData = segmentation.mask;
    if (!maskData.startsWith("data:image/png;base64,")) continue;
    
    const base64Data = maskData.replace("data:image/png;base64,", "");
    const maskBuffer = Buffer.from(base64Data, 'base64');
    
    // Save mask
    const maskFilename = `${segmentation.label}_${i}_mask.png`;
    fs.writeFileSync(`${outputDir}/${maskFilename}`, maskBuffer);
    
    results.push({
      label: segmentation.label,
      boundingBox: { x0, y0, x1, y1 },
      maskFile: maskFilename,
      area: (x1 - x0) * (y1 - y0)
    });
  }
  
  return results;
}

// Usage
const segmentations = await segmentObjects("./room.jpg", ["furniture", "decorations"]);
const processedMasks = await processSegmentationMasks("./room.jpg", segmentations);
```

## Image Analysis Use Cases

### Product Analysis
```javascript
async function analyzeProduct(productImagePath) {
  const prompt = `Analyze this product image and provide:
1. Product category and type
2. Key features and characteristics
3. Condition assessment
4. Brand identification (if visible)
5. Estimated price range
6. Target market/demographic
7. Marketing appeal factors

Format as structured JSON.`;

  const base64Image = fs.readFileSync(productImagePath, { encoding: "base64" });
  
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: [
      {
        inlineData: {
          mimeType: "image/jpeg",
          data: base64Image,
        },
      },
      { text: prompt }
    ],
    config: {
      responseMimeType: "application/json"
    }
  });

  return JSON.parse(response.text);
}
```

### Medical Image Analysis
```javascript
async function analyzeMedicalImage(imagePath, imageType = 'general') {
  const prompts = {
    general: "Describe the anatomical structures and any notable features in this medical image",
    xray: "Analyze this X-ray image for bone structure, alignment, and any visible abnormalities",
    scan: "Describe the tissue structures and contrast patterns in this medical scan",
    dermatology: "Analyze this skin image for lesions, discoloration, or other dermatological features"
  };

  const prompt = `${prompts[imageType]}. Note: This is for educational purposes only and should not be used for medical diagnosis.`;
  
  return await analyzeImage(imagePath, prompt);
}
```

### Document and Text Recognition
```javascript
async function extractTextFromImage(imagePath) {
  const prompt = `Extract all text from this image. Preserve the layout and formatting as much as possible. If there are tables, format them appropriately. Include any handwritten text if present.`;
  
  return await analyzeImage(imagePath, prompt);
}

async function analyzeDocument(imagePath) {
  const prompt = `Analyze this document image and extract:
1. Document type and purpose
2. All text content with formatting preserved
3. Key information and data points
4. Tables and structured data
5. Signatures or stamps (if present)
6. Document quality and legibility assessment

Format the response with clear sections.`;

  return await analyzeImage(imagePath, prompt);
}
```

### Scene Understanding
```javascript
async function analyzeScene(imagePath) {
  const prompt = `Analyze this scene and provide:
1. Location type and setting
2. Time of day and weather conditions
3. People present and their activities
4. Objects and their relationships
5. Mood and atmosphere
6. Safety considerations
7. Interesting or unusual elements

Create a comprehensive scene description.`;

  return await analyzeImage(imagePath, prompt);
}
```

### Quality Assessment
```javascript
async function assessImageQuality(imagePath) {
  const prompt = `Assess the technical quality of this image:
1. Resolution and sharpness
2. Lighting conditions
3. Color balance and saturation
4. Composition and framing
5. Noise and artifacts
6. Overall technical quality score (1-10)
7. Suggestions for improvement

Format as structured analysis.`;

  const base64Image = fs.readFileSync(imagePath, { encoding: "base64" });
  
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: [
      {
        inlineData: {
          mimeType: "image/jpeg",
          data: base64Image,
        },
      },
      { text: prompt }
    ],
    config: {
      responseMimeType: "application/json"
    }
  });

  return JSON.parse(response.text);
}
```

## Advanced Image Processing

### Batch Image Analysis
```javascript
class ImageProcessor {
  constructor() {
    this.ai = new GoogleGenAI({});
  }

  async processBatch(imagePaths, analysisPrompt, options = {}) {
    const { 
      maxConcurrent = 3,
      delay = 1000,
      useFileAPI = false 
    } = options;

    const results = [];
    
    for (let i = 0; i < imagePaths.length; i += maxConcurrent) {
      const batch = imagePaths.slice(i, i + maxConcurrent);
      
      const batchPromises = batch.map(async (imagePath, index) => {
        try {
          let result;
          if (useFileAPI) {
            result = await this.processWithFileAPI(imagePath, analysisPrompt);
          } else {
            result = await this.processInline(imagePath, analysisPrompt);
          }

          return {
            imagePath,
            result,
            success: true,
            index: i + index
          };
        } catch (error) {
          return {
            imagePath,
            error: error.message,
            success: false,
            index: i + index
          };
        }
      });

      const batchResults = await Promise.all(batchPromises);
      results.push(...batchResults);

      // Rate limiting delay
      if (i + maxConcurrent < imagePaths.length) {
        await new Promise(resolve => setTimeout(resolve, delay));
      }
    }

    return results;
  }

  async processInline(imagePath, prompt) {
    return await analyzeImage(imagePath, prompt);
  }

  async processWithFileAPI(imagePath, prompt) {
    return await processLargeImage(imagePath, prompt);
  }
}

// Usage
const processor = new ImageProcessor();
const results = await processor.processBatch([
  "./image1.jpg",
  "./image2.png",
  "./image3.jpeg"
], "Describe the main subject and setting of this image");
```

### Image Classification System
```javascript
class ImageClassifier {
  constructor() {
    this.ai = new GoogleGenAI({});
    this.categories = {
      nature: ["landscape", "wildlife", "plants", "weather"],
      urban: ["buildings", "streets", "vehicles", "infrastructure"],
      people: ["portraits", "groups", "activities", "events"],
      objects: ["products", "tools", "furniture", "technology"]
    };
  }

  async classifyImage(imagePath, customCategories = null) {
    const categories = customCategories || Object.keys(this.categories);
    
    const prompt = `Classify this image into one of these categories: ${categories.join(', ')}. 
    Provide:
    1. Primary category
    2. Confidence score (0-1)
    3. Secondary category (if applicable)
    4. Key features that led to this classification
    5. Subcategory within the primary category
    
    Format as JSON.`;

    const base64Image = fs.readFileSync(imagePath, { encoding: "base64" });
    
    const response = await this.ai.models.generateContent({
      model: "gemini-2.5-flash",
      contents: [
        {
          inlineData: {
            mimeType: "image/jpeg",
            data: base64Image,
          },
        },
        { text: prompt }
      ],
      config: {
        responseMimeType: "application/json"
      }
    });

    return JSON.parse(response.text);
  }

  async batchClassify(imagePaths, categories = null) {
    const results = [];
    
    for (const imagePath of imagePaths) {
      try {
        const classification = await this.classifyImage(imagePath, categories);
        results.push({
          imagePath,
          classification,
          success: true
        });
      } catch (error) {
        results.push({
          imagePath,
          error: error.message,
          success: false
        });
      }
    }

    return results;
  }
}
```

### Visual Search and Similarity
```javascript
async function findSimilarImages(queryImagePath, candidateImagePaths) {
  const contents = [
    { text: "Compare the first image with each of the following images. Rate similarity from 0-10 and explain the similarities/differences. Format as JSON array." },
  ];
  
  // Add query image first
  const queryImage = fs.readFileSync(queryImagePath, { encoding: "base64" });
  contents.push({
    inlineData: {
      mimeType: "image/jpeg",
      data: queryImage,
    },
  });
  
  // Add candidate images
  for (const candidatePath of candidateImagePaths) {
    const candidateImage = fs.readFileSync(candidatePath, { encoding: "base64" });
    contents.push({
      inlineData: {
        mimeType: "image/jpeg",
        data: candidateImage,
      },
    });
  }

  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: contents,
    config: {
      responseMimeType: "application/json"
    }
  });

  return JSON.parse(response.text);
}
```

## Error Handling and Best Practices

### Robust Image Processing
```javascript
async function safeImageProcessing(imagePath, prompt, options = {}) {
  const {
    maxRetries = 3,
    fallbackPrompt = "Describe this image",
    validateImage = true
  } = options;

  if (validateImage) {
    const validation = validateImageFile(imagePath);
    if (!validation.valid) {
      throw new Error(`Invalid image: ${validation.error}`);
    }
  }

  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      const fileSize = fs.statSync(imagePath).size;
      
      if (fileSize > 20 * 1024 * 1024) { // 20MB
        return await processLargeImage(imagePath, prompt);
      } else {
        return await analyzeImage(imagePath, prompt);
      }
    } catch (error) {
      console.error(`Image processing attempt ${attempt} failed:`, error.message);
      
      if (attempt === maxRetries) {
        if (fallbackPrompt && fallbackPrompt !== prompt) {
          console.log('Trying with fallback prompt');
          return await analyzeImage(imagePath, fallbackPrompt);
        }
        throw error;
      }
      
      // Wait before retry
      await new Promise(resolve => setTimeout(resolve, 1000 * attempt));
    }
  }
}

function validateImageFile(imagePath) {
  const supportedFormats = ['.jpg', '.jpeg', '.png', '.webp', '.heic', '.heif'];
  const ext = path.extname(imagePath).toLowerCase();
  
  if (!supportedFormats.includes(ext)) {
    return { valid: false, error: `Unsupported format: ${ext}` };
  }

  const stats = fs.statSync(imagePath);
  const maxSize = 50 * 1024 * 1024; // 50MB
  
  if (stats.size > maxSize) {
    return { valid: false, error: `File too large: ${stats.size} bytes` };
  }

  return { valid: true };
}
```

### Performance Optimization
```javascript
class ImageCache {
  constructor(ttl = 1800000) { // 30 minutes default TTL
    this.cache = new Map();
    this.ttl = ttl;
  }

  generateKey(imagePath, prompt) {
    const stats = fs.statSync(imagePath);
    return `${imagePath}:${stats.mtime.getTime()}:${this.hashPrompt(prompt)}`;
  }

  hashPrompt(prompt) {
    let hash = 0;
    for (let i = 0; i < prompt.length; i++) {
      const char = prompt.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash;
    }
    return hash.toString();
  }

  get(key) {
    const item = this.cache.get(key);
    if (item && Date.now() - item.timestamp < this.ttl) {
      return item.value;
    }
    this.cache.delete(key);
    return null;
  }

  set(key, value) {
    this.cache.set(key, {
      value,
      timestamp: Date.now()
    });
  }
}

// Usage with caching
const imageCache = new ImageCache();

async function analyzeImageWithCache(imagePath, prompt) {
  const cacheKey = imageCache.generateKey(imagePath, prompt);
  const cached = imageCache.get(cacheKey);
  
  if (cached) {
    return cached;
  }

  const result = await safeImageProcessing(imagePath, prompt);
  imageCache.set(cacheKey, result);
  
  return result;
}
```

## Technical Details and Limitations

### Supported Image Formats
```javascript
const SUPPORTED_IMAGE_FORMATS = {
  'image/png': ['.png'],
  'image/jpeg': ['.jpg', '.jpeg'],
  'image/webp': ['.webp'],
  'image/heic': ['.heic'],
  'image/heif': ['.heif']
};

function getSupportedMimeType(filePath) {
  const ext = path.extname(filePath).toLowerCase();
  
  for (const [mimeType, extensions] of Object.entries(SUPPORTED_IMAGE_FORMATS)) {
    if (extensions.includes(ext)) {
      return mimeType;
    }
  }
  
  return null;
}
```

### Token Calculation
```javascript
function calculateImageTokens(imageWidth, imageHeight) {
  // Base token cost for small images
  if (imageWidth <= 384 && imageHeight <= 384) {
    return 258;
  }
  
  // For larger images, calculate tiles
  const tileSize = 768;
  const tilesX = Math.ceil(imageWidth / tileSize);
  const tilesY = Math.ceil(imageHeight / tileSize);
  const totalTiles = tilesX * tilesY;
  
  return totalTiles * 258;
}
```

### Model Capabilities by Version
```javascript
const MODEL_CAPABILITIES = {
  'gemini-1.5-flash': {
    imageUnderstanding: true,
    objectDetection: false,
    segmentation: false,
    maxImages: 3600
  },
  'gemini-1.5-pro': {
    imageUnderstanding: true,
    objectDetection: false,
    segmentation: false,
    maxImages: 3600
  },
  'gemini-2.0-flash': {
    imageUnderstanding: true,
    objectDetection: true,
    segmentation: false,
    maxImages: 3600
  },
  'gemini-2.5-flash': {
    imageUnderstanding: true,
    objectDetection: true,
    segmentation: true,
    maxImages: 3600
  },
  'gemini-2.5-pro': {
    imageUnderstanding: true,
    objectDetection: true,
    segmentation: true,
    maxImages: 3600
  }
};
```

## Integration Examples

### Express.js API Endpoint
```javascript
app.post('/api/analyze-image', upload.single('image'), async (req, res) => {
  try {
    const { prompt, analysisType = 'description' } = req.body;
    const imagePath = req.file.path;

    const analysisPrompts = {
      description: "Provide a detailed description of this image",
      objects: "Identify and list all objects in this image",
      text: "Extract any text visible in this image",
      quality: "Assess the technical quality of this image",
      classification: "Classify this image into appropriate categories"
    };

    const finalPrompt = prompt || analysisPrompts[analysisType];
    const result = await safeImageProcessing(imagePath, finalPrompt);

    // Clean up uploaded file
    fs.unlinkSync(imagePath);

    res.json({
      success: true,
      analysis: result,
      analysisType
    });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});
```

### Image Management Service
```javascript
class ImageAnalysisService {
  constructor() {
    this.ai = new GoogleGenAI({});
    this.cache = new ImageCache();
    this.processor = new ImageProcessor();
    this.classifier = new ImageClassifier();
  }

  async analyzeImage(imagePath, analysisType, customPrompt = null) {
    const prompts = {
      product: "Analyze this product image for features, condition, and marketability",
      medical: "Describe anatomical structures and notable features (educational purposes only)",
      document: "Extract and organize all text and data from this document image",
      scene: "Provide comprehensive scene analysis including setting, objects, and activities"
    };

    const prompt = customPrompt || prompts[analysisType] || prompts.product;
    return await analyzeImageWithCache(imagePath, prompt);
  }

  async detectObjects(imagePath, objectTypes = null) {
    if (objectTypes) {
      return await detectSpecificObjects(imagePath, objectTypes);
    } else {
      return await detectObjects(imagePath);
    }
  }

  async segmentImage(imagePath, targetObjects) {
    const segmentations = await segmentObjects(imagePath, targetObjects);
    return await processSegmentationMasks(imagePath, segmentations);
  }

  async classifyImages(imagePaths, categories = null) {
    return await this.classifier.batchClassify(imagePaths, categories);
  }

  async generateReport(imagePaths, reportType = 'comprehensive') {
    const results = await this.processor.processBatch(
      imagePaths,
      "Analyze this image and extract key information"
    );

    const reportPrompt = `Based on analysis of ${imagePaths.length} images, create a ${reportType} report with:
1. Summary of image content and themes
2. Common objects and patterns identified
3. Quality assessment across images
4. Recommendations for image optimization
5. Key insights and findings`;

    const combinedData = results.map(r => r.result).join('\n\n---\n\n');
    
    const response = await this.ai.models.generateContent({
      model: "gemini-2.5-flash",
      contents: [{ text: `${reportPrompt}\n\nImage Analysis Data:\n${combinedData}` }]
    });

    return response.text;
  }
}
```

## Best Practices Summary

### Image Preparation
1. **Quality assurance**: Use clear, well-lit, properly oriented images
2. **Format optimization**: Use supported formats (JPEG, PNG, WebP, HEIC, HEIF)
3. **Size management**: Consider File API for images > 20MB total request size
4. **Resolution**: Higher resolution provides better analysis but costs more tokens

### Processing Strategy
1. **Prompt specificity**: Be specific about what you want to analyze or extract
2. **Multi-image analysis**: Use multiple images for comparison and context
3. **Batch processing**: Process multiple images efficiently with proper rate limiting
4. **Caching**: Implement caching for frequently analyzed images

### Advanced Features
1. **Object detection**: Use Gemini 2.0+ for bounding box detection
2. **Segmentation**: Use Gemini 2.5+ for detailed segmentation masks
3. **Custom detection**: Specify particular objects or features to detect
4. **Structured output**: Use JSON output for programmatic processing

### Performance Optimization
1. **Token management**: Understand token costs for different image sizes
2. **Error handling**: Implement robust retry logic and fallback strategies
3. **Rate limiting**: Respect API limits and implement proper delays
4. **Model selection**: Choose appropriate model based on required capabilities