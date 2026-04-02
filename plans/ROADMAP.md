# Roadmap — Progressive Agent Examples

> **Updated**: 2026-04-02
> **Branch**: `progressive-agent-examples`
> **Stack**: Spring AI 2.0.0-M3 + Spring Boot 4.1.0-M2 + AgentWorks BOM 1.0.3

---

## Done

- [x] Step 01 `basic-chatbot` — Adib's original: bare ChatClient, no tools
- [x] Step 02 `02-tool-calling` — System prompt + searchRestaurants + ToolCallAdvisor
- [x] Step 03 `03-guardrails` — Full tool set + constraint-aware prompt
- [x] Step 04 `04-turn-limits` — AgentLoopAdvisor (workflow-core 0.2.0, first AgentWorks dep)
- [x] Parent POM with agentworks-bom 1.0.3 dependency management
- [x] `docs/progressive-examples-plan.md` — 12-step plan
- [x] `docs/integration-testing-plan.md` — Testing strategy
- [x] `integration-tests/scripts/run-workshop-tests.sh` — Test runner script (written, untested)
- [x] Pushed to `origin/progressive-agent-examples`

## Recently Completed

- [x] Integration tests: `test-config.json` for all steps, deterministic patterns using hardcoded restaurant data
- [x] Fix Step 04: `AgentLoopAdvisor.builder()` requires `toolCallingManager()` in workflow-core 0.2.0
- [x] Step 05 `05-journal` — AgentLoopAdvisor + agent-journal recording to JSONL via `JournalLoopListener`
- [x] Step 06 `06-mcp-server` + `06-mcp-client` — Restaurant tools as MCP server (Streamable HTTP, port 8081), Jarvis discovers tools dynamically via MCP client. MCP SDK 1.1.1.
- [x] Integration test script supports `dependsOn` for multi-process tests (MCP server → client)
- [x] All 6 steps pass `run-workshop-tests.sh`

## Next — Build Remaining Steps

| Step | Module | What It Adds | Artifact |
|------|--------|-------------|----------|
| 07 | `07-memory` | CompactionMemoryAdvisor | agent-memory (not yet extracted) |
| 08 | `08-human-in-the-loop` | AskUserQuestionTool | spring-ai-agent-utils |
| 09 | `09-subagent` | TaskTool delegation | spring-ai-agent-utils |
| 10 | `10-a2a` | Expense policy as A2A agent | spring-ai-a2a |
| 11 | `11-acp` | Jarvis as ACP endpoint | acp-java |
| 12 | `12-wrap-path` | agent-client wrapping CLI agents | agent-client |

## Backlog

- [ ] Add SLF4J logging to handlers (nice-to-have for debugging, not needed for tests)
- [ ] Pre-built PetClinic agent for 2hr workshop (Mark's session)
- [ ] Judge examples: deterministic + LLM-based
- [ ] Slide decks coordination with Adib + Christian
- [ ] Infrastructure: Codespaces/Gitpod, API key strategy
- [ ] Update research project roadmap at `~/tuvium/projects/tuvium-workshop-research/`
