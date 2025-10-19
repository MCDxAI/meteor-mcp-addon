# Gemini API URL Context Guide

## Overview
The URL context tool enables Gemini models to access and analyze content from web URLs, providing additional context to enhance responses. This tool automatically retrieves content from specified URLs and incorporates it into the model's understanding, making it powerful for data extraction, document comparison, content synthesis, and code analysis.

## Key Capabilities
- **Extract Data**: Pull specific information like prices, names, or key findings from multiple URLs
- **Compare Documents**: Analyze multiple reports, articles, or PDFs to identify differences and track trends
- **Synthesize & Create Content**: Combine information from several source URLs to generate accurate summaries, blog posts, or reports
- **Analyze Code & Docs**: Point to GitHub repositories or technical documentation to explain code, generate setup instructions, or answer questions

## Basic Setup

### JavaScript/TypeScript Implementation
```javascript
import { GoogleGenAI } from "@google/genai";

const ai = new GoogleGenAI({
  apiKey: process.env.GEMINI_API_KEY
});

// Basic URL context usage
async function analyzeUrls(prompt, urls) {
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: prompt,
    config: {
      tools: [{ urlContext: {} }],
    },
  });

  return response;
}

// Usage example
const response = await analyzeUrls(
  "Compare the ingredients and cooking times from the recipes at https://www.foodnetwork.com/recipes/ina-garten/perfect-roast-chicken-recipe-1940592 and https://www.allrecipes.com/recipe/21151/simple-whole-roast-chicken/",
  []
);

console.log(response.text);
console.log(response.candidates[0].urlContextMetadata);
```

### Simple URL Analysis Example
```javascript
async function analyzeWebpage(url, question) {
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: `Based on the content at ${url}, ${question}`,
    config: {
      tools: [{ urlContext: {} }],
    },
  });

  return {
    answer: response.text,
    urlMetadata: response.candidates[0]?.urlContextMetadata,
    tokensUsed: response.usageMetadata
  };
}

// Usage
const result = await analyzeWebpage(
  "https://example.com/product-page",
  "what are the key features and pricing information?"
);
```

## How URL Context Works

### Two-Step Retrieval Process

1. **Index Cache Lookup**: The tool first attempts to fetch content from an internal index cache (optimized for speed)
2. **Live Fetch Fallback**: If content isn't available in the cache (e.g., very new pages), the tool automatically performs a live fetch to retrieve real-time content

### Automatic Content Processing
```javascript
// The model automatically processes various content types
async function processMultipleUrls(urls, task) {
  const urlList = urls.join(", ");
  
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: `${task} using content from these URLs: ${urlList}`,
    config: {
      tools: [{ urlContext: {} }],
    },
  });

  return response;
}

// Example: Analyze multiple documents
const analysis = await processMultipleUrls([
  "https://company.com/annual-report-2023.pdf",
  "https://company.com/annual-report-2024.pdf"
], "Compare the financial performance between these two annual reports");
```

## Understanding URL Context Response

### Response Structure
```javascript
const response = {
  candidates: [{
    content: {
      parts: [{
        text: "Based on the recipes from both websites..."
      }],
      role: "model"
    },
    urlContextMetadata: {
      urlMetadata: [
        {
          retrievedUrl: "https://www.foodnetwork.com/recipes/ina-garten/perfect-roast-chicken-recipe-1940592",
          urlRetrievalStatus: "URL_RETRIEVAL_STATUS_SUCCESS"
        },
        {
          retrievedUrl: "https://www.allrecipes.com/recipe/21151/simple-whole-roast-chicken/",
          urlRetrievalStatus: "URL_RETRIEVAL_STATUS_SUCCESS"
        }
      ]
    }
  }],
  usageMetadata: {
    candidatesTokenCount: 45,
    promptTokenCount: 27,
    toolUsePromptTokenCount: 10309,
    totalTokenCount: 10412
  }
};
```

### URL Retrieval Status Types
- **URL_RETRIEVAL_STATUS_SUCCESS**: Content successfully retrieved
- **URL_RETRIEVAL_STATUS_UNSAFE**: Content failed safety checks
- **URL_RETRIEVAL_STATUS_FAILED**: Technical failure in retrieval
- **URL_RETRIEVAL_STATUS_TIMEOUT**: Request timed out
- **URL_RETRIEVAL_STATUS_UNSUPPORTED**: Content type not supported

