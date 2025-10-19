# Genkit Multi-Agent Systems Guide

## Overview
Multi-agent systems in Genkit enable complex workflows where multiple AI agents collaborate to solve problems, each with specialized roles and capabilities. Genkit provides native support for multi-agent architectures through prompts-as-tools, allowing you to create specialized agents that can be orchestrated by a primary triage agent.

## Why Multi-Agent Systems?

### Single Agent Limitations
As you add more capabilities to a single agent, several problems emerge:
- **Tool overload**: Too many tools stretch the model's ability to consistently choose the right tool
- **Context switching**: Some tasks need focused back-and-forth interaction rather than single tool calls
- **Specialized prompting**: Different scenarios benefit from different tones and approaches (friendly greeting vs. business-like complaint handling)

### Multi-Agent Benefits
- **Specialization**: Each agent focuses on specific domain expertise
- **Scalability**: Add new capabilities without overwhelming existing agents
- **Maintainability**: Easier to debug and improve individual agent behaviors
- **Context management**: Each agent maintains focused context for their domain

## Basic Multi-Agent Architecture

### Simple Customer Service Example
```javascript
import { genkit, z } from 'genkit';
import { googleAI } from '@genkit-ai/google-genai';

const ai = genkit({
  plugins: [googleAI()],
});

// Define tools for specific domains
const menuLookupTool = ai.defineTool({
  name: 'menuLookupTool',
  description: 'use this tool to look up the menu for a given date',
  inputSchema: z.object({
    date: z.string().describe('the date to look up the menu for'),
  }),
  outputSchema: z.string().describe('the menu for a given date'),
}, async (input) => {
  // Retrieve the menu from a database, website, etc.
  return await getMenuForDate(input.date);
});

const reservationTool = ai.defineTool({
  name: 'reservationTool',
  description: 'use this tool to try to book a reservation',
  inputSchema: z.object({
    partySize: z.coerce.number().describe('the number of guests'),
    date: z.string().describe('the date to book for'),
  }),
  outputSchema: z.string().describe(
    "true if the reservation was successfully booked and false if there's no table available"
  ),
}, async (input) => {
  // Access your database to try to make the reservation
  return await bookReservation(input.partySize, input.date);
});
```##
 Prompts as Agents

### Specialized Agent Definition
```javascript
// Define a prompt that represents a specialist agent
const reservationAgent = ai.definePrompt({
  name: 'reservationAgent',
  description: 'Reservation Agent can help manage guest reservations',
  tools: [reservationTool, reservationCancelationTool, reservationListTool],
  system: 'Help guests make and manage reservations',
});

// Menu information specialist
const menuInfoAgent = ai.definePrompt({
  name: 'menuInfoAgent',
  description: 'Menu Information Agent provides details about food and beverages',
  tools: [menuLookupTool, allergenInfoTool, nutritionTool],
  system: 'Provide helpful information about our menu, ingredients, and dietary options',
});

// Complaint handling specialist
const complaintAgent = ai.definePrompt({
  name: 'complaintAgent',
  description: 'Complaint Agent handles customer concerns and issues',
  tools: [issueTrackingTool, refundTool, managerEscalationTool],
  system: 'Handle customer complaints professionally and empathetically. Focus on resolution and customer satisfaction.',
});
```

### Triage Agent Orchestration
```javascript
// The triage agent coordinates with specialist agents
const triageAgent = ai.definePrompt({
  name: 'triageAgent',
  description: 'Triage Agent routes customers to appropriate specialists',
  tools: [reservationAgent, menuInfoAgent, complaintAgent],
  system: `You are an AI customer service agent for Pavel's Cafe.
Greet the user and ask them how you can help. If appropriate, transfer to an
agent that can better handle the request. If you cannot help the customer with
the available tools, politely explain so.`,
});

// Start a chat session with the triage agent
const chat = ai.chat(triageAgent);
```

## Advanced Multi-Agent Patterns

