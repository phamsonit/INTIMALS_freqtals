# FREQTALS #
FREQTALS is a constraint-based algorithm for discovering
maximal frequent subtree patterns from source code.

## Get FREQTALS ##
FREQTALS was compiled into a standalone jar file `freqtals.jar`.
This execution file and additional sample data such as configuration,
input ASTs, etc, can be found in this repository.

Note that: to have matches and clusters of patterns after running freqtats,  `forestmatcher.jar` was integrated into the freqtals. Thus we need to have the forsmatcher.jar in the same directory with freqtats.jar.

## Using FREQTALS ##

### Syntax ###
FREQTALS supports two options:
1. single run: execute FREQTALS on a single dataset and a minimum support threshold.
`java -jar freqtals.jar CONFIG_FILE MIN_SUPPORT INPUT_FOLDER`
2. parallel run: execute FREQTALS on multiple datasets and multiple minimum
support thresholds.
`java -jar freqtals.jar -multi CONFIG_FILE`

where 
- `CONFIG_FILE`: a file containing configurations,
- `MIN_SUPPORT`: minimum support threshold,
- `INPUT_FOLDER`: a folder containing ASTs data. Note that this folder is a subfolder of `inputPath` in the CONFIG_FILE.



### Input ###
The input is a set of XML files. Each XML file corresponds to an AST.
These files must be stored in a subfolder of `inputPath`.

### Output ###
The output includes the following files:
-`INPUT_FOLDER-MIN_SUPPORT-patterns.xml`: list of patterns
-`INPUT_FOLDER-MIN_SUPPORT-matches.xml`: list of matches of patterns in source code
-`INPUT_FOLDER-MIN_SUPPORT-clusters.xml`: list of clusters of patterns
-`INPUT_FOLDER-MIN_SUPPORT-patterns_common.xml`: list of common patterns of patterns in each cluster
-`INPUT_FOLDER-MIN_SUPPORT-matches_common.xml`: list of matches of common patterns in source code
-`INPUT_FOLDER-MIN_SUPPORT-config.properties`: used configurations
-`INPUT_FOLDER-MIN_SUPPORT-patterns-report.txt`: result statistics


### Configurations ###
The `CONFIG_FILE` contains the configurations which are described as follows:
- `inputPath`: directory contains data
- `outputPath`: directory contains output patterns and other files
- `timeout`: budget time to run one dataset
- `minLeaf`: minimum number of leafs of pattern
- `maxLeaf`: maximum number of leafs of pattern
- `minNode`: minimum number of nodes of pattern
- `twoStep`: options to choose mining maximal patterns methods. `true`  - using 2 steps to mine maximal patterns (step 1: find frequent patterns with size constraints; step 2: grown frequent pattern to find maximal patterns without maximal size constraint). `false` - find maximal patterns in 1 step with maximal size constraint.
- `filter`: method to filter maximal patterns. filter = true  - directly filter maximal patterns in the mining process; filter = false - filter maximality after having a list of frequent patterns
- `buildGrammar`: method to build grammar. buildGrammar = true - build grammar from input data; buldGrammar = false - read grammar from given file
- `rootLabelFile`: file contains a list of root labels
- `whiteLabelFile`: file contains a list of labels that only allows in patterns
- `xmlCharacterFile`: file contains a list of xml characters
- Configurations for the parallel run: 
-`minSupportList`: list of minimum support thresholds.
-`inFilesList`: list of directories (each directory corresponds to one dataset).
FREQTALS will run all combinations of minSupportList and inFilesList.
For example, given minSupportList = 4,5,6 and inFilesList = fold1, fold2, fold3
FREQTALS will run 9 executions. 3 times for each fold with 3 minimum support thresholds.
*Please refer the `conf/java/config.properties` for more details.*

### supporting files ###
- `listRootLabel.txt`: set of labels allowed to occur in the root of patterns.
If this file is empty, FREQTALS will consider all labels as root labels of patterns.
For example, if we are interested in patterns which are
rooted by `TypeDeclaration` and `Block` we can put these labels in listRootLabel.
- `listWhiteLabel.txt`: lists of labels and its children allowed to occur in patterns.
The lists of labels and their children that are not allowed to occur in patterns,
a blackLabels, is inferred from this list. For example, the label `MethodDeclaration`
has many children. If we only need to find this label with some of its children such
as `name` and `body` we can put these labels in the listWhiteLabel as
`MethodDeclaration name, body`. Other children of `MethodDeclaration` are considered
as blacklabels which are ignored in the mining process.
- `xmlCharacters.txt`: a list of special xml characters such as $, <, >, ...
They are used to represent the patterns in XML format. 
These files can be found in directory `conf` in this repository. Their paths
must be exactly declared in the equivalent config file.