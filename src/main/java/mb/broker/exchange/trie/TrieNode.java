package mb.broker.exchange.trie;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TrieNode {
    protected final ConcurrentMap<String, TrieNode> children = new ConcurrentHashMap<>();
    protected List<BlockingQueue<String>> boundQueues = new ArrayList<>();
    protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
}

