#
# Define the variable for the Java Compiler
#
JCC = javac

#
# Define the variable for the JavaCompiler Flags
#
#JFLAGS = -Xlint:deprecation
JFLAGS = 

#
# Define the source paths and package paths for brevity
#
PACKAGE_PATH = edu/drexel/cs544/alexlapinski

#
# tell make about java source and binary files
#
.SUFFIXES: .java .class

#
# tell make what to do with java files
#
.java.class:
	$(JCC) $(JFLAGS) $<

#
# Define the default target to compile both Server and Client
#
default: Server Client

#
# Define the target for building the server application
#
Server: $(PACKAGE_PATH)/ServerApplication.class

#
# Define the target for building the client application
#
Client: $(PACKAGE_PATH)/ClientApplication.class

#
# Clean Target
#
clean:
	$(RM) $(PACKAGE_PATH)/*.class