### Processing URL Metadata
```javascript
function processUrlMetadata(response) {
  const metadata = response.candidates[0]?.urlContextMetadata;
  
  if (!metadata) {
    return { success: [], failed: [], summary: "No URL metadata found" };
  }

  const success = [];
  const failed = [];

  metadata.urlMetadata.forEach(url => {
    if (url.urlRetrievalStatus === "URL_RETRIEVAL_STATUS_SUCCESS") {
      success.push(url.retrievedUrl);
    } else {
      failed.push({
        url: url.retrievedUrl,
        status: url.urlRetrievalStatus
      });
    }
  });

  return {
    success,
    failed,
    summary: `Successfully retrieved ${success.length} URLs, ${failed.length} failed`
  };
}

// Usage
const urlAnalysis = processUrlMetadata(response);
console.log(urlAnalysis.summary);
```

## Real-World Use Cases

### Document Comparison and Analysis
```javascript
async function compareDocuments(urls, comparisonCriteria) {
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: `Compare the following documents based on ${comparisonCriteria}: ${urls.join(", ")}. Provide a detailed analysis highlighting similarities, differences, and key insights.`,
    config: {
      tools: [{ urlContext: {} }],
    },
  });

  return {
    comparison: response.text,
    processedUrls: response.candidates[0]?.urlContextMetadata?.urlMetadata || [],
    tokenUsage: response.usageMetadata
  };
}

// Usage
const comparison = await compareDocuments([
  "https://company.com/q1-report.pdf",
  "https://company.com/q2-report.pdf",
  "https://company.com/q3-report.pdf"
], "revenue growth, market expansion, and operational efficiency");
```

### Product Research and Analysis
```javascript
async function analyzeProducts(productUrls) {
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: `Analyze these product pages and create a comprehensive comparison including features, pricing, pros/cons, and recommendations: ${productUrls.join(", ")}`,
    config: {
      tools: [{ urlContext: {} }],
    },
  });

  return {
    analysis: response.text,
    sources: response.candidates[0]?.urlContextMetadata?.urlMetadata || []
  };
}

// Usage
const productAnalysis = await analyzeProducts([
  "https://apple.com/iphone-15",
  "https://samsung.com/galaxy-s24",
  "https://google.com/pixel-8"
]);
```

### Code Repository Analysis
```javascript
async function analyzeCodeRepository(repoUrl, analysisType) {
  const prompts = {
    overview: `Provide an overview of the codebase at ${repoUrl}, including architecture, main technologies used, and project structure.`,
    setup: `Generate setup and installation instructions for the project at ${repoUrl}.`,
    documentation: `Create comprehensive documentation for the code at ${repoUrl}, including API references and usage examples.`,
    review: `Perform a code review of ${repoUrl}, identifying potential improvements, best practices, and security considerations.`
  };

  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: prompts[analysisType] || prompts.overview,
    config: {
      tools: [{ urlContext: {} }],
    },
  });

  return response;
}

// Usage
const codeAnalysis = await analyzeCodeRepository(
  "https://github.com/user/project",
  "review"
);
```

### Content Synthesis and Summarization
```javascript
async function synthesizeContent(urls, outputFormat) {
  const formatPrompts = {
    summary: "Create a comprehensive summary",
    blog: "Write a blog post",
    report: "Generate a detailed report",
    presentation: "Create presentation slides outline"
  };

  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: `${formatPrompts[outputFormat]} based on information from these sources: ${urls.join(", ")}. Ensure accuracy and cite key points from each source.`,
    config: {
      tools: [{ urlContext: {} }],
    },
  });

  return {
    content: response.text,
    sources: response.candidates[0]?.urlContextMetadata?.urlMetadata || [],
    format: outputFormat
  };
}

// Usage
const blogPost = await synthesizeContent([
  "https://research.com/ai-trends-2024",
  "https://techreport.com/machine-learning-advances",
  "https://industry.com/ai-adoption-survey"
], "blog");
```

## Combining URL Context with Other Tools

