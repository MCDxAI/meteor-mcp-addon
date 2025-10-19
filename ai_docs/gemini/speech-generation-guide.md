# Gemini API Speech Generation (TTS) Guide

## Overview
The Gemini API provides native text-to-speech (TTS) capabilities that can transform text into single-speaker or multi-speaker audio. This guide focuses on `gemini-2.5-flash-preview-tts` for optimal cost-effectiveness and performance in JavaScript/TypeScript applications.

## Why Use gemini-2.5-flash-preview-tts
- **Cost-effective**: Free tier with generous limits
- **Fast performance**: Optimized for speed
- **Lower token usage**: More efficient than Pro model
- **Same features**: Full TTS capabilities including multi-speaker support

## Basic Setup

### Installation and Dependencies
```bash
npm install @google/genai wav
```

### Basic Configuration
```javascript
import { GoogleGenAI } from '@google/genai';
import wav from 'wav';

const ai = new GoogleGenAI({
  apiKey: process.env.GEMINI_API_KEY
});
```

## Single-Speaker Text-to-Speech

### Basic Single-Speaker Generation
```javascript
async function generateSingleSpeakerAudio(text, voiceName = 'Kore') {
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash-preview-tts",
    contents: [{ parts: [{ text }] }],
    config: {
      responseModalities: ['AUDIO'],
      speechConfig: {
        voiceConfig: {
          prebuiltVoiceConfig: {
            voiceName: voiceName
          },
        },
      },
    },
  });

  return response.candidates?.[0]?.content?.parts?.[0]?.inlineData?.data;
}

// Usage
const audioData = await generateSingleSpeakerAudio(
  "Say cheerfully: Have a wonderful day!"
);
```

### Saving Audio to File
```javascript
async function saveWaveFile(filename, pcmData, channels = 1, rate = 24000, sampleWidth = 2) {
  return new Promise((resolve, reject) => {
    const writer = new wav.FileWriter(filename, {
      channels,
      sampleRate: rate,
      bitDepth: sampleWidth * 8,
    });
    
    writer.on('finish', resolve);
    writer.on('error', reject);
    writer.write(pcmData);
    writer.end();
  });
}

async function textToSpeechFile(text, filename = 'output.wav', voiceName = 'Kore') {
  try {
    const audioData = await generateSingleSpeakerAudio(text, voiceName);
    const audioBuffer = Buffer.from(audioData, 'base64');
    await saveWaveFile(filename, audioBuffer);
    console.log(`Audio saved to ${filename}`);
  } catch (error) {
    console.error('TTS generation failed:', error);
  }
}
```

## Multi-Speaker Text-to-Speech

### Basic Multi-Speaker Setup
```javascript
async function generateMultiSpeakerAudio(text, speakers) {
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash-preview-tts",
    contents: [{ parts: [{ text }] }],
    config: {
      responseModalities: ['AUDIO'],
      speechConfig: {
        multiSpeakerVoiceConfig: {
          speakerVoiceConfigs: speakers.map(speaker => ({
            speaker: speaker.name,
            voiceConfig: {
              prebuiltVoiceConfig: {
                voiceName: speaker.voice
              }
            }
          }))
        }
      }
    }
  });

  return response.candidates?.[0]?.content?.parts?.[0]?.inlineData?.data;
}

// Usage
const conversationText = `TTS the following conversation between Joe and Jane:
Joe: How's it going today Jane?
Jane: Not too bad, how about you?`;

const speakers = [
  { name: 'Joe', voice: 'Kore' },
  { name: 'Jane', voice: 'Puck' }
];

const audioData = await generateMultiSpeakerAudio(conversationText, speakers);
```

