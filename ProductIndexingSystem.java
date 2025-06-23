import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// --- Classe Product (Auxiliar) ---
class Product {
    private int id;
    private String name;
    private String category;

    public Product(int id, String name, String category) {
        this.id = id;
        this.name = name;
        this.category = category;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    @Override
    public String toString() {
        return "ID: " + id + ", Nome: " + name + ", Categoria: " + category;
    }
}

// --- Classe Node (Base para B+ e B*) ---
class Node {
    protected List<Integer> keys;
    protected boolean isLeaf;
    protected Node parent;
    protected int maxKeys;

    public Node(int maxKeys, boolean isLeaf) {
        this.keys = new ArrayList<>();
        this.isLeaf = isLeaf;
        this.maxKeys = maxKeys;
        this.parent = null;
    }

    public boolean isFull() {
        return keys.size() >= maxKeys;
    }

    public boolean hasMinimumKeys() {
        return keys.size() >= (maxKeys + 1) / 2;
    }

    public List<Integer> getKeys() {
        return keys;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public Node getParent() {
        return parent;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }
}

// --- Classe InternalNode (Nó Interno para B+ e B*) ---
class InternalNode extends Node {
    private List<Node> children;

    public InternalNode(int maxKeys) {
        super(maxKeys, false);
        this.children = new ArrayList<>();
    }

    public void addChild(Node child) {
        children.add(child);
        child.setParent(this);
    }

    public void insertChild(int index, Node child) {
        children.add(index, child);
        child.setParent(this);
    }

    public void removeChild(Node child) {
        children.remove(child);
    }

    public Node findChild(int key) {
        int i = 0;
        while (i < keys.size() && key >= keys.get(i)) {
            i++;
        }
        return children.get(i);
    }

    public List<Node> getChildren() {
        return children;
    }

    public Node getChild(int index) {
        return children.get(index);
    }
}

// --- Classe LeafNode (Nó Folha para B+ e B*) ---
class LeafNode extends Node {
    private List<String> values;
    private LeafNode next;
    private LeafNode previous;

    public LeafNode(int maxKeys) {
        super(maxKeys, true);
        this.values = new ArrayList<>();
        this.next = null;
        this.previous = null;
    }

    public void insert(int key, String value) {
        int insertPos = 0;
        while (insertPos < keys.size() && keys.get(insertPos) < key) {
            insertPos++;
        }
        keys.add(insertPos, key);
        values.add(insertPos, value);
    }

    public boolean remove(int key) {
        int index = keys.indexOf(key);
        if (index != -1) {
            keys.remove(index);
            values.remove(index);
            return true;
        }
        return false;
    }

    public String search(int key) {
        int index = keys.indexOf(key);
        return index != -1 ? values.get(index) : null;
    }

    public LeafNode split() {
        int midPoint = keys.size() / 2;
        LeafNode newLeaf = new LeafNode(maxKeys);
        for (int i = midPoint; i < keys.size(); i++) {
            newLeaf.keys.add(keys.get(i));
            newLeaf.values.add(values.get(i));
        }
        keys.subList(midPoint, keys.size()).clear();
        values.subList(midPoint, values.size()).clear();
        newLeaf.next = this.next;
        newLeaf.previous = this;
        if (this.next != null) {
            this.next.previous = newLeaf;
        }
        this.next = newLeaf;
        return newLeaf;
    }

    public List<String> getValues() {
        return values;
    }

    public LeafNode getNext() {
        return next;
    }

    public LeafNode getPrevious() {
        return previous;
    }

    public void setNext(LeafNode next) {
        this.next = next;
    }

    public void setPrevious(LeafNode previous) {
        this.previous = previous;
    }
}

// --- Classe BPlusTree ---
class BPlusTree {
    private Node root;
    private int maxKeys;
    private LeafNode firstLeaf;

    public BPlusTree(int order) {
        this.maxKeys = order - 1;
        this.root = new LeafNode(maxKeys);
        this.firstLeaf = (LeafNode) root;
    }

