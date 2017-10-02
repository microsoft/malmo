from __future__ import print_function
from __future__ import division
# ------------------------------------------------------------------------------------------------
# Copyright (c) 2016 Microsoft Corporation
# 
# Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
# associated documentation files (the "Software"), to deal in the Software without restriction,
# including without limitation the rights to use, copy, modify, merge, publish, distribute,
# sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
# 
# The above copyright notice and this permission notice shall be included in all copies or
# substantial portions of the Software.
# 
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
# NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
# NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
# DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
# ------------------------------------------------------------------------------------------------

# Travelling Salesperson Contest
# Place one or more agents (at the same starting location) in a field with a random distribution of waypoints.
# The first agent to visit all the waypoints is the winner!
# All calculation must be done once the mission has started - ie a brute-force algorithm might come up with a better
# solution, but the other agents will be half way home before it's left the starting block...

# There are six different approximate solutions implemented here, and any combination of them can be raced
# against each other. It's about the least efficient way imaginable of comparing TSP algorithms...
# but it's fun.

from future import standard_library
standard_library.install_aliases()
from builtins import input
from builtins import range
from past.utils import old_div
from builtins import object
import MalmoPython
import os
import random
import sys
import time
import json
import random
import math
import threading

if sys.version_info[0] == 2:
    # Workaround for https://github.com/PythonCharmers/python-future/issues/262
    from Tkinter import *
else:
    from tkinter import *

###################################################################################################################
# General code for all approaches
###################################################################################################################

class point_node(object):
    def __init__(self, x, y):
        self.x = x
        self.y = y
        self.neighbours=[]

    def get_position(self):
        return (self.x, self.y)

    def add_neighbour(self, neighbour):
        self.neighbours.append(neighbour)

distance = lambda p1, p2: math.sqrt((p1.x - p2.x) * (p1.x - p2.x)) + math.sqrt((p1.y - p2.y) * (p1.y - p2.y))

def path_length(points):
    tot_dist = 0
    p_old = points[0]
    for p_new in points:
        tot_dist += distance(p_new, p_old)
        p_old = p_new
    return tot_dist

###################################################################################################################
# Code to support Minimum Spanning Tree approach
###################################################################################################################

class disjoint_set_forest_node(object):
    def __init__(self, data=None, parent=None):
        self.data = data
        self.parent = parent

    def get_data(self):
        return self.data

    def get_parent(self):
        return self.parent

    def set_parent(self, parent):
        self.parent = parent

    def get_root(self):
        if self.parent == None:
            return self
        self.parent = self.parent.get_root()
        return self.parent

    def combine_sets(self, new_node):
        root_node_a = self.get_root()
        root_node_b = new_node.get_root()
        if root_node_a != root_node_b:
            root_node_b.set_parent(root_node_a)

class edge(object):
    def __init__(self, point1, point2):
        self.point1 = point1
        self.point2 = point2
        self.squared_length = (point1.x - point2.x) * (point1.x - point2.x) + (point1.y - point2.y) * (point1.y - point2.y)

    def get_squared_length(self):
        return self.squared_length

def merge_sort_edges(edges):
    if len(edges) > 1:
        m = len(edges) // 2
        left = edges[:m]
        right = edges[m:]

        merge_sort_edges(left)
        merge_sort_edges(right)

        l = 0
        r = 0
        i = 0
        while l < len(left) and r < len(right):
            if left[l].get_squared_length() < right[r].get_squared_length():
                edges[i] = left[l]
                l += 1
            else:
                edges[i] = right[r]
                r += 1
            i += 1
        while l < len(left):
            edges[i] = left[l]
            l += 1
            i += 1
        while r < len(right):
            edges[i] = right[r]
            r += 1
            i += 1