### Advanced Multi-Speaker Example
```javascript
async function createPodcastSegment(hosts, topic) {
  // First generate transcript
  const transcriptPrompt = `Generate a short transcript around 100 words that reads like it was clipped from a podcast about ${topic}. The hosts names are ${hosts.map(h => h.name).join(' and ')}.`;
  
  const transcriptResponse = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: transcriptPrompt,
  });
  
  const transcript = transcriptResponse.text;
  
  // Then convert to audio
  const audioData = await generateMultiSpeakerAudio(transcript, hosts);
  
  return { transcript, audioData };
}

// Usage
const podcastHosts = [
  { name: 'Dr. Anya', voice: 'Kore' },
  { name: 'Liam', voice: 'Puck' }
];

const { transcript, audioData } = await createPodcastSegment(
  podcastHosts, 
  "exciting discoveries in herpetology"
);
```

## Voice Control and Styling

### Available Voice Options (30 total)
```javascript
const VOICE_OPTIONS = {
  // Bright & Energetic
  bright: ['Zephyr', 'Autonoe', 'Leda'],
  
  // Firm & Authoritative  
  firm: ['Kore', 'Orus', 'Alnilam'],
  
  // Upbeat & Excitable
  upbeat: ['Puck', 'Fenrir', 'Laomedeia'],
  
  // Smooth & Easy-going
  smooth: ['Algieba', 'Despina', 'Callirrhoe', 'Umbriel'],
  
  // Clear & Informative
  clear: ['Charon', 'Iapetus', 'Erinome', 'Rasalgethi'],
  
  // Breathy & Soft
  breathy: ['Enceladus', 'Achernar'],
  
  // Mature & Knowledgeable
  mature: ['Gacrux', 'Sadaltager'],
  
  // Friendly & Warm
  friendly: ['Achird', 'Sulafat', 'Vindemiatrix']
};

// Voice selection helper
function selectVoiceForMood(mood) {
  const voiceMap = {
    excited: 'Puck',
    professional: 'Kore', 
    friendly: 'Achird',
    calm: 'Enceladus',
    authoritative: 'Orus',
    warm: 'Sulafat'
  };
  
  return voiceMap[mood] || 'Kore';
}
```

### Style Control with Prompts
```javascript
async function generateStyledSpeech(text, style, voice = 'Kore') {
  const styledPrompts = {
    whisper: `Say in a spooky whisper: "${text}"`,
    excited: `Say with excitement and energy: "${text}"`,
    sad: `Say in a melancholy, sad tone: "${text}"`,
    angry: `Say with frustration and anger: "${text}"`,
    cheerful: `Say cheerfully: "${text}"`,
    mysterious: `Say mysteriously and dramatically: "${text}"`,
    professional: `Say in a professional, business tone: "${text}"`,
    casual: `Say casually and relaxed: "${text}"`
  };

  const styledText = styledPrompts[style] || text;
  return await generateSingleSpeakerAudio(styledText, voice);
}

// Usage
const audioData = await generateStyledSpeech(
  "Welcome to our podcast", 
  "excited", 
  "Puck"
);
```

### Multi-Speaker Style Control
```javascript
async function generateStyledConversation(speakers, dialogue) {
  const styledPrompt = `${speakers.map(s => s.instruction).join(', ')}: 
${dialogue}`;

  const speakerConfigs = speakers.map(speaker => ({
    name: speaker.name,
    voice: speaker.voice
  }));

  return await generateMultiSpeakerAudio(styledPrompt, speakerConfigs);
}

// Usage
const styledSpeakers = [
  { 
    name: 'Speaker1', 
    voice: 'Enceladus', 
    instruction: 'Make Speaker1 sound tired and bored' 
  },
  { 
    name: 'Speaker2', 
    voice: 'Puck', 
    instruction: 'Make Speaker2 sound excited and happy' 
  }
];

const dialogue = `Speaker1: So... what's on the agenda today?
Speaker2: You're never going to guess!`;

