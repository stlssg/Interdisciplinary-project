# Project Template

In this file, we give you some general guideline and advices for the project. You need two components:

1. Python 3.6
1. gurobi

## Python

Linux users should have python already installed in their PC. For the other users I strongly recomment to install **Anaconda** and to use its terminal for installing new packages. You can find details [here](https://www.anaconda.com/distribution/) and [there](https://www.anaconda.com/distribution/#download-section). 


## gurobi
gurobi is a commercial software. See [here](https://www.gurobi.com/)


## Python packages:
Probably you will need to install several packages (e.g., pulp, numpy, etc). In linux my suggestion is to use pip
~~~
pip3 install <package name>
~~~
e.g., 
~~~
pip3 install pulp
~~~
For windows is suggest to use conda.
~~~
conda install -c conda-forge pulp 
~~~


## Run the code:
Run the code by writing in the terminal
```
python3 main.py
```
and enjoy...


## Text Editor

In order write good code you need a good editor. The best one that I recommend are:

1. [Visual Studio Code](https://code.visualstudio.com/) free;
1. [Sublime Text](https://www.sublimetext.com/) free for non commercial usage;
1. [PyCharm](https://www.jetbrains.com/pycharm/) free for students.

## Project

In the project we consider the following simple problem:
$$
\max \sum_{i \in \mathcal{I}} c_i x_i + \sum_{s\in \mathcal{S}} p_s \big[\sum_{i \in I} q_i^s y_i^s \big]
$$
subject to:
$$
\sum_{i\in \mathcal{I}} w_i x_i \leq W
$$

$$
\sum_{i\in \mathcal{I}} v_i y_i^s \leq W \ \ \ \forall\ s\ \in\ \mathcal{S}
$$

$$
y_i^s \leq x_i \ \ \ \forall\ s\ \in\ \mathcal{S}
$$

$$
x_i, y_i \in \{0, 1\}\ \ \ \forall\ i \in \mathcal{I}
$$



