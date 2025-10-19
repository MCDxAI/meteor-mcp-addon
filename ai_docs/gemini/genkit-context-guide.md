# Genkit Context Guide

## Overview
Genkit's context system provides a powerful way to pass information through your AI workflows without exposing it to the LLM. This creates a secure side channel for authentication, user data, and execution state while maintaining clean separation between user input and system context.

## Context Categories

### Input vs Generation vs Execution Context
```javascript
// INPUT: Direct to LLM
const userQuery = "What's the weather like?";

// GENERATION CONTEXT: Available to LLM but not specific to call
const currentTime = new Date().toISOString();
const userName = "Alice";

// EXECUTION CONTEXT: Available to code but NOT to LLM
const authToken = "bearer_token_123";
const userId = "user_456";
```

## Context Structure and Best Practices

### Recommended Auth Context Format
```javascript
const authContext = {
  auth: {
    uid: "user_unique_id",           // User's unique identifier
    token: { /* decoded claims */ }, // Decoded JWT claims
    rawToken: "jwt_token_string",    // Raw encoded token
    email: "user@example.com",       // User email
    roles: ["admin", "user"],        // User roles/permissions
    // ...any other auth fields
  }
};
```

### Complete Context Example
```javascript
const fullContext = {
  auth: {
    uid: "user_123",
    email: "alice@example.com",
    roles: ["premium_user"],
    subscription: "pro"
  },
  session: {
    id: "session_456",
    startTime: "2024-01-15T10:00:00Z",
    preferences: {
      language: "en",
      timezone: "America/New_York"
    }
  },
  request: {
    ip: "192.168.1.1",
    userAgent: "Mozilla/5.0...",
    traceId: "trace_789"
  },
  business: {
    organizationId: "org_abc",
    plan: "enterprise",
    features: ["advanced_analytics", "custom_models"]
  }
};
```

## Using Context in Actions

### Flows with Context
```javascript
const userSpecificFlow = ai.defineFlow({
  name: 'userSpecificFlow',
  inputSchema: z.object({
    query: z.string(),
    includePersonalData: z.boolean().default(false)
  }),
  outputSchema: z.object({
    response: z.string(),
    personalized: z.boolean()
  })
}, async (input, { context }) => {
  // Validate authentication
  if (!context.auth?.uid) {
    throw new Error("Authentication required");
  }

  // Access user-specific data based on context
  const userPreferences = await getUserPreferences(context.auth.uid);
  
  let prompt = input.query;
  let personalized = false;

  if (input.includePersonalData && context.auth.roles?.includes('premium_user')) {
    const personalData = await getPersonalData(context.auth.uid);
    prompt += `\n\nUser context: ${JSON.stringify(personalData)}`;
    personalized = true;
  }

  const { text } = await ai.generate({
    prompt,
    // Context automatically propagates to generate()
  });

  return {
    response: text,
    personalized
  };
});
```

### Tools with Context Security
```javascript
const searchUserNotes = ai.defineTool({
  name: 'searchUserNotes',
  description: "Search the current user's private notes",
  inputSchema: z.object({
    query: z.string(),
    category: z.string().optional()
  }),
  outputSchema: z.array(z.object({
    id: z.string(),
    title: z.string(),
    content: z.string(),
    createdAt: z.string()
  }))
}, async (input, { context }) => {
  // Security: Ensure user is authenticated
  if (!context.auth?.uid) {
    throw new Error("Must be called by a signed-in user");
  }

  // Security: Only search notes belonging to the authenticated user
  const notes = await searchNotes({
    userId: context.auth.uid,  // Use context, not LLM input
    query: input.query,
    category: input.category
  });

  return notes;
});

const deleteUserNote = ai.defineTool({
  name: 'deleteUserNote',
  description: "Delete a user's note by ID",
  inputSchema: z.object({
    noteId: z.string()
  }),
  outputSchema: z.object({
    success: z.boolean(),
    message: z.string()
  })
}, async (input, { context }) => {
  // Security checks
  if (!context.auth?.uid) {
    throw new Error("Authentication required");
  }

  // Verify note ownership before deletion
  const note = await getNoteById(input.noteId);
  if (!note || note.userId !== context.auth.uid) {
    throw new Error("Note not found or access denied");
  }

  const success = await deleteNote(input.noteId);
  return {
    success,
    message: success ? "Note deleted successfully" : "Failed to delete note"
  };
});
```

