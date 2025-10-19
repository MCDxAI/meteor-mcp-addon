# Gemini API Audio Understanding Guide

## Overview
Gemini models can analyze and understand audio input, enabling use cases such as audio description, summarization, question answering, transcription, and analysis of specific audio segments. This capability supports various audio formats and provides comprehensive audio processing features.

## Key Capabilities
- **Describe and summarize audio content**: Generate detailed descriptions of audio elements
- **Provide transcriptions**: Convert speech to text with high accuracy
- **Answer questions about audio**: Respond to specific queries about audio content
- **Analyze specific segments**: Process particular timestamps within audio files
- **Multi-format support**: Handle various audio formats (WAV, MP3, AIFF, AAC, OGG, FLAC)
- **Non-speech understanding**: Recognize and describe environmental sounds, music, and effects

## Basic Setup

### JavaScript/TypeScript Implementation
```javascript
import { GoogleGenAI, createPartFromUri } from "@google/genai";
import * as fs from "node:fs";

const ai = new GoogleGenAI({
  apiKey: process.env.GEMINI_API_KEY
});

// Basic audio analysis
async function analyzeAudio(audioPath, prompt) {
  // Upload audio file
  const file = await ai.files.upload({
    file: audioPath,
    config: { mimeType: "audio/mp3" }
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
const description = await analyzeAudio("./sample.mp3", "Describe this audio clip");
```

## Audio Input Methods

### File API Upload (Recommended)
```javascript
async function processAudioFile(audioPath, prompt) {
  // Upload the audio file
  const file = await ai.files.upload({
    file: audioPath,
    config: { mimeType: "audio/mp3" }
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
const analysis = await processAudioFile("./interview.wav", "Summarize the key points discussed");
```

### Inline Audio Data (< 20MB)
```javascript
async function processInlineAudio(audioPath, prompt) {
  const audioBuffer = fs.readFileSync(audioPath);
  const base64Audio = audioBuffer.toString("base64");

  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: [
      {
        inlineData: {
          mimeType: "audio/mp3",
          data: base64Audio,
        },
      },
      { text: prompt }
    ]
  });

  return response.text;
}

// Usage (only for small audio files)
const summary = await processInlineAudio("./short-clip.mp3", "What is being discussed in this audio?");
```

### Auto-detect MIME Type
```javascript
function getAudioMimeType(filePath) {
  const ext = path.extname(filePath).toLowerCase();
  const mimeTypes = {
    '.wav': 'audio/wav',
    '.mp3': 'audio/mp3',
    '.aiff': 'audio/aiff',
    '.aac': 'audio/aac',
    '.ogg': 'audio/ogg',
    '.flac': 'audio/flac'
  };
  return mimeTypes[ext] || 'audio/mp3';
}

async function processAudioWithAutoDetect(audioPath, prompt) {
  const mimeType = getAudioMimeType(audioPath);
  
  const file = await ai.files.upload({
    file: audioPath,
    config: { mimeType }
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
```

## Audio Analysis Use Cases

### Transcription Services
```javascript
async function transcribeAudio(audioPath, options = {}) {
  const {
    includeTimestamps = false,
    speakerIdentification = false,
    cleanTranscript = false
  } = options;

  let prompt = "Generate a transcript of the speech in this audio";
  
  if (includeTimestamps) {
    prompt += " with timestamps for major sections and speaker changes";
  }
  
  if (speakerIdentification) {
    prompt += ". Identify different speakers and label them consistently";
  }
  
  if (cleanTranscript) {
    prompt += ". Remove filler words (um, uh, etc.) and organize into readable paragraphs";
  }

  return await processAudioFile(audioPath, prompt);
}

// Usage examples
const basicTranscript = await transcribeAudio("./meeting.wav");
const detailedTranscript = await transcribeAudio("./interview.mp3", {
  includeTimestamps: true,
  speakerIdentification: true,
  cleanTranscript: true
});
```

### Meeting and Interview Analysis
```javascript
async function analyzeMeetingAudio(audioPath) {
  const prompt = `Analyze this meeting audio and provide:
1. Complete transcript with speaker identification
2. Key decisions made and action items
3. Main topics discussed with timestamps
4. Summary of each speaker's contributions
5. Follow-up tasks and deadlines mentioned
6. Overall meeting effectiveness assessment
7. Important quotes or statements

