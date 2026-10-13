/**
 * The maths behind core's holograms, kept apart from the game so it can be tested without
 * one: where the panels stand, which panel and point the player aims at, how a carried
 * hologram follows the player, the two-chunk rule, where each panel's parts are, scrolling,
 * and wrapping and editing text.
 *
 * <p>Core's client module ({@code mcagents-core-client}) is the caller; a mod opens a hologram
 * through that module and never needs these classes. Lengths are in blocks unless a method
 * says panel pixels, and angles are Minecraft yaw degrees.
 */
package io.github.mcagentsmods.core.common.hologram;
