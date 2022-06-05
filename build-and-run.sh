#!/bin/bash

cd src
javac -cp . mochadoom/Engine.java
java mochadoom/Engine $@