Format as professional meeting minutes.`;

  return await processAudioFile(audioPath, prompt);
}

async function analyzeInterview(audioPath, interviewType = 'general') {
  const prompts = {
    general: "Analyze this interview for key insights, main topics, and important responses",
    job: "Analyze this job interview for candidate responses, qualifications discussed, and interviewer questions",
    research: "Analyze this research interview for key findings, methodologies discussed, and expert opinions",
    media: "Analyze this media interview for newsworthy statements, key messages, and public relations aspects"
  };

  const basePrompt = prompts[interviewType] || prompts.general;
  
  const prompt = `${basePrompt}. Provide:
1. Complete transcript with speaker labels
2. Key topics and themes discussed
3. Important quotes and statements
4. Questions asked and responses given
5. Overall tone and sentiment analysis
6. Actionable insights or conclusions

Format as structured interview analysis.`;

  return await processAudioFile(audioPath, prompt);
}
```

### Podcast and Content Analysis
```javascript
async function analyzePodcast(audioPath) {
  const prompt = `Analyze this podcast episode and provide:
1. Episode summary and main themes
2. Key topics discussed with timestamps
3. Host and guest information
4. Important insights and takeaways
5. Quotable moments and soundbites
6. Audience engagement factors
7. Content quality assessment
8. Suggested episode title and description

Format as comprehensive podcast analysis.`;

  return await processAudioFile(audioPath, prompt);
}

async function analyzeEducationalContent(audioPath) {
  const prompt = `Analyze this educational audio content:
1. Learning objectives and key concepts
2. Teaching methods and presentation style
3. Content accuracy and clarity
4. Student engagement factors
5. Suggested improvements
6. Quiz questions based on content (5-10)
7. Summary for different learning levels
8. Key timestamps for important concepts

Format as educational content review.`;

  return await processAudioFile(audioPath, prompt);
}
```

### Music and Sound Analysis
```javascript
async function analyzeMusicAudio(audioPath) {
  const prompt = `Analyze this music audio and describe:
1. Musical genre and style
2. Instruments present
3. Vocal characteristics (if any)
4. Tempo and rhythm patterns
5. Mood and emotional content
6. Production quality
7. Notable musical elements or techniques
8. Overall artistic assessment

Provide detailed musical analysis.`;

  return await processAudioFile(audioPath, prompt);
}

async function analyzeEnvironmentalAudio(audioPath) {
  const prompt = `Analyze the environmental sounds in this audio:
1. Identify all distinct sounds and their sources
2. Describe the acoustic environment/setting
3. Note any human activities or speech
4. Identify background noise and ambiance
5. Assess audio quality and clarity
6. Describe the overall soundscape
7. Note any unusual or significant sounds

Provide comprehensive environmental audio analysis.`;

  return await processAudioFile(audioPath, prompt);
}
```

### Customer Service and Call Analysis
```javascript
async function analyzeCustomerCall(audioPath) {
  const prompt = `Analyze this customer service call:
1. Identify customer issue or inquiry
2. Evaluate agent response and helpfulness
3. Track problem resolution process
4. Note customer satisfaction indicators
5. Identify areas for improvement
6. Assess call quality and professionalism
7. Extract key information and outcomes
8. Rate overall call effectiveness (1-10)

Format as customer service call analysis.`;

  return await processAudioFile(audioPath, prompt);
}

async function analyzeSalesCall(audioPath) {
  const prompt = `Analyze this sales call recording:
1. Identify products/services discussed
2. Track customer objections and responses
3. Note sales techniques used
4. Assess customer interest level
5. Identify next steps or commitments
6. Evaluate sales representative performance
7. Extract key decision factors
8. Predict likelihood of sale closure

Format as sales call analysis report.`;

  return await processAudioFile(audioPath, prompt);
}
```

## Timestamp-Based Analysis

### Specific Time References
```javascript
async function analyzeAudioAtTimestamps(audioPath, timestamps, questions) {
  const timestampQueries = timestamps.map((time, index) => 
    `At ${time}: ${questions[index] || 'What is being discussed at this moment?'}`
  ).join('\n');

  const prompt = `Analyze this audio at specific timestamps:
