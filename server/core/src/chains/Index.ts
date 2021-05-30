// Copyright (c) Microsoft Corporation.
// Licensed under the MIT license.
//
// Entry point for the task chaining module.
//
// Task chaining allows the platform to generate microtasks of a particular task
// from completed assignments of a different task. This concept will enable work
// providers to easily create validationt tasks for their data collection tasks.

export * from './BaseChainInterface';

// List of chains
export const chainNames = ['SPEECH_VALIDATION'] as const;
export type ChainName = typeof chainNames[number];

/**
 * Chain Status type
 *
 * Defines the current status of a chain.
 *
 * 'ACTIVE': Chain is active and should be executed on completed assignmetns of
 *    the source task
 * 'INACTIVE': Chain is inactive and should not be executed on completed
 *    assignments of the source task
 */
export const chainStatuses = ['ACTIVE', 'INACTIVE'] as const;
export type ChainStatus = typeof chainStatuses[number];