### URL Context + Google Search Grounding
```javascript
async function comprehensiveAnalysis(topic, specificUrls = []) {
  const urlPrompt = specificUrls.length > 0 
    ? ` Also analyze these specific URLs: ${specificUrls.join(", ")}`
    : "";

  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: `Provide a comprehensive analysis of ${topic}, including current trends, expert opinions, and recent developments.${urlPrompt}`,
    config: {
      tools: [
        { urlContext: {} },
        { googleSearch: {} }
      ],
    },
  });

  return {
    analysis: response.text,
    urlSources: response.candidates[0]?.urlContextMetadata?.urlMetadata || [],
    searchSources: response.candidates[0]?.groundingMetadata?.groundingChunks || []
  };
}

// Usage
const analysis = await comprehensiveAnalysis(
  "artificial intelligence in healthcare",
  ["https://medical-journal.com/ai-study", "https://hospital.com/ai-implementation"]
);
```

### URL Context + Function Calling
```javascript
const dataExtractionFunction = {
  name: 'extract_structured_data',
  description: 'Extract structured data from analyzed content',
  parameters: {
    type: 'object',
    properties: {
      dataType: { type: 'string', enum: ['financial', 'product', 'research', 'technical'] },
      fields: { type: 'array', items: { type: 'string' } }
    },
    required: ['dataType', 'fields']
  }
};

async function extractDataFromUrls(urls, dataType, fields) {
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: `Analyze the content from these URLs: ${urls.join(", ")} and extract ${dataType} data for the following fields: ${fields.join(", ")}`,
    config: {
      tools: [
        { urlContext: {} },
        { functionDeclarations: [dataExtractionFunction] }
      ],
    },
  });

  return response;
}
```

## Advanced URL Context Patterns

### Batch URL Processing
```javascript
class UrlProcessor {
  constructor() {
    this.ai = new GoogleGenAI({});
    this.maxUrlsPerRequest = 20;
  }

  async processBatch(urls, task, options = {}) {
    const { 
      batchSize = this.maxUrlsPerRequest,
      delay = 1000,
      retryFailed = true 
    } = options;

    const results = [];
    
    for (let i = 0; i < urls.length; i += batchSize) {
      const batch = urls.slice(i, i + batchSize);
      
      try {
        const response = await this.ai.models.generateContent({
          model: "gemini-2.5-flash",
          contents: `${task} using content from: ${batch.join(", ")}`,
          config: {
            tools: [{ urlContext: {} }],
          },
        });

        results.push({
          batch: i / batchSize + 1,
          urls: batch,
          result: response.text,
          metadata: response.candidates[0]?.urlContextMetadata,
          success: true
        });
      } catch (error) {
        results.push({
          batch: i / batchSize + 1,
          urls: batch,
          error: error.message,
          success: false
        });
      }

      // Rate limiting delay
      if (i + batchSize < urls.length) {
        await new Promise(resolve => setTimeout(resolve, delay));
      }
    }

    return results;
  }

  async retryFailedUrls(failedResults) {
    const retryResults = [];
    
    for (const failed of failedResults) {
      if (!failed.success) {
        try {
          const response = await this.ai.models.generateContent({
            model: "gemini-2.5-flash",
            contents: `Analyze content from: ${failed.urls.join(", ")}`,
            config: {
              tools: [{ urlContext: {} }],
            },
          });

          retryResults.push({
            ...failed,
            result: response.text,
            metadata: response.candidates[0]?.urlContextMetadata,
            success: true,
            retried: true
          });
        } catch (error) {
          retryResults.push({
            ...failed,
            retryError: error.message,
            retried: true
          });
        }
      }
    }

    return retryResults;
  }
}

// Usage
const processor = new UrlProcessor();
const results = await processor.processBatch(
  ["https://site1.com", "https://site2.com", "https://site3.com"],
  "Extract key information and summarize main points"
);
```

