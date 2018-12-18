# The freqt_java repository #
This repository contains java source code and sample data for Freqt algorithm.
Freqt is an efficient algorithm for discovering frequent subtrees in labeled ordered tree data.
In this java version, it is modified to discover code fragments which frequently occur
in a given set of ASTs. 

## Getting started ##
freqt_java is compiled into a standalone jar file, named freqt_java.jar.
This jar file and additional data such as config.properties, grammar, input AST, ... 
are stored in this repository. It is better to clone whole freqt_java repository
to your local computer.

### Using freqt_java.jar ###
To mine patterns, run the freqt_java.jar with following option:
`java -jar freqt_java.jar path/config.properties`
where `config.properties` is the file containing all parameters for freqt.
If `path/config.properties` is omitted, freqt will use the file `conf/config.properties`
In this file, we can adjust different parameters such as
-`minsup`: minimum support of patterns (or minimum number of files that contain patterns)
-`minpat`: minimum size of patterns
-`maxpat`: maximun size of patterns
-`minleaf`: minimun number of leafs of patterns
-`maxleaf`: minimun number of leafs of patterns
*Please refer the `conf/config.properties` for more details.*

### Dependent files ###
In order to use grammar to prune the search space and filter the output, various files are required:
- `java_grammar.xml`: this file contains the java grammar definition
- `listRootLabel.txt`: this file contains a list of nodes. Freqt will start to search subtrees from these nodes. If we don't give any node in this file, or freqt cannot locate this file it will search whole data. As a result it will take along time to finish !
For example, if we are interested in patterns which are rooted by TypeDeclaration and ClassInstanceCreation we can put these labels in listRootLabel.
- `listWhiteLabel.txt`: this file contains a list of nodes and their children. Only these children are allowed to expand in the mining process. The absent children will be pruned.
For example, MethodDeclaration has many children. If we only need to find this node with some children such as name and body we can put this node in the listWhiteLabel like MethodDeclaration name body.
- `xmlCharacters.txt`: this file contains some special xml characters such as $, <, >. They are needed to represent the output XML patterns. 

These files are located in `conf` folder, and their paths must be declared exactly in the `config.properties`.

### Input ###
The input of freqt is a set of ASTs. Each AST is a XML file. The path storing these files must be declared in the config.properties.    


### Output ###
The output of freqt is a set of maximal frequent subtrees (if we set `post="true"` in the config.properties) or a set of frequent subtrees (`post="false`)
Depending on the given parameters, the subtrees can be represented as string format `xml="false"` or xml format (`xml="true"`).

- String format: each line of text corresponds to a frequent subtree.
For example: 2	2	4	(B(F(f1))(G)) represents a subtree (B(F(f1))(G))
which has : support = 2, weight support = 2, and size = 4.

2. XML format: each subtree is represented as a substructure in XML file.
For example, the XML format of the above subtree is represented as follow:

```xml
<subtree id="3" support="2" wsupport="2" size="4">
	<B>
		<__directives><match-set/></__directives>
		<F>
			f1
		</F>
		<G>
			<__directives>
				<optional>
					<meta-variable>
						<parameter key="name" value="?G1"/>
					</meta-variable>
				</optional>			
			</__directives>
		</G>
	</B>
</subtree>
```

In XML format, we use different directives:
- match-sequence : represent ordered nodes
- match-set : unordered nodes
- metavariable: nodes which miss one child node
(refer `ForestMatcher` for more details).
