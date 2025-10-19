# Gemini API Structured Output Guide

## Overview
Structured output allows you to configure Gemini to generate JSON or enum values instead of unstructured text, enabling precise data extraction and standardization for further processing. This guide covers implementation patterns and best practices for JavaScript/TypeScript developers.

## Why Use Structured Output
- **Data consistency**: Standardized format for downstream processing
- **Type safety**: Predictable response structure
- **Integration**: Easy parsing for databases, APIs, and applications
- **Validation**: Enforced schema compliance
- **Automation**: Reliable data extraction from unstructured content

## JSON Schema Generation

### Basic JSON Schema Setup
```javascript
import { GoogleGenAI, Type } from "@google/genai";

const ai = new GoogleGenAI({
  apiKey: process.env.GEMINI_API_KEY
});

async function generateStructuredJSON(prompt, schema) {
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: prompt,
    config: {
      responseMimeType: "application/json",
      responseSchema: schema,
    },
  });

  return JSON.parse(response.text);
}
```

### Simple Object Schema
```javascript
const personSchema = {
  type: Type.OBJECT,
  properties: {
    name: { type: Type.STRING },
    age: { type: Type.NUMBER },
    email: { type: Type.STRING },
    isActive: { type: Type.BOOLEAN }
  },
  propertyOrdering: ["name", "age", "email", "isActive"],
  required: ["name", "email"]
};

// Usage
const result = await generateStructuredJSON(
  "Extract person information: John Doe, 30 years old, john@example.com, currently active",
  personSchema
);
// Returns: { name: "John Doe", age: 30, email: "john@example.com", isActive: true }
```

### Array Schema
```javascript
const recipeSchema = {
  type: Type.ARRAY,
  items: {
    type: Type.OBJECT,
    properties: {
      recipeName: { type: Type.STRING },
      ingredients: {
        type: Type.ARRAY,
        items: { type: Type.STRING }
      },
      cookingTime: { type: Type.NUMBER },
      difficulty: { type: Type.STRING }
    },
    propertyOrdering: ["recipeName", "ingredients", "cookingTime", "difficulty"],
    required: ["recipeName", "ingredients"]
  }
};

// Usage
const recipes = await generateStructuredJSON(
  "List a few popular cookie recipes with ingredients and cooking details",
  recipeSchema
);
```

### Nested Object Schema
```javascript
const companySchema = {
  type: Type.OBJECT,
  properties: {
    companyName: { type: Type.STRING },
    address: {
      type: Type.OBJECT,
      properties: {
        street: { type: Type.STRING },
        city: { type: Type.STRING },
        country: { type: Type.STRING },
        zipCode: { type: Type.STRING }
      },
      propertyOrdering: ["street", "city", "country", "zipCode"],
      required: ["street", "city", "country"]
    },
    employees: {
      type: Type.ARRAY,
      items: {
        type: Type.OBJECT,
        properties: {
          name: { type: Type.STRING },
          position: { type: Type.STRING },
          department: { type: Type.STRING }
        },
        propertyOrdering: ["name", "position", "department"],
        required: ["name", "position"]
      }
    }
  },
  propertyOrdering: ["companyName", "address", "employees"],
  required: ["companyName"]
};
```

## Enum Generation

### Basic Enum Schema
```javascript
const instrumentEnum = {
  type: Type.STRING,
  enum: ["Percussion", "String", "Woodwind", "Brass", "Keyboard"]
};

async function classifyInstrument(instrumentName) {
  const response = await ai.models.generateContent({
    model: "gemini-2.5-flash",
    contents: `What type of instrument is a ${instrumentName}?`,
    config: {
      responseMimeType: "text/x.enum",
      responseSchema: instrumentEnum,
    },
  });

  return response.text;
}

// Usage
const classification = await classifyInstrument("oboe");
// Returns: "Woodwind"
```

### Enum in JSON Schema
```javascript
const productReviewSchema = {
  type: Type.OBJECT,
  properties: {
    productName: { type: Type.STRING },
    rating: {
      type: Type.STRING,
      enum: ["excellent", "good", "average", "poor", "terrible"]
    },
    sentiment: {
      type: Type.STRING,
      enum: ["positive", "neutral", "negative"]
    },
    summary: { type: Type.STRING }
  },
  propertyOrdering: ["productName", "rating", "sentiment", "summary"],
  required: ["productName", "rating", "sentiment"]
};

// Usage
const review = await generateStructuredJSON(
  "Analyze this product review: 'The new iPhone is amazing! Great camera and battery life.'",
  productReviewSchema
);
```

## Common Use Cases