    public void insert(int key, String value) {
        LeafNode leaf = findLeafNode(key);
        leaf.insert(key, value);
        if (leaf.isFull()) {
            splitLeafNode(leaf);
        }
    }

    public String search(int key) {
        LeafNode leaf = findLeafNode(key);
        return leaf.search(key);
    }

    public boolean remove(int key) {
        LeafNode leaf = findLeafNode(key);
        boolean removed = leaf.remove(key);

        if (removed) {
            if (!leaf.hasMinimumKeys() && leaf != root) {
                handleLeafUnderflow(leaf);
            }
            if (root.getKeys().isEmpty() && !root.isLeaf()) {
                root = ((InternalNode) root).getChild(0);
                root.setParent(null);
            }
        }
        return removed;
    }

    private LeafNode findLeafNode(int key) {
        Node current = root;
        while (!current.isLeaf()) {
            InternalNode internal = (InternalNode) current;
            int i = 0;
            while (i < internal.getKeys().size() && key >= internal.getKeys().get(i)) {
                i++;
            }
            current = internal.getChildren().get(i);
        }
        return (LeafNode) current;
    }

    private void splitLeafNode(LeafNode leaf) {
        LeafNode newLeaf = leaf.split();
        int promotedKey = newLeaf.getKeys().get(0);
        if (leaf == root) {
            InternalNode newRoot = new InternalNode(maxKeys);
            newRoot.getKeys().add(promotedKey);
            newRoot.addChild(leaf);
            newRoot.addChild(newLeaf);
            root = newRoot;
        } else {
            insertIntoParent(leaf, promotedKey, newLeaf);
        }
    }

    private void insertIntoParent(Node leftChild, int key, Node rightChild) {
        InternalNode parent = (InternalNode) leftChild.getParent();
        if (parent == null) {
            InternalNode newRoot = new InternalNode(maxKeys);
            newRoot.getKeys().add(key);
            newRoot.addChild(leftChild);
            newRoot.addChild(rightChild);
            leftChild.setParent(newRoot);
            rightChild.setParent(newRoot);
            root = newRoot;
            return;
        }

        int insertPos = 0;
        while (insertPos < parent.getKeys().size() && parent.getKeys().get(insertPos) < key) {
            insertPos++;
        }
        parent.getKeys().add(insertPos, key);
        parent.insertChild(insertPos + 1, rightChild);

        if (parent.isFull()) {
            splitInternalNode(parent);
        }
    }

    private void splitInternalNode(InternalNode node) {
        int midPoint = node.getKeys().size() / 2;
        int promotedKey = node.getKeys().get(midPoint);
        InternalNode newInternal = new InternalNode(maxKeys);
        for (int i = midPoint + 1; i < node.getKeys().size(); i++) {
            newInternal.getKeys().add(node.getKeys().get(i));
        }
        for (int i = midPoint + 1; i < node.getChildren().size(); i++) {
            Node child = node.getChildren().get(i);
            newInternal.addChild(child);
        }
        node.getKeys().subList(midPoint, node.getKeys().size()).clear();
        node.getChildren().subList(midPoint + 1, node.getChildren().size()).clear();

        if (node == root) {
            InternalNode newRoot = new InternalNode(maxKeys);
            newRoot.getKeys().add(promotedKey);
            newRoot.addChild(node);
            newRoot.addChild(newInternal);
            root = newRoot;
        } else {
            insertIntoParent(node, promotedKey, newInternal);
        }
    }

