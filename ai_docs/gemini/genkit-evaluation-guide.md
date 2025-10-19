# Genkit Evaluation Guide

## Overview
Evaluation in Genkit is a form of testing that helps validate your LLM's responses and ensures they meet your quality standards. Genkit provides comprehensive evaluation tools including automated metrics, dataset management, and comparison features to systematically assess AI application performance.

## Types of Evaluation

### Inference-Based Evaluation
- **Most common approach**: Tests actual system output for each evaluation run
- **Process**: Runs against pre-determined inputs, assessing corresponding outputs for quality
- **Assessment methods**: Manual inspection or automated metrics
- **Use case**: Testing live system performance and quality

### Raw Evaluation
- **Direct assessment**: Evaluates quality without running inference
- **Requirements**: All fields (input, context, output, reference) must be present in dataset
- **Use case**: Evaluating external data or production traces
- **Automation**: Typically uses automated metrics

## Quick Start Setup

### Basic RAG Application
```javascript
import { genkit, z, Document } from 'genkit';
import { googleAI } from '@genkit-ai/googleai';
import { genkitEval, GenkitMetric } from '@genkit-ai/evaluator';

// Initialize Genkit with evaluation support
export const ai = genkit({
  plugins: [
    googleAI(),
    genkitEval({
      judge: googleAI.model('gemini-2.5-flash'),
      metrics: [
        GenkitMetric.FAITHFULNESS,
        GenkitMetric.ANSWER_RELEVANCY,
        GenkitMetric.MALICIOUSNESS
      ],
    }),
  ]
});

// Dummy retriever for testing
export const dummyRetriever = ai.defineRetriever({
  name: 'dummyRetriever',
}, async (input) => {
  const facts = [
    "Dog is man's best friend",
    'Dogs have evolved and were domesticated from wolves'
  ];
  return { documents: facts.map((t) => Document.fromText(t)) };
});

// Question-answering flow to evaluate
export const qaFlow = ai.defineFlow({
  name: 'qaFlow',
  inputSchema: z.object({ query: z.string() }),
  outputSchema: z.object({ answer: z.string() }),
}, async ({ query }) => {
  const factDocs = await ai.retrieve({
    retriever: dummyRetriever,
    query,
  });

  const { text } = await ai.generate({
    model: googleAI.model('gemini-2.5-flash'),
    prompt: `Answer this question with the given context: ${query}`,
    docs: factDocs,
  });

  return { answer: text };
});
```## Dataset
 Management

### Creating Datasets via Dev UI
1. **Navigate to Datasets**: Go to http://localhost:4000 and click Datasets
2. **Create New Dataset**: Click "Create Dataset" button
3. **Configure Dataset**:
   - Provide a `datasetId` (e.g., "myFactsQaDataset")
   - Select dataset type: "Flow dataset" or "Model dataset"
   - Leave validation target empty for basic setup
4. **Add Examples**: Click "Add example" and enter test cases

### Dataset Types and Schema
```javascript
// Flow Dataset Example
const flowDatasetExample = [
  {
    "input": { "query": "Who is man's best friend?" },
    "reference": "Dogs are man's best friend" // Optional
  },
  {
    "input": { "query": "Can I give milk to my cats?" }
  },
  {
    "input": { "query": "From which animals did dogs evolve?" },
    "reference": "Dogs evolved from wolves"
  }
];

// Model Dataset Example (String input)
const modelDatasetStringExample = [
  {
    "input": "What is the capital of France?",
    "reference": "Paris"
  }
];

// Model Dataset Example (GenerateRequest input)
const modelDatasetAdvancedExample = [
  {
    "input": {
      "messages": [
        { "role": "user", "content": [{ "text": "Explain quantum physics" }] }
      ],
      "config": {
        "temperature": 0.7,
        "maxOutputTokens": 500
      }
    },
    "reference": "Quantum physics explanation..."
  }
];
```

## Built-in Evaluation Metrics

### Genkit Native Evaluators
```javascript
import { genkitEval, GenkitMetric } from '@genkit-ai/evaluator';

const ai = genkit({
  plugins: [
    genkitEval({
      judge: googleAI.model('gemini-2.5-flash'),
      metrics: [
        GenkitMetric.FAITHFULNESS,     // Factual consistency with context
        GenkitMetric.ANSWER_RELEVANCY, // Pertinence to the prompt
        GenkitMetric.MALICIOUSNESS     // Intent to deceive or harm
      ],
    }),
  ]
});
```

