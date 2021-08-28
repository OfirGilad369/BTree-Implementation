import java.util.Arrays;
import java.util.Comparator;

@SuppressWarnings("unchecked")
public class BTree<T extends Comparable<T>> {

    // Default to 2-3 Tree
    private int minKeySize = 1;
    private int minChildrenSize = minKeySize + 1; // 2
    private int maxKeySize = 2 * (minKeySize + 1) - 1; // 2
    private int maxChildrenSize = maxKeySize + 1; // 3

    private Node<T> root = null;
    private int size = 0;

    /**
     * Constructor for B-Tree which defaults to a 2-3 B-Tree.
     */
    public BTree() { }

    /**
     * Constructor for B-Tree of ordered parameter. Order here means minimum 
     * number of keys in a non-root node. 
     * 
     * @param order
     *            of the B-Tree.
     */
    public BTree(int order) {
    	this.minKeySize = order - 1;
        this.minChildrenSize = minKeySize + 1;
        this.maxKeySize = 2 * (minKeySize + 1) - 1;
        this.maxChildrenSize = maxKeySize + 1;
    }
    
    //Task 2.1
    public boolean insert(T value) {
    	
    	Node <T> r = root;
    	
    	//root is null
    	if(r == null) {
    		r = new Node<T>(null, maxKeySize, maxChildrenSize);
            r.addKey(value);
            root = r;
    	}
    	
    	//root is full (in case a 2pass insert was performed)
    	else if (r.numberOfKeys() == maxKeySize) {
    		Node<T> s = new Node<T>(null,maxKeySize,maxChildrenSize);
        	root = s;
        	s.keysSize = 0;
        	s.addChild(r);
        	splitChild(s, 0);
        	insertNonFull(s, value);
    	}
    	
    	//root isn't null and isn't full
    	else {
    		insertNonFull(r, value);
    	}
        size++;
		return true;
    }
    	
    private void insertNonFull(Node<T> x, T k) {
    	
    	int i = x.numberOfKeys();
    	
    	//x is a leaf and isn't full
    	if (x.numberOfChildren() == 0) {
    		
    		while ((i >= 1) && (k.compareTo(x.getKey(i-1)) < 0)) {
    			x.keys[i] = x.keys[i-1];
    			i = i - 1;
    		}
    		x.keys[i] = k;
    		x.keysSize = x.keysSize + 1;
    	}
    	
    	//x has children
    	else {
    		
    		//x isn't full
    		while ((i >= 1) && (k.compareTo(x.getKey(i-1)) <= 0)) {
    			i = i - 1;
    		}
    			
    		//if x child is full split it before entering it
    		if (x.getChild(i).numberOfKeys() == maxKeySize) {
    			splitChild(x, i);
    			
    			if ((k.compareTo(x.getKey(i)) > 0)) {
    				i = i + 1;
    			}
    		}
    		insertNonFull(x.getChild(i), k);  
    	}
    }    
    
    private void splitChild(Node<T> x, int i) {
    	Node<T> y = x.getChild(i);
    	Node<T> z = new Node<T>(null,maxKeySize,maxChildrenSize);
    	
    	for(int j = 0; j < minKeySize; j++) {
    		z.addKey(y.getKey(j + (minKeySize + 1)));
    	}
    	
    	// the missing step (deleting y keys that moved to z)
    	for(int j = 0; j < minKeySize; j++) {
    		y.removeKey(minKeySize + 1);
    	}
    	
    	if(y.numberOfChildren() != 0) {
    		for(int j = 0; j < minKeySize + 1; j++) {
    			z.addChild(y.getChild(j + (minKeySize + 1)));
    		}
    		
    		// the missing step (deleting y children that moved to z)
    		for(int j = 0; j < minKeySize + 1; j++) {
    			y.removeChild(y.childrenSize - 1);
    		}
    	}
    	for(int j = x.numberOfChildren(); j > i; j--) {
    		x.children[j] = x.children[j - 1];
    	}
    	
    	//the missing step (adding z to index i at x children manually)
    	z.parent = x;
    	x.childrenSize++;
        x.children[i] = z;
        
        Arrays.sort(x.children, 0, x.childrenSize, x.comparator);
    	
    	for(int j = x.numberOfKeys(); j > i; j--) {
    		x.keys[j] = x.keys[j - 1];
    	}
    	
    	x.keys[i] = y.keys[minKeySize];
    	y.removeKey(minKeySize);
    	x.keysSize = x.keysSize + 1;
    	
    }
    