    private void handleLeafUnderflow(LeafNode leaf) {
        InternalNode parent = (InternalNode) leaf.getParent();
        int leafIndex = parent.getChildren().indexOf(leaf);

        if (leafIndex > 0) {
            LeafNode leftSibling = (LeafNode) parent.getChildren().get(leafIndex - 1);
            if (leftSibling.getKeys().size() > (maxKeys + 1) / 2) {
                int keyToMove = leftSibling.getKeys().remove(leftSibling.getKeys().size() - 1);
                String valueToMove = leftSibling.getValues().remove(leftSibling.getValues().size() - 1);
                leaf.insert(keyToMove, valueToMove);
                parent.getKeys().set(leafIndex - 1, leaf.getKeys().get(0));
                return;
            }
        }

        if (leafIndex < parent.getChildren().size() - 1) {
            LeafNode rightSibling = (LeafNode) parent.getChildren().get(leafIndex + 1);
            if (rightSibling.getKeys().size() > (maxKeys + 1) / 2) {
                int keyToMove = rightSibling.getKeys().remove(0);
                String valueToMove = rightSibling.getValues().remove(0);
                leaf.insert(keyToMove, valueToMove);
                parent.getKeys().set(leafIndex, rightSibling.getKeys().get(0));
                return;
            }
        }

        if (leafIndex > 0) {
            LeafNode leftSibling = (LeafNode) parent.getChildren().get(leafIndex - 1);
            mergeLeafNodes(leftSibling, leaf, parent, leafIndex - 1);
        } else if (leafIndex < parent.getChildren().size() - 1) {
            LeafNode rightSibling = (LeafNode) parent.getChildren().get(leafIndex + 1);
            mergeLeafNodes(leaf, rightSibling, parent, leafIndex);
        }
    }

    private void mergeLeafNodes(LeafNode leftLeaf, LeafNode rightLeaf, InternalNode parent, int keyIndexInParent) {
        leftLeaf.getKeys().addAll(rightLeaf.getKeys());
        leftLeaf.getValues().addAll(rightLeaf.getValues());
        leftLeaf.setNext(rightLeaf.getNext());
        if (rightLeaf.getNext() != null) {
            rightLeaf.getNext().setPrevious(leftLeaf);
        }
        parent.getKeys().remove(keyIndexInParent);
        parent.removeChild(rightLeaf);

        if (parent != root && !parent.hasMinimumKeys()) {
            handleInternalUnderflow(parent);
        }
    }

    private void handleInternalUnderflow(InternalNode node) {
        InternalNode parent = (InternalNode) node.getParent();
        if (parent == null) {
            if (node.getKeys().isEmpty() && !node.isLeaf()) {
                root = node.getChildren().get(0);
                root.setParent(null);
            }
            return;
        }

        int nodeIndex = parent.getChildren().indexOf(node);

        if (nodeIndex > 0) {
            InternalNode leftSibling = (InternalNode) parent.getChildren().get(nodeIndex - 1);
            if (leftSibling.getKeys().size() > (maxKeys + 1) / 2) {
                int keyFromParent = parent.getKeys().remove(nodeIndex - 1);
                int keyFromSibling = leftSibling.getKeys().remove(leftSibling.getKeys().size() - 1);
                Node childFromSibling = leftSibling.getChildren().remove(leftSibling.getChildren().size() - 1);

                node.getKeys().add(0, keyFromParent);
                node.insertChild(0, childFromSibling);
                childFromSibling.setParent(node);

                parent.getKeys().add(nodeIndex - 1, keyFromSibling);
                return;
            }
        }

        if (nodeIndex < parent.getChildren().size() - 1) {
            InternalNode rightSibling = (InternalNode) parent.getChildren().get(nodeIndex + 1);
            if (rightSibling.getKeys().size() > (maxKeys + 1) / 2) {
                int keyFromParent = parent.getKeys().remove(nodeIndex);
                int keyFromSibling = rightSibling.getKeys().remove(0);
                Node childFromSibling = rightSibling.getChildren().remove(0);

                node.getKeys().add(keyFromParent);
                node.addChild(childFromSibling);
                childFromSibling.setParent(node);

                parent.getKeys().add(nodeIndex, keyFromSibling);
                return;
            }
        }

        if (nodeIndex > 0) {
            InternalNode leftSibling = (InternalNode) parent.getChildren().get(nodeIndex - 1);
            mergeInternalNodes(leftSibling, node, parent, nodeIndex - 1);
        } else if (nodeIndex < parent.getChildren().size() - 1) {
            InternalNode rightSibling = (InternalNode) parent.getChildren().get(nodeIndex + 1);
            mergeInternalNodes(node, rightSibling, parent, nodeIndex);
        }
    }

