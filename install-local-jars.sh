#!/bin/bash

# Script to install local JAR files to Maven local repository

set -e  # Exit on error

echo "=========================================="
echo "Installing local JARs to Maven repository"
echo "=========================================="
echo ""

# Check if libs directory exists
if [ ! -d "libs" ]; then
    echo "ERROR: libs directory not found!"
    exit 1
fi

# Install Calcite Core
echo "Installing Calcite Core..."
if [ -f "libs/calcite-core-1.38.0-U6.jar" ]; then
    mvn install:install-file \
      -Dfile=libs/calcite-core-1.38.0-U6.jar \
      -DgroupId=org.apache.calcite \
      -DartifactId=calcite-core \
      -Dversion=1.38.0-U6 \
      -Dpackaging=jar
    echo "✓ Calcite Core installed successfully"
else
    echo "ERROR: libs/calcite-core-1.38.0-U6.jar not found!"
    exit 1
fi

echo ""

# Install Calcite Linq4j
echo "Installing Calcite Linq4j..."
if [ -f "libs/calcite-linq4j-1.37.0-U8.jar" ]; then
    mvn install:install-file \
      -Dfile=libs/calcite-linq4j-1.37.0-U8.jar \
      -DgroupId=org.apache.calcite \
      -DartifactId=calcite-linq4j \
      -Dversion=1.37.0-U8 \
      -Dpackaging=jar
    echo "✓ Calcite Linq4j installed successfully"
else
    echo "ERROR: libs/calcite-linq4j-1.37.0-U8.jar not found!"
    exit 1
fi

echo ""

# Install ShardingSphere SQL Federation Core
echo "Installing ShardingSphere SQL Federation Core..."
if [ -f "libs/shardingsphere-sql-federation-core-5.5.4-SNAPSHOT-shade.jar" ]; then
    mvn install:install-file \
      -Dfile=libs/shardingsphere-sql-federation-core-5.5.4-SNAPSHOT-shade.jar \
      -DgroupId=org.apache.shardingsphere \
      -DartifactId=shardingsphere-sql-federation-core \
      -Dversion=5.5.4-SNAPSHOT \
      -Dpackaging=jar
    echo "✓ ShardingSphere SQL Federation Core installed successfully"
else
    echo "ERROR: libs/shardingsphere-sql-federation-core-5.5.4-SNAPSHOT-shade.jar not found!"
    exit 1
fi

echo ""
echo "=========================================="
echo "✓ All JARs have been installed to your local Maven repository"
echo "=========================================="
echo ""
echo "You can now run: mvn clean install"