### URL Validation and Preprocessing
```javascript
class UrlValidator {
  constructor() {
    this.supportedProtocols = ['http:', 'https:'];
    this.supportedContentTypes = [
      'text/html', 'application/json', 'text/plain', 'text/xml',
      'text/css', 'text/javascript', 'text/csv', 'text/rtf',
      'image/png', 'image/jpeg', 'image/bmp', 'image/webp',
      'application/pdf'
    ];
  }

  validateUrl(url) {
    try {
      const parsedUrl = new URL(url);
      
      if (!this.supportedProtocols.includes(parsedUrl.protocol)) {
        return { valid: false, error: 'Unsupported protocol' };
      }

      // Check for common unsupported patterns
      if (parsedUrl.hostname.includes('youtube.com') || parsedUrl.hostname.includes('youtu.be')) {
        return { valid: false, error: 'YouTube videos not supported' };
      }

      if (parsedUrl.hostname.includes('docs.google.com')) {
        return { valid: false, error: 'Google Workspace files not supported' };
      }

      return { valid: true, url: parsedUrl.href };
    } catch (error) {
      return { valid: false, error: 'Invalid URL format' };
    }
  }

  async validateUrls(urls) {
    const results = {
      valid: [],
      invalid: [],
      warnings: []
    };

    urls.forEach(url => {
      const validation = this.validateUrl(url);
      
      if (validation.valid) {
        results.valid.push(validation.url);
      } else {
        results.invalid.push({ url, error: validation.error });
      }
    });

    if (results.valid.length > 20) {
      results.warnings.push(`${results.valid.length} URLs provided, but only first 20 will be processed`);
      results.valid = results.valid.slice(0, 20);
    }

    return results;
  }

  async preprocessUrls(urls) {
    const validation = await this.validateUrls(urls);
    
    return {
      processableUrls: validation.valid,
      issues: validation.invalid,
      warnings: validation.warnings,
      summary: `${validation.valid.length} valid URLs, ${validation.invalid.length} invalid URLs`
    };
  }
}

// Usage
const validator = new UrlValidator();
const preprocessed = await validator.preprocessUrls([
  "https://example.com/page1",
  "https://docs.google.com/document/123", // Will be flagged as invalid
  "https://youtube.com/watch?v=123", // Will be flagged as invalid
  "https://example.com/report.pdf"
]);
```

## Error Handling and Best Practices

### Robust URL Context Implementation
```javascript
async function safeUrlAnalysis(urls, task, options = {}) {
  const {
    maxRetries = 3,
    validateUrls = true,
    fallbackOnFailure = true,
    timeoutMs = 30000
  } = options;

  // Validate URLs if requested
  if (validateUrls) {
    const validator = new UrlValidator();
    const validation = await validator.preprocessUrls(urls);
    
    if (validation.processableUrls.length === 0) {
      throw new Error('No valid URLs to process');
    }
    
    urls = validation.processableUrls;
  }

  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

      const response = await ai.models.generateContent({
        model: "gemini-2.5-flash",
        contents: `${task} using content from: ${urls.join(", ")}`,
        config: {
          tools: [{ urlContext: {} }],
        },
      });

      clearTimeout(timeoutId);

      // Check URL retrieval success
      const metadata = response.candidates[0]?.urlContextMetadata;
      const failedUrls = metadata?.urlMetadata?.filter(
        url => url.urlRetrievalStatus !== "URL_RETRIEVAL_STATUS_SUCCESS"
      ) || [];

      return {
        response,
        success: true,
        attempt,
        failedUrls,
        successfulUrls: metadata?.urlMetadata?.filter(
          url => url.urlRetrievalStatus === "URL_RETRIEVAL_STATUS_SUCCESS"
        ) || []
      };
    } catch (error) {
      console.error(`URL analysis attempt ${attempt} failed:`, error.message);
      
      if (attempt === maxRetries) {
        if (fallbackOnFailure) {
          // Fallback to analysis without URL context
          const fallbackResponse = await ai.models.generateContent({
            model: "gemini-2.5-flash",
            contents: `${task} (Note: Unable to access provided URLs, providing general analysis)`,
          });
          
          return {
            response: fallbackResponse,
            success: false,
            isFallback: true,
            error: error.message
          };
        }
        throw error;
      }
      
      // Wait before retry
      await new Promise(resolve => setTimeout(resolve, 1000 * attempt));
    }
  }
}
```