    private void mergeInternalNodes(InternalNode leftNode, InternalNode rightNode, InternalNode parent, int keyIndexInParent) {
        leftNode.getKeys().add(parent.getKeys().remove(keyIndexInParent));
        leftNode.getKeys().addAll(rightNode.getKeys());
        for (Node child : rightNode.getChildren()) {
            leftNode.addChild(child);
        }
        parent.removeChild(rightNode);

        if (parent != root && !parent.hasMinimumKeys()) {
            handleInternalUnderflow(parent);
        } else if (parent == root && parent.getKeys().isEmpty()) {
            root = leftNode;
            leftNode.setParent(null);
        }
    }

    public void printInOrder() {
        LeafNode current = firstLeaf;
        System.out.print("Chaves em ordem: ");
        while (current != null) {
            for (int i = 0; i < current.getKeys().size(); i++) {
                System.out.print(current.getKeys().get(i) + ":" + current.getValues().get(i) + " ");
            }
            current = current.getNext();
        }
        System.out.println();
    }
}

// --- Classe BStarTree ---
class BStarTree {
    private Node root;
    private int maxKeys;
    private LeafNode firstLeaf;

    public BStarTree(int order) {
        this.maxKeys = order - 1; // Para ordem 3, maxKeys = 2
        this.root = new LeafNode(maxKeys);
        this.firstLeaf = (LeafNode) root;
    }

    public void insert(int key, String value) {
        LeafNode leaf = findLeafNode(key);
        leaf.insert(key, value);

        if (leaf.isFull()) {
            if (!tryRedistributeLeaf(leaf)) {
                splitLeafNode(leaf);
            }
        }
    }

    public String search(int key) {
        LeafNode leaf = findLeafNode(key);
        return leaf.search(key);
    }

    public boolean remove(int key) {
        LeafNode leaf = findLeafNode(key);
        boolean removed = leaf.remove(key);

        if (removed) {
            int minKeysBStar = (int) Math.ceil((double) 2 * maxKeys / 3);
            if (leaf != root && leaf.getKeys().size() < minKeysBStar) {
                if (!tryRedistributeLeafOnRemove(leaf)) {
                    handleLeafUnderflow(leaf);
                }
            }
            if (root.getKeys().isEmpty() && !root.isLeaf()) {
                root = ((InternalNode) root).getChild(0);
                root.setParent(null);
            }
        }
        return removed;
    }

    private LeafNode findLeafNode(int key) {
        Node current = root;
        while (!current.isLeaf()) {
            InternalNode internal = (InternalNode) current;
            int i = 0;
            while (i < internal.getKeys().size() && key >= internal.getKeys().get(i)) {
                i++;
            }
            current = internal.getChildren().get(i);
        }
        return (LeafNode) current;
    }

    private boolean tryRedistributeLeaf(LeafNode leaf) {
        InternalNode parent = (InternalNode) leaf.getParent();
        if (parent == null) return false;

        int leafIndex = parent.getChildren().indexOf(leaf);

        if (leafIndex > 0) {
            LeafNode leftSibling = (LeafNode) parent.getChildren().get(leafIndex - 1);
            if (leftSibling.getKeys().size() < maxKeys) {
                int keyToMove = leaf.getKeys().remove(0);
                String valueToMove = leaf.getValues().remove(0);
                leftSibling.insert(keyToMove, valueToMove);
                parent.getKeys().set(leafIndex - 1, leaf.getKeys().get(0));
                return true;
            }
        }

        if (leafIndex < parent.getChildren().size() - 1) {
            LeafNode rightSibling = (LeafNode) parent.getChildren().get(leafIndex + 1);
            if (rightSibling.getKeys().size() < maxKeys) {
                int keyToMove = leaf.getKeys().remove(leaf.getKeys().size() - 1);
                String valueToMove = leaf.getValues().remove(leaf.getValues().size() - 1);
                rightSibling.insert(keyToMove, valueToMove);
                parent.getKeys().set(leafIndex, rightSibling.getKeys().get(0));
                return true;
            }
        }
        return false;
    }

