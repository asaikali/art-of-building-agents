This is a curated reading list of the primary papers that define the modern "AI Agent."

The field moved fast, but a few specific papers established the vocabulary we use today (like "Reasoning Traces," "Verbal Reinforcement," and "Tool Use").

### 1. The "Magna Carta" of LLM Agents

If you only read one paper, make it this one. It fundamentally defined how modern LLM agents work by proving that separating "Thinking" from "Doing" creates a smarter system.

* **Title:** **[ReAct: Synergizing Reasoning and Acting in Language Models](https://arxiv.org/abs/2210.03629)** (Yao et al., 2022)
* **Why it's essential:** This paper introduced the **ReAct** pattern. Before this, models either just answered questions (Reasoning) or just executed commands (Acting). This paper proved that interleaving them (`Thought` → `Action` → `Observation`) significantly reduces hallucinations and error propagation.
* **Key Concept:** The "Trace." (e.g., *Thought: I need to search for the weather. Action: Search[Weather]. Observation: It is raining.*)

### 2. The "Simulation" Paper (Memory & Reflection)

This paper went viral for simulating a town of 25 agents, but its technical contribution is the **Memory Stream** architecture, which is now standard for building long-term agent memory.

* **Title:** **[Generative Agents: Interactive Simulacra of Human Behavior](https://arxiv.org/abs/2304.03442)** (Park et al., 2023)
* **Why it's essential:** It defines how an agent "remembers" things over time. It introduces the architecture of **Retrieval** (finding relevant memories), **Reflection** (synthesizing memories into higher-level thoughts), and **Planning** (using those thoughts to act).
* **Key Concept:** The "Retrieval-Reflection-Planning" cycle.

### 3. The "Self-Correction" Paper

Agents often fail on the first try. This paper defined how agents can "heal" themselves without human intervention.

* **Title:** **[Reflexion: Language Agents with Verbal Reinforcement Learning](https://arxiv.org/abs/2303.11366)** (Shinn et al., 2023)
* **Why it's essential:** It introduces the concept of **Verbal Reinforcement**. Instead of updating the model's weights (which is expensive), the agent updates its *context window* with a verbal critique of why it failed (e.g., "I failed because I didn't import the library. I should import it next time.").
* **Key Concept:** Self-Reflection as a learning mechanism.

### 4. The "Tool Use" Papers

These papers define how agents interact with the outside world (APIs, Calculators, Search Engines).

* **Title:** **[Toolformer: Language Models Can Teach Themselves to Use Tools](https://arxiv.org/abs/2302.04761)** (Schick et al., 2023)
* *Why:* It proved LLMs can autonomously decide *when* to use a tool (like a calculator) rather than relying on a human to prompt it.


* **Title:** **[HuggingGPT: Solving AI Tasks with ChatGPT and its Friends](https://arxiv.org/abs/2303.17580)** (Shen et al., 2023)
* *Why:* It introduced the "Controller" architecture, where a central LLM (the brain) manages dozens of smaller specialized AI models (the experts) to solve complex tasks.



### 5. The "Reasoning" Foundations

Agents cannot exist without the ability to plan. These papers define the "Reasoning" step of the Agentic Loop.

* **Title:** **[Chain-of-Thought Prompting Elicits Reasoning in Large Language Models](https://arxiv.org/abs/2201.11903)** (Wei et al., 2022)
* *The Foundation:* The basis for all agent "thinking."


* **Title:** **[Tree of Thoughts: Deliberate Problem Solving with Large Language Models](https://arxiv.org/abs/2305.10601)** (Yao et al., 2023)
* *The Upgrade:* Defines how an agent can "look ahead" and explore multiple future possibilities before committing to an action (like a chess player).



### 6. The Classical Definition (Pre-LLM)

To understand the *theory* (not just the implementation), you should look at the standard textbook definition.

* **Source:** **Artificial Intelligence: A Modern Approach (Russell & Norvig)**
* **Chapter 2: Intelligent Agents.**
* **Definition:** An agent is anything that can be viewed as perceiving its environment through sensors and acting upon that environment through actuators.
* **Key Term:** The **PAGE** description (Percepts, Actions, Goals, Environment).



### Summary: The "Agent Architecture" Reading Order

If you want to build a mental model of how an agent is built, read them in this order:

1. **Reasoning:** *Chain of Thought* (How it thinks)
2. **Looping:** *ReAct* (How it acts)
3. **Refining:** *Reflexion* (How it fixes errors)
4. **Memory:** *Generative Agents* (How it remembers)
5. **Tools:** *Toolformer* (How it uses APIs)

In 2023, the field was focused on **"Proof of Concept"** (proving agents *could* work).
In 2024 and 2025, the focus shifted entirely to **"Engineering Reliability"** (making them actually useful).

The following papers define the current state of the art. They move away from "magic prompts" and towards rigorous systems, flow engineering, and operating systems control.

### 1. The "Flow Engineering" Revolution

These papers argue that letting an LLM "think freely" is often a mistake. Instead, we should force them into strict, test-driven loops.

* **Title:** **[AlphaCodium: From Prompt Engineering to Flow Engineering](https://arxiv.org/abs/2401.08500)** (Ridnik et al., 2024)
* **The Big Idea:** This paper challenged the standard RAG/Agent approach. It proved that a **rigid, multi-stage flow** (e.g., "First generate tests, then generate code, then run tests, then fix code") consistently beats a "smart" agent that tries to figure it all out at once. It coined the term **"Flow Engineering."**
* **Why read it:** It is the playbook for building reliable coding agents today.


* **Title:** **[SWE-agent: Agent-Computer Interfaces Enable Automated Software Engineering](https://arxiv.org/abs/2405.15793)** (Yang et al., Princeton, 2024)
* **The Big Idea:** Just as humans need a UI (User Interface), Agents need an **ACI (Agent-Computer Interface)**. If you let an agent just "bash" a terminal, it gets confused by the output. This paper designed a special interface that gives the agent "clean" inputs and outputs, drastically improving success rates on GitHub issues.
* **Why read it:** It defines how to build the "environment" your agent lives in.



### 2. The "Anti-Agent" Critique

A critical paper that proved "dumber" systems are often better than "smarter" ones.

* **Title:** **[Agentless: Demystifying LLM-based Software Engineering Agents](https://arxiv.org/abs/2407.01489)** (Xia et al., 2024)
* **The Big Idea:** They built a system with **zero** agentic reasoning (no "thinking" loop). It simply followed a hard-coded three-step process: `Locate File`  `Edit Line`  `Run Test`.
* **The Result:** It beat complex agents (like Devin-style architectures) on benchmarks simply because it didn't get distracted or fall into "reasoning loops."
* **Why read it:** A sobering reminder that sometimes a script is better than an AI.



### 3. The "Generalist Platform" Papers

These papers define the architecture for agents that can do *anything* (coding, browsing, command line).

* **Title:** **[OpenHands (formerly OpenDevin): An Open Platform for AI Software Developers](https://arxiv.org/abs/2407.16741)** (Wang et al., 2024)
* **The Big Idea:** This paper open-sourced the architecture of a "Devin-like" agent. It defines the **Event Stream** architecture, where the agent, the user, and the tools all publish events to a single stream that the agent "reads" to decide what to do next.
* **Why read it:** If you want to build a platform, this is the blueprint.


* **Title:** **[OSWorld: Benchmarking Multimodal Agents for Open-Ended Tasks](https://arxiv.org/abs/2404.07972)** (NeurIPS 2024)
* **The Big Idea:** A massive benchmark testing if agents can control a real computer (Ubuntu/Windows) to do tasks like "Open GIMP and resize this photo."
* **The Result:** Humans succeeded 72% of the time. The best agents succeeded **<12%** of the time.
* **Why read it:** It highlights the current limitations of agents when dealing with GUIs (Graphical User Interfaces).



### 4. The 2025 Frontier: Self-Evolving Agents

This is the cutting edge (late 2024/early 2025).

* **Title:** **[Live-SWE-agent: Can Software Engineering Agents Self-Evolve on the Fly?](https://arxiv.org/abs/2511.13646)** (Xia et al., 2025)
* **The Big Idea:** Most agents have their tools hard-coded by humans. This paper proposes an agent that can **write its own tools** while it works. If it realizes "I keep failing at reading this PDF," it writes a Python script to parse PDFs better, saves it to its toolbox, and uses it for the rest of the job.



### Summary Reading Order for 2024-2025

1. **AlphaCodium:** To understand why "Flow" > "Agent."
2. **SWE-agent:** To understand how to design the environment.
3. **Agentless:** To understand the limitations of over-engineering.
4. **Live-SWE-agent:** To see where the future is going (agents that upgrade themselves).