### Dotprompt with Context
```handlebars
---
model: googleai/gemini-2.5-flash
input:
  schema:
    userQuery: string
    includePersonalization?: boolean
---
{{#if @auth.uid}}
Hello {{@auth.email}}, 
{{#if includePersonalization}}
Based on your preferences ({{@session.preferences.language}}, {{@session.preferences.timezone}}):
{{/if}}
{{/if}}

{{userQuery}}

{{#if @auth.roles}}
{{#if (includes @auth.roles "premium_user")}}
*Premium features available*
{{/if}}
{{/if}}
```

## Providing Context at Runtime

### Flow Context
```javascript
// Calling a flow with context
const result = await userSpecificFlow(
  { 
    query: "What are my recent activities?",
    includePersonalData: true 
  },
  {
    context: {
      auth: {
        uid: currentUser.uid,
        email: currentUser.email,
        roles: currentUser.roles
      },
      session: {
        id: sessionId,
        preferences: userPreferences
      }
    }
  }
);
```

### Generation Context
```javascript
const { text } = await ai.generate({
  prompt: "Find information about my projects",
  tools: [searchUserProjects, getUserStats],
  context: {
    auth: currentUser,
    request: {
      ip: req.ip,
      userAgent: req.headers['user-agent']
    }
  }
});
```

### Chat Context
```javascript
const chat = ai.chat({
  model: googleAI.model('gemini-2.5-flash'),
  system: "You are a helpful assistant with access to user data",
  tools: [searchUserNotes, getUserCalendar],
});

const response = await chat.send(
  "What's on my schedule today?",
  {
    context: {
      auth: currentUser,
      session: currentSession
    }
  }
);
```

## Context Propagation and Overrides

### Automatic Propagation
```javascript
const parentFlow = ai.defineFlow({
  name: 'parentFlow',
  inputSchema: z.object({ task: z.string() }),
  outputSchema: z.object({ result: z.string() })
}, async (input, { context }) => {
  console.log('Parent context:', context.auth?.uid);
  
  // Context automatically propagates to child flow
  const childResult = await childFlow({ subtask: input.task });
  
  // Context also propagates to generate calls
  const { text } = await ai.generate({
    prompt: `Process this result: ${childResult.output}`,
    tools: [contextAwareTool]  // Tool receives same context
  });
  
  return { result: text };
});

const childFlow = ai.defineFlow({
  name: 'childFlow',
  inputSchema: z.object({ subtask: z.string() }),
  outputSchema: z.object({ output: z.string() })
}, async (input, { context }) => {
  console.log('Child context:', context.auth?.uid); // Same as parent
  
  return { output: `Processed: ${input.subtask}` };
});
```

### Context Overrides
```javascript
const contextOverrideFlow = ai.defineFlow({
  name: 'contextOverrideFlow',
  inputSchema: z.object({ adminTask: z.string() }),
  outputSchema: z.object({ result: z.string() })
}, async (input, { context }) => {
  // Current user context
  console.log('Current user:', context.auth?.uid);
  
  // Override context for admin operations
  const adminResult = await adminOnlyFlow(
    { task: input.adminTask },
    {
      context: {
        ...context,
        auth: {
          ...context.auth,
          roles: ['admin', 'system'],
          elevated: true
        }
      }
    }
  );
  
  // Selective override - merge with existing context
  const auditResult = await auditFlow(
    { action: 'admin_operation', details: adminResult },
    {
      context: {
        ...context,
        audit: {
          timestamp: new Date().toISOString(),
          originalUser: context.auth?.uid,
          elevatedAccess: true
        }
      }
    }
  );
  
  return { result: auditResult.summary };
});
```

## Advanced Context Patterns

### Context Middleware Pattern
```javascript
// Context enrichment middleware
function enrichContext(baseContext, request) {
  return {
    ...baseContext,
    request: {
      ip: request.ip,
      userAgent: request.headers['user-agent'],
      timestamp: new Date().toISOString(),
      traceId: generateTraceId()
    },
    session: {
      id: request.sessionId,
      startTime: request.session?.startTime,
      preferences: request.session?.preferences
    }
  };
}

// Usage in Express middleware
app.use(async (req, res, next) => {
  // Extract auth from JWT
  const authContext = await validateAndExtractAuth(req.headers.authorization);
  
  // Enrich with request context
  req.genkitContext = enrichContext(authContext, req);
  next();
});

app.post('/api/chat', async (req, res) => {
  const response = await chatFlow(
    req.body,
    { context: req.genkitContext }
  );
  res.json(response);
});
```

