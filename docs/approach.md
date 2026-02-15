# Building an Agent, One Layer at a Time

This workshop begins with a working scaffold for exploring AI agents. That scaffold remains in place for the entire workshop and evolves with us. The objective is to develop a precise, operational mental model of how an agent emerges through incremental, layered design.

![Agent Architecture](diagrams/end-to-end-architecture.svg)

The Inspector provides three surfaces. The Chat Panel is where we interact with the agent. The State Viewer exposes the agent's internal state. The Trace Viewer shows the execution events generated as the agent runs. From the beginning, interaction, state, and execution are visible.

On the backend, we run a Spring application that contains an Agent and a Trace Log. In the first sample, the Agent is intentionally minimal, The backend calls a chat model and returns the response. This baseline gives us a clear starting point.

Each new section explores a different aspect of building an agent. The scaffold stays fixed — only the Agent inside the backend evolves. Because the Inspector never changes, we can see exactly what each new capability adds to the system and what it costs.

## Samples
### Hand-Cranked Agentic Loop

This exercise builds intuition by removing automation entirely. The agentic loop is executed manually: constructing the prompt, supplying state, observing the model’s response, and deciding the next action. The model reasons; you orchestrate. By explicitly managing conversation history, capabilities, and state updates, the mechanics of agent behavior become clear while you work directly within the same scaffold that will support all subsequent samples. When this structure is later implemented inside the backend Agent component, it will feel mechanical rather than magical.


