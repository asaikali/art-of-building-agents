# The Art of Building Agents 

You’ve probably seen AI agents in action—reasoning through messy problems, orchestrating API calls, recovering from mistakes, and somehow maintaining intent across dozens of steps. At some point you’ve wondered: *how does this actually work—and could I build one?*

This full-day workshop answers those questions from first principles. Through a series of progressively complex examples, you’ll see how a simple request/response interaction evolves into a full agentic system with an explicit loop: **plan → act → observe → decide**.

Rather than treating agents as black boxes, we’ll make the loop visible and connect it directly to the architectural decisions that determine reliability, safety, and testability. A central theme of this workshop is that agents are **systems embedded in systems**. We move beyond prompt design to explore how API design, tool boundaries, and control flow shape what agents are able to do—and how safely they can do it.

> **Note:** This workshop is hands-on and exploratory. Bring your laptop and a willingness to learn. We’ll provide a GitHub repository with prepared examples and API keys for AI services used in the exercises.

### Key Concepts Covered

* **Planning:** How agents reason about goals, create action plans, and decide what to do next.
* **Action:** How agents act in the world by interacting with APIs and external tools.
* **Evaluation:** How agents evaluate progress—and why determining *"Am I getting closer to the goal?"* is a hard problem.
* **Memory Management:** How agents manage the context window across multiple steps, including short- and long-term memory.
* **System Architecture:** How to design agentic systems where architecture, API design, and tool boundaries shape safe behavior.
* **Security:** How agents act securely on behalf of users—and where the risks lie.

### Hands-on Exercises

1. **Building Intuition:** Experimenting with existing agents before writing code.
2. **Spring AI Foundations:** Reviewing key concepts such as tool calling and advisors.
3. **MCP Integration:** Building **Model Context Protocol (MCP)** servers and clients using Spring AI.
4. **The Agentic Loop:** Constructing an explicit loop from first principles.
5. **Evolution:** Evolving a simple chat interaction into a reasoning agent.
6. **API Design:** Designing AI-friendly APIs using hypermedia and server-driven constraints (**Spring HATEOAS**).
7. **Robustness:** Adding termination conditions and error handling for reliable behavior.
8. **Security Mechanisms:** Applying security standards to request and enforce permissions.

### Prerequisites

* Comfortable with **Java** and **Spring Boot**.
* Familiar with foundational AI concepts (models, prompts, embeddings, and tool calling).
* Basic experience with **Spring AI** (key concepts will be reviewed as needed).
* An interest in learning how to architect and build AI agents.

Would you like me to create a stylized header image for this workshop or perhaps draft a LinkedIn post to promote it?