${timestampQueries}

Provide detailed answers for each timestamp with context from the surrounding audio.`;

  return await processAudioFile(audioPath, prompt);
}

// Usage
const timestampAnalysis = await analyzeAudioAtTimestamps(
  "./lecture.mp3",
  ["05:30", "12:15", "18:45"],
  [
    "What concept is being explained?",
    "What example is being given?",
    "What conclusion is being drawn?"
  ]
);
```

### Segment-Based Analysis
```javascript
async function analyzeAudioSegments(audioPath, segments) {
  const segmentQueries = segments.map((segment, index) => 
    `Segment ${index + 1} (${segment.start} to ${segment.end}): ${segment.question}`
  ).join('\n');

  const prompt = `Analyze specific segments of this audio:
${segmentQueries}

For each segment, provide detailed analysis and context.`;

  return await processAudioFile(audioPath, prompt);
}

// Usage
const segmentAnalysis = await analyzeAudioSegments("./presentation.wav", [
  { start: "02:00", end: "05:30", question: "Summarize the introduction" },
  { start: "10:15", end: "15:45", question: "What are the main arguments presented?" },
  { start: "20:00", end: "23:30", question: "What conclusions are drawn?" }
]);
```

### Chapter and Topic Identification
```javascript
async function identifyAudioChapters(audioPath) {
  const prompt = `Analyze this audio and identify natural chapters or topic segments:
1. Detect topic transitions and breaks
2. Suggest chapter titles and descriptions
3. Provide start and end timestamps for each section
4. Identify the main theme of each chapter
5. Note any audio cues for transitions (music, pauses, etc.)
6. Suggest optimal chapter markers for navigation

Format as structured chapter guide with timestamps.`;

  return await processAudioFile(audioPath, prompt);
}
```

## Advanced Audio Processing

### Multi-Language Support
```javascript
async function analyzeMultiLanguageAudio(audioPath, expectedLanguages = []) {
  const languageContext = expectedLanguages.length > 0 
    ? ` The audio may contain ${expectedLanguages.join(', ')}.`
    : '';

  const prompt = `Analyze this audio content and provide:
1. Identify the language(s) spoken${languageContext}
2. Provide transcription in the original language(s)
3. Translate to English if not already in English
4. Note any code-switching or mixed languages
5. Assess audio quality for each language segment
6. Identify speakers and their languages
7. Provide summary in English

Format as multi-language audio analysis.`;

  return await processAudioFile(audioPath, prompt);
}
```

### Audio Quality Assessment
```javascript
async function assessAudioQuality(audioPath) {
  const prompt = `Assess the technical and content quality of this audio:

TECHNICAL QUALITY:
1. Audio clarity and noise levels
2. Volume consistency
3. Frequency response and balance
4. Presence of distortion or artifacts
5. Recording environment assessment
6. Overall technical quality score (1-10)

CONTENT QUALITY:
1. Speech clarity and pronunciation
2. Pace and delivery effectiveness
3. Content organization and flow
4. Engagement and presentation style
5. Professional presentation quality
6. Overall content quality score (1-10)

RECOMMENDATIONS:
1. Technical improvements needed
2. Content presentation suggestions
3. Equipment or environment recommendations

Format as comprehensive audio quality report.`;

  return await processAudioFile(audioPath, prompt);
}
```

### Sentiment and Emotion Analysis
```javascript
async function analyzeSentimentAndEmotion(audioPath) {
  const prompt = `Analyze the sentiment and emotional content of this audio:
1. Overall emotional tone and mood
2. Sentiment analysis (positive, negative, neutral)
3. Emotional changes throughout the audio
4. Speaker confidence and conviction levels
5. Stress or tension indicators
6. Enthusiasm and engagement levels
7. Emotional peaks and significant moments
8. Impact on listener experience

Provide detailed emotional and sentiment analysis with timestamps for key moments.`;

  return await processAudioFile(audioPath, prompt);
}
```

## Batch Audio Processing