### Context Validation
```javascript
const ContextSchema = z.object({
  auth: z.object({
    uid: z.string(),
    email: z.string().email(),
    roles: z.array(z.string()),
    verified: z.boolean()
  }),
  session: z.object({
    id: z.string(),
    preferences: z.object({
      language: z.string(),
      timezone: z.string()
    }).optional()
  }).optional()
});

const validatedFlow = ai.defineFlow({
  name: 'validatedFlow',
  inputSchema: z.object({ query: z.string() }),
  outputSchema: z.object({ response: z.string() })
}, async (input, { context }) => {
  // Validate context structure
  const validatedContext = ContextSchema.parse(context);
  
  // Now TypeScript knows the context structure
  const userId = validatedContext.auth.uid;
  const userRoles = validatedContext.auth.roles;
  
  // Proceed with validated context
  return { response: `Hello ${validatedContext.auth.email}` };
});
```

### Role-Based Access Control
```javascript
function requireRoles(requiredRoles: string[]) {
  return (context: any) => {
    if (!context.auth?.uid) {
      throw new Error("Authentication required");
    }
    
    const userRoles = context.auth.roles || [];
    const hasRequiredRole = requiredRoles.some(role => userRoles.includes(role));
    
    if (!hasRequiredRole) {
      throw new Error(`Access denied. Required roles: ${requiredRoles.join(', ')}`);
    }
    
    return true;
  };
}

const adminFlow = ai.defineFlow({
  name: 'adminFlow',
  inputSchema: z.object({ adminAction: z.string() }),
  outputSchema: z.object({ result: z.string() })
}, async (input, { context }) => {
  // Check permissions
  requireRoles(['admin', 'super_admin'])(context);
  
  // Proceed with admin action
  return { result: `Admin action completed: ${input.adminAction}` };
});

const premiumFlow = ai.defineFlow({
  name: 'premiumFlow',
  inputSchema: z.object({ premiumFeature: z.string() }),
  outputSchema: z.object({ result: z.string() })
}, async (input, { context }) => {
  // Check subscription level
  requireRoles(['premium_user', 'enterprise_user'])(context);
  
  return { result: `Premium feature accessed: ${input.premiumFeature}` };
});
```

### Context Scoping for Multi-Tenant Applications
```javascript
const tenantAwareFlow = ai.defineFlow({
  name: 'tenantAwareFlow',
  inputSchema: z.object({ 
    query: z.string(),
    resourceId: z.string()
  }),
  outputSchema: z.object({ 
    data: z.any(),
    tenant: z.string()
  })
}, async (input, { context }) => {
  const tenantId = context.auth?.tenantId;
  if (!tenantId) {
    throw new Error("Tenant context required");
  }
  
  // Ensure resource belongs to tenant
  const resource = await getResource(input.resourceId);
  if (resource.tenantId !== tenantId) {
    throw new Error("Resource not found or access denied");
  }
  
  // All database queries automatically scoped to tenant
  const data = await queryTenantData(tenantId, input.query);
  
  return { data, tenant: tenantId };
});

const multiTenantTool = ai.defineTool({
  name: 'searchTenantData',
  description: 'Search data within the current tenant scope',
  inputSchema: z.object({
    searchTerm: z.string(),
    category: z.string().optional()
  }),
  outputSchema: z.array(z.any())
}, async (input, { context }) => {
  const tenantId = context.auth?.tenantId;
  if (!tenantId) {
    throw new Error("Tenant context required");
  }
  
  // Automatically scoped search
  return await searchWithinTenant(tenantId, input.searchTerm, input.category);
});
```

## Security Best Practices

### 1. Never Trust LLM Input for Security Decisions
```javascript
// ❌ BAD: Using LLM input for security
const badTool = ai.defineTool({
  name: 'deleteFile',
  inputSchema: z.object({
    userId: z.string(),  // ❌ LLM could provide any userId
    fileId: z.string()
  })
}, async (input) => {
  // ❌ Trusting LLM-provided userId
  return await deleteUserFile(input.userId, input.fileId);
});

// ✅ GOOD: Using context for security
const goodTool = ai.defineTool({
  name: 'deleteFile',
  inputSchema: z.object({
    fileId: z.string()  // ✅ Only file ID from LLM
  })
}, async (input, { context }) => {
  // ✅ Using authenticated user ID from context
  const userId = context.auth?.uid;
  if (!userId) throw new Error("Authentication required");
  
  return await deleteUserFile(userId, input.fileId);
});
```

