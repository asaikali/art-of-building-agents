# Session Handoff — Progressive Agent Examples

> **Date**: 2026-04-02
> **Branch**: `progressive-agent-examples` on `asaikali/art-of-building-agents`
> **Status**: Steps 01-04 built and compiling. Integration tests in progress.

## What's Done

- Steps 01-04 implemented, compiling on Spring AI 2.0.0-M3 + Spring Boot 4.1.0-M2 + AgentWorks BOM 1.0.2
  - `agents/basic-chatbot/` — Adib's original: bare ChatClient (Step 01)
  - `agents/02-tool-calling/` — System prompt + searchRestaurants + ToolCallAdvisor
  - `agents/03-guardrails/` — Full tool set (search, expense policy, dietary, booking) + constraint-aware prompt
  - `agents/04-turn-limits/` — AgentLoopAdvisor from agent-workflow (first AgentWorks dependency)
- `docs/progressive-examples-plan.md` — Full 12-step plan with library source column
- `docs/integration-testing-plan.md` — Testing strategy (spring-ai-examples JBang pattern)
- `integration-tests/scripts/run-workshop-tests.sh` — Test runner script (written but untested)
- Parent POM imports agentworks-bom 1.0.2 for version management

## What's In Progress

Adding SLF4J logging to handlers so integration tests can grep stdout. Started on 02-tool-calling, not yet committed. The handlers currently write to Session objects (Inspector API) but nothing to stdout — tests need log output.

## What's Next

1. **Finish integration tests** — add logging to all 4 handlers, create test-config.json per step, verify run-workshop-tests.sh works end-to-end
2. **Build Step 05** (AgentLoop + journal) — pom.xml was started then removed. Depends on workflow-agents + journal-core from BOM.
3. **Build Steps 06-12** — MCP, memory, human-in-the-loop, sub-agents, A2A, ACP, wrap path
4. **Push updates** to branch for Adib's review

## Key Architecture

- Each step is a standalone Spring Boot app with `agent-core` dependency
- `AgentHandler` interface: `void onMessage(Session session, AgentMessage message)`
- Inspector UI (Vue.js from agent-core) shows Chat/State/Events panels
- API: `POST /api/sessions` → `POST /api/sessions/{id}/messages` → `GET /api/sessions/{id}/messages`
- Steps 01-03: pure Spring AI. Step 04+: AgentWorks libraries via BOM.
- Spotless (Google Java Format) enforced — run `./mvnw spotless:apply` before committing

## Key Dependencies

| Step | What | Artifact | BOM Version |
|------|------|----------|-------------|
| 01-03 | Spring AI ToolCallAdvisor | `spring-ai-starter-model-openai` | Spring AI 2.0.0-M3 |
| 04 | AgentLoopAdvisor | `io.github.markpollack:workflow-core` | 0.1.0 (via BOM 1.0.2) |
| 05 | MiniAgent + Journal | `workflow-agents` + `journal-core` | 0.1.0 / 0.9.0 |
| 07 | Memory compaction | `io.github.markpollack:memory-advisor` | 0.1.0 |
| 10 | A2A | `org.springaicommunity:spring-ai-a2a-server-autoconfigure` | (not in BOM yet) |
| 11 | ACP | `com.agentclientprotocol:acp-agent-support` | 0.9.0 |

## Companion Research Project

Full workshop research at `~/tuvium/projects/tuvium-workshop-research/` — read CLAUDE.md there for the bigger picture (both workshops, narrative arcs, AgentWorks positioning, all source material).
