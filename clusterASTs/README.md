### USAGE ###

`java -jar clusterAST.jar INPUT_DIRECTORY OUTPUT_DIRECTORY NUMBER_OF_CLUSTERS`

where 
- `INPUT_DIRECTORY`   : a root directory containing ASTs.
- `OUTPUT_DIRECTORY`  : a directory containing ASTs of clusters 
- `NUMBER_OF_CLUSTERS`: number of output clusters.

Note: put the script `hierarchical.py` in the same directory as `clusterAST.jar`

### Output ###
The output is a list of sub-directories which contain ASTs of clusters.