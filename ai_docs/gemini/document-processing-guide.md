# Gemini API Document Processing Guide

## Overview
Gemini models can process PDF documents using native vision capabilities to understand entire document contexts. This goes beyond simple text extraction, enabling comprehensive analysis of text, images, diagrams, charts, and tables in documents up to 1000 pages long.

## Key Capabilities
- **Analyze and interpret content**: Process text, images, diagrams, charts, and tables in long documents
- **Extract structured information**: Convert document content into structured output formats
- **Summarize and answer questions**: Based on both visual and textual elements
- **Transcribe document content**: Preserve layouts and formatting for downstream applications
- **Multi-document analysis**: Compare and analyze multiple PDFs simultaneously

## Basic Setup

### JavaScript/TypeScript Implementation
```javascript
import { GoogleGenAI } from "@google/genai";

const ai = new GoogleGenAI({
  apiKey: process.env.GEMINI_API_KEY
});

// Basic document processing
async function processDocument(documentPath, prompt) {
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: [
      {
        inlineData: {
          mimeType: 'application/pdf',
          data: Buffer.from(fs.readFileSync(documentPath)).toString("base64")
        }
      },
      { text: prompt }
    ]
  });

  return response.text;
}

// Usage
const summary = await processDocument("./report.pdf", "Summarize this document");
```

## Document Input Methods

### Inline PDF Data (< 20MB)
```javascript
import * as fs from 'fs';

async function processInlinePDF(filePath, prompt) {
  const pdfBuffer = fs.readFileSync(filePath);
  
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: [
      {
        inlineData: {
          mimeType: 'application/pdf',
          data: pdfBuffer.toString("base64")
        }
      },
      { text: prompt }
    ]
  });

  return response.text;
}

// Usage
const analysis = await processInlinePDF("./contract.pdf", "Extract key terms and conditions");
```

### PDF from URL
```javascript
async function processPDFFromURL(url, prompt) {
  const response = await fetch(url);
  const pdfBuffer = await response.arrayBuffer();
  
  const result = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: [
      {
        inlineData: {
          mimeType: 'application/pdf',
          data: Buffer.from(pdfBuffer).toString("base64")
        }
      },
      { text: prompt }
    ]
  });

  return result.text;
}

// Usage
const summary = await processPDFFromURL(
  "https://example.com/research-paper.pdf",
  "Summarize the methodology and key findings"
);
```

### File API for Large Documents (> 20MB)
```javascript
import { createPartFromUri } from "@google/genai";

async function processLargePDF(filePath, prompt) {
  // Upload the PDF file
  const file = await ai.files.upload({
    file: filePath,
    config: { mimeType: 'application/pdf' }
  });

  // Wait for processing
  let uploadedFile = await ai.files.get({ name: file.name });
  while (uploadedFile.state === 'PROCESSING') {
    console.log('File is processing...');
    await new Promise(resolve => setTimeout(resolve, 5000));
    uploadedFile = await ai.files.get({ name: file.name });
  }

  if (uploadedFile.state === 'FAILED') {
    throw new Error('File processing failed');
  }

  // Generate content
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
const analysis = await processLargePDF("./large-report.pdf", "Create an executive summary");
```

