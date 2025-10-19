# Gemini API Video Understanding Guide

## Overview
Gemini models can process videos to enable frontier use cases including video description, segmentation, information extraction, question answering, and timestamp-specific analysis. Built to be multimodal from the ground up, Gemini continues to push the boundaries of video understanding capabilities.

## Key Capabilities
- **Describe and segment videos**: Extract information and analyze video content
- **Answer questions about video content**: Provide specific answers about video elements
- **Timestamp references**: Refer to specific moments within videos using MM:SS format
- **Transcription and visual descriptions**: Process both audio and visual elements
- **Custom frame rate sampling**: Control video processing parameters
- **YouTube URL support**: Direct analysis of YouTube videos (preview feature)

## Basic Setup

### JavaScript/TypeScript Implementation
```javascript
import { GoogleGenAI, createPartFromUri } from "@google/genai";

const ai = new GoogleGenAI({
  apiKey: process.env.GEMINI_API_KEY
});

// Basic video analysis
async function analyzeVideo(videoPath, prompt) {
  // Upload video file
  const file = await ai.files.upload({
    file: videoPath,
    config: { mimeType: "video/mp4" }
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
const summary = await analyzeVideo("./sample.mp4", "Summarize this video and create a quiz based on the content");
```

## Video Input Methods

### File API Upload (Recommended)
```javascript
async function processVideoFile(videoPath, prompt) {
  // Upload the video file
  const file = await ai.files.upload({
    file: videoPath,
    config: { mimeType: "video/mp4" }
  });

  // Wait for processing if needed
  let uploadedFile = await ai.files.get({ name: file.name });
  while (uploadedFile.state === 'PROCESSING') {
    console.log('Video is processing...');
    await new Promise(resolve => setTimeout(resolve, 5000));
    uploadedFile = await ai.files.get({ name: file.name });
  }

  if (uploadedFile.state === 'FAILED') {
    throw new Error('Video processing failed');
  }

  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: [
      createPartFromUri(uploadedFile.uri, uploadedFile.mimeType),
      { text: prompt }
    ]
  });

  return response.text;
}

// Usage
const analysis = await processVideoFile("./presentation.mp4", "Extract key points and create a summary");
```

### Inline Video Data (< 20MB)
```javascript
import * as fs from "node:fs";

async function processInlineVideo(videoPath, prompt) {
  const videoBuffer = fs.readFileSync(videoPath);
  const base64Video = videoBuffer.toString("base64");

  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: [
      {
        inlineData: {
          mimeType: "video/mp4",
          data: base64Video,
        },
      },
      { text: prompt }
    ]
  });

  return response.text;
}

// Usage (only for small videos)
const summary = await processInlineVideo("./short-clip.mp4", "Describe what happens in this video");
```

### YouTube URL Processing (Preview)
```javascript
async function analyzeYouTubeVideo(youtubeUrl, prompt) {
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: [
      {
        fileData: {
          fileUri: youtubeUrl
        }
      },
      { text: prompt }
    ]
  });

  return response.text;
}

// Usage
const analysis = await analyzeYouTubeVideo(
  "https://www.youtube.com/watch?v=example",
  "Summarize the main points of this video"
);
```

## Advanced Video Processing

### Custom Frame Rate and Clipping
```javascript
async function processVideoWithCustomSettings(videoPath, options = {}) {
  const {
    fps = 1,
    startOffset = null,
    endOffset = null,
    prompt = "Analyze this video content"
  } = options;

  // For inline processing with custom settings
  const videoBuffer = fs.readFileSync(videoPath);
  
  const videoMetadata = {};
  if (fps !== 1) videoMetadata.fps = fps;
  if (startOffset) videoMetadata.startOffset = startOffset;
  if (endOffset) videoMetadata.endOffset = endOffset;

  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: [
      {
        inlineData: {
          mimeType: "video/mp4",
          data: videoBuffer.toString("base64")
        },
        videoMetadata: Object.keys(videoMetadata).length > 0 ? videoMetadata : undefined
      },
      { text: prompt }
    ]
  });

  return response.text;
}

// Usage examples
const highFpsAnalysis = await processVideoWithCustomSettings("./action.mp4", {
  fps: 5,
  prompt: "Analyze the rapid movements in this action sequence"
});

const clippedAnalysis = await processVideoWithCustomSettings("./lecture.mp4", {
  startOffset: "300s",
  endOffset: "600s",
  prompt: "Summarize this 5-minute segment"
});
```