    public T delete(T value) {
    	
    	Node <T> r = root;

        int pre_suc_factor=0;
        while(true) {
            if(r.indexOf(value)==-1) {
                //searching for the node that contains "value"
                int next_child_index = r.get_closest_child_to_target(value)+pre_suc_factor;
                r=r.getChild(next_child_index);
                r= fix_node_size_delete(r);
                pre_suc_factor=0;
                continue;
            }
            else {
                if(r.is_leaf()) {
                	T deletedKey = r.removeKey(value);
                	if (deletedKey != null)
                		size--;
                    return deletedKey;
                }
                if(switch_places_with_predecessor(value,r)) {
                    pre_suc_factor=-1;
                    continue;
                }
                if(switch_places_with_successor(value,r)) {
                    pre_suc_factor =1;
                    continue;
                }
                Node<T>left_child = r.getChild(r.indexOf(value));
                Node<T>right_child = r.getChild(r.indexOf(value)+1);
                r = perform_merge_and_update_tree(r,right_child,left_child,value);

            }
        }
    }

    public boolean switch_places_with_predecessor(T value,Node<T> curr_node) {
        Node<T> predecessor_node = get_predecessor_node(value,curr_node);
        if (predecessor_node==null)
            return false;

        //switch places with predecessor and delete
        curr_node.addKey(predecessor_node.getKey(predecessor_node.numberOfKeys()-1));
        curr_node.removeKey(value);
        predecessor_node.removeKey(predecessor_node.numberOfKeys()-1);
        predecessor_node.addKey(value);
        return true;
    }

    public Node<T> get_predecessor_node(T value,Node<T> curr_node) {
    	
    	//Returns the predecessor if deleting with predecessor is possible , otherwise returns null
        int traget_index = curr_node.indexOf(value);
        Node<T> target = curr_node.getChild(traget_index);
        if(target.numberOfKeys()==minKeySize)
            return null;
        while(!target.is_leaf())
            target=target.getChild(target.numberOfChildren()-1);
        return target;
    }

    public boolean switch_places_with_successor(T value,Node<T> curr_node) {
        Node<T> successor_node = get_successor_node(value,curr_node);
        if (successor_node==null)
            return false;

        //switch places with successor and delete
        curr_node.addKey(successor_node.getKey(0));
        curr_node.removeKey(value);
        successor_node.removeKey(0);
        successor_node.addKey(value);
        return true;
    }

    public Node<T> get_successor_node(T value,Node<T> curr_node) {
    	
    	//Returners the predecessor if deleting with predecessor is possible , otherwise returns null
        int traget_index = curr_node.indexOf(value);
        Node<T> target = curr_node.getChild(traget_index+1);
        if(target.numberOfKeys()==minKeySize)
            return null;
        while(!target.is_leaf())
            target=target.getChild(0);
        return target;
    }


    private Node<T> fix_node_size_delete(Node<T> node) {
        if(node.numberOfKeys()==minKeySize) {
            if (try_shift(node))
                return node;
            return merge_with_adjacent_node(node);
        }
        return node;
    }

    private Node<T>  merge_with_adjacent_node(Node<T> node) {
        Node<T> parent = node.parent;
        int node_index =parent.get_closest_child_to_target(node.getKey(0));
        if(node_index!=0)
            node = merge_left(parent,node_index);
        else
            node = merge_right(parent,node_index);
        return node;
    }

    private Node<T> merge_two_nodes(Node<T> left_node, T midKey,Node<T> right_node) {
        Node<T> new_node = new Node<T>(left_node.parent,maxKeySize, maxChildrenSize);
        for(int i=0;i<left_node.numberOfKeys();i++) {
            new_node.addKey(left_node.getKey(i));
            new_node.addChild(left_node.getChild(i));
        }
        new_node.addChild(left_node.getChild(left_node.numberOfKeys()));
        new_node.addKey(midKey);

        for(int i=0;i<left_node.numberOfKeys();i++) {
            new_node.addKey(right_node.getKey(i));
            new_node.addChild(right_node.getChild(i));
        }
        new_node.addChild(right_node.getChild(left_node.numberOfKeys()));

        return new_node;
    }

    private Node<T> merge_left(Node<T> parent,int right_child_index) {
        Node<T> right_node = parent.getChild(right_child_index);
        Node<T> left_node = parent.getChild(right_child_index-1);
        T mid_key = parent.getKey(right_child_index-1);
        return perform_merge_and_update_tree(parent,right_node,left_node,mid_key);
    }