def min_span_tree(edges, points):
    # Build a MST using Kruskal's algorithm.
    # Make each point the start of a disjoint set forest:
    for p in points:
        p.disjoint_set_forest_node = disjoint_set_forest_node()
    # Sort the edges from shortest to longest:
    merge_sort_edges(edges)
    # Build the tree:
    tree=[]
    i = 0
    finished = False
    while not finished and i < len(edges):
        e = edges[i]
        i += 1
        if e.point1.disjoint_set_forest_node.get_root() != e.point2.disjoint_set_forest_node.get_root():
            # This edge will connect two different trees, so add it to the MST:
            tree.append(e)
            e.point1.disjoint_set_forest_node.combine_sets(e.point2.disjoint_set_forest_node)
            e.point1.add_neighbour(e.point2)
            e.point2.add_neighbour(e.point1)

    return tree

def get_MST_route(points):
    # Create a rough approximation of the optimal TSP route using a depth-first search on the minimum spanning tree:
    # First, create a fully connected graph:
    edges = []
    for i in range(len(points)):
        for j in range(i + 1, len(points)):
            edges.append(edge(points[i], points[j]))
    # Get the MST:
    tree = min_span_tree(edges, points)
    # Perform the search:
    stack=[points[0]]
    order=[]
    for p in points:
        p.visited = False
    while len(stack) > 0:
        current_node = stack.pop()
        current_node.visited = True
        order.append(current_node)
        for neighbour in current_node.neighbours:
            if not neighbour.visited:
                stack.append(neighbour)
    return order

###################################################################################################################
# Code to support Divide and conquer approach
###################################################################################################################

def generate_orders(n):
    # Recursively generate all the permutations of n cities, as a list of colon-delimited strings.
    # Eg "generate_orders(3)" will return ['0:1:2:', '0:2:1:', '1:0:2:', '1:2:0:', '2:0:1:', '2:1:0:']
    values = [True for x in range(n)]
    perms = []
    fill_next_value(0, n, values, perms, "")
    return perms

def fill_next_value(digit, num_digits, available_values, perms, seq_so_far):
    # Recursively generate perumutations.
    if digit == num_digits:
        perms.append(seq_so_far)
    else:
        for i in range(num_digits):
            if available_values[i]:
                values_available_now=list(available_values)
                values_available_now[i] = False
                fill_next_value(digit + 1, num_digits, values_available_now, perms, seq_so_far + str(i) + ":")

def assignKMeans(centroids, points):
    #K-means:
    i = 0
    for centroid in centroids:
        centroid.index = i
        i += 1

    # Give all points a cluster index, and find bounding box:
    minx = maxx = points[0].x
    miny = maxy = points[0].y
    for p in points:
        p.k_index = random.randint(0, len(centroids) - 1)
        if p.x < minx:
            minx = p.x
        if p.y < miny:
            miny = p.y
        if p.x > maxx:
            maxx = p.x
        if p.y > maxy:
            maxy = p.y

    changed = True
    while (changed):
        # Step 2: move centroids:
        counts = [0 for c in centroids]
        for c in centroids:
            c.x = 0
            c.y = 0
        for p in points:
            c = centroids[p.k_index]
            counts[p.k_index] += 1
            c.x += p.x
            c.y += p.y
        for x in range(len(centroids)):
            if counts[x] == 0:
                centroids[x].x = random.randint(minx, maxx)
                centroids[x].y = random.randint(miny, maxy)
            else:
                centroids[x].x /= counts[x]
                centroids[x].y /= counts[x]
        # Step 1: allocate each point to a cluster:
        changed = False
        for p in points:
            mindist = 0
            nearest_centroid = None
            for c in centroids:
                dist = distance(p, c)
                if dist < mindist or nearest_centroid == None:
                    nearest_centroid = c
                    mindist = dist
            if p.k_index != nearest_centroid.index:
                changed = True
                p.k_index = nearest_centroid.index

# Pre-calculate the permutations for n cities, up to n = 8
perm_tables = []
for i in range(9):
    perm_tables.append(generate_orders(i))

