# FREQTALS #
FREQTALS is a constraint-based algorithm for discovering
maximal frequent subtree patterns from legacy source code.

## Get FREQTALS ##
FREQTALS was compiled into a standalone jar file `freqtals.jar`.
This execution file and additional sample data such as configuration,
input ASTs, etc, can be found in repository [link...].

## Using FREQTALS ##
### Execute command ###
FREQTALS supports two execution options:
1. single run: execute FREQTALS on one dataset and a minimum support threshold.
The execution command is:
`java -jar freqtals.jar CONFIG_FILE MIN_SUPPORT INPUT_FOLDER`
2. parallel run: execute FREQTALS on multiple datasets and multiple minimum
support thresholds. The execution command is:
`java -jar freqtals.jar -multi CONFIG_FILE`

where 
- `CONFIG_FILE`: file containing configurations,
- `MIN_SUPPORT`: minimum support threshold,
- `INPUT_FOLDER`: folder containing ASTs data. Note that this folder is a subfolder of `inputPath`.

### Configurations ###
The `CONFIG_FILE` contains the configurations which are described as follows:
- `timeout`: budget time to run one dataset with a minimum support threshold
- `minLeafSize`: minimum number of leafs of pattern
- `maxLeafSize`: maximum number of leafs of pattern
- `nbIdentifiers`: minimum number of `identifier` labels in a pattern
- `maxRepeatLabel`: maximal number of siblings in a pattern that can have the same label.
- `inputPath`: input data path
- `outputPath`: output path
- `post`: using the post-processing step. If `post=true` FREQTALS will discover
maximal patterns **WITHOUT** maxLeafSize constraint. Otherwise, if `post=false`
FREQTALS will discover maximal patterns with maxLeafSize constraint.
- `xml=true`: output patterns as XML format
- Configurations for the parallel run: 
-`minSupportList`: list of minimum support thresholds.
-`inFilesList`: list of directories (each directory corresponds to one dataset).
FREQTALS will run all combinations of minSupportList and inFilesList.
For example, given minSupportList = 4,5,6 and inFilesList = fold1, fold2, fold3
FREQTALS will run 9 executions. 3 times for each fold with 3 minimum support thresholds.
*Please refer the `conf/java/config.properties` for more details.*

### Dependent files ###
- `listRootLabel.txt`: set of labels allowed to occur in the root of patterns.
If this file is empty or it cannot be found, FREQTALS will consider all labels.
For example, if we are interested in patterns which are
rooted by `TypeDeclaration` and `Block` we can put these labels in listRootLabel.
- `listWhiteLabel.txt`: lists of labels and its children allowed to occur in patterns.
The lists of labels and its children not allowed to occur in patterns,
blackLabels, is inferred from this list. For example, the label `MethodDeclaration`
has many children. If we only need to find this label with some of its children such
as `name` and `body` we can put these labels in the listWhiteLabel as
`MethodDeclaration name, body`. Other children of `MethodDeclaration` are considered
as blacklabels which are ignored in the mining process.
- `xmlCharacters.txt`: a list of special xml characters such as $, <, >, ...
They are used to represent the patterns in XML format. 
These files can be found in directory `conf` in this repository. Their paths
must be exactly declared in the equivalent config file.

### Input ###
The input of FREQTALS is a set of XML files. Each XML file corresponds to an AST.
Input data must be stored as a subfolder of `inputPath`.

### Output ###
The output of FREQTALS includes the following files:
-`INPUT_FOLDER-MIN_SUPPORT-patterns.xml`: list of patterns
-`INPUT_FOLDER-MIN_SUPPORT-config.properties`: used configurations
-`INPUT_FOLDER-MIN_SUPPORT-patterns-report.txt`: resulting statistic