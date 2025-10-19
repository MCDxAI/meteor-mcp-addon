# Gemini API Google Search Grounding Guide

## Overview
Grounding with Google Search connects Gemini models to real-time web content, enabling access to current information beyond the model's knowledge cutoff. This feature works with all available languages and helps build applications that provide factual, up-to-date responses with verifiable sources.

## Key Benefits
- **Increase factual accuracy**: Reduce model hallucinations by basing responses on real-world information
- **Access real-time information**: Answer questions about recent events and topics
- **Provide citations**: Build user trust by showing sources for the model's claims
- **Multi-language support**: Works with all available languages in the Gemini API

## Basic Setup

### JavaScript/TypeScript Implementation
```javascript
import { GoogleGenAI } from "@google/genai";

const ai = new GoogleGenAI({
  apiKey: process.env.GEMINI_API_KEY
});

// Define the grounding tool
const groundingTool = {
  googleSearch: {},
};

// Configure generation settings
const config = {
  tools: [groundingTool],
};

// Make a grounded request
async function groundedSearch(query) {
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: query,
    config,
  });

  return response;
}

// Usage
const response = await groundedSearch("Who won the euro 2024?");
console.log(response.text);
```

### Basic Grounding Example
```javascript
async function basicGroundingExample() {
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: "What are the latest developments in renewable energy technology?",
    config: {
      tools: [{ googleSearch: {} }],
    },
  });

  // Check if response was grounded
  if (response.candidates[0]?.groundingMetadata) {
    console.log("Response was grounded with web search");
    console.log("Search queries used:", response.candidates[0].groundingMetadata.webSearchQueries);
  }

  return response;
}
```

## How Google Search Grounding Works

### The 5-Step Process

1. **User Prompt**: Your application sends a user's prompt to the Gemini API with the google_search tool enabled
2. **Prompt Analysis**: The model analyzes the prompt and determines if a Google Search can improve the answer
3. **Google Search**: If needed, the model automatically generates one or multiple search queries and executes them
4. **Search Results Processing**: The model processes the search results, synthesizes the information, and formulates a response
5. **Grounded Response**: The API returns a final response that includes both the answer and groundingMetadata with search queries, web results, and citations

### Automatic Query Generation
```javascript
// The model automatically decides what to search for
const response = await ai.models.generateContent({
  model: "gemini-2.5-flash",
  contents: "Compare the stock prices of Apple and Microsoft today",
  config: {
    tools: [{ googleSearch: {} }],
  },
});

// Model might generate queries like:
// - "Apple stock price today"
// - "Microsoft stock price current"
// - "AAPL vs MSFT stock comparison"
```

## Understanding Grounding Response Structure

### Complete Response Format
```javascript
const response = {
  candidates: [{
    content: {
      parts: [{
        text: "Spain won Euro 2024, defeating England 2-1 in the final. This victory marks Spain's record fourth European Championship title."
      }],
      role: "model"
    },
    groundingMetadata: {
      webSearchQueries: [
        "UEFA Euro 2024 winner",
        "who won euro 2024"
      ],
      searchEntryPoint: {
        renderedContent: "<!-- HTML and CSS for the search widget -->"
      },
      groundingChunks: [
        {
          web: {
            uri: "https://vertexaisearch.cloud.google.com.....",
            title: "aljazeera.com"
          }
        },
        {
          web: {
            uri: "https://vertexaisearch.cloud.google.com.....",
            title: "uefa.com"
          }
        }
      ],
      groundingSupports: [
        {
          segment: {
            startIndex: 0,
            endIndex: 85,
            text: "Spain won Euro 2024, defeatin..."
          },
          groundingChunkIndices: [0]
        },
        {
          segment: {
            startIndex: 86,
            endIndex: 210,
            text: "This victory marks Spain's..."
          },
          groundingChunkIndices: [0, 1]
        }
      ]
    }
  }]
};
```

### Grounding Metadata Fields
- **webSearchQueries**: Array of search queries used by the model
- **searchEntryPoint**: HTML/CSS for rendering search suggestions (required by Terms of Service)
- **groundingChunks**: Array of web sources with URI and title
- **groundingSupports**: Links text segments to their sources for inline citations

## Inline Citations Implementation