    private Node<T> perform_merge_and_update_tree(Node<T> parent,Node<T> right_node,Node<T> left_node,T mid_key) {
        parent.removeChild(right_node);
        parent.removeChild(left_node);
        parent.removeKey(mid_key);
        Node<T> new_child = merge_two_nodes(left_node,mid_key,right_node);

        if(root.numberOfKeys()==0)
            root=new_child;
        else
            parent.addChild(new_child);
        return new_child;
    }
    
    private Node<T> merge_right(Node<T> parent,int left_child_index) {
        Node<T> right_node = parent.getChild(left_child_index+1);
        Node<T> left_node = parent.getChild(left_child_index);
        T mid_key = parent.removeKey(left_child_index);
        return perform_merge_and_update_tree(parent,right_node,left_node,mid_key);
    }

    private boolean try_shift(Node<T> node) {
    	
    	//trying to use shift method , and returns true if succeeded
        Node<T> parent = node.parent;
        int node_index =parent.get_closest_child_to_target(node.getKey(0));
        if(try_shift_from_left(parent,node_index))
            return true;
        if(try_shift_from_right(parent,node_index))
            return true;
        return false;

    }

    private boolean try_shift_from_right(Node<T> parent,int left_child_index) {
        if(left_child_index==parent.numberOfChildren()-1)
            return false;
        Node<T> right_node = parent.getChild(left_child_index+1);
        if (right_node.numberOfKeys()==minKeySize)
            return false;
        shift_from_right(parent,left_child_index);
        return true;
    }
    
    private void shift_from_right(Node<T> parent,int left_child_index) {
        Node<T> right_node = parent.getChild(left_child_index+1);
        Node<T> left_node = parent.getChild(left_child_index);

        T value_removed_from_right = right_node.removesmallestKey();
        Node<T> child_removed_from_right = right_node.removeSmallestchiled();
        T value_removed_from_parent = parent.removeKey(left_child_index);
        parent.addKey(value_removed_from_right);
        left_node.addKey(value_removed_from_parent);
        left_node.addChild(child_removed_from_right);
    }
    
    private boolean try_shift_from_left(Node<T> parent,int right_child_index) {
        if(right_child_index==0)
            return false;
        Node<T> left_node = parent.getChild(right_child_index-1);
        if (left_node.numberOfKeys()==minKeySize)
            return false;
        shift_from_left(parent,right_child_index-1);
        return true;
    }
    
    private void shift_from_left(Node<T> parent,int left_child_index) {
        Node<T> right_node = parent.getChild(left_child_index+1);
        Node<T> left_node = parent.getChild(left_child_index);

        T value_removed_from_left = left_node.removeBiggestKey();
        Node<T> child_removed_from_left = left_node.removeBiggestchiled();
        T value_removed_from_parent = parent.removeKey(left_child_index);
        parent.addKey(value_removed_from_left);
        right_node.addKey(value_removed_from_parent);
        right_node.addChild(child_removed_from_left);
    }
    
	//Task 2.2
    public boolean insert2pass(T value) {
    	
    	Node <T> r = root;
    	
    	//root is null
    	if(r == null) {
    		r = new Node<T>(null, maxKeySize, maxChildrenSize);
            r.addKey(value);
            root = r;
    	}
    	
    	//root isn't null
    	else {
    		insertNonFull2Pass(r, value);
    	}
        size++;
		return true;
    }
    
    private void insertNonFull2Pass(Node<T> x, T k) {
    	
    	int i = x.numberOfKeys();
    	
    	//x is a leaf
    	if(x.numberOfChildren() == 0) {
    		
    		//x has max number of keys - need to split before insertion
    		if (x.numberOfKeys() == maxKeySize) {
    			splitNode2Pass(x);
        		insertNonFull2Pass(x.parent, k);
    		}
    		else {
    			while ((i >= 1) && (k.compareTo(x.getKey(i-1)) < 0)) {
    				x.keys[i] = x.keys[i-1];
    				i = i - 1;
    			}
    			x.keys[i] = k;
    			x.keysSize = x.keysSize + 1;
    			//disk write
    		}
    	}
    	
    	//x has children
    	else {
    		while ((i >= 1) && (k.compareTo(x.getKey(i-1)) <= 0)) {
    			i = i - 1;
    		}
    		
    		insertNonFull2Pass(x.getChild(i), k);    		
    	}
    }
    
    //split way up till it's safe to split x
    private void splitNode2Pass (Node<T> x) {
    	if (x.numberOfKeys() == maxKeySize) {
			if (x.parent != null) {
				if (x.parent.numberOfKeys() == maxKeySize) {
					splitNode2Pass(x.parent);
				}
				splitChild2Pass(x.parent, x.parent.indexOf(x));
			}
			//x is the root 
			else {
				Node <T> r = root;
				Node<T> s = new Node<T>(null,maxKeySize,maxChildrenSize);
    			root = s;
    			s.keysSize = 0;
    			s.addChild(r);
    			splitChild2Pass(s, 0);
			}
		}
    }
    