### Data Extraction from Text
```javascript
const contactExtractionSchema = {
  type: Type.ARRAY,
  items: {
    type: Type.OBJECT,
    properties: {
      name: { type: Type.STRING },
      phone: { type: Type.STRING },
      email: { type: Type.STRING },
      company: { type: Type.STRING },
      role: { type: Type.STRING }
    },
    propertyOrdering: ["name", "phone", "email", "company", "role"],
    required: ["name"]
  }
};

async function extractContacts(businessCard) {
  return await generateStructuredJSON(
    `Extract contact information from this business card: ${businessCard}`,
    contactExtractionSchema
  );
}

// Usage
const contacts = await extractContacts(`
  John Smith
  Senior Developer
  Tech Corp Inc.
  john.smith@techcorp.com
  +1-555-0123
`);
```

### Resume Parsing
```javascript
const resumeSchema = {
  type: Type.OBJECT,
  properties: {
    personalInfo: {
      type: Type.OBJECT,
      properties: {
        name: { type: Type.STRING },
        email: { type: Type.STRING },
        phone: { type: Type.STRING },
        location: { type: Type.STRING }
      },
      propertyOrdering: ["name", "email", "phone", "location"],
      required: ["name"]
    },
    experience: {
      type: Type.ARRAY,
      items: {
        type: Type.OBJECT,
        properties: {
          company: { type: Type.STRING },
          position: { type: Type.STRING },
          startDate: { type: Type.STRING },
          endDate: { type: Type.STRING },
          description: { type: Type.STRING }
        },
        propertyOrdering: ["company", "position", "startDate", "endDate", "description"],
        required: ["company", "position"]
      }
    },
    skills: {
      type: Type.ARRAY,
      items: { type: Type.STRING }
    },
    education: {
      type: Type.ARRAY,
      items: {
        type: Type.OBJECT,
        properties: {
          institution: { type: Type.STRING },
          degree: { type: Type.STRING },
          field: { type: Type.STRING },
          graduationYear: { type: Type.NUMBER }
        },
        propertyOrdering: ["institution", "degree", "field", "graduationYear"],
        required: ["institution", "degree"]
      }
    }
  },
  propertyOrdering: ["personalInfo", "experience", "skills", "education"],
  required: ["personalInfo"]
};

async function parseResume(resumeText) {
  return await generateStructuredJSON(
    `Parse this resume and extract structured information: ${resumeText}`,
    resumeSchema
  );
}
```

### Content Classification
```javascript
const contentClassificationSchema = {
  type: Type.OBJECT,
  properties: {
    category: {
      type: Type.STRING,
      enum: ["technology", "business", "health", "entertainment", "sports", "politics", "science", "other"]
    },
    subcategory: { type: Type.STRING },
    sentiment: {
      type: Type.STRING,
      enum: ["positive", "negative", "neutral"]
    },
    confidence: { type: Type.NUMBER },
    keywords: {
      type: Type.ARRAY,
      items: { type: Type.STRING }
    },
    summary: { type: Type.STRING }
  },
  propertyOrdering: ["category", "subcategory", "sentiment", "confidence", "keywords", "summary"],
  required: ["category", "sentiment", "confidence"]
};

async function classifyContent(content) {
  return await generateStructuredJSON(
    `Classify and analyze this content: ${content}`,
    contentClassificationSchema
  );
}
```

### E-commerce Product Analysis
```javascript
const productAnalysisSchema = {
  type: Type.OBJECT,
  properties: {
    productName: { type: Type.STRING },
    brand: { type: Type.STRING },
    category: { type: Type.STRING },
    price: { type: Type.NUMBER },
    currency: { type: Type.STRING },
    features: {
      type: Type.ARRAY,
      items: { type: Type.STRING }
    },
    specifications: {
      type: Type.OBJECT,
      properties: {
        dimensions: { type: Type.STRING },
        weight: { type: Type.STRING },
        color: { type: Type.STRING },
        material: { type: Type.STRING }
      },
      propertyOrdering: ["dimensions", "weight", "color", "material"]
    },
    availability: {
      type: Type.STRING,
      enum: ["in_stock", "out_of_stock", "limited", "pre_order"]
    }
  },
  propertyOrdering: ["productName", "brand", "category", "price", "currency", "features", "specifications", "availability"],
  required: ["productName", "category"]
};
```

## Advanced Schema Patterns