### JavaScript Citation Processing
```javascript
function addCitations(response) {
  let text = response.text;
  const supports = response.candidates[0]?.groundingMetadata?.groundingSupports;
  const chunks = response.candidates[0]?.groundingMetadata?.groundingChunks;

  if (!supports || !chunks) {
    return text;
  }

  // Sort supports by end_index in descending order to avoid shifting issues
  const sortedSupports = [...supports].sort(
    (a, b) => (b.segment?.endIndex ?? 0) - (a.segment?.endIndex ?? 0)
  );

  for (const support of sortedSupports) {
    const endIndex = support.segment?.endIndex;
    
    if (endIndex === undefined || !support.groundingChunkIndices?.length) {
      continue;
    }

    // Create citation links
    const citationLinks = support.groundingChunkIndices
      .map(i => {
        const uri = chunks[i]?.web?.uri;
        const title = chunks[i]?.web?.title;
        if (uri) {
          return `[${i + 1}](${uri})`;
        }
        return null;
      })
      .filter(Boolean);

    if (citationLinks.length > 0) {
      const citationString = citationLinks.join(", ");
      text = text.slice(0, endIndex) + citationString + text.slice(endIndex);
    }
  }

  return text;
}

// Usage
async function getGroundedResponseWithCitations(query) {
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: query,
    config: {
      tools: [{ googleSearch: {} }],
    },
  });

  const textWithCitations = addCitations(response);
  return {
    originalText: response.text,
    textWithCitations,
    metadata: response.candidates[0]?.groundingMetadata
  };
}
```

### Advanced Citation Formatting
```javascript
function createRichCitations(response) {
  const supports = response.candidates[0]?.groundingMetadata?.groundingSupports;
  const chunks = response.candidates[0]?.groundingMetadata?.groundingChunks;
  
  if (!supports || !chunks) {
    return { text: response.text, sources: [] };
  }

  let text = response.text;
  const sources = [];
  const citationMap = new Map();

  // Build citation map
  supports.forEach((support, supportIndex) => {
    support.groundingChunkIndices?.forEach(chunkIndex => {
      if (chunks[chunkIndex]) {
        const source = chunks[chunkIndex].web;
        if (!citationMap.has(source.uri)) {
          const citationId = sources.length + 1;
          citationMap.set(source.uri, citationId);
          sources.push({
            id: citationId,
            title: source.title,
            uri: source.uri,
            segments: []
          });
        }
        
        const citationId = citationMap.get(source.uri);
        sources[citationId - 1].segments.push({
          text: support.segment.text,
          startIndex: support.segment.startIndex,
          endIndex: support.segment.endIndex
        });
      }
    });
  });

  // Add citations to text
  const sortedSupports = [...supports].sort(
    (a, b) => (b.segment?.endIndex ?? 0) - (a.segment?.endIndex ?? 0)
  );

  for (const support of sortedSupports) {
    const endIndex = support.segment?.endIndex;
    
    if (endIndex !== undefined && support.groundingChunkIndices?.length) {
      const citationIds = support.groundingChunkIndices
        .map(i => chunks[i]?.web?.uri)
        .filter(Boolean)
        .map(uri => citationMap.get(uri))
        .filter(Boolean);

      if (citationIds.length > 0) {
        const citationString = `[${citationIds.join(',')}]`;
        text = text.slice(0, endIndex) + citationString + text.slice(endIndex);
      }
    }
  }

  return { text, sources };
}
```

## Real-World Use Cases

### News and Current Events
```javascript
async function getCurrentNews(topic) {
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: `What are the latest news and developments about ${topic}? Provide a comprehensive summary with key details.`,
    config: {
      tools: [{ googleSearch: {} }],
    },
  });

  return addCitations(response);
}

// Usage
const newsUpdate = await getCurrentNews("artificial intelligence regulations");
```

### Market and Financial Data
```javascript
async function getMarketAnalysis(company) {
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: `Provide current stock price, recent performance, and market analysis for ${company}. Include any recent news that might affect the stock.`,
    config: {
      tools: [{ googleSearch: {} }],
    },
  });

  return {
    analysis: addCitations(response),
    searchQueries: response.candidates[0]?.groundingMetadata?.webSearchQueries,
    sources: response.candidates[0]?.groundingMetadata?.groundingChunks
  };
}
```

