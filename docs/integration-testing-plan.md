# Integration Testing Plan

> **Priority**: Must be in place before building more steps
> **Pattern**: Adapted from `~/projects/spring-ai-examples/integration-testing/`

## The Problem

We're building 12 standalone Spring Boot apps. Each must:
1. Compile against Spring AI 2.0.0-M3 + AgentWorks BOM 1.0.2
2. Start up and connect to an LLM
3. Actually work — the agent must call tools and produce a reasonable response
4. Not regress when we change shared code (agent-core, Spring AI version bumps)

Without automated testing, we'll discover breakage during the workshop with 50 people watching.

## Approach: JBang Integration Tests (from spring-ai-examples)

Each step gets:
```
agents/02-tool-calling/
├── src/...
├── pom.xml
└── integration-tests/
    ├── ExampleInfo.json    # Test config: timeout, success patterns, required env vars
    └── RunToolCalling.java # JBang script: start app, send request, validate response
```

### ExampleInfo.json

```json
{
  "timeoutSec": 60,
  "successRegex": ["restaurant", "Barcelona"],
  "requiredEnv": ["OPENAI_API_KEY"],
  "description": "Step 02: Agent with searchRestaurants tool and ToolCallAdvisor"
}
```

### JBang Test Script

Each test script:
1. Builds the module (`./mvnw -pl agents/XX -am package -DskipTests`)
2. Starts the Spring Boot app in the background
3. Waits for startup (polls `/actuator/health` or checks port 8080)
4. Sends a test message via the Inspector chat API (`POST /sessions/{id}/messages`)
5. Waits for the agent response
6. Validates the response contains expected patterns
7. Tears down the app

### Test Runner

A top-level script runs all steps sequentially:
```bash
./scripts/run-workshop-tests.sh           # Run all steps
./scripts/run-workshop-tests.sh 02        # Run specific step
./scripts/run-workshop-tests.sh 02 03 04  # Run specific steps
```

## What Each Step Tests

| Step | Test Input | Expected in Response | Extra Validation |
|------|-----------|---------------------|-----------------|
| 01 (basic-chatbot) | "Hello" | Any non-empty response | App starts |
| 02 (tool-calling) | "Find a restaurant in Eixample" | Restaurant name from our data (e.g. "Cervecería Catalana" or "Teresa Carles") | Tool was called (check events) |
| 03 (guardrails) | "Business dinner for 4, one vegetarian, must be within expense policy" | Restaurant + expense policy result | Multiple tools called |
| 04 (turn-limits) | Same as 03 | Same as 03 | AgentLoopAdvisor governs (check events for turn count) |
| 05 (journal) | Same as 03 | Same + journal file created | `.agent-journal/` directory has JSONL events |
| 06 (mcp) | Same as 03 | Same | MCP server started, tools discovered |
| 07 (memory) | Two-turn conversation | Second response references first | Memory compaction working |
| 08-12 | TBD per step | TBD | TBD |

## Implementation Plan

### Phase 1: Test Infrastructure (do first)

- [ ] Create `integration-tests/` directory structure at repo root
- [ ] Port `IntegrationTestUtils.java` from spring-ai-examples (adapt for this repo's chat API)
- [ ] Create `scripts/run-workshop-tests.sh` runner
- [ ] Create scaffolding script or template for new step tests

### Phase 2: Tests for Existing Steps

- [ ] `agents/basic-chatbot/integration-tests/` — basic startup + chat
- [ ] `agents/02-tool-calling/integration-tests/` — tool invocation verified via events API
- [ ] `agents/03-guardrails/integration-tests/` — multi-tool invocation
- [ ] `agents/04-turn-limits/integration-tests/` — turn limit governance

### Phase 3: CI (GitHub Actions)

- [ ] `.github/workflows/integration-tests.yml`
- [ ] Runs on PR to `progressive-agent-examples` branch
- [ ] Requires `OPENAI_API_KEY` as repository secret
- [ ] Matrix: test each step independently

## Key Differences from spring-ai-examples

| Aspect | spring-ai-examples | This repo |
|--------|-------------------|-----------|
| App type | Various Spring Boot apps | All use the scaffold (`scaffold/agent-core` + `scaffold/inspector`) |
| API | Various (REST, CLI, etc.) | Unified: `POST /sessions/{id}/messages`, `GET /sessions/{id}/events/stream` |
| Validation | Regex + AI validation | Regex on response + events API for tool call verification |
| Build | Each module independent | Shared parent POM, agent-core dependency |

The unified API is an advantage — every step uses the same Inspector endpoints, so the test utility class can be shared across all steps.

## Open Questions

1. Do we need AI validation (Claude-based) for response quality, or is regex sufficient for workshop testing?
2. Should tests run with a real LLM (requires API key) or can we mock? Real LLM is more realistic but costs money per CI run.
3. Do we set up a shared test API key in the repo secrets, or skip CI and only test locally?