    //doesn't split if x goes max sized
    private void splitChild2Pass(Node<T> x, int i) {
    	
    	Node<T> y = x.getChild(i);
    	Node<T> z = new Node<T>(null,maxKeySize,maxChildrenSize);
    	
    	for(int j = 0; j < minKeySize; j++) {
    		z.addKey(y.getKey(j + (minKeySize + 1)));
    	}
    	
    	// the missing step (deleting y keys that moved to z)
    	for(int j = 0; j < minKeySize; j++) {
    		y.removeKey(minKeySize + 1);
    	}
    	
    	if(y.numberOfChildren() != 0) {
    		for(int j = 0; j < minKeySize + 1; j++) {
    			z.addChild(y.getChild(j + (minKeySize + 1)));
    		}
    		
    		// the missing step (deleting y children that moved to z)
    		for(int j = 0; j < minKeySize + 1; j++) {
    			y.removeChild(y.childrenSize - 1);
    		}
    	}
    	for(int j = x.numberOfChildren(); j > i; j--) {
    		x.children[j] = x.children[j - 1];
    	}
    	
    	//the missing step (adding z to index i at x children manually)
    	z.parent = x;
    	x.childrenSize++;
        x.children[i] = z;
        
        Arrays.sort(x.children, 0, x.childrenSize, x.comparator);
    	
    	for(int j = x.numberOfKeys(); j > i; j--) {
    		x.keys[j] = x.keys[j - 1];
    	}
    	
    	x.keys[i] = y.keys[minKeySize];
    	y.removeKey(minKeySize);
    	x.keysSize = x.keysSize + 1;
    }      
    
    /**
     * {@inheritDoc}
     */
    public boolean add(T value) {
        if (root == null) {
            root = new Node<T>(null, maxKeySize, maxChildrenSize);
            root.addKey(value);
        } else {
            Node<T> node = root;
            while (node != null) {
                if (node.numberOfChildren() == 0) {
                    node.addKey(value);
                    if (node.numberOfKeys() <= maxKeySize) {
                        // A-OK
                        break;
                    }                         
                    // Need to split up
                    split(node);
                    break;
                }
                // Navigate

                // Lesser or equal
                T lesser = node.getKey(0);
                if (value.compareTo(lesser) <= 0) {
                    node = node.getChild(0);
                    continue;
                }

                // Greater
                int numberOfKeys = node.numberOfKeys();
                int last = numberOfKeys - 1;
                T greater = node.getKey(last);
                if (value.compareTo(greater) > 0) {
                    node = node.getChild(numberOfKeys);
                    continue;
                }

                // Search internal nodes
                for (int i = 1; i < node.numberOfKeys(); i++) {
                    T prev = node.getKey(i - 1);
                    T next = node.getKey(i);
                    if (value.compareTo(prev) > 0 && value.compareTo(next) <= 0) {
                        node = node.getChild(i);
                        break;
                    }
                }
            }
        }

        size++;

        return true;
    }

    /**
     * The node's key size is greater than maxKeySize, split down the middle.
     * 
     * @param nodeToSplit
     *            to split.
     */
    private void split(Node<T> nodeToSplit) {
        Node<T> node = nodeToSplit;
        int numberOfKeys = node.numberOfKeys();
        int medianIndex = numberOfKeys / 2;
        T medianValue = node.getKey(medianIndex);

        Node<T> left = new Node<T>(null, maxKeySize, maxChildrenSize);
        for (int i = 0; i < medianIndex; i++) {
            left.addKey(node.getKey(i));
        }
        if (node.numberOfChildren() > 0) {
            for (int j = 0; j <= medianIndex; j++) {
                Node<T> c = node.getChild(j);
                left.addChild(c);
            }
        }

        Node<T> right = new Node<T>(null, maxKeySize, maxChildrenSize);
        for (int i = medianIndex + 1; i < numberOfKeys; i++) {
            right.addKey(node.getKey(i));
        }
        if (node.numberOfChildren() > 0) {
            for (int j = medianIndex + 1; j < node.numberOfChildren(); j++) {
                Node<T> c = node.getChild(j);
                right.addChild(c);
            }
        }

        if (node.parent == null) {
            // new root, height of tree is increased
            Node<T> newRoot = new Node<T>(null, maxKeySize, maxChildrenSize);
            newRoot.addKey(medianValue);
            node.parent = newRoot;
            root = newRoot;
            node = root;
            node.addChild(left);
            node.addChild(right);
        } else {
            // Move the median value up to the parent
            Node<T> parent = node.parent;
            parent.addKey(medianValue);
            parent.removeChild(node);
            parent.addChild(left);
            parent.addChild(right);
            
            if (parent.numberOfKeys() > maxKeySize) split(parent);
        }
    }