### Custom Evaluators
```javascript
const customAccuracyEvaluator = ai.defineEvaluator({
  name: 'customAccuracy',
  displayName: 'Custom Accuracy Evaluator',
  definition: {
    metrics: [
      {
        name: 'accuracy',
        displayName: 'Accuracy Score',
        description: 'Measures correctness of the response'
      }
    ]
  }
}, async (input, output, context) => {
  // Custom evaluation logic
  const { output: evaluation } = await ai.generate({
    model: googleAI.model('gemini-2.5-pro'),
    prompt: `
      Evaluate the accuracy of this response:
      
      Question: ${input.query}
      Response: ${output.answer}
      ${context?.reference ? `Expected: ${context.reference}` : ''}
      
      Rate accuracy from 0.0 to 1.0 and explain your reasoning.
    `,
    output: {
      schema: z.object({
        accuracy: z.number().min(0).max(1),
        reasoning: z.string()
      })
    }
  });

  return {
    accuracy: evaluation.accuracy,
    details: {
      reasoning: evaluation.reasoning,
      hasReference: !!context?.reference
    }
  };
});
```

## Running Evaluations

### Via Developer UI
1. **Start Evaluation**: Click "Run new evaluation" on dataset page
2. **Select Target**: Choose flow or model to evaluate
3. **Choose Dataset**: Select dataset for evaluation
4. **Select Metrics**: Choose evaluation metrics (optional)
5. **Execute**: Click "Run evaluation" and wait for completion
6. **View Results**: Click link to evaluation details page

### Via CLI Commands
```bash
# Start your Genkit app first
genkit start -- npm run dev

# Evaluate flow with existing dataset
genkit eval:flow qaFlow --input myFactsQaDataset

# Evaluate flow with JSON file
genkit eval:flow qaFlow --input testInputs.json

# Evaluate with specific metrics
genkit eval:flow qaFlow --input testInputs.json --evaluators=genkitEval/faithfulness,genkitEval/answer_relevancy

# Evaluate with authentication context
genkit eval:flow qaFlow --input testInputs.json --context '{"auth": {"email_verified": true}}'

# Batch run with labels for later extraction
genkit flow:batchRun qaFlow testInputs.json --label firstRunSimple

# Extract evaluation data from traces
genkit eval:extractData qaFlow --label firstRunSimple --output factsEvalDataset.json

# Run raw evaluation on extracted data
genkit eval:run factsEvalDataset.json
```

## Advanced Evaluation Features

### Evaluation Comparison
```javascript
// Prerequisites for comparison:
// 1. Evaluations from same dataset
// 2. At least two evaluation runs
// 3. Common metrics for highlighting

// Comparison workflow:
// 1. Navigate to dataset's Evaluations tab
// 2. Select baseline evaluation
// 3. Click "+ Comparison" button
// 4. Select comparison evaluation from dropdown
// 5. Enable metric highlighting (optional)

const comparisonAnalysis = {
  baseline: "evaluation_run_1",
  comparison: "evaluation_run_2", 
  metricHighlighting: {
    metric: "accuracy",
    reverseLogic: false, // false = lower is better (red), higher is better (green)
    colorCoding: {
      improvement: "green",
      regression: "red",
      neutral: "gray"
    }
  }
};
```

### Batch Evaluation (Node.js only)
```bash
# Enable batching for performance improvement
genkit eval:flow myFlow --input dataset.json --evaluators=custom/myEval --batchSize 10

# Batch raw evaluation
genkit eval:run dataset.json --evaluators=custom/myEval --batchSize 10
```

**Note**: Batching is also available in the Dev UI for Genkit (JS) applications. You can set batch size when running a new evaluation to enable parallelization. When batching is enabled, input data is grouped into batches of the specified size, with data points in each batch processed in parallel for significant performance improvements.