const audioData = await generateStyledConversation(styledSpeakers, dialogue);
```

## Language Support

### Supported Languages (24 total)
```javascript
const SUPPORTED_LANGUAGES = {
  'ar-EG': 'Arabic (Egyptian)',
  'en-US': 'English (US)',
  'en-IN': 'English (India)',
  'es-US': 'Spanish (US)', 
  'fr-FR': 'French (France)',
  'de-DE': 'German (Germany)',
  'hi-IN': 'Hindi (India)',
  'id-ID': 'Indonesian (Indonesia)',
  'it-IT': 'Italian (Italy)',
  'ja-JP': 'Japanese (Japan)',
  'ko-KR': 'Korean (Korea)',
  'pt-BR': 'Portuguese (Brazil)',
  'ru-RU': 'Russian (Russia)',
  'nl-NL': 'Dutch (Netherlands)',
  'pl-PL': 'Polish (Poland)',
  'th-TH': 'Thai (Thailand)',
  'tr-TR': 'Turkish (Turkey)',
  'vi-VN': 'Vietnamese (Vietnam)',
  'ro-RO': 'Romanian (Romania)',
  'uk-UA': 'Ukrainian (Ukraine)',
  'bn-BD': 'Bengali (Bangladesh)',
  'mr-IN': 'Marathi (India)',
  'ta-IN': 'Tamil (India)',
  'te-IN': 'Telugu (India)'
};

// Language detection is automatic - no need to specify
```

## Advanced Use Cases

### Audiobook Generation
```javascript
async function generateAudiobook(chapters, narratorVoice = 'Kore') {
  const audioChapters = [];
  
  for (let i = 0; i < chapters.length; i++) {
    const chapter = chapters[i];
    console.log(`Generating chapter ${i + 1}: ${chapter.title}`);
    
    const chapterText = `Chapter ${i + 1}: ${chapter.title}. ${chapter.content}`;
    const audioData = await generateSingleSpeakerAudio(chapterText, narratorVoice);
    
    audioChapters.push({
      title: chapter.title,
      audioData,
      filename: `chapter_${i + 1}.wav`
    });
    
    // Save individual chapter
    const audioBuffer = Buffer.from(audioData, 'base64');
    await saveWaveFile(`chapter_${i + 1}.wav`, audioBuffer);
    
    // Rate limiting delay
    await new Promise(resolve => setTimeout(resolve, 1000));
  }
  
  return audioChapters;
}
```

### Interactive Dialogue Generation
```javascript
async function generateInteractiveDialogue(scenario, characters) {
  // Generate dialogue script
  const scriptPrompt = `Create a ${scenario} dialogue between ${characters.map(c => c.name).join(' and ')}. Make it engaging and natural, about 200 words.`;
  
  const scriptResponse = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: scriptPrompt,
  });
  
  // Convert to audio with character voices
  const audioData = await generateMultiSpeakerAudio(
    scriptResponse.text, 
    characters
  );
  
  return {
    script: scriptResponse.text,
    audioData
  };
}

// Usage
const characters = [
  { name: 'Detective', voice: 'Kore' },
  { name: 'Witness', voice: 'Puck' }
];