### Conditional Fields with Enums
```javascript
const taskSchema = {
  type: Type.OBJECT,
  properties: {
    taskType: {
      type: Type.STRING,
      enum: ["bug", "feature", "improvement", "documentation"]
    },
    priority: {
      type: Type.STRING,
      enum: ["low", "medium", "high", "critical"]
    },
    status: {
      type: Type.STRING,
      enum: ["todo", "in_progress", "review", "done"]
    },
    assignee: { type: Type.STRING },
    estimatedHours: { type: Type.NUMBER },
    tags: {
      type: Type.ARRAY,
      items: { type: Type.STRING }
    }
  },
  propertyOrdering: ["taskType", "priority", "status", "assignee", "estimatedHours", "tags"],
  required: ["taskType", "priority", "status"]
};
```

### Validation with Constraints
```javascript
const eventSchema = {
  type: Type.OBJECT,
  properties: {
    eventName: { type: Type.STRING },
    startDate: { 
      type: Type.STRING,
      format: "date-time"
    },
    endDate: { 
      type: Type.STRING,
      format: "date-time"
    },
    attendees: {
      type: Type.ARRAY,
      minItems: 1,
      maxItems: 100,
      items: {
        type: Type.OBJECT,
        properties: {
          name: { type: Type.STRING },
          email: { type: Type.STRING }
        },
        required: ["name", "email"]
      }
    },
    budget: {
      type: Type.NUMBER,
      minimum: 0,
      maximum: 1000000
    }
  },
  propertyOrdering: ["eventName", "startDate", "endDate", "attendees", "budget"],
  required: ["eventName", "startDate", "endDate"]
};
```

## Schema Builders and Helpers

### Schema Builder Utility
```javascript
class SchemaBuilder {
  constructor() {
    this.schema = {};
  }

  object(properties = {}, required = []) {
    this.schema = {
      type: Type.OBJECT,
      properties,
      propertyOrdering: Object.keys(properties),
      required
    };
    return this;
  }

  array(itemSchema, minItems = null, maxItems = null) {
    this.schema = {
      type: Type.ARRAY,
      items: itemSchema,
      ...(minItems !== null && { minItems }),
      ...(maxItems !== null && { maxItems })
    };
    return this;
  }

  string(enumValues = null) {
    this.schema = {
      type: Type.STRING,
      ...(enumValues && { enum: enumValues })
    };
    return this;
  }

  number(min = null, max = null) {
    this.schema = {
      type: Type.NUMBER,
      ...(min !== null && { minimum: min }),
      ...(max !== null && { maximum: max })
    };
    return this;
  }

  build() {
    return this.schema;
  }
}

// Usage
const userSchema = new SchemaBuilder()
  .object({
    name: new SchemaBuilder().string().build(),
    age: new SchemaBuilder().number(0, 120).build(),
    role: new SchemaBuilder().string(["admin", "user", "guest"]).build()
  }, ["name", "role"])
  .build();
```

### Common Schema Templates
```javascript
const SchemaTemplates = {
  person: {
    type: Type.OBJECT,
    properties: {
      firstName: { type: Type.STRING },
      lastName: { type: Type.STRING },
      email: { type: Type.STRING },
      age: { type: Type.NUMBER, minimum: 0, maximum: 150 }
    },
    propertyOrdering: ["firstName", "lastName", "email", "age"],
    required: ["firstName", "lastName"]
  },

  address: {
    type: Type.OBJECT,
    properties: {
      street: { type: Type.STRING },
      city: { type: Type.STRING },
      state: { type: Type.STRING },
      zipCode: { type: Type.STRING },
      country: { type: Type.STRING }
    },
    propertyOrdering: ["street", "city", "state", "zipCode", "country"],
    required: ["street", "city", "country"]
  },

  dateRange: {
    type: Type.OBJECT,
    properties: {
      startDate: { type: Type.STRING, format: "date" },
      endDate: { type: Type.STRING, format: "date" }
    },
    propertyOrdering: ["startDate", "endDate"],
    required: ["startDate", "endDate"]
  }
};
```

## Error Handling and Validation

### Robust Schema Processing
```javascript
async function safeStructuredGeneration(prompt, schema, options = {}) {
  const {
    maxRetries = 3,
    validateResult = true,
    fallbackToText = false
  } = options;

  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      const response = await ai.models.generateContent({
        model: "gemini-2.5-flash",
        contents: prompt,
        config: {
          responseMimeType: "application/json",
          responseSchema: schema,
        },
      });

      const result = JSON.parse(response.text);
      
      if (validateResult && !validateSchema(result, schema)) {
        throw new Error("Response doesn't match schema");
      }

      return result;
    } catch (error) {
      console.error(`Attempt ${attempt} failed:`, error.message);
      
      if (attempt === maxRetries) {
        if (fallbackToText) {
          console.log("Falling back to unstructured text");
          const response = await ai.models.generateContent({
            model: "gemini-2.5-flash",
            contents: prompt,
          });
          return { text: response.text, structured: false };
        }
        throw error;
      }
      
      // Wait before retry
      await new Promise(resolve => setTimeout(resolve, 1000 * attempt));
    }
  }
}

function validateSchema(data, schema) {
  // Basic validation - you might want to use a proper JSON schema validator
  if (schema.type === Type.OBJECT && schema.required) {
    for (const field of schema.required) {
      if (!(field in data)) {
        return false;
      }
    }
  }
  return true;
}
```

