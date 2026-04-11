/**
 * Stable planning requirements captured from the user during the early phases of the Jarvis
 * workflow.
 *
 * <p>This package is meant to hold the raw inputs that define what the user wants to plan. The
 * central split is:
 *
 * <ul>
 *   <li>{@link com.example.jarvis.requirements.EventRequirements} for requirements that apply to
 *       the meal as a whole
 *   <li>{@link com.example.jarvis.requirements.Attendee} for requirements that vary by person
 * </ul>
 *
 * <p>The main idea is that this package answers one question: what are the requirements? It does
 * not track conversation status, validation output, or derived planning decisions. Keeping this
 * package focused makes the requirements model easier to read, easier to confirm with the user, and
 * easier to reuse in later phases.
 */
package com.example.jarvis.requirements;