    /**
     * {@inheritDoc}
     */
    public T remove(T value) {
        T removed = null;
        Node<T> node = this.getNode(value);
        removed = remove(value,node);
        return removed;
    }

    /**
     * Remove the value from the Node and check invariants
     * 
     * @param value
     *            T to remove from the tree
     * @param node
     *            Node to remove value from
     * @return True if value was removed from the tree.
     */
    private T remove(T value, Node<T> node) {
        if (node == null) return null;

        T removed = null;
        int index = node.indexOf(value);
        removed = node.removeKey(value);
        if (node.numberOfChildren() == 0) {
            // leaf node
            if (node.parent != null && node.numberOfKeys() < minKeySize) {
                this.combined(node);
            } else if (node.parent == null && node.numberOfKeys() == 0) {
                // Removing root node with no keys or children
                root = null;
            }
        } else {
            // internal node
            Node<T> lesser = node.getChild(index);
            Node<T> greatest = this.getGreatestNode(lesser);
            T replaceValue = this.removeGreatestValue(greatest);
            node.addKey(replaceValue);
            if (greatest.parent != null && greatest.numberOfKeys() < minKeySize) {
                this.combined(greatest);
            }
            if (greatest.numberOfChildren() > maxChildrenSize) {
                this.split(greatest);
            }
        }

        size--;

        return removed;
    }

