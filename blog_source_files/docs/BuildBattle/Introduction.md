## Goals of this Tutorial

This tutorial demonstrates the steps necessary to create a Project Malm&ouml; task inspired by Minecraft Build Battles.
Along the way, we will briefly discuss Project Malm&ouml; as a platform for engineering reinforcement learning (RL) tasks and
evaluating RL agents. We will also look at many of the inner workings of the platform as appropriate including
XML Schema Definitions and MinecraftForge, the modding backend for Project Malm&ouml;,
along with resources that might help in creating mods.

## Minecraft Build Battles

Before we dive into Project Malm&ouml;, given below are a few videos to illustrate what Build Battles are.

 <center><iframe width="800" height="500" src="https://www.youtube.com/embed/1QnGeOMjJ80"
 frameborder="0" allowfullscreen></iframe>
 <iframe width="800" height="500" src="https://www.youtube.com/embed/aPgVk3jq7ss"
 frameborder="0" allowfullscreen></iframe>
 <iframe width="800" height="500" src="https://www.youtube.com/embed/t56UEQy6jOY"
 frameborder="0" allowfullscreen></iframe></center>

Essentially, Build Battles are Minecraft minigames where players compete to create the most fitting
structure for a given theme. We will be creating a task that is more concrete and is more practical
given the current state of Artificial Intelligence (AI).