### Research and Fact-Checking
```javascript
async function factCheckClaim(claim) {
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: `Fact-check this claim and provide evidence from reliable sources: "${claim}". Explain whether the claim is true, false, or partially true with supporting evidence.`,
    config: {
      tools: [{ googleSearch: {} }],
    },
  });

  const result = addCitations(response);
  const sources = response.candidates[0]?.groundingMetadata?.groundingChunks || [];
  
  return {
    factCheck: result,
    sources: sources.map(chunk => ({
      title: chunk.web.title,
      url: chunk.web.uri
    })),
    searchQueries: response.candidates[0]?.groundingMetadata?.webSearchQueries
  };
}
```

### Product and Service Information
```javascript
async function getProductInfo(productName) {
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: `Provide current information about ${productName}, including pricing, availability, reviews, and recent updates or announcements.`,
    config: {
      tools: [{ googleSearch: {} }],
    },
  });

  return createRichCitations(response);
}
```

## Combining with Other Tools

### Grounding + URL Context
```javascript
async function groundedAnalysisWithUrls(query, urls) {
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: query,
    config: {
      tools: [
        { googleSearch: {} },
        { urlContext: { urls } }
      ],
    },
  });

  return response;
}

// Usage
const analysis = await groundedAnalysisWithUrls(
  "Compare the latest iPhone features with Samsung Galaxy",
  ["https://apple.com/iphone", "https://samsung.com/galaxy"]
);
```

### Grounding + Function Calling
```javascript
const weatherFunction = {
  name: 'get_weather',
  description: 'Get current weather for a location',
  parameters: {
    type: 'object',
    properties: {
      location: { type: 'string' }
    },
    required: ['location']
  }
};

async function groundedWeatherAnalysis(query) {
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: query,
    config: {
      tools: [
        { googleSearch: {} },
        { functionDeclarations: [weatherFunction] }
      ],
    },
  });

  return response;
}
```

## Legacy Gemini 1.5 Support

### Dynamic Retrieval Configuration
```javascript
// For Gemini 1.5 models (legacy approach)
const retrievalTool = {
  googleSearchRetrieval: {
    dynamicRetrievalConfig: {
      mode: "MODE_DYNAMIC",
      dynamicThreshold: 0.7, // Only search if confidence > 70%
    },
  },
};

async function legacyGrounding(query) {
  const response = await ai.models.generateContent({
    model: "gemini-1.5-flash",
    contents: query,
    config: {
      tools: [retrievalTool],
    },
  });

  if (!response.candidates[0]?.groundingMetadata) {
    console.log("Model answered from its own knowledge.");
  }

  return response;
}
```

### Dynamic Threshold Strategies
```javascript
// Conservative approach - only search for very recent topics
const conservativeConfig = {
  googleSearchRetrieval: {
    dynamicRetrievalConfig: {
      mode: "MODE_DYNAMIC",
      dynamicThreshold: 0.9
    }
  }
};

// Balanced approach - search for moderately recent topics
const balancedConfig = {
  googleSearchRetrieval: {
    dynamicRetrievalConfig: {
      mode: "MODE_DYNAMIC", 
      dynamicThreshold: 0.7
    }
  }
};

// Aggressive approach - search for most queries
const aggressiveConfig = {
  googleSearchRetrieval: {
    dynamicRetrievalConfig: {
      mode: "MODE_DYNAMIC",
      dynamicThreshold: 0.3
    }
  }
};
```

## Error Handling and Best Practices

