# Ollama Integration Documentation

**Last Updated:** 2026-02-07
**Research Session:** Comprehensive Ollama + Java ecosystem analysis

## Overview

This directory contains complete documentation for integrating Ollama with Java applications, specifically tailored for the meteor-mcp-addon project.

**Ollama** is a platform for running large language models locally. It provides:
- Local LLM execution (no cloud API required)
- OpenAI-compatible API
- Function/tool calling support
- 100+ models available
- Zero cost (runs locally)

---

## Documents in this Directory

### 1. [ollama-java-integration.md](./ollama-java-integration.md)
**Comprehensive integration guide covering:**
- Google GenAI SDK compatibility (not recommended)
- OpenAI API compatibility (fully supported)
- Java library options (Ollama4j recommended)
- Function calling / tool use support
- REST API reference
- Complete code examples

**When to read:** First time learning about Ollama Java integration

### 2. [ollama4j-api-reference.md](./ollama4j-api-reference.md)
**Complete API reference for Ollama4j library:**
- Installation (Maven/Gradle)
- Chat API
- Tool calling (annotation-based & manual)
- Streaming responses
- Model management
- Configuration options
- Best practices

**When to read:** Implementing Ollama4j in code

### 3. [integration-recommendations.md](./integration-recommendations.md)
**Project-specific implementation guide:**
- Architecture recommendations
- Phase-by-phase implementation plan
- Code examples for meteor-mcp-addon
- Gemini vs Ollama comparison
- StarScript integration
- GUI implementation
- Testing strategy

**When to read:** Planning Ollama integration for this project

### 4. [quick-reference.md](./quick-reference.md)
**Quick reference cheat sheet:**
- Installation & setup commands
- Common code snippets
- Model recommendations
- REST API examples
- Troubleshooting
- Best practices

**When to read:** Quick lookup during development

---

## Quick Start

### 1. Install Ollama
```bash
# Download from https://ollama.com/download
# Start server
ollama serve
```

### 2. Pull a Model
```bash
ollama pull llama3.1:8b
```

### 3. Add Dependency (Gradle Kotlin DSL)
```kotlin
dependencies {
    implementation("io.github.ollama4j:ollama4j:1.1.4")
}
```

### 4. Basic Java Code
```java
import io.github.ollama4j.Ollama;
import io.github.ollama4j.models.chat.OllamaChatMessageRole;
import io.github.ollama4j.models.chat.OllamaChatRequest;

Ollama api = new Ollama("http://localhost:11434");

OllamaChatRequest request = OllamaChatRequest.builder()
    .withModel("llama3.1")
    .withMessage(OllamaChatMessageRole.USER, "Hello!")
    .build();

String response = api.chat(request, token -> {}).getResponseModel().getMessage().getResponse();
System.out.println(response);
```

---

## Key Findings Summary

### Question 1: Does Google GenAI SDK work with Ollama?
**Answer:** Technically yes (via custom endpoint), but **NOT recommended**.
- Different API formats (especially function calling)
- Use Ollama4j instead for native support

### Question 2: Does Ollama have OpenAI-compatible API?
**Answer:** **YES**, fully supported at `http://localhost:11434/v1`
- Chat completions: ✅
- Tool calling: ✅
- Streaming: ✅
- Embeddings: ✅

### Question 3: Best Java library for Ollama?
**Answer:** **Ollama4j** (`io.github.ollama4j:ollama4j:1.1.4`)
- Purpose-built for Ollama
- Active development (474 stars)
- Complete feature set
- Annotation-based tool calling
- MCP support

### Question 4: Does Ollama support function calling?
**Answer:** **YES**, native support
- Models: Llama 3.1+, Mistral, Qwen, FunctionGemma
- Format: Same as OpenAI tool calling
- Works with Ollama4j annotation system