    private void splitLeafNode(LeafNode leaf) {
        LeafNode newLeaf = leaf.split();
        int promotedKey = newLeaf.getKeys().get(0);

        if (leaf == root) {
            InternalNode newRoot = new InternalNode(maxKeys);
            newRoot.getKeys().add(promotedKey);
            newRoot.addChild(leaf);
            newRoot.addChild(newLeaf);
            root = newRoot;
        } else {
            insertIntoParent(leaf, promotedKey, newLeaf);
        }
    }

    private void insertIntoParent(Node leftChild, int key, Node rightChild) {
        InternalNode parent = (InternalNode) leftChild.getParent();
        if (parent == null) {
            InternalNode newRoot = new InternalNode(maxKeys);
            newRoot.getKeys().add(key);
            newRoot.addChild(leftChild);
            newRoot.addChild(rightChild);
            leftChild.setParent(newRoot);
            rightChild.setParent(newRoot);
            root = newRoot;
            return;
        }

        int insertPos = 0;
        while (insertPos < parent.getKeys().size() && parent.getKeys().get(insertPos) < key) {
            insertPos++;
        }
        parent.getKeys().add(insertPos, key);
        parent.insertChild(insertPos + 1, rightChild);

        if (parent.isFull()) {
            if (!tryRedistributeInternal(parent)) {
                splitInternalNode(parent);
            }
        }
    }

    private boolean tryRedistributeInternal(InternalNode node) {
        InternalNode parent = (InternalNode) node.getParent();
        if (parent == null) return false;

        int nodeIndex = parent.getChildren().indexOf(node);

        if (nodeIndex > 0) {
            InternalNode leftSibling = (InternalNode) parent.getChildren().get(nodeIndex - 1);
            if (leftSibling.getKeys().size() < maxKeys) {
                int keyFromParent = parent.getKeys().remove(nodeIndex - 1);
                leftSibling.getKeys().add(keyFromParent);
                int keyToParent = node.getKeys().remove(0);
                parent.getKeys().add(nodeIndex - 1, keyToParent);
                Node childToMove = node.getChildren().remove(0);
                leftSibling.addChild(childToMove);
                childToMove.setParent(leftSibling);
                return true;
            }
        }

        if (nodeIndex < parent.getChildren().size() - 1) {
            InternalNode rightSibling = (InternalNode) parent.getChildren().get(nodeIndex + 1);
            if (rightSibling.getKeys().size() < maxKeys) {
                int keyFromParent = parent.getKeys().remove(nodeIndex);
                node.getKeys().add(keyFromParent);
                int keyToParent = rightSibling.getKeys().remove(0);
                parent.getKeys().add(nodeIndex, keyToParent);
                Node childToMove = rightSibling.getChildren().remove(0);
                node.addChild(childToMove);
                childToMove.setParent(node);
                return true;
            }
        }
        return false;
    }

    private void splitInternalNode(InternalNode node) {
        int midPoint = node.getKeys().size() / 2;
        int promotedKey = node.getKeys().get(midPoint);
        InternalNode newInternal = new InternalNode(maxKeys);

        for (int i = midPoint + 1; i < node.getKeys().size(); i++) {
            newInternal.getKeys().add(node.getKeys().get(i));
        }
        for (int i = midPoint + 1; i < node.getChildren().size(); i++) {
            Node child = node.getChildren().get(i);
            newInternal.addChild(child);
        }

        node.getKeys().subList(midPoint, node.getKeys().size()).clear();
        node.getChildren().subList(midPoint + 1, node.getChildren().size()).clear();

        if (node == root) {
            InternalNode newRoot = new InternalNode(maxKeys);
            newRoot.getKeys().add(promotedKey);
            newRoot.addChild(node);
            newRoot.addChild(newInternal);
            root = newRoot;
        } else {
            insertIntoParent(node, promotedKey, newInternal);
        }
    }