### Robust Grounding Implementation
```javascript
async function safeGroundedSearch(query, options = {}) {
  const {
    maxRetries = 3,
    fallbackToKnowledge = true,
    requireGrounding = false
  } = options;

  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      const response = await ai.models.generateContent({
        model: "gemini-2.5-flash",
        contents: query,
        config: {
          tools: [{ googleSearch: {} }],
        },
      });

      const isGrounded = response.candidates[0]?.groundingMetadata;
      
      if (requireGrounding && !isGrounded) {
        throw new Error("Response was not grounded with search results");
      }

      return {
        response,
        isGrounded,
        searchQueries: isGrounded ? response.candidates[0].groundingMetadata.webSearchQueries : [],
        attempt
      };
    } catch (error) {
      console.error(`Grounding attempt ${attempt} failed:`, error.message);
      
      if (attempt === maxRetries) {
        if (fallbackToKnowledge) {
          // Fallback to non-grounded response
          const fallbackResponse = await ai.models.generateContent({
            model: "gemini-2.5-flash",
            contents: query,
          });
          
          return {
            response: fallbackResponse,
            isGrounded: false,
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

### Grounding Quality Assessment
```javascript
function assessGroundingQuality(response) {
  const metadata = response.candidates[0]?.groundingMetadata;
  
  if (!metadata) {
    return { quality: 'none', score: 0, issues: ['No grounding metadata'] };
  }

  const issues = [];
  let score = 100;

  // Check for search queries
  if (!metadata.webSearchQueries || metadata.webSearchQueries.length === 0) {
    issues.push('No search queries found');
    score -= 30;
  }

  // Check for sources
  if (!metadata.groundingChunks || metadata.groundingChunks.length === 0) {
    issues.push('No grounding sources found');
    score -= 40;
  }

  // Check for citation support
  if (!metadata.groundingSupports || metadata.groundingSupports.length === 0) {
    issues.push('No grounding supports for citations');
    score -= 20;
  }

  // Assess source diversity
  const uniqueDomains = new Set(
    metadata.groundingChunks?.map(chunk => 
      new URL(chunk.web.uri).hostname
    ) || []
  );
  
  if (uniqueDomains.size < 2) {
    issues.push('Limited source diversity');
    score -= 10;
  }

  let quality = 'excellent';
  if (score < 90) quality = 'good';
  if (score < 70) quality = 'fair';
  if (score < 50) quality = 'poor';

  return {
    quality,
    score,
    issues,
    sourceCount: metadata.groundingChunks?.length || 0,
    queryCount: metadata.webSearchQueries?.length || 0,
    uniqueDomains: uniqueDomains.size
  };
}
```

## Performance Optimization

### Query Optimization Strategies
```javascript
// Optimize queries for better grounding results
function optimizeQueryForGrounding(userQuery) {
  const optimizations = {
    // Add temporal context for recent events
    addTimeContext: (query) => {
      const timeKeywords = ['latest', 'recent', 'current', 'today', 'now'];
      if (!timeKeywords.some(keyword => query.toLowerCase().includes(keyword))) {
        return `${query} (latest information)`;
      }
      return query;
    },

    // Make queries more specific
    addSpecificity: (query) => {
      if (query.length < 20) {
        return `${query} - provide detailed current information`;
      }
      return query;
    },

    // Add source quality hints
    addQualityHints: (query) => {
      return `${query}. Please use reliable and authoritative sources.`;
    }
  };

  let optimizedQuery = userQuery;
  optimizedQuery = optimizations.addTimeContext(optimizedQuery);
  optimizedQuery = optimizations.addSpecificity(optimizedQuery);
  optimizedQuery = optimizations.addQualityHints(optimizedQuery);

  return optimizedQuery;
}

async function optimizedGrounding(userQuery) {
  const optimizedQuery = optimizeQueryForGrounding(userQuery);
  
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: optimizedQuery,
    config: {
      tools: [{ googleSearch: {} }],
    },
  });

  return response;
}
```

### Caching and Rate Limiting
```javascript
class GroundingManager {
  constructor() {
    this.cache = new Map();
    this.rateLimiter = new Map();
    this.cacheTTL = 300000; // 5 minutes
    this.minRequestInterval = 1000; // 1 second
  }