### YouTube Video with Clipping
```javascript
async function analyzeYouTubeClip(youtubeUrl, startTime, endTime, prompt) {
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: [
      {
        fileData: {
          fileUri: youtubeUrl
        },
        videoMetadata: {
          startOffset: startTime,
          endOffset: endTime
        }
      },
      { text: prompt }
    ]
  });

  return response.text;
}

// Usage
const clipAnalysis = await analyzeYouTubeClip(
  "https://www.youtube.com/watch?v=example",
  "120s",
  "300s",
  "Analyze this 3-minute segment"
);
```

## Video Analysis Use Cases

### Educational Content Analysis
```javascript
async function analyzeEducationalVideo(videoPath) {
  const prompt = `Analyze this educational video and provide:
1. Main topics and learning objectives
2. Key concepts explained
3. Teaching methods used
4. Visual aids and demonstrations
5. Suggested quiz questions (5-10)
6. Summary for different audience levels
7. Timestamps for major topic transitions

Format as structured educational analysis.`;

  return await processVideoFile(videoPath, prompt);
}
```

### Meeting and Presentation Analysis
```javascript
async function analyzeMeeting(videoPath) {
  const prompt = `Analyze this meeting/presentation video:
1. Identify speakers and their roles
2. Extract key decisions and action items
3. Summarize main discussion points
4. Note any presentations or demos shown
5. Identify follow-up tasks mentioned
6. Create meeting minutes format
7. Highlight important timestamps

Provide a professional meeting summary.`;

  return await processVideoFile(videoPath, prompt);
}
```

### Content Moderation and Safety
```javascript
async function moderateVideoContent(videoPath) {
  const prompt = `Review this video content for:
1. Inappropriate or harmful content
2. Violence or dangerous activities
3. Adult content or suggestive material
4. Hate speech or discriminatory language
5. Copyright or trademark violations
6. Misinformation or false claims
7. Overall safety rating (1-10)

Provide detailed moderation report with timestamps.`;

  return await processVideoFile(videoPath, prompt);
}
```

### Sports and Activity Analysis
```javascript
async function analyzeSportsVideo(videoPath, sport = 'general') {
  const sportPrompts = {
    general: "Analyze the athletic performance and techniques shown",
    football: "Analyze plays, formations, and player movements",
    basketball: "Analyze shooting techniques, defensive strategies, and game flow",
    tennis: "Analyze serving technique, shot selection, and court positioning",
    golf: "Analyze swing mechanics and course strategy"
  };

  const basePrompt = sportPrompts[sport] || sportPrompts.general;
  
  const prompt = `${basePrompt} in this video. Provide:
1. Technical analysis of performance
2. Strengths and areas for improvement
3. Key moments and highlights
4. Coaching recommendations
5. Comparison to best practices
6. Timestamp references for key actions

Format as sports analysis report.`;

  return await processVideoFile(videoPath, prompt);
}
```

### Product Demo and Review Analysis
```javascript
async function analyzeProductDemo(videoPath) {
  const prompt = `Analyze this product demonstration video:
1. Product features highlighted
2. Use cases and applications shown
3. Benefits and advantages mentioned
4. Potential drawbacks or limitations
5. Target audience and market
6. Competitive comparisons made
7. Call-to-action or next steps
8. Overall presentation effectiveness

Create comprehensive product analysis.`;

  return await processVideoFile(videoPath, prompt);
}
```

## Timestamp-Based Analysis

### Specific Time References
```javascript
async function analyzeVideoAtTimestamps(videoPath, timestamps, questions) {
  const timestampQueries = timestamps.map((time, index) => 
    `At ${time}: ${questions[index] || 'What is happening at this moment?'}`
  ).join('\n');

  const prompt = `Analyze this video at specific timestamps:
${timestampQueries}

Provide detailed answers for each timestamp with context.`;

  return await processVideoFile(videoPath, prompt);
}

// Usage
const timestampAnalysis = await analyzeVideoAtTimestamps(
  "./tutorial.mp4",
  ["02:30", "05:15", "08:45"],
  [
    "What concept is being introduced?",
    "What example is being demonstrated?",
    "What conclusion is being drawn?"
  ]
);
```