const dialogue = await generateInteractiveDialogue(
  "crime investigation interview", 
  characters
);
```

### Batch Audio Generation
```javascript
async function batchGenerateAudio(textList, options = {}) {
  const {
    voice = 'Kore',
    outputDir = './audio_output',
    delay = 1000
  } = options;
  
  const results = [];
  
  for (let i = 0; i < textList.length; i++) {
    try {
      const text = textList[i];
      const audioData = await generateSingleSpeakerAudio(text, voice);
      const filename = `audio_${i + 1}.wav`;
      const filepath = `${outputDir}/${filename}`;
      
      const audioBuffer = Buffer.from(audioData, 'base64');
      await saveWaveFile(filepath, audioBuffer);
      
      results.push({
        index: i,
        text,
        filename,
        success: true
      });
      
      console.log(`Generated: ${filename}`);
      
      // Rate limiting
      if (i < textList.length - 1) {
        await new Promise(resolve => setTimeout(resolve, delay));
      }
    } catch (error) {
      results.push({
        index: i,
        text: textList[i],
        error: error.message,
        success: false
      });
    }
  }
  
  return results;
}
```

## Error Handling and Best Practices

### Robust Error Handling
```javascript
async function safeTTSGeneration(text, options = {}) {
  const {
    voice = 'Kore',
    maxRetries = 3,
    retryDelay = 2000
  } = options;
  
  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      const audioData = await generateSingleSpeakerAudio(text, voice);
      return audioData;
    } catch (error) {
      console.error(`TTS attempt ${attempt} failed:`, error.message);
      
      if (error.message.includes('quota')) {
        console.log('Rate limit hit, waiting longer...');
        await new Promise(resolve => setTimeout(resolve, retryDelay * attempt));
      } else if (error.message.includes('safety')) {
        console.log('Content filtered, cannot retry');
        throw error;
      } else if (attempt === maxRetries) {
        throw error;
      } else {
        await new Promise(resolve => setTimeout(resolve, retryDelay));
      }
    }
  }
}
```

### Content Optimization
```javascript
function optimizeTextForTTS(text) {
  return text
    // Handle abbreviations
    .replace(/Dr\./g, 'Doctor')
    .replace(/Mr\./g, 'Mister')
    .replace(/Mrs\./g, 'Missus')
    .replace(/Ms\./g, 'Miss')
    
    // Handle numbers
    .replace(/\b(\d+)%/g, '$1 percent')
    .replace(/\$(\d+)/g, '$1 dollars')
    
    // Handle common symbols
    .replace(/&/g, 'and')
    .replace(/@/g, 'at')
    
    // Clean up extra whitespace
    .replace(/\s+/g, ' ')
    .trim();
}

// Usage
const optimizedText = optimizeTextForTTS("Dr. Smith said it's 95% effective & costs $50.");
const audioData = await generateSingleSpeakerAudio(optimizedText);
```

## Performance Tips

### Token Usage Optimization
- **Context limit**: 32k tokens per session
- **Use gemini-2.5-flash-preview-tts**: More efficient than Pro
- **Batch related content**: Combine short texts when possible
- **Optimize text length**: Aim for 100-500 words per request

### Rate Limiting Best Practices
```javascript
class TTSManager {
  constructor(delayMs = 1000) {
    this.delay = delayMs;
    this.lastRequest = 0;
  }
  
  async generate(text, options = {}) {
    const now = Date.now();
    const timeSinceLastRequest = now - this.lastRequest;
    
    if (timeSinceLastRequest < this.delay) {
      await new Promise(resolve => 
        setTimeout(resolve, this.delay - timeSinceLastRequest)
      );
    }
    
    this.lastRequest = Date.now();
    return await safeTTSGeneration(text, options);
  }
}

// Usage
const ttsManager = new TTSManager(1500); // 1.5 second delay
const audioData = await ttsManager.generate("Hello world!");
```

## Integration Examples

### Express.js API Endpoint
```javascript
app.post('/api/tts', async (req, res) => {
  try {
    const { text, voice = 'Kore', style } = req.body;
    
    if (!text) {
      return res.status(400).json({ error: 'Text is required' });
    }
    
    const optimizedText = optimizeTextForTTS(text);
    const styledText = style ? 
      `Say in a ${style} tone: "${optimizedText}"` : 
      optimizedText;
    
    const audioData = await generateSingleSpeakerAudio(styledText, voice);
    
    res.set({
      'Content-Type': 'audio/wav',
      'Content-Disposition': 'attachment; filename="speech.wav"'
    });
    
    const audioBuffer = Buffer.from(audioData, 'base64');
    res.send(audioBuffer);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});
```

### Real-time Streaming (Conceptual)
```javascript
async function streamTTS(textStream, voice = 'Kore') {
  const audioChunks = [];
  
  for await (const textChunk of textStream) {
    if (textChunk.trim()) {
      const audioData = await generateSingleSpeakerAudio(textChunk, voice);
      audioChunks.push(audioData);
      
      // Emit audio chunk to client
      emit('audioChunk', audioData);
    }
  }
  
  return audioChunks;
}
```