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
Server: ServerApplication.class PacketHelper Packet

#
# Define the target for building the client application
#
Client: ClientApplication.class PacketHelper Packet FileChunker GoBackNProtocol PacketReceiver PacketSender

FileChunker: FileChunker.class
GoBackNProtocol: GoBackNProtocol.class
PacketReceiver: PacketReceiver.class
PacketSender: PacketSender.class
PacketHelper: PacketHelper.class
Packet: packet.class

#
# Clean Target
#
clean:
	$(RM) ServerApplication.class
	$(RM) ClientApplication.class
	$(RM) FileChunker.class
	$(RM) GoBackNProtocol.class
	$(RM) PacketReceiver.class
	$(RM) PacketSender.class
	$(RM) PacketHelper.class
	$(RM) packet.class