### Chapter and Section Identification
```javascript
async function identifyVideoChapters(videoPath) {
  const prompt = `Analyze this video and identify natural chapters or sections:
1. Detect topic transitions and breaks
2. Suggest chapter titles and descriptions
3. Provide start and end timestamps for each section
4. Identify the main theme of each chapter
5. Note any visual or audio cues for transitions
6. Suggest optimal chapter markers for navigation

Format as structured chapter guide with timestamps.`;

  return await processVideoFile(videoPath, prompt);
}
```

### Highlight and Key Moment Detection
```javascript
async function detectVideoHighlights(videoPath, contentType = 'general') {
  const contentPrompts = {
    general: "Identify the most important and engaging moments",
    educational: "Find key learning moments and important explanations",
    entertainment: "Detect funny, exciting, or emotionally impactful moments",
    sports: "Identify scoring plays, great performances, and key game moments",
    business: "Find important decisions, key insights, and action items"
  };

  const prompt = `${contentPrompts[contentType]} in this video:
1. Identify 5-10 key highlights with timestamps
2. Explain why each moment is significant
3. Rate importance/impact (1-10) for each highlight
4. Suggest clip lengths for social media sharing
5. Identify quotable moments or soundbites
6. Note visual elements that make moments compelling

Create highlight reel recommendations.`;

  return await processVideoFile(videoPath, prompt);
}
```

## Transcription and Audio Analysis

### Complete Transcription
```javascript
async function transcribeVideo(videoPath, includeTimestamps = true) {
  const prompt = includeTimestamps 
    ? "Generate a complete transcript of this video with timestamps for each speaker change and major topic shift. Include speaker identification if multiple people are present."
    : "Generate a complete transcript of the speech in this video. Clean up any filler words and organize into readable paragraphs.";

  return await processVideoFile(videoPath, prompt);
}
```

### Audio and Visual Description
```javascript
async function getAudioVisualDescription(videoPath) {
  const prompt = `Provide comprehensive audio and visual descriptions of this video:

AUDIO ANALYSIS:
1. Transcribe all speech with timestamps
2. Identify background music and sound effects
3. Note audio quality and clarity
4. Identify different speakers/voices
5. Describe tone and emotional content

VISUAL ANALYSIS:
1. Describe scenes and settings (sampled at 1fps)
2. Identify people, objects, and activities
3. Note visual transitions and effects
4. Describe text, graphics, or overlays shown
5. Comment on video quality and production value

SYNCHRONIZATION:
1. How audio and visual elements work together
2. Key moments where both are important
3. Any audio-visual mismatches or issues

Format as detailed multimedia analysis.`;

  return await processVideoFile(videoPath, prompt);
}
```

## Batch Video Processing

### Multiple Video Analysis
```javascript
class VideoProcessor {
  constructor() {
    this.ai = new GoogleGenAI({});
  }

  async processBatch(videoPaths, analysisPrompt, options = {}) {
    const { 
      maxConcurrent = 2, // Lower concurrency for video processing
      delay = 5000, // Longer delay for video processing
      useInline = false 
    } = options;

    const results = [];
    
    for (let i = 0; i < videoPaths.length; i += maxConcurrent) {
      const batch = videoPaths.slice(i, i + maxConcurrent);
      
      const batchPromises = batch.map(async (videoPath, index) => {
        try {
          let result;
          if (useInline && this.getFileSize(videoPath) < 20 * 1024 * 1024) {
            result = await this.processInline(videoPath, analysisPrompt);
          } else {
            result = await this.processWithFileAPI(videoPath, analysisPrompt);
          }

          return {
            videoPath,
            result,
            success: true,
            index: i + index
          };
        } catch (error) {
          return {
            videoPath,
            error: error.message,
            success: false,
            index: i + index
          };
        }
      });

      const batchResults = await Promise.all(batchPromises);
      results.push(...batchResults);

      // Rate limiting delay
      if (i + maxConcurrent < videoPaths.length) {
        await new Promise(resolve => setTimeout(resolve, delay));
      }
    }

    return results;
  }

  async processInline(videoPath, prompt) {
    return await processInlineVideo(videoPath, prompt);
  }

  async processWithFileAPI(videoPath, prompt) {
    return await processVideoFile(videoPath, prompt);
  }

  getFileSize(filePath) {
    return fs.statSync(filePath).size;
  }
}

// Usage
const processor = new VideoProcessor();
const results = await processor.processBatch([
  "./video1.mp4",
  "./video2.mp4",
  "./video3.mp4"
], "Create a summary and identify key topics in this video");
```