### URL Content Size Management
```javascript
function estimateTokenUsage(urls, avgContentSize = 5000) {
  // Rough estimation: 1 token ≈ 4 characters
  const estimatedTokensPerUrl = avgContentSize / 4;
  const totalEstimatedTokens = urls.length * estimatedTokensPerUrl;
  
  return {
    estimatedTokensPerUrl,
    totalEstimatedTokens,
    estimatedCost: totalEstimatedTokens * 0.000125, // Rough cost estimate
    warning: totalEstimatedTokens > 100000 ? 'High token usage expected' : null
  };
}

async function optimizedUrlAnalysis(urls, task) {
  const estimation = estimateTokenUsage(urls);
  
  if (estimation.warning) {
    console.warn(`${estimation.warning}: ~${estimation.totalEstimatedTokens} tokens`);
  }

  // If too many tokens, process in smaller batches
  if (estimation.totalEstimatedTokens > 50000) {
    const processor = new UrlProcessor();
    return await processor.processBatch(urls, task, { batchSize: 5 });
  }

  return await safeUrlAnalysis(urls, task);
}
```

## Performance Optimization

### Caching and Rate Limiting
```javascript
class UrlContextManager {
  constructor() {
    this.cache = new Map();
    this.rateLimiter = new Map();
    this.cacheTTL = 600000; // 10 minutes
    this.minRequestInterval = 2000; // 2 seconds
  }

  generateCacheKey(urls, task) {
    const sortedUrls = [...urls].sort();
    return `${task}:${sortedUrls.join(',')}`;
  }

  async analyzeWithCache(urls, task, useCache = true) {
    const cacheKey = this.generateCacheKey(urls, task);
    
    // Check cache
    if (useCache && this.cache.has(cacheKey)) {
      const cached = this.cache.get(cacheKey);
      if (Date.now() - cached.timestamp < this.cacheTTL) {
        return { ...cached.result, fromCache: true };
      }
    }

    // Rate limiting
    const lastRequest = this.rateLimiter.get('lastRequest') || 0;
    const timeSinceLastRequest = Date.now() - lastRequest;
    
    if (timeSinceLastRequest < this.minRequestInterval) {
      await new Promise(resolve => 
        setTimeout(resolve, this.minRequestInterval - timeSinceLastRequest)
      );
    }

    // Make request
    const result = await safeUrlAnalysis(urls, task);
    
    // Update cache and rate limiter
    this.cache.set(cacheKey, {
      result,
      timestamp: Date.now()
    });
    this.rateLimiter.set('lastRequest', Date.now());

    return result;
  }

  clearCache() {
    this.cache.clear();
  }

  getCacheStats() {
    return {
      size: this.cache.size,
      entries: Array.from(this.cache.keys())
    };
  }
}

// Usage
const urlManager = new UrlContextManager();
const result = await urlManager.analyzeWithCache(
  ["https://example.com/doc1", "https://example.com/doc2"],
  "Compare and summarize key points"
);
```

### Smart URL Grouping
```javascript
function groupUrlsByDomain(urls) {
  const groups = new Map();
  
  urls.forEach(url => {
    try {
      const domain = new URL(url).hostname;
      if (!groups.has(domain)) {
        groups.set(domain, []);
      }
      groups.get(domain).push(url);
    } catch (error) {
      console.warn(`Invalid URL: ${url}`);
    }
  });

  return Array.from(groups.entries()).map(([domain, urls]) => ({
    domain,
    urls,
    count: urls.length
  }));
}

async function analyzeBySimilarContent(urls, task) {
  const groups = groupUrlsByDomain(urls);
  const results = [];

  for (const group of groups) {
    const response = await ai.models.generateContent({
      model: "gemini-2.5-flash",
      contents: `${task} for content from ${group.domain}: ${group.urls.join(", ")}`,
      config: {
        tools: [{ urlContext: {} }],
      },
    });

    results.push({
      domain: group.domain,
      urls: group.urls,
      analysis: response.text,
      metadata: response.candidates[0]?.urlContextMetadata
    });
  }

  return results;
}
```

## Supported Models and Content Types

### Model Support
| Model | URL Context Support |
|-------|-------------------|
| gemini-2.5-pro | ✅ |
| gemini-2.5-flash | ✅ |
| gemini-2.5-flash-lite | ✅ |
| gemini-live-2.5-flash-preview | ✅ |
| gemini-2.0-flash-live-001 | ✅ |

### Supported Content Types
```javascript
const SUPPORTED_CONTENT_TYPES = {
  text: [
    'text/html',
    'application/json', 
    'text/plain',
    'text/xml',
    'text/css',
    'text/javascript',
    'text/csv',
    'text/rtf'
  ],
  images: [
    'image/png',
    'image/jpeg', 
    'image/bmp',
    'image/webp'
  ],
  documents: [
    'application/pdf'
  ]
};

const UNSUPPORTED_CONTENT = [
  'Paywalled content',
  'YouTube videos',
  'Google Workspace files (Docs, Sheets, etc.)',
  'Video files (mp4, avi, etc.)',
  'Audio files (mp3, wav, etc.)'
];
```