### 2. Validate Context Structure
```javascript
function validateAuthContext(context: any) {
  if (!context.auth?.uid) {
    throw new Error("Missing authentication context");
  }
  
  if (!context.auth.email || !context.auth.verified) {
    throw new Error("Unverified user account");
  }
  
  return context.auth;
}

const secureFlow = ai.defineFlow({
  name: 'secureFlow',
  inputSchema: z.object({ action: z.string() }),
  outputSchema: z.object({ result: z.string() })
}, async (input, { context }) => {
  const auth = validateAuthContext(context);
  
  // Proceed with validated auth context
  return { result: `Action completed for ${auth.email}` };
});
```

### 3. Audit Context Usage
```javascript
const auditedFlow = ai.defineFlow({
  name: 'auditedFlow',
  inputSchema: z.object({ sensitiveAction: z.string() }),
  outputSchema: z.object({ result: z.string() })
}, async (input, { context }) => {
  // Log security-relevant context usage
  await auditLog({
    action: 'sensitive_action',
    userId: context.auth?.uid,
    userEmail: context.auth?.email,
    timestamp: new Date().toISOString(),
    input: input.sensitiveAction,
    ip: context.request?.ip,
    userAgent: context.request?.userAgent
  });
  
  // Proceed with action
  return { result: "Sensitive action completed" };
});
```

## Integration with Existing Guides

### Context + Function Calling (from function-calling-guide.md)
```javascript
// Secure function calling with context
const transferMoney = ai.defineTool({
  name: 'transferMoney',
  description: 'Transfer money between user accounts',
  inputSchema: z.object({
    toAccount: z.string(),
    amount: z.number(),
    memo: z.string().optional()
  }),
  outputSchema: z.object({
    transactionId: z.string(),
    success: z.boolean()
  })
}, async (input, { context }) => {
  // Security: Use context for authentication
  const fromUserId = context.auth?.uid;
  if (!fromUserId) {
    throw new Error("Authentication required for money transfer");
  }
  
  // Security: Check user permissions
  if (!context.auth.roles?.includes('verified_user')) {
    throw new Error("Account verification required");
  }
  
  // Security: Check transfer limits based on user tier
  const userTier = context.auth.subscription || 'basic';
  const maxTransfer = getTransferLimit(userTier);
  
  if (input.amount > maxTransfer) {
    throw new Error(`Transfer amount exceeds limit for ${userTier} users`);
  }
  
  // Execute transfer using context-derived user ID
  const transaction = await executeTransfer({
    fromUserId,  // From context, not LLM
    toAccount: input.toAccount,
    amount: input.amount,
    memo: input.memo
  });
  
  return {
    transactionId: transaction.id,
    success: transaction.success
  };
});
```

### Context + Structured Output (from structured-output-guide.md)
```javascript
const UserDataSchema = z.object({
  personalInfo: z.object({
    name: z.string(),
    email: z.string(),
    preferences: z.object({
      language: z.string(),
      notifications: z.boolean()
    })
  }),
  accountInfo: z.object({
    tier: z.string(),
    joinDate: z.string(),
    lastActive: z.string()
  })
});

const getUserProfileFlow = ai.defineFlow({
  name: 'getUserProfile',
  inputSchema: z.object({
    includePrivateData: z.boolean().default(false)
  }),
  outputSchema: UserDataSchema
}, async (input, { context }) => {
  const userId = context.auth?.uid;
  if (!userId) {
    throw new Error("Authentication required");
  }
  
  const userData = await getUserData(userId);
  
  // Filter sensitive data based on context
  if (!input.includePrivateData || !context.auth.roles?.includes('admin')) {
    delete userData.personalInfo.email;
  }
  
  const { output } = await ai.generate({
    prompt: `Format this user data: ${JSON.stringify(userData)}`,
    output: { schema: UserDataSchema }
  });
  
  return output;
});
```

## Best Practices Summary

### Context Design
1. **Separate concerns**: Keep input, generation context, and execution context distinct
2. **Security first**: Never trust LLM input for security decisions
3. **Validate structure**: Use schemas to validate context structure
4. **Audit usage**: Log security-relevant context access

### Performance
1. **Minimize context size**: Only include necessary data
2. **Cache context**: Avoid repeated context enrichment
3. **Lazy loading**: Load context data only when needed
4. **Context pooling**: Reuse context objects when possible

### Security
1. **Authentication**: Always validate user authentication in context
2. **Authorization**: Check permissions using context, not LLM input
3. **Data scoping**: Automatically scope data access to authenticated user
4. **Audit trails**: Log all security-relevant operations with context

### Integration
1. **Middleware pattern**: Use middleware to enrich context consistently
2. **Context propagation**: Leverage automatic propagation for nested calls
3. **Override carefully**: Only override context when necessary
4. **Type safety**: Use TypeScript and Zod for context validation