### Video Comparison Analysis
```javascript
async function compareVideos(videoPaths, comparisonCriteria) {
  const uploadedFiles = [];
  
  // Upload all videos
  for (let i = 0; i < videoPaths.length; i++) {
    const file = await ai.files.upload({
      file: videoPaths[i],
      config: { mimeType: "video/mp4" }
    });
    
    // Wait for processing
    let uploadedFile = await ai.files.get({ name: file.name });
    while (uploadedFile.state === 'PROCESSING') {
      await new Promise(resolve => setTimeout(resolve, 5000));
      uploadedFile = await ai.files.get({ name: file.name });
    }
    
    uploadedFiles.push(uploadedFile);
  }

  // Create content for comparison
  const contents = [
    { text: `Compare these videos based on ${comparisonCriteria}. Analyze similarities, differences, and provide detailed comparison.` }
  ];
  
  uploadedFiles.forEach(file => {
    contents.push(createPartFromUri(file.uri, file.mimeType));
  });

  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: contents
  });

  return response.text;
}

// Usage
const comparison = await compareVideos([
  "./presentation-v1.mp4",
  "./presentation-v2.mp4"
], "content quality, presentation style, and audience engagement");
```

## Error Handling and Best Practices

### Robust Video Processing
```javascript
async function safeVideoProcessing(videoPath, prompt, options = {}) {
  const {
    maxRetries = 3,
    timeoutMs = 300000, // 5 minutes for video processing
    fallbackToInline = false
  } = options;

  // Validate video file
  const validation = validateVideoFile(videoPath);
  if (!validation.valid) {
    throw new Error(`Invalid video: ${validation.error}`);
  }

  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      const fileSize = fs.statSync(videoPath).size;
      
      if (fileSize > 20 * 1024 * 1024 || !fallbackToInline) {
        return await processVideoFile(videoPath, prompt);
      } else {
        return await processInlineVideo(videoPath, prompt);
      }
    } catch (error) {
      console.error(`Video processing attempt ${attempt} failed:`, error.message);
      
      if (attempt === maxRetries) {
        throw error;
      }
      
      // Wait before retry with exponential backoff
      await new Promise(resolve => setTimeout(resolve, 2000 * attempt));
    }
  }
}

function validateVideoFile(videoPath) {
  const supportedFormats = ['.mp4', '.mpeg', '.mov', '.avi', '.flv', '.mpg', '.webm', '.wmv', '.3gpp'];
  const ext = path.extname(videoPath).toLowerCase();
  
  if (!supportedFormats.includes(ext)) {
    return { valid: false, error: `Unsupported format: ${ext}` };
  }

  const stats = fs.statSync(videoPath);
  const maxSize = 2 * 1024 * 1024 * 1024; // 2GB
  
  if (stats.size > maxSize) {
    return { valid: false, error: `File too large: ${stats.size} bytes` };
  }

  return { valid: true, size: stats.size };
}
```

### Performance Optimization
```javascript
class VideoCache {
  constructor(ttl = 3600000) { // 1 hour default TTL
    this.cache = new Map();
    this.ttl = ttl;
  }

  generateKey(videoPath, prompt, options = {}) {
    const stats = fs.statSync(videoPath);
    const optionsStr = JSON.stringify(options);
    return `${videoPath}:${stats.mtime.getTime()}:${this.hashString(prompt + optionsStr)}`;
  }

  hashString(str) {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      const char = str.charCodeAt(i);
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
const videoCache = new VideoCache();

async function analyzeVideoWithCache(videoPath, prompt, options = {}) {
  const cacheKey = videoCache.generateKey(videoPath, prompt, options);
  const cached = videoCache.get(cacheKey);
  
  if (cached) {
    return cached;
  }

  const result = await safeVideoProcessing(videoPath, prompt, options);
  videoCache.set(cacheKey, result);
  
  return result;
}
```