def brute_force_best_perm(points, perms):
    # Get the best route by brute force - can do this up to about eight cities before execution time becomes prohibitive.
    # (NB we don't do anything clever here, like caching partial routes etc. Why be clever when you are being a brute?)
    best_length = 0
    best_order = []
    for p in perms:
        length = 0
        indices = p.split(":")
        indices.remove('')
        p1 = points[int(indices[0])]
        for i in indices:
            p2 = points[int(i)]
            dx = (p2.x - p1.x) * (p2.x - p1.x)
            dz = (p2.y - p1.y) * (p2.y - p1.y)
            length += dx + dz
            p1 = p2
        if best_length == 0 or length < best_length:
            best_order = indices
            best_length = length

    route = [None for x in points]
    i = 0
    for ind in best_order:
        route[i] = points[int(ind)]
        i += 1
    return route

def get_divide_and_conquer_route(points):
    route = []
    divide_and_generate_route(points, route)
    return route

BRANCH_FACTOR=7 # Split any task of >7 cities into sub-cities.
def divide_and_generate_route(points_in, route, level=0):
    points = [point_node(p.x, p.y) for p in points_in]  # Copy
    if len(points) == 0:
        return
    elif len(points) > BRANCH_FACTOR:
        # Too many points for brute-force to work.
        # Use k-means to split into BRANCH_FACTOR groups.
        centroids = [point_node(0,0) for x in range(BRANCH_FACTOR)]
        assignKMeans(centroids, points)
        # Find the best route through the centroids:
        perms = perm_tables[BRANCH_FACTOR]
        centroid_route = brute_force_best_perm(centroids, perms)
        # And recurse:
        for c in centroid_route:
            cluster = [point_node(p.x, p.y) for p in points if p.k_index == c.index]
            divide_and_generate_route(cluster, route, level+1)
    elif len(points) == 1:
        route += points
    else:
        # This group is small enough to solve by brute-force:
        perms = perm_tables[len(points)]
        route += brute_force_best_perm(points, perms)

###################################################################################################################
# Nearest neighbour approach
###################################################################################################################

def get_nearest_neighbour_route(in_points):
    # Simply sort by greedily choosing closest point next. Works surprisingly well in our toy case (but open to attack -
    # eg consider the 1d case of these points: [0,1,-2,5,-10,21...])
    points = list(in_points)
    for n in range(len(points)-1):
        p1 = points[n]
        dists = [distance(p1, p) for p in points[n+1:]]
        nn_ind = n+1 + dists.index(min(dists))
        tmp = points[n+1]
        points[n+1] = points[nn_ind]
        points[nn_ind] = tmp
        n+=1
    return points

###################################################################################################################
# Convex hull approach
###################################################################################################################

def get_spiral_route(points):
    # Just for fun - construct a spiral route by modifying the Jarvis March convex hull algorithm such that it is
    # not allowed to return to any points that have already been visited.
    # Definitely not the shortest route, but (aside from the fudging needed to fix the start point) it creates
    # a route that looks pretty (never crosses itself) and only ever requires the agent to turn left...

    # Useful lambda: Use determinant to quickly calculate whether point p is to the left of line p1-p2
    left_of_line = lambda p1, p2, p: (p.x-p1.x)*(p2.y-p1.y) > (p.y-p1.y)*(p2.x-p1.x)
    
    # remove required starting point; add it back in later.
    required_start_point = points.pop(0)
    # find left-most point:
    for p in points:
        p.on_hull = False
    xvalues = [p.x for p in points]
    startpoint = points[xvalues.index(min(xvalues))]
    bestcandidate = startpoint
    hull = []
    hull.append(startpoint)
    startpoint.on_hull = True
    while True:
        for p in points:
            if (not p.on_hull) and (left_of_line(startpoint, bestcandidate, p) or bestcandidate == startpoint):
                bestcandidate = p
        #if bestcandidate == hull[0] and len(hull) > 1:
        if len(hull) == len(points):
            points.insert(0, required_start_point)
            hull.append(required_start_point)
            hull.reverse()
            return hull
        hull.append(bestcandidate)
        bestcandidate.on_hull = True
        startpoint = bestcandidate

###################################################################################################################
# Genetic algorithm approach
###################################################################################################################

def shuffle(points):
    for i in range(1, len(points)):    # Always leave starting point in place.
        ind = random.randint(i, len(points) - 1)
        (points[i], points[ind]) = (points[ind], points[i])