  async groundedSearch(query, useCache = true) {
    const cacheKey = this.normalizeQuery(query);
    
    // Check cache
    if (useCache && this.cache.has(cacheKey)) {
      const cached = this.cache.get(cacheKey);
      if (Date.now() - cached.timestamp < this.cacheTTL) {
        return { ...cached.response, fromCache: true };
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
    const response = await ai.models.generateContent({
      model: "gemini-2.5-flash",
      contents: query,
      config: {
        tools: [{ googleSearch: {} }],
      },
    });

    // Update cache and rate limiter
    this.cache.set(cacheKey, {
      response,
      timestamp: Date.now()
    });
    this.rateLimiter.set('lastRequest', Date.now());

    return response;
  }

  normalizeQuery(query) {
    return query.toLowerCase().trim().replace(/\s+/g, ' ');
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
const groundingManager = new GroundingManager();
const response = await groundingManager.groundedSearch("latest AI developments");
```

## Supported Models and Capabilities

| Model | Google Search Grounding | Legacy google_search_retrieval |
|-------|------------------------|--------------------------------|
| Gemini 2.5 Pro | ✅ | ❌ |
| Gemini 2.5 Flash | ✅ | ❌ |
| Gemini 2.5 Flash-Lite | ✅ | ❌ |
| Gemini 2.0 Flash | ✅ | ❌ |
| Gemini 1.5 Pro | ✅ | ✅ |
| Gemini 1.5 Flash | ✅ | ✅ |

## Pricing and Billing

### Billing Model
- **Per-request billing**: Charged per API request that includes the google_search tool
- **Multiple queries**: If the model executes multiple search queries within a single request, it counts as one billable use
- **No additional search fees**: The search functionality is included in the tool usage fee

### Cost Optimization
```javascript
// Batch related queries to minimize API calls
async function batchGroundedQueries(queries) {
  const combinedQuery = `Please answer the following questions using current information:
${queries.map((q, i) => `${i + 1}. ${q}`).join('\n')}

Provide comprehensive answers with proper citations for each question.`;

  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: combinedQuery,
    config: {
      tools: [{ googleSearch: {} }],
    },
  });

  return response;
}
```

## Integration Examples

### Express.js API Endpoint
```javascript
app.post('/api/grounded-search', async (req, res) => {
  try {
    const { query, requireCitations = false } = req.body;
    
    if (!query) {
      return res.status(400).json({ error: 'Query is required' });
    }

    const response = await ai.models.generateContent({
      model: "gemini-2.5-flash",
      contents: query,
      config: {
        tools: [{ googleSearch: {} }],
      },
    });

    const isGrounded = response.candidates[0]?.groundingMetadata;
    let result = {
      answer: response.text,
      isGrounded,
      searchQueries: isGrounded ? response.candidates[0].groundingMetadata.webSearchQueries : []
    };

    if (requireCitations && isGrounded) {
      result.answerWithCitations = addCitations(response);
      result.sources = response.candidates[0].groundingMetadata.groundingChunks.map(chunk => ({
        title: chunk.web.title,
        url: chunk.web.uri
      }));
    }

    res.json(result);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});
```

### Real-time News Dashboard
```javascript
class NewsGroundingService {
  constructor() {
    this.ai = new GoogleGenAI({});
  }

  async getTopicUpdates(topics) {
    const results = await Promise.all(
      topics.map(async (topic) => {
        try {
          const response = await this.ai.models.generateContent({
            model: "gemini-2.5-flash",
            contents: `What are the latest developments and news about ${topic}? Provide a concise summary with key points.`,
            config: {
              tools: [{ googleSearch: {} }],
            },
          });

          return {
            topic,
            summary: response.text,
            sources: response.candidates[0]?.groundingMetadata?.groundingChunks || [],
            searchQueries: response.candidates[0]?.groundingMetadata?.webSearchQueries || [],
            timestamp: new Date().toISOString()
          };
        } catch (error) {
          return {
            topic,
            error: error.message,
            timestamp: new Date().toISOString()
          };
        }
      })
    );

    return results;
  }

  async getBreakingNews(category = 'general') {
    const response = await this.ai.models.generateContent({
      model: "gemini-2.5-flash",
      contents: `What are the most important breaking news stories in ${category} today? Provide a summary of the top 5 stories with key details.`,
      config: {
        tools: [{ googleSearch: {} }],
      },
    });

    return {
      news: addCitations(response),
      sources: response.candidates[0]?.groundingMetadata?.groundingChunks || [],
      category,
      timestamp: new Date().toISOString()
    };
  }
}
```

## Best Practices Summary

### Query Design
1. **Be specific**: Include relevant context and timeframes in queries
2. **Request current information**: Use words like "latest", "current", "recent"
3. **Specify source quality**: Ask for reliable and authoritative sources
4. **Combine related questions**: Batch similar queries to reduce API calls

### Citation Handling
1. **Always process citations**: Use the groundingSupports data for inline citations
2. **Preserve source information**: Store and display source titles and URLs
3. **Handle missing citations**: Gracefully handle responses without grounding metadata
4. **Validate citation quality**: Assess source diversity and reliability

### Performance
1. **Implement caching**: Cache responses for frequently asked questions
2. **Use rate limiting**: Respect API limits and implement proper delays
3. **Optimize queries**: Pre-process user queries for better grounding results
4. **Monitor quality**: Track grounding success rates and source quality

### Error Handling
1. **Implement fallbacks**: Provide non-grounded responses when grounding fails
2. **Retry logic**: Implement exponential backoff for failed requests
3. **Quality assessment**: Evaluate grounding quality and inform users
4. **Graceful degradation**: Handle partial failures and missing metadata