### Multiple Audio Analysis
```javascript
class AudioProcessor {
  constructor() {
    this.ai = new GoogleGenAI({});
  }

  async processBatch(audioPaths, analysisPrompt, options = {}) {
    const { 
      maxConcurrent = 3,
      delay = 2000,
      useInline = false 
    } = options;

    const results = [];
    
    for (let i = 0; i < audioPaths.length; i += maxConcurrent) {
      const batch = audioPaths.slice(i, i + maxConcurrent);
      
      const batchPromises = batch.map(async (audioPath, index) => {
        try {
          let result;
          if (useInline && this.getFileSize(audioPath) < 20 * 1024 * 1024) {
            result = await this.processInline(audioPath, analysisPrompt);
          } else {
            result = await this.processWithFileAPI(audioPath, analysisPrompt);
          }

          return {
            audioPath,
            result,
            success: true,
            index: i + index
          };
        } catch (error) {
          return {
            audioPath,
            error: error.message,
            success: false,
            index: i + index
          };
        }
      });

      const batchResults = await Promise.all(batchPromises);
      results.push(...batchResults);

      // Rate limiting delay
      if (i + maxConcurrent < audioPaths.length) {
        await new Promise(resolve => setTimeout(resolve, delay));
      }
    }

    return results;
  }

  async processInline(audioPath, prompt) {
    return await processInlineAudio(audioPath, prompt);
  }

  async processWithFileAPI(audioPath, prompt) {
    return await processAudioFile(audioPath, prompt);
  }

  getFileSize(filePath) {
    return fs.statSync(filePath).size;
  }
}

// Usage
const processor = new AudioProcessor();
const results = await processor.processBatch([
  "./audio1.mp3",
  "./audio2.wav",
  "./audio3.flac"
], "Transcribe this audio and provide a summary of key points");
```

### Audio Comparison Analysis
```javascript
async function compareAudioFiles(audioPaths, comparisonCriteria) {
  const uploadedFiles = [];
  
  // Upload all audio files
  for (const audioPath of audioPaths) {
    const mimeType = getAudioMimeType(audioPath);
    const file = await ai.files.upload({
      file: audioPath,
      config: { mimeType }
    });
    uploadedFiles.push(file);
  }

  // Create content for comparison
  const contents = [
    { text: `Compare these audio files based on ${comparisonCriteria}. Analyze similarities, differences, and provide detailed comparison.` }
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
const comparison = await compareAudioFiles([
  "./speaker1.wav",
  "./speaker2.wav"
], "voice characteristics, speaking style, and content delivery");
```

## Token Counting and Cost Estimation

### Audio Token Calculation
```javascript
async function countAudioTokens(audioPath) {
  const file = await ai.files.upload({
    file: audioPath,
    config: { mimeType: getAudioMimeType(audioPath) }
  });

  const tokenCount = await ai.models.countTokens({
    model: "gemini-2.5-flash",
    contents: [createPartFromUri(file.uri, file.mimeType)]
  });

  return tokenCount.totalTokens;
}

function estimateAudioTokens(durationSeconds) {
  // Gemini represents each second of audio as 32 tokens
  return durationSeconds * 32;
}

function calculateAudioCost(durationSeconds, inputTokenPrice = 0.000125) {
  const tokens = estimateAudioTokens(durationSeconds);
  return {
    tokens,
    estimatedCost: tokens * inputTokenPrice,
    durationMinutes: durationSeconds / 60
  };
}

// Usage
const cost = calculateAudioCost(300); // 5-minute audio
console.log(`5-minute audio: ${cost.tokens} tokens, ~$${cost.estimatedCost.toFixed(4)}`);
```

## Error Handling and Best Practices