def get_genetic_algorithm_route(progress_callback, points, k, iters, mutation_probability, crossover_probability):
    # Seems to work best with tournament selection. Never really gets particularly close to optimum solution,
    # even with fancy self-adapting mutation/crossover probabilites, large generations, many iterations, etc.
    # But it's cute.

    # Choose k random starting points:
    generation = []
    total_fitness = 0
    roulette = False
    tournament_size = int(2 * math.ceil(math.sqrt(k)))

    for i in range(k):
        sample = list(points)
        shuffle(sample)
        score = old_div(1.0, path_length(sample))
        total_fitness += score
        generation.append((sample, score))

    for it in range(iters):
        if progress_callback != None:
            progress_callback(100 * float(it) / float(iters))

        # Create next generation:
        next_gen = []
        for i in range(int(math.ceil(old_div(float(k), 2.0)))):
            # Choose two parents:
            if roulette:
                # Choose by roulette:
                r = random.random() * total_fitness
                t = generation[0][1]
                c = 0
                while t < r and c < len(generation):
                    c += 1
                    t += generation[c][1]
                parent1 = generation[c][0]
                r = random.random() * total_fitness
                t = generation[0][1]
                c = 0
                while t < r and c < len(generation):
                    c += 1
                    t += generation[c][1]
                parent2 = generation[c][0]
            else:
                # Choose by tournament:
                tournament=[]
                for t in range(tournament_size):
                    tournament.append(random.choice(generation))
                parent1 = max(tournament, key=lambda p:p[1])[0]
                tournament=[]
                for t in range(tournament_size):
                    tournament.append(random.choice(generation))
                parent2 = max(tournament, key=lambda p:p[1])[0]

            # Perform crossover?
            p_crossover = random.random() * crossover_probability
            if random.random() <= p_crossover:
                left = random.randint(0, len(points)-1)
                right = left
                while (right == left):
                    right = random.randint(0, len(points)-1)
                if left > right:
                    (left, right) = (right, left)
                subsection1 = parent1[left:right]
                subsection2 = parent2[left:right]
                remainder1 = []
                remainder2 = []
                for s in parent2:
                    if not s in subsection1:
                        remainder1.append(s)
                for s in parent1:
                    if not s in subsection2:
                        remainder2.append(s)
                child1 = remainder2[:left] + subsection2 + remainder2[left:]
                child2 = remainder1[:left] + subsection1 + remainder1[left:]
            else:
                # No crossover - leave parents unchanged.
                child1 = list(parent1)
                child2 = list(parent2)

            # Perform mutation (taking care to leave starting point untouched):
            p_mutation = random.random() * mutation_probability
            if random.random() < p_mutation:
                ma = random.randint(1, len(child1)-1)
                mb = random.randint(1, len(child1)-1)
                (child1[ma], child1[mb]) = (child1[mb], child1[ma])
            if random.random() < p_mutation:
                ma = random.randint(1, len(child2)-1)
                mb = random.randint(1, len(child2)-1)
                (child2[ma], child2[mb]) = (child2[mb], child2[ma])

            # Add to generation:
            next_gen.append((child1, old_div(1.0,path_length(child1))))
            if len(next_gen) < k:   # Deal with case where k is an odd number.
                next_gen.append((child2, old_div(1.0,path_length(child2))))

        # Generation completed - adjust crossover and mutation probabilites
        generation = list(next_gen)
        print("Gen", it, ": best path = ", path_length(max(generation, key=lambda p:p[1])[0]))
        total_fitness = sum(p[1] for p in generation)
    return max(generation, key=lambda p:p[1])[0]

###################################################################################################################
# Simulated annealing approach
###################################################################################################################

