#!/bin/bash
#echo "run freqt with 2 configurations"
#timeout in minute
timeout=1
for minsup in {9..11}
  do
	  echo "**********************"
	  echo "Min support = "$minsup
  	  echo "**********************"
	  for fold in {1..10}
	   do		   
		   java -jar freqt_java.jar 'conf/java/config.properties' $minsup $fold $timeout

	   done
 done