### Hierarchical Agent System
```javascript
// Research specialist agents
const marketResearchAgent = ai.definePrompt({
  name: 'marketResearchAgent',
  description: 'Conducts market research and competitive analysis',
  tools: [webSearchTool, industryDataTool, competitorAnalysisTool],
  system: 'You are a market research specialist. Provide thorough analysis of market trends, competitor activities, and industry insights.',
});

const technicalResearchAgent = ai.definePrompt({
  name: 'technicalResearchAgent', 
  description: 'Researches technical solutions and implementations',
  tools: [documentationSearchTool, codeAnalysisTool, architectureReviewTool],
  system: 'You are a technical research specialist. Focus on technical feasibility, implementation details, and architectural considerations.',
});

// Analysis specialists
const dataAnalysisAgent = ai.definePrompt({
  name: 'dataAnalysisAgent',
  description: 'Performs statistical and data analysis',
  tools: [calculatorTool, statisticsTool, visualizationTool],
  system: 'You are a data analyst. Provide statistical insights, identify patterns, and create data-driven recommendations.',
});

// Synthesis coordinator
const researchCoordinatorAgent = ai.definePrompt({
  name: 'researchCoordinatorAgent',
  description: 'Coordinates research efforts and synthesizes findings',
  tools: [marketResearchAgent, technicalResearchAgent, dataAnalysisAgent],
  system: `You are a research coordinator. Delegate research tasks to appropriate specialists,
synthesize their findings, and provide comprehensive analysis and recommendations.`,
});
```

### Collaborative Decision Making
```javascript
const stakeholderAgents = {
  engineering: ai.definePrompt({
    name: 'engineeringAgent',
    description: 'Engineering perspective on technical decisions',
    tools: [technicalAnalysisTool, feasibilityTool, resourceEstimationTool],
    system: 'Evaluate from an engineering perspective: technical feasibility, resource requirements, and implementation complexity.',
  }),
  
  product: ai.definePrompt({
    name: 'productAgent', 
    description: 'Product management perspective on features and priorities',
    tools: [userResearchTool, marketAnalysisTool, prioritizationTool],
    system: 'Evaluate from a product perspective: user value, market fit, and strategic alignment.',
  }),
  
  business: ai.definePrompt({
    name: 'businessAgent',
    description: 'Business perspective on ROI and strategy',
    tools: [financialAnalysisTool, riskAssessmentTool, competitiveAnalysisTool],
    system: 'Evaluate from a business perspective: ROI, market opportunity, and strategic impact.',
  })
};

const decisionFacilitatorAgent = ai.definePrompt({
  name: 'decisionFacilitatorAgent',
  description: 'Facilitates collaborative decision making across stakeholders',
  tools: Object.values(stakeholderAgents),
  system: `You are a decision facilitator. Gather input from all stakeholder perspectives,
identify areas of agreement and disagreement, and help reach consensus on decisions.`,
});
```

## Multi-Agent Flows

### Sequential Agent Workflow
```javascript
export const researchAndAnalysisFlow = ai.defineFlow({
  name: 'researchAndAnalysisFlow',
  inputSchema: z.object({
    topic: z.string(),
    depth: z.enum(['basic', 'detailed', 'comprehensive']).default('detailed'),
    stakeholders: z.array(z.string()).optional()
  }),
  outputSchema: z.object({
    research: z.string(),
    analysis: z.string(),
    recommendations: z.array(z.string()),
    stakeholderInput: z.record(z.string()).optional()
  })
}, async (input) => {
  // Step 1: Initial research
  const researchChat = ai.chat(researchCoordinatorAgent);
  const researchResult = await researchChat.send(
    `Conduct ${input.depth} research on: ${input.topic}`
  );

  // Step 2: Stakeholder input (if requested)
  let stakeholderInput = {};
  if (input.stakeholders) {
    for (const stakeholder of input.stakeholders) {
      const agent = stakeholderAgents[stakeholder];
      if (agent) {
        const stakeholderChat = ai.chat(agent);
        const response = await stakeholderChat.send(
          `Evaluate this research from your perspective: ${researchResult.text}`
        );
        stakeholderInput[stakeholder] = response.text;
      }
    }
  }

  // Step 3: Final synthesis
  const facilitatorChat = ai.chat(decisionFacilitatorAgent);
  const finalAnalysis = await facilitatorChat.send(`
    Synthesize this research and stakeholder input into final recommendations:
    
    Research: ${researchResult.text}
    
    Stakeholder Input: ${JSON.stringify(stakeholderInput)}
  `);

  return {
    research: researchResult.text,
    analysis: finalAnalysis.text,
    recommendations: extractRecommendations(finalAnalysis.text),
    stakeholderInput: Object.keys(stakeholderInput).length > 0 ? stakeholderInput : undefined
  };
});
```