    /**
     * Remove greatest valued key from node.
     * 
     * @param node
     *            to remove greatest value from.
     * @return value removed;
     */
    private T removeGreatestValue(Node<T> node) {
        T value = null;
        if (node.numberOfKeys() > 0) {
            value = node.removeKey(node.numberOfKeys() - 1);
        }
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public void clear() {
        root = null;
        size = 0;
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(T value) {
        Node<T> node = getNode(value);
        return (node != null);
    }

    /**
     * Get the node with value.
     * 
     * @param value
     *            to find in the tree.
     * @return Node<T> with value.
     */
    private Node<T> getNode(T value) {
        Node<T> node = root;
        while (node != null) {
            T lesser = node.getKey(0);
            if (value.compareTo(lesser) < 0) {
                if (node.numberOfChildren() > 0)
                    node = node.getChild(0);
                else
                    node = null;
                continue;
            }

            int numberOfKeys = node.numberOfKeys();
            int last = numberOfKeys - 1;
            T greater = node.getKey(last);
            if (value.compareTo(greater) > 0) {
                if (node.numberOfChildren() > numberOfKeys)
                    node = node.getChild(numberOfKeys);
                else
                    node = null;
                continue;
            }

            for (int i = 0; i < numberOfKeys; i++) {
                T currentValue = node.getKey(i);
                if (currentValue.compareTo(value) == 0) {
                    return node;
                }

                int next = i + 1;
                if (next <= last) {
                    T nextValue = node.getKey(next);
                    if (currentValue.compareTo(value) < 0 && nextValue.compareTo(value) > 0) {
                        if (next < node.numberOfChildren()) {
                            node = node.getChild(next);
                            break;
                        }
                        return null;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Get the greatest valued child from node.
     * 
     * @param nodeToGet
     *            child with the greatest value.
     * @return Node<T> child with greatest value.
     */
    private Node<T> getGreatestNode(Node<T> nodeToGet) {
        Node<T> node = nodeToGet;
        while (node.numberOfChildren() > 0) {
            node = node.getChild(node.numberOfChildren() - 1);
        }
        return node;
    }
    
    //the opposite of getGreatestNode
    private Node<T> getSmallestNode(Node<T> nodeToGet) {
        Node<T> node = nodeToGet;
        while (node.numberOfChildren() > 0) {
            node = node.getChild(0);
        }
        return node;
    }

    /**
     * Combined children keys with parent when size is less than minKeySize.
     * 
     * @param node
     *            with children to combined.
     * @return True if combined successfully.
     */
    private boolean combined(Node<T> node) {
        Node<T> parent = node.parent;
        int index = parent.indexOf(node);
        int indexOfLeftNeighbor = index - 1;
        int indexOfRightNeighbor = index + 1;

        Node<T> rightNeighbor = null;
        int rightNeighborSize = -minChildrenSize;
        if (indexOfRightNeighbor < parent.numberOfChildren()) {
            rightNeighbor = parent.getChild(indexOfRightNeighbor);
            rightNeighborSize = rightNeighbor.numberOfKeys();
        }

        // Try to borrow neighbor
        if (rightNeighbor != null && rightNeighborSize > minKeySize) {
            // Try to borrow from right neighbor
            T removeValue = rightNeighbor.getKey(0);
            int prev = getIndexOfPreviousValue(parent, removeValue);
            T parentValue = parent.removeKey(prev);
            T neighborValue = rightNeighbor.removeKey(0);
            node.addKey(parentValue);
            parent.addKey(neighborValue);
            if (rightNeighbor.numberOfChildren() > 0) {
                node.addChild(rightNeighbor.removeChild(0));
            }
        } else {
            Node<T> leftNeighbor = null;
            int leftNeighborSize = -minChildrenSize;
            if (indexOfLeftNeighbor >= 0) {
                leftNeighbor = parent.getChild(indexOfLeftNeighbor);
                leftNeighborSize = leftNeighbor.numberOfKeys();
            }

            if (leftNeighbor != null && leftNeighborSize > minKeySize) {
                // Try to borrow from left neighbor
                T removeValue = leftNeighbor.getKey(leftNeighbor.numberOfKeys() - 1);
                int prev = getIndexOfNextValue(parent, removeValue);
                T parentValue = parent.removeKey(prev);
                T neighborValue = leftNeighbor.removeKey(leftNeighbor.numberOfKeys() - 1);
                node.addKey(parentValue);
                parent.addKey(neighborValue);
                if (leftNeighbor.numberOfChildren() > 0) {
                    node.addChild(leftNeighbor.removeChild(leftNeighbor.numberOfChildren() - 1));
                }
            } else if (rightNeighbor != null && parent.numberOfKeys() > 0) {
                // Can't borrow from neighbors, try to combined with right neighbor
                T removeValue = rightNeighbor.getKey(0);
                int prev = getIndexOfPreviousValue(parent, removeValue);
                T parentValue = parent.removeKey(prev);
                parent.removeChild(rightNeighbor);
                node.addKey(parentValue);
                for (int i = 0; i < rightNeighbor.keysSize; i++) {
                    T v = rightNeighbor.getKey(i);
                    node.addKey(v);
                }
                for (int i = 0; i < rightNeighbor.childrenSize; i++) {
                    Node<T> c = rightNeighbor.getChild(i);
                    node.addChild(c);
                }

                if (parent.parent != null && parent.numberOfKeys() < minKeySize) {
                    // removing key made parent too small, combined up tree
                    this.combined(parent);
                } else if (parent.numberOfKeys() == 0) {
                    // parent no longer has keys, make this node the new root
                    // which decreases the height of the tree
                    node.parent = null;
                    root = node;
                }
            } else if (leftNeighbor != null && parent.numberOfKeys() > 0) {
                // Can't borrow from neighbors, try to combined with left neighbor
                T removeValue = leftNeighbor.getKey(leftNeighbor.numberOfKeys() - 1);
                int prev = getIndexOfNextValue(parent, removeValue);
                T parentValue = parent.removeKey(prev);
                parent.removeChild(leftNeighbor);
                node.addKey(parentValue);
                for (int i = 0; i < leftNeighbor.keysSize; i++) {
                    T v = leftNeighbor.getKey(i);
                    node.addKey(v);
                }
                for (int i = 0; i < leftNeighbor.childrenSize; i++) {
                    Node<T> c = leftNeighbor.getChild(i);
                    node.addChild(c);
                }

                if (parent.parent != null && parent.numberOfKeys() < minKeySize) {
                    // removing key made parent too small, combined up tree
                    this.combined(parent);
                } else if (parent.numberOfKeys() == 0) {
                    // parent no longer has keys, make this node the new root
                    // which decreases the height of the tree
                    node.parent = null;
                    root = node;
                }
            }
        }

        return true;
    }

    /**
     * Get the index of previous key in node.
     * 
     * @param node
     *            to find the previous key in.
     * @param value
     *            to find a previous value for.
     * @return index of previous key or -1 if not found.
     */
    private int getIndexOfPreviousValue(Node<T> node, T value) {
        for (int i = 1; i < node.numberOfKeys(); i++) {
            T t = node.getKey(i);
            if (t.compareTo(value) >= 0)
                return i - 1;
        }
        return node.numberOfKeys() - 1;
    }

    /**
     * Get the index of next key in node.
     * 
     * @param node
     *            to find the next key in.
     * @param value
     *            to find a next value for.
     * @return index of next key or -1 if not found.
     */
    private int getIndexOfNextValue(Node<T> node, T value) {
        for (int i = 0; i < node.numberOfKeys(); i++) {
            T t = node.getKey(i);
            if (t.compareTo(value) >= 0)
                return i;
        }
        return node.numberOfKeys() - 1;
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        return size;
    }

    /**
     * {@inheritDoc}
     */
    public boolean validate() {
        if (root == null) return true;
        return validateNode(root);
    }

    /**
     * Validate the node according to the B-Tree invariants.
     * 
     * @param node
     *            to validate.
     * @return True if valid.
     */
    private boolean validateNode(Node<T> node) {
        int keySize = node.numberOfKeys();
        if (keySize > 1) {
            // Make sure the keys are sorted
            for (int i = 1; i < keySize; i++) {
                T p = node.getKey(i - 1);
                T n = node.getKey(i);
                if (p.compareTo(n) > 0)
                    return false;
            }
        }
        int childrenSize = node.numberOfChildren();
        if (node.parent == null) {
            // root
            if (keySize > maxKeySize) {
                // check max key size. root does not have a min key size
                return false;
            } else if (childrenSize == 0) {
                // if root, no children, and keys are valid
                return true;
            } else if (childrenSize < 2) {
                // root should have zero or at least two children
                return false;
            } else if (childrenSize > maxChildrenSize) {
                return false;
            }
        } else {
            // non-root
            if (keySize < minKeySize) {
                return false;
            } else if (keySize > maxKeySize) {
                return false;
            } else if (childrenSize == 0) {
                return true;
            } else if (keySize != (childrenSize - 1)) {
                // If there are children, there should be one more child then
                // keys
                return false;
            } else if (childrenSize < minChildrenSize) {
                return false;
            } else if (childrenSize > maxChildrenSize) {
                return false;
            }
        }

        Node<T> first = node.getChild(0);
        // The first child's last key should be less than the node's first key
        if (first.getKey(first.numberOfKeys() - 1).compareTo(node.getKey(0)) > 0)
            return false;

        Node<T> last = node.getChild(node.numberOfChildren() - 1);
        // The last child's first key should be greater than the node's last key
        if (last.getKey(0).compareTo(node.getKey(node.numberOfKeys() - 1)) < 0)
            return false;

        // Check that each node's first and last key holds it's invariance
        for (int i = 1; i < node.numberOfKeys(); i++) {
            T p = node.getKey(i - 1);
            T n = node.getKey(i);
            Node<T> c = node.getChild(i);
            if (p.compareTo(c.getKey(0)) > 0)
                return false;
            if (n.compareTo(c.getKey(c.numberOfKeys() - 1)) < 0)
                return false;
        }

        for (int i = 0; i < node.childrenSize; i++) {
            Node<T> c = node.getChild(i);
            boolean valid = this.validateNode(c);
            if (!valid)
                return false;
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return TreePrinter.getString(this);
    }
    
    
    private static class Node<T extends Comparable<T>> {

        private T[] keys = null;
        private int keysSize = 0;
        private Node<T>[] children = null;
        private int childrenSize = 0;
        private Comparator<Node<T>> comparator = new Comparator<Node<T>>() {
            public int compare(Node<T> arg0, Node<T> arg1) {
                return arg0.getKey(0).compareTo(arg1.getKey(0));
            }
        };

        protected Node<T> parent = null;

        private Node(Node<T> parent, int maxKeySize, int maxChildrenSize) {
            this.parent = parent;
            this.keys = (T[]) new Comparable[maxKeySize + 1];
            this.keysSize = 0;
            this.children = new Node[maxChildrenSize + 1];
            this.childrenSize = 0;
        }

        private T getKey(int index) {
            return keys[index];
        }

        private int indexOf(T value) {
            for (int i = 0; i < keysSize; i++) {
                if (keys[i].equals(value)) return i;
            }
            return -1;
        }

        private void addKey(T value) {
            keys[keysSize++] = value;
            Arrays.sort(keys, 0, keysSize);
        }

        private T removeKey(T value) {
            T removed = null;
            boolean found = false;
            if (keysSize == 0) return null;
            for (int i = 0; i < keysSize; i++) {
                if (keys[i].equals(value)) {
                    found = true;
                    removed = keys[i];
                } else if (found) {
                    // shift the rest of the keys down
                    keys[i - 1] = keys[i];
                }
            }
            if (found) {
                keysSize--;
                keys[keysSize] = null;
            }
            return removed;
        }

        private T removeKey(int index) {
            if (index >= keysSize)
                return null;
            T value = keys[index];
            for (int i = index + 1; i < keysSize; i++) {
                // shift the rest of the keys down
                keys[i - 1] = keys[i];
            }
            keysSize--;
            keys[keysSize] = null;
            return value;
        }

        private T removeBiggestKey() {
            return removeKey(numberOfKeys()-1);
        }

        private Node<T> removeBiggestchiled() {
            if(numberOfChildren()==0)
                return null;
            return removeChild(numberOfChildren()-1);
        }

        private T removesmallestKey() {
            return removeKey(0);
        }
        private Node<T> removeSmallestchiled() {
            if(numberOfChildren()==0)
                return null;
            return removeChild(0);
        }


        private int numberOfKeys() {
            return keysSize;
        }

        private boolean is_leaf()
        {return numberOfChildren()==0;}

        private Node<T> getChild(int index) {
            if (index >= childrenSize)
                return null;
            return children[index];
        }
        
        private int indexOf(Node<T> child) {
            for (int i = 0; i < childrenSize; i++) {
                if (children[i].equals(child))
                    return i;
            }
            return -1;
        }
        
        //sub function to get the closest child of 'target'
        private int get_closest_child_to_target(T traget) {
        	
        	//Returns the index of the key who is closest to target and bigger , if no such index exists
            for(int i=0;i<keysSize;i++)
                if(traget.compareTo(keys[i])<0)
                    return i;
            return numberOfKeys();
        }

        private boolean addChild(Node<T> child) {
            if(child!=null) {
                child.parent = this;
                children[childrenSize++] = child;
                Arrays.sort(children, 0, childrenSize, comparator);
            }
            return true;
        }

        private boolean removeChild(Node<T> child) {
            boolean found = false;
            if (childrenSize == 0)
                return found;
            for (int i = 0; i < childrenSize; i++) {
                if (children[i].equals(child)) {
                    found = true;
                } else if (found) {
                    // shift the rest of the keys down
                    children[i - 1] = children[i];
                }
            }
            if (found) {
                childrenSize--;
                children[childrenSize] = null;
            }
            return found;
        }

        private Node<T> removeChild(int index) {
            if (index >= childrenSize)
                return null;
            Node<T> value = children[index];
            children[index] = null;
            for (int i = index + 1; i < childrenSize; i++) {
                // shift the rest of the keys down
                children[i - 1] = children[i];
            }
            childrenSize--;
            children[childrenSize] = null;
            return value;
        }



        private int numberOfChildren() {
            return childrenSize;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();

            builder.append("keys=[");
            for (int i = 0; i < numberOfKeys(); i++) {
                T value = getKey(i);
                builder.append(value);
                if (i < numberOfKeys() - 1)
                    builder.append(", ");
            }
            builder.append("]\n");

            if (parent != null) {
                builder.append("parent=[");
                for (int i = 0; i < parent.numberOfKeys(); i++) {
                    T value = parent.getKey(i);
                    builder.append(value);
                    if (i < parent.numberOfKeys() - 1)
                        builder.append(", ");
                }
                builder.append("]\n");
            }

            if (children != null) {
                builder.append("keySize=").append(numberOfKeys()).append(" children=").append(numberOfChildren()).append("\n");
            }

            return builder.toString();
        }
    }

    private static class TreePrinter {

        public static <T extends Comparable<T>> String getString(BTree<T> tree) {
            if (tree.root == null) return "Tree has no nodes.";
            return getString(tree.root, "", true);
        }

        private static <T extends Comparable<T>> String getString(Node<T> node, String prefix, boolean isTail) {
            StringBuilder builder = new StringBuilder();

            builder.append(prefix).append((isTail ? "└── " : "├── "));
            for (int i = 0; i < node.numberOfKeys(); i++) {
                T value = node.getKey(i);
                builder.append(value);
                if (i < node.numberOfKeys() - 1)
                    builder.append(", ");
            }
            builder.append("\n");

            if (node.children != null) {
                for (int i = 0; i < node.numberOfChildren() - 1; i++) {
                    Node<T> obj = node.getChild(i);
                    builder.append(getString(obj, prefix + (isTail ? "    " : "│   "), false));
                }
                if (node.numberOfChildren() >= 1) {
                    Node<T> obj = node.getChild(node.numberOfChildren() - 1);
                    builder.append(getString(obj, prefix + (isTail ? "    " : "│   "), true));
                }
            }

            return builder.toString();
        }
    }
}