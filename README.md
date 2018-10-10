# The Freqt_java repository #

This repository contains java source code and sample data for Freqt algorithm 

## Freqt improvement: ##

1. read XMLs in multiple folders by using standard java libraries
2. output patterns are represented as XML form which is used for Ekeko algorithm
3. apply subtree mining techniques to filter redundant patterns
	- minimum number of leaf nodes
	- single path
4. apply grammar to search subtrees
	- don't allow children of an unordered node to have the same label
	- only expand subtree from the first child of an ordered node


## Usage ##


## Input/output ##

**Input:** ASTs


**Output:**

1. Text format

Each line denotes a frequent sub-tree pattern, consisting of four columns, support, weighted support, size of tree (# of nodes), and frequent sub-tree pattern represented in strict S-expression.

Here is a concrete example of output.
2	2	4	(B(F(f1))(G))

2. XML format

```xml
<subtree id="3" support="2" wsupport="2" size="4">
	<B>
		<__directives><match-set/></__directives>
		<F>
			f1
		</F>
		<G>
			<__directives>
			<optional-meta-variable>
				<parameter key="name" value="?G1"/>
			</optional-meta-variable>
			</__directives>
		</G>
	</B>
</subtree>
```





