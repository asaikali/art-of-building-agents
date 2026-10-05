/**
 * LLM-as-judge constraint checks: the model makes a qualitative call from structured metadata.
 *
 * <p>These checks exist for questions that have nothing to compute — there is no formula for "is
 * this venue right for a client dinner". The model is given compact context (meal purpose,
 * restaurant description, atmosphere, price tier) and asked to judge overall fit, return one of
 * PASS / FAIL / UNSURE, and explain briefly.
 *
 * <p>The prompt instructs the model to use only the provided metadata, avoid inventing facts, and
 * be conservative when evidence is weak — keeping the judgment narrow and inspectable.
 */
package com.example.jarvis.constraints.llmjudge;
