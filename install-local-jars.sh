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

# Install ShardingSphere Parser SQL Engine Core
echo "Installing ShardingSphere Parser SQL Engine Core..."
if [ -f "libs/shardingsphere-parser-sql-engine-core-5.5.3-SNAPSHOT.jar" ]; then
    mvn install:install-file \
      -Dfile=libs/shardingsphere-parser-sql-engine-core-5.5.3-SNAPSHOT.jar \
      -DgroupId=org.apache.shardingsphere \
      -DartifactId=shardingsphere-parser-sql-engine-core \
      -Dversion=5.5.3-SNAPSHOT \
      -Dpackaging=jar
    echo "✓ ShardingSphere Parser SQL Engine Core installed successfully"
else
    echo "ERROR: libs/shardingsphere-parser-sql-engine-core-5.5.3-SNAPSHOT.jar not found!"
    exit 1
fi

echo ""

# Install ShardingSphere Infra Exception
echo "Installing ShardingSphere Infra Exception..."
if [ -f "libs/shardingsphere-infra-exception-5.5.3-SNAPSHOT.jar" ]; then
    mvn install:install-file \
      -Dfile=libs/shardingsphere-infra-exception-5.5.3-SNAPSHOT.jar \
      -DgroupId=org.apache.shardingsphere \
      -DartifactId=shardingsphere-infra-exception \
      -Dversion=5.5.3-SNAPSHOT \
      -Dpackaging=jar
    echo "✓ ShardingSphere Infra Exception installed successfully"
else
    echo "ERROR: libs/shardingsphere-infra-exception-5.5.3-SNAPSHOT.jar not found!"
    exit 1
fi

echo ""

# Install ShardingSphere Parser SQL Statement Core
echo "Installing ShardingSphere Parser SQL Statement Core..."
if [ -f "libs/shardingsphere-parser-sql-statement-core-5.5.3-SNAPSHOT.jar" ]; then
    mvn install:install-file \
      -Dfile=libs/shardingsphere-parser-sql-statement-core-5.5.3-SNAPSHOT.jar \
      -DgroupId=org.apache.shardingsphere \
      -DartifactId=shardingsphere-parser-sql-statement-core \
      -Dversion=5.5.3-SNAPSHOT \
      -Dpackaging=jar
    echo "✓ ShardingSphere Parser SQL Statement Core installed successfully"
else
    echo "ERROR: libs/shardingsphere-parser-sql-statement-core-5.5.3-SNAPSHOT.jar not found!"
    exit 1
fi

echo ""

# Install ShardingSphere Parser SQL Engine Rewiter
echo "Installing ShardingSphere Parser SQL Engine Rewiter..."
if [ -f "libs/shardingsphere-parser-sql-engine-rewiter-5.5.3-SNAPSHOT.jar" ]; then
    mvn install:install-file \
      -Dfile=libs/shardingsphere-parser-sql-engine-rewiter-5.5.3-SNAPSHOT.jar \
      -DgroupId=org.apache.shardingsphere \
      -DartifactId=shardingsphere-parser-sql-engine-rewiter \
      -Dversion=5.5.3-SNAPSHOT \
      -Dpackaging=jar
    echo "✓ ShardingSphere Parser SQL Engine Rewiter installed successfully"
else
    echo "ERROR: libs/shardingsphere-parser-sql-engine-rewiter-5.5.3-SNAPSHOT.jar not found!"
    exit 1
fi

echo ""

# Install ShardingSphere SQL Federation Compiler
echo "Installing ShardingSphere SQL Federation Compiler..."
if [ -f "libs/shardingsphere-sql-federation-compiler-5.5.3-SNAPSHOT.jar" ]; then
    mvn install:install-file \
      -Dfile=libs/shardingsphere-sql-federation-compiler-5.5.3-SNAPSHOT.jar \
      -DgroupId=org.apache.shardingsphere \
      -DartifactId=shardingsphere-sql-federation-compiler \
      -Dversion=5.5.3-SNAPSHOT \
      -Dpackaging=jar
    echo "✓ ShardingSphere SQL Federation Compiler installed successfully"
else
    echo "ERROR: libs/shardingsphere-sql-federation-compiler-5.5.3-SNAPSHOT.jar not found!"
    exit 1
fi

echo ""
echo "=========================================="
echo "✓ All JARs have been installed to your local Maven repository"
echo "=========================================="
echo ""
echo "You can now run: mvn clean install"


