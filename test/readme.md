# Testing freqtal on small datasets ##

## Sample datasets ##
The sample datasets include 1 abstract data which uses A,B,C,... as labels, and 4 small Java projects which are selected from examples of design patterns. The input datasets, configurations and output are stored in the following directories:

- `input-artificial-data`: contains input ASTs
- `output-artificial-data`: constains output files
- `conf-artificial-data`: contains configurations

## Running freqtals on sample datasets ##
- run freqtals on abstract-data
` java -jar freqtals.jar test/conf-artificial-data/abstract-data/config.properties 2 abstract-data `
- parallel run Freqtals on 4 design pattern projects
` java -jar freqtals.jar -multi test/conf-artifical-data/design-patterns/config.properties `