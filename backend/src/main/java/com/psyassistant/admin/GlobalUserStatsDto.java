package com.psyassistant.admin;

/**
 * Summary counts of users across all organizations.
 *
 * @param total   all users
 * @param online  users active within the online window and not blocked
 * @param blocked users that are suspended or temporarily locked
 */
public record GlobalUserStatsDto(long total, long online, long blocked) {
}
