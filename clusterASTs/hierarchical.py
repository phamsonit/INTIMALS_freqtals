# Module sys has to be imported:
import sys

#!/Users/user/PycharmProjects/clustering/venv/bin

#from matplotlib import pyplot as plt
#import plotly.figure_factory as ff
#from scipy.spatial.distance import pdist
from scipy.cluster import hierarchy
import pandas as pd
import numpy as np

inputFile = sys.argv[1]
outputFile = sys.argv[2]
numClusters = sys.argv[3]

#input = "fold4.csv"
#output = "output.txt"
#numClusters = 5

#print(inputFile)
#print(outputFile)
#print(numClusters)


data = pd.read_csv(inputFile, header=None)
fr = pd.DataFrame(data)
u, s, v = np.linalg.svd(fr, full_matrices=True)
Z = hierarchy.linkage(u, "ward")

Y = hierarchy.fcluster(Z, numClusters, 'maxclust')
#print(Y)
with open(outputFile, "w") as f:
    for item in Y:
        f.write(str(item)+" ")
f.close()

# plt.ylabel('distance')
# plt.xlabel('samples')
# den = hierarchy.dendrogram(Z)
# plt.show()

#fig = ff.create_dendrogram(Z)
#fig.show()