### Parallel Agent Execution
```javascript
export const parallelAnalysisFlow = ai.defineFlow({
  name: 'parallelAnalysisFlow',
  inputSchema: z.object({
    topic: z.string(),
    perspectives: z.array(z.enum(['market', 'technical', 'business', 'user'])),
    synthesize: z.boolean().default(true)
  }),
  outputSchema: z.object({
    perspectives: z.record(z.string()),
    synthesis: z.string().optional(),
    consensus: z.object({
      agreements: z.array(z.string()),
      disagreements: z.array(z.string()),
      recommendations: z.array(z.string())
    }).optional()
  })
}, async (input) => {
  const agentMap = {
    market: marketResearchAgent,
    technical: technicalResearchAgent, 
    business: stakeholderAgents.business,
    user: stakeholderAgents.product
  };

  // Execute analyses in parallel
  const analysisPromises = input.perspectives.map(async (perspective) => {
    const agent = agentMap[perspective];
    const chat = ai.chat(agent);
    const response = await chat.send(
      `Analyze "${input.topic}" from the ${perspective} perspective`
    );
    return { perspective, analysis: response.text };
  });

  const analyses = await Promise.all(analysisPromises);
  const perspectives = Object.fromEntries(
    analyses.map(a => [a.perspective, a.analysis])
  );

  let synthesis, consensus;
  if (input.synthesize) {
    const facilitatorChat = ai.chat(decisionFacilitatorAgent);
    const synthesisResult = await facilitatorChat.send(`
      Synthesize these different perspectives on "${input.topic}":
      ${analyses.map(a => `${a.perspective}: ${a.analysis}`).join('\n\n')}
      
      Identify agreements, disagreements, and provide balanced recommendations.
    `);
    
    synthesis = synthesisResult.text;
    consensus = extractConsensus(synthesisResult.text);
  }

  return { perspectives, synthesis, consensus };
});
```

## Agent Communication Patterns

### Message Passing System
```javascript
class AgentCommunicationHub {
  constructor() {
    this.agents = new Map();
    this.messageHistory = [];
    this.activeConversations = new Map();
  }

  registerAgent(name, agent) {
    this.agents.set(name, agent);
  }

  async sendMessage(from, to, message, conversationId = null) {
    const messageObj = {
      id: generateMessageId(),
      from,
      to,
      message,
      conversationId,
      timestamp: new Date().toISOString()
    };

    this.messageHistory.push(messageObj);
    
    if (conversationId) {
      if (!this.activeConversations.has(conversationId)) {
        this.activeConversations.set(conversationId, []);
      }
      this.activeConversations.get(conversationId).push(messageObj);
    }

    // Route message to target agent
    const targetAgent = this.agents.get(to);
    if (targetAgent) {
      const chat = ai.chat(targetAgent);
      const context = conversationId ? 
        this.getConversationContext(conversationId) : '';
      
      const response = await chat.send(`
        Message from ${from}: ${message}
        ${context ? `\nConversation context: ${context}` : ''}
      `);
      
      return response.text;
    }
    
    throw new Error(`Agent ${to} not found`);
  }

  getConversationContext(conversationId) {
    const conversation = this.activeConversations.get(conversationId) || [];
    return conversation.map(msg => 
      `${msg.from} -> ${msg.to}: ${msg.message}`
    ).join('\n');
  }

  broadcastMessage(from, message, excludeAgents = []) {
    const promises = [];
    
    for (const [agentName] of this.agents) {
      if (agentName !== from && !excludeAgents.includes(agentName)) {
        promises.push(this.sendMessage(from, agentName, message));
      }
    }
    
    return Promise.all(promises);
  }
}
```

