#!/bin/bash

cd src
find . -type f -name '*.class' -delete
javac -cp . mochadoom/Engine.java
java mochadoom/Engine $@