## Integration Examples

### Express.js API Endpoint
```javascript
app.post('/api/analyze-urls', async (req, res) => {
  try {
    const { urls, task, options = {} } = req.body;
    
    if (!urls || !Array.isArray(urls) || urls.length === 0) {
      return res.status(400).json({ error: 'URLs array is required' });
    }

    if (urls.length > 20) {
      return res.status(400).json({ error: 'Maximum 20 URLs allowed per request' });
    }

    const validator = new UrlValidator();
    const validation = await validator.preprocessUrls(urls);
    
    if (validation.processableUrls.length === 0) {
      return res.status(400).json({ 
        error: 'No valid URLs to process',
        issues: validation.issues 
      });
    }

    const result = await safeUrlAnalysis(
      validation.processableUrls, 
      task || "Analyze and summarize the content",
      options
    );

    res.json({
      success: true,
      analysis: result.response.text,
      processedUrls: validation.processableUrls,
      failedUrls: result.failedUrls,
      tokenUsage: result.response.usageMetadata,
      warnings: validation.warnings
    });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});
```

### Document Analysis Service
```javascript
class DocumentAnalysisService {
  constructor() {
    this.ai = new GoogleGenAI({});
    this.urlManager = new UrlContextManager();
  }

  async analyzeDocuments(documentUrls, analysisType) {
    const analysisPrompts = {
      summary: "Provide a comprehensive summary of each document and identify key themes",
      comparison: "Compare and contrast the documents, highlighting similarities and differences",
      extraction: "Extract key data points, statistics, and important findings",
      synthesis: "Synthesize information from all documents into a cohesive analysis"
    };

    const prompt = analysisPrompts[analysisType] || analysisPrompts.summary;
    
    return await this.urlManager.analyzeWithCache(documentUrls, prompt);
  }

  async generateReport(documentUrls, reportType = 'comprehensive') {
    const analysis = await this.analyzeDocuments(documentUrls, 'synthesis');
    
    const reportPrompt = `Based on the analyzed documents, generate a ${reportType} report with:
    1. Executive Summary
    2. Key Findings
    3. Detailed Analysis
    4. Recommendations
    5. Supporting Data`;

    const response = await this.ai.models.generateContent({
      model: "gemini-2.5-flash",
      contents: `${reportPrompt}\n\nDocument sources: ${documentUrls.join(", ")}`,
      config: {
        tools: [{ urlContext: {} }],
      },
    });

    return {
      report: response.text,
      sources: documentUrls,
      metadata: response.candidates[0]?.urlContextMetadata,
      generatedAt: new Date().toISOString()
    };
  }
}

// Usage
const docService = new DocumentAnalysisService();
const report = await docService.generateReport([
  "https://company.com/annual-report.pdf",
  "https://industry.com/market-analysis.pdf"
], "executive");
```

## Best Practices Summary

### URL Selection and Preparation
1. **Use direct URLs**: Provide specific URLs to the exact content you want analyzed
2. **Verify accessibility**: Ensure URLs don't require login or are behind paywalls
3. **Complete URLs**: Always include the protocol (https://)
4. **Content type awareness**: Understand supported vs unsupported content types

### Performance Optimization
1. **Batch processing**: Group related URLs for efficient processing
2. **Caching**: Implement caching for frequently analyzed URLs
3. **Rate limiting**: Respect API limits and implement proper delays
4. **Token management**: Monitor token usage, especially with large documents

### Error Handling
1. **URL validation**: Pre-validate URLs before sending requests
2. **Retry logic**: Implement exponential backoff for failed requests
3. **Fallback strategies**: Provide alternatives when URL retrieval fails
4. **Status monitoring**: Check urlContextMetadata for retrieval status

### Integration Patterns
1. **Combine tools**: Use URL context with Google Search and function calling
2. **Structured responses**: Process urlContextMetadata for verification
3. **Batch operations**: Handle multiple URLs efficiently
4. **Quality assessment**: Evaluate retrieval success and content quality