### Collaborative Problem Solving
```javascript
export const collaborativeProblemSolvingFlow = ai.defineFlow({
  name: 'collaborativeProblemSolving',
  inputSchema: z.object({
    problem: z.string(),
    requiredAgents: z.array(z.string()),
    maxRounds: z.number().default(5)
  }),
  outputSchema: z.object({
    solution: z.string(),
    collaboration: z.array(z.object({
      round: z.number(),
      contributions: z.record(z.string()),
      consensus: z.number()
    })),
    finalConsensus: z.number()
  })
}, async (input) => {
  const hub = new AgentCommunicationHub();
  const conversationId = generateConversationId();
  
  // Register required agents
  const agentInstances = {};
  for (const agentName of input.requiredAgents) {
    const agent = getAgentByName(agentName);
    hub.registerAgent(agentName, agent);
    agentInstances[agentName] = agent;
  }

  const collaboration = [];
  let currentProblem = input.problem;

  for (let round = 1; round <= input.maxRounds; round++) {
    const contributions = {};
    
    // Each agent contributes to the current problem
    for (const agentName of input.requiredAgents) {
      const response = await hub.sendMessage(
        'coordinator',
        agentName,
        `Round ${round}: Help solve this problem: ${currentProblem}`,
        conversationId
      );
      contributions[agentName] = response;
    }

    // Assess consensus
    const consensusChat = ai.chat(decisionFacilitatorAgent);
    const consensusResult = await consensusChat.send(`
      Assess the consensus level (0-1) among these contributions:
      ${Object.entries(contributions).map(([agent, contrib]) => 
        `${agent}: ${contrib}`
      ).join('\n\n')}
    `);
    
    const consensus = extractConsensusScore(consensusResult.text);
    
    collaboration.push({
      round,
      contributions,
      consensus
    });

    // If high consensus, we can conclude
    if (consensus > 0.8) {
      break;
    }

    // Update problem statement based on contributions
    currentProblem = await refineProblemStatement(currentProblem, contributions);
  }

  // Generate final solution
  const solutionChat = ai.chat(decisionFacilitatorAgent);
  const finalSolution = await solutionChat.send(`
    Based on this collaborative process, provide the final solution:
    ${JSON.stringify(collaboration)}
  `);

  return {
    solution: finalSolution.text,
    collaboration,
    finalConsensus: collaboration[collaboration.length - 1]?.consensus || 0
  };
});
```

## Agent Specialization Patterns

### Domain Expert Agents
```javascript
// Financial analysis specialist
const financialAnalystAgent = ai.definePrompt({
  name: 'financialAnalyst',
  description: 'Financial analysis and modeling expert',
  tools: [
    financialCalculatorTool,
    marketDataTool,
    riskAssessmentTool,
    valuationTool
  ],
  system: `You are a senior financial analyst with expertise in:
- Financial modeling and valuation
- Risk assessment and management  
- Market analysis and forecasting
- Investment evaluation and recommendations
Provide detailed financial analysis with supporting calculations.`,
});

// Legal compliance specialist
const legalComplianceAgent = ai.definePrompt({
  name: 'legalCompliance',
  description: 'Legal and regulatory compliance expert',
  tools: [
    regulatoryLookupTool,
    complianceCheckTool,
    legalResearchTool,
    riskMitigationTool
  ],
  system: `You are a legal compliance expert specializing in:
