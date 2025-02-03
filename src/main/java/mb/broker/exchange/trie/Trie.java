package mb.broker.exchange.trie;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;

/**
 * A Trie implementation for storing and matching hierarchical keys with support for wildcard pattern matching.
 * Keys are represented as strings, with segments separated by "." (e.g., "a.b.c").
 * Each node in the Trie is associated with a key pattern and can store queues. A node's children extend the node's key
 * pattern by one word.
 * <p>
 * Features:
 * <ul>
 *     <li>Supports exact matches for keys</li>
 *     <li>Provides wildcard matching:</li>
 *     <ul>
 *         <li>"*" matches exactly one segment</li>
 *         <li>"#" matches zero or more segments, including an empty sequence.</li>
 *     </ul>
 */
public class Trie {
    private final TrieNode root = new TrieNode();

    /**
     * Inserts a key and binds it to a queue. The key may contain wildcards "*" and "#".
     *
     * @param key   the key to insert
     * @param queue the queue to bind to the key
     */
    public void insert(String key, BlockingQueue<String> queue) {
        validateKey(key);

        TrieNode current = root;
        TrieNode child;
        String[] keySplit = key.split("\\.");

        // Go to matching leaf node or create path to new leaf node
        for (String word : keySplit) {
            try {
                current.lock.writeLock().lock();
                child = current.children.get(word);
                if (child == null) {
                    child = new TrieNode();
                    current.children.put(word, child);
                    if ("#".equals(word)) {
                        child.children.put("#", child);
                    }
                }
            } finally {
                current.lock.writeLock().unlock();
            }
            current = child;
        }

        // Add queue
        current.lock.writeLock().lock();
        try {
            current.boundQueues.add(queue);
        } finally {
            current.lock.writeLock().unlock();
        }
    }

    /**
     * Searches for all queues bound to a given key.
     *
     * @param key the key to search for
     * @return a list of queues bound to the key, empty if no matching queues are found.
     */
    public List<BlockingQueue<String>> search(String key) {
        assert !key.isBlank();
        String[] keySplit = key.split("\\.");

        List<TrieNode> currentPartialMatches = new ArrayList<>();
        currentPartialMatches.add(root);
        for (String word : keySplit) {
            Queue<TrieNode> previousPartialMatches = new LinkedList<>(currentPartialMatches);
            currentPartialMatches.clear();
            while (!previousPartialMatches.isEmpty()) {
                TrieNode current = previousPartialMatches.poll();
                try {
                    current.lock.readLock().lock();

                    // Match exact key segment and wildcards
                    addIfNotNull(currentPartialMatches, current.children.get(word));    // exact
                    addIfNotNull(currentPartialMatches, current.children.get("*"));     // one
                    TrieNode matchZeroOrMore = addIfNotNull(currentPartialMatches, current.children.get("#"));    // zero
                    if (matchZeroOrMore != null) {
                        addIfNotNull(currentPartialMatches, matchZeroOrMore.children.get(word));  // more (match word eventually)
                    }
                } finally {
                    current.lock.readLock().unlock();
                }
            }
        }

        // After iterating over all segments, include matches from "#" wildcards
        List<TrieNode> finalMatches = new ArrayList<>(currentPartialMatches);
        for (TrieNode current : currentPartialMatches) {
            try {
                current.lock.readLock().lock();
                addIfNotNull(finalMatches, current.children.get("#"));
            } finally {
                current.lock.readLock().unlock();
            }
        }

        // Collect queues from all matched nodes
        return finalMatches.stream()
                .flatMap(n -> n.boundQueues.stream())
                .distinct() // Ensure uniqueness
                .toList();
    }

    /**
     * Validates a key to ensure it does not contain invalid patterns like "#.#" or "#.*".
     *
     * @param key the key to validate
     * @throws IllegalArgumentException if the key contains invalid patterns
     */
    private void validateKey(String key) {
        String[] segments = key.split("\\.");

        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];

            // Check for invalid "#" combinations
            if ("#".equals(segment)) {
                if ( i + 1 < segments.length && ( "#".equals(segments[i + 1]) || "*".equals(segments[i + 1]) ) ) {
                    throw new IllegalArgumentException("Invalid key: '#' cannot be followed by '#' or '*'.");
                }
            }

            // Check for mixed or malformed segments
            if (segment.contains("#") && !"#".equals(segment)) {
                throw new IllegalArgumentException("Invalid key: '#' must appear as a standalone segment.");
            }

            if (segment.contains("*") && !"*".equals(segment)) {
                throw new IllegalArgumentException("Invalid key: '*' must appear as a standalone segment.");
            }
        }
    }

    /**
     * Adds a node to the list if it is not null.
     *
     * @param list the list to add to
     * @param node the node to add
     * @return the node
     */
    private TrieNode addIfNotNull(List<TrieNode> list, TrieNode node) {
        if (node != null) {
            list.add(node);
        }
        return node;
    }
}