    private boolean tryRedistributeLeafOnRemove(LeafNode leaf) {
        InternalNode parent = (InternalNode) leaf.getParent();
        int leafIndex = parent.getChildren().indexOf(leaf);
        int minKeysBStar = (int) Math.ceil((double) 2 * maxKeys / 3);

        if (leafIndex > 0) {
            LeafNode leftSibling = (LeafNode) parent.getChildren().get(leafIndex - 1);
            if (leftSibling.getKeys().size() > minKeysBStar) {
                int keyToMove = leftSibling.getKeys().remove(leftSibling.getKeys().size() - 1);
                String valueToMove = leftSibling.getValues().remove(leftSibling.getValues().size() - 1);
                leaf.insert(keyToMove, valueToMove);
                parent.getKeys().set(leafIndex - 1, leaf.getKeys().get(0));
                return true;
            }
        }

        if (leafIndex < parent.getChildren().size() - 1) {
            LeafNode rightSibling = (LeafNode) parent.getChildren().get(leafIndex + 1);
            if (rightSibling.getKeys().size() > minKeysBStar) {
                int keyToMove = rightSibling.getKeys().remove(0);
                String valueToMove = rightSibling.getValues().remove(0);
                leaf.insert(keyToMove, valueToMove);
                parent.getKeys().set(leafIndex, rightSibling.getKeys().get(0));
                return true;
            }
        }
        return false;
    }

    private void handleLeafUnderflow(LeafNode leaf) {
        InternalNode parent = (InternalNode) leaf.getParent();
        int leafIndex = parent.getChildren().indexOf(leaf);

        if (leafIndex > 0) {
            LeafNode leftSibling = (LeafNode) parent.getChildren().get(leafIndex - 1);
            mergeLeafNodes(leftSibling, leaf, parent, leafIndex - 1);
        } else if (leafIndex < parent.getChildren().size() - 1) {
            LeafNode rightSibling = (LeafNode) parent.getChildren().get(leafIndex + 1);
            mergeLeafNodes(leaf, rightSibling, parent, leafIndex);
        }
    }

    private void mergeLeafNodes(LeafNode leftLeaf, LeafNode rightLeaf, InternalNode parent, int keyIndexInParent) {
        leftLeaf.getKeys().addAll(rightLeaf.getKeys());
        leftLeaf.getValues().addAll(rightLeaf.getValues());
        leftLeaf.setNext(rightLeaf.getNext());
        if (rightLeaf.getNext() != null) {
            rightLeaf.getNext().setPrevious(leftLeaf);
        }
        parent.getKeys().remove(keyIndexInParent);
        parent.removeChild(rightLeaf);

        int minKeysBStarInternal = (int) Math.ceil((double) 2 * maxKeys / 3);
        if (parent != root && parent.getKeys().size() < minKeysBStarInternal) {
            if (!tryRedistributeInternalOnRemove(parent)) {
                handleInternalUnderflow(parent);
            }
        }
    }

    private boolean tryRedistributeInternalOnRemove(InternalNode node) {
        InternalNode parent = (InternalNode) node.getParent();
        if (parent == null) return false;

        int nodeIndex = parent.getChildren().indexOf(node);
        int minKeysBStarInternal = (int) Math.ceil((double) 2 * maxKeys / 3);

        if (nodeIndex > 0) {
            InternalNode leftSibling = (InternalNode) parent.getChildren().get(nodeIndex - 1);
            if (leftSibling.getKeys().size() > minKeysBStarInternal) {
                int keyFromParent = parent.getKeys().remove(nodeIndex - 1);
                node.getKeys().add(0, keyFromParent);
                int keyFromSibling = leftSibling.getKeys().remove(leftSibling.getKeys().size() - 1);
                parent.getKeys().add(nodeIndex - 1, keyFromSibling);
                Node childToMove = leftSibling.getChildren().remove(leftSibling.getChildren().size() - 1);
                node.insertChild(0, childToMove);
                childToMove.setParent(node);
                return true;
            }
        }

        if (nodeIndex < parent.getChildren().size() - 1) {
            InternalNode rightSibling = (InternalNode) parent.getChildren().get(nodeIndex + 1);
            if (rightSibling.getKeys().size() > minKeysBStarInternal) {
                int keyFromParent = parent.getKeys().remove(nodeIndex);
                node.getKeys().add(keyFromParent);
                int keyToParent = rightSibling.getKeys().remove(0);
                parent.getKeys().add(nodeIndex, keyToParent);
                Node childToMove = rightSibling.getChildren().remove(0);
                node.addChild(childToMove);
                childToMove.setParent(node);
                return true;
            }
        }
        return false;
    }