def get_simulated_annealing_route(input_points):
    # Possibly the most succesful route in our toy example, relatively quick to compute for smallish numbers of cities.
    points = list(input_points)
    initial_temperature = math.sqrt(len(input_points))
    temperature = initial_temperature
    t = 0
    alpha = 0.9
    while temperature > 0.25:
        kept_bad = 0
        print("Temp: ", temperature, end=' ')
        dist_before = path_length(points)
        for i in range(len(points)*len(points)):
            i_from = random.randint(1, len(points)-1)
            p = points.pop(i_from)
            i_to = random.randint(1, len(points))
            points.insert(i_to, p)
            dist_after = path_length(points)
            delta = dist_before - dist_after
            if delta < 0:
                # Did not improve things.
                prop = math.exp(old_div(delta,temperature))
                if random.random() > prop:
                    # Reject this move.
                    points.pop(i_to)
                    points.insert(i_from, p)
                    #print "rejected"
                else:
                    # Kept this move.
                    dist_before = dist_after
                    kept_bad += 1
                    #print "dn:", delta
            else:
                # Kept this move.
                dist_before = dist_after
                #print "up:", delta
        t += 1
        temperature = 10 * (alpha**t)
        print("length: ", dist_before, end=' ')
        print("bad moves kept:", kept_bad)
    return points


###################################################################################################################
# Drawing code
###################################################################################################################

def clear_screen(w):
    w.delete("all")

def draw_points(w, points, r=4, c=None):
    fills=["#ff00ff", "#ffff00", "#00ffff", "#ff0000", "#00ff00", "#0000ff", "#880088", "#888800", "#008888", "#880000", "#008800", "#000088"]

    for p in points:
        x = p.get_position()[0]
        y = p.get_position()[1]

        if c != None:
            fill = c
        elif hasattr(p, 'k_index'):
            fill = fills[p.k_index % len(fills)]
        elif hasattr(p, 'index'):
            fill = fills[p.index % len(fills)]
        else:
            fill = "#dddddd"
        w.create_oval(4*(x + 50)-r, 4*(y + 50)-r, 4*(x + 50)+r, 4*(y + 50)+r, fill=fill)

def draw_tree(w, tree, line_width=2, line_colour = "#000000"):
    for e in tree:
        x1 = e.point1.get_position()[0]
        y1 = e.point1.get_position()[1]
        x2 = e.point2.get_position()[0]
        y2 = e.point2.get_position()[1]
        w.create_line(4*(x1 + 50), 4*(y1 + 50), 4*(x2 + 50), 4*(y2 + 50), width=line_width, fill=line_colour)

def draw_path(w, points, line_width=2, line_colour = "#00ff00"):
    x = points[0].get_position()[0]
    y = points[0].get_position()[1]
    for o in points:
        nx = o.get_position()[0]
        ny = o.get_position()[1]
        w.create_line(4*(x+50), 4*(y+50), 4*(nx+50), 4*(ny+50), width=line_width, fill=line_colour)
        x = nx
        y = ny

master = Tk()
w = Canvas(master, width=400, height=400)
w.pack()

def GetMissionXML(summary, agentnames):
    xml = '''<?xml version="1.0" encoding="UTF-8" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <About>
            <Summary>''' + summary + '''</Summary>
        </About>

        <ModSettings>
            <MsPerTick>20</MsPerTick>
        </ModSettings>
        <ServerSection>
            <ServerHandlers>
                <FlatWorldGenerator generatorString="3;7,6*103;2;" />
                <DrawingDecorator>
                    <DrawCuboid x1="-52" y1="5" z1="-52" x2="52" y2="10" z2="52" type="air"/>
                    <DrawCuboid x1="-52" y1="6" z1="-52" x2="52" y2="7" z2="52" type="iron_block"/>
                    <DrawCuboid x1="-50" y1="7" z1="-50" x2="50" y2="7" z2="50" type="obsidian"/>
                    ''' + getCitiesDrawingXML(points) + '''
                </DrawingDecorator>
            </ServerHandlers>
        </ServerSection>'''

    for an in agentnames:
        xml += '''
            <AgentSection mode="Survival">
                <Name>''' + an + '''</Name>
                <AgentStart>
                    <Placement x="0.5" y="10.0" z="0.5"/>
                    <Inventory>
                    </Inventory>
                </AgentStart>
                <AgentHandlers>
                    <ContinuousMovementCommands turnSpeedDegs="240"/>
                    <ObservationFromFullStats/>
                    <ChatCommands/>
                    <MissionQuitCommands/>
                </AgentHandlers>
            </AgentSection>'''
    return xml + '</Mission>'