### Robust Audio Processing
```javascript
async function safeAudioProcessing(audioPath, prompt, options = {}) {
  const {
    maxRetries = 3,
    timeoutMs = 120000, // 2 minutes for audio processing
    fallbackToInline = false
  } = options;

  // Validate audio file
  const validation = validateAudioFile(audioPath);
  if (!validation.valid) {
    throw new Error(`Invalid audio: ${validation.error}`);
  }

  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      const fileSize = fs.statSync(audioPath).size;
      
      if (fileSize > 20 * 1024 * 1024 || !fallbackToInline) {
        return await processAudioFile(audioPath, prompt);
      } else {
        return await processInlineAudio(audioPath, prompt);
      }
    } catch (error) {
      console.error(`Audio processing attempt ${attempt} failed:`, error.message);
      
      if (attempt === maxRetries) {
        throw error;
      }
      
      // Wait before retry with exponential backoff
      await new Promise(resolve => setTimeout(resolve, 1000 * attempt));
    }
  }
}

function validateAudioFile(audioPath) {
  const supportedFormats = ['.wav', '.mp3', '.aiff', '.aac', '.ogg', '.flac'];
  const ext = path.extname(audioPath).toLowerCase();
  
  if (!supportedFormats.includes(ext)) {
    return { valid: false, error: `Unsupported format: ${ext}` };
  }

  const stats = fs.statSync(audioPath);
  const maxSize = 2 * 1024 * 1024 * 1024; // 2GB theoretical limit
  
  if (stats.size > maxSize) {
    return { valid: false, error: `File too large: ${stats.size} bytes` };
  }

  return { valid: true, size: stats.size };
}
```

### Performance Optimization
```javascript
class AudioCache {
  constructor(ttl = 1800000) { // 30 minutes default TTL
    this.cache = new Map();
    this.ttl = ttl;
  }

  generateKey(audioPath, prompt) {
    const stats = fs.statSync(audioPath);
    return `${audioPath}:${stats.mtime.getTime()}:${this.hashPrompt(prompt)}`;
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
const audioCache = new AudioCache();

async function analyzeAudioWithCache(audioPath, prompt) {
  const cacheKey = audioCache.generateKey(audioPath, prompt);
  const cached = audioCache.get(cacheKey);
  
  if (cached) {
    return cached;
  }

  const result = await safeAudioProcessing(audioPath, prompt);
  audioCache.set(cacheKey, result);
  
  return result;
}
```

## Technical Details and Limitations

### Supported Audio Formats
```javascript
const SUPPORTED_AUDIO_FORMATS = {
  'audio/wav': ['.wav'],
  'audio/mp3': ['.mp3'],
  'audio/aiff': ['.aiff'],
  'audio/aac': ['.aac'],
  'audio/ogg': ['.ogg'],
  'audio/flac': ['.flac']
};

function getSupportedAudioMimeType(filePath) {
  const ext = path.extname(filePath).toLowerCase();
  
  for (const [mimeType, extensions] of Object.entries(SUPPORTED_AUDIO_FORMATS)) {
    if (extensions.includes(ext)) {
      return mimeType;
    }
  }
  
  return null;
}
```

### Audio Processing Specifications
```javascript
const AUDIO_PROCESSING_SPECS = {
  tokenization: {
    tokensPerSecond: 32,
    maxDuration: 34200, // 9.5 hours in seconds
    downsampling: '16 Kbps',
    channelProcessing: 'Combined to single channel'
  },
  
  limitations: {
    maxFiles: 'No limit on number of files',
    maxCombinedDuration: 34200, // 9.5 hours total per prompt
    processingTime: 'Varies by duration and complexity'
  }
};

function checkAudioCompatibility(durationSeconds, totalDurationInPrompt = 0) {
  const maxDuration = AUDIO_PROCESSING_SPECS.tokenization.maxDuration;
  const newTotal = totalDurationInPrompt + durationSeconds;
  
  return {
    compatible: newTotal <= maxDuration,
    maxDuration: maxDuration,
    currentTotal: newTotal,
    remainingCapacity: maxDuration - newTotal,
    recommendation: newTotal > maxDuration 
      ? 'Total audio duration exceeds 9.5 hour limit. Consider splitting into multiple requests.'
      : 'Audio duration is within limits'
  };
}
```

## Integration Examples

### Express.js API Endpoint
```javascript
app.post('/api/analyze-audio', upload.single('audio'), async (req, res) => {
  try {
    const { prompt, analysisType = 'transcription', includeTimestamps = false } = req.body;
    const audioPath = req.file.path;

    // Validate audio
    const validation = validateAudioFile(audioPath);
    if (!validation.valid) {
      return res.status(400).json({ error: validation.error });
    }

    const analysisPrompts = {
      transcription: "Generate a complete transcript of this audio",
      summary: "Provide a comprehensive summary of this audio content",
      sentiment: "Analyze the sentiment and emotional content of this audio",
      quality: "Assess the technical and content quality of this audio",
      topics: "Identify and analyze the main topics discussed in this audio"
    };

    let finalPrompt = prompt || analysisPrompts[analysisType];
    if (includeTimestamps && !prompt) {
      finalPrompt += " with relevant timestamps";
    }

    const result = await safeAudioProcessing(audioPath, finalPrompt);

    // Clean up uploaded file
    fs.unlinkSync(audioPath);

    res.json({
      success: true,
      analysis: result,
      analysisType,
      audioInfo: {
        size: validation.size,
        format: path.extname(audioPath)
      }
    });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});
```