    private void handleInternalUnderflow(InternalNode node) {
        InternalNode parent = (InternalNode) node.getParent();
        if (parent == null) {
            if (node.getKeys().isEmpty() && !node.isLeaf()) {
                root = node.getChildren().get(0);
                root.setParent(null);
            }
            return;
        }

        int nodeIndex = parent.getChildren().indexOf(node);

        if (nodeIndex > 0) {
            InternalNode leftSibling = (InternalNode) parent.getChildren().get(nodeIndex - 1);
            mergeInternalNodes(leftSibling, node, parent, nodeIndex - 1);
        } else if (nodeIndex < parent.getChildren().size() - 1) {
            InternalNode rightSibling = (InternalNode) parent.getChildren().get(nodeIndex + 1);
            mergeInternalNodes(node, rightSibling, parent, nodeIndex);
        }
    }

    private void mergeInternalNodes(InternalNode leftNode, InternalNode rightNode, InternalNode parent, int keyIndexInParent) {
        leftNode.getKeys().add(parent.getKeys().remove(keyIndexInParent));
        leftNode.getKeys().addAll(rightNode.getKeys());
        for (Node child : rightNode.getChildren()) {
            leftNode.addChild(child);
        }
        parent.removeChild(rightNode);

        int minKeysBStarInternal = (int) Math.ceil((double) 2 * maxKeys / 3);
        if (parent != root && parent.getKeys().size() < minKeysBStarInternal) {
            if (!tryRedistributeInternalOnRemove(parent)) {
                handleInternalUnderflow(parent);
            }
        } else if (parent == root && parent.getKeys().isEmpty()) {
            root = leftNode;
            leftNode.setParent(null);
        }
    }

    public void printInOrder() {
        LeafNode current = firstLeaf;
        System.out.print("Chaves em ordem: ");
        while (current != null) {
            for (int i = 0; i < current.getKeys().size(); i++) {
                System.out.print(current.getKeys().get(i) + ":" + current.getValues().get(i) + " ");
            }
            current = current.getNext();
        }
        System.out.println();
    }
}

// --- Classe Principal: ProductIndexingSystem ---
public class ProductIndexingSystem {
    private BPlusTree bPlusProductIndex;
    private BStarTree bStarProductIndex;
    private static final int ORDER = 3;
    private static final String TXT_FILE = "produtos_corrigido.txt"; // Alterado para .txt

    public ProductIndexingSystem() {
        this.bPlusProductIndex = new BPlusTree(ORDER);
        this.bStarProductIndex = new BStarTree(ORDER);
    }

    private List<Product> loadProductsFromTxt() {
        List<Product> products = new ArrayList<>();
        String line;
        try (BufferedReader br = new BufferedReader(new FileReader(TXT_FILE))) {
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length == 3) {
                    int id = Integer.parseInt(data[0].trim());
                    String name = data[1].trim();
                    String category = data[2].trim();
                    products.add(new Product(id, name, category));
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler o arquivo TXT: " + e.getMessage());
        }
        return products;
    }

