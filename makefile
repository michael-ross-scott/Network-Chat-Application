# Makefile for Network Assignment

LIB = ../lib
SRCDIR = src
BINDIR = bin

CLI = $(LIB)/cli/commons-cli-1.3.1.jar
ASM = $(LIB)/asm/asm-5.0.4.jar:$(LIB)/asm/asm-commons-5.0.4.jar:$(LIB)/asm/asm-tree-5.0.4.jar
JUNIT = $(LIB)/junit/junit-4.12.jar:$(LIB)/junit/hamcrest-core-1.3.jar
JACOCO = $(LIB)/jacoco/org.jacoco.core-0.7.5.201505241946.jar:$(LIB)/jacoco/org.jacoco.report-0.7.5.201505241946.jar:
TOOLS = $(LIB)/tools

JAVAC = javac
JFLAGS = -g -d $(BINDIR) -cp $(BINDIR):$(JUNIT)


vpath %.java $(SRCDIR):$(SRCDIR):$(TESTDIR)
vpath %.class $(BINDIR):$(BINDIR)

# define general build rule for java sources
.SUFFIXES:  .java  .class

.java.class:
	$(JAVAC)  $(JFLAGS)  $<

#default rule - will be invoked by make
all: Client.class Server.class FirstClient.class

clean:
	@rm -f  $(BINDIR)/*.class
	@rm -f $(BINDIR)/*/*.class
	@rm -f jacoco.exec *.xml *.csv
	@rm -Rf coveragereport
	@rm -Rf doc