## Technical Details and Limitations

### Supported Video Formats
```javascript
const SUPPORTED_VIDEO_FORMATS = {
  'video/mp4': ['.mp4'],
  'video/mpeg': ['.mpeg', '.mpg'],
  'video/mov': ['.mov'],
  'video/avi': ['.avi'],
  'video/x-flv': ['.flv'],
  'video/webm': ['.webm'],
  'video/wmv': ['.wmv'],
  'video/3gpp': ['.3gpp']
};

function getSupportedVideoMimeType(filePath) {
  const ext = path.extname(filePath).toLowerCase();
  
  for (const [mimeType, extensions] of Object.entries(SUPPORTED_VIDEO_FORMATS)) {
    if (extensions.includes(ext)) {
      return mimeType;
    }
  }
  
  return null;
}
```

### Token Calculation and Costs
```javascript
function calculateVideoTokens(durationSeconds, mediaResolution = 'default') {
  // Frame tokens (1 FPS sampling)
  const frameTokens = mediaResolution === 'low' ? 66 : 258;
  const totalFrameTokens = durationSeconds * frameTokens;
  
  // Audio tokens (32 tokens per second)
  const audioTokens = durationSeconds * 32;
  
  // Total tokens per second
  const tokensPerSecond = frameTokens + 32;
  
  return {
    totalTokens: totalFrameTokens + audioTokens,
    frameTokens: totalFrameTokens,
    audioTokens: audioTokens,
    tokensPerSecond: tokensPerSecond,
    estimatedCost: (totalFrameTokens + audioTokens) * 0.000125 // Rough estimate
  };
}

// Usage
const videoTokens = calculateVideoTokens(300); // 5-minute video
console.log(`5-minute video will use approximately ${videoTokens.totalTokens} tokens`);
```

### Model Context Limits
```javascript
const VIDEO_CONTEXT_LIMITS = {
  '2M_context': {
    defaultResolution: { maxDuration: 7200 }, // 2 hours
    lowResolution: { maxDuration: 21600 }     // 6 hours
  },
  '1M_context': {
    defaultResolution: { maxDuration: 3600 }, // 1 hour
    lowResolution: { maxDuration: 10800 }     // 3 hours
  }
};

function checkVideoCompatibility(durationSeconds, modelContext = '1M_context', mediaResolution = 'default') {
  const limits = VIDEO_CONTEXT_LIMITS[modelContext];
  const resolutionKey = mediaResolution === 'low' ? 'lowResolution' : 'defaultResolution';
  const maxDuration = limits[resolutionKey].maxDuration;
  
  return {
    compatible: durationSeconds <= maxDuration,
    maxDuration: maxDuration,
    currentDuration: durationSeconds,
    recommendation: durationSeconds > maxDuration 
      ? `Video too long. Consider using ${mediaResolution === 'low' ? 'shorter clips' : 'low resolution'} or splitting into segments.`
      : 'Video duration is compatible'
  };
}
```

## Integration Examples

### Express.js API Endpoint
```javascript
app.post('/api/analyze-video', upload.single('video'), async (req, res) => {
  try {
    const { prompt, analysisType = 'summary', includeTimestamps = false } = req.body;
    const videoPath = req.file.path;

    // Validate video
    const validation = validateVideoFile(videoPath);
    if (!validation.valid) {
      return res.status(400).json({ error: validation.error });
    }

    const analysisPrompts = {
      summary: "Provide a comprehensive summary of this video content",
      transcription: "Generate a complete transcript of this video with speaker identification",
      highlights: "Identify key highlights and important moments with timestamps",
      chapters: "Break down this video into logical chapters with timestamps",
      moderation: "Review this video content for safety and appropriateness"
    };

    let finalPrompt = prompt || analysisPrompts[analysisType];
    if (includeTimestamps && !prompt) {
      finalPrompt += " Include relevant timestamps in your analysis.";
    }

    const result = await safeVideoProcessing(videoPath, finalPrompt);

    // Clean up uploaded file
    fs.unlinkSync(videoPath);

    res.json({
      success: true,
      analysis: result,
      analysisType,
      videoInfo: {
        size: validation.size,
        format: path.extname(videoPath)
      }
    });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});
```