def getCitiesDrawingXML(points):
    ''' Build an XML string that contains a square for each city'''
    xml = ""
    for p in points:
        x = str(p.x)
        z = str(p.y)
        xml += '<DrawBlock x="' + x + '" y="7" z="' + z + '" type="beacon"/>'
        xml += '<DrawItem x="' + x + '" y="10" z="' + z + '" type="ender_pearl"/>'
    return xml

class RouteGenerators(object):
    NearestNeighbour, Genetic, DivideAndConquer, MinSpanTree, Spiral, Annealing = list(range(6))
    Generators = {NearestNeighbour:lambda progress_callback, points : get_nearest_neighbour_route(points),
                  Genetic:lambda progress_callback, points : get_genetic_algorithm_route(None, points, 20, 3000, 0.7, 0.9),
                  DivideAndConquer:lambda progress_callback, points : get_divide_and_conquer_route(points),
                  MinSpanTree:lambda progress_callback, points : get_MST_route(points),
                  Spiral:lambda progress_callback, points : get_spiral_route(points),
                  Annealing:lambda progress_callback, points : get_simulated_annealing_route(points)}
    DisplayNames = ["Nearest Neighbour", "Genetic Algorithm", "Divide-and-conquer", "Minimum-Span-Tree", "Modified Convex Hull", "Simulated Annealing"]
    AgentNames = ["NearestN", "GenAl", "DivConq", "MinSpan", "ConvHull", "SimAnn"]

class Manager(object):
    def __init__(self, root, canvas, points):
        self.data = {}
        self.points = points
        self.valid = True
        self.canvas = canvas
        self.root = root
        draw_points(self.canvas, points, 4, "#ff00dd")
        self.update()
        self.finished_agents = 0

    def newPosition(self, agent, point, text):
        if not agent in self.data:
            self.data[agent]={"pos":point_node(0,0), "target":point_node(0,0)}
        self.data[agent]["last_pos"] = self.data[agent]["pos"]
        self.data[agent]["pos"] = point
        self.data[agent]["text"] = text
        self.valid = False

    def finished(self, agent):
        self.finished_agents += 1
        if self.finished_agents == len(self.data):
            self.root.quit()

    def draw(self):
        for agent in self.data:
            fill = ["#ff0000","#00ff00","#0000ff","#ff00ff","#ffff00","#00ffff","#000000","#ffffff"][agent % 8]
            if "canvas_item" in self.data[agent]:
                self.canvas.delete(self.data[agent]["canvas_item"])
            if "canvas_text_item" in self.data[agent]:
                self.canvas.delete(self.data[agent]["canvas_text_item"])
            if "last_pos" in self.data[agent]:
                point = self.data[agent]["last_pos"]
                if point != None:
                    x, y = point.x, point.y
                    r = 1
                    self.canvas.create_oval(4*(x + 50)-r, 4*(y + 50)-r, 4*(x + 50)+r, 4*(y + 50)+r, fill=fill, outline=fill)
                    self.data[agent]["last_pos"] = None
            point = self.data[agent]["pos"]
            x, y = point.x, point.y
            r = 3
            self.data[agent]["canvas_item"]=self.canvas.create_oval(4*(x + 50)-r, 4*(y + 50)-r, 4*(x + 50)+r, 4*(y + 50)+r, fill=fill)
            self.data[agent]["canvas_text_item"]=self.canvas.create_text(4*(x + 50)+10, 4*(y + 50)+10, text=self.data[agent]["text"])

        self.root.update()
        self.valid = True

    def update(self):
        if not self.valid:
            self.draw()
        self.root.after(50, self.update)

