#!/bin/bash

cd src
javac -cp . mochadoom/Engine.java
jar cmf Manifest.txt mochadoom.jar .
zip -d mochadoom.jar *.java README.md Manifest.txt