### Custom Extractors
```javascript
// Enhanced flow with custom steps
export const enhancedQaFlow = ai.defineFlow({
  name: 'enhancedQaFlow',
  inputSchema: z.object({ query: z.string() }),
  outputSchema: z.object({ answer: z.string() }),
}, async ({ query }) => {
  const factDocs = await ai.retrieve({
    retriever: dummyRetriever,
    query,
  });

  // Custom step for extraction
  const filteredFacts = await ai.run('factFiltering', async () => {
    return factDocs.filter((d) => isRelevantFact(d.text, query));
  });

  const { text } = await ai.generate({
    model: googleAI.model('gemini-2.5-flash'),
    prompt: `Answer this question with the given context: ${query}`,
    docs: filteredFacts,
  });

  return { answer: text };
});

// Custom extractor configuration (genkit-tools.conf.js)
module.exports = {
  evaluators: [
    {
      actionRef: '/flow/enhancedQaFlow',
      extractors: {
        context: { outputOf: 'factFiltering' }, // Use filtered facts as context
        input: (trace) => trace.input, // Custom extraction function
        output: { outputOf: 'generate' } // Use generation output
      },
    },
  ],
};
```

## Test Data Synthesis

### LLM-Generated Test Cases
```javascript
import { chunk } from 'llm-chunk';
import path from 'path';
import { readFile } from 'fs/promises';
import pdf from 'pdf-parse';

const chunkingConfig = {
  minLength: 1000,
  maxLength: 2000,
  splitter: 'sentence',
  overlap: 100,
  delimiters: '',
};

async function extractText(filePath: string) {
  const pdfFile = path.resolve(filePath);
  const dataBuffer = await readFile(pdfFile);
  const data = await pdf(dataBuffer);
  return data.text;
}

export const synthesizeQuestions = ai.defineFlow({
  name: 'synthesizeQuestions',
  inputSchema: z.object({ 
    filePath: z.string().describe('PDF file path'),
    questionsPerChunk: z.number().default(1),
    questionTypes: z.array(z.enum(['factual', 'analytical', 'comparative'])).default(['factual'])
  }),
  outputSchema: z.object({
    questions: z.array(z.object({
      query: z.string(),
      type: z.string(),
      sourceChunk: z.string(),
      difficulty: z.enum(['easy', 'medium', 'hard']).optional()
    })),
  }),
}, async ({ filePath, questionsPerChunk, questionTypes }) => {
  const pdfText = await ai.run('extract-text', () => extractText(filePath));
  const chunks = await ai.run('chunk-text', async () => chunk(pdfText, chunkingConfig));
  
  const questions = [];
  
  for (let i = 0; i < chunks.length; i++) {
    for (const questionType of questionTypes) {
      for (let j = 0; j < questionsPerChunk; j++) {
        const { output } = await ai.generate({
          model: googleAI.model('gemini-2.5-flash'),
          prompt: `Generate a ${questionType} question about the following text: ${chunks[i]}`,
          output: {
            schema: z.object({
              query: z.string(),
              difficulty: z.enum(['easy', 'medium', 'hard'])
            })
          }
        });
        
        questions.push({
          query: output.query,
          type: questionType,
          sourceChunk: chunks[i].substring(0, 200) + '...',
          difficulty: output.difficulty
        });
      }
    }
  }
  
  return { questions };
});

// Export synthesized questions
// genkit flow:run synthesizeQuestions '{"filePath": "document.pdf"}' --output questions.json
```

## Evaluation Best Practices

### Dataset Design
1. **Diverse Examples**: Include varied input types and edge cases
2. **Reference Answers**: Provide expected outputs when possible
3. **Balanced Distribution**: Ensure good coverage of use cases
4. **Regular Updates**: Keep datasets current with application changes

### Metric Selection
1. **Task-Appropriate**: Choose metrics that align with your use case
2. **Multiple Perspectives**: Use complementary metrics for comprehensive evaluation
3. **Custom Metrics**: Develop domain-specific evaluators when needed
4. **Baseline Comparison**: Establish performance baselines for comparison

### Evaluation Workflow
1. **Automated Pipeline**: Integrate evaluation into CI/CD workflows
2. **Regular Cadence**: Run evaluations on schedule and before releases
3. **Regression Detection**: Monitor for performance degradation
4. **Iterative Improvement**: Use results to guide model and prompt improvements

### Performance Optimization
1. **Batch Processing**: Use batching for large datasets
2. **Parallel Execution**: Run independent evaluations concurrently
3. **Caching**: Cache evaluation results for repeated runs
4. **Resource Management**: Monitor compute usage during evaluation

This comprehensive evaluation guide provides the foundation for systematic testing and quality assurance of your Genkit AI applications.