### File API from URL
```javascript
async function uploadPDFFromURL(url, displayName) {
  const response = await fetch(url);
  const pdfBuffer = await response.arrayBuffer();
  const fileBlob = new Blob([pdfBuffer], { type: 'application/pdf' });

  const file = await ai.files.upload({
    file: fileBlob,
    config: { displayName }
  });

  // Wait for processing
  let uploadedFile = await ai.files.get({ name: file.name });
  while (uploadedFile.state === 'PROCESSING') {
    await new Promise(resolve => setTimeout(resolve, 5000));
    uploadedFile = await ai.files.get({ name: file.name });
  }

  return uploadedFile;
}

async function processRemotePDF(url, prompt, displayName = 'Remote PDF') {
  const file = await uploadPDFFromURL(url, displayName);
  
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

## Multi-Document Processing

### Compare Multiple Documents
```javascript
async function compareDocuments(documentUrls, comparisonPrompt) {
  const uploadedFiles = [];
  
  // Upload all documents
  for (let i = 0; i < documentUrls.length; i++) {
    const file = await uploadPDFFromURL(documentUrls[i], `Document ${i + 1}`);
    uploadedFiles.push(file);
  }

  // Create content parts
  const contentParts = [{ text: comparisonPrompt }];
  uploadedFiles.forEach(file => {
    contentParts.push(createPartFromUri(file.uri, file.mimeType));
  });

  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: contentParts
  });

  return response.text;
}

// Usage
const comparison = await compareDocuments([
  "https://example.com/report-2023.pdf",
  "https://example.com/report-2024.pdf"
], "Compare the financial performance between these two annual reports and highlight key changes");
```

### Batch Document Analysis
```javascript
class DocumentProcessor {
  constructor() {
    this.ai = new GoogleGenAI({});
  }

  async processBatch(documents, analysisPrompt, options = {}) {
    const { 
      maxConcurrent = 3,
      delay = 1000,
      retryFailed = true 
    } = options;

    const results = [];
    
    for (let i = 0; i < documents.length; i += maxConcurrent) {
      const batch = documents.slice(i, i + maxConcurrent);
      
      const batchPromises = batch.map(async (doc, index) => {
        try {
          let result;
          if (doc.url) {
            result = await this.processRemotePDF(doc.url, analysisPrompt);
          } else if (doc.path) {
            result = await this.processLocalPDF(doc.path, analysisPrompt);
          } else {
            throw new Error('Document must have either url or path');
          }

          return {
            document: doc,
            result,
            success: true,
            index: i + index
          };
        } catch (error) {
          return {
            document: doc,
            error: error.message,
            success: false,
            index: i + index
          };
        }
      });

      const batchResults = await Promise.all(batchPromises);
      results.push(...batchResults);

      // Rate limiting delay
      if (i + maxConcurrent < documents.length) {
        await new Promise(resolve => setTimeout(resolve, delay));
      }
    }

    return results;
  }

  async processLocalPDF(filePath, prompt) {
    if (this.getFileSize(filePath) > 20 * 1024 * 1024) {
      return await processLargePDF(filePath, prompt);
    } else {
      return await processInlinePDF(filePath, prompt);
    }
  }

  async processRemotePDF(url, prompt) {
    return await processRemotePDF(url, prompt);
  }

  getFileSize(filePath) {
    return fs.statSync(filePath).size;
  }
}

// Usage
const processor = new DocumentProcessor();
const documents = [
  { path: "./doc1.pdf" },
  { url: "https://example.com/doc2.pdf" },
  { path: "./doc3.pdf" }
];