### Audio Management Service
```javascript
class AudioAnalysisService {
  constructor() {
    this.ai = new GoogleGenAI({});
    this.cache = new AudioCache();
    this.processor = new AudioProcessor();
  }

  async analyzeAudio(audioPath, analysisType, customPrompt = null) {
    const prompts = {
      meeting: "Analyze this meeting audio for key decisions, action items, and participant contributions",
      interview: "Analyze this interview for key insights, important responses, and main topics",
      podcast: "Analyze this podcast for main themes, key takeaways, and engaging moments",
      lecture: "Analyze this educational content for learning objectives and key concepts",
      call: "Analyze this call for customer issues, resolution process, and service quality"
    };

    const prompt = customPrompt || prompts[analysisType] || prompts.meeting;
    return await analyzeAudioWithCache(audioPath, prompt);
  }

  async transcribeAudio(audioPath, options = {}) {
    return await transcribeAudio(audioPath, options);
  }

  async analyzeQuality(audioPath) {
    return await assessAudioQuality(audioPath);
  }

  async analyzeSentiment(audioPath) {
    return await analyzeSentimentAndEmotion(audioPath);
  }

  async compareAudio(audioPaths, comparisonType = 'content') {
    const comparisonPrompts = {
      content: "Compare the content, topics, and information presented",
      quality: "Compare audio quality, clarity, and technical aspects",
      speakers: "Compare speaker characteristics, delivery style, and presentation",
      sentiment: "Compare emotional tone, sentiment, and engagement levels"
    };

    return await compareAudioFiles(audioPaths, comparisonPrompts[comparisonType]);
  }

  async generateAudioReport(audioPaths, reportType = 'comprehensive') {
    const results = await this.processor.processBatch(
      audioPaths,
      "Analyze this audio and extract key information, topics, and insights"
    );

    const reportPrompt = `Based on analysis of ${audioPaths.length} audio files, create a ${reportType} report with:
1. Executive summary of all audio content
2. Common themes and topics across files
3. Quality assessment and technical analysis
4. Key insights and recommendations
5. Comparative analysis between audio files
6. Suggested improvements or optimizations

Format as professional audio analysis report.`;

    const combinedData = results.map(r => r.result).join('\n\n---\n\n');
    
    const response = await this.ai.models.generateContent({
      model: "gemini-2.5-flash",
      contents: [{ text: `${reportPrompt}\n\nAudio Analysis Data:\n${combinedData}` }]
    });

    return response.text;
  }
}
```

## Best Practices Summary

### Audio Preparation
1. **Quality assurance**: Use clear, high-quality audio with minimal background noise
2. **Format optimization**: Use supported formats (WAV, MP3, FLAC recommended)
3. **Duration management**: Consider 9.5-hour total limit per prompt
4. **File size**: Use File API for audio files > 20MB

### Processing Strategy
1. **Method selection**: Use File API for most audio files, inline only for very small clips
2. **Batch processing**: Process multiple audio files efficiently with proper rate limiting
3. **Caching**: Implement caching for frequently analyzed audio content
4. **Error handling**: Implement robust retry logic for audio processing failures

### Prompt Engineering
1. **Timestamp requests**: Use MM:SS format when referring to specific times
2. **Specific analysis**: Be clear about what aspects to analyze (speech, music, environment)
3. **Output format**: Specify desired format (transcript, summary, structured data)
4. **Context provision**: Provide context about audio type and analysis goals

### Performance Optimization
1. **Token management**: Monitor token usage (32 tokens per second of audio)
2. **Cost estimation**: Calculate costs based on audio duration before processing
3. **Rate limiting**: Respect API limits and implement proper delays
4. **Quality assessment**: Validate audio quality before processing for better results