### Schema Complexity Management
```javascript
function optimizeSchema(schema) {
  // Remove optional properties if schema is too complex
  if (schema.type === Type.OBJECT && schema.properties) {
    const required = schema.required || [];
    const optimizedProperties = {};
    
    // Keep only required properties if schema is complex
    for (const [key, value] of Object.entries(schema.properties)) {
      if (required.includes(key)) {
        optimizedProperties[key] = value;
      }
    }
    
    return {
      ...schema,
      properties: optimizedProperties,
      propertyOrdering: Object.keys(optimizedProperties)
    };
  }
  
  return schema;
}
```

## Best Practices

### Property Ordering
```javascript
// Always specify propertyOrdering for consistent results
const goodSchema = {
  type: Type.OBJECT,
  properties: {
    name: { type: Type.STRING },
    age: { type: Type.NUMBER },
    email: { type: Type.STRING }
  },
  propertyOrdering: ["name", "age", "email"], // ✅ Explicit ordering
  required: ["name"]
};

// Avoid relying on alphabetical ordering
const badSchema = {
  type: Type.OBJECT,
  properties: {
    name: { type: Type.STRING },
    age: { type: Type.NUMBER },
    email: { type: Type.STRING }
  },
  // ❌ No propertyOrdering - unpredictable results
  required: ["name"]
};
```

### Schema Size Optimization
```javascript
// Keep schemas concise to avoid token limit issues
function createOptimalSchema(fields) {
  // Limit property names to reasonable length
  const optimizedFields = {};
  
  for (const [key, value] of Object.entries(fields)) {
    const shortKey = key.length > 20 ? key.substring(0, 20) : key;
    optimizedFields[shortKey] = value;
  }
  
  return {
    type: Type.OBJECT,
    properties: optimizedFields,
    propertyOrdering: Object.keys(optimizedFields)
  };
}
```

### Context Enhancement
```javascript
async function generateWithContext(data, schema, context = "") {
  const enhancedPrompt = `
${context}

Please extract the following information from the data below and format it according to the specified schema:

Data: ${data}

Instructions:
- Be precise and accurate
- Use null for missing information
- Follow the exact schema structure
- Maintain data types as specified
`;

  return await generateStructuredJSON(enhancedPrompt, schema);
}
```

## Integration Examples

### Express.js API Endpoint
```javascript
app.post('/api/extract', async (req, res) => {
  try {
    const { text, schemaType } = req.body;
    
    const schemas = {
      contact: contactExtractionSchema,
      resume: resumeSchema,
      product: productAnalysisSchema
    };
    
    const schema = schemas[schemaType];
    if (!schema) {
      return res.status(400).json({ error: 'Invalid schema type' });
    }
    
    const result = await safeStructuredGeneration(text, schema, {
      validateResult: true,
      fallbackToText: false
    });
    
    res.json({ success: true, data: result });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});
```

### Batch Processing
```javascript
async function batchStructuredExtraction(items, schema, options = {}) {
  const { concurrency = 3, delay = 1000 } = options;
  const results = [];
  
  for (let i = 0; i < items.length; i += concurrency) {
    const batch = items.slice(i, i + concurrency);
    
    const batchPromises = batch.map(async (item, index) => {
      try {
        const result = await safeStructuredGeneration(item.text, schema);
        return { 
          id: item.id, 
          index: i + index, 
          data: result, 
          success: true 
        };
      } catch (error) {
        return { 
          id: item.id, 
          index: i + index, 
          error: error.message, 
          success: false 
        };
      }
    });
    
    const batchResults = await Promise.all(batchPromises);
    results.push(...batchResults);
    
    // Rate limiting delay
    if (i + concurrency < items.length) {
      await new Promise(resolve => setTimeout(resolve, delay));
    }
  }
  
  return results;
}
```

## Performance Tips

1. **Schema Optimization**: Keep schemas simple and focused
2. **Property Ordering**: Always specify `propertyOrdering` for consistency
3. **Required Fields**: Mark essential fields as required
4. **Enum Usage**: Use enums for controlled vocabularies
5. **Context Enhancement**: Provide clear instructions and examples
6. **Error Handling**: Implement retry logic and validation
7. **Token Management**: Monitor schema size impact on token usage