const results = await processor.processBatch(
  documents,
  "Extract key insights and create a summary"
);
```

## Document Analysis Use Cases

### Contract Analysis
```javascript
async function analyzeContract(contractPath) {
  const prompt = `Analyze this contract and extract:
1. Parties involved
2. Key terms and conditions
3. Important dates and deadlines
4. Financial obligations
5. Termination clauses
6. Risk factors

Format the response as structured JSON.`;

  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: [
      {
        inlineData: {
          mimeType: 'application/pdf',
          data: Buffer.from(fs.readFileSync(contractPath)).toString("base64")
        }
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

### Financial Report Analysis
```javascript
async function analyzeFinancialReport(reportPath) {
  const prompt = `Analyze this financial report and provide:
1. Revenue trends and growth rates
2. Profitability metrics
3. Key financial ratios
4. Cash flow analysis
5. Risk factors mentioned
6. Management outlook and guidance
7. Comparison with previous periods

Create a comprehensive executive summary.`;

  return await processDocument(reportPath, prompt);
}
```

### Research Paper Processing
```javascript
async function processResearchPaper(paperPath) {
  const prompt = `Analyze this research paper and extract:
1. Abstract and key objectives
2. Methodology used
3. Main findings and results
4. Statistical significance of results
5. Limitations acknowledged
6. Future research directions
7. Citations and references to key prior work

Provide a structured academic summary.`;

  return await processDocument(paperPath, prompt);
}
```

### Legal Document Review
```javascript
async function reviewLegalDocument(documentPath, reviewType = 'general') {
  const prompts = {
    general: "Review this legal document for key provisions, potential issues, and important clauses",
    compliance: "Review this document for compliance issues and regulatory requirements",
    risk: "Identify potential legal risks and liability issues in this document",
    comparison: "Compare this document against standard industry practices and highlight deviations"
  };

  const prompt = prompts[reviewType] || prompts.general;
  return await processDocument(documentPath, prompt);
}
```

### Invoice and Receipt Processing
```javascript
async function processInvoice(invoicePath) {
  const prompt = `Extract information from this invoice/receipt:
1. Vendor/supplier information
2. Invoice number and date
3. Line items with descriptions, quantities, and prices
4. Subtotal, taxes, and total amount
5. Payment terms and due date
6. Any discounts or special conditions

Format as structured JSON for accounting system integration.`;

  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: [
      {
        inlineData: {
          mimeType: 'application/pdf',
          data: Buffer.from(fs.readFileSync(invoicePath)).toString("base64")
        }
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

## Advanced Document Processing

### Document Classification
```javascript
async function classifyDocument(documentPath) {
  const prompt = `Classify this document and provide:
1. Document type (contract, report, invoice, manual, etc.)
2. Industry or domain
3. Confidence level (1-10)
4. Key identifying features
5. Suggested processing workflow

Format as JSON.`;

  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: [
      {
        inlineData: {
          mimeType: 'application/pdf',
          data: Buffer.from(fs.readFileSync(documentPath)).toString("base64")
        }
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

### Table and Chart Extraction
```javascript
async function extractTablesAndCharts(documentPath) {
  const prompt = `Extract all tables and charts from this document:
1. For each table: provide the data in CSV format with appropriate headers
2. For each chart: describe the type, data represented, and key insights
3. Identify the page number or section where each element appears
4. Note any relationships between tables and charts

Organize the response clearly with sections for tables and charts.`;

  return await processDocument(documentPath, prompt);
}
```

### Document Summarization with Different Lengths
```javascript
async function createSummaries(documentPath, lengths = ['brief', 'detailed', 'executive']) {
  const prompts = {
    brief: "Create a 2-3 sentence summary of this document highlighting only the most critical points",
    detailed: "Create a comprehensive summary covering all major sections and key points (300-500 words)",
    executive: "Create an executive summary suitable for senior management (150-200 words)"
  };

  const summaries = {};
  
  for (const length of lengths) {
    summaries[length] = await processDocument(documentPath, prompts[length]);
  }

  return summaries;
}
```

### Document Translation and Localization
```javascript
async function translateDocument(documentPath, targetLanguage) {
  const prompt = `Translate this document to ${targetLanguage} while:
1. Preserving the original structure and formatting
2. Maintaining technical terminology accuracy
3. Adapting cultural references appropriately
4. Keeping legal and regulatory terms precise
5. Noting any untranslatable terms or concepts

Provide the translation and a summary of any translation challenges.`;

  return await processDocument(documentPath, prompt);
}
```

## Error Handling and Best Practices

### Robust Document Processing
```javascript
async function safeDocumentProcessing(documentPath, prompt, options = {}) {
  const {
    maxRetries = 3,
    timeoutMs = 60000,
    fallbackToText = false
  } = options;

  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      // Check file size and choose appropriate method
      const fileSize = fs.statSync(documentPath).size;
      
      if (fileSize > 20 * 1024 * 1024) {
        return await processLargePDF(documentPath, prompt);
      } else {
        return await processInlinePDF(documentPath, prompt);
      }
    } catch (error) {
      console.error(`Document processing attempt ${attempt} failed:`, error.message);
      
      if (attempt === maxRetries) {
        if (fallbackToText && error.message.includes('vision')) {
          // Fallback to text-only processing
          console.log('Falling back to text-only processing');
          return await processDocumentAsText(documentPath, prompt);
        }
        throw error;
      }
      
      // Wait before retry
      await new Promise(resolve => setTimeout(resolve, 1000 * attempt));
    }
  }
}

async function processDocumentAsText(documentPath, prompt) {
  // This would require a PDF text extraction library
  // For demonstration purposes, we'll show the structure
  const textContent = await extractTextFromPDF(documentPath);
  
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: [
      { text: `${prompt}\n\nDocument content:\n${textContent}` }
    ]
  });

  return response.text;
}
```

### Document Validation
```javascript
function validatePDF(filePath) {
  const stats = fs.statSync(filePath);
  const maxSize = 50 * 1024 * 1024; // 50MB limit for File API
  
  if (stats.size > maxSize) {
    throw new Error(`File size ${stats.size} exceeds maximum allowed size of ${maxSize}`);
  }

  const buffer = fs.readFileSync(filePath, { start: 0, end: 4 });
  const pdfSignature = buffer.toString('ascii');
  
  if (!pdfSignature.startsWith('%PDF')) {
    throw new Error('File is not a valid PDF document');
  }

  return {
    valid: true,
    size: stats.size,
    sizeCategory: stats.size > 20 * 1024 * 1024 ? 'large' : 'small'
  };
}
```

### Performance Optimization
```javascript
class DocumentCache {
  constructor(ttl = 3600000) { // 1 hour default TTL
    this.cache = new Map();
    this.ttl = ttl;
  }

  generateKey(filePath, prompt) {
    const stats = fs.statSync(filePath);
    return `${filePath}:${stats.mtime.getTime()}:${this.hashPrompt(prompt)}`;
  }

  hashPrompt(prompt) {
    // Simple hash function for demonstration
    let hash = 0;
    for (let i = 0; i < prompt.length; i++) {
      const char = prompt.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash; // Convert to 32-bit integer
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

  clear() {
    this.cache.clear();
  }
}

// Usage with caching
const documentCache = new DocumentCache();

async function processDocumentWithCache(filePath, prompt) {
  const cacheKey = documentCache.generateKey(filePath, prompt);
  const cached = documentCache.get(cacheKey);
  
  if (cached) {
    return cached;
  }

  const result = await safeDocumentProcessing(filePath, prompt);
  documentCache.set(cacheKey, result);
  
  return result;
}
```

## Technical Details and Limitations

### Document Specifications
- **Maximum pages**: 1000 pages per document
- **Page tokenization**: Each page = 258 tokens
- **Resolution handling**: 
  - Pages scaled down to max 3072x3072 (preserving aspect ratio)
  - Smaller pages scaled up to 768x768
- **File size limits**:
  - Inline data: 20MB total request size
  - File API: 50MB per file

### Supported MIME Types
```javascript
const SUPPORTED_DOCUMENT_TYPES = {
  primary: ['application/pdf'],
  textOnly: [
    'text/plain',
    'text/markdown', 
    'text/html',
    'text/xml',
    'application/json'
  ]
};

function isSupportedDocumentType(mimeType) {
  return SUPPORTED_DOCUMENT_TYPES.primary.includes(mimeType) ||
         SUPPORTED_DOCUMENT_TYPES.textOnly.includes(mimeType);
}
```

### Best Practices
```javascript
const DOCUMENT_BEST_PRACTICES = {
  preparation: [
    'Rotate pages to correct orientation before uploading',
    'Ensure pages are not blurry or low quality',
    'Remove unnecessary pages to reduce token usage'
  ],
  prompting: [
    'Place text prompt after document in contents array',
    'Be specific about desired output format',
    'Use structured prompts for complex extractions'
  ],
  performance: [
    'Use File API for documents > 20MB',
    'Cache results for repeated analysis',
    'Process documents in batches for efficiency'
  ]
};
```

## Integration Examples

### Express.js API Endpoint
```javascript
app.post('/api/process-document', upload.single('document'), async (req, res) => {
  try {
    const { prompt, analysisType = 'summary' } = req.body;
    const documentPath = req.file.path;

    // Validate document
    const validation = validatePDF(documentPath);
    
    const analysisPrompts = {
      summary: prompt || "Provide a comprehensive summary of this document",
      extract: "Extract key information and data points from this document",
      classify: "Classify this document and identify its type and purpose",
      translate: `Translate this document to ${req.body.targetLanguage || 'English'}`
    };

    const finalPrompt = analysisPrompts[analysisType] || prompt;
    const result = await safeDocumentProcessing(documentPath, finalPrompt);

    // Clean up uploaded file
    fs.unlinkSync(documentPath);

    res.json({
      success: true,
      analysis: result,
      documentInfo: {
        size: validation.size,
        sizeCategory: validation.sizeCategory
      }
    });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});
```

### Document Management Service
```javascript
class DocumentManagementService {
  constructor() {
    this.ai = new GoogleGenAI({});
    this.cache = new DocumentCache();
    this.processor = new DocumentProcessor();
  }

  async analyzeDocument(filePath, analysisType, customPrompt = null) {
    const prompts = {
      financial: "Analyze financial data, metrics, and trends in this document",
      legal: "Review legal terms, clauses, and potential issues",
      technical: "Extract technical specifications and requirements",
      compliance: "Check for compliance requirements and regulatory issues"
    };

    const prompt = customPrompt || prompts[analysisType] || prompts.financial;
    return await processDocumentWithCache(filePath, prompt);
  }

  async compareDocuments(documentPaths, comparisonType = 'general') {
    const prompts = {
      general: "Compare these documents and highlight key differences",
      financial: "Compare financial performance and metrics across these documents",
      legal: "Compare legal terms and identify discrepancies",
      version: "Identify changes between document versions"
    };

    return await compareDocuments(documentPaths, prompts[comparisonType]);
  }

  async generateReport(documentPaths, reportType = 'comprehensive') {
    const results = await this.processor.processBatch(
      documentPaths.map(path => ({ path })),
      "Extract key insights and data points"
    );

    const reportPrompt = `Based on the analysis of ${documentPaths.length} documents, create a ${reportType} report with:
1. Executive summary
2. Key findings across all documents
3. Trends and patterns identified
4. Recommendations
5. Supporting data and evidence`;

    // Combine all results for final report generation
    const combinedData = results.map(r => r.result).join('\n\n---\n\n');
    
    const response = await this.ai.models.generateContent({
      model: "gemini-2.5-flash",
      contents: [{ text: `${reportPrompt}\n\nAnalysis Data:\n${combinedData}` }]
    });

    return response.text;
  }
}
```

## Best Practices Summary

### Document Preparation
1. **Quality assurance**: Ensure documents are properly oriented and high quality
2. **Size optimization**: Remove unnecessary pages to reduce token usage
3. **Format validation**: Verify PDF format and integrity before processing

### Processing Strategy
1. **Method selection**: Use inline data for small files, File API for large files
2. **Batch processing**: Group related documents for efficient analysis
3. **Caching**: Implement caching for frequently analyzed documents
4. **Error handling**: Implement robust retry logic and fallback strategies

### Prompt Engineering
1. **Specificity**: Be specific about desired output format and content
2. **Structure**: Use structured prompts for complex extractions
3. **Context**: Provide context about document type and analysis goals
4. **Output format**: Specify JSON output for structured data extraction