### Video Management Service
```javascript
class VideoAnalysisService {
  constructor() {
    this.ai = new GoogleGenAI({});
    this.cache = new VideoCache();
    this.processor = new VideoProcessor();
  }

  async analyzeVideo(videoPath, analysisType, customPrompt = null) {
    const prompts = {
      educational: "Analyze this educational content for learning objectives, key concepts, and teaching effectiveness",
      meeting: "Extract meeting minutes, action items, and key decisions from this video",
      presentation: "Analyze presentation structure, key points, and audience engagement factors",
      training: "Evaluate training content for completeness, clarity, and instructional design",
      marketing: "Analyze marketing message, target audience appeal, and call-to-action effectiveness"
    };

    const prompt = customPrompt || prompts[analysisType] || prompts.educational;
    return await analyzeVideoWithCache(videoPath, prompt);
  }

  async transcribeVideo(videoPath, options = {}) {
    const { includeTimestamps = true, cleanTranscript = false } = options;
    
    let prompt = "Generate a complete transcript of this video";
    if (includeTimestamps) prompt += " with timestamps for major sections";
    if (cleanTranscript) prompt += ". Remove filler words and organize into readable paragraphs";
    
    return await analyzeVideoWithCache(videoPath, prompt, options);
  }

  async detectHighlights(videoPath, contentType = 'general', maxHighlights = 10) {
    const prompt = `Identify the top ${maxHighlights} highlights in this ${contentType} video. For each highlight, provide:
1. Timestamp (MM:SS format)
2. Duration recommendation for clip
3. Description of what makes it significant
4. Importance rating (1-10)
5. Suggested title for the highlight

Format as structured highlight list.`;

    return await analyzeVideoWithCache(videoPath, prompt, { contentType, maxHighlights });
  }

  async compareVideos(videoPaths, comparisonType = 'content') {
    const comparisonPrompts = {
      content: "Compare the content, topics, and information presented",
      quality: "Compare video quality, production value, and technical aspects",
      engagement: "Compare audience engagement factors and presentation effectiveness",
      performance: "Compare speaker performance, delivery, and communication skills"
    };

    return await compareVideos(videoPaths, comparisonPrompts[comparisonType]);
  }

  async generateVideoReport(videoPaths, reportType = 'comprehensive') {
    const results = await this.processor.processBatch(
      videoPaths,
      "Analyze this video and extract key information, topics, and insights"
    );

    const reportPrompt = `Based on analysis of ${videoPaths.length} videos, create a ${reportType} report with:
1. Executive summary of all video content
2. Common themes and topics across videos
3. Quality assessment and technical analysis
4. Key insights and recommendations
5. Comparative analysis between videos
6. Suggested improvements or optimizations

Format as professional video analysis report.`;

    const combinedData = results.map(r => r.result).join('\n\n---\n\n');
    
    const response = await this.ai.models.generateContent({
      model: "gemini-2.5-flash",
      contents: [{ text: `${reportPrompt}\n\nVideo Analysis Data:\n${combinedData}` }]
    });

    return response.text;
  }
}
```

## Best Practices Summary

### Video Preparation
1. **Quality assurance**: Use clear, well-produced videos with good audio quality
2. **Format optimization**: Use supported formats (MP4 recommended for best compatibility)
3. **Duration management**: Consider context limits and token costs for long videos
4. **File size**: Use File API for videos > 20MB or longer than ~1 minute

### Processing Strategy
1. **Method selection**: Use File API for most videos, inline only for very short clips
2. **Frame rate optimization**: Adjust FPS based on content type (higher for action, lower for static)
3. **Clipping**: Use start/end offsets to analyze specific segments
4. **Batch processing**: Process multiple videos efficiently with proper rate limiting

### Prompt Engineering
1. **Timestamp requests**: Use MM:SS format when referring to specific times
2. **Specific analysis**: Be clear about what aspects to analyze (audio, visual, both)
3. **Output format**: Specify desired format (transcript, summary, structured data)
4. **Context provision**: Provide context about video type and analysis goals

### Performance Optimization
1. **Caching**: Implement caching for frequently analyzed videos
2. **Error handling**: Implement robust retry logic for video processing
3. **Rate limiting**: Respect API limits and implement proper delays
4. **Token management**: Monitor token usage for long videos and optimize accordingly