### Question 5: REST API format?
**Answer:** See [ollama-java-integration.md](./ollama-java-integration.md#rest-api-format-for-tool-calling)
- Native endpoint: `POST /api/chat`
- OpenAI endpoint: `POST /v1/chat/completions`
- Streaming enabled by default

---

## Recommended Implementation Path

For **meteor-mcp-addon**, follow this approach:

### Phase 1: Add Dependency
```kotlin
// build.gradle.kts
implementation("io.github.ollama4j:ollama4j:1.1.4")
```

### Phase 2: Create Config System
```java
public class OllamaConfig {
    private boolean enabled = false;
    private String host = "http://localhost:11434";
    private String defaultModel = "llama3.1";
    // ... NBT serialization
}
```

### Phase 3: Create Executor
```java
public class OllamaExecutor {
    private final Ollama api;

    public String execute(String model, String prompt) { ... }
    public String executeWithMCP(String model, String prompt, List<MCPServerConnection> servers) { ... }
}
```

### Phase 4: Add StarScript Functions
```java
// {ollama("prompt")}
// {ollama_mcp("prompt with tools")}
```

### Phase 5: Add Chat Commands
```java
// .ollama chat "prompt"
// .ollama-mcp "prompt"
```

### Phase 6: Add GUI Settings
```java
// Meteor GUI > MCP > Ollama Settings
```

**Full implementation details:** See [integration-recommendations.md](./integration-recommendations.md)

---

## Gemini vs Ollama Comparison

| Feature | Gemini | Ollama |
|---------|--------|---------|
| Location | Cloud (Google) | Local machine |
| Cost | Paid (free tier) | Free |
| Privacy | Data → Google | Fully local |
| Speed | Network latency | Very fast (local) |
| Quality | Very high | Good (model-dependent) |
| Internet | Required | Not required |
| Models | Google only | 100+ models |

**Recommendation:** Implement **both** for maximum flexibility.
- Gemini: Complex reasoning, large context
- Ollama: Fast, private, offline, free

---

## Recommended Models for Minecraft Mod

| Use Case | Model | Size | Reason |
|----------|-------|------|--------|
| Chat commands | `llama3.1:8b` | 4.7GB | Best balance |
| HUD elements | `qwen2.5:0.5b` | 400MB | Fastest |
| MCP tool use | `llama3.1:8b` | 4.7GB | Excellent tools |
| Code generation | `qwen2.5-coder:7b` | 4.7GB | Code-specialized |

---

## External Resources

### Official Links
- **Ollama:** https://ollama.com/
- **Ollama GitHub:** https://github.com/ollama/ollama
- **Ollama Docs:** https://docs.ollama.com/
- **Ollama4j GitHub:** https://github.com/ollama4j/ollama4j
- **Ollama4j Docs:** https://ollama4j.github.io/ollama4j/
- **Model Library:** https://ollama.com/library

### Key Documentation
- [OpenAI Compatibility](https://docs.ollama.com/api/openai-compatibility)
- [Tool Calling Guide](https://docs.ollama.com/capabilities/tool-calling)
- [Streaming Guide](https://docs.ollama.com/capabilities/streaming)
- [API Reference](https://github.com/ollama/ollama/blob/main/docs/api.md)

---

## Document Structure

```
AI_DOCS/ollama/
├── README.md                          (this file)
├── ollama-java-integration.md         (comprehensive guide)
├── ollama4j-api-reference.md          (API documentation)
├── integration-recommendations.md     (project-specific)
└── quick-reference.md                 (cheat sheet)
```

---

## Sources

All documentation is based on:
- Official Ollama documentation (docs.ollama.com)
- Ollama4j library documentation (ollama4j.github.io)
- Community resources (Medium, IBM, DeepWiki)
- Maven Central repository information
- GitHub repositories (ollama/ollama, ollama4j/ollama4j)

**Research Date:** 2026-02-07
**Researcher:** Claude Code AI Documentation Specialist

---

## Next Steps

1. **Read:** [integration-recommendations.md](./integration-recommendations.md) for full implementation plan
2. **Install:** Ollama server and pull `llama3.1` model
3. **Add:** Ollama4j dependency to `build.gradle.kts`
4. **Implement:** Phase-by-phase following the recommendations doc
5. **Test:** StarScript functions and chat commands

---

**Documentation Set Version:** 1.0
**Total Documents:** 5 (including this README)
**Total Pages:** ~50 pages of documentation
**Coverage:** Complete (installation → production deployment)