class SalesmanAgent(threading.Thread):
    def __init__(self, role, routeType, clientPool, missionXML, points, manager):
        threading.Thread.__init__(self)
        self.role = role
        self.route_generator = routeType
        self.client_pool = clientPool
        self.mission_xml = missionXML
        self.agent_host = MalmoPython.AgentHost()
        self.mission = MalmoPython.MissionSpec(missionXML, True)
        self.mission_record = MalmoPython.MissionRecordSpec("tsp_" + str(self.role) + ".tgz")
        self.mission_record.recordCommands()

        self.points = [point_node(p.x, p.y) for p in points] # take a copy
        self.manager = manager

    def run(self):
        used_attempts = 0
        max_attempts = 5
        while True:
            try:
                # Attempt to start the mission:
                self.agent_host.startMission(self.mission, self.client_pool, self.mission_record, self.role, "TSPExperiment")
                break
            except MalmoPython.MissionException as e:
                errorCode = e.details.errorCode
                if errorCode == MalmoPython.MissionErrorCode.MISSION_SERVER_WARMING_UP:
                    # Server not quite ready yet - waiting...
                    time.sleep(2)
                elif errorCode == MalmoPython.MissionErrorCode.MISSION_INSUFFICIENT_CLIENTS_AVAILABLE:
                    # Not enough available Minecraft instances running.
                    used_attempts += 1
                    if used_attempts < max_attempts:
                        time.sleep(2)
                elif errorCode == MalmoPython.MissionErrorCode.MISSION_SERVER_NOT_FOUND:
                    # Server not found - has the mission with role 0 been started yet?
                    used_attempts += 1
                    if used_attempts < max_attempts:
                        time.sleep(2)
                else:
                    print("Other error:", e.message)
                    print("Bailing immediately.")
                    exit(1)
            if used_attempts == max_attempts:
                print("Failed to start mission - bailing now.")
                exit(1)

        start_time = time.time()
        world_state = self.agent_host.getWorldState()
        while not world_state.has_mission_begun:
            time.sleep(0.1)
            world_state = self.agent_host.getWorldState()
            if len(world_state.errors) > 0:
                for err in world_state.errors:
                    print(err)
                exit(1)
            if time.time() - start_time > 120:
                print("Mission failed to begin within two minutes - did you forget to start the other agent?")
                exit(1)
        self.route = self.calculateRoute(self.points)
        self.runMissionLoop()
        print(RouteGenerators.DisplayNames[self.route_generator], "agent has finished!")

    def calculateRoute(self, points):
        return RouteGenerators.Generators[self.route_generator](self, points)

    def onRouteCalculationProgress(self, percentage):
        pass

    def angvel(self, target, current, scale):
        '''Use sigmoid function to choose a delta that will help smoothly steer from current angle to target angle.'''
        delta = target - current
        while delta < -180:
            delta += 360;
        while delta > 180:
            delta -= 360;
        return (old_div(2.0, (1.0 + math.exp(old_div(-delta,scale))))) - 1.0
        
    def runMissionLoop(self):
        turn_key = ""
        yawToPoint = lambda point, x, y, z: -180 * math.atan2(point.x-x, point.y-z) / math.pi
        self.currentCity = 0
        self.agent_host.sendCommand("move 1")   # Full speed ahead...
        while (True):
            world_state = self.agent_host.getWorldState()
            if not world_state.is_mission_running:
                return
            if world_state.number_of_observations_since_last_state > 0:
                obvsText = world_state.observations[-1].text
                data = json.loads(obvsText) # observation comes in as a JSON string...
                current_x = data.get(u'XPos', 0)
                current_z = data.get(u'ZPos', 0)
                current_y = data.get(u'YPos', 0)
                self.manager.newPosition(self.role, point_node(current_x, current_z), RouteGenerators.DisplayNames[self.route_generator])
                yaw = data.get(u'Yaw', 0)
                pitch = data.get(u'Pitch', 0)
                if distance(point_node(current_x-0.5, current_z-0.5), self.route[self.currentCity]) < 1:
                    self.currentCity += 1
                    chat_string = "Reached " + str(self.currentCity) + "/" + str(len(self.route))
                    self.agent_host.sendCommand("chat " + chat_string)
                    if self.currentCity >= len(self.route):
                        self.agent_host.sendCommand("turn 0")
                        self.agent_host.sendCommand("move 0")
                        self.agent_host.sendCommand("jump 1")
                        self.agent_host.sendCommand("quit")
                        self.manager.finished(self.role)
                        return  # Finished!
                yaw_to_next_point = yawToPoint(self.route[self.currentCity], current_x-0.5, current_y, current_z-0.5)
                turn_speed = self.angvel(yaw_to_next_point, yaw, 16.0)
                move_speed = (1.0 - abs(turn_speed)) * (1.0 - abs(turn_speed))
                self.agent_host.sendCommand("turn " + str(turn_speed))
                self.agent_host.sendCommand("move " + str(move_speed))
            time.sleep(0.001)