    public static void main(String[] args) {
        ProductIndexingSystem system = new ProductIndexingSystem();
        List<Product> productsToProcess = system.loadProductsFromTxt();

        if (productsToProcess.isEmpty()) {
            System.out.println("Nenhum produto carregado do arquivo TXT. Verifique 'produtos_corrigido.txt'.");
            return;
        }

        // --- Teste com Árvore B+ ---
        System.out.println("--- Executando com Árvore B+ (Ordem " + ORDER + ") ---");
        long startTimeInsertBPlus = System.nanoTime();
        for (Product product : productsToProcess) {
            system.bPlusProductIndex.insert(product.getId(), product.getName() + " (" + product.getCategory() + ")");
        }
        long endTimeInsertBPlus = System.nanoTime();
        long durationInsertBPlus = (endTimeInsertBPlus - startTimeInsertBPlus) / 1_000_000;
        System.out.println("Tempo para inserir " + productsToProcess.size() + " produtos na Árvore B+: " + durationInsertBPlus + " ms");

        Random random = new Random();
        List<Integer> keysToRemoveBPlus = new ArrayList<>();
        System.out.println("\n--- Removendo 10 produtos aleatórios na Árvore B+ (ID entre 1000 e 2000) ---");
        for (int i = 0; i < 10; i++) {
            keysToRemoveBPlus.add(random.nextInt(1001) + 1000); // Chaves de 1000 a 2000
        }

        long startTimeRemoveBPlus = System.nanoTime();
        for (int key : keysToRemoveBPlus) {
            String productInfo = system.bPlusProductIndex.search(key);
            if (productInfo != null) {
                boolean removed = system.bPlusProductIndex.remove(key);
                System.out.println("Tentando remover ID " + key + ": " + (removed ? "Removido: " + productInfo : "Falha ao remover produto existente."));
            } else {
                System.out.println("Tentando remover ID " + key + ": Produto não encontrado.");
            }
        }
        long endTimeRemoveBPlus = System.nanoTime();
        long durationRemoveBPlus = (endTimeRemoveBPlus - startTimeRemoveBPlus) / 1_000_000;
        System.out.println("Tempo para remover 10 produtos na Árvore B+: " + durationRemoveBPlus + " ms");

        System.out.println("\n" + "---".repeat(20) + "\n"); // Separador

        // --- Teste com Árvore B* ---
        System.out.println("--- Executando com Árvore B* (Ordem " + ORDER + ") ---");
        long startTimeInsertBStar = System.nanoTime();
        for (Product product : productsToProcess) {
            system.bStarProductIndex.insert(product.getId(), product.getName() + " (" + product.getCategory() + ")");
        }
        long endTimeInsertBStar = System.nanoTime();
        long durationInsertBStar = (endTimeInsertBStar - startTimeInsertBStar) / 1_000_000;
        System.out.println("Tempo para inserir " + productsToProcess.size() + " produtos na Árvore B*: " + durationInsertBStar + " ms");

        List<Integer> keysToRemoveBStar = new ArrayList<>();
        System.out.println("\n--- Removendo 10 produtos aleatórios na Árvore B* (ID entre 1000 e 2000) ---");
        for (int i = 0; i < 10; i++) {
            keysToRemoveBStar.add(random.nextInt(1001) + 1000); // Chaves de 1000 a 2000
        }

        long startTimeRemoveBStar = System.nanoTime();
        for (int key : keysToRemoveBStar) {
            String productInfo = system.bStarProductIndex.search(key);
            if (productInfo != null) {
                boolean removed = system.bStarProductIndex.remove(key);
                System.out.println("Tentando remover ID " + key + ": " + (removed ? "Removido: " + productInfo : "Falha ao remover produto existente."));
            } else {
                System.out.println("Tentando remover ID " + key + ": Produto não encontrado.");
            }
        }
        long endTimeRemoveBStar = System.nanoTime();
        long durationRemoveBStar = (endTimeRemoveBStar - startTimeRemoveBStar) / 1_000_000;
        System.out.println("Tempo para remover 10 produtos na Árvore B*: " + durationRemoveBStar + " ms");
    }
}