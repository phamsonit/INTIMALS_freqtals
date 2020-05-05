# FREQTALS #
FREQTALS, an extension of FREQT tree mining algorithm, is designed to discover patterns in software source code. It can discover either patterns in individual systems or patterns that occur with different frequences in two versions of a system.

## Getting FREQTALS ##
FREQTALS is implemented in Java. The software is built as a standalone jar file `freqtals.jar`. Thus user can execute it in any flatform.

## External libraries ##

FREQTALS uses `forestmatcher.jar` (https://gitlab.soft.vub.ac.be/intimals/forest-matcher) to match patterns in source code. Thus, please make sure that the forsmatcher.jar file is stored in the same directory with freqtats.jar.

## Using FREQTALS ##

### Syntax ###
FREQTALS supports two running options:
1. single run: execute FREQTALS on a single dataset and a minimum support threshold.
`java -jar freqtals.jar CONFIG_FILE MIN_SUPPORT INPUT_DIR`
2. parallel run: execute FREQTALS on multiple datasets and multiple minimum support thresholds.
`java -jar freqtals.jar -multi CONFIG_FILE`

where 
- `CONFIG_FILE`: a file containing configurations,
- `MIN_SUPPORT`: a minimum support threshold,
- `INPUT_DIR`: a directory containing XML files. Note that this directory is a sub-directory of directory that is indicated in the CONFIG_FILE.

### CONFIG_FILE ###
A `config.properties` contains different attributes which are described as follows:

- `twoClass=[true/false]`: `true`-running algorithm on two versions of a system; `false`-running the algorithm on one system.
- `inputPath=[path]`: a main directory that contains `INPUT_DIR`
- `outputPath=[path]`: a directory contains output
- `timeout=[int]`: running time
- `minLeaf=[int]`: minimum number of leafs of pattern
- `maxLeaf=[int]`: maximum number of leafs of pattern
- `minNode=[int]`: minimum number of nodes of pattern
- `twoStep=[true/false]`: an option to run FREQTALS in one step or two step: `twoStep=true` running the software in two steps(step 1: find frequent patterns with size constraints; step 2: find maximal patterns without maximal size constraint from root occurrences of patterns found in the first step). `twoStep=false` - find maximal patterns with maximal size constraint.
- `weighted=[true/false]`: `true`-counting support based on number of occurrences; `false`-counting support based on number of files

- **Configurations used together with twoClass=true**
- `inputPath1=[dir]`: directory contains files of old version
- `inputPath2=[dir]`: directory contains files of new version
- `minDSScore=[double]`: minimum chi-square score
- `numPatterns=[int]`: number of patterns, that have highest chi-square score, to be kept in the first step.

- **Configurations for additional file**
- `buildGrammar=[true/false]`: `true`-create grammar from input data; `false`- read grammar from file.
- `rootLabelFile=[path]`: file contains a list of root labels
- `whiteLabelFile=[path]`: file contains a list of labels that only allows in patterns
- `xmlCharacterFile=[path]`: file contains a list of xml characters

- **Configurations for the parallel run**
- `minSupportList=...`: list of minimum support thresholds.
- `inFilesList=...`: list of directories (each directory corresponds to one dataset).

FREQTALS will run all combinations of minSupportList and inFilesList.
For example, given minSupportList = 4,5,6 and inFilesList = fold1, fold2, fold3
FREQTALS will run 9 executions. 3 times for each fold with 3 minimum support thresholds.
*Please refer the `conf/java/config.properties` for more details.*

### Input ###
The input data is a set of XML files. Each XML file corresponds to an AST. These files must be stored in the `INPUT_DIR`.

### Output ###
The output of the software includes the following files:
-`INPUT_DIR_MIN_SUPPORT_patterns.xml`: list of patterns
-`INPUT_DIR_MIN_SUPPORT_matches.xml`: list of matches of patterns in source code
-`INPUT_DIR_MIN_SUPPORT_clusters.xml`: list of clusters of patterns
-`INPUT_DIR_MIN_SUPPORT_patterns_common.xml`: list of common patterns of patterns in each cluster
-`INPUT_DIR_MIN_SUPPORT_matches_common.xml`: list of matches of common patterns in source code
-`INPUT_DIR_MIN_SUPPORT_config.properties`: used configurations
-`INPUT_DIR_MIN_SUPPORT_patterns-report.txt`: result statistics

In case of running FREQTALS on two versions of a system, the matches files will be `INPUT_DIR_MIN_SUPPORT_matches_1.xml` and `INPUT_DIR_MIN_SUPPORT_matches_2.xml` which are the matches of patterns in old version and new version, respectively.

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