if sys.version_info[0] == 2:
    sys.stdout = os.fdopen(sys.stdout.fileno(), 'w', 0)  # flush print output immediately
else:
    import functools
    print = functools.partial(print, flush=True)

# Create a pool of Minecraft Mod clients.
# By default, mods will choose consecutive mission control ports, starting at 10000,
# so running four mods locally should produce the following pool by default (assuming nothing else
# is using these ports):
my_client_pool = MalmoPython.ClientPool()
my_client_pool.add(MalmoPython.ClientInfo("127.0.0.1", 10000))
my_client_pool.add(MalmoPython.ClientInfo("127.0.0.1", 10001))
my_client_pool.add(MalmoPython.ClientInfo("127.0.0.1", 10002))
my_client_pool.add(MalmoPython.ClientInfo("127.0.0.1", 10003))
my_client_pool.add(MalmoPython.ClientInfo("127.0.0.1", 10004))
my_client_pool.add(MalmoPython.ClientInfo("127.0.0.1", 10005))

# Create one agent host for parsing:
parser = MalmoPython.AgentHost()
options = [("nn", "n", RouteGenerators.NearestNeighbour, True),
           ("gen-al", "g", RouteGenerators.Genetic, False),
           ("div-and-conq", "d", RouteGenerators.DivideAndConquer, False),
           ("mst", "m", RouteGenerators.MinSpanTree, True),
           ("conv-hull", "c", RouteGenerators.Spiral, False),
           ("sa", "s", RouteGenerators.Annealing, True)]

for opt in options:
    parser.addOptionalFlag(opt[0] + "," + opt[1], "Add " + RouteGenerators.DisplayNames[opt[2]] + " agent")

parser.addOptionalIntArgument("points,p", "Number of points to use", 50)

try:
    parser.parse( sys.argv )
except RuntimeError as e:
    print('ERROR:',e)
    print(parser.getUsage())
    exit(1)
if parser.receivedArgument("help"):
    print(parser.getUsage())
    exit(0)

# Parse the command-line options:
TOTAL_POINTS = parser.getIntArgument("points")
INTEGRATION_TEST_MODE = parser.receivedArgument("test")

agentnames = [RouteGenerators.AgentNames[x[2]] for x in options if parser.receivedArgument(x[0])]
if not len(agentnames):
    agentnames = [RouteGenerators.AgentNames[x[2]] for x in options if x[3]]

# Create some data:
points = [point_node(0,0)]  # Fix start point at the centre
for i in range(TOTAL_POINTS-1):
    points.append(point_node(random.randint(-50,50), random.randint(-50,50)))

# Create mission xml:
xml = GetMissionXML("Travelling Salesfolk!", agentnames)

# Create the agents:
manager = Manager(master, w, points)
agents = []

role = 0
for opt in options:
    if parser.receivedArgument(opt[0]):
        agents.append(SalesmanAgent(role, opt[2], my_client_pool, xml, points, manager))
        role += 1
if not len(agents):
    print("No agents specified - using defaults")
    for opt in options:
        if opt[3]:
            agents.append(SalesmanAgent(role, opt[2], my_client_pool, xml, points, manager))
            role += 1

# Start them all off...
for agent in agents:
    agent.start()

# And wait.
mainloop()

for agent in agents:
    agent.join()

# Allow user time to admire the finished plot:
if not INTEGRATION_TEST_MODE:
    nb = input('Press enter to quit')
