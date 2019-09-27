# Module sys has to be imported:
import sys

from matplotlib import pyplot as plt
import plotly.figure_factory as ff
from scipy.spatial.distance import pdist
from scipy.cluster import hierarchy
from sklearn.cluster import KMeans
import pandas as pd
import numpy as np

inputFile = sys.argv[1]
outputFile = sys.argv[2]
algorithmName = sys.argv[3]
numClusters = int(sys.argv[4])

#inputFile = "jhotdraw.csv"
#outputFile = "output.txt"
#algorithmName = "1"
#numClusters = 5


#read input data
#https://pandas.pydata.org/pandas-docs/stable/reference/api/pandas.read_csv.html
D = pd.read_csv(inputFile, header=None)
DF = pd.DataFrame(D)

#using SVD to reduce the size of input data
#https://docs.scipy.org/doc/numpy/reference/generated/numpy.linalg.svd.html
u, s, v = np.linalg.svd(DF, full_matrices=True)

#get a condensed distance matrix from input dataset
#https://docs.scipy.org/doc/scipy/reference/generated/scipy.spatial.distance.pdist.html#scipy.spatial.distance.pdist
#X = pdist(u, 'euclidean')


if algorithmName == "1":
    print("hierarchy")
    #create hierarchical clustering
    #https://docs.scipy.org/doc/scipy/reference/generated/scipy.cluster.hierarchy.linkage.html#scipy.cluster.hierarchy.linkage
    Y = hierarchy.linkage(u, "ward")
    #create clusters from hierarchical cluster
    #https://docs.scipy.org/doc/scipy/reference/generated/scipy.cluster.hierarchy.fcluster.html
    Z = hierarchy.fcluster(Y, numClusters, 'maxclust')
else:
    print("kmeans")
    # https://scikit-learn.org/stable/modules/generated/sklearn.cluster.KMeans.html
    kmeans = KMeans(n_clusters=numClusters, init='k-means++', max_iter=300, n_init=10, random_state=0)
    kmeans.fit_predict(u)
    # get clusters information
    Z = kmeans.labels_

print(Z)
#write clusters to file
with open(outputFile, "w") as f:
    for item in Z:
        f.write(str(item)+" ")
f.close()

#-----------------------------------
#fig = ff.create_dendrogram(Y)
#fig.show()