- Regulatory requirements and compliance
- Risk identification and mitigation
- Legal research and analysis
- Policy development and review
Focus on identifying legal risks and ensuring compliance.`,
});

// Technical architecture specialist  
const architectureAgent = ai.definePrompt({
  name: 'technicalArchitect',
  description: 'Technical architecture and system design expert',
  tools: [
    systemDesignTool,
    performanceAnalysisTool,
    securityAssessmentTool,
    scalabilityTool
  ],
  system: `You are a senior technical architect with expertise in:
- System architecture and design patterns
- Performance optimization and scalability
- Security architecture and best practices
- Technology evaluation and selection
Provide detailed technical recommendations with architectural diagrams.`,
});
```

### Quality Assurance Agent
```javascript
const qaAgent = ai.definePrompt({
  name: 'qualityAssurance',
  description: 'Quality assurance and validation specialist',
  tools: [
    validationTool,
    testingTool,
    qualityMetricsTool,
    complianceCheckTool
  ],
  system: `You are a quality assurance specialist responsible for:
- Validating outputs against quality criteria
- Identifying potential issues and risks
- Ensuring compliance with standards
- Providing improvement recommendations
Be thorough and critical in your assessments.`,
});

export const qualityAssuredWorkflow = ai.defineFlow({
  name: 'qualityAssuredWorkflow',
  inputSchema: z.object({
    task: z.string(),
    qualityCriteria: z.array(z.string()),
    requiredApproval: z.boolean().default(true)
  }),
  outputSchema: z.object({
    result: z.string(),
    qualityAssessment: z.object({
      score: z.number(),
      issues: z.array(z.string()),
      recommendations: z.array(z.string()),
      approved: z.boolean()
    }),
    iterations: z.number()
  })
}, async (input) => {
  let iterations = 0;
  let currentResult = '';
  let approved = false;

  while (!approved && iterations < 3) {
    iterations++;
    
    // Generate or refine result
    const workChat = ai.chat(getAppropriateAgent(input.task));
    const workResult = await workChat.send(
      iterations === 1 ? input.task : 
      `Improve this work based on QA feedback: ${currentResult}`
    );
    currentResult = workResult.text;

    // Quality assessment
    const qaChat = ai.chat(qaAgent);
    const qaResult = await qaChat.send(`
      Assess this work against the quality criteria:
      
      Work: ${currentResult}
      Quality Criteria: ${input.qualityCriteria.join(', ')}
      
      Provide score (0-100), identify issues, and give recommendations.
    `);

    const assessment = parseQualityAssessment(qaResult.text);
    
    if (assessment.score >= 80 || !input.requiredApproval) {
      approved = true;
    }

    if (approved || iterations >= 3) {
      return {
        result: currentResult,
        qualityAssessment: {
          ...assessment,
          approved
        },
        iterations
      };
    }
  }
});
```

## Best Practices for Multi-Agent Systems

### Agent Design Principles
1. **Single Responsibility**: Each agent should have a clear, focused purpose
2. **Clear Interfaces**: Define clear input/output contracts between agents
3. **Loose Coupling**: Agents should be independent and composable
4. **Fail Gracefully**: Handle agent failures without breaking the entire system

### Communication Patterns
1. **Message Passing**: Use structured messages for agent communication
2. **Context Sharing**: Share relevant context without overwhelming agents
3. **Async Operations**: Design for asynchronous agent interactions
4. **Conflict Resolution**: Implement mechanisms for resolving disagreements

### Performance Optimization
1. **Parallel Execution**: Run independent agents in parallel when possible
2. **Caching**: Cache agent responses for repeated queries
3. **Load Balancing**: Distribute work across multiple agent instances
4. **Resource Management**: Monitor and limit resource usage per agent

### Monitoring and Debugging
1. **Conversation Tracking**: Log all agent interactions and decisions
2. **Performance Metrics**: Track response times and success rates
3. **Quality Metrics**: Monitor output quality across agents
4. **Error Handling**: Implement comprehensive error handling and recovery

This guide provides a comprehensive foundation for building sophisticated multi-agent systems in Genkit, from simple delegation patterns to complex collaborative workflows.