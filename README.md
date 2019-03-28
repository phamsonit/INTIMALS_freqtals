# The freqt_java repository #
This repository contains Java source code of Freqt algorithm. Freqt is an
efficient algorithm for discovering frequent subtree patterns in labeled
ordered tree data. In this version, it was adapted to discover maximal
frequent subtree patterns.

## Getting started ##
Freqt was compiled into a standalone jar file, named freqt_java.jar.
This jar file and additional data such as config.properties, input AST,
etc, are stored in this repository. It is better to clone whole freqt_java
repository to your local computer.

### Using freqt_java.jar ###
There are two options to run Freqt.
1. single run: run Freqt on a directory and a minimum support. The
execution command as follow:
`java -jar freqt_java.jar CONFIG_FILE [MIN_SUPPORT] [INPUT_FOLDER]`
2. parallel run: run Freqt on multiple directories and multiple supports.
The execution command as follow:
`java -jar freqt_java.jar -multi CONFIG_FILE`

Here, `CONFIG_FILE` contains parameters for executing Freqt.
Some essential parameters are:
-`timeout`: time budget for a run 
-`minLeafSize`: minimun number of leafs of patterns
-`maxLeafSize`: maximum number of leafs of patterns
-`nbIdentifier`: minimum number of `identifier` labels in pattern
-`maxRepeatLabel`: maximal times a child of a node occurs in a pattern
-`post`: using post-processing or not. If `post=true` Freqt will discover
maximal patterns WITHOUT size constraints. Otherwise, if `post=false`
it will discover maximal patterns with size constraints.
-`xml`: output patterns as XML format
- parallel run parameters: 
-`minSupportList`: list of minSup.
-`inFilesList`: list of directories.
Freqt will run all combinations of minSupportList and inFilesList.
For example, minSupportList = 4,5,6; inFilesList = fold1, fold2, fold3.
Freqt will execute 9 times. 3 times for each fold with 3 minSup.

*Please refer the `conf/config.properties` for more details.*

### Dependent files ###
- `listRootLabel.txt`: this file contains a list of nodes. Freqt will
start to search subtrees from these nodes. If we don't give any node in
this file, or freqt cannot locate this file it will search whole data.
As a result it will take along time to finish !
For example, if we are interested in patterns which are rooted by
TypeDeclaration and ClassInstanceCreation we can put these labels in
listRootLabel.
- `listWhiteLabel.txt`: this file contains a list of nodes and their
children. Only these children are allowed to expand in the mining process.
The absent children will be pruned.
For example, MethodDeclaration has many children. If we only need to find
this node with some children such as name and body we can put this node in
the listWhiteLabel like MethodDeclaration name body.
- `xmlCharacters.txt`: this file contains some special xml characters such
as $, <, >. They are needed to represent the output XML patterns. 

These files are located in `conf` folder, and their paths must be declared
exactly in the `config.properties`.

### Input ###
The input of freqt is a set of XML files. Each XML file is an AST.
The path storing these files must be declared by `inFiles` in the
`config.properties`.

### Output ###
The output of freqt is a set of maximal frequent subtree patterns.
Patterns are represented as XML format. This format is suitable
for `matcher` tool.
In XML format, we use different directives:
- match-sequence : represent ordered nodes
- match-set : unordered nodes
- metavariable: node which misses one child
(refer to `ForestMatcher` for more details).
