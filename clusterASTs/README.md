### USAGE ###

`java -jar clusterAST.jar inputDirectory outputDirectory createDataBaseOption algorithmName numberCluster`


`inputDirectory` : a directory containing ASTs
`outputDirectory` : a directory containing output clusters
`createDataBaseOption` : 1 - keep all labels; 2 - keep only leafs
`algorithmName` : 1 - hierarchical; 2 - KMeans
`numberCluster` : number of output clusters

Note: clusteringAST uses root labels and white labels to limit the labels considered by clustering algorithms.
Thus, please be sure that you put the `listRootLabel.txt` and `listWhiteLabel.txt` in the same directory as clusterAST.jar


### Input ###
A set of ASTs in XML format

### Output ###
The output is a set of sub-directories. Each sub-directory contains a list of ASTs which belong to its cluster.